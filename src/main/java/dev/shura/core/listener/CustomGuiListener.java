package dev.shura.core.listener;

import dev.shura.core.ShuraCore;
import dev.shura.core.gui.editor.CustomGui;
import dev.shura.core.gui.editor.GuiEditorManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class CustomGuiListener implements Listener {

    private final ShuraCore plugin;
    private final GuiEditorManager manager;

    public CustomGuiListener(ShuraCore plugin) {
        this.plugin = plugin;
        this.manager = plugin.getGuiEditorManager();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        // Handle RoundsSelectionGui
        if (event.getInventory().getHolder() instanceof dev.shura.core.gui.RoundsSelectionGui roundsGui) {
            event.setCancelled(true);
            roundsGui.handleClick(event.getSlot(), event.getClick());
            return;
        }
        
        String title = event.getView().getTitle();
        
        CustomGui matchedGui = null;
        for (String guiName : manager.getGuiNames()) {
            CustomGui gui = manager.getGui(guiName);
            if (gui == null) continue;
            
            String guiTitle = dev.shura.core.extra.MessageService.colorizeComponent(gui.getTitle()).toString();
            if (title.equals(guiTitle) || title.equals(gui.getTitle())) {
                matchedGui = gui;
                break;
            }
        }
        
        if (matchedGui == null) return;
        event.setCancelled(true);
        
        if (matchedGui.getName().equalsIgnoreCase("party-close")) {
            if (event.getSlot() == 0) {
                player.closeInventory();
            } else if (event.getSlot() == 2) {
                player.closeInventory();
                player.performCommand("party info");
            } else if (event.getSlot() == 4) {
                player.closeInventory();
                plugin.getPartyManager().disbandParty(player);
            }
            return;
        }
        
        if (matchedGui.getName().equalsIgnoreCase("party-match")) {
            if (event.getSlot() == 0) {
                player.closeInventory();
                player.sendMessage(Component.text("Party FFA coming soon!", NamedTextColor.YELLOW));
            } else if (event.getSlot() == 2) {
                player.closeInventory();
                plugin.getGuiEditorManager().openGui(player, "party-split");
            } else if (event.getSlot() == 4) {
                player.closeInventory();
                plugin.getGuiEditorManager().openGui(player, "party-vs-party");
            }
            return;
        }
        
        if (matchedGui.getName().equalsIgnoreCase("party-split")) {
            if (event.getSlot() == 1) {
                player.closeInventory();
                plugin.getGuiEditorManager().openGui(player, "teams-of-4");
            } else if (event.getSlot() == 3) {
                player.closeInventory();
                plugin.getGuiEditorManager().openGui(player, "teams-of-2");
            } else if (event.getSlot() == 5) {
                player.closeInventory();
                plugin.getGuiEditorManager().openGui(player, "teams-of-3");
            } else if (event.getSlot() == 7) {
                player.closeInventory();
                plugin.getGuiEditorManager().openGui(player, "party-match");
            }
            return;
        }
        
        if (matchedGui.getName().equalsIgnoreCase("teams-of-2") ||
            matchedGui.getName().equalsIgnoreCase("teams-of-3") ||
            matchedGui.getName().equalsIgnoreCase("teams-of-4")) {
            if (event.getSlot() == 49) {
                player.closeInventory();
                plugin.getGuiEditorManager().openGui(player, "party-split");
            }
            return;
        }
        
        if (matchedGui.getName().equalsIgnoreCase("party") && event.getSlot() == 2) {
            player.closeInventory();
            plugin.getPartyManager().createParty(player);
        }
        
        if (matchedGui.getName().equalsIgnoreCase("party-vs-party") && event.getSlot() == 49) {
            player.closeInventory();
            plugin.getGuiEditorManager().openGui(player, "party-match");
            return;
        }
        
        if (matchedGui.getName().equalsIgnoreCase("premium-queue")) {
            if (event.getSlot() == 49) {
                player.closeInventory();
                plugin.getGuiEditorManager().openGui(player, "subtiers-queue");
            } else if (event.getSlot() == 50) {
                player.closeInventory();
                player.sendMessage(Component.text("PvPTiers queue coming soon!", NamedTextColor.YELLOW));
            }
            return;
        }
        
        if (matchedGui.getName().equalsIgnoreCase("subtiers-queue") && event.getSlot() == 26) {
            player.closeInventory();
            plugin.getGuiEditorManager().openGui(player, "premium-queue");
            return;
        }
        
        if (matchedGui.getName().equalsIgnoreCase("duel-request")) {
            if (event.getSlot() == 1) {
                player.closeInventory();
                plugin.getDuelManager().acceptRequest(player);
            } else if (event.getSlot() == 3) {
                player.closeInventory();
                plugin.getDuelManager().denyRequest(player);
            }
            return;
        }
        
        if (matchedGui.getName().equalsIgnoreCase("main-menu-duels")) {
            if (event.getSlot() == 15) {
                Player target = plugin.getDuelCommand().getDuelTarget(player);
                if (target != null) {
                    player.closeInventory();
                    new dev.shura.core.gui.DuelMCTiersGui(plugin, target).open(player);
                }
            }
            return;
        }
        
        if (matchedGui.getName().equalsIgnoreCase("mctiers-duel")) {
            if (event.getSlot() == 11) {
                Player target = plugin.getDuelCommand().getDuelTarget(player);
                if (target != null) {
                    player.closeInventory();
                    
                    dev.shura.core.kit.Kit kit = plugin.getKitManager().getKit("Sword-COM");
                    if (kit != null) {
                        dev.shura.core.duel.DuelSession session = new dev.shura.core.duel.DuelSession(target, kit);
                        plugin.getDuelCommand().setDuelSession(player, session);
                        new dev.shura.core.gui.RoundsSelectionGui(plugin, player).open();
                    } else {
                        player.sendMessage(Component.text("Kit not found.", NamedTextColor.RED));
                        plugin.getDuelCommand().clearDuelTarget(player);
                    }
                }
            }
            return;
        }
        
        if (matchedGui.getName().equalsIgnoreCase("rounds-selection")) {
            if (event.getSlot() == 40) {
                dev.shura.core.duel.DuelSession session = plugin.getDuelCommand().getDuelSession(player);
                if (session == null) return;
                
                if (event.isLeftClick()) {
                    session.decrementRounds();
                    dev.shura.core.util.SoundUtil.playRoundChange(player);
                } else if (event.isRightClick()) {
                    session.incrementRounds();
                    dev.shura.core.util.SoundUtil.playRoundChange(player);
                }
                
                plugin.getGuiEditorManager().openGui(player, "rounds-selection");
            } else if (event.getSlot() == 24) {
                player.closeInventory();
                dev.shura.core.duel.DuelSession session = plugin.getDuelCommand().getDuelSession(player);
                if (session != null) {
                    plugin.getDuelManager().sendRequest(player, session.getTarget(), session.getKit(), session.getFormat());
                    plugin.getDuelCommand().clearDuelSession(player);
                }
            }
            return;
        }
    }
}
