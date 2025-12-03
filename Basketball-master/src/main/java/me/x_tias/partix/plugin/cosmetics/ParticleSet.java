/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.bukkit.Color
 *  org.bukkit.Particle
 *  org.bukkit.Particle$DustOptions
 */
package me.x_tias.partix.plugin.cosmetics;

import lombok.Getter;
import org.bukkit.Color;
import org.bukkit.Particle;

@Getter
public class ParticleSet {
    private final Particle particle;
    private final Particle.DustOptions dustOptions;

    public ParticleSet(Particle particle, Color color) {
        this.particle = particle;
        this.dustOptions = new Particle.DustOptions(color, 1.0f);
    }

    public ParticleSet(Particle particle) {
        this.particle = particle;
        this.dustOptions = null;
    }

    public static ParticleSet of(Particle particle, Color color) {
        return new ParticleSet(particle, color);
    }

    public static ParticleSet of(Particle particle) {
        return new ParticleSet(particle);
    }

    public static ParticleSet empty() {
        return new ParticleSet(null);
    }
}

