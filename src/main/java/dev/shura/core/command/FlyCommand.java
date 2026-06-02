package dev.shura.core.command;

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

public class FlyCommand implements CommandExecutor {

    private final ShuraCore plugin;

    public FlyCommand(ShuraCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Players only.", NamedTextColor.RED));
            return true;
        }
        if (!player.hasPermission("shura.command.fly")) {
            plugin.getMessageService().send(player, "errors.no-permission");
            return true;
        }

        Player target = args.length > 0 ? Bukkit.getPlayer(args[0]) : player;
        if (target == null) {
            plugin.getMessageService().send(player, "errors.player-not-found", Map.of("target", args[0]));
            return true;
        }

        boolean flying = !target.getAllowFlight();
        target.setAllowFlight(flying);
        if (!flying) target.setFlying(false);

        Component status = flying
                ? Component.text("enabled", NamedTextColor.GREEN)
                : Component.text("disabled", NamedTextColor.RED);

        target.sendMessage(Component.text("Fly ", NamedTextColor.GRAY).append(status));
        if (!target.equals(player))
            player.sendMessage(Component.text("Fly ", NamedTextColor.GRAY).append(status)
                    .append(Component.text(" for " + target.getName(), NamedTextColor.GRAY)));

        SoundUtil.playSuccess(target);
        return true;
    }
}
