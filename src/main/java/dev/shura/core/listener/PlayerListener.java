package dev.shura.core.listener;

import dev.shura.core.ShuraCore;
import dev.shura.core.extra.LobbyItems;
import dev.shura.core.util.LocationUtil;
import dev.shura.core.util.SoundUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerListener implements Listener {

    private final ShuraCore plugin;
    private final Map<UUID, Location> backLocations = new HashMap<>();

    public PlayerListener(ShuraCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Load profile async
        plugin.getProfileManager().loadProfile(player.getUniqueId(), player.getName())
                .thenAccept(profile -> {
                    if (profile == null) return;
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        plugin.getBoardManager().createBoard(player);
                        plugin.getTabManager().updateAll(player);
                        plugin.getKitEditor().loadCustomKits(player.getUniqueId());
                        if (profile.isVanished()) applyVanish(player);
                        LobbyItems.give(plugin, player);
                    });
                });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Handle match disconnect
        if (plugin.getMatchManager().isInMatch(uuid)) {
            var match = plugin.getMatchManager().getMatchByPlayer(uuid);
            if (match != null) match.handleLeave(player);
        } else if (plugin.getTeamMatchManager().isInMatch(uuid)) {
            var teamMatch = plugin.getTeamMatchManager().getMatchByPlayer(uuid);
            if (teamMatch != null) teamMatch.handleLeave(player);
        }

        // Leave queue
        if (plugin.getQueueManager().isInQueue(uuid)) {
            plugin.getQueueManager().leaveAllQueues(player);
        }

        // Handle party disconnect
        plugin.getPartyManager().handleDisconnect(uuid);

        // Cleanup duel requests
        plugin.getDuelManager().cleanup(uuid);

        // Remove spectator
        if (plugin.getSpectatorManager().isSpectating(uuid)) {
            plugin.getSpectatorManager().removeSpectator(player);
        }

        plugin.getInteractionManager().remove(uuid);
        plugin.getBoardManager().removeBoard(player);
        plugin.getProfileManager().unloadProfile(uuid);
        plugin.getKitEditor().unload(uuid);
        backLocations.remove(uuid);
        // Update tablist for remaining players
        plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> plugin.getServer().getOnlinePlayers()
                        .forEach(p -> plugin.getTabManager().update(p)), 1L);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        backLocations.put(player.getUniqueId(), player.getLocation().clone());

        // Handle match death (1v1)
        if (plugin.getMatchManager().isInMatch(player.getUniqueId())) {
            event.setDeathMessage(null);
            event.getDrops().clear();
            event.setDroppedExp(0);
            event.setCancelled(true);

            // Fake death - set health to max and handle via match
            player.setHealth(player.getMaxHealth());

            var match = plugin.getMatchManager().getMatchByPlayer(player.getUniqueId());
            if (match != null) {
                match.handleDeath(player);
            }
        } else if (plugin.getTeamMatchManager().isInMatch(player.getUniqueId())) {
            // Handle match death (team)
            event.setDeathMessage(null);
            event.getDrops().clear();
            event.setDroppedExp(0);
            event.setCancelled(true);
            player.setHealth(player.getMaxHealth());

            var teamMatch = plugin.getTeamMatchManager().getMatchByPlayer(player.getUniqueId());
            if (teamMatch != null) {
                teamMatch.handleDeath(player);
            }
        }
    }

    @EventHandler
    public void onGodDamage(org.bukkit.event.entity.EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (plugin.isInAnyMatch(player.getUniqueId())) return;
        var profile = plugin.getProfileManager().getProfile(player);
        if (profile != null && profile.isGodMode()) event.setCancelled(true);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (plugin.isInAnyMatch(player.getUniqueId())) return;

        // Respawn at lobby spawn
        String spawnStr = plugin.getConfig().getString("lobby.spawn", "0,64,0,0,0");
        String world = plugin.getConfig().getString("lobby.world", "world");
        try {
            event.setRespawnLocation(LocationUtil.deserialize(world + "," + spawnStr));
        } catch (Exception ignored) {}

        // Give lobby items after respawn (1 tick delay so inventory is ready)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> LobbyItems.give(plugin, player), 1L);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (plugin.isInAnyMatch(player.getUniqueId())) return;
        if (plugin.getSpectatorManager().isSpectating(player.getUniqueId())) return;

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (LobbyItems.isLobbyItem(item)) {
            event.setCancelled(true);
            LobbyItems.handleClick(plugin, player, item);
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (plugin.isInAnyMatch(player.getUniqueId())) return;
        if (plugin.getSpectatorManager().isSpectating(player.getUniqueId())) return;
        String lobbyWorld = plugin.getConfig().getString("lobby.world", "world");
        if (player.getWorld().getName().equals(lobbyWorld)) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> LobbyItems.give(plugin, player), 1L);
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (plugin.isInAnyMatch(player.getUniqueId())) return;
        if (plugin.getSpectatorManager().isSpectating(player.getUniqueId())) return;
        // Save back location on teleport
        backLocations.put(player.getUniqueId(), event.getFrom().clone());
    }

    public Location getBackLocation(UUID uuid) { return backLocations.get(uuid); }
    public void setBackLocation(UUID uuid, Location location) { backLocations.put(uuid, location); }

    private void applyVanish(Player player) {
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (!online.hasPermission("shura.command.vanish")) {
                online.hidePlayer(plugin, player);
            }
        }
    }
}
