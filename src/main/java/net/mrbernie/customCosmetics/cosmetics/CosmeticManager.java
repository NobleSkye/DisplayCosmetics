package net.mrbernie.customCosmetics.cosmetics;

import net.mrbernie.customCosmetics.CustomCosmetics;
import net.mrbernie.customCosmetics.creation.CreationSession;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CosmeticManager {

    private final CustomCosmetics plugin;
    private final VisibilityManager visibilityManager;
    private final Map<String, Cosmetic> registeredCosmetics = new HashMap<>();
    private final Map<UUID, Map<String, ServerSideCosmeticDisplay>> activeCosmetics = new ConcurrentHashMap<>();
    private final Map<UUID, List<String>> equippedCosmetics = new HashMap<>();

    private static final float PIXEL_SCALE = 0.0625f;

    public CosmeticManager(CustomCosmetics plugin) {
        this.plugin = plugin;
        this.visibilityManager = plugin.getVisibilityManager();
        plugin.getLogger().info("Using server-side entity rendering for cosmetics.");
    }

    public void loadPlayerData(Player player) {
        UUID playerUUID = player.getUniqueId();
        List<String> equippedIds = plugin.getDataManager().getEquippedCosmetics(playerUUID);
        equippedCosmetics.put(playerUUID, new ArrayList<>(equippedIds));
        plugin.getVisibilityManager().loadPlayerSettings(player);
        if (!equippedIds.isEmpty()) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    equippedIds.forEach(id -> spawnCosmetic(player, id));
                }
            }, 20L);
        }
    }

    public void savePlayerData(Player player) {
        List<String> finalEquippedIds = equippedCosmetics.getOrDefault(player.getUniqueId(), new ArrayList<>());
        plugin.getDataManager().saveEquippedCosmetics(player.getUniqueId(), finalEquippedIds);
        plugin.getVisibilityManager().savePlayerSettings(player);
    }

    public void loadCosmetics() {
        registeredCosmetics.clear();
        registeredCosmetics.putAll(plugin.getDataManager().loadAllCosmeticDefinitions());
        startCosmeticUpdateTask();
    }

    public Cosmetic createCosmeticFromBuild(Player player, CreationSession session) {
        // Check cosmetic limit
        int currentCount = plugin.getDataManager().getCosmeticCountByPlayer(player.getUniqueId());
        int limit = getCosmeticLimit(player);
        
        if (currentCount >= limit) {
            player.sendMessage(ChatColor.RED + "You have reached your cosmetic creation limit (" + limit + ")!");
            return null;
        }
        
        Location corner = session.buildAreaCorner();
        Map<Vector, BlockData> blocks = new HashMap<>();
        int minX = 16, minY = 16, minZ = 16;
        int maxX = -1, maxY = -1, maxZ = -1;
        for (int x = 0; x < 16; x++) { for (int y = 0; y < 16; y++) { for (int z = 0; z < 16; z++) {
            Location blockLoc = corner.clone().add(x, y, z);
            if (blockLoc.getBlock().getType() != Material.AIR) {
                Vector absolutePos = new Vector(x, y, z);
                blocks.put(absolutePos, blockLoc.getBlock().getBlockData());
                if (x < minX) minX = x; if (y < minY) minY = y; if (z < minZ) minZ = z;
                if (x > maxX) maxX = x; if (y > maxY) maxY = y; if (z > maxZ) maxZ = z;
            }
        }}}
        if (blocks.isEmpty()) {
            player.sendMessage(ChatColor.RED + "You haven't built anything! The creation was canceled.");
            return null;
        }
        double centerX = minX + (double) (maxX - minX) / 2.0;
        double centerY = minY + (double) (maxY - minY) / 2.0;
        double centerZ = minZ + (double) (maxZ - minZ) / 2.0;
        Vector center = new Vector(centerX, centerY, centerZ);
        Map<Vector, BlockData> relativeBlocks = new HashMap<>();
        blocks.forEach((absolutePos, blockData) -> {
            Vector relativePos = absolutePos.clone().subtract(center);
            relativeBlocks.put(relativePos, blockData);
        });
        String cosmeticId = player.getUniqueId() + "." + System.currentTimeMillis();
        Cosmetic cosmetic = new Cosmetic(cosmeticId, session.cosmeticName(), session.cosmeticType(), relativeBlocks);
        registeredCosmetics.put(cosmeticId, cosmetic);
        plugin.getDataManager().saveCosmeticDefinition(cosmetic, player.getUniqueId());
        int newCount = plugin.getDataManager().getCosmeticCountByPlayer(player.getUniqueId());
        player.sendMessage(ChatColor.GREEN + "Cosmetics '" + session.cosmeticName() + "' successfully created! The item will appear in 2 seconds.");
        player.sendMessage(ChatColor.GRAY + "You have created " + newCount + "/" + limit + " cosmetics.");
        return cosmetic;
    }
    
    private int getCosmeticLimit(Player player) {
        // Check for bypass permission (unlimited)
        if (player.hasPermission("customcosmetics.limit.bypass")) {
            return Integer.MAX_VALUE;
        }
        // Check for specific permission limits
        for (int i = 100; i >= 1; i--) {
            if (player.hasPermission("customcosmetics.limit." + i)) {
                return i;
            }
        }
        // Default limit if no permission found
        return 3;
    }

    private void spawnCosmetic(Player player, String cosmeticId) {
        Cosmetic cosmetic = registeredCosmetics.get(cosmeticId);
        if (cosmetic == null) {
            plugin.getLogger().warning("Attempted to spawn non-existent cosmetic: " + cosmeticId);
            return;
        }
        
        ServerSideCosmeticDisplay display = new ServerSideCosmeticDisplay(plugin, player, cosmetic);
        display.spawn();
        
        activeCosmetics.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>()).put(cosmeticId, display);
        
        // Apply visibility settings
        if (!visibilityManager.canSeeSelf(player)) {
            display.hideFrom(player);
        }
        
        // Hide from other players if they have others visibility disabled
        for (Player other : player.getWorld().getPlayers()) {
            if (other.equals(player)) continue;
            if (!visibilityManager.canSeeOthers(other)) {
                display.hideFrom(other);
            }
        }
    }

    private void despawnCosmetic(Player player, String cosmeticId) {
        UUID playerUUID = player.getUniqueId();
        if (activeCosmetics.containsKey(playerUUID)) {
            Map<String, ServerSideCosmeticDisplay> playerActiveCosmetics = activeCosmetics.get(playerUUID);
            if (playerActiveCosmetics.containsKey(cosmeticId)) {
                playerActiveCosmetics.get(cosmeticId).destroy();
                playerActiveCosmetics.remove(cosmeticId);
            }
        }
    }

    public void equipCosmetic(Player player, String cosmeticId) {
        equippedCosmetics.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>()).add(cosmeticId);
        spawnCosmetic(player, cosmeticId);
    }

    public void unequipCosmetic(Player player, String cosmeticId) {
        equippedCosmetics.getOrDefault(player.getUniqueId(), new ArrayList<>()).remove(cosmeticId);
        despawnCosmetic(player, cosmeticId);
    }

    public void removeAllCosmetics(Player player) {
        List<String> equipped = new ArrayList<>(getEquippedCosmetics(player));
        equipped.forEach(id -> despawnCosmetic(player, id));
        activeCosmetics.remove(player.getUniqueId());
        plugin.getVisibilityManager().unloadPlayerSettings(player);
    }



    public ItemStack getCosmeticItemStack(String cosmeticId) {
        Cosmetic cosmetic = registeredCosmetics.get(cosmeticId);
        if (cosmetic == null) {
            return null;
        }
        ItemStack item = new ItemStack(Material.STICK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + cosmetic.displayName());
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Type: " + cosmetic.type().getDisplayName(),
                    ChatColor.DARK_GRAY + cosmetic.id()
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    public void giveCosmeticItem(Player player, Cosmetic cosmetic) {
        ItemStack item = getCosmeticItemStack(cosmetic.id());
        if(item != null) {
            player.getInventory().addItem(item);
        }
    }

    private void startCosmeticUpdateTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID uuid : new ArrayList<>(activeCosmetics.keySet())) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player == null || !player.isOnline()) {
                        activeCosmetics.get(uuid).values().forEach(ServerSideCosmeticDisplay::destroy);
                        activeCosmetics.remove(uuid);
                        equippedCosmetics.remove(uuid);
                        continue;
                    }
                    Map<String, ServerSideCosmeticDisplay> playerCosmetics = activeCosmetics.get(uuid);
                    if (playerCosmetics == null) continue;
                    
                    playerCosmetics.forEach((cosmeticId, display) -> {
                        display.update();
                    });
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    public void refreshVisibilityForPlayer(Player player) {
        // Update visibility for player's own cosmetics
        if (activeCosmetics.containsKey(player.getUniqueId())) {
            Map<String, ServerSideCosmeticDisplay> ownDisplays = activeCosmetics.get(player.getUniqueId());
            for (ServerSideCosmeticDisplay display : ownDisplays.values()) {
                if (visibilityManager.canSeeSelf(player)) {
                    display.showTo(player);
                } else {
                    display.hideFrom(player);
                }
            }
        }
        
        // Update visibility of other players' cosmetics for this player
        for (Player other : player.getWorld().getPlayers()) {
            if (other.equals(player)) continue;
            
            if (activeCosmetics.containsKey(other.getUniqueId())) {
                Map<String, ServerSideCosmeticDisplay> otherDisplays = activeCosmetics.get(other.getUniqueId());
                for (ServerSideCosmeticDisplay display : otherDisplays.values()) {
                    if (visibilityManager.canSeeOthers(player)) {
                        display.showTo(player);
                    } else {
                        display.hideFrom(player);
                    }
                }
            }
        }
    }

    private Location getCosmeticLocation(Player player, CosmeticType type, Vector relativeOffset) {
        Location playerLoc = player.getEyeLocation();
        Vector offset = new Vector();
        switch (type) {
            case HEAD:
                offset.setY(0.5);
                break;
            case BACK:
                offset = playerLoc.getDirection().clone().setY(0).normalize().multiply(-0.3);
                offset.setY(-0.3);
                break;
            case SHOULDER:
                Vector right = playerLoc.getDirection().clone().setY(0).normalize().crossProduct(new Vector(0, 1, 0)).normalize().multiply(0.35);
                offset = right;
                offset.setY(-0.1);
                break;
        }
        Vector rotatedOffset = relativeOffset.clone().rotateAroundY(-Math.toRadians(playerLoc.getYaw()));
        return playerLoc.add(offset).add(rotatedOffset);
    }

    public Cosmetic getCosmeticByItem(ItemStack item) {
        if (item == null || !item.hasItemMeta() || item.getItemMeta().getLore() == null) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.getLore();
        if (lore.size() < 2) return null;
        String idLine = lore.getLast();
        String cosmeticId = ChatColor.stripColor(idLine);
        return registeredCosmetics.get(cosmeticId);
    }

    public List<String> getEquippedCosmetics(Player player) {
        return equippedCosmetics.getOrDefault(player.getUniqueId(), new ArrayList<>());
    }
}