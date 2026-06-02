package dev.shura.core.util;

import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PlayerDragUtil {

    private static List<PathPoint> templatePath = null;
    private static Plugin pluginInstance = null;

    public static void init(Plugin plugin) {
        pluginInstance = plugin;
        loadTemplatePath(plugin);
    }

    private static void loadTemplatePath(Plugin plugin) {
        File file = new File(plugin.getDataFolder(), "drag_path.yml");
        if (!file.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<String> pathData = config.getStringList("path");
        if (pathData.isEmpty()) return;

        templatePath = new ArrayList<>();
        for (String data : pathData) {
            String[] parts = data.split(",");
            if (parts.length >= 3) {
                templatePath.add(new PathPoint(
                    Double.parseDouble(parts[0]),
                    Double.parseDouble(parts[1]),
                    Double.parseDouble(parts[2])
                ));
            }
        }

        // Remove duplicate consecutive points
        List<PathPoint> cleaned = new ArrayList<>();
        PathPoint last = null;
        for (PathPoint p : templatePath) {
            if (last == null || !p.equals(last)) {
                cleaned.add(p);
                last = p;
            }
        }
        templatePath = cleaned;
    }

    public static void dragPlayer(Player player, Location target, Plugin plugin, Runnable onComplete) {
        if (pluginInstance == null) init(plugin);

        player.setAllowFlight(true);
        player.setFlying(true);
        player.setInvulnerable(true);
        player.setVelocity(new Vector(0, 0, 0));
        
        Location start = player.getLocation().clone();
        
        // If player is in a different world, teleport them first
        if (!start.getWorld().equals(target.getWorld())) {
            player.teleport(target);
            start = target.clone();
        }

        // Use template path if available, otherwise fallback to simple arc
        if (templatePath != null && !templatePath.isEmpty()) {
            dragWithTemplate(player, start, target, plugin, onComplete);
        } else {
            dragWithArc(player, start, target, plugin, onComplete);
        }
    }

    private static void dragWithTemplate(Player player, Location start, Location target, Plugin plugin, Runnable onComplete) {
        // Normalize template path to 0-1 range
        double minX = templatePath.stream().mapToDouble(p -> p.x).min().orElse(0);
        double maxX = templatePath.stream().mapToDouble(p -> p.x).max().orElse(1);
        double minY = templatePath.stream().mapToDouble(p -> p.y).min().orElse(0);
        double maxY = templatePath.stream().mapToDouble(p -> p.y).max().orElse(1);
        double minZ = templatePath.stream().mapToDouble(p -> p.z).min().orElse(0);
        double maxZ = templatePath.stream().mapToDouble(p -> p.z).max().orElse(1);

        double rangeX = maxX - minX;
        double rangeY = maxY - minY;
        double rangeZ = maxZ - minZ;

        final int[] index = {0};
        final int skipRate = Math.max(1, templatePath.size() / 80); // Spread over ~4 seconds (80 ticks)
        
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || index[0] >= templatePath.size()) {
                    player.setFlying(false);
                    player.setAllowFlight(false);
                    player.setInvulnerable(false);
                    player.setVelocity(new Vector(0, 0, 0));
                    player.teleport(target);
                    if (onComplete != null) onComplete.run();
                    cancel();
                    return;
                }
                
                PathPoint point = templatePath.get(index[0]);
                
                // Normalize point to 0-1
                double normX = rangeX > 0 ? (point.x - minX) / rangeX : 0;
                double normY = rangeY > 0 ? (point.y - minY) / rangeY : 0;
                double normZ = rangeZ > 0 ? (point.z - minZ) / rangeZ : 0;
                
                // Map to actual start->target range
                double x = start.getX() + (target.getX() - start.getX()) * normX;
                double y = start.getY() + (target.getY() - start.getY()) * normY;
                double z = start.getZ() + (target.getZ() - start.getZ()) * normZ;
                
                Location newLoc = new Location(target.getWorld(), x, y, z, target.getYaw(), target.getPitch());
                player.teleport(newLoc);
                player.setVelocity(new Vector(0, 0, 0));
                
                index[0] += skipRate;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private static void dragWithArc(Player player, Location start, Location target, Plugin plugin, Runnable onComplete) {
        double distance = start.distance(target);
        double maxHeight = Math.max(start.getY(), target.getY()) + Math.min(distance * 0.3, 3.0);
        
        final int totalTicks = 30;
        final int[] tick = {0};
        
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || tick[0] >= totalTicks) {
                    player.setFlying(false);
                    player.setAllowFlight(false);
                    player.setInvulnerable(false);
                    player.setVelocity(new Vector(0, 0, 0));
                    player.teleport(target);
                    if (onComplete != null) onComplete.run();
                    cancel();
                    return;
                }
                
                double progress = (double) tick[0] / totalTicks;
                double eased = progress < 0.5 
                    ? 4 * progress * progress * progress
                    : 1 - Math.pow(-2 * progress + 2, 3) / 2;
                
                double x = start.getX() + (target.getX() - start.getX()) * eased;
                double z = start.getZ() + (target.getZ() - start.getZ()) * eased;
                
                double arcProgress = Math.sin(progress * Math.PI);
                double baseY = start.getY() + (target.getY() - start.getY()) * eased;
                double arcHeight = (maxHeight - Math.max(start.getY(), target.getY())) * arcProgress;
                double y = baseY + arcHeight;
                
                Location newLoc = new Location(target.getWorld(), x, y, z, target.getYaw(), target.getPitch());
                player.teleport(newLoc);
                player.setVelocity(new Vector(0, 0, 0));
                
                tick[0]++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private static class PathPoint {
        double x, y, z;
        
        PathPoint(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof PathPoint other)) return false;
            return Math.abs(x - other.x) < 0.001 && 
                   Math.abs(y - other.y) < 0.001 && 
                   Math.abs(z - other.z) < 0.001;
        }
    }
}
