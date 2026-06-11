package dev.shura.core.listener;

import dev.shura.core.ShuraCore;
import dev.shura.core.match.MatchState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class InventoryListener implements Listener {

    private final ShuraCore plugin;

    public InventoryListener(ShuraCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(event.getView().title());

        if (title.contains("Arena:")) {
            event.setCancelled(true);
            String arenaName = title.substring(title.indexOf("Arena:") + 6).trim();
            dev.shura.core.arena.Arena arena = plugin.getArenaManager().getArenaByName(arenaName);
            if (arena != null) {
                dev.shura.core.gui.ArenaSettingsGui.handleClick(plugin, event, arena);
            }
            return;
        }

        // Lock inventory while a match (1v1 or team) is in its setup phase
        if (plugin.getAnyMatchState(player.getUniqueId()) == MatchState.STARTING) {
            event.setCancelled(true);
        }
    }
}
