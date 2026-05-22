package dev.shura.core.party;

import dev.shura.core.ShuraCore;
import dev.shura.core.event.PartyCreateEvent;
import dev.shura.core.event.PartyDisbandEvent;
import dev.shura.core.event.PartyJoinEvent;
import dev.shura.core.util.SoundUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PartyManager {

    private final ShuraCore plugin;
    private final Map<UUID, Party> parties = new ConcurrentHashMap<>();       // partyId -> Party
    private final Map<UUID, UUID> playerPartyMap = new ConcurrentHashMap<>(); // playerUUID -> partyId

    public PartyManager(ShuraCore plugin) {
        this.plugin = plugin;
    }

    public Party createParty(Player leader) {
        if (isInParty(leader.getUniqueId())) {
            leader.sendMessage(Component.text("You are already in a party.", NamedTextColor.RED));
            return null;
        }
        Party party = new Party(leader.getUniqueId());
        parties.put(party.getPartyId(), party);
        playerPartyMap.put(leader.getUniqueId(), party.getPartyId());
        leader.sendMessage(Component.text("Party created!", NamedTextColor.GREEN));
        SoundUtil.playPartyCreate(leader);
        Bukkit.getPluginManager().callEvent(new PartyCreateEvent(party, leader));
        return party;
    }

    public void disbandParty(Player leader) {
        Party party = getParty(leader.getUniqueId());
        if (party == null) { leader.sendMessage(Component.text("You are not in a party.", NamedTextColor.RED)); return; }
        if (!party.isLeader(leader.getUniqueId())) { leader.sendMessage(Component.text("Only the party leader can disband.", NamedTextColor.RED)); return; }

        party.broadcast(Component.text("The party has been disbanded.", NamedTextColor.RED));
        party.getMembers().forEach(uuid -> playerPartyMap.remove(uuid));
        parties.remove(party.getPartyId());
        party.getMembers().forEach(uuid -> {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) SoundUtil.playError(p);
        });
        Bukkit.getPluginManager().callEvent(new PartyDisbandEvent(party));
    }

    public void invitePlayer(Player leader, Player target) {
        Party party = getParty(leader.getUniqueId());
        if (party == null) { leader.sendMessage(Component.text("You are not in a party.", NamedTextColor.RED)); return; }
        if (!party.isLeader(leader.getUniqueId())) { leader.sendMessage(Component.text("Only the party leader can invite.", NamedTextColor.RED)); return; }
        if (isInParty(target.getUniqueId())) { leader.sendMessage(Component.text(target.getName() + " is already in a party.", NamedTextColor.RED)); return; }
        if (party.hasInvite(target.getUniqueId())) { leader.sendMessage(Component.text("Already sent an invite to " + target.getName() + ".", NamedTextColor.RED)); return; }

        party.sendInvite(target.getUniqueId());
        leader.sendMessage(Component.text("Invite sent to ", NamedTextColor.GREEN).append(Component.text(target.getName(), NamedTextColor.AQUA)));
        target.sendMessage(Component.text(leader.getName(), NamedTextColor.AQUA)
                .append(Component.text(" invited you to their party! ", NamedTextColor.GREEN))
                .append(Component.text("/party accept", NamedTextColor.YELLOW)));
        SoundUtil.playPartyInvite(target);
    }

    public void acceptInvite(Player player) {
        // Find party that has an invite for this player
        Party party = parties.values().stream()
                .filter(p -> p.hasInvite(player.getUniqueId()))
                .findFirst().orElse(null);

        if (party == null) { player.sendMessage(Component.text("You have no pending party invite.", NamedTextColor.RED)); return; }
        if (isInParty(player.getUniqueId())) { player.sendMessage(Component.text("You are already in a party.", NamedTextColor.RED)); return; }

        party.acceptInvite(player.getUniqueId());
        playerPartyMap.put(player.getUniqueId(), party.getPartyId());

        party.broadcast(Component.text(player.getName(), NamedTextColor.AQUA)
                .append(Component.text(" joined the party!", NamedTextColor.GREEN)));
        SoundUtil.playSuccess(player);
        Bukkit.getPluginManager().callEvent(new PartyJoinEvent(party, player));
    }

    public void kickPlayer(Player leader, Player target) {
        Party party = getParty(leader.getUniqueId());
        if (party == null) { leader.sendMessage(Component.text("You are not in a party.", NamedTextColor.RED)); return; }
        if (!party.isLeader(leader.getUniqueId())) { leader.sendMessage(Component.text("Only the party leader can kick.", NamedTextColor.RED)); return; }
        if (leader.getUniqueId().equals(target.getUniqueId())) { leader.sendMessage(Component.text("You cannot kick yourself.", NamedTextColor.RED)); return; }
        if (!party.isMember(target.getUniqueId())) { leader.sendMessage(Component.text(target.getName() + " is not in your party.", NamedTextColor.RED)); return; }

        party.removeMember(target.getUniqueId());
        playerPartyMap.remove(target.getUniqueId());
        party.broadcast(Component.text(target.getName(), NamedTextColor.RED).append(Component.text(" was kicked from the party.", NamedTextColor.GRAY)));
        target.sendMessage(Component.text("You were kicked from the party.", NamedTextColor.RED));
        SoundUtil.playError(target);
        Bukkit.getScheduler().runTaskLater(plugin, () -> dev.shura.core.lobby.LobbyItems.give(plugin, target), 1L);
    }

    public void leaveParty(Player player) {
        Party party = getParty(player.getUniqueId());
        if (party == null) { player.sendMessage(Component.text("You are not in a party.", NamedTextColor.RED)); return; }
        if (party.isLeader(player.getUniqueId())) {
            disbandParty(player);
            return;
        }
        party.removeMember(player.getUniqueId());
        playerPartyMap.remove(player.getUniqueId());
        party.broadcast(Component.text(player.getName(), NamedTextColor.YELLOW).append(Component.text(" left the party.", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("You left the party.", NamedTextColor.GRAY));
        Bukkit.getScheduler().runTaskLater(plugin, () -> dev.shura.core.lobby.LobbyItems.give(plugin, player), 1L);
    }

    public void transferLeader(Player leader, Player newLeader) {
        Party party = getParty(leader.getUniqueId());
        if (party == null || !party.isLeader(leader.getUniqueId())) return;
        if (!party.isMember(newLeader.getUniqueId())) { leader.sendMessage(Component.text(newLeader.getName() + " is not in your party.", NamedTextColor.RED)); return; }

        party.setLeader(newLeader.getUniqueId());
        party.broadcast(Component.text(newLeader.getName(), NamedTextColor.AQUA).append(Component.text(" is now the party leader.", NamedTextColor.GREEN)));
        // Update items: old leader becomes member, new leader gets leader item
        dev.shura.core.lobby.LobbyItems.give(plugin, leader);
        dev.shura.core.lobby.LobbyItems.give(plugin, newLeader); // transfer has no event, keep here
    }

    public Party getParty(UUID playerUuid) {
        UUID partyId = playerPartyMap.get(playerUuid);
        return partyId != null ? parties.get(partyId) : null;
    }

    public boolean isInParty(UUID uuid) { return playerPartyMap.containsKey(uuid); }

    public void handleDisconnect(UUID uuid) {
        Party party = getParty(uuid);
        if (party == null) return;
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) leaveParty(player);
        else {
            party.removeMember(uuid);
            playerPartyMap.remove(uuid);
            if (party.size() == 0) parties.remove(party.getPartyId());
        }
    }
}
