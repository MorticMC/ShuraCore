package dev.shura.core.extra;

import dev.shura.core.ShuraCore;
import dev.shura.core.util.LocationUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class SpawnManager {

    private final ShuraCore plugin;
    private Location spawnLocation;

    public SpawnManager(ShuraCore plugin) {
        this.plugin = plugin;
        loadSpawn();
    }

    private void loadSpawn() {
        String serialized = plugin.getConfig().getString("spawn-location");
        if (serialized != null && !serialized.isEmpty()) {
            spawnLocation = LocationUtil.deserialize(serialized);
        }
    }

    public void setSpawn(Location location) {
        this.spawnLocation = location;
        plugin.getConfig().set("spawn-location", LocationUtil.serialize(location));
        plugin.saveConfig();
    }

    public Location getSpawn() {
        if (spawnLocation != null) return spawnLocation.clone();
        return Bukkit.getWorlds().get(0).getSpawnLocation();
    }

    public void teleportToSpawn(Player player) {
        player.teleport(getSpawn());
    }
}
