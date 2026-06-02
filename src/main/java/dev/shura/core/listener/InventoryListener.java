package dev.shura.core.listener;

import dev.shura.core.ShuraCore;
import dev.shura.core.match.Match;
import dev.shura.core.match.MatchState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

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
        
        Match match = plugin.getMatchManager().getMatchByPlayer(player.getUniqueId());
        if (match != null && match.getState() == MatchState.STARTING) {
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        Match match = plugin.getMatchManager().getMatchByPlayer(player.getUniqueId());
        if (match != null && match.getKit().getRules().isNoOffhand()) {
            event.setCancelled(true);
        }
    }
}
