/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  net.kyori.adventure.text.format.TextColor
 *  org.bukkit.Material
 */
package me.x_tias.partix.plugin.team.mba;

import me.x_tias.partix.plugin.team.BaseTeam;
import me.x_tias.partix.util.Items;
import me.x_tias.partix.util.Text;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;

public final class TeamPatriots
        extends BaseTeam {
    public TeamPatriots() {
        int red = 13111342;
        int white = 0xFFFFFF;
        int blue = 11618;
        this.firstColor = TextColor.color(red);
        this.secondColor = TextColor.color(white);
        this.thirdColor = TextColor.color(blue);
        this.abrv = Text.gradient("BOS", this.firstColor, this.thirdColor, true);
        this.name = Text.gradient("Boston\u00a0Patriots", this.firstColor, this.thirdColor, true);
        this.chest = Items.armor(Material.LEATHER_CHESTPLATE, red, "Jersey", "§7Home jersey");
        this.away = Items.armor(Material.LEATHER_LEGGINGS, red, "Pants", "§7Away pants");
        this.pants = Items.armor(Material.LEATHER_LEGGINGS, blue, "Pants", "§7Home pants");
        this.boots = Items.armor(Material.LEATHER_BOOTS, white, "Boots", "§7Team boots");
        this.block = Items.create(Material.BLUE_GLAZED_TERRACOTTA, this.name, "§7West");
    }
}

