package dev.shura.core.listener;

import dev.shura.core.ShuraCore;
import dev.shura.core.kit.Kit;
import dev.shura.core.kit.KitRules;
import dev.shura.core.match.MatchState;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;

import java.util.UUID;

/**
 * Enforces {@link KitRules} for the kit a player is currently fighting with,
 * in both 1v1 ({@link dev.shura.core.match.Match}) and team
 * ({@link dev.shura.core.match.TeamMatch}) matches.
 */
public class KitRulesListener implements Listener {

    private final ShuraCore plugin;

    public KitRulesListener(ShuraCore plugin) {
        this.plugin = plugin;
    }

    /** Returns the active kit rules for a player in a live match, or null if not applicable. */
    private KitRules rulesFor(UUID uuid) {
        Kit kit = plugin.getAnyMatchKit(uuid);
        return kit != null ? kit.getRules() : null;
    }

    private boolean fighting(UUID uuid) {
        MatchState state = plugin.getAnyMatchState(uuid);
        return state == MatchState.IN_PROGRESS;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        KitRules rules = rulesFor(event.getPlayer().getUniqueId());
        if (rules != null && rules.isNoBuilding()) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        KitRules rules = rulesFor(event.getPlayer().getUniqueId());
        if (rules != null && rules.isNoBlockBreak()) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onRegen(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        KitRules rules = rulesFor(player.getUniqueId());
        if (rules == null || !rules.isNoRegen()) return;
        if (event.getRegainReason() == EntityRegainHealthEvent.RegainReason.REGEN
                || event.getRegainReason() == EntityRegainHealthEvent.RegainReason.SATIATED) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFallDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        KitRules rules = rulesFor(player.getUniqueId());
        if (rules != null && rules.isNoFallDamage()) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onHunger(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        KitRules rules = rulesFor(player.getUniqueId());
        if (rules != null && rules.isNoHunger()) {
            event.setCancelled(true);
            event.setFoodLevel(20);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        KitRules rules = rulesFor(event.getPlayer().getUniqueId());
        if (rules != null && rules.isNoOffhand()) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPearlThrow(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof EnderPearl pearl)) return;
        if (!(pearl.getShooter() instanceof Player player)) return;
        KitRules rules = rulesFor(player.getUniqueId());
        if (rules != null && rules.isNoEnderpearl()) {
            event.setCancelled(true);
            if (fighting(player.getUniqueId())) {
                player.sendMessage(dev.shura.core.extra.MessageService.colorizeComponent(
                        "&cEnder pearls are disabled in this kit."));
            }
        }
    }
}
