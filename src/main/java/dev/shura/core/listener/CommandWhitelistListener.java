package dev.shura.core.listener;

import dev.shura.core.ShuraCore;
import dev.shura.core.util.SoundUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.TabCompleteEvent;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CommandWhitelistListener implements Listener {

    private final ShuraCore plugin;

    public CommandWhitelistListener(ShuraCore plugin) {
        this.plugin = plugin;
    }

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
            // Still typing the command name — only show whitelisted commands that match
            String partial = buffer.substring(1).toLowerCase();
            Set<String> allowed = plugin.getWhitelistManager().getAllowedCommandsForPlayer(player);
            List<String> filtered = allowed.stream()
                    .filter(cmd -> cmd.startsWith(partial))
                    .sorted()
                    .collect(Collectors.toList());
            event.setCompletions(filtered);
        } else {
            // Typed a full command — clear suggestions if it's not whitelisted
            if (!plugin.getWhitelistManager().isAllowed(player, typed)) {
            event.setCompletions(Collections.emptyList());
            }
        }
    }
}
