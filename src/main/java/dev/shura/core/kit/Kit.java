package dev.shura.core.kit;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.List;

public class Kit {

    private final String id;
    private String name;
    private String tierlistId;
    private ItemStack[] inventory;
    private ItemStack[] armor;
    private ItemStack offhand;
    private List<PotionEffect> effects = new ArrayList<>();
    private KitRules rules = new KitRules();

    public Kit(String id, String name) {
        this.id = id;
        this.name = name;
        this.inventory = new ItemStack[36];
        this.armor = new ItemStack[4];
    }

    public void applyTo(Player player) {
        player.getInventory().setContents(inventory.clone());
        player.getInventory().setArmorContents(armor.clone());
        if (offhand != null) player.getInventory().setItemInOffHand(offhand.clone());
        // Effects are applied by PotionApplicator after countdown
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTierlistId() { return tierlistId; }
    public void setTierlistId(String tierlistId) { this.tierlistId = tierlistId; }
    public ItemStack[] getInventory() { return inventory; }
    public void setInventory(ItemStack[] inventory) { this.inventory = inventory; }
    public ItemStack[] getArmor() { return armor; }
    public void setArmor(ItemStack[] armor) { this.armor = armor; }
    public ItemStack getOffhand() { return offhand; }
    public void setOffhand(ItemStack offhand) { this.offhand = offhand; }
    public List<PotionEffect> getEffects() { return effects; }
    public void setEffects(List<PotionEffect> effects) { this.effects = effects; }
    public KitRules getRules() { return rules; }
    public void setRules(KitRules rules) { this.rules = rules; }
}
