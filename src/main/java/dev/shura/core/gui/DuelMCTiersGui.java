package dev.shura.core.gui;

import dev.shura.core.ShuraCore;
import org.bukkit.entity.Player;

public class DuelMCTiersGui {

    private final ShuraCore plugin;
    private final Player target;

    public DuelMCTiersGui(ShuraCore plugin, Player target) {
        this.plugin = plugin;
        this.target = target;
    }

    public void open(Player player) {
        plugin.getGuiEditorManager().openGui(player, "mctiers-duel");
    }
}
