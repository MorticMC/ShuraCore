package dev.shura.core.util;

import dev.shura.core.ShuraCore;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

public class SoundUtil {

    private static ShuraCore plugin;

    public static void init(ShuraCore instance) {
        plugin = instance;
    }

    // Play sound with namespaced key support
    private static void playRaw(Player player, String soundName, float volume, float pitch) {
        if (player == null || soundName == null) return;
        
        if (soundName.contains(":") || soundName.contains(".")) {
            // Parse optional category suffix e.g. "minecraft:block.note_block.hat ambient"
            String[] parts = soundName.trim().split("\\s+", 2);
            String key = parts[0];
            SoundCategory category = SoundCategory.MASTER;
            if (parts.length > 1) {
                try { category = SoundCategory.valueOf(parts[1].toUpperCase()); }
                catch (IllegalArgumentException ignored) {}
            }
            player.playSound(player.getLocation(), key, category, volume, pitch);
        } else {
            try {
                Sound sound = Sound.valueOf(soundName.toUpperCase());
                player.playSound(player.getLocation(), sound, volume, pitch);
            } catch (IllegalArgumentException e) {
                if (plugin != null) {
                    plugin.getLogger().warning("Invalid sound: " + soundName);
                }
            }
        }
    }

    // Countdown sounds for 5,4,3,2,1
    public static void playCountdownTick(Player player) {
        playRaw(player, "minecraft:block.note_block.hat", 2.0f, 0.6f);
        playRaw(player, "minecraft:block.note_block.iron_xylophone", 2.0f, 1.4f);
    }

    // Fight sound (countdown 0)
    public static void playCountdownFinal(Player player) {
        playRaw(player, "minecraft:block.note_block.flute", 2.0f, 0.0f);
    }

    // Round win sounds
    public static void playRoundWin(Player player) {
        playRaw(player, "minecraft:entity.wind_charge.wind_burst", 1.0f, 0.0f);
        playRaw(player, "minecraft:item.firecharge.use", 1.0f, 0.0f);
    }

    // Round loss sounds
    public static void playRoundLoss(Player player) {
        playRaw(player, "minecraft:block.beacon.deactivate", 1.0f, 1.0f);
        playRaw(player, "minecraft:entity.wither_skeleton.death", 1.0f, 1.0f);
    }

    // GUI sounds
    public static void playGuiOpen(Player player) {
        playRaw(player, "ui.loom.take_result", 1.0f, 2.0f);
    }

    public static void playGuiClick(Player player) {
        playRaw(player, "minecraft:block.wooden_door.open", 2.0f, 2.0f);
    }

    public static void playGuiClose(Player player) {
        playRaw(player, "minecraft:item.book.page_turn", 0.6f, 0.8f);
    }

    // Round change sound
    public static void playRoundChange(Player player) {
        playRaw(player, "minecraft:block.note_block.bell", 1.5f, 1.8f);
    }

    // Match sounds
    public static void playMatchWin(Player player) {
        playRaw(player, "minecraft:entity.player.levelup", 1.0f, 1.0f);
    }

    public static void playMatchLoss(Player player) {
        playRaw(player, "minecraft:entity.wither.spawn", 0.3f, 1.5f);
    }

    // Utility sounds
    public static void playError(Player player) {
        playRaw(player, "minecraft:entity.villager.no", 0.8f, 1.0f);
    }

    public static void playSuccess(Player player) {
        playRaw(player, "minecraft:entity.experience_orb.pickup", 1.0f, 1.5f);
    }

    // Match found sounds
    public static void playMatchFound(Player player) {
        playRaw(player, "minecraft:block.note_block.pling", 1.0f, 2.0f);
        playRaw(player, "minecraft:block.note_block.pling", 1.0f, 1.8f);
        playRaw(player, "minecraft:block.note_block.pling", 1.0f, 2.0f);
    }

    // Party sounds
    public static void playPartyCreate(Player player) {
        playRaw(player, "ui.loom.take_result", 1.0f, 1.5f);
        playRaw(player, "item.goat_horn.sound.0", 1.0f, 1.5f);
    }

    public static void playPartyInvite(Player player) {
        playRaw(player, "minecraft:entity.experience_orb.pickup", 0.8f, 1.2f);
    }

    // Duel sounds
    public static void playDuelRequest(Player player) {
        playRaw(player, "minecraft:entity.experience_orb.pickup", 1.0f, 1.0f);
    }

    public static void playDuelAccept(Player player) {
        playRaw(player, "minecraft:entity.player.levelup", 0.5f, 1.5f);
    }

    // Command blocked sound
    public static void playCommandBlocked(Player player) {
        playRaw(player, "minecraft:block.note_block.didgeridoo", 1.0f, 0.0f);
    }
}
