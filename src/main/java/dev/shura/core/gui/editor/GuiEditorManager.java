package dev.shura.core.gui.editor;

import dev.shura.core.ShuraCore;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class GuiEditorManager {

    private final ShuraCore plugin;
    private final File guisFolder;
    private final Map<String, CustomGui> loadedGuis = new HashMap<>();

    public GuiEditorManager(ShuraCore plugin) {
        this.plugin = plugin;
        this.guisFolder = new File(plugin.getDataFolder(), "custom-guis");
        if (!guisFolder.exists()) guisFolder.mkdirs();
        copyDefaultGuis();
        loadAllGuis();
    }

    private void copyDefaultGuis() {
        File resourcesFolder = new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().getPath());
        try {
            java.util.jar.JarFile jar = new java.util.jar.JarFile(resourcesFolder);
            java.util.Enumeration<java.util.jar.JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                java.util.jar.JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.startsWith("custom-guis/") && name.endsWith(".yml")) {
                    String fileName = name.substring(name.lastIndexOf('/') + 1);
                    File targetFile = new File(guisFolder, fileName);
                    if (!targetFile.exists()) {
                        plugin.saveResource(name, false);
                    }
                }
            }
            jar.close();
        } catch (Exception e) {
            plugin.getLogger().info("Running in IDE mode, skipping default GUI copy");
        }
    }

    private void loadAllGuis() {
        File[] files = guisFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;
        for (File file : files) {
            String name = file.getName().replace(".yml", "");
            loadedGuis.put(name.toLowerCase(), loadGui(name));
        }
    }

    public boolean createGui(String name, GuiType guiType, String title) {
        if (loadedGuis.containsKey(name.toLowerCase())) return false;
        CustomGui gui = new CustomGui(name, guiType, title);
        loadedGuis.put(name.toLowerCase(), gui);
        saveGui(gui);
        return true;
    }

    public boolean removeGui(String name) {
        CustomGui gui = loadedGuis.remove(name.toLowerCase());
        if (gui == null) return false;
        File file = new File(guisFolder, name + ".yml");
        return file.delete();
    }

    public CustomGui getGui(String name) {
        return loadedGuis.get(name.toLowerCase());
    }

    public Set<String> getGuiNames() {
        return new HashSet<>(loadedGuis.keySet());
    }

    public void saveGui(CustomGui gui) {
        File file = new File(guisFolder, gui.getName() + ".yml");
        FileConfiguration config = new YamlConfiguration();
        config.set("name", gui.getName());
        config.set("type", gui.getGuiType().getName());
        config.set("title", gui.getTitle());
        if (gui.getCommand() != null) {
            config.set("command", gui.getCommand());
        }
        
        for (Map.Entry<Integer, ItemStack> entry : gui.getItems().entrySet()) {
            ItemStack item = entry.getValue();
            if (isVanillaItem(item)) {
                config.set("items." + entry.getKey() + ".material", item.getType().name());
                config.set("items." + entry.getKey() + ".amount", item.getAmount());
                if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                    String displayName = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(item.getItemMeta().displayName());
                    config.set("items." + entry.getKey() + ".name", displayName);
                }
                if (item.hasItemMeta() && item.getItemMeta().hasLore()) {
                    java.util.List<String> lore = new java.util.ArrayList<>();
                    for (net.kyori.adventure.text.Component line : item.getItemMeta().lore()) {
                        lore.add(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(line));
                    }
                    config.set("items." + entry.getKey() + ".lore", lore);
                }
                if (item.hasItemMeta()) {
                    org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                    if (!meta.getItemFlags().isEmpty()) {
                        java.util.List<String> flags = new java.util.ArrayList<>();
                        for (org.bukkit.inventory.ItemFlag flag : meta.getItemFlags()) {
                            flags.add(flag.name());
                        }
                        config.set("items." + entry.getKey() + ".flags", flags);
                    }
                    if (meta.hasCustomModelData()) {
                        config.set("items." + entry.getKey() + ".custom-model-data", meta.getCustomModelData());
                    }
                }
            } else {
                try {
                    String data = dev.shura.core.util.JsonUtil.itemsToBase64(new ItemStack[]{item});
                    config.set("items." + entry.getKey() + ".data", data);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to save item at slot " + entry.getKey());
                }
            }
        }
        
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save GUI: " + gui.getName());
        }
    }

    private boolean isVanillaItem(ItemStack item) {
        if (!item.hasItemMeta()) return true;
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        return !meta.hasCustomModelData() && 
               meta.getEnchants().isEmpty() && 
               !meta.isUnbreakable() &&
               !(meta instanceof org.bukkit.inventory.meta.SkullMeta);
    }

    private CustomGui loadGui(String name) {
        File file = new File(guisFolder, name + ".yml");
        if (!file.exists()) return null;
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        String typeStr = config.getString("type", "chest-3");
        GuiType guiType = GuiType.fromString(typeStr);
        if (guiType == null) guiType = GuiType.CHEST_3;
        String title = config.getString("title", name);
        CustomGui gui = new CustomGui(name, guiType, title);
        
        if (config.contains("command")) {
            gui.setCommand(config.getString("command"));
        }
        
        boolean globalHideFlags = config.getBoolean("hide-flags", false);
        
        if (config.contains("items")) {
            for (String key : config.getConfigurationSection("items").getKeys(false)) {
                try {
                    int slot = Integer.parseInt(key);
                    
                    if (config.contains("items." + key + ".data")) {
                        String data = config.getString("items." + key + ".data");
                        ItemStack[] items = dev.shura.core.util.JsonUtil.itemsFromBase64(data);
                        if (items.length > 0 && items[0] != null) {
                            ItemStack item = items[0];
                            if (globalHideFlags) applyHideAllFlags(item);
                            // Always hide all flags for GUI items
                            applyHideAllFlags(item);
                            gui.setItem(slot, item);
                        }
                    } else if (config.contains("items." + key + ".material")) {
                        String materialStr = config.getString("items." + key + ".material");
                        org.bukkit.Material material = org.bukkit.Material.getMaterial(materialStr);
                        if (material != null) {
                            int amount = config.getInt("items." + key + ".amount", 1);
                            dev.shura.core.util.ItemBuilder builder = new dev.shura.core.util.ItemBuilder(material, amount);
                            
                            String itemName = config.getString("items." + key + ".name");
                            if (itemName != null) builder.name(itemName);
                            
                            java.util.List<String> lore = config.getStringList("items." + key + ".lore");
                            if (!lore.isEmpty()) builder.lore(lore.toArray(new String[0]));
                            
                            if (config.contains("items." + key + ".custom-model-data")) {
                                int customModelData = config.getInt("items." + key + ".custom-model-data");
                                builder.customModel(customModelData);
                            }
                            
                            // Always hide all item flags for GUI items
                            builder.hideAll();
                            
                            ItemStack item = builder.build();
                            
                            java.util.List<String> flags = config.getStringList("items." + key + ".flags");
                            if (!flags.isEmpty()) {
                                applyItemFlags(item, flags);
                            } else if (globalHideFlags) {
                                applyHideAllFlags(item);
                            }
                            
                            gui.setItem(slot, item);
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to load item at slot " + key);
                }
            }
        }
        
        return gui;
    }
    
    private void applyItemFlags(ItemStack item, java.util.List<String> flags) {
        if (item == null || !item.hasItemMeta()) return;
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        
        for (String flagStr : flags) {
            if (flagStr.equalsIgnoreCase("HIDE_ALL")) {
                for (org.bukkit.inventory.ItemFlag flag : org.bukkit.inventory.ItemFlag.values()) {
                    meta.addItemFlags(flag);
                }
                break;
            }
            try {
                org.bukkit.inventory.ItemFlag flag = org.bukkit.inventory.ItemFlag.valueOf(flagStr.toUpperCase());
                meta.addItemFlags(flag);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid item flag: " + flagStr);
            }
        }
        
        item.setItemMeta(meta);
    }
    
    private void applyHideAllFlags(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return;
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        for (org.bukkit.inventory.ItemFlag flag : org.bukkit.inventory.ItemFlag.values()) {
            meta.addItemFlags(flag);
        }
        item.setItemMeta(meta);
    }

    public void openEditor(Player player, String guiName) {
        CustomGui gui = getGui(guiName);
        if (gui == null) return;
        new GuiEditorScreen(plugin, this, gui).open(player);
    }

    public void openGui(Player player, String guiName) {
        CustomGui gui = getGui(guiName);
        if (gui == null) return;
        new CustomGuiViewer(plugin, gui).open(player);
    }

    public void reloadGuis() {
        loadedGuis.clear();
        loadAllGuis();
    }
}
