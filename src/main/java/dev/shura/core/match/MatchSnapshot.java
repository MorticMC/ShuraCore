package dev.shura.core.match;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.Collection;

public class MatchSnapshot {

    private final ItemStack[] inventory;
    private final ItemStack[] armor;
    private final ItemStack offhand;
    private final Collection<PotionEffect> effects;
    private final double health;
    private final int food;
    private final int level;
    private final float exp;

    public MatchSnapshot(Player player) {
        this.inventory = player.getInventory().getContents().clone();
        this.armor = player.getInventory().getArmorContents().clone();
        this.offhand = player.getInventory().getItemInOffHand().clone();
        this.effects = player.getActivePotionEffects();
        this.health = player.getHealth();
        this.food = player.getFoodLevel();
        this.level = player.getLevel();
        this.exp = player.getExp();
    }

    public void restore(Player player) {
        player.getInventory().setContents(inventory);
        player.getInventory().setArmorContents(armor);
        player.getInventory().setItemInOffHand(offhand);
        player.getActivePotionEffects().forEach(e -> player.removePotionEffect(e.getType()));
        effects.forEach(player::addPotionEffect);
        player.setHealth(Math.min(health, player.getMaxHealth()));
        player.setFoodLevel(food);
        player.setLevel(level);
        player.setExp(exp);
    }

    public double getHealth() { return health; }
    public int getFood() { return food; }
    public Collection<PotionEffect> getEffects() { return effects; }
}
