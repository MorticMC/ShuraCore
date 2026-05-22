# ShuraCore - Technology Stack

## Programming Languages
- **Java 21**: Primary development language
- **YAML**: Configuration files
- **XML**: Maven build configuration

## Build System

### Maven
- **Version**: Maven 3.x
- **Artifact**: ShuraCore-1.0.0.jar
- **Packaging**: JAR with shaded dependencies
- **Encoding**: UTF-8

### Build Commands
```bash
# Compile the project
mvn clean compile

# Package into JAR
mvn clean package

# Install to local repository
mvn clean install

# Skip tests during build
mvn clean package -DskipTests
```

### Maven Shade Plugin
- **Version**: 3.5.3
- **Purpose**: Bundle dependencies into final JAR
- **Relocations**: Prevents conflicts with other plugins
  - `com.zaxxer.hikari` → `dev.shura.core.libs.hikari`
  - `dev.triumphteam.gui` → `dev.shura.core.libs.gui`
  - `fr.mrmicky.fastboard` → `dev.shura.core.libs.fastboard`

## Core Dependencies

### Minecraft Platform
- **Paper API 1.21.1-R0.1-SNAPSHOT** (provided)
  - Modern Minecraft server API
  - Repository: https://repo.papermc.io/repository/maven-public/

### Database
- **HikariCP 5.1.0** (compiled)
  - High-performance JDBC connection pooling
  - Shaded to avoid conflicts
- **SQLite JDBC 3.47.1.0** (compiled)
  - Embedded database driver
  - Native libraries filtered for Linux x86_64 only

### GUI Framework
- **Triumph GUI 3.1.14-SNAPSHOT** (compiled)
  - Inventory-based GUI framework
  - Repository: https://repo.triumphteam.dev/snapshots/
  - Shaded to avoid conflicts

### Scoreboard
- **FastBoard 2.1.5** (compiled)
  - Lightweight scoreboard library
  - Shaded to avoid conflicts

### Permissions
- **LuckPerms API 5.4** (provided)
  - Permission system integration
  - Repository: https://oss.sonatype.org/content/repositories/snapshots/

## Development Environment

### Java Requirements
- **JDK**: Java 21 or higher
- **Compiler Source**: Java 21
- **Compiler Target**: Java 21

### IDE Setup
- Import as Maven project
- Enable annotation processing
- Set project SDK to Java 21

### Server Requirements
- **Minecraft Version**: 1.21.1
- **Server Software**: Paper or compatible fork
- **Required Plugins**: LuckPerms
- **Java Runtime**: Java 21+

## Project Structure

### Source Directories
- `src/main/java/`: Java source code
- `src/main/resources/`: Configuration files and plugin.yml

### Build Output
- `target/ShuraCore.jar`: Final shaded JAR
- `target/original-ShuraCore.jar`: Pre-shade JAR
- `target/classes/`: Compiled class files

### Local Maven Repository
- `.m2/repository/`: Cached dependencies

## Configuration Files

### Plugin Configuration (YAML)
- `config.yml`: Main plugin settings
- `messages.yml`: Localized messages
- `kits-arenas.yml`: Kit and arena definitions
- `scoreboards-sounds.yml`: Scoreboard templates and sounds
- `gui.yml`: Main GUI configuration
- `cracked-queue-gui.yml`: Cracked queue interface
- `premium-queue-gui.yml`: Premium queue interface
- `mctiers-queue-gui.yml`: MCTiers queue interface

### Plugin Metadata
- `plugin.yml`: Plugin information, commands, and permissions

## Database

### SQLite Configuration
- **Type**: Embedded file-based database
- **Location**: Plugin data folder
- **Connection Pool**: HikariCP
- **Schema**: Managed by DatabaseService

## External Integrations

### LuckPerms
- **Purpose**: Permission and rank management
- **Integration**: RankManager uses LuckPerms API
- **Required**: Plugin will not load without LuckPerms

## Performance Optimizations

### Dependency Shading
- Relocates dependencies to prevent classpath conflicts
- Reduces plugin load time

### SQLite Native Libraries
- Filtered to include only Linux x86_64 natives
- Reduces JAR size significantly
- Excludes: Android, Musl, ARM variants, Mac, Windows, BSD

### Connection Pooling
- HikariCP provides efficient database connection management
- Reduces database overhead

## Development Workflow

### Local Development
1. Clone repository
2. Import Maven project in IDE
3. Run `mvn clean install`
4. Copy `target/ShuraCore.jar` to test server plugins folder
5. Start Paper server with Java 21

### Testing
- Manual testing on Paper 1.21.1 server
- Requires LuckPerms plugin installed
- Test with both cracked and premium accounts

### Deployment
1. Build with `mvn clean package`
2. Verify `target/ShuraCore.jar` exists
3. Deploy to production server
4. Restart server or use plugin reload (if supported)

## Version Information
- **Plugin Version**: 1.0.0
- **API Version**: 1.21
- **Author**: PureMortic
- **Description**: ShuraPvP Duels & Practice Core
