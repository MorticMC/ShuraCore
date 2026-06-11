package dev.shura.core.party;

import dev.shura.core.ShuraCore;
import dev.shura.core.arena.Arena;
import dev.shura.core.arena.ArenaCopy;
import dev.shura.core.kit.Kit;
import dev.shura.core.match.MatchFormat;
import dev.shura.core.match.MatchType;
import dev.shura.core.util.SoundUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Builds the teams and launches a {@link dev.shura.core.match.TeamMatch} for the
 * two party game-modes:
 * <ul>
 *   <li><b>Party split</b> — the leader splits their own party into Red/Blue.</li>
 *   <li><b>Party vs party</b> — two separate parties fight (Red = challenger).</li>
 * </ul>
 */
public class PartyMatchService {

    private final ShuraCore plugin;

    public PartyMatchService(ShuraCore plugin) {
        this.plugin = plugin;
    }

    /** Splits the leader's party into two alternating teams and starts the match. */
    public void startSplit(Player leader, MatchFormat format) {
        Party party = plugin.getPartyManager().getParty(leader.getUniqueId());
        if (party == null) { error(leader, "You are not in a party."); return; }
        if (!party.isLeader(leader.getUniqueId())) { error(leader, "Only the party leader can split the party."); return; }
        if (party.isInMatch()) { error(leader, "Your party is already in a match."); return; }

        List<Player> online = onlineMembers(party);
        if (online.size() < 2) { error(leader, "You need at least 2 online members to split."); return; }

        // Alternate members into Red/Blue (leader is first → Red)
        List<Player> red = new ArrayList<>();
        List<Player> blue = new ArrayList<>();
        for (int i = 0; i < online.size(); i++) {
            (i % 2 == 0 ? red : blue).add(online.get(i));
        }

        launch(leader, red, blue, format);
    }

    /** Starts a Red (challenger) vs Blue (challenged) match between two parties. */
    public void startPartyVsParty(Player challengerLeader, Party challenger, Party challenged, MatchFormat format) {
        if (challenger == null || challenged == null) { error(challengerLeader, "A party is no longer valid."); return; }
        if (challenger.isInMatch() || challenged.isInMatch()) { error(challengerLeader, "One of the parties is already in a match."); return; }

        List<Player> red = onlineMembers(challenger);
        List<Player> blue = onlineMembers(challenged);
        if (red.isEmpty() || blue.isEmpty()) { error(challengerLeader, "Both parties need online members."); return; }

        launch(challengerLeader, red, blue, format);
    }

    private void launch(Player initiator, List<Player> red, List<Player> blue, MatchFormat format) {
        Kit kit = resolveKit();
        if (kit == null) { error(initiator, "No kits are configured for matches."); return; }

        Arena arena = plugin.getArenaManager().getAvailableArenaForKit(kit.getId());
        if (arena == null) { error(initiator, "No arenas available right now. Please try again shortly."); return; }

        ArenaCopy copy = plugin.getArenaManager().getOrCreateCopy(arena);
        if (copy == null) { error(initiator, "Arenas are warming up. Please try again in a moment."); return; }

        for (Player p : red) SoundUtil.playMatchFound(p);
        for (Player p : blue) SoundUtil.playMatchFound(p);

        plugin.getTeamMatchManager().createMatch(red, blue, kit, arena, copy, MatchType.UNRANKED, format);
    }

    /** Picks the first kit that currently has an available arena, falling back to any kit. */
    private Kit resolveKit() {
        Kit fallback = null;
        for (Kit kit : plugin.getKitManager().getAllKits()) {
            if (fallback == null) fallback = kit;
            if (plugin.getArenaManager().getAvailableArenaForKit(kit.getId()) != null) {
                return kit;
            }
        }
        return fallback;
    }

    private List<Player> onlineMembers(Party party) {
        List<Player> list = new ArrayList<>();
        for (UUID uuid : party.getMembers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()
                    && !plugin.getMatchManager().isInMatch(uuid)
                    && !plugin.getTeamMatchManager().isInMatch(uuid)) {
                list.add(p);
            }
        }
        return list;
    }

    private void error(Player player, String message) {
        if (player != null) player.sendMessage(Component.text(message, NamedTextColor.RED));
    }
}
