/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  io.papermc.paper.event.player.AsyncChatEvent
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.TextComponent
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 */
package me.x_tias.partix.plugin.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.x_tias.partix.Partix;
import me.x_tias.partix.plugin.athlete.Athlete;
import me.x_tias.partix.plugin.athlete.AthleteManager;
import me.x_tias.partix.util.Message;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ChatListener
        implements Listener {
    @EventHandler
    public void onSendMessage(AsyncChatEvent e) {
        Player player = e.getPlayer();
        Athlete athlete = AthleteManager.get(player.getUniqueId());
        String message = ((TextComponent) e.message()).content();
        if (message.startsWith("#")) {
            Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(Message.shoutMessage(athlete, Component.text(message.replaceFirst("#", "").trim()))));
            Partix.getInstance().getLogger().info("[Shout] " + e.getPlayer().getName() + " > " + ((TextComponent) e.message()).content());
        } else {
            athlete.getPlace().sendMessage(Message.sendMessage(athlete, e.message()));
            Partix.getInstance().getLogger().info("[Chat] " + e.getPlayer().getName() + " > " + ((TextComponent) e.message()).content());
        }
        e.setCancelled(true);
    }
}

