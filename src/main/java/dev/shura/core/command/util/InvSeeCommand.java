package dev.shura.core.command.util;

import dev.shura.core.ShuraCore;
import dev.shura.core.message.MessageService;
import dev.shura.core.util.SoundUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.Map;

public class InvSeeCommand implements CommandExecutor {

    private final ShuraCore plugin;

    public InvSeeCommand(ShuraCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Players only.", NamedTextColor.RED));
            return true;
        }
        if (!player.hasPermission("shura.command.invsee")) {
            plugin.getMessageService().send(player, "errors.no-permission");
            return true;
        }
        if (args.length == 0) {
            plugin.getMessageService().send(player, "errors.invalid-args", Map.of("usage", "/invsee <player>"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            plugin.getMessageService().send(player, "errors.player-not-found", Map.of("target", args[0]));
            return true;
        }

        Inventory inv = Bukkit.createInventory(null, 54,
                MessageService.colorizeComponent("&e" + target.getName() + "'s Inventory"));

        // Slots 0-35: inventory, 36-39: armor, 40: offhand
        System.arraycopy(target.getInventory().getContents(), 0, inv.getContents(), 0, 36);
        inv.setItem(36, target.getInventory().getHelmet());
        inv.setItem(37, target.getInventory().getChestplate());
        inv.setItem(38, target.getInventory().getLeggings());
        inv.setItem(39, target.getInventory().getBoots());
        inv.setItem(40, target.getInventory().getItemInOffHand());

        player.openInventory(inv);
        SoundUtil.playGuiOpen(player);
        return true;
    }
}
