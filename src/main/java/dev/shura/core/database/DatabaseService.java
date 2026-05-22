package dev.shura.core.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.shura.core.ShuraCore;

import java.io.File;
import java.sql.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;

public class DatabaseService {

    private final ShuraCore plugin;
    private HikariDataSource dataSource;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    public DatabaseService(ShuraCore plugin) {
        this.plugin = plugin;
    }

    public boolean connect() {
        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists() && !dataFolder.mkdirs()) {
                plugin.getLogger().severe("Failed to create plugin data folder.");
                return false;
            }
            
            File dbFile = new File(dataFolder, "shura.db");
            
            HikariConfig hikari = new HikariConfig();
            hikari.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            hikari.setDriverClassName("org.sqlite.JDBC");
            hikari.setMaximumPoolSize(10);
            hikari.setMinimumIdle(2);
            // Shorter connection timeout for faster failure detection
            hikari.setConnectionTimeout(10000); // 10 seconds instead of 30
            hikari.setIdleTimeout(600000);
            hikari.setMaxLifetime(1800000);
            hikari.setPoolName("ShuraCore-Pool");
            hikari.setValidationTimeout(5000);
            hikari.setInitializationFailTimeout(10000);

            java.util.logging.Logger.getLogger("dev.shura.core.libs.hikari").setLevel(java.util.logging.Level.OFF);
            dataSource = new HikariDataSource(hikari);
            createTables();
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to connect to database.", e);
            return false;
        }
    }

    private void createTables() throws SQLException {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS profiles (
                    uuid VARCHAR(36) PRIMARY KEY,
                    name VARCHAR(16) NOT NULL,
                    tierlist_elo TEXT NOT NULL DEFAULT '{}',
                    tierlist_matches TEXT NOT NULL DEFAULT '{}',
                    stats TEXT NOT NULL DEFAULT '{}',
                    last_seen BIGINT NOT NULL DEFAULT 0
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS kits (
                    id VARCHAR(36) PRIMARY KEY,
                    name VARCHAR(64) NOT NULL,
                    tierlist_id VARCHAR(64),
                    inventory TEXT NOT NULL DEFAULT '{}',
                    armor TEXT NOT NULL DEFAULT '{}',
                    effects TEXT NOT NULL DEFAULT '[]',
                    rules TEXT NOT NULL DEFAULT '{}',
                    created_at BIGINT NOT NULL DEFAULT 0
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS arenas (
                    id VARCHAR(36) PRIMARY KEY,
                    name VARCHAR(64) NOT NULL,
                    world VARCHAR(64) NOT NULL,
                    pos1 VARCHAR(128) NOT NULL DEFAULT '',
                    pos2 VARCHAR(128) NOT NULL DEFAULT '',
                    spawn_a VARCHAR(128) NOT NULL DEFAULT '',
                    spawn_b VARCHAR(128) NOT NULL DEFAULT '',
                    kit_id VARCHAR(36),
                    copies TEXT NOT NULL DEFAULT '[]',
                    enabled INTEGER NOT NULL DEFAULT 1
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS match_history (
                    id VARCHAR(36) PRIMARY KEY,
                    tierlist_id VARCHAR(64),
                    kit_id VARCHAR(36) NOT NULL,
                    match_type VARCHAR(32) NOT NULL,
                    winner_uuid VARCHAR(36) NOT NULL,
                    loser_uuid VARCHAR(36) NOT NULL,
                    winner_elo_change INT NOT NULL DEFAULT 0,
                    loser_elo_change INT NOT NULL DEFAULT 0,
                    duration BIGINT NOT NULL DEFAULT 0,
                    played_at BIGINT NOT NULL DEFAULT 0
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS command_whitelist (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    command VARCHAR(64) NOT NULL,
                    lp_group VARCHAR(64) NOT NULL,
                    allowed INTEGER NOT NULL DEFAULT 1,
                    UNIQUE (command, lp_group)
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS server_settings (
                    setting_key VARCHAR(64) PRIMARY KEY,
                    setting_value TEXT NOT NULL
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS player_kits (
                    player_uuid VARCHAR(36) NOT NULL,
                    kit_id VARCHAR(36) NOT NULL,
                    inventory TEXT NOT NULL,
                    armor TEXT NOT NULL,
                    PRIMARY KEY (player_uuid, kit_id)
                )
            """);
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    // Async query — returns result
    public <T> CompletableFuture<T> query(String sql, StatementConsumer prepare, Function<ResultSet, T> mapper) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                if (prepare != null) prepare.accept(stmt);
                try (ResultSet rs = stmt.executeQuery()) {
                    return mapper.apply(rs);
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Query failed: " + sql, e);
                return null;
            }
        }, executor);
    }

    // Async update — INSERT, UPDATE, DELETE
    public CompletableFuture<Integer> update(String sql, StatementConsumer prepare) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                if (prepare != null) prepare.accept(stmt);
                return stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Update failed: " + sql, e);
                return -1;
            }
        }, executor);
    }

    // Async update with no return
    public void updateAsync(String sql, StatementConsumer prepare) {
        update(sql, prepare);
    }

    // Async update with callback
    public void updateAsync(String sql, StatementConsumer prepare, Consumer<Integer> callback) {
        update(sql, prepare).thenAccept(callback);
    }

    // Get or create a server setting
    public CompletableFuture<String> getSetting(String key, String defaultValue) {
        return query("SELECT setting_value FROM server_settings WHERE setting_key = ?",
                stmt -> stmt.setString(1, key),
                rs -> {
                    try {
                        if (rs.next()) return rs.getString("setting_value");
                    } catch (SQLException e) {
                        plugin.getLogger().log(Level.SEVERE, "Failed to get setting: " + key, e);
                    }
                    return defaultValue;
                });
    }

    public void setSetting(String key, String value) {
        updateAsync("INSERT INTO server_settings (setting_key, setting_value) VALUES (?, ?) " +
                "ON CONFLICT(setting_key) DO UPDATE SET setting_value = excluded.setting_value",
                stmt -> {
                    stmt.setString(1, key);
                    stmt.setString(2, value);
                });
    }

    public void disconnect() {
        executor.shutdown();
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    public boolean isConnected() {
        return dataSource != null && !dataSource.isClosed();
    }

    @FunctionalInterface
    public interface StatementConsumer {
        void accept(PreparedStatement stmt) throws SQLException;
    }
}
