/*
 * Decompiled with CFR 0.152.
 */
package me.x_tias.partix.server.specific;

import me.x_tias.partix.mini.factories.Hub;
import me.x_tias.partix.plugin.athlete.Athlete;
import me.x_tias.partix.server.Place;

import java.util.ArrayList;
import java.util.UUID;

public abstract class Game
        extends Place {
    public UUID owner;

    public void kickAll() {
        for (Athlete athlete : new ArrayList<>(this.getAthletes())) {
            Hub.hub.join(athlete);
        }
    }
}

