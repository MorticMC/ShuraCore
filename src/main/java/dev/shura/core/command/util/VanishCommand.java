package dev.shura.core.command.util;

import dev.shura.core.ShuraCore;
import dev.shura.core.profile.Profile;
import dev.shura.core.util.SoundUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class VanishCommand implements CommandExecutor {

    private final ShuraCore plugin;

    public VanishCommand(ShuraCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Players only.", NamedTextColor.RED));
            return true;
        }
        if (!player.hasPermission("shura.command.vanish")) {
            plugin.getMessageService().send(player, "errors.no-permission");
            return true;
        }

        Player target = args.length > 0 ? Bukkit.getPlayer(args[0]) : player;
        if (target == null) {
            plugin.getMessageService().send(player, "errors.player-not-found", Map.of("target", args[0]));
            return true;
        }

        Profile profile = plugin.getProfileManager().getProfile(target);
        if (profile == null) return true;

        boolean vanish = !profile.isVanished();
        profile.setVanished(vanish);

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(target)) continue;
            if (vanish && !online.hasPermission("shura.command.vanish")) {
                online.hidePlayer(plugin, target);
            } else {
                online.showPlayer(plugin, target);
            }
        }

        Component status = vanish
                ? Component.text("You are now vanished.", NamedTextColor.GRAY)
                : Component.text("You are no longer vanished.", NamedTextColor.GRAY);

        target.sendMessage(status);
        if (!target.equals(player))
            player.sendMessage(Component.text("Vanish ", NamedTextColor.GRAY)
                    .append(vanish ? Component.text("enabled", NamedTextColor.GREEN) : Component.text("disabled", NamedTextColor.RED))
                    .append(Component.text(" for " + target.getName(), NamedTextColor.GRAY)));

        SoundUtil.playSuccess(target);
        return true;
    }
}
