/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitRunnable
 *  org.bukkit.util.BoundingBox
 */
package me.x_tias.partix.plugin.ball;

import me.x_tias.partix.Partix;
import me.x_tias.partix.mini.basketball.BasketballGame;
import me.x_tias.partix.plugin.ball.types.Basketball;
import me.x_tias.partix.plugin.ball.types.Golfball;
import me.x_tias.partix.plugin.ball.types.Puck;
import me.x_tias.partix.server.Place;
import me.x_tias.partix.util.Colour;
import me.x_tias.partix.util.Items;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BallFactory {
    private static final List<Ball> ballList = new ArrayList<>();

    public static void setup() {
        new BukkitRunnable() {

            public void run() {
                new ArrayList<>(ballList).forEach(Ball::move);
            }
        }.runTaskTimer(Partix.getInstance(), 1L, 1L);
    }

    public static Ball create(Location location, BallType ballType, Place place) {
        switch (ballType) {
            case PUCK: {
                Puck ball = new Puck(location, place);
                ballList.add(ball);
                return ball;
            }
            case BASKETBALL: {
                if (place instanceof BasketballGame game) {
                    Basketball ball = new Basketball(location, game);
                    ballList.add(ball);
                    return ball;
                }
                return null;
            }
            case GOLFBALL: {
                Golfball ball = new Golfball(location, place);
                ballList.add(ball);
                return ball;
            }
        }
        Puck puck = new Puck(location, place);
        ballList.add(puck);
        return puck;
    }

    public static boolean hasBall(Player player) {
        for (Ball ball : BallFactory.getNearby(player.getLocation(), 3.0)) {
            if (ball.getCurrentDamager() == null || !ball.getCurrentDamager().equals(player)) continue;
            player.getInventory().setItem(0, Items.get(Component.text("Minecraft Basketball").color(Colour.partix()), Material.POLISHED_BLACKSTONE_BUTTON));
            return true;
        }
        return false;
    }

    public static void remove(Ball ball) {
        ballList.remove(ball);
        ball.removeCurrentDamager();
    }

    public static void removeBalls(Location location, double radius) {
        List<Ball> inRange = ballList.stream().filter(ball -> ball.getLocation().distance(location) < radius - ball.getDimensions().getX() / 2.0).toList();
        if (!inRange.isEmpty()) {
            inRange.forEach(Ball::remove);
        }
    }

    public static void removeBalls(BoundingBox box) {
        List<Ball> inBox = ballList.stream().filter(ball -> box.contains(ball.getLocation().toVector())).toList();
        if (!inBox.isEmpty()) {
            inBox.forEach(Ball::remove);
        }
    }

    public static List<Ball> getNearby(Location location, double radius) {
        return ballList.stream().filter(ball -> ball.getLocation().distance(location) < radius - ball.getDimensions().getX() / 2.0).toList();
    }

    public static Optional<Ball> getNearest(Location location, double max) {
        List<Ball> near = BallFactory.getNearby(location, max);
        double distance = max + 0.5;
        Ball closest = null;
        for (Ball b : near) {
            double dist = b.getLocation().distance(location);
            if (!(dist < distance)) continue;
            closest = b;
            distance = dist;
        }
        return Optional.ofNullable(closest);
    }
}

