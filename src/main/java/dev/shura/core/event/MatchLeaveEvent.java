package dev.shura.core.event;

import dev.shura.core.match.Match;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class MatchLeaveEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Match match;
    private final Player left;

    public MatchLeaveEvent(Match match, Player left) {
        this.match = match;
        this.left = left;
    }

    public Match getMatch() { return match; }
    public Player getLeft() { return left; }

    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
