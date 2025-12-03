/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 */
package me.x_tias.partix.server.specific;

import me.x_tias.partix.plugin.athlete.Athlete;
import me.x_tias.partix.server.Place;
import org.bukkit.entity.Player;

public abstract class Lobby
        extends Place {
    @Override
    public void join(Athlete... athletes) {
        super.join(athletes);
        for (Athlete athlete : athletes) {
            this.giveItems(athlete.getPlayer());
        }
    }

    public abstract void giveItems(Player var1);
}

