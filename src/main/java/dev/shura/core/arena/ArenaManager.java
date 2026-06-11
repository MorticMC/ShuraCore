package dev.shura.core.arena;

import dev.shura.core.ShuraCore;
import dev.shura.core.util.LocationUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class ArenaManager {

    private static final int COPY_SPACING = 512; // blocks between copies (large enough to avoid overlap for big arenas)
    private static final int GRID_WIDTH = 20;    // copies per grid row
    private static final int MIN_COPIES = 2;

    private final ShuraCore plugin;
    private final Map<String, Arena> arenas = new ConcurrentHashMap<>();
    private final SchematicService schematics;
    private final ArenaReset arenaReset;
    // Occupied copy origins per world — freed when a copy is removed so slots are reused (no leak)
    private final Map<String, Set<Location>> usedLocations = new ConcurrentHashMap<>();

    public ArenaManager(ShuraCore plugin) {
        this.plugin = plugin;
        this.schematics = new SchematicService(plugin);
        this.arenaReset = new ArenaReset(plugin, schematics);
        createDuelWorld();
        loadAll();
    }

    public SchematicService getSchematics() { return schematics; }

    private void createDuelWorld() {
        WorldCreator creator = new WorldCreator("shura_duels");
        creator.type(org.bukkit.WorldType.FLAT);
        creator.environment(World.Environment.NORMAL);
        creator.createWorld();
    }

    private void loadAll() {
        plugin.getDatabaseService().query("SELECT * FROM arenas", null, rs -> {
            List<Arena> loaded = new ArrayList<>();
            try {
                while (rs.next()) loaded.add(deserialize(rs));
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load arenas.", e);
            }
            return loaded;
        }).thenAccept(list -> {
            if (list == null) return;
            list.forEach(a -> arenas.put(a.getId(), a));
            Bukkit.getScheduler().runTask(plugin, this::initializeCopies);
        });
    }

    private void initializeCopies() {
        for (Arena arena : arenas.values()) {
            if (!arena.isEnabled() || !arena.isFullyConfigured()) continue;
            while (arena.getCopies().size() < arena.getAutoCloneCount()) {
                generateCopy(arena);
            }
        }
    }

    public ArenaCopy getOrCreateCopy(Arena arena) {
        ArenaCopy available = arena.getAvailableCopy();
        if (available != null && available.claim()) {
            return available;
        }
        // No ready copy free — generate more (async paste). The caller should retry shortly.
        for (int i = 0; i < arena.getAutoCloneCount(); i++) {
            generateCopy(arena);
        }
        available = arena.getAvailableCopy();
        if (available != null && available.claim()) {
            return available;
        }
        return null; // copies are warming up; caller re-queues / retries
    }

    public ArenaCopy generateCopy(Arena arena) {
        World duelWorld = Bukkit.getWorld("shura_duels");
        if (duelWorld == null) {
            plugin.getLogger().warning("shura_duels world not found! Cannot generate arena copy.");
            return null;
        }

        Location origin = findAvailableLocation(duelWorld);

        // Offsets are calculated from the MINIMUM corner (the clipboard's origin)
        Location pos1 = arena.getPos1();
        Location pos2 = arena.getPos2();
        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());

        int offsetX = origin.getBlockX() - minX;
        int offsetY = origin.getBlockY() - minY;
        int offsetZ = origin.getBlockZ() - minZ;

        Location copySpawnA = arena.getSpawnA().clone().add(offsetX, offsetY, offsetZ);
        Location copySpawnB = arena.getSpawnB().clone().add(offsetX, offsetY, offsetZ);
        copySpawnA.setWorld(duelWorld);
        copySpawnB.setWorld(duelWorld);

        ArenaCopy copy = new ArenaCopy(arena.getId(), origin, copySpawnA, copySpawnB);
        arena.addCopy(copy);

        // Paste via FAWE (async). Mark ready only once the blocks have landed.
        schematics.pasteAsync(arena, origin, () -> copy.setReady(true));

        return copy;
    }

    /** Returns the first free grid slot in the duel world, reusing slots freed by removed copies. */
    private Location findAvailableLocation(World world) {
        Set<Location> occupied = usedLocations.computeIfAbsent(world.getName(), k -> ConcurrentHashMap.newKeySet());

        for (int attempt = 0; attempt < GRID_WIDTH * GRID_WIDTH * 4; attempt++) {
            int gridX = (attempt % GRID_WIDTH) * COPY_SPACING;
            int gridZ = (attempt / GRID_WIDTH) * COPY_SPACING;
            Location slot = new Location(world, gridX, 64, gridZ);
            if (occupied.add(slot)) {
                return slot;
            }
        }
        // Extremely unlikely fallback far outside the grid
        Location fallback = new Location(world, GRID_WIDTH * COPY_SPACING * 2, 64, 0);
        occupied.add(fallback);
        return fallback;
    }

    private void freeLocation(Location origin) {
        if (origin == null || origin.getWorld() == null) return;
        Set<Location> occupied = usedLocations.get(origin.getWorld().getName());
        if (occupied != null) occupied.remove(origin);
    }

    public void resetArena(ArenaCopy copy) {
        arenaReset.reset(copy, () -> {
            Arena arena = arenas.get(copy.getArenaId());
            if (arena == null) return;
            // Trim surplus copies back down to the minimum and free their origin slot
            if (arena.getCopies().size() > MIN_COPIES) {
                arena.removeCopy(copy);
                freeLocation(copy.getOrigin());
            }
        });
    }

    public void copyAndPasteArena(Arena sourceArena, Location pasteLocation, org.bukkit.entity.Player player) {
        if (sourceArena.getPos1() == null || sourceArena.getPos2() == null ||
            sourceArena.getSpawnA() == null || sourceArena.getSpawnB() == null) {
            player.sendMessage(dev.shura.core.extra.MessageService.colorizeComponent("&cSource arena is not fully configured!"));
            return;
        }

        // Calculate offsets from source arena to paste location
        Location sourcePos1 = sourceArena.getPos1();
        int offsetX = pasteLocation.getBlockX() - sourcePos1.getBlockX();
        int offsetY = pasteLocation.getBlockY() - sourcePos1.getBlockY();
        int offsetZ = pasteLocation.getBlockZ() - sourcePos1.getBlockZ();

        // Create new arena with offset positions
        String newArenaId = UUID.randomUUID().toString();
        String newArenaName = sourceArena.getName() + "_copy_" + System.currentTimeMillis();
        Arena newArena = new Arena(newArenaId, newArenaName, pasteLocation.getWorld().getName());

        // Set positions with offsets
        Location newPos1 = sourcePos1.clone().add(offsetX, offsetY, offsetZ);
        Location newPos2 = sourceArena.getPos2().clone().add(offsetX, offsetY, offsetZ);
        Location newSpawnA = sourceArena.getSpawnA().clone().add(offsetX, offsetY, offsetZ);
        Location newSpawnB = sourceArena.getSpawnB().clone().add(offsetX, offsetY, offsetZ);

        newArena.setPos1(newPos1);
        newArena.setPos2(newPos2);
        newArena.setSpawnA(newSpawnA);
        newArena.setSpawnB(newSpawnB);
        newArena.setEnabled(true);

        // Copy bound kits
        for (String kitId : sourceArena.getBoundKitIds()) {
            newArena.addBoundKit(kitId);
        }

        // Paste blocks from source to the new location via FAWE so recorded positions stay aligned
        Location srcPos1 = sourceArena.getPos1();
        Location srcMin = new Location(srcPos1.getWorld(),
                Math.min(srcPos1.getBlockX(), sourceArena.getPos2().getBlockX()),
                Math.min(srcPos1.getBlockY(), sourceArena.getPos2().getBlockY()),
                Math.min(srcPos1.getBlockZ(), sourceArena.getPos2().getBlockZ()));
        Location targetMin = srcMin.clone().add(offsetX, offsetY, offsetZ);
        targetMin.setWorld(pasteLocation.getWorld());

        schematics.pasteAsync(sourceArena, targetMin, () -> {
            saveArena(newArena);
            player.sendMessage(dev.shura.core.extra.MessageService.colorizeComponent(
                "&aArena copied and pasted! New arena: &e" + newArenaName));
            dev.shura.core.util.SoundUtil.playSuccess(player);
            while (newArena.getCopies().size() < MIN_COPIES) {
                generateCopy(newArena);
            }
        });
    }

    public void setAutoCloneCount(Arena arena, int count) {
        arena.setAutoCloneCount(count);
        saveArena(arena);
    }

    public void saveArena(Arena arena) {
        String boundKitsJson = String.join(",", arena.getBoundKitIds());
        plugin.getDatabaseService().update(
                "INSERT INTO arenas (id, name, world, pos1, pos2, spawn_a, spawn_b, kit_id, copies, enabled, bound_kits, auto_clone_count) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, '[]', ?, ?, ?) ON CONFLICT(id) DO UPDATE SET " +
                "name=excluded.name, world=excluded.world, pos1=excluded.pos1, pos2=excluded.pos2, " +
                "spawn_a=excluded.spawn_a, spawn_b=excluded.spawn_b, kit_id=excluded.kit_id, enabled=excluded.enabled, bound_kits=excluded.bound_kits, auto_clone_count=excluded.auto_clone_count",
                stmt -> {
                    stmt.setString(1, arena.getId());
                    stmt.setString(2, arena.getName());
                    stmt.setString(3, arena.getWorld());
                    stmt.setString(4, arena.getPos1() != null ? LocationUtil.serialize(arena.getPos1()) : "");
                    stmt.setString(5, arena.getPos2() != null ? LocationUtil.serialize(arena.getPos2()) : "");
                    stmt.setString(6, arena.getSpawnA() != null ? LocationUtil.serialize(arena.getSpawnA()) : "");
                    stmt.setString(7, arena.getSpawnB() != null ? LocationUtil.serialize(arena.getSpawnB()) : "");
                    stmt.setString(8, arena.getKitId());
                    stmt.setBoolean(9, arena.isEnabled());
                    stmt.setString(10, boundKitsJson);
                    stmt.setInt(11, arena.getAutoCloneCount());
                });
        arenas.put(arena.getId(), arena);
    }

    public void deleteArena(String id) {
        Arena arena = arenas.remove(id);
        if (arena != null) {
            arena.getCopies().forEach(c -> freeLocation(c.getOrigin()));
            arena.getCopies().clear();
            arenaReset.invalidate(id);
        }
        plugin.getDatabaseService().updateAsync("DELETE FROM arenas WHERE id = ?",
                stmt -> stmt.setString(1, id));
    }

    public Arena getArena(String id) { return arenas.get(id); }

    public Arena getArenaByName(String name) {
        return arenas.values().stream()
                .filter(a -> a.getName().equalsIgnoreCase(name))
                .findFirst().orElse(null);
    }

    public List<Arena> getAvailableArenas() {
        return arenas.values().stream()
                .filter(a -> a.isEnabled() && a.isFullyConfigured())
                .toList();
    }

    public Arena getAvailableArenaForKit(String kitId) {
        List<Arena> matching = arenas.values().stream()
                .filter(a -> a.isEnabled() && a.isFullyConfigured()
                        && (a.getKitId() == null || a.getKitId().equals(kitId) || a.isBoundToKit(kitId))
                        && a.hasAvailableCopy())
                .toList();
        if (matching.isEmpty()) return null;
        return matching.get(new Random().nextInt(matching.size()));
    }

    public Collection<Arena> getAllArenas() { return Collections.unmodifiableCollection(arenas.values()); }

    private Arena deserialize(ResultSet rs) throws SQLException {
        Arena arena = new Arena(rs.getString("id"), rs.getString("name"), rs.getString("world"));
        arena.setPos1(LocationUtil.deserialize(rs.getString("pos1")));
        arena.setPos2(LocationUtil.deserialize(rs.getString("pos2")));
        arena.setSpawnA(LocationUtil.deserialize(rs.getString("spawn_a")));
        arena.setSpawnB(LocationUtil.deserialize(rs.getString("spawn_b")));
        arena.setKitId(rs.getString("kit_id"));
        arena.setEnabled(rs.getBoolean("enabled"));
        
        try {
            String boundKits = rs.getString("bound_kits");
            if (boundKits != null && !boundKits.isEmpty()) {
                for (String kitId : boundKits.split(",")) {
                    if (!kitId.isEmpty()) arena.addBoundKit(kitId);
                }
            }
        } catch (SQLException e) {
            // Column doesn't exist yet
        }
        
        try {
            int autoCloneCount = rs.getInt("auto_clone_count");
            if (autoCloneCount > 0) arena.setAutoCloneCount(autoCloneCount);
        } catch (SQLException e) {
            // Column doesn't exist yet, use default
        }
        
        return arena;
    }
}
