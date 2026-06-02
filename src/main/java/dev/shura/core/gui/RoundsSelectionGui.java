package dev.shura.core.gui;

import dev.shura.core.ShuraCore;
import dev.shura.core.duel.DuelSession;
import dev.shura.core.gui.editor.CustomGui;
import dev.shura.core.util.SoundUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class RoundsSelectionGui implements InventoryHolder {

    private final ShuraCore plugin;
    private final Player player;
    private final Inventory inventory;
    private final CustomGui customGui;

    public RoundsSelectionGui(ShuraCore plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.customGui = plugin.getGuiEditorManager().getGui("rounds-selection");
        this.inventory = plugin.getServer().createInventory(this, 54, 
            dev.shura.core.extra.MessageService.colorizeComponent(customGui.getTitle()));
        setupItems();
    }

    private void setupItems() {
        // Load all items from custom GUI
        for (int i = 0; i < 54; i++) {
            ItemStack item = customGui.getItem(i);
            if (item != null) {
                inventory.setItem(i, item.clone());
            }
        }
        
        // Update rounds item dynamically
        DuelSession session = plugin.getDuelCommand().getDuelSession(player);
        if (session != null) {
            int rounds = session.getCustomRounds();
            ItemStack roundsItem = inventory.getItem(40);
            if (roundsItem != null) {
                roundsItem.setAmount(rounds);
                // Update lore with current rounds
                if (roundsItem.hasItemMeta()) {
                    var meta = roundsItem.getItemMeta();
                    if (meta.hasLore()) {
                        var lore = meta.lore();
                        if (lore != null && lore.size() > 3) {
                            lore.set(3, dev.shura.core.extra.MessageService.colorizeComponent(
                                "§r&7Current: &e" + rounds + " &7Round" + (rounds > 1 ? "s" : "")));
                            meta.lore(lore);
                            roundsItem.setItemMeta(meta);
                        }
                    }
                }
            }
        }
    }

    public void handleClick(int slot, ClickType clickType) {
        DuelSession session = plugin.getDuelCommand().getDuelSession(player);
        if (session == null) return;

        if (slot == 40) {
            if (clickType == ClickType.LEFT) {
                session.incrementRounds();
                SoundUtil.playRoundChange(player);
            } else if (clickType == ClickType.RIGHT) {
                session.decrementRounds();
                SoundUtil.playRoundChange(player);
            }
            setupItems();
        } else if (slot == 24) {
            player.closeInventory();
            plugin.getDuelManager().sendRequest(player, session.getTarget(), session.getKit(), session.getFormat());
            plugin.getDuelCommand().clearDuelSession(player);
        }
    }

    public void open() {
        player.openInventory(inventory);
        SoundUtil.playGuiOpen(player);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
