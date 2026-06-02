package dev.shura.core.extra;

import dev.shura.core.ShuraCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class TabManager {

    private final ShuraCore plugin;

    public TabManager(ShuraCore plugin) {
        this.plugin = plugin;
    }

    // Call whenever a player's state changes (enters/leaves match, queue, spectate)
    public void update(Player changed) {
        boolean shouldHide = shouldHide(changed);

        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(changed)) continue;

            if (shouldHide) {
                // Hide 'changed' from everyone not in the same context
                if (!shareContext(changed, other)) {
                    other.hidePlayer(plugin, changed);
                } else {
                    other.showPlayer(plugin, changed);
                }
            } else {
                // 'changed' is back in lobby — show to all non-hidden players
                if (!shouldHide(other)) {
                    other.showPlayer(plugin, changed);
                    changed.showPlayer(plugin, other);
                }
            }
        }

        // Also update what 'changed' sees
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(changed)) continue;
            if (shouldHide(other) && !shareContext(changed, other)) {
                changed.hidePlayer(plugin, other);
            } else if (!shouldHide(other)) {
                changed.showPlayer(plugin, other);
            }
        }
    }

    // Refresh all players — called on join
    public void updateAll(Player joining) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(joining)) continue;
            if (shouldHide(online)) {
                joining.hidePlayer(plugin, online);
            } else {
                joining.showPlayer(plugin, online);
            }
        }
        update(joining);
    }

    // A player should be hidden from the main tablist if in match or spectating
    // Players in queue should remain visible in lobby
    private boolean shouldHide(Player player) {
        UUID uuid = player.getUniqueId();
        return plugin.getMatchManager().isInMatch(uuid)
                || plugin.getSpectatorManager().isSpectating(uuid);
    }

    // Two players share context if they are in the same match (including spectators)
    private boolean shareContext(Player a, Player b) {
        UUID uuidA = a.getUniqueId();
        UUID uuidB = b.getUniqueId();

        // Both in same match
        var matchA = plugin.getMatchManager().getMatchByPlayer(uuidA);
        var matchB = plugin.getMatchManager().getMatchByPlayer(uuidB);
        if (matchA != null && matchA.equals(matchB)) return true;

        // A is spectating B's match or vice versa
        var specMatchA = plugin.getSpectatorManager().getSpectatedMatch(uuidA);
        var specMatchB = plugin.getSpectatorManager().getSpectatedMatch(uuidB);

        if (specMatchA != null && (specMatchA.hasPlayer(uuidB) || specMatchA.equals(specMatchB))) return true;
        if (specMatchB != null && (specMatchB.hasPlayer(uuidA) || specMatchB.equals(specMatchA))) return true;

        // A is in match, B is spectating that match
        if (matchA != null && specMatchB != null && specMatchB.equals(matchA)) return true;
        if (matchB != null && specMatchA != null && specMatchA.equals(matchB)) return true;

        return false;
    }
}
