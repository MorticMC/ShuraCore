package dev.shura.core.listener;

import dev.shura.core.ShuraCore;
import dev.shura.core.match.Match;
import dev.shura.core.match.MatchState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class MatchListener implements Listener {

    private final ShuraCore plugin;

    public MatchListener(ShuraCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamageInCountdown(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Match match = plugin.getMatchManager().getMatchByPlayer(player.getUniqueId());
        if (match == null) return;
        if (match.getState() == MatchState.COUNTDOWN || match.getState() == MatchState.STARTING) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAttackInCountdown(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        Match match = plugin.getMatchManager().getMatchByPlayer(attacker.getUniqueId());
        if (match == null) return;
        if (match.getState() == MatchState.COUNTDOWN || match.getState() == MatchState.STARTING) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreakInCountdown(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Match match = plugin.getMatchManager().getMatchByPlayer(player.getUniqueId());
        if (match == null) return;
        if (match.getState() == MatchState.COUNTDOWN || match.getState() == MatchState.STARTING) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlaceInCountdown(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Match match = plugin.getMatchManager().getMatchByPlayer(player.getUniqueId());
        if (match == null) return;
        if (match.getState() == MatchState.COUNTDOWN || match.getState() == MatchState.STARTING) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onItemDropInCountdown(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        Match match = plugin.getMatchManager().getMatchByPlayer(player.getUniqueId());
        if (match == null) return;
        if (match.getState() == MatchState.COUNTDOWN || match.getState() == MatchState.STARTING) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteractInCountdown(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Match match = plugin.getMatchManager().getMatchByPlayer(player.getUniqueId());
        if (match == null) return;
        if (match.getState() == MatchState.COUNTDOWN || match.getState() == MatchState.STARTING) {
            event.setCancelled(true);
        }
    }
}
