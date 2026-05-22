package dev.shura.core.gui;

import dev.shura.core.ShuraCore;
import dev.shura.core.config.KitsArenasConfig;
import dev.shura.core.message.MessageService;
import dev.shura.core.util.GuiUtil;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class QueueGuiUtil {

    public record GamemodeEntry(String tierlistKey, String gamemodeKey, String name, Material material, String fullId) {}

    private static final Map<String, List<String>> GUI_ALIASES = new LinkedHashMap<>();

    static {
        alias("sword", "sword");
        alias("axe", "axe");
        alias("mace", "mace");
        alias("netherite-op", "netherite", "nethop");
        alias("netherite", "netherite", "nethop");
        alias("uhc", "uhc");
        alias("smp", "smp");
        alias("crystal", "crystal", "vanilla");
        alias("vanilla", "vanilla", "crystal");
        alias("cart", "cart", "diamondcart", "ltcart", "htcart");
        alias("potion", "potion", "diapot");
        alias("custom-player-kits", "custom");
    }

    private QueueGuiUtil() {}

    private static void alias(String guiKey, String... keys) {
        GUI_ALIASES.put(guiKey, List.of(keys));
    }

    public static List<String> aliasesFor(String guiKey) {
        return GUI_ALIASES.getOrDefault(guiKey, List.of(guiKey));
    }

    public static List<GamemodeEntry> collectGamemodes(KitsArenasConfig kac, boolean ranked) {
        List<GamemodeEntry> gamemodes = new ArrayList<>();
        for (String tierlistKey : kac.getTierlistIds()) {
            List<String> keys = ranked
                    ? kac.getRankedGamemodes(tierlistKey)
                    : kac.getUnrankedGamemodes(tierlistKey);
            for (String gamemodeKey : keys) {
                gamemodes.add(new GamemodeEntry(
                        tierlistKey,
                        gamemodeKey,
                        kac.getGamemodeName(tierlistKey, gamemodeKey),
                        kac.getGamemodeMaterial(tierlistKey, gamemodeKey),
                        kac.getGamemodeFullId(tierlistKey, gamemodeKey)
                ));
            }
        }
        return gamemodes;
    }

    public static List<GamemodeEntry> collectRankedWithUnrankedFallback(KitsArenasConfig kac) {
        List<GamemodeEntry> gamemodes = collectGamemodes(kac, true);
        if (gamemodes.isEmpty()) {
            gamemodes = collectGamemodes(kac, false);
        }
        return gamemodes;
    }

    public static GamemodeEntry resolve(List<GamemodeEntry> gamemodes, String guiKey, String preferredTierlist) {
        List<String> aliases = aliasesFor(guiKey);
        if (preferredTierlist != null) {
            for (GamemodeEntry entry : gamemodes) {
                if (!entry.tierlistKey.equalsIgnoreCase(preferredTierlist)) continue;
                if (matches(entry, aliases)) return entry;
            }
        }
        for (GamemodeEntry entry : gamemodes) {
            if (matches(entry, aliases)) return entry;
        }
        return null;
    }

    public static GamemodeEntry resolveForTierlist(KitsArenasConfig kac, String tierlistKey, String guiKey, boolean ranked) {
        List<String> aliases = aliasesFor(guiKey);
        List<String> keys = ranked
                ? kac.getRankedGamemodes(tierlistKey)
                : kac.getUnrankedGamemodes(tierlistKey);
        if (keys.isEmpty() && ranked) {
            keys = kac.getUnrankedGamemodes(tierlistKey);
        }
        for (String gamemodeKey : keys) {
            GamemodeEntry entry = new GamemodeEntry(
                    tierlistKey,
                    gamemodeKey,
                    kac.getGamemodeName(tierlistKey, gamemodeKey),
                    kac.getGamemodeMaterial(tierlistKey, gamemodeKey),
                    kac.getGamemodeFullId(tierlistKey, gamemodeKey)
            );
            if (matches(entry, aliases)) return entry;
        }
        return null;
    }

    private static boolean matches(GamemodeEntry entry, List<String> aliases) {
        for (String alias : aliases) {
            if (entry.gamemodeKey.equalsIgnoreCase(alias) || entry.name.equalsIgnoreCase(alias)) {
                return true;
            }
        }
        return false;
    }

    public static void renderGamemodeItems(
            ShuraCore plugin,
            Gui gui,
            FileConfiguration config,
            ConfigurationSection gamemodeSection,
            List<GamemodeEntry> gamemodes,
            KitsArenasConfig kac,
            Player player,
            boolean premium,
            String defaultLore,
            NamedTextColor tierlistColor,
            Runnable refreshGui
    ) {
        for (String key : gamemodeSection.getKeys(false)) {
            String path = "gamemode-items." + key;
            String name = config.getString(path + ".name");
            if (name == null || name.isBlank()) continue;

            int slot = config.getInt(path + ".slot");
            Material material = Material.valueOf(config.getString(path + ".material", "PAPER"));
            boolean queueEnabled = config.getBoolean(path + ".queue-enabled", !"custom-player-kits".equals(key));
            List<String> extraLore = config.getStringList(path + ".lore");

            GamemodeEntry matchingEntry = queueEnabled ? resolve(gamemodes, key, "mctiers") : null;
            if (queueEnabled && matchingEntry == null) continue;

            List<Component> lore = new ArrayList<>();
            lore.add(MessageService.colorizeComponent(defaultLore).decoration(TextDecoration.ITALIC, false));
            for (String line : extraLore) {
                lore.add(MessageService.colorizeComponent(line).decoration(TextDecoration.ITALIC, false));
            }

            var itemBuilder = GuiUtil.cleanItem(material)
                    .name(MessageService.colorizeComponent(name).decoration(TextDecoration.ITALIC, false));

            if (matchingEntry != null) {
                lore.add(Component.empty());
                lore.add(Component.text("Tierlist: ", NamedTextColor.GRAY)
                        .append(Component.text(kac.getTierlistId(matchingEntry.tierlistKey()), tierlistColor))
                        .decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("Mode: ", NamedTextColor.GRAY)
                        .append(Component.text(matchingEntry.fullId(), NamedTextColor.WHITE))
                        .decoration(TextDecoration.ITALIC, false));
                lore.add(Component.empty());

                boolean inQueue = plugin.getQueueManager().isInQueue(
                        player.getUniqueId(), matchingEntry.fullId(), premium);
                if (inQueue) {
                    lore.add(Component.text("✓ In Queue", NamedTextColor.GREEN)
                            .decoration(TextDecoration.ITALIC, false));
                    lore.add(Component.text("Click to leave!", NamedTextColor.RED)
                            .decoration(TextDecoration.ITALIC, false));
                    itemBuilder.glow(true);
                } else {
                    lore.add(Component.text("Click to queue!", NamedTextColor.YELLOW)
                            .decoration(TextDecoration.ITALIC, false));
                }

                GamemodeEntry finalEntry = matchingEntry;
                itemBuilder.lore(lore);
                GuiItem item = itemBuilder.asGuiItem(e -> {
                    player.playSound(player.getLocation(), "minecraft:block.wooden_door.open",
                            org.bukkit.SoundCategory.AMBIENT, 2.0f, 2.0f);
                    boolean wasInQueue = plugin.getQueueManager().joinQueue(
                            player, finalEntry.fullId(), premium);
                    if (wasInQueue && refreshGui != null) {
                        refreshGui.run();
                    }
                });
                gui.setItem(slot, item);
            } else {
                itemBuilder.lore(lore);
                gui.setItem(slot, itemBuilder.asGuiItem(e ->
                        player.playSound(player.getLocation(), "minecraft:block.wooden_door.open",
                                org.bukkit.SoundCategory.AMBIENT, 2.0f, 2.0f)));
            }
        }
    }
}
