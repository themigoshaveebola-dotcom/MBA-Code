/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Sound
 *  org.bukkit.SoundCategory
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 */
package me.x_tias.partix.season;

import me.x_tias.partix.Partix;
import me.x_tias.partix.database.SeasonDb;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.*;

public class Season
        implements Runnable {
    private static SimpleDateFormat dateFormat;
    private static Calendar targetTime;
    private static String timeRemaining;

    public static void setup() {
        targetTime = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"));
        targetTime.set(Calendar.DAY_OF_WEEK, 1);
        targetTime.set(Calendar.HOUR_OF_DAY, 21);
        targetTime.set(Calendar.MINUTE, 0);
        targetTime.set(Calendar.SECOND, 0);
        targetTime.set(Calendar.MILLISECOND, 0);
        Calendar currentTime = Calendar.getInstance();
        if (currentTime.after(targetTime)) {
            targetTime.add(Calendar.WEEK_OF_YEAR, 1);
        }
        long delay = (targetTime.getTimeInMillis() - currentTime.getTimeInMillis()) / 50L;
        Bukkit.getServer().getScheduler().runTaskTimer(Partix.getInstance(), new Season(), delay, 20L);
        dateFormat = new SimpleDateFormat("dd'd:'HH'h:'mm'm'");
        dateFormat.setTimeZone(targetTime.getTimeZone());
    }

    private static void endWeek() {
        SeasonDb.getTop(SeasonDb.Stat.POINTS, 15).thenAccept(topPoints -> {
            HashMap<Integer, UUID> map = new HashMap<>(topPoints);
            SeasonDb.setAll(SeasonDb.Stat.POINTS, 0);
            SeasonDb.setAll(SeasonDb.Stat.WINS, 0);
            SeasonDb.setAll(SeasonDb.Stat.LOSSES, 0);
            if (map.get(1) != null) {
                Player topPlayer = Partix.getPlayer(map.get(1));
                if (topPlayer != null && topPlayer.isOnline()) {
                    topPlayer.playSound(topPlayer.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER, 1.0f, 1.0f);
                }
            }
            Partix.getInstance().getLogger().info("[!!!] Season has ended! All player stats reset.");
        });
    }

    public static String getTimeRemaining() {
        Calendar currentTime = Calendar.getInstance();
        long remainingMillis = targetTime.getTimeInMillis() - currentTime.getTimeInMillis();
        if (remainingMillis <= 0L) {
            return "Countdown Over";
        }
        long seconds = remainingMillis / 1000L;
        long minutes = seconds / 60L;
        long hours = minutes / 60L;
        long days = hours / 24L;
        seconds %= 60L;
        return String.format("%dd:%02dh:%02dm", days, hours %= 24L, minutes %= 60L);
    }

    @Override
    public void run() {
        long remainingMillis;
        Calendar currentTime = Calendar.getInstance();
        if (currentTime.after(targetTime)) {
            targetTime.add(Calendar.WEEK_OF_YEAR, 1);
            targetTime.set(Calendar.DAY_OF_WEEK, 1);
            targetTime.set(Calendar.HOUR_OF_DAY, 21);
            targetTime.set(Calendar.MINUTE, 0);
            targetTime.set(Calendar.SECOND, 0);
            targetTime.set(Calendar.MILLISECOND, 0);
            Season.endWeek();
        }
        if ((remainingMillis = targetTime.getTimeInMillis() - currentTime.getTimeInMillis()) <= 0L) {
            return;
        }
        timeRemaining = dateFormat.format(new Date(remainingMillis));
    }
}

