/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.entity.Entity
 *  org.bukkit.event.Cancellable
 *  org.bukkit.event.Event
 *  org.bukkit.event.HandlerList
 *  org.jetbrains.annotations.NotNull
 */
package me.x_tias.partix.plugin.ball.event;

import lombok.Getter;
import me.x_tias.partix.plugin.ball.Ball;
import me.x_tias.partix.plugin.ball.BallType;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class BallHitEntityEvent
        extends Event
        implements Cancellable {
    private static final HandlerList HANDLERS_LIST = new HandlerList();
    @Getter
    private final Ball ball;
    @Getter
    private final BallType ballType;
    @Getter
    private final Entity entity;
    @Getter
    private final Location hitLocation;
    private boolean isCancelled;

    public BallHitEntityEvent(Ball ball, BallType ballType, Entity entity, Location hitLocation) {
        this.ball = ball;
        this.ballType = ballType;
        this.entity = entity;
        this.hitLocation = hitLocation;
        this.isCancelled = false;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS_LIST;
    }

    public boolean isCancelled() {
        return this.isCancelled;
    }

    public void setCancelled(boolean b) {
        this.isCancelled = b;
    }

    @NotNull
    public HandlerList getHandlers() {
        return HANDLERS_LIST;
    }

}

