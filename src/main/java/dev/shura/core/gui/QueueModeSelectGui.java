package dev.shura.core.gui;

import dev.shura.core.ShuraCore;
import dev.shura.core.message.MessageService;
import dev.shura.core.util.SoundUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class QueueModeSelectGui implements Listener {

    private static final HashMap<UUID, QueueModeSelectGui> activeGuis = new HashMap<>();
    
    private final ShuraCore plugin;
    private final String gamemodeFullId;
    private final boolean isPremium;
    private final Player player;

    public QueueModeSelectGui(ShuraCore plugin, String gamemodeFullId, boolean isPremium) {
        this.plugin = plugin;
        this.gamemodeFullId = gamemodeFullId;
        this.isPremium = isPremium;
        this.player = null;
    }

    public void open(Player player) {
        GuiConfig gc = plugin.getGuiConfig();
        Inventory hopper = Bukkit.createInventory(null, org.bukkit.event.inventory.InventoryType.HOPPER, gc.queueModeSelectorTitle());

        // Ranked option
        ItemStack rankedItem = new ItemStack(gc.queueModeSelectorRankedMat());
        ItemMeta rankedMeta = rankedItem.getItemMeta();
        rankedMeta.displayName(gc.queueModeSelectorRankedName());
        List<Component> rankedLore = new ArrayList<>();
        for (String line : gc.queueModeSelectorRankedLore()) {
            rankedLore.add(MessageService.colorizeComponent(line)
                    .decoration(TextDecoration.ITALIC, false));
        }
        rankedMeta.lore(rankedLore);
        rankedMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        rankedItem.setItemMeta(rankedMeta);
        hopper.setItem(gc.queueModeSelectorRankedSlot(), rankedItem);

        // Unranked option
        ItemStack unrankedItem = new ItemStack(gc.queueModeSelectorUnrankedMat());
        ItemMeta unrankedMeta = unrankedItem.getItemMeta();
        unrankedMeta.displayName(gc.queueModeSelectorUnrankedName());
        List<Component> unrankedLore = new ArrayList<>();
        for (String line : gc.queueModeSelectorUnrankedLore()) {
            unrankedLore.add(MessageService.colorizeComponent(line)
                    .decoration(TextDecoration.ITALIC, false));
        }
        unrankedMeta.lore(unrankedLore);
        unrankedMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        unrankedItem.setItemMeta(unrankedMeta);
        hopper.setItem(gc.queueModeSelectorUnrankedSlot(), unrankedItem);

        activeGuis.put(player.getUniqueId(), this);
        player.openInventory(hopper);
        SoundUtil.playGuiOpen(player);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        QueueModeSelectGui gui = activeGuis.get(player.getUniqueId());
        if (gui == null) return;
        if (!event.getView().title().equals(gui.plugin.getGuiConfig().queueModeSelectorTitle())) return;

        event.setCancelled(true);

        int slot = event.getRawSlot();
        GuiConfig gc = gui.plugin.getGuiConfig();
        if (slot == gc.queueModeSelectorRankedSlot()) {
            // Ranked
            SoundUtil.playGuiClick(player);
            player.closeInventory();
            gui.queuePlayer(player, true);
            activeGuis.remove(player.getUniqueId());
        } else if (slot == gc.queueModeSelectorUnrankedSlot()) {
            // Unranked
            SoundUtil.playGuiClick(player);
            player.closeInventory();
            gui.queuePlayer(player, false);
            activeGuis.remove(player.getUniqueId());
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!event.getView().title().equals(plugin.getGuiConfig().queueModeSelectorTitle())) return;
        
        activeGuis.remove(player.getUniqueId());
    }

    private void queuePlayer(Player player, boolean ranked) {
        plugin.getQueueManager().joinQueue(player, gamemodeFullId, isPremium);
    }

    public static void registerListener(ShuraCore plugin) {
        Bukkit.getPluginManager().registerEvents(new QueueModeSelectGui(plugin, "", false), plugin);
    }
}
