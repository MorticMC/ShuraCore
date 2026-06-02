package dev.shura.core.gui;

import dev.shura.core.ShuraCore;
import dev.shura.core.extra.DuelRequest;
import dev.shura.core.extra.MessageService;
import dev.shura.core.util.SoundUtil;
import org.bukkit.entity.Player;

public class DuelRequestGui {

    private final ShuraCore plugin;
    private final DuelRequest request;

    public DuelRequestGui(ShuraCore plugin, DuelRequest request) {
        this.plugin = plugin;
        this.request = request;
    }

    public void open(Player receiver) {
        plugin.getGuiEditorManager().openGui(receiver, "duel-request");
        SoundUtil.playDuelRequest(receiver);
    }
}
