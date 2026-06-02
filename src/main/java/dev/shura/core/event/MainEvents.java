package dev.shura.core.event;

import dev.shura.core.match.Match;
import dev.shura.core.match.MatchPlayer;
import dev.shura.core.party.Party;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class MainEvents {

// ==================== MATCH EVENTS ====================

public static class MatchStartEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Match match;

    public MatchStartEvent(Match match) { this.match = match; }
    public Match getMatch() { return match; }

    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}

public static class MatchDeathEvent extends Event {
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

public static class MatchRoundEndEvent extends Event {
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

public static class MatchEndEvent extends Event {
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

public static class MatchLeaveEvent extends Event {
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

// ==================== PARTY EVENTS ====================

public static class PartyCreateEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Party party;
    private final Player leader;

    public PartyCreateEvent(Party party, Player leader) {
        this.party = party;
        this.leader = leader;
    }

    public Party getParty() { return party; }
    public Player getLeader() { return leader; }

    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}

public static class PartyJoinEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Party party;
    private final Player joined;

    public PartyJoinEvent(Party party, Player joined) {
        this.party = party;
        this.joined = joined;
    }

    public Party getParty() { return party; }
    public Player getJoined() { return joined; }

    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}

public static class PartyDisbandEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Party party;

    public PartyDisbandEvent(Party party) { this.party = party; }
    public Party getParty() { return party; }

    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}

// ==================== QUEUE EVENTS ====================

public static class QueueJoinEvent extends Event {
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

public static class QueueLeaveEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final String tierlistId;

    public QueueLeaveEvent(Player player, String tierlistId) {
        this.player = player;
        this.tierlistId = tierlistId;
    }

    public Player getPlayer() { return player; }
    public String getTierlistId() { return tierlistId; }

    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}

}
