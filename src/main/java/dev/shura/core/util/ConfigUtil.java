package dev.shura.core.util;

import org.bukkit.configuration.file.FileConfiguration;

public class ConfigUtil {

    private final FileConfiguration config;

    public ConfigUtil(FileConfiguration config) {
        this.config = config;
    }

    public String getString(String path, String def) {
        return config.getString(path, def);
    }

    public int getInt(String path, int def) {
        return config.getInt(path, def);
    }

    public boolean getBoolean(String path, boolean def) {
        return config.getBoolean(path, def);
    }

    public double getDouble(String path, double def) {
        return config.getDouble(path, def);
    }

    public long getLong(String path, long def) {
        return config.getLong(path, def);
    }

    public boolean contains(String path) {
        return config.contains(path);
    }
}
