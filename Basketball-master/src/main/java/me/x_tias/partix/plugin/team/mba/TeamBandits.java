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

public final class TeamBandits
        extends BaseTeam {
    public TeamBandits() {
        int black = 131586;
        int red = 14877957;
        int steel = 0x6E6E6E;
        this.firstColor = TextColor.color(black);
        this.secondColor = TextColor.color(red);
        this.thirdColor = TextColor.color(steel);
        this.abrv = Text.gradient("CLB", this.secondColor, this.firstColor, true);
        this.name = Text.gradient("Cleveland\u00a0Bandits", this.secondColor, this.firstColor, true);
        this.chest = Items.armor(Material.LEATHER_CHESTPLATE, red, "Jersey", "§7Home jersey");
        this.away = Items.armor(Material.LEATHER_LEGGINGS, red, "Pants", "§7Away pants");
        this.pants = Items.armor(Material.LEATHER_LEGGINGS, black, "Pants", "§7Home pants");
        this.boots = Items.armor(Material.LEATHER_BOOTS, steel, "Boots", "§7Team boots");
        this.block = Items.create(Material.BLACK_GLAZED_TERRACOTTA, this.name, "§7East");
    }
}

