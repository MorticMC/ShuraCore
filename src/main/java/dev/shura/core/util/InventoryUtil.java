package dev.shura.core.util;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class InventoryUtil {

    public static boolean isEmpty(Inventory inventory) {
        for (ItemStack item : inventory.getContents())
            if (item != null) return false;
        return true;
    }

    public static ItemStack[] clone(ItemStack[] items) {
        ItemStack[] cloned = new ItemStack[items.length];
        for (int i = 0; i < items.length; i++)
            cloned[i] = items[i] != null ? items[i].clone() : null;
        return cloned;
    }

    public static boolean containsItem(Inventory inventory, org.bukkit.Material material) {
        for (ItemStack item : inventory.getContents())
            if (item != null && item.getType() == material) return true;
        return false;
    }

    public static void clear(org.bukkit.entity.Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.getInventory().setItemInOffHand(null);
        player.getActivePotionEffects().forEach(e -> player.removePotionEffect(e.getType()));
    }
}
