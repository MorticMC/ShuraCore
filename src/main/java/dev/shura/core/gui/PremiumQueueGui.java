package dev.shura.core.gui;

import dev.shura.core.ShuraCore;
import dev.shura.core.extra.KitsArenasConfig;
import dev.shura.core.extra.MessageService;
import dev.shura.core.util.GuiUtil;
import dev.shura.core.util.SoundUtil;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PremiumQueueGui {

    private final ShuraCore plugin;
    private FileConfiguration config;

    public PremiumQueueGui(ShuraCore plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        File file = new File(plugin.getDataFolder(), "custom-guis/premium-queue.yml");
        if (!file.exists()) plugin.saveResource("custom-guis/premium-queue.yml", false);
        config = YamlConfiguration.loadConfiguration(file);
    }

    public void open(Player player) {
        KitsArenasConfig kac = plugin.getKitsArenasConfig();

        List<QueueGuiUtil.GamemodeEntry> gamemodes = QueueGuiUtil.collectRankedWithUnrankedFallback(kac);

        int rows = config.getInt("gui-size", 6);
        String title = config.getString("gui-name", "Premium Queue");

        Gui gui = Gui.gui()
                .title(MessageService.colorizeComponent(title))
                .rows(rows)
                .disableAllInteractions()
                .create();

        // Primary fillers
        List<Integer> primarySlots = config.getIntegerList("fillers.primary.slots");
        Material primaryMaterial = Material.valueOf(config.getString("fillers.primary.material", "BLACK_STAINED_GLASS_PANE"));
        for (int slot : primarySlots) {
            gui.setItem(slot, ItemBuilder.from(primaryMaterial).name(Component.empty()).asGuiItem());
        }

        // Secondary fillers
        List<Integer> secondarySlots = config.getIntegerList("fillers.secondary.slots");
        Material secondaryMaterial = Material.valueOf(config.getString("fillers.secondary.material", "GRAY_STAINED_GLASS_PANE"));
        for (int slot : secondarySlots) {
            gui.setItem(slot, ItemBuilder.from(secondaryMaterial).name(Component.empty()).asGuiItem());
        }

        String defaultLore = config.getString("default-lore", "&6▟&f▙ LMB");

        // Utility items
        ConfigurationSection utilitySection = config.getConfigurationSection("utility-items");
        if (utilitySection != null) {
            for (String key : utilitySection.getKeys(false)) {
                String name = config.getString("utility-items." + key + ".name");
                int slot = config.getInt("utility-items." + key + ".slot");
                Material material = Material.valueOf(config.getString("utility-items." + key + ".material"));
                List<String> lore = config.getStringList("utility-items." + key + ".lore");
                
                List<Component> loreComponents = new ArrayList<>();
                loreComponents.add(MessageService.colorizeComponent(defaultLore).decoration(TextDecoration.ITALIC, false));
                for (String loreLine : lore) {
                    loreComponents.add(MessageService.colorizeComponent(loreLine).decoration(TextDecoration.ITALIC, false));
                }

                gui.setItem(slot, GuiUtil.cleanItem(material)
                        .name(MessageService.colorizeComponent(name).decoration(TextDecoration.ITALIC, false))
                        .lore(loreComponents)
                        .asGuiItem(e -> {
                            player.playSound(player.getLocation(), "minecraft:block.wooden_door.open", org.bukkit.SoundCategory.AMBIENT, 2.0f, 2.0f);
                            player.closeInventory();
                        }));
            }
        }

        ConfigurationSection gamemodeSection = config.getConfigurationSection("gamemode-items");
        if (gamemodeSection != null) {
            QueueGuiUtil.renderGamemodeItems(plugin, gui, config, gamemodeSection, gamemodes, kac,
                    player, true, defaultLore, NamedTextColor.GOLD, () -> open(player));
        }

        // Tierlist items
        ConfigurationSection tierlistSection = config.getConfigurationSection("tierlists");
        if (tierlistSection != null) {
            for (String key : tierlistSection.getKeys(false)) {
                String name = config.getString("tierlists." + key + ".name");
                int slot = config.getInt("tierlists." + key + ".slot");
                Material material = Material.valueOf(config.getString("tierlists." + key + ".material"));

                gui.setItem(slot, GuiUtil.cleanItem(material)
                        .name(MessageService.colorizeComponent(name).decoration(TextDecoration.ITALIC, false))
                        .lore(MessageService.colorizeComponent(defaultLore).decoration(TextDecoration.ITALIC, false))
                        .asGuiItem(e -> {
                            player.playSound(player.getLocation(), "minecraft:block.wooden_door.open", org.bukkit.SoundCategory.AMBIENT, 2.0f, 2.0f);
                            player.closeInventory();
                            if (key.equals("mctiers")) {
                                new MCTiersQueueGui(plugin, true).open(player);
                            } else if (key.equals("subtiers")) {
                                plugin.getGuiEditorManager().openGui(player, "subtiers-queue");
                            } else if (key.equals("pvptiers")) {
                                player.sendMessage(Component.text("PvPTiers queue coming soon!", NamedTextColor.YELLOW));
                            }
                        }));
            }
        }

        gui.open(player);
        player.playSound(player.getLocation(), "minecraft:ui.loom.take_result", org.bukkit.SoundCategory.MASTER, 1.0f, 2.0f);
    }

}
