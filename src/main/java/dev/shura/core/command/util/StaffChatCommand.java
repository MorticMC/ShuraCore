package dev.shura.core.command.util;

import dev.shura.core.ShuraCore;
import dev.shura.core.message.MessageService;
import dev.shura.core.profile.Profile;
import dev.shura.core.util.SoundUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StaffChatCommand implements CommandExecutor {

    private final ShuraCore plugin;

    public StaffChatCommand(ShuraCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Players only.", NamedTextColor.RED));
            return true;
        }
        if (!player.hasPermission("shura.command.staffchat")) {
            plugin.getMessageService().send(player, "errors.no-permission");
            return true;
        }

        Profile profile = plugin.getProfileManager().getProfile(player);
        if (profile == null) return true;

        // /sc <message> — one-off staff message without toggling
        if (args.length > 0) {
            String message = String.join(" ", args);
            Component formatted = Component.text("[Staff] ", NamedTextColor.RED)
                    .append(Component.text(player.getName(), NamedTextColor.GRAY))
                    .append(Component.text(": ", NamedTextColor.DARK_GRAY))
                    .append(MessageService.colorizeComponent(message));

            plugin.getServer().getOnlinePlayers().stream()
                    .filter(p -> p.hasPermission("shura.command.staffchat"))
                    .forEach(p -> p.sendMessage(formatted));
            plugin.getServer().getConsoleSender().sendMessage(formatted);
            return true;
        }

        // /sc — toggle staff chat mode
        boolean toggled = !profile.isStaffChat();
        profile.setStaffChat(toggled);

        player.sendMessage(Component.text("Staff chat ", NamedTextColor.GRAY)
                .append(toggled
                        ? Component.text("enabled.", NamedTextColor.GREEN)
                        : Component.text("disabled.", NamedTextColor.RED)));
        SoundUtil.playSuccess(player);
        return true;
    }
}
