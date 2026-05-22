package dev.shura.core.event;

import dev.shura.core.match.Match;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class MatchDeathEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Match match;
    private final Player died;

    public MatchDeathEvent(Match match, Player died) {
        this.match = match;
        this.died = died;
    }

    public Match getMatch() { return match; }
    public Player getDied() { return died; }

    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
