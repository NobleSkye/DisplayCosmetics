package net.mrbernie.customCosmetics.commands;

import net.mrbernie.customCosmetics.CustomCosmetics;
import net.mrbernie.customCosmetics.cosmetics.Cosmetic;
import net.mrbernie.customCosmetics.cosmetics.CosmeticType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CosmeticsCommand implements CommandExecutor, TabCompleter {

    private final CustomCosmetics plugin;
    public static final String INV_TITLE = "Your Cosmetics";

    public CosmeticsCommand(CustomCosmetics plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("The command is for players only.");
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelpMessage(player);
            return true;
        }

        try {
            String subCommand = args[0].toLowerCase();
            switch (subCommand) {
                case "create":
                    handleCreateCommand(player, args);
                    break;
                case "accept":
                    handleAcceptCommand(player);
                    break;
                case "inv":
                    handleListCommand(player);
                    break;
                case "equip":
                    handleInvCommand(player);
                    break;
                case "delete":
                    handleDeleteCommand(player, args);
                    break;
                case "cleanup":
                    handleCleanupCommand(player);
                    break;
                case "reload":
                    handleReloadCommand(sender);
                    break;
                case "toggle":
                    handleToggleCommand(player, args);
                    break;
                default:
                    sendHelpMessage(player);
                    break;
            }
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "An internal error has occurred. Inform the administrator.");
            plugin.getLogger().severe("Error when executing the command /cc:");
            e.printStackTrace();
        }
        return true;
    }

    private void handleCreateCommand(Player player, String[] args) {

        if (!player.hasPermission("customcosmetics.create")) {
            player.sendMessage(ChatColor.RED + "You don't have the right to create cosmetics.");
            return;
        }


        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /cc create <Name> <head/shoulder/back>");
            return;
        }
        String cosmeticName = String.join(" ", Arrays.copyOfRange(args, 1, args.length - 1));
        String typeArg = args[args.length - 1];
        CosmeticType type = CosmeticType.fromString(typeArg);
        if (type == null) {
            player.sendMessage(ChatColor.RED + "Incorrect type! Available: head, shoulder, back");
            return;
        }
        plugin.getCreationManager().startCreation(player, cosmeticName, type);
    }

    private void handleAcceptCommand(Player player) {

        if (!player.hasPermission("customcosmetics.create")) {
            player.sendMessage(ChatColor.RED + "You don't have the rights to create cosmetics.");
            return;
        }


        if (!plugin.getCreationManager().isInCreation(player)) {
            player.sendMessage(ChatColor.RED + "You are not in creation mode.");
            return;
        }
        plugin.getCreationManager().finishCreation(player);
    }

    private void handleInvCommand(Player player) {

        if (!player.hasPermission("customcosmetics.equip")) {
            player.sendMessage(ChatColor.RED + "You do not have the right to equip cosmetics.");
            return;
        }


        openCosmeticsGUI(player);
    }

    private void handleListCommand(Player player) {
        if (!player.hasPermission("customcosmetics.inv")) {
            player.sendMessage(ChatColor.RED + "You do not have the right to view cosmetics.");
            return;
        }

        plugin.getCosmeticsGUI().openCategoryGUI(player);
    }

    private void handleDeleteCommand(Player player, String[] args) {
        if (!player.hasPermission("customcosmetics.delete")) {
            player.sendMessage(ChatColor.RED + "You do not have the right to delete cosmetics.");
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /cc delete <cosmetic-name>");
            return;
        }

        String cosmeticName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        
        // Find cosmetic by name
        Cosmetic cosmetic = plugin.getDataManager().getPlayerCosmetics(player.getUniqueId())
                .stream()
                .filter(c -> c.displayName().equalsIgnoreCase(cosmeticName))
                .findFirst()
                .orElse(null);

        if (cosmetic == null) {
            player.sendMessage(ChatColor.RED + "Cosmetic '" + cosmeticName + "' not found!");
            return;
        }

        plugin.getDataManager().deleteCosmetic(player.getUniqueId(), cosmetic.id());
        plugin.getCosmeticManager().unequipCosmetic(player, cosmetic.id());
        plugin.getCosmeticManager().loadCosmetics();
        player.sendMessage(ChatColor.GREEN + "Deleted cosmetic: " + cosmeticName);
    }

    private void handleCleanupCommand(Player player) {
        if (!player.hasPermission("customcosmetics.admin")) {
            player.sendMessage(ChatColor.RED + "You do not have the right to use this command.");
            return;
        }

        player.sendMessage(ChatColor.YELLOW + "Unequipping all cosmetics and scanning for invalid ones...");
        
        // First, unequip all cosmetics from the player
        List<String> equippedIds = new ArrayList<>(plugin.getCosmeticManager().getEquippedCosmetics(player));
        for (String id : equippedIds) {
            plugin.getCosmeticManager().unequipCosmetic(player, id);
        }
        
        // Then clean up invalid cosmetics
        int cleaned = plugin.getDataManager().cleanupInvalidCosmetics(player.getUniqueId());
        
        if (cleaned > 0) {
            plugin.getCosmeticManager().loadCosmetics();
            player.sendMessage(ChatColor.GREEN + "Unequipped all cosmetics and cleaned up " + cleaned + " invalid cosmetic(s)!");
        } else {
            player.sendMessage(ChatColor.GREEN + "Unequipped all cosmetics. No invalid cosmetics found!");
        }
    }

    private void handleReloadCommand(CommandSender sender) {

        if (!sender.hasPermission("customcosmetics.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have the rights to execute this command.");
            return;
        }


        plugin.getCosmeticManager().loadCosmetics();
        sender.sendMessage(ChatColor.GREEN + "The cosmetics configuration has been reset!");
    }

    private void openCosmeticsGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, InventoryType.HOPPER, INV_TITLE);
        List<String> equippedIds = plugin.getCosmeticManager().getEquippedCosmetics(player);
        for (String cosmeticId : equippedIds) {
            ItemStack cosmeticItem = plugin.getCosmeticManager().getCosmeticItemStack(cosmeticId);
            if (cosmeticItem != null) {
                gui.addItem(cosmeticItem);
            }
        }
        player.openInventory(gui);
    }

    private void handleToggleCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /cc toggle <self|others>");
            return;
        }
        
        String option = args[1].toLowerCase();
        switch (option) {
            case "self":
                boolean newSelfState = !plugin.getVisibilityManager().canSeeSelf(player);
                plugin.getVisibilityManager().setCanSeeSelf(player, newSelfState);
                plugin.getCosmeticManager().refreshVisibilityForPlayer(player);
                player.sendMessage(ChatColor.GREEN + "You can " + (newSelfState ? "now" : "no longer") + " see your own cosmetics.");
                break;
            case "others":
                boolean newOthersState = !plugin.getVisibilityManager().canSeeOthers(player);
                plugin.getVisibilityManager().setCanSeeOthers(player, newOthersState);
                plugin.getCosmeticManager().refreshVisibilityForPlayer(player);
                player.sendMessage(ChatColor.GREEN + "You can " + (newOthersState ? "now" : "no longer") + " see other players' cosmetics.");
                break;
            default:
                player.sendMessage(ChatColor.RED + "Usage: /cc toggle <self|others>");
                break;
        }
    }

    private void sendHelpMessage(Player player) {
        player.sendMessage(ChatColor.GOLD + "--- Commands CustomCosmetics ---");
        player.sendMessage(ChatColor.YELLOW + "/cc create <Name> <Type> - Start making cosmetics.");
        player.sendMessage(ChatColor.YELLOW + "/cc accept - Complete the creation and save the cosmetics.");
        player.sendMessage(ChatColor.YELLOW + "/cc inv - Browse all your cosmetics by category.");
        player.sendMessage(ChatColor.YELLOW + "/cc equip - Open the equipment inventory.");
        player.sendMessage(ChatColor.YELLOW + "/cc delete <Name> - Delete a cosmetic.");
        player.sendMessage(ChatColor.YELLOW + "/cc toggle self - Toggle seeing your own cosmetics.");
        player.sendMessage(ChatColor.YELLOW + "/cc toggle others - Toggle seeing other players' cosmetics.");
        if (player.hasPermission("customcosmetics.admin")) {
            player.sendMessage(ChatColor.RED + "/cc cleanup - Remove invalid/empty cosmetics.");
            player.sendMessage(ChatColor.RED + "/cc reload - Restart the cosmetics configuration.");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            if (sender.hasPermission("customcosmetics.create")) {
                completions.addAll(Arrays.asList("create", "accept"));
            }
            if (sender.hasPermission("customcosmetics.inv")) {
                completions.add("inv");
            }
            if (sender.hasPermission("customcosmetics.equip")) {
                completions.add("equip");
            }
            if (sender.hasPermission("customcosmetics.delete")) {
                completions.add("delete");
            }
            completions.add("toggle");
            if (sender.hasPermission("customcosmetics.admin")) {
                completions.add("cleanup");
                completions.add("reload");
            }
            return completions.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args[0].equalsIgnoreCase("create") && sender.hasPermission("customcosmetics.create")) {
            if (args.length > 2) {
                return Arrays.asList("head", "shoulder", "back").stream()
                        .filter(s -> s.startsWith(args[args.length - 1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        if (args[0].equalsIgnoreCase("toggle") && args.length == 2) {
            return Arrays.asList("self", "others").stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}