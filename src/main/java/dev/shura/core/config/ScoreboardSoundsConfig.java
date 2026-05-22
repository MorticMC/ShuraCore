package dev.shura.core.config;

import dev.shura.core.ShuraCore;
import dev.shura.core.message.MessageService;
import net.kyori.adventure.text.Component;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.List;
import java.util.Map;

public class ScoreboardSoundsConfig {

    private final ShuraCore plugin;
    private FileConfiguration config;

    public ScoreboardSoundsConfig(ShuraCore plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "scoreboards-sounds.yml");
        if (!file.exists()) plugin.saveResource("scoreboards-sounds.yml", false);
        config = YamlConfiguration.loadConfiguration(file);
    }

    // ── Scoreboard ────────────────────────────────────────────────────────────

    public boolean isBoardEnabled(String board) {
        return config.getBoolean("scoreboards." + board + ".enabled", true);
    }

    public Component boardTitle(String board) {
        return MessageService.colorizeComponent(
                config.getString("scoreboards." + board + ".title", "&bShuraPvP"));
    }

    public List<String> boardLines(String board) {
        return config.getStringList("scoreboards." + board + ".lines");
    }

    public String serverName() {
        return config.getString("server-name", "play.shura.dev");
    }

    // ── Sounds ────────────────────────────────────────────────────────────────

    public void playSound(Player player, String key) {
        if (player == null) return;
        ConfigurationSection sec = config.getConfigurationSection("sounds." + key);
        if (sec != null) {
            playSingle(player, sec);
            return;
        }
        List<?> list = config.getList("sounds." + key);
        if (list == null) return;
        for (Object entry : list) {
            if (entry instanceof ConfigurationSection cs) {
                playSingle(player, cs);
            } else if (entry instanceof Map<?, ?> map) {
                String soundName = (String) map.get("sound");
                float volume = map.get("volume") instanceof Number n ? n.floatValue() : 1.0f;
                float pitch  = map.get("pitch")  instanceof Number n ? n.floatValue() : 1.0f;
                playSoundRaw(player, soundName, volume, pitch);
            }
        }
    }

    private void playSingle(Player player, ConfigurationSection sec) {
        playSoundRaw(player, sec.getString("sound"),
                (float) sec.getDouble("volume", 1.0),
                (float) sec.getDouble("pitch", 1.0));
    }

    private void playSoundRaw(Player player, String soundName, float volume, float pitch) {
        if (soundName == null) return;
        // Support namespaced keys like "minecraft:block.note_block.didgeridoo"
        // as well as Bukkit enum names like "BLOCK_NOTE_BLOCK_PLING"
        if (soundName.contains(":") || soundName.contains(".")) {
            // Parse optional category suffix e.g. "minecraft:block.note_block.didgeridoo ambient"
            String[] parts = soundName.trim().split("\\s+", 2);
            String key = parts[0];
            org.bukkit.SoundCategory category = org.bukkit.SoundCategory.MASTER;
            if (parts.length > 1) {
                try { category = org.bukkit.SoundCategory.valueOf(parts[1].toUpperCase()); }
                catch (IllegalArgumentException ignored) {}
            }
            player.playSound(player.getLocation(), key, category, volume, pitch);
        } else {
            try {
                Sound sound = Sound.valueOf(soundName.toUpperCase());
                player.playSound(player.getLocation(), sound, volume, pitch);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid sound in scoreboards-sounds.yml: " + soundName);
            }
        }
    }

    public void playCountdownTick(Player player, int secondsRemaining) {
        if (player == null) return;
        String soundName = config.getString("sounds.countdown-tick.sound", "BLOCK_NOTE_BLOCK_PLING");
        float volume = (float) config.getDouble("sounds.countdown-tick.volume", 1.0);
        float pitch = secondsRemaining == 1 ? 2.0f : 0.5f + (0.1f * (5 - secondsRemaining));
        playSoundRaw(player, soundName, volume, pitch);
    }
}
