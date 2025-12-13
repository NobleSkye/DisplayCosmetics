package net.mrbernie.customCosmetics.cosmetics;

import net.mrbernie.customCosmetics.CustomCosmetics;
import org.bukkit.entity.Player;

import java.util.*;

public class VisibilityManager {
    
    private final CustomCosmetics plugin;
    private final Map<UUID, Boolean> canSeeSelf = new HashMap<>();
    private final Map<UUID, Boolean> canSeeOthers = new HashMap<>();
    
    public VisibilityManager(CustomCosmetics plugin) {
        this.plugin = plugin;
    }
    
    public void loadPlayerSettings(Player player) {
        UUID uuid = player.getUniqueId();
        canSeeSelf.put(uuid, plugin.getDataManager().getCanSeeSelf(uuid));
        canSeeOthers.put(uuid, plugin.getDataManager().getCanSeeOthers(uuid));
    }
    
    public void savePlayerSettings(Player player) {
        UUID uuid = player.getUniqueId();
        plugin.getDataManager().saveVisibilitySettings(uuid, 
            canSeeSelf.getOrDefault(uuid, false), 
            canSeeOthers.getOrDefault(uuid, true));
    }
    
    public void unloadPlayerSettings(Player player) {
        UUID uuid = player.getUniqueId();
        canSeeSelf.remove(uuid);
        canSeeOthers.remove(uuid);
    }
    
    public boolean canSeeSelf(Player player) {
        return canSeeSelf.getOrDefault(player.getUniqueId(), false);
    }
    
    public boolean canSeeOthers(Player player) {
        return canSeeOthers.getOrDefault(player.getUniqueId(), true);
    }
    
    public void setCanSeeSelf(Player player, boolean value) {
        canSeeSelf.put(player.getUniqueId(), value);
        savePlayerSettings(player);
    }
    
    public void setCanSeeOthers(Player player, boolean value) {
        canSeeOthers.put(player.getUniqueId(), value);
        savePlayerSettings(player);
    }
    
    public boolean shouldShowCosmetic(Player viewer, Player wearer) {
        if (viewer.equals(wearer)) {
            return canSeeSelf(viewer);
        } else {
            return canSeeOthers(viewer);
        }
    }
    
    public Collection<Player> getViewersFor(Player wearer) {
        List<Player> viewers = new ArrayList<>();
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (shouldShowCosmetic(online, wearer)) {
                viewers.add(online);
            }
        }
        return viewers;
    }
}
