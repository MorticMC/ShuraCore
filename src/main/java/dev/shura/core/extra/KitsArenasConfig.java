package dev.shura.core.extra;

import dev.shura.core.ShuraCore;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class KitsArenasConfig {

    private final ShuraCore plugin;
    private File file;
    private FileConfiguration config;

    public KitsArenasConfig(ShuraCore plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        file = new File(plugin.getDataFolder(), "kits.yml");
        if (!file.exists()) {
            plugin.saveResource("kits.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save kits.yml: " + e.getMessage());
        }
    }

    public void reload() {
        load();
    }

    // Get all tierlist IDs
    public Set<String> getTierlistIds() {
        ConfigurationSection tierlistsSection = config.getConfigurationSection("tierlists");
        return tierlistsSection != null ? tierlistsSection.getKeys(false) : new HashSet<>();
    }

    // Get tierlist short ID
    public String getTierlistId(String tierlistKey) {
        return config.getString("tierlists." + tierlistKey + ".id", tierlistKey.toUpperCase());
    }

    // Get all gamemode keys for a tierlist
    public Set<String> getGamemodeKeys(String tierlistKey) {
        ConfigurationSection gamemodesSection = config.getConfigurationSection("tierlists." + tierlistKey + ".gamemodes");
        return gamemodesSection != null ? gamemodesSection.getKeys(false) : new HashSet<>();
    }

    // Get gamemode name
    public String getGamemodeName(String tierlistKey, String gamemodeKey) {
        return config.getString("tierlists." + tierlistKey + ".gamemodes." + gamemodeKey + ".name", gamemodeKey);
    }

    // Get gamemode material
    public Material getGamemodeMaterial(String tierlistKey, String gamemodeKey) {
        String materialStr = config.getString("tierlists." + tierlistKey + ".gamemodes." + gamemodeKey + ".material", "PAPER");
        try {
            return Material.valueOf(materialStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Material.PAPER;
        }
    }

    // Get gamemode full-id
    public String getGamemodeFullId(String tierlistKey, String gamemodeKey) {
        return config.getString("tierlists." + tierlistKey + ".gamemodes." + gamemodeKey + ".full-id", gamemodeKey);
    }

    // Get gamemode slot
    public int getGamemodeSlot(String tierlistKey, String gamemodeKey) {
        return config.getInt("tierlists." + tierlistKey + ".gamemodes." + gamemodeKey + ".slot", 10);
    }

    // Get gamemode queue type
    public String getGamemodeQueueType(String tierlistKey, String gamemodeKey) {
        return config.getString("tierlists." + tierlistKey + ".gamemodes." + gamemodeKey + ".queue-type", "both");
    }

    // Get all ranked gamemodes for a tierlist
    public List<String> getRankedGamemodes(String tierlistKey) {
        List<String> ranked = new ArrayList<>();
        for (String gamemodeKey : getGamemodeKeys(tierlistKey)) {
            String queueType = getGamemodeQueueType(tierlistKey, gamemodeKey);
            if ("ranked".equalsIgnoreCase(queueType) || "both".equalsIgnoreCase(queueType)) {
                ranked.add(gamemodeKey);
            }
        }
        return ranked;
    }

    // Get all unranked gamemodes for a tierlist
    public List<String> getUnrankedGamemodes(String tierlistKey) {
        List<String> unranked = new ArrayList<>();
        for (String gamemodeKey : getGamemodeKeys(tierlistKey)) {
            String queueType = getGamemodeQueueType(tierlistKey, gamemodeKey);
            if ("unranked".equalsIgnoreCase(queueType) || "both".equalsIgnoreCase(queueType)) {
                unranked.add(gamemodeKey);
            }
        }
        return unranked;
    }

    // Set/update a gamemode
    public void setGamemode(String tierlistKey, String gamemodeKey, String name, Material material, String fullId, int slot, String queueType) {
        String path = "tierlists." + tierlistKey + ".gamemodes." + gamemodeKey;
        config.set(path + ".name", name);
        config.set(path + ".material", material.name());
        config.set(path + ".full-id", fullId);
        config.set(path + ".slot", slot);
        config.set(path + ".queue-type", queueType);
        save();
    }

    // Remove a gamemode
    public void removeGamemode(String tierlistKey, String gamemodeKey) {
        config.set("tierlists." + tierlistKey + ".gamemodes." + gamemodeKey, null);
        save();
    }

    // Check if gamemode exists
    public boolean hasGamemode(String tierlistKey, String gamemodeKey) {
        return config.contains("tierlists." + tierlistKey + ".gamemodes." + gamemodeKey);
    }

    // Get all gamemodes for a tierlist as map
    public Map<String, Map<String, Object>> getAllGamemodes(String tierlistKey) {
        Map<String, Map<String, Object>> gamemodes = new HashMap<>();
        for (String gamemodeKey : getGamemodeKeys(tierlistKey)) {
            Map<String, Object> data = new HashMap<>();
            data.put("name", getGamemodeName(tierlistKey, gamemodeKey));
            data.put("material", getGamemodeMaterial(tierlistKey, gamemodeKey));
            data.put("full-id", getGamemodeFullId(tierlistKey, gamemodeKey));
            data.put("slot", getGamemodeSlot(tierlistKey, gamemodeKey));
            data.put("queue-type", getGamemodeQueueType(tierlistKey, gamemodeKey));
            gamemodes.put(gamemodeKey, data);
        }
        return gamemodes;
    }
}
