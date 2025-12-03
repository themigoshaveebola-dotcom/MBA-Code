/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerMoveEvent
 */
package me.x_tias.partix.plugin.listener;

import me.x_tias.partix.mini.game.GoalGame;
import me.x_tias.partix.plugin.athlete.Athlete;
import me.x_tias.partix.plugin.athlete.AthleteManager;
import me.x_tias.partix.server.Place;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.UUID;

public class BroadcastMovementBlocker
        implements Listener {
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent ev) {
        Player p = ev.getPlayer();
        UUID id = p.getUniqueId();
        Athlete ath = AthleteManager.get(id);
        if (ath == null) {
            return;
        }
        Place place = ath.getPlace();
        if (!(place instanceof GoalGame current)) {
            return;
        }
        if (current.broadcasters.contains(id)) {
            ev.setTo(ev.getFrom());
        }
    }
}

