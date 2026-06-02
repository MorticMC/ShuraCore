package dev.shura.core.extra;

import dev.shura.core.ShuraCore;
import dev.shura.core.extra.MessageService;
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
        File file = new File(plugin.getDataFolder(), "utilities.yml");
        if (!file.exists()) plugin.saveResource("utilities.yml", false);
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

    // ── Round Titles ──────────────────────────────────────────────────────────

    public String getRoundWinTitle() {
        return config.getString("sounds.round-win.title", "&#00FFEA🏆&#54FEF0🏆&#7EFEF3🏆&#A8FDF6🏆&#D2FDF9🏆&#FCFCFC🏆&#CAECFD🏆&#65CCFE🏆");
    }

    public String getRoundLossTitle() {
        return config.getString("sounds.round-loss.title", "&#FBFBFB☠&#FBD2D2☠&#FBA0A0☠&#FB8080☠&#FB6161☠&#FB5B4B☠&#FB312A☠&#FB1414☠&#FB0000☠");
    }

    // ── Sounds ────────────────────────────────────────────────────────────────

    public void playSound(Player player, String key) {
        if (player == null) return;
        
        // Check if it's a nested structure with "sounds" key (like round-win/round-loss)
        ConfigurationSection parentSec = config.getConfigurationSection("sounds." + key);
        if (parentSec != null && parentSec.contains("sounds")) {
            List<?> soundsList = parentSec.getList("sounds");
            if (soundsList != null) {
                for (Object entry : soundsList) {
                    if (entry instanceof Map<?, ?> map) {
                        String soundName = (String) map.get("sound");
                        float volume = map.get("volume") instanceof Number n ? n.floatValue() : 1.0f;
                        float pitch = map.get("pitch") instanceof Number n ? n.floatValue() : 1.0f;
                        playSoundRaw(player, soundName, volume, pitch);
                    }
                }
                return;
            }
        }
        
        // Original logic for simple sound entries
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
                plugin.getLogger().warning("Invalid sound in utilities.yml: " + soundName);
            }
        }
    }

    public void playCountdownTick(Player player, int secondsRemaining) {
        if (player == null) return;
        
        if (secondsRemaining == 0) {
            // Fight sound only
            String soundName = config.getString("sounds.countdown-final.sound", "minecraft:block.note_block.flute ambient");
            float volume = (float) config.getDouble("sounds.countdown-final.volume", 2.0);
            float pitch = (float) config.getDouble("sounds.countdown-final.pitch", 0.0);
            playSoundRaw(player, soundName, volume, pitch);
        } else {
            // Sounds for 5,4,3,2,1: play both sounds together
            String soundName1 = config.getString("sounds.countdown-tick.sound", "minecraft:block.note_block.hat ambient");
            float volume1 = (float) config.getDouble("sounds.countdown-tick.volume", 2.0);
            float pitch1 = (float) config.getDouble("sounds.countdown-tick.pitch", 0.6);
            playSoundRaw(player, soundName1, volume1, pitch1);
            
            String soundName2 = config.getString("sounds.countdown-tick-alt.sound", "minecraft:block.note_block.iron_xylophone ambient");
            float volume2 = (float) config.getDouble("sounds.countdown-tick-alt.volume", 2.0);
            float pitch2 = (float) config.getDouble("sounds.countdown-tick-alt.pitch", 1.4);
            playSoundRaw(player, soundName2, volume2, pitch2);
        }
    }
}
