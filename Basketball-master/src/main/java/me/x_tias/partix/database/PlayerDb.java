/*
 * Decompiled with CFR 0.152.
 */
package me.x_tias.partix.database;

import lombok.Getter;
import me.x_tias.partix.Partix;
import org.bukkit.Bukkit;

import java.sql.*;
import java.util.HashMap;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class PlayerDb {
    private static final String DATABASE = "players";
    private static final String url = Databases.getUrl();
    private static final String dbName = Databases.getName();
    private static final String username = Databases.getUsername();
    private static final String password = Databases.getPassword();
    private static final Logger logger = Partix.getInstance().getLogger();

    private static Connection getConnection() throws SQLException {
        String connectionUrl = url + dbName + "?autoReconnect=true";
        Properties connectionProps = new Properties();
        connectionProps.put("user", username);
        connectionProps.put("password", password);
        return DriverManager.getConnection(connectionUrl, connectionProps);
    }

    public static void setup() {
        Bukkit.getScheduler().runTaskAsynchronously(Partix.getInstance(), () -> {
            try (Connection connection = PlayerDb.getConnection();
                 Statement statement = connection.createStatement()) {
                String createTableQuery = "CREATE TABLE IF NOT EXISTS `players` (`uuid` VARCHAR(36) PRIMARY KEY, `ign` VARCHAR(255) DEFAULT '$UnknownName')";
                statement.execute(createTableQuery);
                for (Stat stat : Stat.values()) {
                    String columnName = stat.name();
                    if (PlayerDb.columnExists(connection, columnName)) continue;
                    String addColumnQuery = "ALTER TABLE `players` ADD COLUMN `" + columnName + "` INT DEFAULT " + stat.getDefaultValue();
                    statement.execute(addColumnQuery);
                    logger.info("✅ Added missing column: " + columnName);
                }
            } catch (SQLException e) {
                logger.severe("❌ Database setup failed: " + e.getMessage());
            }
        });
    }

    /*
     * Enabled aggressive exception aggregation
     */
    private static boolean columnExists(Connection connection, String columnName) {
        try (PreparedStatement stmt = connection.prepareStatement("SHOW COLUMNS FROM `players` LIKE ?")) {
            boolean bl;
            block14:
            {
                stmt.setString(1, columnName);
                ResultSet rs = stmt.executeQuery();
                try {
                    bl = rs.next();
                    if (rs == null) break block14;
                } catch (Throwable throwable) {
                    if (rs != null) {
                        try {
                            rs.close();
                        } catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                rs.close();
            }
            return bl;
        } catch (SQLException e) {
            logger.severe("❌ Error checking column: " + columnName + " - " + e.getMessage());
            return false;
        }
    }

    public static void create(UUID playerUUID, String playerName) {
        Bukkit.getScheduler().runTaskAsynchronously(Partix.getInstance(), () -> {
            try (Connection connection = PlayerDb.getConnection();
                 PreparedStatement statement = connection.prepareStatement("INSERT IGNORE INTO players (uuid, ign) VALUES (?, ?)")) {
                statement.setString(1, playerUUID.toString());
                statement.setString(2, playerName);
                statement.execute();
            } catch (SQLException e) {
                logger.severe("❌ Failed to create player record: " + e.getMessage());
            }
        });
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    public static CompletableFuture<Integer> get(UUID playerUUID, Stat stat) {
        final CompletableFuture<Integer> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(Partix.getInstance(), () -> {
            try (Connection connection = PlayerDb.getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT " + stat.name() + " FROM players WHERE uuid = ?")) {
                statement.setString(1, playerUUID.toString());
                try (ResultSet rs = statement.executeQuery()) {
                    if (!rs.next()) {
                        future.complete(stat.getDefaultValue());
                        return;
                    }
                    future.complete(rs.getInt(stat.name()));
                    return;
                }
            } catch (SQLException e) {
                logger.severe("❌ Failed to get stat " + stat.name() + ": " + e.getMessage());
            }
            future.complete(stat.getDefaultValue());
        });
        return future;
    }

    public static void add(UUID playerUUID, Stat stat, int amount) {
        Bukkit.getScheduler().runTaskAsynchronously(Partix.getInstance(), () -> {
            try (Connection connection = PlayerDb.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "UPDATE players SET " + stat.name() + " = COALESCE(" + stat.name() + ", 0) + ? WHERE uuid = ?")) {
                statement.setInt(1, amount);
                statement.setString(2, playerUUID.toString());
                int rowsAffected = statement.executeUpdate();
                if (rowsAffected == 0) {
                    logger.warning("⚠️ No rows updated for player " + playerUUID + " stat " + stat.name());
                }
            } catch (SQLException e) {
                logger.severe("❌ Failed to add to " + stat.name() + ": " + e.getMessage());
            }
        });
    }
    public static void remove(UUID playerUUID, Stat stat, int amount) {
        if (amount <= 0) {
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(Partix.getInstance(), () -> {
            try (Connection connection = PlayerDb.getConnection();
                 PreparedStatement statement = connection.prepareStatement("UPDATE players SET " + stat.name() + " = GREATEST(0, " + stat.name() + " - ?) WHERE uuid = ?")) {
                statement.setInt(1, amount);
                statement.setString(2, playerUUID.toString());
                statement.execute();
            } catch (SQLException e) {
                logger.severe("❌ Failed to remove from " + stat.name() + ": " + e.getMessage());
            }
        });
    }

    public static void set(UUID playerUUID, Stat stat, int value) {
        Bukkit.getScheduler().runTaskAsynchronously(Partix.getInstance(), () -> {
            try (Connection connection = PlayerDb.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "INSERT INTO players (uuid, " + stat.name() + ") VALUES (?, ?) " +
                                 "ON DUPLICATE KEY UPDATE " + stat.name() + " = ?")) {
                statement.setString(1, playerUUID.toString());
                statement.setInt(2, value);
                statement.setInt(3, value);
                statement.execute();
            } catch (SQLException e) {
                logger.severe("❌ Failed to set " + stat.name() + ": " + e.getMessage());
            }
        });
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    public static CompletableFuture<String> getName(UUID playerUUID) {
        final CompletableFuture<String> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(Partix.getInstance(), () -> {
            try (Connection connection = PlayerDb.getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT ign FROM players WHERE uuid = ?")) {
                statement.setString(1, playerUUID.toString());
                try (ResultSet rs = statement.executeQuery()) {
                    if (!rs.next()) {
                        future.complete("$NotFound");
                        return;
                    }
                    future.complete(rs.getString("ign"));
                    return;
                }
            } catch (SQLException e) {
                logger.severe("❌ Failed to get player name: " + e.getMessage());
            }
            future.complete("$NotFound");
        });

        return future;
    }

    public static CompletableFuture<HashMap<Integer, UUID>> getTop(Stat stat, int n) {
        HashMap<Integer, UUID> map = new HashMap<>();
        final CompletableFuture<HashMap<Integer, UUID>> future = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(Partix.getInstance(), () -> {
            try (Connection connection = PlayerDb.getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT uuid, " + stat.name() + " FROM players ORDER BY " + stat.name() + " DESC LIMIT ?")) {
                statement.setInt(1, n);
                try (ResultSet rs = statement.executeQuery()) {
                    int rank = 1;
                    while (rs.next()) {
                        map.put(rank, UUID.fromString(rs.getString("uuid")));
                        ++rank;
                    }
                }
                future.complete(map);
            } catch (SQLException e) {
                logger.severe("❌ Failed to fetch top players for " + stat.name() + ": " + e.getMessage());
            }
            future.complete(map);
        });

        return future;
    }

    @Getter
    public enum Stat {
        CHAMPIONSHIPS(0),
        SEASONS_GOLD(0),
        EXP(0),
        COINS(0),
        TRAIL(0),
        EXPLOSION(0),
        GREEN_SOUND(0),
        RENDER(0),
        WINSONG(0),
        BALL_TRAIL(0),
        DEFAULT_COSMETICS(0),
        MBA_BUCKS(0),

        // Career Record
        CAREER_WINS(0),
        CAREER_LOSSES(0),
        CAREER_GAMES_PLAYED(0),

        // Career Stats
        CAREER_POINTS(0),
        CAREER_ASSISTS(0),
        CAREER_REBOUNDS(0),
        CAREER_STEALS(0),
        CAREER_BLOCKS(0),
        CAREER_TURNOVERS(0),
        CAREER_FG_MADE(0),
        CAREER_FG_ATTEMPTED(0),
        CAREER_3FG_MADE(0),
        CAREER_3FG_ATTEMPTED(0),
        CAREER_THREES(0),

        // Season 1 Record
        SEASON_1_WINS(0),
        SEASON_1_LOSSES(0),
        SEASON_1_GAMES_PLAYED(0),

        // Season 1 Stats
        SEASON_1_POINTS(0),
        SEASON_1_ASSISTS(0),
        SEASON_1_REBOUNDS(0),
        SEASON_1_STEALS(0),
        SEASON_1_BLOCKS(0),
        SEASON_1_TURNOVERS(0),
        SEASON_1_FG_MADE(0),
        SEASON_1_FG_ATTEMPTED(0),
        SEASON_1_3FG_MADE(0),
        SEASON_1_3FG_ATTEMPTED(0),
        SEASON_1_THREES(0);

        private final int defaultValue;

        Stat(int defaultValue) {
            this.defaultValue = defaultValue;
        }
    }
}

