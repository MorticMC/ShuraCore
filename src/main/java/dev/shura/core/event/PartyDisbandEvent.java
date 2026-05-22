package dev.shura.core.event;

import dev.shura.core.party.Party;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PartyDisbandEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Party party;

    public PartyDisbandEvent(Party party) { this.party = party; }
    public Party getParty() { return party; }

    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
