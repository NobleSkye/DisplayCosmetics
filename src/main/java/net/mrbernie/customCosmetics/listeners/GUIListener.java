package net.mrbernie.customCosmetics.listeners;

import net.mrbernie.customCosmetics.CustomCosmetics;
import net.mrbernie.customCosmetics.commands.CosmeticsCommand;
import net.mrbernie.customCosmetics.cosmetics.Cosmetic;
import net.mrbernie.customCosmetics.cosmetics.CosmeticType;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GUIListener implements Listener {

    private final CustomCosmetics plugin;
    private final Map<UUID, CosmeticType> viewingType = new HashMap<>();
    private final Map<UUID, Integer> currentPage = new HashMap<>();

    public GUIListener(CustomCosmetics plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        Player player = (Player) event.getWhoClicked();
        
        // Handle cosmetics categories GUI
        if (title.equals("§6§lCosmetics Categories")) {
            event.setCancelled(true);
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

            String displayName = clickedItem.getItemMeta() != null ? 
                    ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName()) : "";

            CosmeticType type = null;
            if (displayName.contains("HEAD")) {
                type = CosmeticType.HEAD;
            } else if (displayName.contains("SHOULDER")) {
                type = CosmeticType.SHOULDER;
            } else if (displayName.contains("BACK")) {
                type = CosmeticType.BACK;
            }

            if (type != null) {
                viewingType.put(player.getUniqueId(), type);
                currentPage.put(player.getUniqueId(), 0);
                plugin.getCosmeticsGUI().openCosmeticsPage(player, type, 0);
            }
            return;
        }

        // Handle cosmetics page GUI
        if (title.contains("Cosmetics") && title.contains("Page")) {
            event.setCancelled(true);
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

            String displayName = clickedItem.getItemMeta() != null ? 
                    ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName()) : "";

            // Navigation buttons
            if (displayName.contains("Previous Page")) {
                int page = currentPage.getOrDefault(player.getUniqueId(), 0);
                CosmeticType type = viewingType.get(player.getUniqueId());
                if (type != null && page > 0) {
                    currentPage.put(player.getUniqueId(), page - 1);
                    plugin.getCosmeticsGUI().openCosmeticsPage(player, type, page - 1);
                }
                return;
            } else if (displayName.contains("Next Page")) {
                int page = currentPage.getOrDefault(player.getUniqueId(), 0);
                CosmeticType type = viewingType.get(player.getUniqueId());
                if (type != null) {
                    currentPage.put(player.getUniqueId(), page + 1);
                    plugin.getCosmeticsGUI().openCosmeticsPage(player, type, page + 1);
                }
                return;
            } else if (displayName.contains("Back to Categories")) {
                plugin.getCosmeticsGUI().openCategoryGUI(player);
                return;
            }

            // Cosmetic item clicked - equip or delete
            if (clickedItem.getType() == Material.IRON_HELMET || 
                clickedItem.getType() == Material.IRON_CHESTPLATE) {
                
                String cosmeticName = ChatColor.stripColor(
                    clickedItem.getItemMeta().getDisplayName()
                );
                
                // Find cosmetic by name
                Cosmetic cosmetic = plugin.getDataManager().getPlayerCosmetics(player.getUniqueId())
                        .stream()
                        .filter(c -> c.displayName().equals(cosmeticName))
                        .findFirst()
                        .orElse(null);

                if (cosmetic != null) {
                    // Shift-right-click to delete
                    if (event.isShiftClick() && event.isRightClick()) {
                        plugin.getDataManager().deleteCosmetic(player.getUniqueId(), cosmetic.id());
                        plugin.getCosmeticManager().unequipCosmetic(player, cosmetic.id());
                        plugin.getCosmeticManager().loadCosmetics();
                        player.sendMessage(ChatColor.RED + "Deleted cosmetic: " + cosmeticName);
                        
                        // Refresh the page
                        CosmeticType type = viewingType.get(player.getUniqueId());
                        int page = currentPage.getOrDefault(player.getUniqueId(), 0);
                        plugin.getCosmeticsGUI().openCosmeticsPage(player, type, page);
                    } else if (event.isRightClick()) {
                        // Right-click to unequip (if equipped)
                        if (plugin.getCosmeticManager().isEquipped(player, cosmetic.id())) {
                            plugin.getCosmeticManager().unequipCosmetic(player, cosmetic.id());
                            player.sendMessage(ChatColor.YELLOW + "Unequipped " + cosmeticName + "!");
                            
                            // Refresh the page
                            CosmeticType type = viewingType.get(player.getUniqueId());
                            int page = currentPage.getOrDefault(player.getUniqueId(), 0);
                            plugin.getCosmeticsGUI().openCosmeticsPage(player, type, page);
                        } else {
                            player.sendMessage(ChatColor.RED + "This cosmetic is not equipped!");
                        }
                    } else {
                        // Left-click to equip
                        if (plugin.getCosmeticManager().equipCosmetic(player, cosmetic.id())) {
                            player.sendMessage(ChatColor.GREEN + "Equipped " + cosmeticName + "!");
                            
                            // Refresh the page
                            CosmeticType type = viewingType.get(player.getUniqueId());
                            int page = currentPage.getOrDefault(player.getUniqueId(), 0);
                            plugin.getCosmeticsGUI().openCosmeticsPage(player, type, page);
                        }
                    }
                }
            }
            return;
        }

        // Original equipment GUI handling
        if (!event.getView().getTitle().equals(CosmeticsCommand.INV_TITLE)) {
            return;
        }

        event.setCancelled(true);
        ItemStack clickedItem = event.getCurrentItem();
        Inventory topInventory = event.getView().getTopInventory();
        Inventory bottomInventory = event.getView().getBottomInventory();

        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        Cosmetic cosmetic = plugin.getCosmeticManager().getCosmeticByItem(clickedItem);
        if (cosmetic == null) {
            return;
        }

        if (event.getClickedInventory().equals(topInventory)) {
            if (player.getInventory().firstEmpty() == -1) {
                player.sendMessage(ChatColor.RED + "There is no room in your inventory!");
                return;
            }

            plugin.getCosmeticManager().unequipCosmetic(player, cosmetic.id());
            player.getInventory().addItem(clickedItem.clone());
            topInventory.setItem(event.getSlot(), null);
            player.sendMessage(ChatColor.YELLOW + "The cosmetics are removed.");
        } else if (event.getClickedInventory().equals(bottomInventory)) {
            if (topInventory.firstEmpty() == -1) {
                player.sendMessage(ChatColor.RED + "There are no free slots for cosmetics!");
                return;
            }

            plugin.getCosmeticManager().equipCosmetic(player, cosmetic.id());
            topInventory.addItem(clickedItem.clone());
            bottomInventory.setItem(event.getSlot(), null);
            player.sendMessage(ChatColor.GREEN + "Cosmetics are on.");
        }
    }
}