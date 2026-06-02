package dev.shura.core.listener;

import dev.shura.core.ShuraCore;
import dev.shura.core.party.Party;
import dev.shura.core.profile.Profile;
import dev.shura.core.util.SoundUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.server.TabCompleteEvent;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MainListener implements Listener {

    private final ShuraCore plugin;

    public MainListener(ShuraCore plugin) {
        this.plugin = plugin;
    }

    // ==================== CHAT ====================

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        event.setCancelled(true);

        Profile profile = plugin.getProfileManager().getProfile(player);
        if (profile != null && profile.isStaffChat()) {
            Component formatted = Component.text("[Staff] ", NamedTextColor.RED)
                    .append(Component.text(player.getName(), NamedTextColor.GRAY))
                    .append(Component.text(": ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(message, NamedTextColor.WHITE));
            plugin.getServer().getOnlinePlayers().stream()
                    .filter(p -> p.hasPermission("shura.command.staffchat"))
                    .forEach(p -> p.sendMessage(formatted));
            plugin.getServer().getConsoleSender().sendMessage(formatted);
            return;
        }

        if (message.startsWith("!") && plugin.getPartyManager().isInParty(player.getUniqueId())) {
            Party party = plugin.getPartyManager().getParty(player.getUniqueId());
            if (party != null) {
                party.broadcast(Component.text("[Party] ", NamedTextColor.DARK_GREEN)
                        .append(Component.text(player.getName(), NamedTextColor.GREEN))
                        .append(Component.text(": ", NamedTextColor.DARK_GRAY))
                        .append(Component.text(message.substring(1).trim(), NamedTextColor.WHITE)));
            }
            return;
        }

        plugin.getServer().getScheduler().runTask(plugin,
                () -> plugin.getChatManager().handleChat(player, message));
    }

    // ==================== COMMAND WHITELIST ====================

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (player.isOp()) return;

        String command = event.getMessage().split(" ")[0].substring(1).toLowerCase();

        if (!plugin.getWhitelistManager().isAllowed(player, command)) {
            event.setCancelled(true);
            player.sendMessage(plugin.getMessageService().get("whitelist.command-blocked"));
            SoundUtil.playCommandBlocked(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTabComplete(TabCompleteEvent event) {
        if (!(event.getSender() instanceof Player player)) return;
        if (player.isOp()) return;

        String buffer = event.getBuffer();
        if (!buffer.startsWith("/")) return;

        String typed = buffer.substring(1).split(" ")[0].toLowerCase();

        if (!buffer.contains(" ")) {
            String partial = buffer.substring(1).toLowerCase();
            Set<String> allowed = plugin.getWhitelistManager().getAllowedCommandsForPlayer(player);
            List<String> filtered = allowed.stream()
                    .filter(cmd -> cmd.startsWith(partial))
                    .sorted()
                    .collect(Collectors.toList());
            event.setCompletions(filtered);
        } else {
            if (!plugin.getWhitelistManager().isAllowed(player, typed)) {
                event.setCompletions(Collections.emptyList());
            }
        }
    }

    // ==================== LOBBY PROTECTION ====================

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("shura.admin")) return;
        if (plugin.getMatchManager().isInMatch(player.getUniqueId())) return;
        if (plugin.getInteractionManager().isUnrestricted(player.getUniqueId())) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("shura.admin")) return;
        if (plugin.getMatchManager().isInMatch(player.getUniqueId())) return;
        if (plugin.getInteractionManager().isUnrestricted(player.getUniqueId())) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("shura.admin")) return;
        if (plugin.getMatchManager().isInMatch(player.getUniqueId())) return;
        if (plugin.getInteractionManager().isUnrestricted(player.getUniqueId())) return;
        event.setCancelled(true);
    }
}
