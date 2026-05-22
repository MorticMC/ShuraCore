package dev.shura.core.listener;

import dev.shura.core.ShuraCore;
import dev.shura.core.match.Match;
import dev.shura.core.match.MatchState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;

public class MatchListener implements Listener {

    private final ShuraCore plugin;

    public MatchListener(ShuraCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getMatchManager().isInMatch(player.getUniqueId())) return;

        event.setDeathMessage(null);
        event.getDrops().clear();
        event.setDroppedExp(0);
        event.setKeepInventory(true);
        event.setKeepLevel(true);

        Match match = plugin.getMatchManager().getMatchByPlayer(player.getUniqueId());
        if (match != null && match.getState() == MatchState.IN_PROGRESS) {
            match.handleDeath(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player damaged)) return;
        if (!(event.getDamager() instanceof Player damager)) return;

        Match match = plugin.getMatchManager().getMatchByPlayer(damaged.getUniqueId());
        if (match == null) return;

        // Block damage if match is not in progress
        if (match.getState() != MatchState.IN_PROGRESS) {
            event.setCancelled(true);
            return;
        }

        // Block friendly fire — both players must be in the same match and opponents
        if (!match.hasPlayer(damager.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFoodChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Match match = plugin.getMatchManager().getMatchByPlayer(player.getUniqueId());
        if (match == null) return;
        // Prevent food drain during countdown
        if (match.getState() == MatchState.STARTING) event.setCancelled(true);
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (plugin.getMatchManager().isInMatch(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Match match = plugin.getMatchManager().getMatchByPlayer(player.getUniqueId());
        if (match != null && match.getState() == MatchState.STARTING) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(org.bukkit.event.block.BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getMatchManager().isInMatch(player.getUniqueId())) return;
        Match match = plugin.getMatchManager().getMatchByPlayer(player.getUniqueId());
        if (match != null && match.getKit().getRules().isNoBuilding()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(org.bukkit.event.block.BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getMatchManager().isInMatch(player.getUniqueId())) return;
        Match match = plugin.getMatchManager().getMatchByPlayer(player.getUniqueId());
        if (match != null && match.getKit().getRules().isNoBuilding()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        Match match = plugin.getMatchManager().getMatchByPlayer(player.getUniqueId());
        if (match == null) return;
        if (match.getState() == MatchState.STARTING || match.getState() == MatchState.ENDING) {
            event.setCancelled(true);
        }
    }
}
