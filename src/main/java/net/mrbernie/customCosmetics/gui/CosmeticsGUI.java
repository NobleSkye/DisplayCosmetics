package net.mrbernie.customCosmetics.gui;

import net.mrbernie.customCosmetics.CustomCosmetics;
import net.mrbernie.customCosmetics.cosmetics.Cosmetic;
import net.mrbernie.customCosmetics.cosmetics.CosmeticType;
import net.mrbernie.customCosmetics.utils.DataManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.util.*;
import java.util.stream.Collectors;

public class CosmeticsGUI {

    private final CustomCosmetics plugin;
    private final DataManager dataManager;
    private static final int COSMETICS_PER_PAGE = 27; // 3 rows of 9 slots

    public CosmeticsGUI(CustomCosmetics plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
    }

    /**
     * Opens the cosmetics inventory showing all player's cosmetics with pagination
     * @param player The player to show the GUI to
     * @param page The page number (0-indexed)
     */
    public void openCosmeticsInventory(Player player, int page) {
        List<Cosmetic> allCosmetics = dataManager.getPlayerCosmetics(player.getUniqueId());
        
        // Calculate pagination
        int totalPages = Math.max(1, (int) Math.ceil((double) allCosmetics.size() / COSMETICS_PER_PAGE));
        page = Math.max(0, Math.min(page, totalPages - 1));
        
        int startIndex = page * COSMETICS_PER_PAGE;
        int endIndex = Math.min(startIndex + COSMETICS_PER_PAGE, allCosmetics.size());
        
        // Create 4-row inventory
        String title = "§6§lCosmetics §7(Page " + (page + 1) + "/" + totalPages + ")";
        Inventory gui = Bukkit.createInventory(null, 36, title);
        
        // Add cosmetics to first 3 rows (slots 0-26)
        for (int i = startIndex; i < endIndex; i++) {
            Cosmetic cosmetic = allCosmetics.get(i);
            ItemStack item = getCosmeticItemStack(cosmetic, player);
            gui.setItem(i - startIndex, item);
        }
        
        // Bottom row (row 4): Gray stained glass panes with navigation
        ItemStack grayGlass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = grayGlass.getItemMeta();
        glassMeta.setDisplayName(" ");
        grayGlass.setItemMeta(glassMeta);
        
        // Fill bottom row with gray glass
        for (int i = 27; i < 36; i++) {
            gui.setItem(i, grayGlass.clone());
        }
        
        // Previous page arrow (slot 27 - bottom left)
        if (page > 0) {
            ItemStack prevArrow = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prevArrow.getItemMeta();
            prevMeta.setDisplayName("§e◀ Previous Page");
            prevMeta.setLore(Arrays.asList("§7Page " + page + "/" + totalPages));
            prevArrow.setItemMeta(prevMeta);
            gui.setItem(27, prevArrow);
        }
        
        // Info item (slot 31 - bottom center)
        int totalCreated = allCosmetics.size();
        int maxAllowed = getMaxCosmetics(player);
        
        ItemStack infoItem = new ItemStack(Material.NETHER_STAR);
        ItemMeta infoMeta = infoItem.getItemMeta();
        infoMeta.setDisplayName("§6§lYour Cosmetics");
        infoMeta.setLore(Arrays.asList(
            "§7Total: §e" + totalCreated + "§7/§e" + maxAllowed,
            "",
            "§7§oLeft-Click to equip",
            "§7§oRight-Click to unequip",
            "§7§oShift-Right-Click to delete"
        ));
        infoItem.setItemMeta(infoMeta);
        gui.setItem(31, infoItem);
        
        // Next page arrow (slot 35 - bottom right)
        if (page < totalPages - 1) {
            ItemStack nextArrow = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = nextArrow.getItemMeta();
            nextMeta.setDisplayName("§eNext Page ▶");
            nextMeta.setLore(Arrays.asList("§7Page " + (page + 2) + "/" + totalPages));
            nextArrow.setItemMeta(nextMeta);
            gui.setItem(35, nextArrow);
        }
        
        player.openInventory(gui);
    }

    /**
     * Opens the first page of cosmetics
     */
    public void openCosmeticsInventory(Player player) {
        openCosmeticsInventory(player, 0);
    }

    // Legacy method for backwards compatibility - redirects to new inventory
    public void openCategoryGUI(Player player) {
        openCosmeticsInventory(player, 0);
    }

    // Legacy method for backwards compatibility - redirects to new inventory
    public void openCosmeticsPage(Player player, CosmeticType type, int page) {
        openCosmeticsInventory(player, page);
    }

    private int getMaxCosmetics(Player player) {
        if (player.hasPermission("customcosmetics.limit.bypass")) {
            return Integer.MAX_VALUE;
        }
        
        // Check for specific limit permissions
        for (PermissionAttachmentInfo info : player.getEffectivePermissions()) {
            String perm = info.getPermission();
            if (perm.startsWith("customcosmetics.limit.")) {
                try {
                    String numStr = perm.substring("customcosmetics.limit.".length());
                    if (!numStr.equals("bypass")) {
                        return Integer.parseInt(numStr);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        
        return 5; // Default limit
    }

    private List<Cosmetic> getPlayerCosmeticsByType(UUID playerUUID, CosmeticType type) {
        return dataManager.getPlayerCosmetics(playerUUID).stream()
                .filter(c -> c.type() == type)
                .collect(Collectors.toList());
    }

    private ItemStack getCosmeticItemStack(Cosmetic cosmetic) {
        return getCosmeticItemStack(cosmetic, null);
    }

    public ItemStack getCosmeticItemStack(Cosmetic cosmetic, Player player) {
        Material material;
        boolean addTrim = false;

        switch (cosmetic.type()) {
            case HEAD:
                material = Material.IRON_HELMET;
                break;
            case SHOULDER:
                material = Material.IRON_CHESTPLATE;
                break;
            case BACK:
                material = Material.IRON_CHESTPLATE;
                addTrim = true;
                break;
            default:
                material = Material.PAPER;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta metaBase = item.getItemMeta();
        if (metaBase != null) {
            // Check if cosmetic is equipped
            boolean isEquipped = false;
            if (player != null) {
                isEquipped = plugin.getCosmeticManager().isEquipped(player, cosmetic.id());
            }

            metaBase.setDisplayName((isEquipped ? "§a" : "§e") + cosmetic.displayName());
            
            List<String> lore = new ArrayList<>();
            lore.add("§7Type: §f" + cosmetic.type().name());
            lore.add("§7Blocks: §f" + cosmetic.blocks().size());
            lore.add("");
            
            if (isEquipped) {
                lore.add("§a✓ Currently Equipped");
                lore.add("");
                lore.add("§eRight-Click to unequip");
                lore.add("§cShift-Right-Click to delete");
                
                // Add enchantment glint
                metaBase.addEnchant(Enchantment.UNBREAKING, 1, true);
                metaBase.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            } else {
                lore.add("§eLeft-Click to equip");
                lore.add("§cShift-Right-Click to delete");
            }
            
            metaBase.setLore(lore);

            if (addTrim && metaBase instanceof ArmorMeta) {
                try {
                    ArmorMeta armorMeta = (ArmorMeta) metaBase;
                    TrimMaterial trimMaterial = Registry.TRIM_MATERIAL.get(org.bukkit.NamespacedKey.minecraft("amethyst"));
                    TrimPattern trimPattern = Registry.TRIM_PATTERN.get(org.bukkit.NamespacedKey.minecraft("coast"));
                    if (trimMaterial != null && trimPattern != null) {
                        ArmorTrim trim = new ArmorTrim(trimMaterial, trimPattern);
                        armorMeta.setTrim(trim);
                        item.setItemMeta(armorMeta);
                        return item;
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to add armor trim to cosmetic item");
                }
            }

            item.setItemMeta(metaBase);
        }

        return item;
    }
}
