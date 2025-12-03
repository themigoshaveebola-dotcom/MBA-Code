/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  net.kyori.adventure.text.format.TextColor
 *  org.bukkit.Material
 */
package me.x_tias.partix.plugin.team.mcaa.bigsky;

import me.x_tias.partix.plugin.team.BaseTeam;
import me.x_tias.partix.util.Items;
import me.x_tias.partix.util.Text;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;

public final class TeamNorthernColorado
        extends BaseTeam {
    public TeamNorthernColorado() {
        int navy = 10325;
        int gold = 16758812;
        int white = 0xFFFFFF;
        this.firstColor = TextColor.color(navy);
        this.secondColor = TextColor.color(gold);
        this.thirdColor = TextColor.color(white);
        this.abrv = Text.gradient("UNC", this.secondColor, this.firstColor, true);
        this.name = Text.gradient("Northern\u00a0Colorado\u00a0Bears", this.secondColor, this.firstColor, true);
        this.chest = Items.armor(Material.LEATHER_CHESTPLATE, navy, "Jersey", "§7Home jersey");
        this.away = Items.armor(Material.LEATHER_LEGGINGS, navy, "Pants", "§7Away pants");
        this.pants = Items.armor(Material.LEATHER_LEGGINGS, gold, "Pants", "§7Home pants");
        this.boots = Items.armor(Material.LEATHER_BOOTS, gold, "Boots", "§7Team boots");
        this.block = Items.create(Material.BLUE_GLAZED_TERRACOTTA, this.name, "§7Big\u202fSky");
    }
}

