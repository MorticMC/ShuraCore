package dev.shura.core.event;

import dev.shura.core.match.Match;
import dev.shura.core.match.MatchPlayer;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class MatchEndEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Match match;
    private final MatchPlayer winner;
    private final MatchPlayer loser;

    public MatchEndEvent(Match match, MatchPlayer winner, MatchPlayer loser) {
        this.match = match;
        this.winner = winner;
        this.loser = loser;
    }

    public Match getMatch() { return match; }
    public MatchPlayer getWinner() { return winner; }
    public MatchPlayer getLoser() { return loser; }

    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
