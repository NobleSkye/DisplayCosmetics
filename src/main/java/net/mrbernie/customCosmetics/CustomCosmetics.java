package net.mrbernie.customCosmetics;

import net.mrbernie.customCosmetics.commands.CosmeticsCommand;
import net.mrbernie.customCosmetics.cosmetics.CosmeticManager;
import net.mrbernie.customCosmetics.cosmetics.VisibilityManager;
import net.mrbernie.customCosmetics.creation.CreationManager;
import net.mrbernie.customCosmetics.gui.CosmeticsGUI;
import net.mrbernie.customCosmetics.listeners.GUIListener;
import net.mrbernie.customCosmetics.listeners.PlayerListener;
import net.mrbernie.customCosmetics.utils.DataManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class CustomCosmetics extends JavaPlugin {

    private CreationManager creationManager;
    private CosmeticManager cosmeticManager;
    private DataManager dataManager;
    private VisibilityManager visibilityManager;
    private CosmeticsGUI cosmeticsGUI;

    @Override
    public void onEnable() {

        this.dataManager = new DataManager(this);
        this.visibilityManager = new VisibilityManager(this);
        this.creationManager = new CreationManager(this);
        this.cosmeticManager = new CosmeticManager(this);
        this.cosmeticsGUI = new CosmeticsGUI(this);


        this.cosmeticManager.loadCosmetics();


        CosmeticsCommand cosmeticsCommand = new CosmeticsCommand(this);
        getCommand("cc").setExecutor(cosmeticsCommand);
        getCommand("cc").setTabCompleter(cosmeticsCommand);


        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        getLogger().info("CustomCosmetics plugin has been enabled!");
    }

    @Override
    public void onDisable() {

        getLogger().info("CustomCosmetics plugin has been disabled!");
    }


    public CreationManager getCreationManager() {
        return creationManager;
    }

    public CosmeticManager getCosmeticManager() {
        return cosmeticManager;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public VisibilityManager getVisibilityManager() {
        return visibilityManager;
    }

    public CosmeticsGUI getCosmeticsGUI() {
        return cosmeticsGUI;
    }
}