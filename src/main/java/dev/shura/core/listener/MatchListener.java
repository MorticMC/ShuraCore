package dev.shura.core.listener;

import dev.shura.core.ShuraCore;
import dev.shura.core.match.MatchState;
import dev.shura.core.match.TeamMatch;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
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

    /** True while a player's match (1v1 or team) is in the pre-fight countdown/setup phase. */
    private boolean inCountdown(Player player) {
        MatchState state = plugin.getAnyMatchState(player.getUniqueId());
        return state == MatchState.COUNTDOWN || state == MatchState.STARTING;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamageInCountdown(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (inCountdown(player)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAttackInCountdown(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (inCountdown(attacker)) event.setCancelled(true);
    }

    /** Prevents teammates from damaging each other in a team match. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFriendlyFire(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        Player attacker = resolveAttacker(event.getDamager());
        if (attacker == null || attacker.equals(victim)) return;

        TeamMatch match = plugin.getTeamMatchManager().getMatchByPlayer(attacker.getUniqueId());
        if (match == null || !match.hasPlayer(victim.getUniqueId())) return;

        if (match.getTeam(attacker.getUniqueId()) == match.getTeam(victim.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    private Player resolveAttacker(org.bukkit.entity.Entity damager) {
        if (damager instanceof Player player) return player;
        if (damager instanceof Projectile projectile
                && projectile.getShooter() instanceof Player shooter) return shooter;
        return null;
    }

    @EventHandler
    public void onBlockBreakInCountdown(BlockBreakEvent event) {
        if (inCountdown(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler
    public void onBlockPlaceInCountdown(BlockPlaceEvent event) {
        if (inCountdown(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler
    public void onItemDropInCountdown(PlayerDropItemEvent event) {
        if (inCountdown(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler
    public void onInteractInCountdown(PlayerInteractEvent event) {
        if (inCountdown(event.getPlayer())) event.setCancelled(true);
    }
}
