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

public class BasketballDb {
    public static final Logger logger;
    private static final String DATABASE = "basketball";
    private static final String url;
    private static final String dbName;
    private static final String username;
    private static final String password;
    private static Connection connection;

    static {
        url = Databases.getUrl();
        dbName = Databases.getName();
        username = Databases.getUsername();
        password = Databases.getPassword();
        logger = Partix.getInstance().getLogger();
    }

    private static Connection getConnection() throws SQLException {
        String connectionUrl = url + dbName + "?autoReconnect=true";
        Properties connectionProps = new Properties();
        connectionProps.put("user", username);
        connectionProps.put("password", password);
        return DriverManager.getConnection(connectionUrl, connectionProps);
    }

    public static void setup() throws SQLException {
        Bukkit.getScheduler().runTaskAsynchronously(Partix.getInstance(), () -> {
            try (Connection connection = BasketballDb.getConnection();
                 Statement statement = connection.createStatement()) {
                String createTableQuery = "CREATE TABLE IF NOT EXISTS `basketball` (`uuid` VARCHAR(36) PRIMARY KEY)";
                statement.execute(createTableQuery);
                for (Stat stat : Stat.values()) {
                    String columnName = stat.name();
                    String addColumnQuery = "ALTER TABLE `basketball` ADD COLUMN IF NOT EXISTS `" + columnName + "` INT DEFAULT " + stat.getDefaultValue();
                    statement.execute(addColumnQuery);
                }
            } catch (SQLException e) {
                throw new RuntimeException("Failed to setup database.", e);
            }
        });
    }

    public static void create(UUID playerUUID) {
        if (playerUUID == null) {
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(Partix.getInstance(), () -> {
            try (Connection connection = BasketballDb.getConnection();
                 PreparedStatement statement = connection.prepareStatement("INSERT IGNORE INTO basketball (uuid) VALUES (?)")) {
                statement.setString(1, playerUUID.toString());
                statement.execute();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to create record.", e);
            }
        });
    }

    public static void add(UUID playerUUID, Stat stat, int amount) {
        if (playerUUID == null) {
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(Partix.getInstance(), () -> {
            try (Connection connection = BasketballDb.getConnection();
                 PreparedStatement statement = connection.prepareStatement("UPDATE basketball SET " + stat.name() + " = " + stat.name() + " + ? WHERE uuid = ?")) {
                statement.setInt(1, amount);
                statement.setString(2, playerUUID.toString());
                statement.execute();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to create record.", e);
            }
        });
    }

    public static void remove(UUID playerUUID, Stat stat, int amount) {
        if (playerUUID == null) {
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(Partix.getInstance(), () -> {
            try (Connection connection = BasketballDb.getConnection();
                 PreparedStatement statement = connection.prepareStatement("UPDATE basketball SET " + stat.name() + " = GREATEST(0, " + stat.name() + " - ?) WHERE uuid = ?")) {
                statement.setInt(1, amount);
                statement.setString(2, playerUUID.toString());
                statement.execute();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to create record.", e);
            }
        });
    }

    /**
     * Retrieves the value of a specific stat for a player.
     *
     * @param playerUUID The UUID of the player.
     * @param stat       The stat to retrieve.
     * @return The value of the stat, -1 if playerUUID is null, or 0 if no record is found.
     * @throws RuntimeException if a database error occurs.
     */
    public static CompletableFuture<Integer> get(UUID playerUUID, Stat stat) {
        // Return -1 if the playerUUID is null
        if (playerUUID == null) {
            return CompletableFuture.completedFuture(-1);
        }

        final CompletableFuture<Integer> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(Partix.getInstance(), () -> {
            // Try-with-resources to ensure proper cleanup of Connection, PreparedStatement, and ResultSet
            try (Connection connection = BasketballDb.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT " + stat.name() + " FROM basketball WHERE uuid = ?")) {

                // Set the query parameter for the UUID
                statement.setString(1, playerUUID.toString());

                // Execute the query and handle the result set
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        // Return the stat value if a record is found
                        future.complete(rs.getInt(stat.name()));
                        return;
                    }
                }

                // Return 0 if no record is found
                future.complete(0);
            } catch (SQLException e) {
                // Wrap SQLException into RuntimeException for clarity
                throw new RuntimeException("Failed to get record.", e);
            }
        });
        return future;
    }

    public static CompletableFuture<HashMap<Integer, UUID>> getTop(Stat stat, int n) {
        HashMap<Integer, UUID> map = new HashMap<>();
        final CompletableFuture<HashMap<Integer, UUID>> future = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(Partix.getInstance(), () -> {
            try (Connection connection = BasketballDb.getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT uuid, " + stat.name() + " FROM basketball ORDER BY " + stat.name() + " DESC LIMIT ?")) {
                statement.setInt(1, n);
                try (ResultSet rs = statement.executeQuery()) {
                    int rank = 1;
                    while (rs.next()) {
                        map.put(rank, UUID.fromString(rs.getString("uuid")));
                        ++rank;
                    }
                }
                future.complete(map);
            } catch (Exception exception) {
                // empty catch block
                logger.severe("Failed to get top players for stat " + stat.name() + ": " + exception.getMessage());
                future.complete(map);
            }
        });

        return future;
    }

    public static void setAll(Stat stat, int value) {
        Bukkit.getScheduler().runTaskAsynchronously(Partix.getInstance(), () -> {
            try (Connection connection = BasketballDb.getConnection();
                 PreparedStatement statement = connection.prepareStatement("UPDATE basketball SET " + stat.name() + " = ?")) {
                statement.setInt(1, value);
                statement.execute();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to set all players' value.", e);
            }
        });
    }

    @Getter
    public enum Stat {
        WINS(0),
        LOSSES(0),
        POINTS(0),
        THREES(0),
        MVP(0);

        private final int defaultValue;

        Stat(int defaultValue) {
            this.defaultValue = defaultValue;
        }

    }
}

