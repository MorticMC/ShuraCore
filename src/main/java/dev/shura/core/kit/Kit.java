package dev.shura.core.kit;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
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
        ItemStack[] contents = inventory.clone();
        if (rules.isNoShield()) stripShields(contents);
        player.getInventory().setContents(contents);
        player.getInventory().setArmorContents(hideTrims(armor.clone()));
        if (offhand != null && !rules.isNoOffhand()
                && !(rules.isNoShield() && offhand.getType() == org.bukkit.Material.SHIELD)) {
            player.getInventory().setItemInOffHand(offhand.clone());
        } else {
            player.getInventory().setItemInOffHand(null);
        }
        // Effects are applied by PotionApplicator after countdown
    }

    private static void stripShields(ItemStack[] items) {
        if (items == null) return;
        for (int i = 0; i < items.length; i++) {
            if (items[i] != null && items[i].getType() == org.bukkit.Material.SHIELD) {
                items[i] = null;
            }
        }
    }

    /**
     * Hides the armor-trim attribute lines from the tooltip/lore of armor pieces.
     * The trim is kept on the item (visual model stays) — only the lore text is suppressed.
     */
    private static ItemStack[] hideTrims(ItemStack[] pieces) {
        if (pieces == null) return null;
        for (ItemStack piece : pieces) {
            if (piece == null || piece.getType().isAir()) continue;
            ItemMeta meta = piece.getItemMeta();
            if (meta == null) continue;
            meta.addItemFlags(ItemFlag.HIDE_ARMOR_TRIM);
            piece.setItemMeta(meta);
        }
        return pieces;
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
