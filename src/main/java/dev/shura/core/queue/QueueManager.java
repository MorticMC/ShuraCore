package dev.shura.core.queue;

import dev.shura.core.ShuraCore;
import dev.shura.core.arena.Arena;
import dev.shura.core.arena.ArenaCopy;
import dev.shura.core.event.MainEvents.QueueJoinEvent;
import dev.shura.core.event.MainEvents.QueueLeaveEvent;
import dev.shura.core.kit.Kit;
import dev.shura.core.match.MatchFormat;
import dev.shura.core.match.MatchType;
import dev.shura.core.profile.Profile;
import dev.shura.core.tierlist.Tierlist;
import dev.shura.core.util.SoundUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class QueueManager {

    private final ShuraCore plugin;
    // tierlistId -> queue of entries
    private final Map<String, ConcurrentLinkedQueue<QueueEntry>> queues = new HashMap<>();
    // playerUUID -> set of entries (for multiple queues)
    private final Map<UUID, Set<QueueEntry>> playerQueueMap = new HashMap<>();

    public QueueManager(ShuraCore plugin) {
        this.plugin = plugin;
        // Start matchmaking task — runs every second
        new QueueTask(plugin).runTaskTimer(plugin, 20L, 20L);
    }

    public boolean joinQueue(Player player, String tierlistId) {
        return joinQueue(player, tierlistId, false);
    }

    public boolean joinQueue(Player player, String tierlistId, boolean premium) {
        Profile profile = plugin.getProfileManager().getProfile(player);
        if (profile == null) return false;
        if (profile.isInMatch()) {
            player.sendMessage(Component.text("You are already in a match.", NamedTextColor.RED));
            return false;
        }

        // Check if already in this specific queue - if so, leave it
        if (isInQueue(player.getUniqueId(), tierlistId, premium)) {
            return leaveQueue(player, tierlistId, premium);
        }

        Tierlist tierlist = plugin.getTierlistManager().getTierlist(tierlistId);
        if (tierlist == null) {
            // Try to find by full-id in kits-arenas.yml
            tierlist = findTierlistByFullId(tierlistId);
        }
        if (tierlist == null || !tierlist.isEnabled()) {
            player.sendMessage(Component.text("That queue is not available.", NamedTextColor.RED));
            return false;
        }

        QueueEntry entry = new QueueEntry(player.getUniqueId(), player.getName(), tierlistId, premium);

        String queueKey = tierlistId + (premium ? "_premium" : "_cracked");
        queues.computeIfAbsent(queueKey, k -> new ConcurrentLinkedQueue<>()).add(entry);
        playerQueueMap.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(entry);
        profile.setInQueue(true);

        player.sendMessage(Component.text("You joined the ", NamedTextColor.GREEN)
                .append(Component.text(tierlist.getName(), NamedTextColor.AQUA))
                .append(Component.text(" queue.", NamedTextColor.GREEN)));
        plugin.getTabManager().update(player);
        plugin.getLobbyItems().give(plugin, player);

        Bukkit.getPluginManager().callEvent(new QueueJoinEvent(player, tierlistId));
        return true;
    }

    public boolean leaveQueue(Player player, String tierlistId) {
        return leaveQueue(player, tierlistId, false) || leaveQueue(player, tierlistId, true);
    }

    public boolean leaveQueue(Player player, String tierlistId, boolean premium) {
        Set<QueueEntry> entries = playerQueueMap.get(player.getUniqueId());
        if (entries == null) return false;

        QueueEntry toRemove = null;
        for (QueueEntry entry : entries) {
            if (entry.getTierlistId().equals(tierlistId) && entry.isPremium() == premium) {
                toRemove = entry;
                break;
            }
        }

        if (toRemove == null) return false;

        entries.remove(toRemove);
        if (entries.isEmpty()) {
            playerQueueMap.remove(player.getUniqueId());
        }

        String queueKey = tierlistId + (premium ? "_premium" : "_cracked");
        ConcurrentLinkedQueue<QueueEntry> queue = queues.get(queueKey);
        if (queue != null) queue.remove(toRemove);

        Profile profile = plugin.getProfileManager().getProfile(player);
        if (profile != null) {
            profile.setInQueue(playerQueueMap.containsKey(player.getUniqueId()));
        }

        Tierlist tierlist = plugin.getTierlistManager().getTierlist(tierlistId);
        String name = tierlist != null ? tierlist.getName() : tierlistId;
        player.sendMessage(Component.text("You left the ", NamedTextColor.RED)
                .append(Component.text(name, NamedTextColor.AQUA))
                .append(Component.text(" queue.", NamedTextColor.RED)));
        plugin.getTabManager().update(player);
        if (entries.isEmpty()) plugin.getLobbyItems().give(plugin, player);

        Bukkit.getPluginManager().callEvent(new QueueLeaveEvent(player, tierlistId));
        return true;
    }

    public boolean leaveAllQueues(Player player) {
        Set<QueueEntry> entries = playerQueueMap.remove(player.getUniqueId());
        if (entries == null || entries.isEmpty()) return false;

        for (QueueEntry entry : entries) {
            String queueKey = entry.getTierlistId() + (entry.isPremium() ? "_premium" : "_cracked");
            ConcurrentLinkedQueue<QueueEntry> queue = queues.get(queueKey);
            if (queue != null) queue.remove(entry);
            Bukkit.getPluginManager().callEvent(new QueueLeaveEvent(player, entry.getTierlistId()));
        }

        Profile profile = plugin.getProfileManager().getProfile(player);
        if (profile != null) profile.setInQueue(false);

        player.sendMessage(Component.text("You left all queues.", NamedTextColor.RED));
        plugin.getTabManager().update(player);
        plugin.getLobbyItems().give(plugin, player);
        return true;
    }

    // Called every second by QueueTask
    public void tick() {
        for (Map.Entry<String, ConcurrentLinkedQueue<QueueEntry>> entry : queues.entrySet()) {
            String queueKey = entry.getKey();
            ConcurrentLinkedQueue<QueueEntry> queue = entry.getValue();
            
            if (queue.size() < 2) continue;

            List<QueueEntry> entries = new ArrayList<>(queue);
            entries.forEach(QueueEntry::tickRange);

            Set<QueueEntry> matched = new HashSet<>();

            for (int i = 0; i < entries.size(); i++) {
                QueueEntry a = entries.get(i);
                if (matched.contains(a)) continue;

                for (int j = i + 1; j < entries.size(); j++) {
                    QueueEntry b = entries.get(j);
                    if (matched.contains(b)) continue;

                    if (a.canMatch(b)) {
                        matched.add(a);
                        matched.add(b);
                        queue.remove(a);
                        queue.remove(b);
                        removeFromAllQueues(a.getPlayerUuid());
                        removeFromAllQueues(b.getPlayerUuid());
                        // Schedule match creation on main thread
                        Bukkit.getScheduler().runTask(plugin, () -> createMatch(a, b));
                        break;
                    }
                }
            }
        }
    }

    private void createMatch(QueueEntry a, QueueEntry b) {
        Player playerA = Bukkit.getPlayer(a.getPlayerUuid());
        Player playerB = Bukkit.getPlayer(b.getPlayerUuid());

        if (playerA == null || playerB == null) {
            // One player disconnected — re-queue the other (preserving premium/cracked)
            if (playerA != null) joinQueue(playerA, a.getTierlistId(), a.isPremium());
            if (playerB != null) joinQueue(playerB, b.getTierlistId(), b.isPremium());
            return;
        }

        // Parse tierlist from full-id (e.g., "Sword-COM" -> "mctiers")
        String tierlistId = parseTierlistFromFullId(a.getTierlistId());
        Tierlist tierlist = plugin.getTierlistManager().getTierlist(tierlistId);
        if (tierlist == null) return;

        // Get kit from full-id
        String kitId = a.getTierlistId(); // Use full-id as kit ID
        Kit kit = plugin.getKitManager().getKit(kitId);
        if (kit == null) {
            playerA.sendMessage(Component.text("Match failed: kit not found.", NamedTextColor.RED));
            playerB.sendMessage(Component.text("Match failed: kit not found.", NamedTextColor.RED));
            return;
        }

        Arena arena = plugin.getArenaManager().getAvailableArenaForKit(kit.getId());
        if (arena == null) {
            playerA.sendMessage(Component.text("No arenas available. Please wait...", NamedTextColor.RED));
            playerB.sendMessage(Component.text("No arenas available. Please wait...", NamedTextColor.RED));
            // Re-queue both (preserving premium/cracked)
            joinQueue(playerA, a.getTierlistId(), a.isPremium());
            joinQueue(playerB, b.getTierlistId(), b.isPremium());
            return;
        }

        ArenaCopy copy = plugin.getArenaManager().getOrCreateCopy(arena);
        if (copy == null) return;

        SoundUtil.playMatchFound(playerA);
        SoundUtil.playMatchFound(playerB);

        playerA.sendMessage(Component.text("Match found! Opponent: ", NamedTextColor.GREEN)
                .append(Component.text(playerB.getName(), NamedTextColor.AQUA)));
        playerB.sendMessage(Component.text("Match found! Opponent: ", NamedTextColor.GREEN)
                .append(Component.text(playerA.getName(), NamedTextColor.AQUA)));

        Profile profileA = plugin.getProfileManager().getProfile(playerA);
        Profile profileB = plugin.getProfileManager().getProfile(playerB);
        if (profileA != null) profileA.setInQueue(false);
        if (profileB != null) profileB.setInQueue(false);

        plugin.getMatchManager().createMatch(
                playerA, playerB, kit, arena, copy,
                MatchType.RANKED, MatchFormat.FT3, tierlistId
        );

        // Update tablist for both players
        plugin.getTabManager().update(playerA);
        plugin.getTabManager().update(playerB);
    }

    private String parseTierlistFromFullId(String fullId) {
        // Parse "Sword-COM" -> "mctiers", "Sword-IO" -> "pvptiers", etc.
        if (fullId.endsWith("-COM")) return "mctiers";
        if (fullId.endsWith("-IO")) return "pvptiers";
        if (fullId.endsWith("-Sub")) return "subtiers";
        if (fullId.endsWith("-Cactus")) return "cactustiers";
        if (fullId.endsWith("-Nova")) return "novatiers";
        if (fullId.endsWith("-Ex")) return "extiers";
        if (fullId.endsWith("-Extra")) return "extragm";
        return fullId; // Fallback
    }

    public boolean isInQueue(UUID uuid) { return playerQueueMap.containsKey(uuid) && !playerQueueMap.get(uuid).isEmpty(); }

    public boolean isInQueue(UUID uuid, String tierlistId) {
        Set<QueueEntry> entries = playerQueueMap.get(uuid);
        if (entries == null) return false;
        return entries.stream().anyMatch(e -> e.getTierlistId().equals(tierlistId));
    }

    public boolean isInQueue(UUID uuid, String tierlistId, boolean premium) {
        Set<QueueEntry> entries = playerQueueMap.get(uuid);
        if (entries == null) return false;
        return entries.stream().anyMatch(e -> e.getTierlistId().equals(tierlistId) && e.isPremium() == premium);
    }

    public Set<QueueEntry> getEntries(UUID uuid) { 
        Set<QueueEntry> entries = playerQueueMap.get(uuid);
        return entries != null ? new HashSet<>(entries) : new HashSet<>();
    }

    /** Total players queued for a tierlist/kit id across both the cracked and premium queues. */
    public int getQueueSize(String tierlistId) {
        return getQueueSize(tierlistId, false) + getQueueSize(tierlistId, true);
    }

    public int getQueueSize(String tierlistId, boolean premium) {
        ConcurrentLinkedQueue<QueueEntry> queue = queues.get(tierlistId + (premium ? "_premium" : "_cracked"));
        return queue == null ? 0 : queue.size();
    }

    /** Total players queued across every queue. */
    public int getTotalQueued() {
        return queues.values().stream().mapToInt(ConcurrentLinkedQueue::size).sum();
    }

    public void clearAll() {
        queues.clear();
        playerQueueMap.clear();
    }

    private Tierlist findTierlistByFullId(String fullId) {
        // For now, treat the full-id as the tierlist identifier
        // Create a temporary tierlist object for queue purposes
        return new Tierlist(fullId, fullId, fullId);
    }

    private void removeFromAllQueues(UUID playerUuid) {
        Set<QueueEntry> entries = playerQueueMap.remove(playerUuid);
        if (entries != null) {
            for (QueueEntry entry : entries) {
                String premiumKey = entry.getTierlistId() + "_premium";
                String crackedKey = entry.getTierlistId() + "_cracked";
                ConcurrentLinkedQueue<QueueEntry> premiumQueue = queues.get(premiumKey);
                ConcurrentLinkedQueue<QueueEntry> crackedQueue = queues.get(crackedKey);
                if (premiumQueue != null) premiumQueue.remove(entry);
                if (crackedQueue != null) crackedQueue.remove(entry);
            }
        }
        Profile profile = plugin.getProfileManager().getProfile(playerUuid);
        if (profile != null) profile.setInQueue(false);
    }
}
