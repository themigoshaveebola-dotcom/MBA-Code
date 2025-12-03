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
import me.x_tias.partix.database.PlayerDb;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AllTimeLeaderboard {
    @Getter
    private static final Map<Integer, String> trophies = new ConcurrentHashMap<>();
    @Getter
    private static final Map<Integer, String> goldSeasons = new ConcurrentHashMap<>();

    public static void setup() {
        new BukkitRunnable() {

            public void run() {
                PlayerDb.getTop(PlayerDb.Stat.CHAMPIONSHIPS, 20).thenAccept(topTrophies -> {
                    for (int i = 1; i < 11; ++i) {
                        Object value;
                        String name;
                        if (topTrophies.get(i) == null) {
                            name = "None";
                            value = "0 Trophies";
                        } else {
                            OfflinePlayer player = Bukkit.getOfflinePlayer(topTrophies.get(i));
                            if (player.hasPlayedBefore() || player.isOnline()) {
                                name = player.getName();
                                value = PlayerDb.get(player.getUniqueId(), PlayerDb.Stat.CHAMPIONSHIPS) + " Trophies";
                            } else {
                                name = "None";
                                value = "0 Trophies";
                            }
                        }
                        if (i == 1) {
                            trophies.put(1, "§e1. " + name + " §f- §e" + value);
                            continue;
                        }
                        if (i == 2) {
                            trophies.put(1, "§f2. " + name + " §7- §f" + value);
                            continue;
                        }
                        if (i == 3) {
                            trophies.put(1, "§43. " + name + " §7- §4" + value);
                            continue;
                        }
                        trophies.put(i, "§e" + i + ". " + name + " §7- §7" + value);
                    }
                });
                PlayerDb.getTop(PlayerDb.Stat.SEASONS_GOLD, 20).thenAccept(topGold -> {
                    for (int i = 1; i < 11; ++i) {
                        Object value;
                        String name;
                        if (topGold.get(i) == null) {
                            return;
                        }
                        OfflinePlayer player = Bukkit.getOfflinePlayer(topGold.get(i));
                        if (player.hasPlayedBefore() || player.isOnline()) {
                            name = player.getName();
                            value = PlayerDb.get(player.getUniqueId(), PlayerDb.Stat.SEASONS_GOLD) + " Seasons";
                        } else {
                            name = "None";
                            value = "0";
                        }
                        if (i == 1) {
                            goldSeasons.put(1, "§e1. " + name + " §f- §e" + value);
                            continue;
                        }
                        if (i == 2) {
                            goldSeasons.put(2, "§f2. " + name + " §7- §f" + value);
                            continue;
                        }
                        if (i == 3) {
                            goldSeasons.put(3, "§43. " + name + " §7- §4" + value);
                            continue;
                        }
                        goldSeasons.put(i, "§e" + i + ". " + name + " §7- §7" + value);
                    }
                });
            }
        }.runTaskTimerAsynchronously(Partix.getInstance(), 30L, 600L);
    }

}

