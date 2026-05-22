package dev.shura.core.event;

import dev.shura.core.party.Party;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PartyCreateEvent extends Event {

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
