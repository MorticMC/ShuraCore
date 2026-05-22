package dev.shura.core.event;

import dev.shura.core.match.Match;
import dev.shura.core.match.MatchPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class MatchRoundEndEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Match match;
    private final MatchPlayer roundWinner;
    private final MatchPlayer roundLoser;

    public MatchRoundEndEvent(Match match, MatchPlayer roundWinner, MatchPlayer roundLoser) {
        this.match = match;
        this.roundWinner = roundWinner;
        this.roundLoser = roundLoser;
    }

    public Match getMatch() { return match; }
    public MatchPlayer getRoundWinner() { return roundWinner; }
    public MatchPlayer getRoundLoser() { return roundLoser; }

    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
