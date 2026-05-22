package dev.shura.core.chat;

import dev.shura.core.ShuraCore;
import dev.shura.core.match.Match;
import dev.shura.core.message.MessageService;
import dev.shura.core.practice.PracticeGamemode;
import dev.shura.core.profile.Profile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.luckperms.api.model.user.User;
import net.luckperms.api.cacheddata.CachedMetaData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ChatManager {

    private final ShuraCore plugin;

    public ChatManager(ShuraCore plugin) {
        this.plugin = plugin;
    }

    // Returns true if the event should be cancelled (we handled it)
    public boolean handleChat(Player player, String message) {
        UUID uuid = player.getUniqueId();
        Profile profile = plugin.getProfileManager().getProfile(player);

        // Staff chat toggle — handled by ChatListener already, skip here
        if (profile != null && profile.isStaffChat()) return false;

        // Party chat prefix '!'
        if (message.startsWith("!") && plugin.getPartyManager().isInParty(uuid)) return false;

        // In match (player or spectator)
        if (plugin.getMatchManager().isInMatch(uuid)) {
            sendMatchChat(player, message, false);
            return true;
        }

        if (plugin.getSpectatorManager().isSpectating(uuid)) {
            sendMatchChat(player, message, true);
            return true;
        }

        // In practice FFA
        if (plugin.getPracticeManager().isInPractice(uuid)) {
            sendPracticeChat(player, message);
            return true;
        }

        // Lobby (includes queue — queue players use lobby chat)
        sendLobbyChat(player, message);
        return true;
    }

    // ── MATCH CHAT ───────────────────────────────────────────────────────────
    private void sendMatchChat(Player sender, String message, boolean isSpectator) {
        Match match = isSpectator
                ? plugin.getSpectatorManager().getSpectatedMatch(sender.getUniqueId())
                : plugin.getMatchManager().getMatchByPlayer(sender.getUniqueId());

        if (match == null) return;

        Component prefix = isSpectator
                ? Component.text("[Spectator] ", NamedTextColor.GRAY, TextDecoration.ITALIC)
                : Component.text("[Match] ", NamedTextColor.AQUA);

        Component formatted = prefix
                .append(Component.text(sender.getName(), NamedTextColor.WHITE))
                .append(Component.text(": ", NamedTextColor.DARK_GRAY))
                .append(Component.text(message, NamedTextColor.GRAY));

        // Send to both match players
        sendToPlayer(match.getMatchPlayerA().getUuid(), formatted);
        sendToPlayer(match.getMatchPlayerB().getUuid(), formatted);

        // Send to all spectators of this match
        match.getSpectators().forEach(uuid -> sendToPlayer(uuid, formatted));
    }

    // ── PRACTICE CHAT ────────────────────────────────────────────────────────
    private void sendPracticeChat(Player sender, String message) {
        PracticeGamemode gm = plugin.getPracticeManager().getGamemode(sender.getUniqueId());
        if (gm == null) return;

        Component formatted = Component.text("[" + gm.getDisplayName() + "] ", NamedTextColor.GREEN)
                .append(Component.text(sender.getName(), NamedTextColor.WHITE))
                .append(Component.text(": ", NamedTextColor.DARK_GRAY))
                .append(Component.text(message, NamedTextColor.GRAY));

        // Only players in the same FFA gamemode see this
        for (Player online : Bukkit.getOnlinePlayers()) {
            PracticeGamemode theirGm = plugin.getPracticeManager().getGamemode(online.getUniqueId());
            if (gm.equals(theirGm)) {
                online.sendMessage(formatted);
            }
        }
    }

    // ── LOBBY CHAT ───────────────────────────────────────────────────────────
    private void sendLobbyChat(Player sender, String message) {
        String prefix = getLuckPermsPrefix(sender);
        String suffix = getLuckPermsSuffix(sender);

        Component formatted = MessageService.colorizeComponent(prefix)
                .append(Component.text(sender.getName(), NamedTextColor.WHITE))
                .append(MessageService.colorizeComponent(suffix))
                .append(Component.text(": ", NamedTextColor.DARK_GRAY))
                .append(Component.text(message, NamedTextColor.WHITE));

        // Send to all lobby players (not in match, not spectating, not in practice)
        for (Player online : Bukkit.getOnlinePlayers()) {
            UUID uuid = online.getUniqueId();
            if (!plugin.getMatchManager().isInMatch(uuid)
                    && !plugin.getSpectatorManager().isSpectating(uuid)
                    && !plugin.getPracticeManager().isInPractice(uuid)) {
                online.sendMessage(formatted);
            }
        }

        // Also log to console
        Bukkit.getConsoleSender().sendMessage(formatted);
    }

    // ── LUCKPERMS ────────────────────────────────────────────────────────────
    private String getLuckPermsPrefix(Player player) {
        User user = plugin.getLuckPerms().getUserManager().getUser(player.getUniqueId());
        if (user == null) return "";
        CachedMetaData meta = user.getCachedData().getMetaData();
        String prefix = meta.getPrefix();
        return prefix != null ? prefix + " " : "";
    }

    private String getLuckPermsSuffix(Player player) {
        User user = plugin.getLuckPerms().getUserManager().getUser(player.getUniqueId());
        if (user == null) return "";
        CachedMetaData meta = user.getCachedData().getMetaData();
        String suffix = meta.getSuffix();
        return suffix != null ? " " + suffix : "";
    }

    private void sendToPlayer(UUID uuid, Component message) {
        Player p = Bukkit.getPlayer(uuid);
        if (p != null) p.sendMessage(message);
    }
}
