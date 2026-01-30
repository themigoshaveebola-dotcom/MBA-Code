/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 */
package me.x_tias.partix.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.UUID;

import org.bukkit.entity.Player;

public class Databases {
    public static void setup() {
        try {
            BasketballDb.setup();
            SeasonDb.setup();
            PlayerDb.setup();
            // PlayerDb.fixCrateDataColumn(); // Method doesn't exist
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void create(Player player) {
        UUID uuid = player.getUniqueId();
        BasketballDb.create(uuid);
        SeasonDb.create(uuid);
        PlayerDb.create(uuid, player.getName());
    }

    public static String getUrl() {
        return "jdbc:mysql://us01-sql.pebblehost.com:3306/";
    }

    public static String getName() {
        return "customer_1273413_minecraftbasketball";
    }

    public static String getUsername() {
        return "customer_1273413_minecraftbasketball";
    }

    public static String getPassword() {
        return "MhHWg^4.D3jK.wk.e=daMB2H";
    }

    public static boolean contains(UUID playerId, String key) {
        return false;
    }

    public static Connection getConnection() throws SQLException {
        String connectionUrl = Databases.getUrl() + Databases.getName() + "?autoReconnect=true";
        Properties props = new Properties();
        props.setProperty("user", Databases.getUsername());
        props.setProperty("password", Databases.getPassword());
        return DriverManager.getConnection(connectionUrl, props);
    }
}

