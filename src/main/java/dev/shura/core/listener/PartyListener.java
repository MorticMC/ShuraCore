package dev.shura.core.listener;

import dev.shura.core.ShuraCore;
import dev.shura.core.event.PartyCreateEvent;
import dev.shura.core.event.PartyDisbandEvent;
import dev.shura.core.event.PartyJoinEvent;
import dev.shura.core.lobby.LobbyItems;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class PartyListener implements Listener {

    private final ShuraCore plugin;

    public PartyListener(ShuraCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPartyCreate(PartyCreateEvent event) {
        Player leader = event.getLeader();
        if (leader == null || !leader.isOnline()) return;
        // 1-tick delay so playerPartyMap is fully committed before give() reads it
        Bukkit.getScheduler().runTaskLater(plugin, () -> LobbyItems.give(plugin, leader), 1L);
    }

    @EventHandler
    public void onPartyJoin(PartyJoinEvent event) {
        event.getParty().getMembers().forEach(uuid -> {
            Player member = Bukkit.getPlayer(uuid);
            if (member != null && member.isOnline())
                Bukkit.getScheduler().runTaskLater(plugin, () -> LobbyItems.give(plugin, member), 1L);
        });
    }

    @EventHandler
    public void onPartyDisband(PartyDisbandEvent event) {
        // playerPartyMap is already cleared before this event fires, so give() will see no party
        event.getParty().getMembers().forEach(uuid -> {
            Player member = Bukkit.getPlayer(uuid);
            if (member != null && member.isOnline())
                LobbyItems.give(plugin, member);
        });
    }
}
