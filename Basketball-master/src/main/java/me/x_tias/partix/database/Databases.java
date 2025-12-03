/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 */
package me.x_tias.partix.database;

import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.UUID;

public class Databases {
    public static void setup() {
        try {
            BasketballDb.setup();
            SeasonDb.setup();
            PlayerDb.setup();
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
        return "jdbc:mysql://sql1.revivenode.com:3306/";
    }

    public static String getName() {
        return "s33066_MBA";
    }

    public static String getUsername() {
        return "u33066_lxlvlVUN8X";
    }

    public static String getPassword() {
        return "=j22tJcO3+=vXg@jFNSzma6L";
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

