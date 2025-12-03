/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  net.kyori.adventure.text.format.TextColor
 *  org.bukkit.Material
 */
package me.x_tias.partix.plugin.team.mcaa.big10;

import me.x_tias.partix.plugin.team.BaseTeam;
import me.x_tias.partix.util.Items;
import me.x_tias.partix.util.Text;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;

public final class TeamWisconsin
        extends BaseTeam {
    public TeamWisconsin() {
        int cardinal = 0xC5050C;
        int white = 0xFFFFFF;
        int black = 0;
        this.firstColor = TextColor.color(cardinal);
        this.secondColor = TextColor.color(white);
        this.thirdColor = TextColor.color(black);
        this.abrv = Text.gradient("WIS", this.firstColor, this.secondColor, true);
        this.name = Text.gradient("Wisconsin\u00a0Badgers", this.firstColor, this.secondColor, true);
        this.chest = Items.armor(Material.LEATHER_CHESTPLATE, cardinal, "Jersey", "§7Home jersey");
        this.away = Items.armor(Material.LEATHER_LEGGINGS, cardinal, "Pants", "§7Away pants");
        this.pants = Items.armor(Material.LEATHER_LEGGINGS, white, "Pants", "§7Home pants");
        this.boots = Items.armor(Material.LEATHER_BOOTS, black, "Boots", "§7Team boots");
        this.block = Items.create(Material.RED_TERRACOTTA, this.name, "§7Big\u202fTen");
    }
}

