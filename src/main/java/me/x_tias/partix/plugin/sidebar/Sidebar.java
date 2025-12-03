/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 *  org.bukkit.scoreboard.Criteria
 *  org.bukkit.scoreboard.DisplaySlot
 *  org.bukkit.scoreboard.Objective
 *  org.bukkit.scoreboard.Score
 *  org.bukkit.scoreboard.Scoreboard
 *  org.bukkit.scoreboard.ScoreboardManager
 */
package me.x_tias.partix.plugin.sidebar;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.HashMap;
import java.util.List;

public class Sidebar {
    private static final HashMap<Player, Scoreboard> scoreboards = new HashMap<>();

    public static void set(Player player, Component title, String... lines) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard scoreboard = scoreboards.get(player);
        if (scoreboard == null) {
            scoreboard = manager.getNewScoreboard();
            scoreboards.put(player, scoreboard);
        } else {
            for (String entry : scoreboard.getEntries()) {
                scoreboard.resetScores(entry);
            }
        }
        Objective objective = scoreboard.getObjective(DisplaySlot.SIDEBAR);
        if (objective == null) {
            objective = scoreboard.registerNewObjective("sidebar", Criteria.DUMMY, title);
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        }
        objective.displayName(title);
        int i = 100;
        for (String s : lines) {
            Score row = objective.getScore(s);
            row.setScore(i);
            --i;
        }
        player.setScoreboard(scoreboard);
    }

    public static void set(List<Player> players, Component title, String... lines) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        for (Player player : players) {
            Scoreboard scoreboard = scoreboards.get(player);
            if (scoreboard == null) {
                scoreboard = manager.getNewScoreboard();
                scoreboards.put(player, scoreboard);
            } else {
                for (String entry : scoreboard.getEntries()) {
                    scoreboard.resetScores(entry);
                }
            }
            Objective objective = scoreboard.getObjective(DisplaySlot.SIDEBAR);
            if (objective == null) {
                objective = scoreboard.registerNewObjective("sidebar", Criteria.DUMMY, title);
                objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            }
            objective.displayName(title);
            int i = 100;
            for (String s : lines) {
                Score row = objective.getScore(s);
                row.setScore(i);
                --i;
            }
            player.setScoreboard(scoreboard);
        }
    }
}

