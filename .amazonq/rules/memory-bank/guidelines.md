# ShuraCore - Development Guidelines

## Code Quality Standards

### Package Organization
- **Strict package-by-feature structure**: Each feature has its own package (arena, match, queue, party, etc.)
- **Flat package hierarchy**: Avoid deep nesting; keep packages at 1-2 levels under `dev.shura.core`
- **Utility packages**: Common utilities in `util/` package, command utilities in `command/util/`
- **Consistent naming**: Package names are lowercase, singular nouns (match, not matches)

### Class Naming Conventions
- **Manager suffix**: Classes managing collections or lifecycle use `Manager` (ArenaManager, MatchManager, QueueManager)
- **Service suffix**: Infrastructure services use `Service` (DatabaseService, MessageService)
- **Listener suffix**: Event handlers use `Listener` (PlayerListener, MatchListener)
- **Command suffix**: Command executors use `Command` (ShuraCommand, DuelCommand)
- **Gui suffix**: Inventory-based interfaces use `Gui` (ProfileGui, SettingsGui)
- **Data models**: Simple nouns without suffix (Arena, Kit, Match, Profile, Party)

### Code Formatting
- **No braces for single-line blocks**: Use `if (condition) statement;` without braces
- **Switch expressions**: Use arrow syntax `case "value" -> statement;` instead of traditional switch
- **Compact method chains**: Chain method calls on same line when readable
- **Minimal whitespace**: No excessive blank lines between methods
- **Inline variable declarations**: Declare variables close to usage, not at method start

### Documentation Standards
- **No JavaDoc**: Code is self-documenting through clear naming
- **No inline comments**: Except for complex algorithms or non-obvious logic
- **Section comments**: Use ASCII art separators in long classes (e.g., `// ── ARENA ───────────`)
- **Configuration comments**: Document config keys in code where they're used

## Architectural Patterns

### Manager Pattern (Used in 90% of core systems)
Centralized management of entities with CRUD operations:

```java
public class ExampleManager {
    private final ShuraCore plugin;
    private final Map<UUID, Example> registry = new HashMap<>();
    
    public ExampleManager(ShuraCore plugin) {
        this.plugin = plugin;
        loadFromDatabase(); // or loadFromConfig()
    }
    
    public Example getExample(UUID id) { return registry.get(id); }
    public void addExample(Example example) { registry.put(example.getId(), example); }
    public void removeExample(UUID id) { registry.remove(id); }
    public Collection<Example> getAllExamples() { return registry.values(); }
}
```

**Examples**: ArenaManager, KitManager, MatchManager, QueueManager, PartyManager, ProfileManager

### Singleton Plugin Access
All managers and services access plugin instance:
```java
private final ShuraCore plugin;
public ExampleClass(ShuraCore plugin) { this.plugin = plugin; }
```

Access other managers via plugin:
```java
plugin.getMatchManager().createMatch(...);
plugin.getProfileManager().getProfile(player);
```

### Async Database Pattern
All database operations are asynchronous using CompletableFuture:

```java
// Query with result
public CompletableFuture<Profile> loadProfile(UUID uuid) {
    return plugin.getDatabaseService().query(
        "SELECT * FROM profiles WHERE uuid = ?",
        stmt -> stmt.setString(1, uuid.toString()),
        rs -> {
            if (rs.next()) return parseProfile(rs);
            return null;
        }
    );
}

// Update without callback
plugin.getDatabaseService().updateAsync(
    "UPDATE profiles SET elo = ? WHERE uuid = ?",
    stmt -> {
        stmt.setInt(1, newElo);
        stmt.setString(2, uuid.toString());
    }
);

// Update with callback
plugin.getDatabaseService().updateAsync(sql, prepare, rowsAffected -> {
    if (rowsAffected > 0) plugin.getLogger().info("Updated successfully");
});
```

**Key Points**:
- Use `query()` for SELECT statements returning data
- Use `updateAsync()` for INSERT/UPDATE/DELETE
- Use `StatementConsumer` functional interface for prepared statements
- Never block main thread with database calls

### Event-Driven Communication
Custom events for inter-system communication:

```java
// Fire event
Bukkit.getPluginManager().callEvent(new MatchStartEvent(this));

// Listen to event
@EventHandler
public void onMatchStart(MatchStartEvent event) {
    Match match = event.getMatch();
    // Handle event
}
```

**Custom Events**: MatchStartEvent, MatchEndEvent, MatchDeathEvent, QueueJoinEvent, PartyCreateEvent, etc.

### State Machine Pattern
Used in Match system for lifecycle management:

```java
public enum MatchState {
    WAITING, STARTING, IN_PROGRESS, ENDING, FINISHED
}

private MatchState state;

public void start() {
    state = MatchState.STARTING;
    // ... transition logic
}

public void handleDeath(Player died) {
    if (state != MatchState.IN_PROGRESS) return; // Guard clause
    // ... handle death
}
```

### Configuration-Driven Design
Extensive use of YAML configuration for customization:

```java
// Load from config
int countdown = plugin.getConfig().getInt("settings.countdown-seconds", 5);
String message = plugin.getMessageService().get("errors.no-permission");

// Custom config classes
plugin.getKitsArenasConfig().getKits();
plugin.getScoreboardSoundsConfig().getScoreboard("lobby");
```

### Builder Pattern for Items
ItemBuilder provides fluent API:

```java
ItemStack item = new ItemBuilder(Material.DIAMOND_SWORD)
    .name(Component.text("PvP Sword", NamedTextColor.AQUA))
    .lore(Component.text("Click to fight!"))
    .enchant(Enchantment.SHARPNESS, 2)
    .build();
```

## Common Code Idioms

### Null-Safe Player Retrieval
```java
Player player = Bukkit.getPlayer(uuid);
if (player == null) return; // or handle offline case
```

### Permission Checks
```java
if (!sender.hasPermission("shura.admin")) {
    if (sender instanceof Player p) plugin.getMessageService().send(p, "errors.no-permission");
    return true;
}
```

### Component-Based Messaging
Use Kyori Adventure API for all messages:

```java
player.sendMessage(Component.text("Match starting in ", NamedTextColor.YELLOW)
    .append(Component.text(seconds, NamedTextColor.AQUA))
    .append(Component.text("...", NamedTextColor.YELLOW)));
```

For colored messages from config:
```java
MessageService.colorizeComponent("&#00B4FF[ShuraCore] &aSuccess!");
```

### Lambda Command Registration
```java
getCommand("leave").setExecutor((sender, cmd, label, args) -> {
    if (!(sender instanceof Player p)) return true;
    // Handle command
    return true;
});
```

### Stream-Based Filtering
```java
Kit kit = plugin.getKitManager().getAllKits().stream()
    .filter(k -> k.getName().equalsIgnoreCase(name))
    .findFirst().orElse(null);
```

### Scheduler Patterns
```java
// Delayed task
Bukkit.getScheduler().runTaskLater(plugin, () -> finish(), 100L);

// Repeating task
Bukkit.getScheduler().runTaskTimer(plugin, () -> tick(), 0L, 20L);

// Async task
Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> heavyOperation());
```

### Guard Clauses
Early returns for invalid states:

```java
public void handleDeath(Player died) {
    if (state != MatchState.IN_PROGRESS) return;
    if (died == null) return;
    // Main logic
}
```

## Frequently Used Annotations

### Bukkit Event Handlers
```java
@EventHandler
public void onPlayerJoin(PlayerJoinEvent event) {
    // Handle event
}

@EventHandler(priority = EventPriority.HIGH)
public void onPlayerQuit(PlayerQuitEvent event) {
    // Handle with priority
}
```

### Override Annotations
Always use @Override for interface implementations:
```java
@Override
public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    // Command logic
    return true;
}
```

### Functional Interfaces
```java
@FunctionalInterface
public interface StatementConsumer {
    void accept(PreparedStatement stmt) throws SQLException;
}
```

## Internal API Usage Patterns

### Profile Management
```java
// Get profile (cached)
Profile profile = plugin.getProfileManager().getProfile(player);

// Check match state
if (profile.isInMatch()) {
    player.sendMessage(Component.text("You are already in a match.", NamedTextColor.RED));
    return;
}

// Update stats
profile.incrementMatches(tierlistId);
profile.recordWin();
plugin.getProfileManager().saveProfile(profile);
```

### Match Creation
```java
plugin.getMatchManager().createMatch(
    playerA, playerB,           // Players
    kit,                        // Kit to use
    arena, arenaCopy,          // Arena and copy
    MatchType.RANKED,          // Match type
    MatchFormat.FT3,           // Format (first to 3)
    tierlistId                 // Tierlist ID (null for unranked)
);
```

### Queue Operations
```java
// Join queue
plugin.getQueueManager().joinQueue(player, tierlistId, premium);

// Leave queue
plugin.getQueueManager().leaveAllQueues(player);

// Check queue status
if (plugin.getQueueManager().isInQueue(player.getUniqueId())) {
    // Player is queued
}
```

### Arena Management
```java
// Get available arena
Arena arena = plugin.getArenaManager().getAvailableArenaForKit(kitId);

// Create arena copy
ArenaCopy copy = plugin.getArenaManager().getOrCreateCopy(arena);

// Reset arena after match
plugin.getArenaManager().resetArena(arenaCopy);
```

### Sound System
```java
SoundUtil.playSuccess(player);
SoundUtil.playMatchFound(player);
SoundUtil.playMatchStart(player);
SoundUtil.playMatchWin(player);
SoundUtil.playCountdownTick(player, secondsRemaining);
```

### Message Service
```java
// Send localized message
plugin.getMessageService().send(player, "errors.no-permission");

// Colorize custom message
Component colored = MessageService.colorizeComponent("&#00B4FF&lTitle &7Description");
player.sendMessage(colored);
```

### Tab List Updates
```java
// Update player's tab list
plugin.getTabManager().update(player);
```

### Scoreboard Updates
```java
// Update match scoreboard
plugin.getBoardManager().updateMatch(match);

// Update lobby scoreboard
plugin.getBoardManager().updateLobby(player);
```

### Lobby Items
```java
// Give lobby items to player
plugin.getLobbyItems().give(plugin, player);
```

## Best Practices

### Error Handling
- **Graceful degradation**: Handle null cases without crashing
- **User feedback**: Always inform players of errors with colored messages
- **Logging**: Log severe errors with stack traces
- **Validation**: Validate input before processing

### Performance Considerations
- **Async database**: Never block main thread with database operations
- **Concurrent collections**: Use ConcurrentLinkedQueue for thread-safe queues
- **Caching**: Cache frequently accessed data (profiles, kits, arenas)
- **Lazy loading**: Load data on-demand, not at startup

### Resource Management
- **Connection pooling**: Use HikariCP for database connections
- **Task cancellation**: Cancel BukkitTasks in cleanup methods
- **Arena cleanup**: Always reset arenas after matches
- **Inventory clearing**: Clear player inventories before matches

### Code Organization
- **Single responsibility**: Each class has one clear purpose
- **Minimal coupling**: Use plugin instance for cross-system communication
- **Consistent patterns**: Follow established patterns throughout codebase
- **Readable names**: Use descriptive variable and method names

### Testing Considerations
- **Null checks**: Always check for null players/entities
- **State validation**: Verify state before operations
- **Edge cases**: Handle disconnections, server restarts, etc.
- **Cleanup**: Ensure proper cleanup in onDisable()

## Common Pitfalls to Avoid

1. **Don't block main thread**: Use async for database and heavy operations
2. **Don't forget null checks**: Players can disconnect at any time
3. **Don't skip state validation**: Check match/queue state before operations
4. **Don't hardcode messages**: Use MessageService for all user-facing text
5. **Don't forget cleanup**: Cancel tasks, clear maps, close connections in onDisable()
6. **Don't use deprecated APIs**: Use Paper/Adventure APIs, not legacy Bukkit
7. **Don't ignore events**: Fire custom events for extensibility
8. **Don't skip validation**: Validate arena/kit configuration before use
