# ShuraCore - Project Structure

## Directory Organization

```
ShuraCore/
├── src/main/
│   ├── java/dev/shura/core/     # Main source code
│   └── resources/                # Configuration files
├── .amazonq/rules/memory-bank/   # Project documentation
├── pom.xml                       # Maven build configuration
└── target/                       # Compiled artifacts
```

## Core Package Structure

### Main Entry Point
- **ShuraCore.java**: Plugin main class, initialization, and lifecycle management

### Arena System (`arena/`)
Manages PvP arenas with dynamic copying and restoration
- **Arena**: Arena data model with locations and configuration
- **ArenaCopy**: Creates temporary arena instances for matches
- **ArenaManager**: Central arena registry and management
- **ArenaReset**: Handles arena restoration after matches
- **ArenaValidator**: Validates arena configuration
- **ArenaWand**: In-game tool for arena creation

### Command System (`command/`)
Command handlers organized by functionality
- **ShuraCommand**: Main admin command hub
- **DuelCommand, QueueCommand**: Matchmaking commands
- **PartyCommand**: Party management
- **PracticeCommand**: FFA practice access
- **ProfileCommand, SettingsCommand**: Player interface commands
- **util/**: Utility commands (teleport, gamemode, moderation, etc.)

### Configuration (`config/`)
Configuration file managers
- **KitsArenasConfig**: Kit and arena definitions
- **ScoreboardSoundsConfig**: Scoreboard and sound settings

### Database (`database/`)
Data persistence layer
- **DatabaseService**: SQLite database with HikariCP connection pooling

### Duel System (`duel/`)
Player-to-player challenge system
- **DuelManager**: Manages duel requests and matchmaking
- **DuelRequest**: Duel request data model

### Event System (`event/`)
Custom plugin events for extensibility
- Match events: MatchStartEvent, MatchEndEvent, MatchDeathEvent, etc.
- Party events: PartyCreateEvent, PartyJoinEvent, PartyDisbandEvent
- Queue events: QueueJoinEvent, QueueLeaveEvent

### GUI System (`gui/`)
Interactive inventory-based interfaces
- **editor/**: Dynamic GUI editor system
- Queue GUIs: CrackedQueueGui, PremiumQueueGui, MCTiersQueueGui
- Game GUIs: KitSelectorGui, KitEditorGui, ArenaEditorGui
- Info GUIs: ProfileGui, RankingsGui, SettingsGui
- Utility: GuiConfig, GuiUtil, ConfirmationGui

### Kit System (`kit/`)
Loadout management
- **Kit**: Kit data model with items and configuration
- **KitManager**: Kit registry and loading
- **KitEditor**: Player kit customization
- **KitRules**: Kit-specific gameplay rules

### Listener System (`listener/`)
Event handlers for Bukkit/Paper events
- **MatchListener**: Match gameplay events
- **PlayerListener**: Player join/quit/interaction
- **QueueListener**: Queue-related events
- **PartyListener**: Party events
- **ChatListener**: Chat formatting and filtering
- **MaintenanceListener**: Maintenance mode enforcement
- **SpectatorListener**: Spectator restrictions

### Lobby System (`lobby/`)
Spawn area management
- **LobbyItems**: Lobby item distribution

### Match System (`match/`)
Core match gameplay engine
- **Match**: Match instance with state management
- **MatchManager**: Match registry and lifecycle
- **MatchPlayer**: Player state within matches
- **MatchSnapshot**: Post-match statistics
- **MatchLogger**: Match history tracking
- **MatchFormat**: Match configuration (rounds, teams)
- **MatchType**: Match type enumeration
- **MatchState**: Match state machine

### Party System (`party/`)
Team management
- **Party**: Party data model
- **PartyManager**: Party registry and operations
- **PartyInvite**: Party invitation handling

### Profile System (`profile/`)
Player data management
- **Profile**: Player statistics and settings
- **ProfileManager**: Profile loading and caching

### Queue System (`queue/`)
Matchmaking system
- **QueueManager**: Queue registry and matchmaking logic
- **QueueEntry**: Player queue entry
- **QueueTask**: Background matchmaking task

### Utility Systems
- **chat/**: Chat management and formatting
- **maintenance/**: Maintenance mode control
- **message/**: Message service for localization
- **potion/**: Potion effect application
- **practice/**: FFA practice mode management
- **rank/**: LuckPerms integration
- **scoreboard/**: FastBoard scoreboard management
- **spectator/**: Spectator mode management
- **tab/**: Tab list management
- **tierlist/**: Ranking system
- **whitelist/**: Maintenance whitelist

### Utility Package (`util/`)
Reusable helper classes
- **ItemBuilder**: Fluent item creation
- **LocationUtil**: Location serialization
- **InventoryUtil**: Inventory operations
- **ColorUtil**: Color code processing
- **ConfigUtil**: Configuration helpers
- **JsonUtil**: JSON serialization
- **SoundUtil**: Sound playback
- **TaskUtil**: Scheduler utilities
- **CooldownManager**: Cooldown tracking
- **BlockSnapshot**: Block state capture

## Architectural Patterns

### Manager Pattern
Most systems use a manager class for centralized control:
- ArenaManager, KitManager, MatchManager, PartyManager, QueueManager, ProfileManager

### Event-Driven Architecture
Custom events allow decoupled communication between systems

### Service Layer
DatabaseService and MessageService provide abstracted functionality

### Configuration-Driven
Extensive YAML configuration for customization without code changes

### State Machine
Match system uses MatchState enum for lifecycle management

### Snapshot Pattern
MatchSnapshot captures match state for post-game analysis

### Builder Pattern
ItemBuilder provides fluent API for item creation

### Registry Pattern
Managers maintain registries of active entities (matches, parties, queues)

## Component Relationships

```
Player → Profile → Statistics/Settings
Player → Queue → Match → Arena
Player → Party → Team Match
Match → Kit → Items/Rules
Match → Arena → ArenaCopy
Match → MatchPlayer → MatchSnapshot
GUI → GuiConfig → YAML Files
Commands → Managers → Core Systems
Listeners → Events → Managers
```

## Resource Files

Configuration files in `src/main/resources/`:
- **plugin.yml**: Plugin metadata and command definitions
- **config.yml**: Main plugin configuration
- **messages.yml**: Localized messages
- **kits-arenas.yml**: Kit and arena definitions
- **scoreboards-sounds.yml**: Scoreboard and sound configuration
- **gui.yml**: Main GUI configuration
- **cracked-queue-gui.yml**: Cracked queue GUI
- **premium-queue-gui.yml**: Premium queue GUI
- **mctiers-queue-gui.yml**: MCTiers queue GUI
