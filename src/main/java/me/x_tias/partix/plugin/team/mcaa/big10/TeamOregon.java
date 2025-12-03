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

public final class TeamOregon
        extends BaseTeam {
    public TeamOregon() {
        int green = 20281;
        int yellow = 16703779;
        int black = 0;
        this.firstColor = TextColor.color(green);
        this.secondColor = TextColor.color(yellow);
        this.thirdColor = TextColor.color(black);
        this.abrv = Text.gradient("ORE", this.firstColor, this.secondColor, true);
        this.name = Text.gradient("Oregon\u00a0Ducks", this.firstColor, this.secondColor, true);
        this.chest = Items.armor(Material.LEATHER_CHESTPLATE, green, "Jersey", "§7Home jersey");
        this.away = Items.armor(Material.LEATHER_LEGGINGS, green, "Pants", "§7Away pants");
        this.pants = Items.armor(Material.LEATHER_LEGGINGS, yellow, "Pants", "§7Home pants");
        this.boots = Items.armor(Material.LEATHER_BOOTS, black, "Boots", "§7Team boots");
        this.block = Items.create(Material.LIME_CONCRETE, this.name, "§7Big\u202fTen");
    }
}

