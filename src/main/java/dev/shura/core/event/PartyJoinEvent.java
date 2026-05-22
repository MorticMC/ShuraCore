package dev.shura.core.event;

import dev.shura.core.party.Party;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PartyJoinEvent extends Event {

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
