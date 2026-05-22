package dev.shura.core.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class QueueJoinEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final String tierlistId;

    public QueueJoinEvent(Player player, String tierlistId) {
        this.player = player;
        this.tierlistId = tierlistId;
    }

    public Player getPlayer() { return player; }
    public String getTierlistId() { return tierlistId; }

    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
