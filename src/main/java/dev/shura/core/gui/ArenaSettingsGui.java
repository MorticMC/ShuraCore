package dev.shura.core.gui;

import dev.shura.core.ShuraCore;
import dev.shura.core.arena.Arena;
import dev.shura.core.extra.MessageService;
import dev.shura.core.util.ItemBuilder;
import dev.shura.core.util.SoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class ArenaSettingsGui {

    private final ShuraCore plugin;
    private final Arena arena;

    public ArenaSettingsGui(ShuraCore plugin, Arena arena) {
        this.plugin = plugin;
        this.arena = arena;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, MessageService.colorizeComponent("&#00B4FFArena: &e" + arena.getName()));

        inv.setItem(11, new ItemBuilder(Material.REDSTONE_BLOCK)
                .name("&#FF0000Corner 1")
                .lore(arena.getPos1() != null ? "&aSet" : "&7Click to set")
                .build());

        inv.setItem(13, new ItemBuilder(Material.REDSTONE_BLOCK)
                .name("&#FF0000Corner 2")
                .lore(arena.getPos2() != null ? "&aSet" : "&7Click to set")
                .build());

        inv.setItem(15, new ItemBuilder(Material.RED_WOOL)
                .name("&#FF4444Red Spawn")
                .lore(arena.getSpawnA() != null ? "&aSet" : "&7Click to set")
                .build());

        inv.setItem(20, new ItemBuilder(Material.BLUE_WOOL)
                .name("&#4444FFBlue Spawn")
                .lore(arena.getSpawnB() != null ? "&aSet" : "&7Click to set")
                .build());

        inv.setItem(24, arena.isEnabled()
                ? new ItemBuilder(Material.LIME_DYE).name("&#00FF00Enabled").lore("&7Click to disable").build()
                : new ItemBuilder(Material.GRAY_DYE).name("&#FF0000Disabled").lore("&7Click to enable").build());

        inv.setItem(22, new ItemBuilder(Material.BARRIER)
                .name("&#FF0000Close")
                .build());

        player.openInventory(inv);
    }

    public static void handleClick(ShuraCore plugin, InventoryClickEvent event, Arena arena) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        int slot = event.getSlot();

        switch (slot) {
            case 11 -> {
                arena.setPos1(player.getLocation());
                plugin.getArenaManager().saveArena(arena);
                player.sendMessage(MessageService.colorizeComponent("&aCorner 1 set"));
                SoundUtil.playSuccess(player);
                new ArenaSettingsGui(plugin, arena).open(player);
            }
            case 13 -> {
                arena.setPos2(player.getLocation());
                plugin.getArenaManager().saveArena(arena);
                player.sendMessage(MessageService.colorizeComponent("&aCorner 2 set"));
                SoundUtil.playSuccess(player);
                new ArenaSettingsGui(plugin, arena).open(player);
            }
            case 15 -> {
                arena.setSpawnA(player.getLocation());
                plugin.getArenaManager().saveArena(arena);
                player.sendMessage(MessageService.colorizeComponent("&aRed spawn set"));
                SoundUtil.playSuccess(player);
                new ArenaSettingsGui(plugin, arena).open(player);
            }
            case 20 -> {
                arena.setSpawnB(player.getLocation());
                plugin.getArenaManager().saveArena(arena);
                player.sendMessage(MessageService.colorizeComponent("&aBlue spawn set"));
                SoundUtil.playSuccess(player);
                new ArenaSettingsGui(plugin, arena).open(player);
            }
            case 24 -> {
                arena.setEnabled(!arena.isEnabled());
                plugin.getArenaManager().saveArena(arena);
                player.sendMessage(MessageService.colorizeComponent(arena.isEnabled() 
                        ? "&aArena enabled"
                        : "&cArena disabled"));
                SoundUtil.playSuccess(player);
                new ArenaSettingsGui(plugin, arena).open(player);
            }
            case 22 -> {
                player.closeInventory();
                SoundUtil.playSuccess(player);
            }
        }
    }
}
