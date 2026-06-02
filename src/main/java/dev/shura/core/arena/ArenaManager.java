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

    private static final int COPY_SPACING = 150; // blocks between copies
    private static final int MIN_COPIES = 2;

    private final ShuraCore plugin;
    private final Map<String, Arena> arenas = new ConcurrentHashMap<>();
    private final ArenaReset arenaReset;
    private final Map<String, Set<Location>> usedLocations = new ConcurrentHashMap<>(); // Track used locations per world

    public ArenaManager(ShuraCore plugin) {
        this.plugin = plugin;
        this.arenaReset = new ArenaReset(plugin);
        createDuelWorld();
        loadAll();
    }

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
        if (available != null) {
            available.claim();
            return available;
        }
        // All copies in use — generate new ones based on autoCloneCount
        for (int i = 0; i < arena.getAutoCloneCount(); i++) {
            generateCopy(arena);
        }
        available = arena.getAvailableCopy();
        if (available != null) available.claim();
        return available;
    }

    public ArenaCopy generateCopy(Arena arena) {
        World duelWorld = Bukkit.getWorld("shura_duels");
        if (duelWorld == null) {
            plugin.getLogger().warning("shura_duels world not found! Cannot generate arena copy.");
            return null;
        }

        // Find available location with smart spacing
        int arenaWidth = arena.getWidth();
        int arenaLength = arena.getLength();
        Location origin = findAvailableLocation(duelWorld, arenaWidth, arenaLength);

        // Calculate offset from MINIMUM corner (not pos1)
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

        // Paste arena blocks at origin (sync — runs on main thread)
        pasteArena(arena, origin, duelWorld);

        // Capture snapshot after paste
        Bukkit.getScheduler().runTaskLater(plugin, () ->
                arenaReset.captureSnapshot(copy, arena), 5L);

        return copy;
    }

    private Location findAvailableLocation(World world, int arenaWidth, int arenaLength) {
        Set<Location> worldLocations = usedLocations.computeIfAbsent(world.getName(), k -> new HashSet<>());
        
        int spacing = COPY_SPACING;
        int maxAttempts = 100;
        int attempt = 0;
        
        while (attempt < maxAttempts) {
            int gridX = (attempt % 10) * spacing;
            int gridZ = (attempt / 10) * spacing;
            Location testLoc = new Location(world, gridX, 64, gridZ);
            
            // Check if this location conflicts with any existing arena
            boolean conflict = false;
            for (Location usedLoc : worldLocations) {
                double distance = Math.sqrt(
                    Math.pow(testLoc.getX() - usedLoc.getX(), 2) +
                    Math.pow(testLoc.getZ() - usedLoc.getZ(), 2)
                );
                
                // If too close, try with double spacing
                if (distance < spacing) {
                    conflict = true;
                    break;
                }
            }
            
            if (!conflict) {
                worldLocations.add(testLoc);
                return testLoc;
            }
            
            // If conflict at 150, try 300 blocks away
            if (spacing == COPY_SPACING) {
                spacing = COPY_SPACING * 2;
            }
            
            attempt++;
        }
        
        // Fallback to random location far away
        Location fallback = new Location(world, attempt * spacing, 64, attempt * spacing);
        worldLocations.add(fallback);
        return fallback;
    }

    private void pasteArena(Arena arena, Location origin, World targetWorld) {
        Location pos1 = arena.getPos1();
        Location pos2 = arena.getPos2();
        World sourceWorld = pos1.getWorld();

        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        int offsetX = origin.getBlockX() - minX;
        int offsetY = origin.getBlockY() - minY;
        int offsetZ = origin.getBlockZ() - minZ;

        // Paste in chunks to reduce lag
        final int BLOCKS_PER_TICK = 1000;
        List<int[]> blocks = new ArrayList<>();
        
        for (int x = minX; x <= maxX; x++)
            for (int y = minY; y <= maxY; y++)
                for (int z = minZ; z <= maxZ; z++)
                    blocks.add(new int[]{x, y, z});
        
        final int[] index = {0};
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            int count = 0;
            while (index[0] < blocks.size() && count < BLOCKS_PER_TICK) {
                int[] pos = blocks.get(index[0]);
                targetWorld.getBlockAt(pos[0] + offsetX, pos[1] + offsetY, pos[2] + offsetZ)
                        .setBlockData(sourceWorld.getBlockAt(pos[0], pos[1], pos[2]).getBlockData().clone(), false);
                index[0]++;
                count++;
            }
            if (index[0] >= blocks.size()) task.cancel();
        }, 0L, 1L);
    }

    public void resetArena(ArenaCopy copy) {
        arenaReset.reset(copy, () -> {
            Arena arena = arenas.get(copy.getArenaId());
            if (arena == null) return;
            // If total copies exceed minimum and this copy is no longer needed, remove it
            if (arena.getCopies().size() > MIN_COPIES) {
                arena.removeCopy(copy);
                arenaReset.removeSnapshot(copy.getCopyId());
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

        // Paste blocks from source to new location
        pasteArenaBlocks(sourceArena, pasteLocation, pasteLocation.getWorld());

        // Save new arena
        saveArena(newArena);

        player.sendMessage(dev.shura.core.extra.MessageService.colorizeComponent(
            "&aArena copied and pasted! New arena: &e" + newArenaName));
        dev.shura.core.util.SoundUtil.playSuccess(player);

        // Initialize copies for the new arena
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            while (newArena.getCopies().size() < MIN_COPIES) {
                generateCopy(newArena);
            }
        }, 20L);
    }

    private void pasteArenaBlocks(Arena sourceArena, Location pasteOrigin, World targetWorld) {
        Location pos1 = sourceArena.getPos1();
        Location pos2 = sourceArena.getPos2();
        World sourceWorld = pos1.getWorld();

        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        int offsetX = pasteOrigin.getBlockX() - minX;
        int offsetY = pasteOrigin.getBlockY() - minY;
        int offsetZ = pasteOrigin.getBlockZ() - minZ;

        final int BLOCKS_PER_TICK = 1000;
        List<int[]> blocks = new ArrayList<>();
        
        for (int x = minX; x <= maxX; x++)
            for (int y = minY; y <= maxY; y++)
                for (int z = minZ; z <= maxZ; z++)
                    blocks.add(new int[]{x, y, z});
        
        final int[] index = {0};
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            int count = 0;
            while (index[0] < blocks.size() && count < BLOCKS_PER_TICK) {
                int[] pos = blocks.get(index[0]);
                targetWorld.getBlockAt(pos[0] + offsetX, pos[1] + offsetY, pos[2] + offsetZ)
                        .setBlockData(sourceWorld.getBlockAt(pos[0], pos[1], pos[2]).getBlockData().clone(), false);
                index[0]++;
                count++;
            }
            if (index[0] >= blocks.size()) task.cancel();
        }, 0L, 1L);
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
        if (arena != null) arena.getCopies().forEach(c -> arenaReset.removeSnapshot(c.getCopyId()));
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
