package net.mrbernie.customCosmetics.utils;

import net.mrbernie.customCosmetics.CustomCosmetics;
import net.mrbernie.customCosmetics.cosmetics.Cosmetic;
import net.mrbernie.customCosmetics.cosmetics.CosmeticType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class DataManager {

    private final CustomCosmetics plugin;
    private File cosmeticsFolder;

    public DataManager(CustomCosmetics plugin) {
        this.plugin = plugin;
        setupFiles();
    }

    private void setupFiles() {
        // Create cosmetics folder
        cosmeticsFolder = new File(plugin.getDataFolder(), "cosmetics");
        if (!cosmeticsFolder.exists()) {
            cosmeticsFolder.mkdirs();
        }
    }
    
    private File getPlayerFile(UUID playerUUID) {
        return new File(cosmeticsFolder, playerUUID.toString() + ".yml");
    }
    
    private FileConfiguration getPlayerConfig(UUID playerUUID) {
        File file = getPlayerFile(playerUUID);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create player cosmetics file: " + playerUUID);
                e.printStackTrace();
            }
        }
        return YamlConfiguration.loadConfiguration(file);
    }
    
    private void savePlayerConfig(UUID playerUUID, FileConfiguration config) {
        try {
            config.save(getPlayerFile(playerUUID));
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save player cosmetics file: " + playerUUID);
            e.printStackTrace();
        }
    }

    public void saveEquippedCosmetics(UUID playerUUID, List<String> cosmeticIds) {
        FileConfiguration config = getPlayerConfig(playerUUID);
        config.set("equipped", cosmeticIds);
        savePlayerConfig(playerUUID, config);
    }

    public List<String> getEquippedCosmetics(UUID playerUUID) {
        return getPlayerConfig(playerUUID).getStringList("equipped");
    }

    public void saveVisibilitySettings(UUID playerUUID, boolean canSeeSelf, boolean canSeeOthers) {
        FileConfiguration config = getPlayerConfig(playerUUID);
        config.set("visibility.self", canSeeSelf);
        config.set("visibility.others", canSeeOthers);
        savePlayerConfig(playerUUID, config);
    }

    public boolean getCanSeeSelf(UUID playerUUID) {
        return getPlayerConfig(playerUUID).getBoolean("visibility.self", true);
    }

    public boolean getCanSeeOthers(UUID playerUUID) {
        return getPlayerConfig(playerUUID).getBoolean("visibility.others", true);
    }

    public void saveCosmeticDefinition(Cosmetic cosmetic, UUID creatorUUID) {
        FileConfiguration config = getPlayerConfig(creatorUUID);
        String path = "cosmetics." + cosmetic.id();
        config.set(path + ".name", cosmetic.displayName());
        config.set(path + ".type", cosmetic.type().name());

        // Save blocks
        List<String> blockData = new ArrayList<>();
        cosmetic.blocks().forEach((vec, blockData1) -> {
            blockData.add(vec.getBlockX() + "," + vec.getBlockY() + "," + vec.getBlockZ() + "," + blockData1.getAsString());
        });
        config.set(path + ".blocks", blockData);

        savePlayerConfig(creatorUUID, config);
    }

    public void deleteCosmetic(UUID playerUUID, String cosmeticId) {
        FileConfiguration config = getPlayerConfig(playerUUID);
        String path = "cosmetics." + cosmeticId;
        config.set(path, null);
        savePlayerConfig(playerUUID, config);
    }

    public int cleanupInvalidCosmetics(UUID playerUUID) {
        FileConfiguration config = getPlayerConfig(playerUUID);
        ConfigurationSection cosmeticsSection = config.getConfigurationSection("cosmetics");
        if (cosmeticsSection == null) {
            return 0;
        }

        int cleaned = 0;
        List<String> toRemove = new ArrayList<>();
        List<String> equippedToRemove = new ArrayList<>();
        List<String> currentEquipped = getEquippedCosmetics(playerUUID);

        for (String id : cosmeticsSection.getKeys(false)) {
            String path = "cosmetics." + id;
            String name = config.getString(path + ".name");
            String typeStr = config.getString(path + ".type");
            List<String> blockDataList = config.getStringList(path + ".blocks");

            // Mark for removal if:
            // 1. No name or type
            // 2. No blocks or empty blocks list
            // 3. Invalid type
            if (name == null || name.isEmpty() || 
                typeStr == null || typeStr.isEmpty() ||
                blockDataList == null || blockDataList.isEmpty() ||
                CosmeticType.fromString(typeStr) == null) {
                toRemove.add(id);
                cleaned++;
                
                // If this invalid cosmetic is equipped, mark it for unequipping
                if (currentEquipped.contains(id)) {
                    equippedToRemove.add(id);
                }
            }
        }

        // Remove invalid cosmetics
        for (String id : toRemove) {
            config.set("cosmetics." + id, null);
            plugin.getLogger().info("Cleaned up invalid cosmetic: " + id + " for player " + playerUUID);
        }
        
        // Unequip invalid cosmetics
        if (!equippedToRemove.isEmpty()) {
            currentEquipped.removeAll(equippedToRemove);
            config.set("equipped", currentEquipped);
        }

        if (cleaned > 0) {
            savePlayerConfig(playerUUID, config);
        }

        return cleaned;
    }

    public Map<String, Cosmetic> loadAllCosmeticDefinitions() {
        Map<String, Cosmetic> definitions = new HashMap<>();

        // Load cosmetics from all player files
        File[] playerFiles = cosmeticsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (playerFiles == null) {
            plugin.getLogger().info("No player cosmetic files found.");
            return definitions;
        }

        for (File playerFile : playerFiles) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
            ConfigurationSection cosmeticsSection = config.getConfigurationSection("cosmetics");
            if (cosmeticsSection == null) continue;

            for (String id : cosmeticsSection.getKeys(false)) {
                String path = "cosmetics." + id;
                String name = config.getString(path + ".name");
                String typeStr = config.getString(path + ".type");

                // Validate type
                if (typeStr == null || typeStr.isEmpty()) {
                    plugin.getLogger().warning("Skipping cosmetic '" + id + "': type field is missing");
                    continue;
                }

                CosmeticType type = CosmeticType.fromString(typeStr);
                if (type == null) {
                    plugin.getLogger().warning("Skipping cosmetic '" + id + "': invalid type '" + typeStr + "'");
                    continue;
                }

                Map<Vector, BlockData> blocks = new HashMap<>();
                List<String> blockDataList = config.getStringList(path + ".blocks");

                for (String data : blockDataList) {
                    String[] parts = data.split(",", 4);
                    if (parts.length >= 4) {
                        try {
                            int x = Integer.parseInt(parts[0]);
                            int y = Integer.parseInt(parts[1]);
                            int z = Integer.parseInt(parts[2]);
                            Vector vec = new Vector(x, y, z);
                            BlockData bd = Bukkit.createBlockData(parts[3]);
                            blocks.put(vec, bd);
                        } catch (Exception e) {
                            plugin.getLogger().warning("Failed to parse block data: " + data);
                        }
                    }
                }

                Cosmetic cosmetic = new Cosmetic(id, name, type, blocks);
                definitions.put(id, cosmetic);
            }
        }

        plugin.getLogger().info("Loaded " + definitions.size() + " cosmetic definitions.");
        return definitions;
    }
    
    public int getCosmeticCountByPlayer(UUID playerUUID) {
        FileConfiguration config = getPlayerConfig(playerUUID);
        ConfigurationSection cosmeticsSection = config.getConfigurationSection("cosmetics");
        if (cosmeticsSection == null) {
            return 0;
        }
        return cosmeticsSection.getKeys(false).size();
    }
    
    public List<Cosmetic> getPlayerCosmetics(UUID playerUUID) {
        List<Cosmetic> cosmetics = new ArrayList<>();
        FileConfiguration config = getPlayerConfig(playerUUID);
        ConfigurationSection cosmeticsSection = config.getConfigurationSection("cosmetics");
        if (cosmeticsSection == null) {
            return cosmetics;
        }

        for (String id : cosmeticsSection.getKeys(false)) {
            String path = "cosmetics." + id;
            String name = config.getString(path + ".name");
            String typeStr = config.getString(path + ".type");

            if (typeStr == null || typeStr.isEmpty()) continue;

            CosmeticType type = CosmeticType.fromString(typeStr);
            if (type == null) continue;

            Map<Vector, BlockData> blocks = new HashMap<>();
            List<String> blockDataList = config.getStringList(path + ".blocks");

            for (String data : blockDataList) {
                String[] parts = data.split(",", 4);
                if (parts.length >= 4) {
                    try {
                        int x = Integer.parseInt(parts[0]);
                        int y = Integer.parseInt(parts[1]);
                        int z = Integer.parseInt(parts[2]);
                        Vector vec = new Vector(x, y, z);
                        BlockData bd = Bukkit.createBlockData(parts[3]);
                        blocks.put(vec, bd);
                    } catch (Exception e) {
                        // Skip invalid blocks
                    }
                }
            }

            cosmetics.add(new Cosmetic(id, name, type, blocks));
        }
        return cosmetics;
    }
}