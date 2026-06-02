package dev.shura.core.gui;

import dev.shura.core.ShuraCore;
import dev.shura.core.arena.Arena;
import dev.shura.core.extra.MessageService;
import dev.shura.core.util.GuiUtil;
import dev.shura.core.util.SoundUtil;
import dev.triumphteam.gui.guis.Gui;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class ArenaMenuGui {

    private final ShuraCore plugin;
    private final Arena arena;

    public ArenaMenuGui(ShuraCore plugin, Arena arena) {
        this.plugin = plugin;
        this.arena = arena;
    }

    public void open(Player player) {
        Gui gui = Gui.gui()
                .title(MessageService.colorizeComponent("&eArena Menu: &f" + arena.getName()))
                .rows(3)
                .disableAllInteractions()
                .create();

        gui.getFiller().fill(GuiUtil.cleanItem(Material.BLACK_STAINED_GLASS_PANE).name(Component.empty()).asGuiItem());

        gui.setItem(10, GuiUtil.cleanItem(Material.CHAIN)
                .name(Component.text("Binds", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false))
                .lore(Component.text("Click to manage kit bindings", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                .asGuiItem(e -> {
                    player.playSound(player.getLocation(), "minecraft:block.wooden_door.open", org.bukkit.SoundCategory.AMBIENT, 2.0f, 2.0f);
                    new ArenaBindsGui(plugin, arena).open(player);
                }));

        gui.setItem(13, GuiUtil.cleanItem(Material.PAPER)
                .name(Component.text("Copy Arena", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false))
                .lore(
                    Component.text("Click to copy this arena", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("Stand where you want to paste", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("and it will create a new arena", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                )
                .asGuiItem(e -> {
                    if (arena.getPos1() == null || arena.getPos2() == null || 
                        arena.getSpawnA() == null || arena.getSpawnB() == null) {
                        player.sendMessage(MessageService.colorizeComponent("&cArena must have all positions set!"));
                        SoundUtil.playError(player);
                        return;
                    }
                    player.closeInventory();
                    plugin.getArenaManager().copyAndPasteArena(arena, player.getLocation(), player);
                }));

        gui.setItem(16, GuiUtil.cleanItem(Material.BARRIER)
                .name(Component.text("Close", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false))
                .asGuiItem(e -> {
                    player.closeInventory();
                }));

        gui.open(player);
        player.playSound(player.getLocation(), "minecraft:ui.loom.take_result", org.bukkit.SoundCategory.MASTER, 1.0f, 2.0f);
    }
}
