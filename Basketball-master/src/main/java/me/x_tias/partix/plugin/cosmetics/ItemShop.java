/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.plugin.Plugin
 */
package me.x_tias.partix.plugin.cosmetics;

import me.x_tias.partix.Partix;
import org.bukkit.Bukkit;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class ItemShop
        implements Runnable {
    public static CosmeticHolder defaultTrail;
    public static CosmeticHolder defaultBorder;
    public static CosmeticHolder defaultExplosion;
    public static CosmeticHolder vipTrail;
    public static CosmeticHolder vipBorder;
    public static CosmeticHolder vipExplosion;
    public static CosmeticHolder proTrail;
    public static CosmeticHolder proBorder;
    public static CosmeticHolder proExplosion;
    private static SimpleDateFormat dateFormat;
    private static Calendar targetTime;
    private static String timeRemaining;

    public static void setup() {
        targetTime = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"));
        targetTime.set(Calendar.HOUR_OF_DAY, 20);
        targetTime.set(Calendar.MINUTE, 0);
        targetTime.set(Calendar.SECOND, 0);
        targetTime.set(Calendar.MILLISECOND, 0);
        ItemShop.refreshItems();
        Calendar currentTime = Calendar.getInstance();
        if (currentTime.after(targetTime)) {
            targetTime.add(Calendar.DAY_OF_YEAR, 1);
        }
        long delay = (targetTime.getTimeInMillis() - currentTime.getTimeInMillis()) / 50L;
        Bukkit.getServer().getScheduler().runTaskTimer(Partix.getInstance(), new ItemShop(), delay, 1728000L);
        dateFormat = new SimpleDateFormat("dd'd:'HH'h:'mm'm'");
        dateFormat.setTimeZone(targetTime.getTimeZone());
    }

    private static void refreshItems() {
        defaultExplosion = Cosmetics.randomExplosion();
        defaultTrail = Cosmetics.randomTrail();
        vipExplosion = Cosmetics.randomExplosion();
        vipTrail = Cosmetics.randomTrail();
    }

    private static void endDay() {
        ItemShop.refreshItems();
        Partix.getInstance().getLogger().info("[!!!] Item Shop Reset!");
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
            targetTime.add(Calendar.DAY_OF_YEAR, 1);
            targetTime.set(Calendar.HOUR_OF_DAY, 20);
            targetTime.set(Calendar.MINUTE, 0);
            targetTime.set(Calendar.SECOND, 0);
            targetTime.set(Calendar.MILLISECOND, 0);
            ItemShop.endDay();
        }
        if ((remainingMillis = targetTime.getTimeInMillis() - currentTime.getTimeInMillis()) <= 0L) {
            return;
        }
        timeRemaining = dateFormat.format(new Date(remainingMillis));
    }
}

