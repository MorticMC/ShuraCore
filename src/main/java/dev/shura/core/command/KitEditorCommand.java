package dev.shura.core.command;

import dev.shura.core.ShuraCore;
import dev.shura.core.extra.MessageService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class KitEditorCommand implements CommandExecutor {

    private final ShuraCore plugin;

    public KitEditorCommand(ShuraCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        plugin.getGuiEditorManager().openGui(player, "kiteditor-main");
        return true;
    }
}
