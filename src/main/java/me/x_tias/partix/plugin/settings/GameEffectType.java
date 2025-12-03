/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.bukkit.potion.PotionEffect
 *  org.bukkit.potion.PotionEffectType
 */
package me.x_tias.partix.plugin.settings;

import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public enum GameEffectType {
    NONE(null),
    SPEED_1(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0, true, false)),
    SPEED_2(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, true, false)),
    JUMP_1(new PotionEffect(PotionEffectType.JUMP_BOOST, Integer.MAX_VALUE, 0, true, false)),
    JUMP_2(new PotionEffect(PotionEffectType.JUMP_BOOST, Integer.MAX_VALUE, 1, true, false));

    public final PotionEffect effect;

    GameEffectType(PotionEffect effect) {
        this.effect = effect;
    }
}

