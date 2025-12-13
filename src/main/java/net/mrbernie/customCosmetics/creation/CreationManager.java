package net.mrbernie.customCosmetics.creation;

import net.mrbernie.customCosmetics.CustomCosmetics;
import net.mrbernie.customCosmetics.cosmetics.Cosmetic;
import net.mrbernie.customCosmetics.cosmetics.CosmeticType;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CreationManager {

    private final CustomCosmetics plugin;
    private final Map<UUID, CreationSession> creationSessions = new HashMap<>();
    private World creationWorld;

    private final Map<UUID, ItemStack[]> playerInventories = new HashMap<>();

    private static final int BUILD_AREA_X = 0;
    private static final int BUILD_AREA_Y = 64;
    private static final int BUILD_AREA_Z = 0;
    private static final int BUILD_AREA_SIZE = 16;

    public CreationManager(CustomCosmetics plugin) {
        this.plugin = plugin;
        setupCreationWorld();
    }

    private void setupCreationWorld() {
        WorldCreator wc = new WorldCreator("cosmetic_creation_world");
        wc.type(WorldType.FLAT);
        wc.generateStructures(false);
        wc.generatorSettings("{\"structures\": {\"structures\": {}}, \"layers\": [{\"block\": \"air\", \"height\": 1}], \"biome\":\"minecraft:plains\"}");
        wc.createWorld();
        this.creationWorld = Bukkit.getWorld("cosmetic_creation_world");

        if (this.creationWorld != null) {
            this.creationWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            this.creationWorld.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
            this.creationWorld.setGameRule(GameRule.DO_MOB_SPAWNING, false);
            this.creationWorld.setTime(6000);
        }
    }

    public void startCreation(Player player, String cosmeticName, CosmeticType type) {
        if (creationSessions.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You are already in creation mode!");
            return;
        }

        if (!creationWorld.getPlayers().isEmpty()) {
            player.sendMessage(ChatColor.RED + "The creation space is currently occupied. Please wait.");
            return;
        }

        Location buildAreaCorner = new Location(creationWorld, BUILD_AREA_X, BUILD_AREA_Y, BUILD_AREA_Z);


        for (int x = 0; x < BUILD_AREA_SIZE; x++) {
            for (int z = 0; z < BUILD_AREA_SIZE; z++) {
                buildAreaCorner.clone().add(x, -1, z).getBlock().setType(Material.BEDROCK);
            }
        }

        for (int x = 0; x < BUILD_AREA_SIZE; x++) {
            for (int y = 0; y < BUILD_AREA_SIZE; y++) {
                for (int z = 0; z < BUILD_AREA_SIZE; z++) {
                    buildAreaCorner.clone().add(x, y, z).getBlock().setType(Material.AIR);
                }
            }
        }

        playerInventories.put(player.getUniqueId(), player.getInventory().getContents());
        player.getInventory().clear();

        Location originalLocation = player.getLocation();

        CreationSession session = new CreationSession(cosmeticName, type, originalLocation, buildAreaCorner);
        creationSessions.put(player.getUniqueId(), session);

        player.setGameMode(GameMode.CREATIVE);
        Location spawnLoc = buildAreaCorner.clone().add(8, 0.5, 8);
        spawnLoc.setYaw(180); // Face north
        spawnLoc.setPitch(0); // Look straight ahead
        player.teleport(spawnLoc);
        player.sendMessage(ChatColor.GREEN + "You have entered the creation mode. Build your item within the platform (16x16x16).");
        player.sendMessage(ChatColor.YELLOW + "When you're done, write /cc accept");
    }

    public void finishCreation(Player player) {
        CreationSession session = creationSessions.get(player.getUniqueId());
        if (session == null) {
            player.sendMessage(ChatColor.RED + "You are not in creation mode.");
            return;
        }


        Cosmetic createdCosmetic = plugin.getCosmeticManager().createCosmeticFromBuild(player, session);


        if (createdCosmetic == null) {

            player.teleport(session.originalLocation());
            player.setGameMode(GameMode.SURVIVAL);
            player.getInventory().setContents(playerInventories.get(player.getUniqueId()));
            playerInventories.remove(player.getUniqueId());
            creationSessions.remove(player.getUniqueId());

            clearPlatform(session.buildAreaCorner());
            return;
        }


        player.teleport(session.originalLocation());
        player.setGameMode(GameMode.SURVIVAL);
        player.getInventory().setContents(playerInventories.get(player.getUniqueId()));
        playerInventories.remove(player.getUniqueId());


        Bukkit.getScheduler().runTaskLater(plugin, () -> {

            if (player.isOnline()) {
                plugin.getCosmeticManager().giveCosmeticItem(player, createdCosmetic);
            }
        }, 40L);


        creationSessions.remove(player.getUniqueId());


        clearPlatform(session.buildAreaCorner());
    }

    private void clearPlatform(Location corner) {
        for (int x = 0; x < BUILD_AREA_SIZE; x++) {
            for (int y = 0; y < BUILD_AREA_SIZE; y++) {
                for (int z = 0; z < BUILD_AREA_SIZE; z++) {
                    corner.clone().add(x, y, z).getBlock().setType(Material.AIR);
                }
            }
        }
    }

    public boolean isInCreation(Player player) {
        return creationSessions.containsKey(player.getUniqueId());
    }

    public CreationSession getSession(Player player) {
        return creationSessions.get(player.getUniqueId());
    }
}