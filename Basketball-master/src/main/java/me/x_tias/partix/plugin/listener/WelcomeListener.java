/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  org.bukkit.GameMode
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.event.player.PlayerLoginEvent
 *  org.bukkit.event.player.PlayerLoginEvent$Result
 *  org.bukkit.event.player.PlayerQuitEvent
 */
package me.x_tias.partix.plugin.listener;

import me.x_tias.partix.database.Databases;
import me.x_tias.partix.plugin.athlete.Athlete;
import me.x_tias.partix.plugin.athlete.AthleteManager;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class WelcomeListener
        implements Listener {
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        e.joinMessage(Component.empty());
        player.getActivePotionEffects().clear();
        player.setFoodLevel(20);
        player.setCollidable(false);
        Athlete athlete = AthleteManager.create(player);
        player.setGameMode(GameMode.ADVENTURE);
        Databases.create(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        e.quitMessage(Component.empty());
        Athlete athlete = AthleteManager.remove(player);
        athlete.getPlace().quit(athlete);
    }

    @EventHandler
    public void onTest(PlayerLoginEvent e) {
        if (e.getResult().equals(PlayerLoginEvent.Result.KICK_WHITELIST)) {
            e.kickMessage(Component.text("§6§lYou are not whitelisted\n\n\n"));
            return;
        }
        if (e.getResult().equals(PlayerLoginEvent.Result.KICK_BANNED)) {
            e.kickMessage(Component.text("§6§lYou are blacklisted\n\n§fYou can appeal your blacklist on our §ddiscord§f!\n\n\n§7Partix Store: §eComing Soon\n§7Partix Discord: §ehttps:\n"));
            return;
        }
        if (e.getResult().equals(PlayerLoginEvent.Result.KICK_FULL)) {
            if (e.getPlayer().hasPermission("rank.vip")) {
                e.allow();
                return;
            }
            e.kickMessage(Component.text("§6§lThe server is full (50/50)\n\n§fOur §a§lVIP §r§frank allows you to bypass this limit!\n\n\n§7Partix Store: §Coming Soon\n§7Partix Discord: §ehttps:\n"));
        }
    }
}

