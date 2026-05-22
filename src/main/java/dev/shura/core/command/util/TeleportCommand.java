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

public class TeleportCommand implements CommandExecutor {

    private final ShuraCore plugin;

    public TeleportCommand(ShuraCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Players only.", NamedTextColor.RED));
            return true;
        }
        if (!player.hasPermission("shura.command.tp")) {
            plugin.getMessageService().send(player, "errors.no-permission");
            return true;
        }
        if (args.length == 0) {
            plugin.getMessageService().send(player, "errors.invalid-args", Map.of("usage", "/tp <player> [target]"));
            return true;
        }

        // /tp <target> — teleport self to target
        if (args.length == 1) {
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                plugin.getMessageService().send(player, "errors.player-not-found", Map.of("target", args[0]));
                return true;
            }
            player.teleport(target.getLocation());
            player.sendMessage(Component.text("Teleported to ", NamedTextColor.GRAY)
                    .append(Component.text(target.getName(), NamedTextColor.AQUA)));
            SoundUtil.playTeleport(player);
            return true;
        }

        // /tp <player> <target> — teleport player to target
        Player from = Bukkit.getPlayer(args[0]);
        Player to = Bukkit.getPlayer(args[1]);

        if (from == null) {
            plugin.getMessageService().send(player, "errors.player-not-found", Map.of("target", args[0]));
            return true;
        }
        if (to == null) {
            plugin.getMessageService().send(player, "errors.player-not-found", Map.of("target", args[1]));
            return true;
        }

        from.teleport(to.getLocation());
        from.sendMessage(Component.text("You were teleported to ", NamedTextColor.GRAY)
                .append(Component.text(to.getName(), NamedTextColor.AQUA)));
        player.sendMessage(Component.text("Teleported ", NamedTextColor.GRAY)
                .append(Component.text(from.getName(), NamedTextColor.AQUA))
                .append(Component.text(" to ", NamedTextColor.GRAY))
                .append(Component.text(to.getName(), NamedTextColor.AQUA)));
        SoundUtil.playTeleport(from);
        return true;
    }
}
