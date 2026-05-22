package dev.shura.core.gui;

import dev.shura.core.ShuraCore;
import dev.shura.core.config.KitsArenasConfig;
import dev.shura.core.message.MessageService;
import dev.shura.core.util.GuiUtil;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
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

public class MCTiersQueueGui {

    private static final String TIERLIST_KEY = "mctiers";

    private final ShuraCore plugin;
    private final boolean premium;
    private FileConfiguration config;

    public MCTiersQueueGui(ShuraCore plugin, boolean premium) {
        this.plugin = plugin;
        this.premium = premium;
        loadConfig();
    }

    private void loadConfig() {
        File file = new File(plugin.getDataFolder(), "mctiers-queue-gui.yml");
        if (!file.exists()) plugin.saveResource("mctiers-queue-gui.yml", false);
        config = YamlConfiguration.loadConfiguration(file);
    }

    public void open(Player player) {
        KitsArenasConfig kac = plugin.getKitsArenasConfig();

        int rows = config.getInt("gui-size", 4);
        String title = config.getString("gui-name", "MCTiers Queue");

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

        List<String> defaultLoreList = config.getStringList("default-lore");
        List<Component> defaultLore = new ArrayList<>();
        for (String line : defaultLoreList) {
            defaultLore.add(MessageService.colorizeComponent(line).decoration(TextDecoration.ITALIC, false));
        }

        // Go back button
        String backName = config.getString("go-back.name");
        int backSlot = config.getInt("go-back.slot");
        Material backMaterial = Material.valueOf(config.getString("go-back.material"));

        gui.setItem(backSlot, GuiUtil.cleanItem(backMaterial)
                .name(MessageService.colorizeComponent(backName).decoration(TextDecoration.ITALIC, false))
                .asGuiItem(e -> {
                    player.playSound(player.getLocation(), "minecraft:block.wooden_door.open", org.bukkit.SoundCategory.AMBIENT, 2.0f, 2.0f);
                    if (premium) {
                        new PremiumQueueGui(plugin).open(player);
                    } else {
                        new CrackedQueueGui(plugin).open(player);
                    }
                }));

        // Tierlist info
        String tierlistName = config.getString("tierlist-info.name");
        int tierlistSlot = config.getInt("tierlist-info.slot");
        Material tierlistMaterial = Material.valueOf(config.getString("tierlist-info.material"));

        gui.setItem(tierlistSlot, GuiUtil.cleanItem(tierlistMaterial)
                .name(MessageService.colorizeComponent(tierlistName).decoration(TextDecoration.ITALIC, false))
                .asGuiItem(e -> player.playSound(player.getLocation(), "minecraft:block.wooden_door.open", org.bukkit.SoundCategory.AMBIENT, 2.0f, 2.0f)));;

        // Gamemode items
        ConfigurationSection gamemodeSection = config.getConfigurationSection("gamemodes");
        if (gamemodeSection != null) {
            for (String key : gamemodeSection.getKeys(false)) {
                QueueGuiUtil.GamemodeEntry entry = QueueGuiUtil.resolveForTierlist(kac, TIERLIST_KEY, key, true);
                if (entry == null) continue;

                String name = config.getString("gamemodes." + key + ".name");
                int slot = config.getInt("gamemodes." + key + ".slot");
                Material material = Material.valueOf(config.getString("gamemodes." + key + ".material"));

                boolean inQueue = plugin.getQueueManager().isInQueue(player.getUniqueId(), entry.fullId(), premium);

                List<Component> lore = new ArrayList<>(defaultLore);
                lore.add(Component.empty());
                lore.add(Component.text("Mode: ", NamedTextColor.GRAY)
                        .append(Component.text(entry.fullId(), NamedTextColor.WHITE))
                        .decoration(TextDecoration.ITALIC, false));
                lore.add(Component.empty());
                if (inQueue) {
                    lore.add(Component.text("✓ In Queue", NamedTextColor.GREEN)
                            .decoration(TextDecoration.ITALIC, false));
                    lore.add(Component.text("Click to leave!", NamedTextColor.RED)
                            .decoration(TextDecoration.ITALIC, false));
                } else {
                    lore.add(Component.text("Click to queue!", NamedTextColor.YELLOW)
                            .decoration(TextDecoration.ITALIC, false));
                }

                var itemBuilder = GuiUtil.cleanItem(material)
                        .name(MessageService.colorizeComponent(name).decoration(TextDecoration.ITALIC, false))
                        .lore(lore);

                if (inQueue) {
                    itemBuilder.glow(true);
                }

                QueueGuiUtil.GamemodeEntry finalEntry = entry;
                GuiItem item = itemBuilder.asGuiItem(e -> {
                    player.playSound(player.getLocation(), "minecraft:block.wooden_door.open", org.bukkit.SoundCategory.AMBIENT, 2.0f, 2.0f);
                    boolean wasInQueue = plugin.getQueueManager().joinQueue(player, finalEntry.fullId(), premium);
                    if (wasInQueue) {
                        open(player);
                    }
                });
                gui.setItem(slot, item);
            }
        }

        gui.open(player);
        player.playSound(player.getLocation(), "minecraft:ui.loom.take_result", org.bukkit.SoundCategory.MASTER, 1.0f, 2.0f);
    }
}
