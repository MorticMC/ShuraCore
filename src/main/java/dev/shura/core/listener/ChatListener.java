package dev.shura.core.listener;

import dev.shura.core.ShuraCore;
import dev.shura.core.party.Party;
import dev.shura.core.profile.Profile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {

    private final ShuraCore plugin;

    public ChatListener(ShuraCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        event.setCancelled(true);

        // Staff chat toggle — route to staff only
        Profile profile = plugin.getProfileManager().getProfile(player);
        if (profile != null && profile.isStaffChat()) {
            Component formatted = Component.text("[Staff] ", NamedTextColor.RED)
                    .append(Component.text(player.getName(), NamedTextColor.GRAY))
                    .append(Component.text(": ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(message, NamedTextColor.WHITE));
            plugin.getServer().getOnlinePlayers().stream()
                    .filter(p -> p.hasPermission("shura.command.staffchat"))
                    .forEach(p -> p.sendMessage(formatted));
            plugin.getServer().getConsoleSender().sendMessage(formatted);
            return;
        }

        // Party chat prefix '!'
        if (message.startsWith("!") && plugin.getPartyManager().isInParty(player.getUniqueId())) {
            Party party = plugin.getPartyManager().getParty(player.getUniqueId());
            if (party != null) {
                party.broadcast(Component.text("[Party] ", NamedTextColor.DARK_GREEN)
                        .append(Component.text(player.getName(), NamedTextColor.GREEN))
                        .append(Component.text(": ", NamedTextColor.DARK_GRAY))
                        .append(Component.text(message.substring(1).trim(), NamedTextColor.WHITE)));
            }
            return;
        }

        // Delegate all other chat to ChatManager (runs sync since we need Bukkit API)
        plugin.getServer().getScheduler().runTask(plugin,
                () -> plugin.getChatManager().handleChat(player, message));
    }
}
