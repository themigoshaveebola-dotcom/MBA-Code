/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.bukkit.event.HandlerList
 *  org.jetbrains.annotations.NotNull
 */
package me.x_tias.partix.plugin.ball.event;

import lombok.Getter;
import me.x_tias.partix.plugin.ball.Ball;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
public class PlayerHitBallEvent
        extends Event {
    private static final HandlerList HANDLERS_LIST = new HandlerList();
    private final Player player;
    private final Ball ball;
    private final Location hitLocation;

    public PlayerHitBallEvent(Player player, Ball ball, Location hitLocation) {
        this.ball = ball;
        this.hitLocation = hitLocation;
        this.player = player;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS_LIST;
    }

    @NotNull
    public HandlerList getHandlers() {
        return HANDLERS_LIST;
    }

}

