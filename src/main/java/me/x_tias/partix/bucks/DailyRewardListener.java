/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerJoinEvent
 */
package me.x_tias.partix.bucks;

import me.x_tias.partix.database.PlayerDb;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

public class DailyRewardListener
        implements Listener {
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        LocalDate todayNY = LocalDate.now(ZoneId.of("America/New_York"));
        long epochDayNow = todayNY.toEpochDay();

        DailyRewardDb.getLastDailyClaim(uuid).thenAccept(lastClaim -> {
            if (epochDayNow != lastClaim) {
                PlayerDb.add(uuid, PlayerDb.Stat.MBA_BUCKS, 50);
                DailyRewardDb.setLastDailyClaim(uuid, epochDayNow);
                event.getPlayer().sendMessage("§aYou have received your daily reward of 100 MBA Bucks!");
                Bukkit.getLogger().info("Gave " + event.getPlayer().getName() + " daily reward");
            }
        });
    }
}

