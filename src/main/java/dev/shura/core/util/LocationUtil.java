package dev.shura.core.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class LocationUtil {

    // Format: world,x,y,z,yaw,pitch
    public static String serialize(Location loc) {
        return String.format("%s,%.4f,%.4f,%.4f,%.4f,%.4f",
                loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(),
                loc.getYaw(), loc.getPitch());
    }

    public static Location deserialize(String serialized) {
        String[] parts = serialized.split(",");
        if (parts.length < 4) throw new IllegalArgumentException("Invalid location string: " + serialized);
        World world = Bukkit.getWorld(parts[0]);
        if (world == null) throw new IllegalArgumentException("World not found: " + parts[0]);
        double x = Double.parseDouble(parts[1]);
        double y = Double.parseDouble(parts[2]);
        double z = Double.parseDouble(parts[3]);
        float yaw = parts.length > 4 ? Float.parseFloat(parts[4]) : 0f;
        float pitch = parts.length > 5 ? Float.parseFloat(parts[5]) : 0f;
        return new Location(world, x, y, z, yaw, pitch);
    }

    public static Location offset(Location base, int offsetX, int offsetZ) {
        return base.clone().add(offsetX, 0, offsetZ);
    }
}
