/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  net.kyori.adventure.text.format.TextColor
 *  org.bukkit.Material
 */
package me.x_tias.partix.plugin.team.mcaa.big12;

import me.x_tias.partix.plugin.team.BaseTeam;
import me.x_tias.partix.util.Items;
import me.x_tias.partix.util.Text;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;

public final class TeamColorado
        extends BaseTeam {
    public TeamColorado() {
        int black = 0;
        int gold = 13613180;
        int silver = 10857647;
        this.firstColor = TextColor.color(black);
        this.secondColor = TextColor.color(gold);
        this.thirdColor = TextColor.color(silver);
        this.abrv = Text.gradient("CU", this.firstColor, this.secondColor, true);
        this.name = Text.gradient("Colorado\u202fBuffaloes", this.firstColor, this.secondColor, true);
        this.chest = Items.armor(Material.LEATHER_CHESTPLATE, black, "Jersey", "§7Home jersey");
        this.away = Items.armor(Material.LEATHER_LEGGINGS, black, "Pants", "§7Away pants");
        this.pants = Items.armor(Material.LEATHER_LEGGINGS, black, "Pants", "§7Home pants");
        this.boots = Items.armor(Material.LEATHER_BOOTS, gold, "Boots", "§7Team boots");
        this.block = Items.create(Material.BLACK_GLAZED_TERRACOTTA, this.name, "§7BIG‑12");
    }
}

