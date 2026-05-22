package dev.shura.core.command;

import dev.shura.core.ShuraCore;
import dev.shura.core.gui.ProfileGui;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ProfileCommand implements CommandExecutor {

    private final ShuraCore plugin;

    public ProfileCommand(ShuraCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        new ProfileGui(plugin).open(player);
        return true;
    }
}
