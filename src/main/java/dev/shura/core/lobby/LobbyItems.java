package dev.shura.core.lobby;

import dev.shura.core.ShuraCore;
import dev.shura.core.gui.ProfileGui;
import dev.shura.core.gui.SettingsGui;
import dev.shura.core.message.MessageService;
import net.luckperms.api.LuckPermsProvider;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

public class LobbyItems {

    private static NamespacedKey MARKER_KEY;
    private static NamespacedKey TYPE_KEY;

    public static void init(ShuraCore plugin) {
        MARKER_KEY = new NamespacedKey(plugin, "lobby_item");
        TYPE_KEY   = new NamespacedKey(plugin, "lobby_item_type");
    }

    private static ItemStack fromConfig(ShuraCore plugin, String key, Material fallbackMaterial, String fallbackName) {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("lobby-items." + key);
        
        // Try to load from Base64 data first (new format)
        if (sec != null && sec.contains("data")) {
            try {
                String data = sec.getString("data");
                ItemStack[] items = dev.shura.core.util.JsonUtil.itemsFromBase64(data);
                if (items.length > 0 && items[0] != null) {
                    ItemStack item = items[0];
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        // Add persistent data markers
                        meta.getPersistentDataContainer().set(MARKER_KEY, PersistentDataType.BYTE, (byte) 1);
                        String command = sec.getString("command", "");
                        String typeValue = command.isEmpty() ? key : (command.startsWith("/") ? command : key);
                        meta.getPersistentDataContainer().set(TYPE_KEY, PersistentDataType.STRING, typeValue);
                        item.setItemMeta(meta);
                    }
                    return item;
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load item from Base64 for key: " + key);
            }
        }
        
        // Fallback to old format (material + name + texture)
        Material mat = fallbackMaterial;
        String name = fallbackName;
        int customModelData = -1;
        String texture = null;
        String command = "";

        if (sec != null) {
            try { mat = Material.valueOf(sec.getString("material", fallbackMaterial.name()).toUpperCase()); }
            catch (IllegalArgumentException ignored) {}
            name = sec.getString("name", fallbackName);
            customModelData = sec.getInt("custom-model-data", -1);
            texture = sec.getString("texture", null);
            command = sec.getString("command", "");
        }
        return make(plugin, key, name, mat, customModelData, texture, command);
    }

    private static ItemStack make(ShuraCore plugin, String key, String name, Material material, int customModelData, String texture, String command) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(MessageService.colorizeComponent(name)
                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));

        if (customModelData > 0) meta.setCustomModelData(customModelData);

        meta.getPersistentDataContainer().set(MARKER_KEY, PersistentDataType.BYTE, (byte) 1);
        String typeValue = command.isEmpty() ? key : (command.startsWith("/") ? command : key);
        meta.getPersistentDataContainer().set(TYPE_KEY, PersistentDataType.STRING, typeValue);

        item.setItemMeta(meta);

        // Only apply texture if material is PLAYER_HEAD and texture is provided
        if (material == Material.PLAYER_HEAD && texture != null && !texture.isEmpty()) {
            applyTexture(item, texture);
        }

        return item;
    }

    private static void applyTexture(ItemStack item, String texture) {
        if (!(item.getItemMeta() instanceof SkullMeta skullMeta)) return;
        try {
            PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
            PlayerTextures textures = profile.getTextures();
            
            // Check if it's a base64 encoded texture (starts with eyJ)
            if (texture.startsWith("eyJ")) {
                // Decode base64 to get the texture URL
                String decoded = new String(java.util.Base64.getDecoder().decode(texture));
                // Extract URL from JSON: {"textures":{"SKIN":{"url":"http://..."}}} 
                int urlStart = decoded.indexOf("http");
                int urlEnd = decoded.indexOf("\"", urlStart);
                if (urlStart != -1 && urlEnd != -1) {
                    String url = decoded.substring(urlStart, urlEnd);
                    textures.setSkin(new URL(url));
                }
            } else {
                // Handle as direct URL or texture hash
                String url = texture.startsWith("http") ? texture
                        : "https://textures.minecraft.net/texture/" + texture;
                textures.setSkin(new URL(url));
            }
            
            profile.setTextures(textures);
            skullMeta.setOwnerProfile(profile);
            item.setItemMeta(skullMeta);
        } catch (Exception e) {
            // invalid texture — leave as plain head
        }
    }

    public static int getSlot(ShuraCore plugin, String key, int fallback) {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("lobby-items." + key);
        return sec != null ? sec.getInt("slot", fallback) : fallback;
    }

    public static void give(ShuraCore plugin, Player player) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, () -> give(plugin, player));
            return;
        }

        player.getInventory().clear();

        boolean inParty = plugin.getPartyManager().isInParty(player.getUniqueId());
        boolean inQueue = plugin.getQueueManager().isInQueue(player.getUniqueId());

        if (!inParty) {
            // Try to load from saved lobby items first
            if (hasItemSet(plugin, "lobby")) {
                giveItemSet(plugin, player, "lobby");
                if (inQueue) {
                    player.getInventory().setItem(8, fromConfig(plugin, "lobby-item8", Material.REDSTONE, "&#FB0000Leave Queue"));
                }
            } else {
                // Fallback to default lobby items
                player.getInventory().setItem(
                        getSlot(plugin, "cracked_queue", 0),
                        fromConfig(plugin, "cracked_queue", Material.PAPER, "&#00B4FF&lCracked Queue &7\u00bb &eRight-Click"));

                player.getInventory().setItem(
                        getSlot(plugin, "premium_queue", 1),
                        fromConfig(plugin, "premium_queue", Material.NETHER_STAR, "&#FFD700&lPremium Queue &7\u00bb &eRight-Click"));

                player.getInventory().setItem(
                        getSlot(plugin, "profile", 7),
                        fromConfig(plugin, "profile", Material.PLAYER_HEAD, "&#00B4FF&lProfile &7\u00bb &eRight-Click"));

                if (inQueue) {
                    player.getInventory().setItem(
                            getSlot(plugin, "leave_queue", 8),
                            fromConfig(plugin, "leave_queue", Material.REDSTONE, "&#FB0000Leave Queue"));
                } else {
                    player.getInventory().setItem(
                            getSlot(plugin, "lobby-item7", 8),
                            fromConfig(plugin, "lobby-item7", Material.COMPARATOR, "&#00B4FF&lSettings &7\u00bb &eRight-Click"));
                }

                player.getInventory().setItem(
                        getSlot(plugin, "party", 4),
                        fromConfig(plugin, "party", Material.FEATHER, "&#00B4FF&lParty &7\u00bb &eRight-Click"));
            }
        } else {
            var party = plugin.getPartyManager().getParty(player.getUniqueId());
            boolean isLeader = party != null && party.isLeader(player.getUniqueId());

            if (isLeader) {
                // Try to load from saved party-leader items first
                if (hasItemSet(plugin, "party-leader")) {
                    giveItemSet(plugin, player, "party-leader");
                } else {
                    // Fallback to default party leader items
                    player.getInventory().setItem(getSlot(plugin, "party-match",   0), fromConfig(plugin, "party-match",   Material.NETHER_STAR,   "&#FFD700&lParty Match &7\u00bb &eRight-Click"));
                    player.getInventory().setItem(getSlot(plugin, "party-disband",  4), fromConfig(plugin, "party-disband",  Material.TNT,           "&#FF4444&lDisband Party &7\u00bb &eRight-Click"));
                    player.getInventory().setItem(getSlot(plugin, "party-manage",   8), fromConfig(plugin, "party-manage",   Material.BOOK,          "&#FFD700&lManage Party &7\u00bb &eRight-Click"));
                }
            } else {
                // Try to load from saved party-member items first
                if (hasItemSet(plugin, "party-member")) {
                    giveItemSet(plugin, player, "party-member");
                } else {
                    // Fallback to default party member items
                    player.getInventory().setItem(getSlot(plugin, "party-spectate", 0), fromConfig(plugin, "party-spectate", Material.ENDER_EYE, "&#00B4FF&lSpectate Party &7\u00bb &eRight-Click"));
                    player.getInventory().setItem(getSlot(plugin, "party-info",     4), fromConfig(plugin, "party-info",     Material.BOOK,          "&#00B4FF&lParty Info &7\u00bb &eRight-Click"));
                    player.getInventory().setItem(getSlot(plugin, "party-leave",    6), fromConfig(plugin, "party-leave",    Material.OAK_DOOR,      "&#FF4444&lLeave Party &7\u00bb &eRight-Click"));
                }
            }
        }
    }

    private static boolean hasItemSet(ShuraCore plugin, String setName) {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("lobby-items");
        if (sec == null) return false;
        for (String key : sec.getKeys(false)) {
            if (key.startsWith(setName + "-item")) {
                return true;
            }
        }
        return false;
    }

    private static void giveItemSet(ShuraCore plugin, Player player, String setName) {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("lobby-items");
        if (sec == null) return;

        for (String key : sec.getKeys(false)) {
            if (key.startsWith(setName + "-item")) {
                ConfigurationSection itemSec = sec.getConfigurationSection(key);
                if (itemSec == null || !itemSec.contains("data")) continue;

                try {
                    String data = itemSec.getString("data");
                    int slot = itemSec.getInt("slot", 0);
                    String command = itemSec.getString("command", "");

                    ItemStack[] items = dev.shura.core.util.JsonUtil.itemsFromBase64(data);
                    if (items.length > 0 && items[0] != null) {
                        ItemStack item = items[0].clone();
                        ItemMeta meta = item.getItemMeta();
                        if (meta != null) {
                            meta.getPersistentDataContainer().set(MARKER_KEY, PersistentDataType.BYTE, (byte) 1);
                            String typeValue = command.isEmpty() ? key : (command.startsWith("/") ? command : key);
                            meta.getPersistentDataContainer().set(TYPE_KEY, PersistentDataType.STRING, typeValue);
                            item.setItemMeta(meta);
                        }
                        player.getInventory().setItem(slot, item);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to load item " + key + ": " + e.getMessage());
                }
            }
        }
    }

    public static boolean isLobbyItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(MARKER_KEY, PersistentDataType.BYTE);
    }

    private static String getItemType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return "";
        String val = item.getItemMeta().getPersistentDataContainer().get(TYPE_KEY, PersistentDataType.STRING);
        return val != null ? val : "";
    }

    public static void handleClick(ShuraCore plugin, Player player, ItemStack item) {
        if (!isLobbyItem(item)) return;

        String type = getItemType(item);
        
        // If type starts with "/", it's a command - execute it
        if (type.startsWith("/")) {
            player.performCommand(type.substring(1));
            return;
        }

        // Otherwise handle as legacy function
        switch (type) {
            case "cracked-queue", "cracked_queue" -> new dev.shura.core.gui.CrackedQueueGui(plugin).open(player);
            case "premium-queue", "premium_queue" -> {
                boolean isPremium = LuckPermsProvider.get().getUserManager().getUser(player.getUniqueId()) != null
                        && LuckPermsProvider.get().getUserManager().getUser(player.getUniqueId())
                        .getInheritedGroups(LuckPermsProvider.get().getContextManager().getStaticQueryOptions())
                        .stream().anyMatch(g -> g.getName().equalsIgnoreCase("premium"));
                if (isPremium) new dev.shura.core.gui.PremiumQueueGui(plugin).open(player);
                else player.sendMessage(MessageService.colorizeComponent("&cYou need &6Premium &cto access this queue."));
            }
            case "party", "party-manage" -> new dev.shura.core.gui.PartyGui(plugin).open(player);
            case "party-match"    -> player.sendMessage(MessageService.colorizeComponent("&cParty match coming soon."));
            case "party-disband"  -> plugin.getPartyManager().disbandParty(player);
            case "party-spectate" -> {
                // Check if party is in a match
                var party = plugin.getPartyManager().getParty(player.getUniqueId());
                if (party != null && party.isInMatch()) {
                    plugin.getSpectatorManager().spectateMatch(player, party.getMatchId());
                } else {
                    player.sendMessage(MessageService.colorizeComponent("&cYour party is not in a match."));
                }
            }
            case "party-info" -> {
                var party = plugin.getPartyManager().getParty(player.getUniqueId());
                if (party == null) {
                    player.sendMessage(MessageService.colorizeComponent("&cYou are not in a party."));
                    return;
                }
                Player leader = Bukkit.getPlayer(party.getLeader());
                String leaderName = leader != null ? leader.getName() : "Unknown";
                player.sendMessage(MessageService.colorizeComponent("&6Party Info:"));
                player.sendMessage(MessageService.colorizeComponent("&7Leader: &f" + leaderName));
                player.sendMessage(MessageService.colorizeComponent("&7Members: &f" + party.size()));
            }
            case "party-chat"     -> player.sendMessage(MessageService.colorizeComponent("&7Use &f!<message> &7to chat with your party."));
            case "party-leave"    -> plugin.getPartyManager().leaveParty(player);
            case "profile"        -> new ProfileGui(plugin).open(player);
            case "settings"       -> new SettingsGui(plugin).open(player);
            case "leave-queue", "leave_queue", "/leave", "/leavequeue"    -> {
                player.playSound(player.getLocation(), "minecraft:block.wooden_door.open", org.bukkit.SoundCategory.AMBIENT, 2.0f, 2.0f);
                plugin.getQueueManager().leaveAllQueues(player);
            }
        }
    }
}
