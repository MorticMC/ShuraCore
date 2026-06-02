package dev.shura.core.extra;

import dev.shura.core.kit.Kit;
import dev.shura.core.util.SoundUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

public class PotionApplicator {

    public static void apply(Player player, Kit kit) {
        removeKitPotions(player);
        applyEffects(player, kit);
    }

    private static void removeKitPotions(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null) continue;
            if (isPotion(item.getType())) {
                player.getInventory().setItem(i, null);
            }
        }
        // Also check offhand
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (isPotion(offhand.getType())) {
            player.getInventory().setItemInOffHand(null);
        }
    }

    private static void applyEffects(Player player, Kit kit) {
        // Clear existing effects first
        player.getActivePotionEffects().forEach(e -> player.removePotionEffect(e.getType()));
        for (PotionEffect effect : kit.getEffects()) {
            player.addPotionEffect(effect);
        }
    }

    private static boolean isPotion(Material material) {
        return material == Material.POTION
                || material == Material.SPLASH_POTION
                || material == Material.LINGERING_POTION;
    }
}
