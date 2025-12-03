/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.KeybindComponent
 *  net.kyori.adventure.text.format.TextColor
 *  org.bukkit.Color
 *  org.bukkit.Location
 *  org.bukkit.entity.Player
 */
package me.x_tias.partix.plugin.ball.types;

import me.x_tias.partix.plugin.ball.Ball;
import me.x_tias.partix.plugin.ball.BallType;
import me.x_tias.partix.server.Place;
import me.x_tias.partix.util.Colour;
import me.x_tias.partix.util.Position;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class Puck
        extends Ball {
    int handYaw = 30;
    int handModifier = 0;
    int delay = 5;

    public Puck(Location location, Place place) {
        super(location, place, BallType.PUCK, 0.4, 0.1, 0.05, 0.03, 0.19, 0.74, 0.05, 0.53, true, true, 1.5, Color.BLACK, Color.BLACK);
    }

    @Override
    public Component getControls(Player player) {
        String leftClick = "Pass";
        String rightClick = "Shoot";
        String dropItem = "Side Step";
        String swapHand = "Swap Hand";
        Component lc = Component.text("[", Colour.border()).append(Component.keybind("key.attack", Colour.title()).append(Component.text("]", Colour.border())).append(Component.text(" " + leftClick + ", ", Colour.text())));
        Component rc = Component.text("[", Colour.border()).append(Component.keybind("key.use", Colour.title()).append(Component.text("]", Colour.border())).append(Component.text(" " + rightClick + ", ", Colour.text())));
        Component di = Component.text("[", Colour.border()).append(Component.keybind("key.drop", Colour.title()).append(Component.text("]", Colour.border())).append(Component.text(" " + dropItem + ", ", Colour.text())));
        Component sh = Component.text("[", Colour.border()).append(Component.keybind("key.swapOffhand", Colour.title()).append(Component.text("]", Colour.border())).append(Component.text(" " + swapHand + ", ", Colour.text())));
        return lc.append(rc).append(di).append(sh);
    }

    public void takePuck(Player player) {
        if (this.delay < 1) {
            if (this.getCurrentDamager() == null) {
                this.setDamager(player);
                this.delay = 20;
                return;
            }
            if (this.getCurrentDamager() != player) {
                this.setDamager(player);
                this.delay = 20;
            }
        }
    }

    public void pass(Player player) {
        if (this.delay < 1 && this.getCurrentDamager() != null && player.equals(this.getCurrentDamager())) {
            this.setHorizontal(player, Position.stabilize(player, 0.0).getDirection().multiply(1.0));
            this.delay = 5;
            this.giveaway();
        }
    }

    public void shoot(Player player) {
        if (this.delay < 1 && this.getCurrentDamager() != null && player.equals(this.getCurrentDamager())) {
            this.setHorizontal(player, player.getLocation().getDirection().multiply(2.0));
            this.delay = 5;
            this.giveaway();
        }
    }

    public void leftClick(Player player) {
        if (this.getCurrentDamager() != null) {
            this.pass(player);
        } else if (this.delay < 1) {
            this.takePuck(player);
        }
    }

    public boolean changeHand(Player player) {
        if (this.getCurrentDamager() != null && this.getCurrentDamager().equals(player)) {
            if (this.handModifier == 0) {
                this.handModifier = 8;
            } else if (this.handModifier == 8) {
                this.handModifier = -8;
            } else if (this.handModifier == -8) {
                this.handModifier = 8;
            }
            return true;
        }
        return false;
    }

    public boolean sideStep(Player player) {
        if (this.getCurrentDamager() != null && this.getCurrentDamager().equals(player)) {
            this.handModifier = this.handModifier == 0 ? 5 : (this.handModifier > 0 ? -5 : 5);
            this.setVertical(0.225);
            return true;
        }
        return false;
    }

    public void giveaway() {
        this.removeCurrentDamager();
    }

    private void modifyHand() {
        int nextHand = this.handYaw + this.handModifier;
        if (nextHand < 40 && nextHand > -40) {
            this.handYaw = nextHand;
        }
    }

    public boolean collides(Player player) {
        if (this.delay < 1 && this.getLastDamager() != null) {
            player.sendMessage("collision");
            return this.getLastDamager() == player;
        }
        return false;
    }

    @Override
    public void modify() {
        if (this.getCurrentDamager() != null) {
            Player poss = this.getCurrentDamager();
            this.modifyHand();
            this.setHorizontal(Position.stabilize(poss, this.handYaw, 1.25));
        }
        if (this.delay > 0) {
            --this.delay;
        }
    }
}

