/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitRunnable
 */
package me.x_tias.partix.season;

import lombok.Getter;
import me.x_tias.partix.Partix;
import me.x_tias.partix.database.SeasonDb;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SeasonLeaderboard {
    @Getter
    private static final Map<Integer, String> rankedLeaderboard = new ConcurrentHashMap<>();

    public static void setup() {
        new BukkitRunnable() {

            public void run() {
                SeasonDb.getTop(SeasonDb.Stat.POINTS, 15).thenAccept(topPlayers -> {
                    rankedLeaderboard.clear();
                    for (int i = 1; i <= 15; ++i) {
                        Object points;
                        Object record;
                        String displayName;
                        UUID playerUUID = topPlayers.get(i);
                        if (playerUUID == null) {
                            displayName = "None";
                            record = "(0-0)";
                            points = "0 Points";
                        } else {
                            OfflinePlayer player = Bukkit.getOfflinePlayer(playerUUID);
                            displayName = player.hasPlayedBefore() || player.isOnline() ? player.getName() : "Unknown";
                            int wins = SeasonDb.get(playerUUID, SeasonDb.Stat.WINS).join();
                            int losses = SeasonDb.get(playerUUID, SeasonDb.Stat.LOSSES).join();
                            int totalPoints = SeasonDb.get(playerUUID, SeasonDb.Stat.POINTS).join();
                            record = "(" + wins + "-" + losses + ")";
                            points = totalPoints + " Points";
                        }
                        String rankPrefix = switch (i) {
                            case 1 -> "§6✯ §e1. " + displayName + " " + record + " §f- §e" + points + " §6✯";
                            case 2 -> "§7✦ §f2. " + displayName + " " + record + " §7- §f" + points + " §7✦";
                            case 3 -> "§c★ §43. " + displayName + " " + record + " §7- §4" + points + " §c★";
                            default -> "§e" + i + ". " + displayName + " " + record + " §7- §7" + points;
                        };
                        rankedLeaderboard.put(i, rankPrefix);
                    }
                });
            }
        }.runTaskTimerAsynchronously(Partix.getInstance(), 30L, 600L);
    }

}

