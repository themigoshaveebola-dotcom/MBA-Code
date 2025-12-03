/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitRunnable
 */
package me.x_tias.partix.bucks;

import me.x_tias.partix.Partix;
import me.x_tias.partix.database.PlayerDb;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MbaBucksLeaderboard {
    private static final Map<Integer, String> mbaLeaderboard = new ConcurrentHashMap<>();

    public static void setup() {
        new BukkitRunnable() {

            public void run() {
                PlayerDb.getTop(PlayerDb.Stat.MBA_BUCKS, 15).thenAccept(topPlayers -> {
                    mbaLeaderboard.clear();
                    for (int rank = 1; rank <= 15; ++rank) {
                        UUID uuid = topPlayers.get(rank);
                        if (uuid == null) {
                            mbaLeaderboard.put(rank, "§7" + rank + ". §fNone - 0 MBA");
                            continue;
                        }
                        OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
                        String name = offline.getName() != null ? offline.getName() : "Unknown";
                        int finalRank = rank;
                        PlayerDb.get(uuid, PlayerDb.Stat.MBA_BUCKS).thenAccept(bucks -> {
                            mbaLeaderboard.put(finalRank, switch (finalRank) {
                                case 1 -> "§6✯ §e1. " + name + " §f- §6" + bucks + " MBA §6✯";
                                case 2 -> "§7✦ §f2. " + name + " §f- §7" + bucks + " MBA §7✦";
                                case 3 -> "§c★ §43. " + name + " §f- §4" + bucks + " MBA §c★";
                                default -> "§e" + finalRank + ". §f" + name + " §7- §f" + bucks + " MBA";
                            });
                        });
                    }
                });
            }
        }.runTaskTimerAsynchronously(Partix.getInstance(), 20L, 600L);
    }

    public static Map<Integer, String> getLeaderboard() {
        return mbaLeaderboard;
    }

    public static String getLine(int rank) {
        return mbaLeaderboard.getOrDefault(rank, "§7" + rank + ". ...");
    }
}

