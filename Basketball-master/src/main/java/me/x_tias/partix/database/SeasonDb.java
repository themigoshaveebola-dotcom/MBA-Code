/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.bukkit.Bukkit
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

public class SeasonDb {
    private static final String DATABASE = "season";
    private static final String url;
    private static final String dbName;
    private static final String username;
    private static final String password;
    private static final Logger logger;
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
            try (Connection connection = SeasonDb.getConnection();
                 Statement statement = connection.createStatement()) {
                String createTableQuery = "CREATE TABLE IF NOT EXISTS `season` (`uuid` VARCHAR(36) PRIMARY KEY)";
                statement.execute(createTableQuery);
                for (Stat stat : Stat.values()) {
                    String columnName = stat.name();
                    String addColumnQuery = "ALTER TABLE `season` ADD COLUMN IF NOT EXISTS `" + columnName + "` INT DEFAULT " + stat.getDefaultValue();
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
            try (Connection connection = SeasonDb.getConnection();
                 PreparedStatement statement = connection.prepareStatement("INSERT IGNORE INTO season (uuid) VALUES (?)")) {
                statement.setString(1, playerUUID.toString());
                statement.execute();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to create record.", e);
            }
        });
    }

    public static void add(UUID playerUUID, boolean isWinner, boolean isMVP) {
        if (playerUUID == null) {
            return;
        }
        int points = 0;
        points = isWinner ? (points += 2) : --points;
        if (isMVP) {
            ++points;
        }
        Bukkit.getLogger().info("[DEBUG] Adding " + points + " points to player " + playerUUID);

        int finalPoints = points;
        Bukkit.getScheduler().runTaskAsynchronously(Partix.getInstance(), () -> {
            try (Connection connection = SeasonDb.getConnection();
                 PreparedStatement statement = connection.prepareStatement("UPDATE season SET POINTS = POINTS + ? WHERE uuid = ?")) {
                statement.setInt(1, finalPoints);
                statement.setString(2, playerUUID.toString());
                statement.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to update ranked points.", e);
            }
        });
    }

    public static void remove(UUID playerUUID, Stat stat, int amount) {
        if (playerUUID == null) {
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(Partix.getInstance(), () -> {
            try (Connection connection = SeasonDb.getConnection();
                 PreparedStatement statement = connection.prepareStatement("UPDATE season SET " + stat.name() + " = GREATEST(0, " + stat.name() + " - ?) WHERE uuid = ?")) {
                statement.setInt(1, amount);
                statement.setString(2, playerUUID.toString());
                statement.execute();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to create record.", e);
            }
        });
    }

    /**
     * Fetches the specified stat for a player identified by their UUID.
     *
     * @param playerUUID The UUID of the player.
     * @param stat       The stat to retrieve.
     * @return The value of the stat, -1 if playerUUID is null, or 0 if no data is found.
     * @throws RuntimeException If a database error occurs.
     */
    public static CompletableFuture<Integer> get(UUID playerUUID, Stat stat) {
        if (playerUUID == null) {
            return CompletableFuture.completedFuture(-1); // If playerUUID is null, return -1 as an invalid input indicator.
        }

        String query = "SELECT " + stat.name() + " FROM season WHERE uuid = ?";

        final CompletableFuture<Integer> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(Partix.getInstance(), () -> {
            // Use try-with-resources to automatically close resources
            try (Connection connection = SeasonDb.getConnection();
                 PreparedStatement statement = connection.prepareStatement(query)) {

                // Set the UUID in the query
                statement.setString(1, playerUUID.toString());

                // Execute the query and process the ResultSet
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        future.complete(resultSet.getInt(stat.name()));
                        return;
                    }
                }

                future.complete(0);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to get record.", e); // Wrap and rethrow the exception
            }
        });

        return future;
    }

    public static CompletableFuture<HashMap<Integer, UUID>> getTop(Stat stat, int n) {
        HashMap<Integer, UUID> map = new HashMap<>();
        final CompletableFuture<HashMap<Integer, UUID>> future = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(Partix.getInstance(), () -> {
            try (Connection connection = SeasonDb.getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT uuid, " + stat.name() + " FROM season ORDER BY " + stat.name() + " DESC LIMIT ?")) {
                statement.setInt(1, n);
                try (ResultSet rs = statement.executeQuery()) {
                    int rank = 1;
                    while (rs.next()) {
                        map.put(rank, UUID.fromString(rs.getString("uuid")));
                        ++rank;
                    }
                }
                future.complete(map);
                return;
            } catch (Exception exception) {
                // empty catch block
                future.complete(map);
            }
            future.complete(map); // Ensure we complete the future even if an exception occurs
        });

        return future;
    }

    public static void setAll(Stat stat, int value) {
        Bukkit.getScheduler().runTaskAsynchronously(Partix.getInstance(), () -> {
            try (Connection connection = SeasonDb.getConnection();
                 PreparedStatement statement = connection.prepareStatement("UPDATE season SET " + stat.name() + " = ?")) {
                statement.setInt(1, value);
                statement.execute();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to set all players' value.", e);
            }
        });
    }

    public static void set(UUID playerUUID, Stat stat, int value) {
        if (playerUUID == null) {
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(Partix.getInstance(), () -> {
            try (Connection connection = SeasonDb.getConnection();
                 PreparedStatement statement = connection.prepareStatement("UPDATE season SET " + stat.name() + " = ? WHERE uuid = ?")) {
                statement.setInt(1, value);
                statement.setString(2, playerUUID.toString());
                statement.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to set stat value.", e);
            }
        });
    }

    @Getter
    public enum Stat {
        WINS(0),
        LOSSES(0),
        POINTS(0);

        private final int defaultValue;

        Stat(int defaultValue) {
            this.defaultValue = defaultValue;
        }

    }
}

