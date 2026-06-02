package dev.shura.core.kit;

import dev.shura.core.ShuraCore;
import dev.shura.core.util.JsonUtil;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class KitEditor {

    private final ShuraCore plugin;
    // playerUUID -> kitId -> custom inventory
    private final Map<UUID, Map<String, ItemStack[]>> customInventories = new ConcurrentHashMap<>();
    // playerUUID -> kitId -> custom armor
    private final Map<UUID, Map<String, ItemStack[]>> customArmors = new ConcurrentHashMap<>();

    public KitEditor(ShuraCore plugin) {
        this.plugin = plugin;
    }

    public void openEditor(Player player, Kit kit) {
        new dev.shura.core.gui.KitEditorGui(plugin, player, kit).open(player);
    }

    public void saveCustomKit(Player player, Kit kit, ItemStack[] inventory, ItemStack[] armor) {
        UUID uuid = player.getUniqueId();
        customInventories.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>()).put(kit.getId(), inventory);
        customArmors.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>()).put(kit.getId(), armor);

        String invJson = JsonUtil.itemsToBase64(inventory);
        String armorJson = JsonUtil.itemsToBase64(armor);

        plugin.getDatabaseService().updateAsync(
                "INSERT INTO player_kits (player_uuid, kit_id, inventory, armor) VALUES (?, ?, ?, ?) " +
                "ON CONFLICT(player_uuid, kit_id) DO UPDATE SET inventory=excluded.inventory, armor=excluded.armor",
                stmt -> {
                    stmt.setString(1, uuid.toString());
                    stmt.setString(2, kit.getId());
                    stmt.setString(3, invJson);
                    stmt.setString(4, armorJson);
                });

        player.sendMessage(plugin.getMessageService().get("kit.saved"));
    }

    public void loadCustomKits(UUID uuid) {
        plugin.getDatabaseService().query(
                "SELECT kit_id, inventory, armor FROM player_kits WHERE player_uuid = ?",
                stmt -> stmt.setString(1, uuid.toString()),
                rs -> {
                    Map<String, ItemStack[]> invMap = new ConcurrentHashMap<>();
                    Map<String, ItemStack[]> armorMap = new ConcurrentHashMap<>();
                    try {
                        while (rs.next()) {
                            String kitId = rs.getString("kit_id");
                            String invJson = rs.getString("inventory");
                            String armorJson = rs.getString("armor");
                            if (invJson != null && !invJson.isEmpty())
                                invMap.put(kitId, JsonUtil.itemsFromBase64(invJson));
                            if (armorJson != null && !armorJson.isEmpty())
                                armorMap.put(kitId, JsonUtil.itemsFromBase64(armorJson));
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to load custom kits for " + uuid);
                    }
                    customInventories.put(uuid, invMap);
                    customArmors.put(uuid, armorMap);
                    return null;
                });
    }

    public void unload(UUID uuid) {
        customInventories.remove(uuid);
        customArmors.remove(uuid);
    }

    public ItemStack[] getCustomInventory(UUID uuid, String kitId) {
        Map<String, ItemStack[]> map = customInventories.get(uuid);
        return map != null ? map.get(kitId) : null;
    }

    public ItemStack[] getCustomArmor(UUID uuid, String kitId) {
        Map<String, ItemStack[]> map = customArmors.get(uuid);
        return map != null ? map.get(kitId) : null;
    }

    public boolean hasCustomKit(UUID uuid, String kitId) {
        Map<String, ItemStack[]> map = customInventories.get(uuid);
        return map != null && map.containsKey(kitId);
    }
}
