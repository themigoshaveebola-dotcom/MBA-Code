/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.TextComponent
 *  net.kyori.adventure.text.format.TextColor
 *  org.bukkit.Color
 *  org.bukkit.Location
 *  org.bukkit.entity.Player
 */
package me.x_tias.partix.plugin.ball.types;

import me.x_tias.partix.plugin.ball.Ball;
import me.x_tias.partix.plugin.ball.BallType;
import me.x_tias.partix.server.Place;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class Golfball
        extends Ball {
    public Golfball(Location location, Place place) {
        super(location, place, BallType.GOLFBALL, 0.4, 0.01, 0.01, 0.035, 0.19, 0.94, 0.05, 0.66, true, true, 3.0, Color.WHITE, Color.WHITE);
    }

    @Override
    public Component getControls(Player player) {
        String leftClick = "";
        String rightClick = "";
        String dropItem = "";
        String swapHand = "";
        Component lc = Component.text("[", TextColor.color(0x3D3D3D)).append(Component.keybind("key.attack", TextColor.color(5157655))).append(Component.text("]", TextColor.color(0x3D3D3D))).append(Component.text(" " + leftClick + "  ", TextColor.color(12896538)));
        Component rc = Component.text("[", TextColor.color(0x3D3D3D)).append(Component.keybind("key.use", TextColor.color(5157655))).append(Component.text("]", TextColor.color(0x3D3D3D))).append(Component.text(" " + rightClick + "  ", TextColor.color(12896538)));
        Component di = Component.text("[", TextColor.color(0x3D3D3D)).append(Component.keybind("key.drop", TextColor.color(5157655))).append(Component.text("]", TextColor.color(0x3D3D3D))).append(Component.text(" " + dropItem + "  ", TextColor.color(12896538)));
        Component sh = Component.text("[", TextColor.color(0x3D3D3D)).append(Component.keybind("key.swapOffhand", TextColor.color(5157655))).append(Component.text("]", TextColor.color(0x3D3D3D))).append(Component.text(" " + swapHand + "  ", TextColor.color(12896538)));
        return lc.append(rc).append(di).append(sh);
    }

    @Override
    public void modify() {
    }
}

