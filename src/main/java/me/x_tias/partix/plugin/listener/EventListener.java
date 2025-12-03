/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.bukkit.GameMode
 *  org.bukkit.Location
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.Action
 *  org.bukkit.event.player.PlayerDropItemEvent
 *  org.bukkit.event.player.PlayerInteractEvent
 *  org.bukkit.event.player.PlayerSwapHandItemsEvent
 *  org.bukkit.inventory.EquipmentSlot
 *  org.bukkit.util.Vector
 */
package me.x_tias.partix.plugin.listener;

import me.x_tias.partix.Partix;
import me.x_tias.partix.plugin.athlete.AthleteManager;
import me.x_tias.partix.plugin.ball.Ball;
import me.x_tias.partix.plugin.ball.BallFactory;
import me.x_tias.partix.plugin.ball.event.*;
import me.x_tias.partix.plugin.cooldown.Cooldown;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.Vector;

import java.util.Objects;
import java.util.Optional;

public class EventListener
        implements Listener {
    private boolean cantExecute(Player player) {
        return !player.getGameMode().equals(GameMode.ADVENTURE) || AthleteManager.get(player.getUniqueId()).isSpectating();
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        boolean isRestricted = Cooldown.isRestricted(player);
        if (!isRestricted) {
            Cooldown.setRestricted(player.getUniqueId(), 5);
        }
        if (Objects.equals(e.getHand(), EquipmentSlot.HAND)) {
            if (isRestricted) {
                return;
            }
            if (e.getAction().equals(Action.RIGHT_CLICK_BLOCK) || e.getAction().equals(Action.RIGHT_CLICK_AIR)) {
                if (!player.getGameMode().equals(GameMode.ADVENTURE)) {
                    return;
                }
                PressRightClickEvent event = new PressRightClickEvent(player, player.getInventory().getItemInMainHand());
                Partix.getInstance().getServer().getPluginManager().callEvent(event);
            } else if (e.getAction().equals(Action.LEFT_CLICK_BLOCK) || e.getAction().equals(Action.LEFT_CLICK_AIR)) {
                if (this.cantExecute(player)) {
                    return;
                }
                Location start = player.getEyeLocation();
                Vector direction = player.getLocation().getDirection();
                for (double distance = 0.0; distance < 4.0; distance += 0.1) {
                    Ball ball;
                    Player hit;
                    Location detect = start.clone().add(direction.clone().multiply(distance));
                    Optional<Player> playerDetection = detect.getNearbyPlayers(1.5).stream().findFirst();
                    Optional<Ball> ballDetection = BallFactory.getNearest(detect, 1.5);
                    double pDistance = 10.0;
                    double bDistance;
                    if (playerDetection.isPresent() && (hit = playerDetection.get()).getLocation().distance(detect) < 1.0) {
                        pDistance = hit.getLocation().distance(detect);
                    }
                    if (ballDetection.isPresent() && (ball = ballDetection.get()).getLocation().distance(detect) < 0.1 + ball.getHitboxSize()) {
                        boolean b;
                        bDistance = ball.getLocation().distance(detect);
                        if (ball.getCurrentDamager() == null) {
                            b = true;
                        } else {
                            boolean bl = b = ball.getCurrentDamager() != player;
                        }
                        if (!b || !(bDistance < pDistance)) break;
                        Location pl = player.getLocation();
                        pl.setY(ball.getLocation().getY());
                        if (pl.distance(ball.getLocation()) < ball.getStealBallDistance()) {
                            Partix.getInstance().getServer().getPluginManager().callEvent(new PlayerHitBallEvent(player, ball, detect.clone()));
                        }
                        return;
                    }
                    if (pDistance < 3.0) break;
                }
                if (e.getAction().isLeftClick()) {
                    Partix.getInstance().getServer().getPluginManager().callEvent(new PressLeftClickEvent(player, player.getInventory().getItemInMainHand()));
                }
            }
        }
    }

    @EventHandler
    public void onDropItem(PlayerDropItemEvent e) {
        Player player = e.getPlayer();
        if (Cooldown.isRestricted(player)) {
            e.setCancelled(true);
            return;
        }
        if (this.cantExecute(player)) {
            return;
        }
        PressDropKeyEvent event = new PressDropKeyEvent(player, e.getItemDrop().getItemStack());
        Partix.getInstance().getServer().getPluginManager().callEvent(event);
        if (event.isItemKept()) {
            e.setCancelled(true);
        } else {
            e.setCancelled(false);
            e.getItemDrop().remove();
        }
    }

    @EventHandler
    public void onSwapItem(PlayerSwapHandItemsEvent e) {
        Player player = e.getPlayer();
        if (Cooldown.isRestricted(player)) {
            e.setCancelled(true);
            return;
        }
        if (this.cantExecute(player)) {
            return;
        }
        Partix.getInstance().getServer().getPluginManager().callEvent(new PressSwapKeyEvent(player, e.getMainHandItem()));
        e.setCancelled(true);
    }
}

