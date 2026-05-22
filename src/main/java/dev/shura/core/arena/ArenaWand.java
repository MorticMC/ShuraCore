package dev.shura.core.arena;

import dev.shura.core.util.ItemBuilder;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ArenaWand {

    public static final ItemStack WAND_ITEM = new ItemBuilder(Material.BLAZE_ROD)
            .name("&6Arena Wand")
            .lore("&eLeft Click &7— Set Position 1", "&eRight Click &7— Set Position 2")
            .glowing()
            .build();

    private final Map<UUID, Location> pos1Map = new HashMap<>();
    private final Map<UUID, Location> pos2Map = new HashMap<>();

    public void setPos1(Player player, Location location) {
        pos1Map.put(player.getUniqueId(), location.clone());
        player.sendMessage(dev.shura.core.util.ColorUtil.color(
                "&aPosition 1 set to &e" + formatLoc(location)));
    }

    public void setPos2(Player player, Location location) {
        pos2Map.put(player.getUniqueId(), location.clone());
        player.sendMessage(dev.shura.core.util.ColorUtil.color(
                "&aPosition 2 set to &e" + formatLoc(location)));
    }

    public Location getPos1(Player player) { return pos1Map.get(player.getUniqueId()); }
    public Location getPos2(Player player) { return pos2Map.get(player.getUniqueId()); }

    public boolean hasBothPositions(Player player) {
        return pos1Map.containsKey(player.getUniqueId()) && pos2Map.containsKey(player.getUniqueId());
    }

    public void clear(Player player) {
        pos1Map.remove(player.getUniqueId());
        pos2Map.remove(player.getUniqueId());
    }

    public boolean isWand(ItemStack item) {
        return item != null && item.isSimilar(WAND_ITEM);
    }

    private String formatLoc(Location loc) {
        return String.format("(%d, %d, %d)", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }
}
