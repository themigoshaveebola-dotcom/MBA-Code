/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.Material
 */
package me.x_tias.partix.mini.basketball;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Material;

@Getter
public class Stadium {
    private final String name;
    private final Material block;
    private final Location location;
    private final Category category;
    @Setter
    private BasketballGame game;

    public Stadium(String name, Material block, Location loc, Category cat) {
        this.name = name;
        this.block = block;
        this.location = loc;
        this.category = cat;
    }

    public boolean hasActiveGame() {
        return this.game != null;
    }

    public enum Category {
        MBA,
        MCAA,
        RETRO

    }
}

