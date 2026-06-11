package dev.shura.core.party;

import dev.shura.core.ShuraCore;
import dev.shura.core.match.MatchFormat;
import dev.shura.core.util.SoundUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles party-vs-party duel challenges: a party leader challenges another
 * party, whose leader accepts/denies. Mirrors {@link dev.shura.core.extra.DuelManager}
 * but operates on whole parties.
 */
public class PartyDuelManager {

    private static final long EXPIRY_TICKS = 600L; // 30s

    private final ShuraCore plugin;
    // challenged leader UUID -> challenge
    private final Map<UUID, Challenge> pending = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> expiryTasks = new ConcurrentHashMap<>();

    public PartyDuelManager(ShuraCore plugin) {
        this.plugin = plugin;
    }

    public void sendChallenge(Player senderLeader, Player target, MatchFormat format) {
        PartyManager pm = plugin.getPartyManager();
        Party senderParty = pm.getParty(senderLeader.getUniqueId());

        if (senderParty == null) { err(senderLeader, "You are not in a party."); return; }
        if (!senderParty.isLeader(senderLeader.getUniqueId())) { err(senderLeader, "Only the party leader can challenge."); return; }
        if (senderParty.isInMatch()) { err(senderLeader, "Your party is already in a match."); return; }

        Party targetParty = pm.getParty(target.getUniqueId());
        if (targetParty == null) { err(senderLeader, target.getName() + " is not in a party."); return; }
        if (targetParty.getPartyId().equals(senderParty.getPartyId())) { err(senderLeader, "You cannot challenge your own party."); return; }
        if (targetParty.isInMatch()) { err(senderLeader, "That party is already in a match."); return; }

        Player targetLeader = Bukkit.getPlayer(targetParty.getLeader());
        if (targetLeader == null) { err(senderLeader, "That party's leader is offline."); return; }

        if (pending.containsKey(targetLeader.getUniqueId())) { err(senderLeader, "That party already has a pending challenge."); return; }

        Challenge challenge = new Challenge(senderParty.getPartyId(), targetParty.getPartyId(),
                senderLeader.getUniqueId(), format);
        pending.put(targetLeader.getUniqueId(), challenge);

        senderLeader.sendMessage(Component.text("Challenge sent to ", NamedTextColor.GREEN)
                .append(Component.text(targetLeader.getName() + "'s party", NamedTextColor.AQUA))
                .append(Component.text(".", NamedTextColor.GREEN)));

        Component clickable = Component.text(senderLeader.getName() + "'s party", NamedTextColor.AQUA)
                .append(Component.text(" challenged your party to a duel! ", NamedTextColor.GREEN))
                .append(Component.text("[Accept]", NamedTextColor.YELLOW)
                        .clickEvent(ClickEvent.runCommand("/party duelaccept"))
                        .hoverEvent(HoverEvent.showText(Component.text("Click to accept", NamedTextColor.YELLOW))));
        targetParty.broadcast(clickable);
        SoundUtil.playDuelRequest(targetLeader);

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (pending.remove(targetLeader.getUniqueId()) != null) {
                expiryTasks.remove(targetLeader.getUniqueId());
                Player s = Bukkit.getPlayer(challenge.challengerLeader);
                if (s != null) s.sendMessage(Component.text("Your party challenge expired.", NamedTextColor.GRAY));
            }
        }, EXPIRY_TICKS);
        expiryTasks.put(targetLeader.getUniqueId(), task);
    }

    public void accept(Player leader) {
        Challenge challenge = pending.remove(leader.getUniqueId());
        cancelExpiry(leader.getUniqueId());
        if (challenge == null) { err(leader, "You have no pending party challenge."); return; }

        PartyManager pm = plugin.getPartyManager();
        Party challengerParty = pm.getPartyById(challenge.challengerPartyId);
        Party challengedParty = pm.getParty(leader.getUniqueId());

        if (challengerParty == null) { err(leader, "The challenging party no longer exists."); return; }
        if (challengedParty == null || !challengedParty.isLeader(leader.getUniqueId())) {
            err(leader, "Only your party leader can accept."); return;
        }

        Player challengerLeader = Bukkit.getPlayer(challenge.challengerLeader);
        plugin.getPartyMatchService().startPartyVsParty(
                challengerLeader != null ? challengerLeader : leader,
                challengerParty, challengedParty, challenge.format);
    }

    public void deny(Player leader) {
        Challenge challenge = pending.remove(leader.getUniqueId());
        cancelExpiry(leader.getUniqueId());
        if (challenge == null) { err(leader, "You have no pending party challenge."); return; }
        leader.sendMessage(Component.text("Challenge denied.", NamedTextColor.RED));
        Player challengerLeader = Bukkit.getPlayer(challenge.challengerLeader);
        if (challengerLeader != null) {
            challengerLeader.sendMessage(Component.text(leader.getName() + "'s party denied your challenge.", NamedTextColor.RED));
        }
    }

    public boolean hasPending(UUID leaderUuid) { return pending.containsKey(leaderUuid); }

    public void cleanup(UUID uuid) {
        pending.remove(uuid);
        cancelExpiry(uuid);
    }

    private void cancelExpiry(UUID uuid) {
        BukkitTask task = expiryTasks.remove(uuid);
        if (task != null) task.cancel();
    }

    private void err(Player player, String message) {
        if (player != null) player.sendMessage(Component.text(message, NamedTextColor.RED));
    }

    private record Challenge(UUID challengerPartyId, UUID challengedPartyId,
                             UUID challengerLeader, MatchFormat format) {}
}
