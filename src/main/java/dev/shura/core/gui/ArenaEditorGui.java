package dev.shura.core.gui;

import dev.shura.core.ShuraCore;
import dev.shura.core.util.GuiUtil;
import dev.shura.core.arena.Arena;
import dev.shura.core.util.GuiUtil;
import dev.shura.core.arena.ArenaValidator;
import dev.shura.core.util.GuiUtil;
import dev.shura.core.extra.MessageService;
import dev.shura.core.util.GuiUtil;
import dev.shura.core.util.SoundUtil;
import dev.shura.core.util.GuiUtil;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ArenaEditorGui {

    private final ShuraCore plugin;
    private final Arena arena;

    public ArenaEditorGui(ShuraCore plugin, Arena arena) {
        this.plugin = plugin;
        this.arena = arena;
    }

    public void open(Player player) {
        GuiConfig gc = plugin.getGuiConfig();
        Gui gui = Gui.gui()
                .title(MessageService.colorizeComponent("&#00B4FF&lArena Editor &8— &e" + arena.getName()))
                .rows(gc.arenaEditorRows())
                .disableAllInteractions()
                .create();

        gui.getFiller().fillBorder(GuiUtil.cleanItem(gc.arenaEditorBorder())
                .name(Component.empty()).asGuiItem());

        gui.setItem(2, 2, statusItem("Position 1", arena.getPos1() != null,
                arena.getPos1() != null ? formatLoc(arena.getPos1()) : "Not set — use wand left-click"));
        gui.setItem(2, 3, statusItem("Position 2", arena.getPos2() != null,
                arena.getPos2() != null ? formatLoc(arena.getPos2()) : "Not set — use wand right-click"));
        gui.setItem(2, 5, statusItem("Spawn A", arena.getSpawnA() != null,
                arena.getSpawnA() != null ? formatLoc(arena.getSpawnA()) : "Not set — /shura arena setspawn A"));
        gui.setItem(2, 6, statusItem("Spawn B", arena.getSpawnB() != null,
                arena.getSpawnB() != null ? formatLoc(arena.getSpawnB()) : "Not set — /shura arena setspawn B"));

        gui.setItem(2, 8, GuiUtil.cleanItem(arena.isEnabled() ? gc.arenaEditorEnabledMat() : gc.arenaEditorDisabledMat())
                .name(MessageService.colorizeComponent(arena.isEnabled() ? "&#00FF7F&lEnabled" : "&#FF4444&lDisabled"))
                .lore(Component.empty(), Component.text("Click to toggle", NamedTextColor.YELLOW))
                .asGuiItem(e -> {
                    arena.setEnabled(!arena.isEnabled());
                    plugin.getArenaManager().saveArena(arena);
                    SoundUtil.playGuiClick(player);
                    open(player);
                }));

        gui.setItem(3, 5, GuiUtil.cleanItem(gc.arenaEditorSaveMat())
                .name(gc.arenaEditorSaveName())
                .lore(buildValidationLore())
                .asGuiItem(e -> {
                    ArenaValidator.ValidationResult result = ArenaValidator.validate(arena);
                    if (!result.valid()) {
                        result.errors().forEach(err -> player.sendMessage(Component.text("✗ " + err, NamedTextColor.RED)));
                        SoundUtil.playError(player);
                        return;
                    }
                    plugin.getArenaManager().saveArena(arena);
                    player.sendMessage(MessageService.colorizeComponent("&aArena &e" + arena.getName() + " &asaved!"));
                    SoundUtil.playSuccess(player);
                    player.closeInventory();
                }));

        gui.setItem(3, 7, GuiUtil.cleanItem(gc.arenaEditorDeleteMat())
                .name(gc.arenaEditorDeleteName())
                .lore(Component.empty(), Component.text("This cannot be undone!", NamedTextColor.RED))
                .asGuiItem(e -> {
                    SoundUtil.playGuiClick(player);
                    new ConfirmationGui(
                            plugin,
                            "&cDelete Arena",
                            Component.text("Delete arena " + arena.getName() + "?", NamedTextColor.RED),
                            () -> {
                                plugin.getArenaManager().deleteArena(arena.getId());
                                player.sendMessage(MessageService.colorizeComponent("&cArena &e" + arena.getName() + " &cdeleted."));
                            },
                            () -> open(player)
                    ).open(player);
                }));

        gui.open(player);
        SoundUtil.playGuiOpen(player);
    }

    private dev.triumphteam.gui.guis.GuiItem statusItem(String label, boolean set, String detail) {
        Material mat = set ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
        String color = set ? "&#00FF7F" : "&#FF4444";
        String status = set ? "✔ Set" : "✘ Not Set";
        return GuiUtil.cleanItem(mat)
                .name(MessageService.colorizeComponent(color + "&l" + label))
                .lore(
                        Component.empty(),
                        Component.text("Status: ", NamedTextColor.GRAY)
                                .append(MessageService.colorizeComponent(color + status)),
                        Component.text(detail, NamedTextColor.GRAY),
                        Component.empty()
                )
                .asGuiItem(e -> {});
    }

    private List<Component> buildValidationLore() {
        ArenaValidator.ValidationResult result = ArenaValidator.validate(arena);
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        if (result.valid()) {
            lore.add(Component.text("✔ Ready to save!", NamedTextColor.GREEN));
        } else {
            result.errors().forEach(err -> lore.add(Component.text("✗ " + err, NamedTextColor.RED)));
        }
        lore.add(Component.empty());
        return lore;
    }

    private String formatLoc(org.bukkit.Location loc) {
        return String.format("%.1f, %.1f, %.1f", loc.getX(), loc.getY(), loc.getZ());
    }
}
