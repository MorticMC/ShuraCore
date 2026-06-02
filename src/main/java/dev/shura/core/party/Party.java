package dev.shura.core.party;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

public class Party {

    private final UUID partyId;
    private UUID leader;
    private final Set<UUID> members = new LinkedHashSet<>();
    private final Map<UUID, Long> pendingInvites = new HashMap<>(); // uuid -> expiry timestamp
    private boolean open; // open parties allow anyone to join without invite
    private UUID matchId; // current match ID if in a match

    private static final long INVITE_EXPIRY_MS = 30_000L;

    public Party(UUID leader) {
        this.partyId = UUID.randomUUID();
        this.leader = leader;
        this.members.add(leader);
    }

    public void addMember(UUID uuid) { members.add(uuid); }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
        pendingInvites.remove(uuid);
    }

    public void sendInvite(UUID uuid) {
        pendingInvites.put(uuid, System.currentTimeMillis() + INVITE_EXPIRY_MS);
    }

    public boolean hasInvite(UUID uuid) {
        Long expiry = pendingInvites.get(uuid);
        if (expiry == null) return false;
        if (System.currentTimeMillis() > expiry) {
            pendingInvites.remove(uuid);
            return false;
        }
        return true;
    }

    public void acceptInvite(UUID uuid) {
        pendingInvites.remove(uuid);
        members.add(uuid);
    }

    public void broadcast(net.kyori.adventure.text.Component message) {
        members.forEach(uuid -> {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.sendMessage(message);
        });
    }

    public boolean isLeader(UUID uuid) { return leader.equals(uuid); }
    public boolean isMember(UUID uuid) { return members.contains(uuid); }
    public int size() { return members.size(); }

    public UUID getPartyId() { return partyId; }
    public UUID getLeader() { return leader; }
    public void setLeader(UUID leader) { this.leader = leader; }
    public Set<UUID> getMembers() { return Collections.unmodifiableSet(members); }
    public boolean isOpen() { return open; }
    public void setOpen(boolean open) { this.open = open; }
    public UUID getMatchId() { return matchId; }
    public void setMatchId(UUID matchId) { this.matchId = matchId; }
    public boolean isInMatch() { return matchId != null; }
}
