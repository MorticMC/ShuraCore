package dev.shura.core.tierlist;

import dev.shura.core.ShuraCore;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TierlistManager {

    private final ShuraCore plugin;
    private final Map<String, Tierlist> tierlists = new ConcurrentHashMap<>();

    // The default tierlists — IDs are fixed, names/kits configurable
    private static final String[][] DEFAULT_TIERLISTS = {
        {"mctiers",    "MCTiers"},
        {"pvptiers",   "PvPTiers"},
        {"novatiers",  "NovaTiers"},
        {"extiers",    "ExTiers"},
        {"cactustiers","CactusTiers"},
        {"extragm",    "Extra Gamemodes"},
        {"subtiers",   "SubTiers"}
    };

    public TierlistManager(ShuraCore plugin) {
        this.plugin = plugin;
        loadFromConfig();
    }

    private void loadFromConfig() {
        for (String[] entry : DEFAULT_TIERLISTS) {
            String id = entry[0];
            String defaultName = entry[1];

            String name = plugin.getConfig().getString("tierlists." + id + ".name", defaultName);
            String kitId = plugin.getConfig().getString("tierlists." + id + ".kit-id", null);
            boolean enabled = plugin.getConfig().getBoolean("tierlists." + id + ".enabled", true);
            boolean queueEnabled = plugin.getConfig().getBoolean("tierlists." + id + ".queue-enabled", false);

            Tierlist tierlist = new Tierlist(id, name, kitId);
            tierlist.setEnabled(enabled);
            tierlist.setQueueEnabled(queueEnabled);
            tierlists.put(id, tierlist);
        }
    }

    public Tierlist getTierlist(String id) { return tierlists.get(id); }

    public Collection<Tierlist> getAllTierlists() {
        return Collections.unmodifiableCollection(tierlists.values());
    }

    public List<Tierlist> getQueueEnabledTierlists() {
        return tierlists.values().stream()
                .filter(t -> t.isEnabled() && t.isQueueEnabled())
                .toList();
    }

    public void setQueueEnabled(String id, boolean enabled) {
        Tierlist tierlist = tierlists.get(id);
        if (tierlist == null) return;
        tierlist.setQueueEnabled(enabled);
        plugin.getDatabaseService().setSetting("tierlist.queue." + id, String.valueOf(enabled));
    }

    public void setEnabled(String id, boolean enabled) {
        Tierlist tierlist = tierlists.get(id);
        if (tierlist == null) return;
        tierlist.setEnabled(enabled);
    }
}
