/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.block.Block
 *  org.bukkit.block.BlockFace
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
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class BallHitBlockEvent
        extends Event
        implements Cancellable {
    private static final HandlerList HANDLERS_LIST = new HandlerList();
    @Getter
    private final Ball ball;
    @Getter
    private final BallType ballType;
    @Getter
    private final Block block;
    @Getter
    private final BlockFace blockFace;
    @Getter
    private final Location hitLocation;
    private boolean isCancelled;

    public BallHitBlockEvent(Ball ball, BallType ballType, Block block, BlockFace blockFace, Location hitLocation) {
        this.ball = ball;
        this.ballType = ballType;
        this.block = block;
        this.hitLocation = hitLocation;
        this.blockFace = blockFace;
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

