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
import me.x_tias.partix.database.DiscordLinkDb;
import me.x_tias.partix.plugin.athlete.Athlete;
import me.x_tias.partix.plugin.athlete.AthleteManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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
        
        // Check if Discord is linked - if not, send reminder message
        DiscordLinkDb.getLinkedDiscord(player.getUniqueId()).thenAccept(discordId -> {
            if (discordId == null) {
                player.sendMessage(Component.empty());
                player.sendMessage(Component.text("⚠ ", NamedTextColor.YELLOW, TextDecoration.BOLD)
                        .append(Component.text("Your Discord account isn't linked!", NamedTextColor.RED))
                );
                player.sendMessage(Component.text("   Use ", NamedTextColor.GRAY)
                        .append(Component.text("/linkdiscord", NamedTextColor.AQUA, TextDecoration.BOLD)
                                .clickEvent(ClickEvent.runCommand("/linkdiscord")))
                        .append(Component.text(" to link your account.", NamedTextColor.GRAY))
                );
                player.sendMessage(Component.text("   Discord: ", NamedTextColor.GRAY)
                        .append(Component.text("https://discord.gg/yra3gjNRpD", NamedTextColor.LIGHT_PURPLE)
                                .clickEvent(ClickEvent.openUrl("https://discord.gg/yra3gjNRpD")))
                );
                player.sendMessage(Component.empty());
            }
        });
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
            e.kickMessage(Component.text("§6§lYou are blacklisted\n\n§fYou can appeal your blacklist on our §ddiscord§f!\n\n\n§7MBA Store: §eComing Soon\n§7Discord: §ehttps://discord.gg/yra3gjNRpD\n"));
            return;
        }
        if (e.getResult().equals(PlayerLoginEvent.Result.KICK_FULL)) {
            if (e.getPlayer().hasPermission("rank.vip")) {
                e.allow();
                return;
            }
            e.kickMessage(Component.text("§6§lThe server is full (50/50)\n\n§fOur §a§lVIP §r§frank allows you to bypass this limit!\n\n\n§7MBA Store: §Coming Soon\n§7Discord: §ehttps://discord.gg/yra3gjNRpD\n"));
        }
    }
}

