package dev.shura.core.command.util;

import dev.shura.core.ShuraCore;
import dev.shura.core.util.SoundUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MessageCommand implements CommandExecutor {

    private final ShuraCore plugin;
    // playerUUID -> last messaged UUID
    private final Map<UUID, UUID> replyMap = new ConcurrentHashMap<>();

    public MessageCommand(ShuraCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Players only.", NamedTextColor.RED));
            return true;
        }
        if (!player.hasPermission("shura.command.msg")) {
            plugin.getMessageService().send(player, "errors.no-permission");
            return true;
        }

        // /r <message>
        if (label.equalsIgnoreCase("r")) {
            if (args.length == 0) {
                plugin.getMessageService().send(player, "errors.invalid-args", Map.of("usage", "/r <message>"));
                return true;
            }
            UUID replyTo = replyMap.get(player.getUniqueId());
            if (replyTo == null) {
                player.sendMessage(Component.text("No one to reply to.", NamedTextColor.RED));
                return true;
            }
            Player target = Bukkit.getPlayer(replyTo);
            if (target == null) {
                player.sendMessage(Component.text("That player is no longer online.", NamedTextColor.RED));
                return true;
            }
            sendMessage(player, target, String.join(" ", args));
            return true;
        }

        // /msg <player> <message>
        if (args.length < 2) {
            plugin.getMessageService().send(player, "errors.invalid-args", Map.of("usage", "/msg <player> <message>"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            plugin.getMessageService().send(player, "errors.player-not-found", Map.of("target", args[0]));
            return true;
        }

        String message = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        sendMessage(player, target, message);
        return true;
    }

    private void sendMessage(Player from, Player to, String message) {
        Component outgoing = Component.text("→ ", NamedTextColor.GRAY)
                .append(Component.text(to.getName(), NamedTextColor.AQUA))
                .append(Component.text(": ", NamedTextColor.DARK_GRAY))
                .append(Component.text(message, NamedTextColor.WHITE));

        Component incoming = Component.text("← ", NamedTextColor.GRAY)
                .append(Component.text(from.getName(), NamedTextColor.AQUA))
                .append(Component.text(": ", NamedTextColor.DARK_GRAY))
                .append(Component.text(message, NamedTextColor.WHITE));

        from.sendMessage(outgoing);
        to.sendMessage(incoming);

        replyMap.put(from.getUniqueId(), to.getUniqueId());
        replyMap.put(to.getUniqueId(), from.getUniqueId());

        SoundUtil.playDuelRequest(to);
    }

    public void clearReply(UUID uuid) {
        replyMap.remove(uuid);
    }
}
