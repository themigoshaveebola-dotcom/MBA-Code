/*
 * Decompiled with CFR 0.152.
 */
package me.x_tias.partix.bucks;

import me.x_tias.partix.Partix;
import me.x_tias.partix.database.Databases;
import org.bukkit.Bukkit;

import java.sql.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DailyRewardDb {
    public static void setup() {
        Bukkit.getScheduler().runTaskAsynchronously(Partix.getInstance(), () -> {
            try (Connection conn = Databases.getConnection();
                 Statement st = conn.createStatement()) {
                st.executeUpdate("ALTER TABLE `players` ADD COLUMN IF NOT EXISTS `last_daily_claim` BIGINT DEFAULT 0");
            } catch (SQLException e) {
                Logger.getLogger("DailyRewardDb").log(Level.SEVERE, "Failed to add last_daily_claim column", e);
            }
        });
    }

    public static CompletableFuture<Long> getLastDailyClaim(UUID uuid) {
        final CompletableFuture<Long> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(Partix.getInstance(), () -> {
            long val = 0L;
            try (Connection conn = Databases.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT last_daily_claim FROM players WHERE uuid=?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        val = rs.getLong("last_daily_claim");
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            future.complete(val);
        });
        return future;
    }

    public static void setLastDailyClaim(UUID uuid, long day) {
        Bukkit.getScheduler().runTaskAsynchronously(Partix.getInstance(), () -> {
            try (Connection conn = Databases.getConnection();
                 PreparedStatement ps = conn.prepareStatement("UPDATE players SET last_daily_claim=? WHERE uuid=?")) {
                ps.setLong(1, day);
                ps.setString(2, uuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }
}

