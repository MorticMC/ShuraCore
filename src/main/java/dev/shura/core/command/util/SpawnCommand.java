package dev.shura.core.command.util;

import dev.shura.core.ShuraCore;
import dev.shura.core.util.LocationUtil;
import dev.shura.core.util.SoundUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpawnCommand implements CommandExecutor {

    private final ShuraCore plugin;
    private final boolean isSetter;

    public SpawnCommand(ShuraCore plugin, boolean isSetter) {
        this.plugin = plugin;
        this.isSetter = isSetter;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Players only.", NamedTextColor.RED));
            return true;
        }

        if (isSetter) {
            if (!player.hasPermission("shura.admin")) {
                plugin.getMessageService().send(player, "errors.no-permission");
                return true;
            }
            Location loc = player.getLocation();
            String world = loc.getWorld().getName();
            plugin.getConfig().set("lobby.world", world);
            plugin.getConfig().set("lobby.spawn", String.format("%.2f,%.2f,%.2f,%.2f,%.2f",
                    loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch()));
            plugin.saveConfig();
            player.sendMessage(Component.text("Spawn set to your current location.", NamedTextColor.GREEN));
            SoundUtil.playSuccess(player);
            return true;
        }

        if (!player.hasPermission("shura.command.spawn")) {
            plugin.getMessageService().send(player, "errors.no-permission");
            return true;
        }

        String spawnStr = plugin.getConfig().getString("lobby.spawn", "0,64,0,0,0");
        String world = plugin.getConfig().getString("lobby.world", "world");
        try {
            Location spawn = LocationUtil.deserialize(world + "," + spawnStr);
            player.teleport(spawn);
            player.sendMessage(Component.text("Teleported to spawn.", NamedTextColor.GREEN));
            SoundUtil.playTeleport(player);
            dev.shura.core.lobby.LobbyItems.give(plugin, player);
        } catch (Exception e) {
            player.sendMessage(Component.text("Spawn is not configured.", NamedTextColor.RED));
        }
        return true;
    }
}
