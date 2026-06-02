package dev.shura.core.command;

import dev.shura.core.ShuraCore;
import dev.shura.core.gui.RankingsGui;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RankingsCommand implements CommandExecutor {

    private final ShuraCore plugin;

    public RankingsCommand(ShuraCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Players only.", NamedTextColor.RED));
            return true;
        }
        new RankingsGui(plugin).open(player);
        return true;
    }
}
