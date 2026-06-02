package dev.shura.core.command;

import dev.shura.core.ShuraCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SpawnCommand implements CommandExecutor {

    private final ShuraCore plugin;

    public SpawnCommand(ShuraCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        if (plugin.getMatchManager().isInMatch(player.getUniqueId())) {
            player.sendMessage(Component.text("You cannot use this while in a match.", NamedTextColor.RED));
            return true;
        }

        plugin.getSpawnManager().teleportToSpawn(player);
        player.sendMessage(Component.text("Teleported to spawn.", NamedTextColor.GREEN));
        return true;
    }
}
