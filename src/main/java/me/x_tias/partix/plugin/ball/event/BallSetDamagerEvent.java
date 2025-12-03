package me.x_tias.partix.plugin.ball.event;

import lombok.Getter;
import lombok.Setter;
import me.x_tias.partix.plugin.ball.Ball;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
public class BallSetDamagerEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList HANDLERS_LIST = new HandlerList();
    private final Ball ball;
    private boolean cancelled;

    public BallSetDamagerEvent(Player player, Ball ball) {
        super(player);
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