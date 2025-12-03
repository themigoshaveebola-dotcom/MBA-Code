/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitRunnable
 */
package me.x_tias.partix.server;

import me.x_tias.partix.Partix;
import me.x_tias.partix.server.specific.Lobby;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class PlaceLoader {
    public static List<Place> places = new ArrayList<>();

    public static void setup() {
        new BukkitRunnable() {

            public void run() {
                places.forEach(Place::onTick);
            }
        }.runTaskTimer(Partix.getInstance(), 1L, 1L);
    }

    public static void create(Place place) {
        Lobby lobby;
        if (place instanceof Lobby && !places.contains(lobby = (Lobby) place)) {
            places.add(lobby);
        }
    }
}

