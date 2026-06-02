package dev.shura.core.command;

import dev.shura.core.ShuraCore;
import dev.shura.core.gui.ProfileGui;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ProfileCommand implements CommandExecutor {

    private final ShuraCore plugin;

    public ProfileCommand(ShuraCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        new ProfileGui(plugin).open(player);
        return true;
    }
}
