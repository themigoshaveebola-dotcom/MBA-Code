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

public final class TeamKansas
        extends BaseTeam {
    public TeamKansas() {
        int blue = 20922;
        int red = 15204365;
        int yellow = 16762925;
        this.firstColor = TextColor.color(blue);
        this.secondColor = TextColor.color(red);
        this.thirdColor = TextColor.color(yellow);
        this.abrv = Text.gradient("KU", this.firstColor, this.secondColor, true);
        this.name = Text.gradient("Kansas\u202fJayhawks", this.firstColor, this.secondColor, true);
        this.chest = Items.armor(Material.LEATHER_CHESTPLATE, blue, "Jersey", "§7Home jersey");
        this.away = Items.armor(Material.LEATHER_LEGGINGS, blue, "Pants", "§7Away pants");
        this.pants = Items.armor(Material.LEATHER_LEGGINGS, blue, "Pants", "§7Home pants");
        this.boots = Items.armor(Material.LEATHER_BOOTS, yellow, "Boots", "§7Team boots");
        this.block = Items.create(Material.BLUE_GLAZED_TERRACOTTA, this.name, "§7BIG‑12");
    }
}

