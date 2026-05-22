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
        
        Match match = plugin.getMatchManager().getMatchByPlayer(player.getUniqueId());
        if (match != null && match.getState() == MatchState.STARTING) {
            event.setCancelled(true);
            return;
        }
        
        // Don't cancel any GUI clicks - triumphgui handles it
        if (event.getClickedInventory() != player.getInventory()) {
            return;
        }
        
        // Only cancel clicks in player's own inventory when in lobby with no GUI open
        if (!plugin.getMatchManager().isInMatch(player.getUniqueId()) &&
            !plugin.getPracticeManager().isInPractice(player.getUniqueId()) &&
            !plugin.getSpectatorManager().isSpectating(player.getUniqueId())) {
            
            // Check if a GUI is open (top inventory is not player's inventory)
            if (event.getView().getTopInventory().getType() == org.bukkit.event.inventory.InventoryType.CHEST) {
                return; // Don't cancel, let triumphgui handle it
            }
            
            event.setCancelled(true);
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
