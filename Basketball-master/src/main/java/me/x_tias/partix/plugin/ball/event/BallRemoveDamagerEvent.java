package me.x_tias.partix.plugin.ball.event;

import lombok.Getter;
import me.x_tias.partix.plugin.ball.Ball;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
public class BallRemoveDamagerEvent extends Event {
    private static final HandlerList HANDLERS_LIST = new HandlerList();
    private final @Nullable Player player;
    private final Ball ball;

    public BallRemoveDamagerEvent(@Nullable Player player, Ball ball) {
        this.player = player;
        this.ball = ball;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS_LIST;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS_LIST;
    }
}