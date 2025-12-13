package net.mrbernie.customCosmetics.listeners;

import net.mrbernie.customCosmetics.CustomCosmetics;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final CustomCosmetics plugin;

    public PlayerListener(CustomCosmetics plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getCosmeticManager().loadPlayerData(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getCosmeticManager().savePlayerData(player);
        plugin.getCosmeticManager().removeAllCosmetics(player);
    }
    
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getCreationManager().isInCreation(player)) {
            return;
        }
        
        Location blockLoc = event.getBlock().getLocation();
        if (!isWithinBuildArea(blockLoc)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You can only build within the 16x16x16 area!");
        }
    }
    
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getCreationManager().isInCreation(player)) {
            return;
        }
        
        Location blockLoc = event.getBlock().getLocation();
        if (!isWithinBuildArea(blockLoc)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You can only break blocks within the 16x16x16 area!");
        }
    }
    
    private boolean isWithinBuildArea(Location loc) {
        if (!loc.getWorld().getName().equals("cosmetic_creation_world")) {
            return true;
        }
        
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        
        return x >= 0 && x < 16 && 
               y >= 64 && y < 80 && 
               z >= 0 && z < 16;
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getCreationManager().isInCreation(player)) {
            return;
        }
        
        // Block non-block items like flint and steel
        if (event.hasItem() && event.getItem() != null) {
            Material material = event.getItem().getType();
            if (!material.isBlock() && event.getAction() != Action.LEFT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_AIR) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "You can only use blocks in creation mode!");
            }
        }
    }
    
    @EventHandler
    public void onRedstoneChange(BlockRedstoneEvent event) {
        Location loc = event.getBlock().getLocation();
        if (!loc.getWorld().getName().equals("cosmetic_creation_world")) {
            return;
        }
        
        // Cancel all redstone events to prevent TNT activation
        event.setNewCurrent(0);
    }
}