package net.mrbernie.customCosmetics.gui;

import net.mrbernie.customCosmetics.CustomCosmetics;
import net.mrbernie.customCosmetics.cosmetics.Cosmetic;
import net.mrbernie.customCosmetics.cosmetics.CosmeticType;
import net.mrbernie.customCosmetics.utils.DataManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;

import java.util.*;
import java.util.stream.Collectors;

public class CosmeticsGUI {

    private final CustomCosmetics plugin;
    private final DataManager dataManager;
    private static final int PAGE_SIZE = 45; // 5 rows of 9 slots

    public CosmeticsGUI(CustomCosmetics plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
    }

    public void openCategoryGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, "§6§lCosmetics Categories");

        // HEAD category
        ItemStack headItem = new ItemStack(Material.IRON_HELMET);
        ItemMeta headMeta = headItem.getItemMeta();
        headMeta.setDisplayName("§e§lHEAD Cosmetics");
        List<Cosmetic> headCosmetics = getPlayerCosmeticsByType(player.getUniqueId(), CosmeticType.HEAD);
        headMeta.setLore(Arrays.asList("§7Click to view", "§7Total: §e" + headCosmetics.size()));
        headItem.setItemMeta(headMeta);
        gui.setItem(11, headItem);

        // SHOULDER category
        ItemStack shoulderItem = new ItemStack(Material.IRON_CHESTPLATE);
        ItemMeta shoulderMeta = shoulderItem.getItemMeta();
        shoulderMeta.setDisplayName("§e§lSHOULDER Cosmetics");
        List<Cosmetic> shoulderCosmetics = getPlayerCosmeticsByType(player.getUniqueId(), CosmeticType.SHOULDER);
        shoulderMeta.setLore(Arrays.asList("§7Click to view", "§7Total: §e" + shoulderCosmetics.size()));
        shoulderItem.setItemMeta(shoulderMeta);
        gui.setItem(13, shoulderItem);

        // BACK category
        ItemStack backItem = new ItemStack(Material.IRON_CHESTPLATE);
        ItemMeta backMetaBase = backItem.getItemMeta();
        if (backMetaBase instanceof ArmorMeta) {
            ArmorMeta backMeta = (ArmorMeta) backMetaBase;
            backMeta.setDisplayName("§e§lBACK Cosmetics");
            List<Cosmetic> backCosmetics = getPlayerCosmeticsByType(player.getUniqueId(), CosmeticType.BACK);
            backMeta.setLore(Arrays.asList("§7Click to view", "§7Total: §e" + backCosmetics.size()));
            
            // Add purple armor trim
            try {
                TrimMaterial trimMaterial = Registry.TRIM_MATERIAL.get(org.bukkit.NamespacedKey.minecraft("amethyst"));
                TrimPattern trimPattern = Registry.TRIM_PATTERN.get(org.bukkit.NamespacedKey.minecraft("coast"));
                if (trimMaterial != null && trimPattern != null) {
                    ArmorTrim trim = new ArmorTrim(trimMaterial, trimPattern);
                    backMeta.setTrim(trim);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to add armor trim to BACK cosmetic icon");
            }
            
            backItem.setItemMeta(backMeta);
        }
        gui.setItem(15, backItem);

        player.openInventory(gui);
    }

    public void openCosmeticsPage(Player player, CosmeticType type, int page) {
        List<Cosmetic> cosmetics = getPlayerCosmeticsByType(player.getUniqueId(), type);
        int maxPage = Math.max(0, (cosmetics.size() - 1) / PAGE_SIZE);

        if (page > maxPage) page = maxPage;
        if (page < 0) page = 0;

        String title = "§6§l" + type.name() + " Cosmetics §8(Page " + (page + 1) + "/" + (maxPage + 1) + ")";
        Inventory gui = Bukkit.createInventory(null, 54, title);

        // Add cosmetics to page
        int startIndex = page * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, cosmetics.size());

        for (int i = startIndex; i < endIndex; i++) {
            Cosmetic cosmetic = cosmetics.get(i);
            ItemStack item = getCosmeticItemStack(cosmetic);
            gui.setItem(i - startIndex, item);
        }

        // Navigation buttons
        if (page > 0) {
            ItemStack prevButton = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prevButton.getItemMeta();
            prevMeta.setDisplayName("§e← Previous Page");
            prevButton.setItemMeta(prevMeta);
            gui.setItem(48, prevButton);
        }

        if (page < maxPage) {
            ItemStack nextButton = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = nextButton.getItemMeta();
            nextMeta.setDisplayName("§eNext Page →");
            nextButton.setItemMeta(nextMeta);
            gui.setItem(50, nextButton);
        }

        // Back button
        ItemStack backButton = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.setDisplayName("§c← Back to Categories");
        backButton.setItemMeta(backMeta);
        gui.setItem(49, backButton);

        player.openInventory(gui);
    }

    private List<Cosmetic> getPlayerCosmeticsByType(UUID playerUUID, CosmeticType type) {
        return dataManager.getPlayerCosmetics(playerUUID).stream()
                .filter(c -> c.type() == type)
                .collect(Collectors.toList());
    }

    private ItemStack getCosmeticItemStack(Cosmetic cosmetic) {
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
            metaBase.setDisplayName("§e" + cosmetic.displayName());
            metaBase.setLore(Arrays.asList(
                    "§7Type: §f" + cosmetic.type().name(),
                    "§7Blocks: §f" + cosmetic.blocks().size(),
                    "",
                    "§eClick to equip"
            ));

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
