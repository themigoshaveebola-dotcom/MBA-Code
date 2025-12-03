/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.bukkit.FluidCollisionMode
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.bukkit.event.HandlerList
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.util.RayTraceResult
 *  org.jetbrains.annotations.NotNull
 */
package me.x_tias.partix.plugin.ball.event;

import lombok.Getter;
import me.x_tias.partix.plugin.athlete.Athlete;
import me.x_tias.partix.plugin.athlete.AthleteManager;
import org.bukkit.FluidCollisionMode;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.jetbrains.annotations.NotNull;

public class PressRightClickEvent
        extends Event {
    private static final HandlerList HANDLERS_LIST = new HandlerList();
    @Getter
    private final Player player;
    private final ItemStack item;
    @Getter
    private final boolean thrownInBlock;

    public PressRightClickEvent(Player player, ItemStack item) {
        boolean isInBlock;
        this.player = player;
        this.item = item;
        try {
            RayTraceResult result = player.getWorld().rayTraceBlocks(player.getEyeLocation(), player.getLocation().getDirection(), 0.5, FluidCollisionMode.NEVER, false);
            isInBlock = result != null && result.getHitBlock() != null;
        } catch (Exception ex) {
            isInBlock = false;
        }
        this.thrownInBlock = isInBlock;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS_LIST;
    }

    @NotNull
    public HandlerList getHandlers() {
        return HANDLERS_LIST;
    }

    public ItemStack getItemStack() {
        return this.item;
    }

    public Athlete getAthlete() {
        return AthleteManager.get(this.player.getUniqueId());
    }
}

