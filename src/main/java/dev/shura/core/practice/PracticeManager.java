package dev.shura.core.practice;

import dev.shura.core.ShuraCore;
import dev.shura.core.kit.Kit;
import dev.shura.core.potion.PotionApplicator;
import dev.shura.core.util.LocationUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PracticeManager {

    private final ShuraCore plugin;
    // playerUUID -> gamemode they are practicing
    private final Map<UUID, PracticeGamemode> practicePlayers = new ConcurrentHashMap<>();
    // gamemode -> spawn location in shura_ffa world
    private final Map<PracticeGamemode, Location> spawnLocations = new ConcurrentHashMap<>();

    public PracticeManager(ShuraCore plugin) {
        this.plugin = plugin;
        loadSpawnLocations();
    }

    private void loadSpawnLocations() {
        for (PracticeGamemode gm : PracticeGamemode.values()) {
            String path = "practice.spawns." + gm.getId();
            String locStr = plugin.getConfig().getString(path);
            if (locStr != null) {
                try {
                    spawnLocations.put(gm, LocationUtil.deserialize(locStr));
                } catch (Exception e) {
                    // ignore — spawn not configured yet
                }
            }
        }
    }

    public boolean joinPractice(Player player, PracticeGamemode gamemode) {
        if (plugin.getMatchManager().isInMatch(player.getUniqueId())) {
            player.sendMessage(Component.text("You cannot join practice while in a match.", NamedTextColor.RED));
            return false;
        }
        if (plugin.getQueueManager().isInQueue(player.getUniqueId())) {
            player.sendMessage(Component.text("You cannot join practice while in a queue.", NamedTextColor.RED));
            return false;
        }

        Location spawn = spawnLocations.get(gamemode);
        if (spawn == null) {
            player.sendMessage(Component.text("That practice arena is not configured yet.", NamedTextColor.RED));
            return false;
        }

        // Find kit for this gamemode
        Kit kit = plugin.getKitManager().getAllKits().stream()
                .filter(k -> gamemode.getId().equalsIgnoreCase(k.getTierlistId()))
                .findFirst().orElse(null);

        leavePractice(player, false);
        practicePlayers.put(player.getUniqueId(), gamemode);

        player.teleport(spawn);
        player.setGameMode(GameMode.SURVIVAL);
        player.getInventory().clear();
        player.getActivePotionEffects().forEach(e -> player.removePotionEffect(e.getType()));
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);

        if (kit != null) {
            kit.applyTo(player);
            PotionApplicator.apply(player, kit);
        }

        player.sendMessage(Component.text("Joined ", NamedTextColor.GREEN)
                .append(Component.text(gamemode.getDisplayName(), NamedTextColor.AQUA))
                .append(Component.text(" FFA practice.", NamedTextColor.GREEN)));
        return true;
    }

    public void leavePractice(Player player, boolean sendMessage) {
        PracticeGamemode gm = practicePlayers.remove(player.getUniqueId());
        if (gm == null) return;

        player.getInventory().clear();
        player.getActivePotionEffects().forEach(e -> player.removePotionEffect(e.getType()));

        if (sendMessage)
            player.sendMessage(Component.text("Left ", NamedTextColor.GRAY)
                    .append(Component.text(gm.getDisplayName(), NamedTextColor.AQUA))
                    .append(Component.text(" practice.", NamedTextColor.GRAY)));
    }

    public void setSpawn(PracticeGamemode gamemode, Location location) {
        spawnLocations.put(gamemode, location);
        plugin.getConfig().set("practice.spawns." + gamemode.getId(), LocationUtil.serialize(location));
        plugin.saveConfig();
    }

    public boolean isInPractice(UUID uuid) { return practicePlayers.containsKey(uuid); }
    public PracticeGamemode getGamemode(UUID uuid) { return practicePlayers.get(uuid); }
    public int getPlayerCount(PracticeGamemode gamemode) {
        return (int) practicePlayers.values().stream().filter(gm -> gm == gamemode).count();
    }
}
