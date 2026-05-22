package dev.shura.core.command.util;

import dev.shura.core.ShuraCore;
import dev.shura.core.listener.PlayerListener;
import dev.shura.core.util.SoundUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BackCommand implements CommandExecutor {

    private final ShuraCore plugin;

    public BackCommand(ShuraCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Players only.", NamedTextColor.RED));
            return true;
        }
        if (!player.hasPermission("shura.command.back")) {
            plugin.getMessageService().send(player, "errors.no-permission");
            return true;
        }

        PlayerListener listener = plugin.getPlayerListener();
        if (listener == null) {
            player.sendMessage(Component.text("No previous location found.", NamedTextColor.RED));
            return true;
        }

        Location back = listener.getBackLocation(player.getUniqueId());
        if (back == null) {
            player.sendMessage(Component.text("No previous location found.", NamedTextColor.RED));
            return true;
        }

        Location current = player.getLocation().clone();
        player.teleport(back);
        listener.setBackLocation(player.getUniqueId(), current);
        player.sendMessage(Component.text("Teleported to your previous location.", NamedTextColor.GREEN));
        SoundUtil.playTeleport(player);
        return true;
    }
}
