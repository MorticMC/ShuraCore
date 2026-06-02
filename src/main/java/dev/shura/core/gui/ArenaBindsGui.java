package dev.shura.core.gui;

import dev.shura.core.ShuraCore;
import dev.shura.core.arena.Arena;
import dev.shura.core.extra.MessageService;
import dev.shura.core.kit.Kit;
import dev.shura.core.util.GuiUtil;
import dev.triumphteam.gui.guis.PaginatedGui;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class ArenaBindsGui {

    private final ShuraCore plugin;
    private final Arena arena;

    public ArenaBindsGui(ShuraCore plugin, Arena arena) {
        this.plugin = plugin;
        this.arena = arena;
    }

    public void open(Player player) {
        open(player, 1);
    }

    public void open(Player player, int page) {
        PaginatedGui gui = dev.triumphteam.gui.guis.Gui.paginated()
                .title(MessageService.colorizeComponent("&eBinds: &f" + arena.getName()))
                .rows(6)
                .pageSize(28)
                .disableAllInteractions()
                .create();

        gui.getFiller().fillBorder(GuiUtil.cleanItem(Material.BLACK_STAINED_GLASS_PANE).name(Component.empty()).asGuiItem());

        var kac = plugin.getKitsArenasConfig();
        for (String tierlistKey : kac.getTierlistIds()) {
            for (String gamemodeKey : kac.getGamemodeKeys(tierlistKey)) {
                String name = kac.getGamemodeName(tierlistKey, gamemodeKey);
                String fullId = kac.getGamemodeFullId(tierlistKey, gamemodeKey);
                Material material = kac.getGamemodeMaterial(tierlistKey, gamemodeKey);
                
                boolean bound = arena.isBoundToKit(fullId);
                Material woolMaterial = bound ? Material.GREEN_WOOL : Material.RED_WOOL;

                gui.addItem(GuiUtil.cleanItem(woolMaterial)
                        .name(Component.text(name, bound ? NamedTextColor.GREEN : NamedTextColor.RED)
                                .decoration(TextDecoration.ITALIC, false))
                        .lore(
                                Component.empty(),
                                Component.text("ID: " + fullId, NamedTextColor.GRAY)
                                        .decoration(TextDecoration.ITALIC, false),
                                Component.text("Status: " + (bound ? "Bound" : "Not Bound"), NamedTextColor.GRAY)
                                        .decoration(TextDecoration.ITALIC, false),
                                Component.text("Click to " + (bound ? "unbind" : "bind"), NamedTextColor.YELLOW)
                                        .decoration(TextDecoration.ITALIC, false)
                        )
                        .asGuiItem(e -> {
                            int currentPage = gui.getCurrentPageNum();
                            if (bound) {
                                arena.removeBoundKit(fullId);
                            } else {
                                arena.addBoundKit(fullId);
                            }
                            plugin.getArenaManager().saveArena(arena);
                            player.playSound(player.getLocation(), "minecraft:block.note_block.pling", org.bukkit.SoundCategory.MASTER, 1.0f, bound ? 0.5f : 2.0f);
                            open(player, currentPage);
                        }));
            }
        }

        gui.setItem(6, 3, GuiUtil.cleanItem(Material.ARROW)
                .name(Component.text("Previous", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false))
                .asGuiItem(e -> {
                    gui.previous();
                    player.playSound(player.getLocation(), "minecraft:ui.button.click", org.bukkit.SoundCategory.MASTER, 1.0f, 1.0f);
                }));

        gui.setItem(6, 5, GuiUtil.cleanItem(Material.BARRIER)
                .name(Component.text("Back", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false))
                .asGuiItem(e -> {
                    player.playSound(player.getLocation(), "minecraft:block.wooden_door.open", org.bukkit.SoundCategory.AMBIENT, 2.0f, 2.0f);
                    new ArenaMenuGui(plugin, arena).open(player);
                }));

        gui.setItem(6, 7, GuiUtil.cleanItem(Material.ARROW)
                .name(Component.text("Next", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false))
                .asGuiItem(e -> {
                    gui.next();
                    player.playSound(player.getLocation(), "minecraft:ui.button.click", org.bukkit.SoundCategory.MASTER, 1.0f, 1.0f);
                }));

        gui.open(player, page);
        player.playSound(player.getLocation(), "minecraft:ui.loom.take_result", org.bukkit.SoundCategory.MASTER, 1.0f, 2.0f);
    }
}
