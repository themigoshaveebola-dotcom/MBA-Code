package me.x_tias.partix.plugin.rightclick.events;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

@Getter
public class PlayerRightClickStopHoldEvent extends PlayerEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    public PlayerRightClickStopHoldEvent(@NotNull Player who) {
        super(who);
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }
}
