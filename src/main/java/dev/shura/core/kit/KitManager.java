package dev.shura.core.kit;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dev.shura.core.ShuraCore;
import dev.shura.core.util.JsonUtil;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class KitManager {

    private final ShuraCore plugin;
    private final Map<String, Kit> kits = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();

    public KitManager(ShuraCore plugin) {
        this.plugin = plugin;
        loadAll();
    }

    private void loadAll() {
        plugin.getDatabaseService().query("SELECT * FROM kits", null, rs -> {
            List<Kit> loaded = new ArrayList<>();
            try {
                while (rs.next()) loaded.add(deserialize(rs));
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load kits.", e);
            }
            return loaded;
        }).thenAccept(list -> {
            if (list != null) list.forEach(k -> kits.put(k.getId(), k));
        });
    }

    public void saveKit(Kit kit) {
        String inventoryJson = JsonUtil.itemsToBase64(kit.getInventory());
        String armorJson = JsonUtil.itemsToBase64(kit.getArmor());
        String offhandJson = kit.getOffhand() != null ? JsonUtil.itemsToBase64(new ItemStack[]{kit.getOffhand()}) : "[]";
        String effectsJson = JsonUtil.effectsToJson(kit.getEffects());
        String rulesJson = gson.toJson(kit.getRules());

        plugin.getDatabaseService().updateAsync(
                "INSERT INTO kits (id, name, tierlist_id, inventory, armor, offhand, effects, rules, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT(id) DO UPDATE SET " +
                "name=excluded.name, tierlist_id=excluded.tierlist_id, inventory=excluded.inventory, " +
                "armor=excluded.armor, offhand=excluded.offhand, effects=excluded.effects, rules=excluded.rules",
                stmt -> {
                    stmt.setString(1, kit.getId());
                    stmt.setString(2, kit.getName());
                    stmt.setString(3, kit.getTierlistId());
                    stmt.setString(4, inventoryJson);
                    stmt.setString(5, armorJson);
                    stmt.setString(6, offhandJson);
                    stmt.setString(7, effectsJson);
                    stmt.setString(8, rulesJson);
                    stmt.setLong(9, System.currentTimeMillis());
                });
        kits.put(kit.getId(), kit);
    }

    public void deleteKit(String id) {
        kits.remove(id);
        plugin.getDatabaseService().updateAsync("DELETE FROM kits WHERE id = ?",
                stmt -> stmt.setString(1, id));
    }

    public Kit getKit(String id) { return kits.get(id); }

    public Collection<Kit> getAllKits() { return Collections.unmodifiableCollection(kits.values()); }

    public List<Kit> getKitsByTierlist(String tierlistId) {
        return kits.values().stream()
                .filter(k -> tierlistId.equals(k.getTierlistId()))
                .toList();
    }

    public Kit createKit(String name, String tierlistId) {
        String id = UUID.randomUUID().toString();
        Kit kit = new Kit(id, name);
        kit.setTierlistId(tierlistId);
        saveKit(kit);
        return kit;
    }

    private Kit deserialize(ResultSet rs) throws SQLException {
        Kit kit = new Kit(rs.getString("id"), rs.getString("name"));
        kit.setTierlistId(rs.getString("tierlist_id"));

        String inventoryJson = rs.getString("inventory");
        String armorJson = rs.getString("armor");
        String offhandJson = null;
        try {
            offhandJson = rs.getString("offhand");
        } catch (SQLException e) {
            // Column doesn't exist yet, ignore
        }
        String effectsJson = rs.getString("effects");
        String rulesJson = rs.getString("rules");

        if (inventoryJson != null && !inventoryJson.isEmpty() && !inventoryJson.equals("{}"))
            kit.setInventory(JsonUtil.itemsFromBase64(inventoryJson));
        if (armorJson != null && !armorJson.isEmpty() && !armorJson.equals("{}"))
            kit.setArmor(JsonUtil.itemsFromBase64(armorJson));
        if (offhandJson != null && !offhandJson.isEmpty() && !offhandJson.equals("[]")) {
            ItemStack[] offhand = JsonUtil.itemsFromBase64(offhandJson);
            if (offhand.length > 0) kit.setOffhand(offhand[0]);
        }
        if (effectsJson != null && !effectsJson.isEmpty())
            kit.setEffects(JsonUtil.effectsFromJson(effectsJson));
        if (rulesJson != null && !rulesJson.isEmpty())
            kit.setRules(gson.fromJson(rulesJson, KitRules.class));

        return kit;
    }
}
