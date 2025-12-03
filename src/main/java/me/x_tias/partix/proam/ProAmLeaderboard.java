/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitRunnable
 */
package me.x_tias.partix.proam;

import lombok.Getter;
import me.x_tias.partix.Partix;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ProAmLeaderboard {
    @Getter
    private static final Map<Integer, String> teamLeaderboard = new ConcurrentHashMap<>();

    public static void setup() {
        new BukkitRunnable() {

            public void run() {
                ProAmLeaderboard.updateLeaderboard();
            }
        }.runTaskTimerAsynchronously(Partix.getInstance(), 30L, 600L);
    }

    private static void updateLeaderboard() {
        ArrayList<ProAmTeam> sortedTeams = new ArrayList<>(Partix.getInstance().getProAmManager().getAllTeams());
        sortedTeams.sort((t1, t2) -> Integer.compare(t2.getElo(), t1.getElo()));
        teamLeaderboard.clear();
        int rank = 1;
        for (ProAmTeam team : sortedTeams) {
            if (rank > 15) break;
            String abbr = team.getName().length() >= 3 ? team.getName().substring(0, 3).toUpperCase() : team.getName().toUpperCase();
            String line = switch (rank) {
                case 1 -> "§6✯ §e1. " + team.getName() + " [" + abbr + "] - " + team.getElo() + " Elo §6✯";
                case 2 -> "§7✦ §f2. " + team.getName() + " [" + abbr + "] - " + team.getElo() + " Elo §7✦";
                case 3 -> "§c★ §43. " + team.getName() + " [" + abbr + "] - " + team.getElo() + " Elo §c★";
                default -> "§e" + rank + ". " + team.getName() + " [" + abbr + "] - " + team.getElo() + " Elo";
            };
            teamLeaderboard.put(rank, line);
            ++rank;
        }
    }

}

