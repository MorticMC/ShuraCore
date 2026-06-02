package dev.shura.core.match;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.Collection;
import java.util.UUID;

public class MatchPlayer {

    private final UUID uuid;
    private final String name;
    private int roundsWon;
    private boolean alive;
    private boolean disconnected;

    // Snapshot of player state before match — restored on match end
    private ItemStack[] inventorySnapshot;
    private ItemStack[] armorSnapshot;
    private ItemStack offhandSnapshot;
    private Collection<PotionEffect> effectsSnapshot;
    private double healthSnapshot;
    private int foodSnapshot;
    private int levelSnapshot;
    private float expSnapshot;
    private Location locationSnapshot;
    private org.bukkit.GameMode gameModeSnapshot;

    public MatchPlayer(Player player) {
        this.uuid = player.getUniqueId();
        this.name = player.getName();
        this.alive = true;
        this.roundsWon = 0;
        this.disconnected = false;
        snapshot(player);
    }

    private void snapshot(Player player) {
        inventorySnapshot = player.getInventory().getContents().clone();
        armorSnapshot = player.getInventory().getArmorContents().clone();
        offhandSnapshot = player.getInventory().getItemInOffHand().clone();
        effectsSnapshot = player.getActivePotionEffects();
        healthSnapshot = player.getHealth();
        foodSnapshot = player.getFoodLevel();
        levelSnapshot = player.getLevel();
        expSnapshot = player.getExp();
        locationSnapshot = player.getLocation().clone();
        gameModeSnapshot = player.getGameMode();
    }

    public void restore(Player player) {
        player.getInventory().setContents(inventorySnapshot);
        player.getInventory().setArmorContents(armorSnapshot);
        player.getInventory().setItemInOffHand(offhandSnapshot);
        player.getActivePotionEffects().forEach(e -> player.removePotionEffect(e.getType()));
        effectsSnapshot.forEach(player::addPotionEffect);
        player.setHealth(Math.min(healthSnapshot, player.getMaxHealth()));
        player.setFoodLevel(foodSnapshot);
        player.setLevel(levelSnapshot);
        player.setExp(expSnapshot);
        player.setGameMode(gameModeSnapshot);
        player.teleport(locationSnapshot);
    }

    public void incrementRoundsWon() { roundsWon++; }

    public UUID getUuid() { return uuid; }
    public String getName() { return name; }
    public int getRoundsWon() { return roundsWon; }
    public boolean isAlive() { return alive; }
    public void setAlive(boolean alive) { this.alive = alive; }
    public boolean isDisconnected() { return disconnected; }
    public void setDisconnected(boolean disconnected) { this.disconnected = disconnected; }
    public Location getLocationSnapshot() { return locationSnapshot; }
}
