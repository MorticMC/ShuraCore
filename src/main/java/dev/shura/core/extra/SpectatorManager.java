package dev.shura.core.extra;

import dev.shura.core.ShuraCore;
import dev.shura.core.match.Match;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SpectatorManager {

    private final ShuraCore plugin;
    private final Map<UUID, UUID> spectatorMatchMap = new ConcurrentHashMap<>(); // spectatorUUID -> matchId

    public SpectatorManager(ShuraCore plugin) {
        this.plugin = plugin;
    }

    public boolean addSpectator(Player spectator, Player target) {
        Match match = plugin.getMatchManager().getMatchByPlayer(target.getUniqueId());
        if (match == null) {
            spectator.sendMessage(Component.text(target.getName() + " is not in a match.", NamedTextColor.RED));
            return false;
        }
        if (plugin.getMatchManager().isInMatch(spectator.getUniqueId())) {
            spectator.sendMessage(Component.text("You cannot spectate while in a match.", NamedTextColor.RED));
            return false;
        }

        spectatorMatchMap.put(spectator.getUniqueId(), match.getMatchId());
        match.addSpectator(spectator.getUniqueId());

        spectator.setGameMode(GameMode.SPECTATOR);
        spectator.teleport(match.getArenaCopy().getSpawnA());
        spectator.sendMessage(Component.text("Now spectating ", NamedTextColor.GRAY)
                .append(Component.text(match.getMatchPlayerA().getName(), NamedTextColor.AQUA))
                .append(Component.text(" vs ", NamedTextColor.GRAY))
                .append(Component.text(match.getMatchPlayerB().getName(), NamedTextColor.AQUA)));

        // Update tablist — spectator hidden from lobby
        plugin.getTabManager().update(spectator);

        // Hide spectator from match players
        for (UUID memberUuid : new UUID[]{match.getMatchPlayerA().getUuid(), match.getMatchPlayerB().getUuid()}) {
            Player p = Bukkit.getPlayer(memberUuid);
            if (p != null) p.hidePlayer(plugin, spectator);
        }
        return true;
    }

    public boolean spectateMatch(Player spectator, UUID matchId) {
        Match match = plugin.getMatchManager().getMatch(matchId);
        if (match == null) {
            spectator.sendMessage(Component.text("This match is no longer active.", NamedTextColor.RED));
            return false;
        }
        if (plugin.getMatchManager().isInMatch(spectator.getUniqueId())) {
            spectator.sendMessage(Component.text("You cannot spectate while in a match.", NamedTextColor.RED));
            return false;
        }

        spectatorMatchMap.put(spectator.getUniqueId(), matchId);
        match.addSpectator(spectator.getUniqueId());

        spectator.setGameMode(GameMode.SPECTATOR);
        spectator.teleport(match.getArenaCopy().getSpawnA());
        spectator.sendMessage(Component.text("Now spectating ", NamedTextColor.GRAY)
                .append(Component.text(match.getMatchPlayerA().getName(), NamedTextColor.AQUA))
                .append(Component.text(" vs ", NamedTextColor.GRAY))
                .append(Component.text(match.getMatchPlayerB().getName(), NamedTextColor.AQUA)));

        // Update tablist — spectator hidden from lobby
        plugin.getTabManager().update(spectator);

        // Hide spectator from match players
        for (UUID memberUuid : new UUID[]{match.getMatchPlayerA().getUuid(), match.getMatchPlayerB().getUuid()}) {
            Player p = Bukkit.getPlayer(memberUuid);
            if (p != null) p.hidePlayer(plugin, spectator);
        }
        return true;
    }

    public void removeSpectator(Player spectator) {
        UUID matchId = spectatorMatchMap.remove(spectator.getUniqueId());
        if (matchId != null) {
            Match match = plugin.getMatchManager().getMatch(matchId);
            if (match != null) {
                match.removeSpectator(spectator.getUniqueId());
                // Unhide spectator from match players
                for (UUID memberUuid : new UUID[]{match.getMatchPlayerA().getUuid(), match.getMatchPlayerB().getUuid()}) {
                    Player p = Bukkit.getPlayer(memberUuid);
                    if (p != null) p.showPlayer(plugin, spectator);
                }
            }
        }

        // Restore to lobby
        spectator.setGameMode(GameMode.SURVIVAL);
        String spawnStr = plugin.getConfig().getString("lobby.spawn", "0,64,0,0,0");
        String world = plugin.getConfig().getString("lobby.world", "world");
        try {
            spectator.teleport(dev.shura.core.util.LocationUtil.deserialize(world + "," + spawnStr));
        } catch (Exception ignored) {}

        // Update tablist — spectator back in lobby
        plugin.getTabManager().update(spectator);
    }

    public boolean isSpectating(UUID uuid) { return spectatorMatchMap.containsKey(uuid); }

    public Match getSpectatedMatch(UUID uuid) {
        UUID matchId = spectatorMatchMap.get(uuid);
        return matchId != null ? plugin.getMatchManager().getMatch(matchId) : null;
    }
}
