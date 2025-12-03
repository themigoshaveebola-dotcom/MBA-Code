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

public final class TeamArizona
        extends BaseTeam {
    public TeamArizona() {
        int red = 12852794;
        int navy = 795456;
        int grey = 8751498;
        this.firstColor = TextColor.color(red);
        this.secondColor = TextColor.color(navy);
        this.thirdColor = TextColor.color(grey);
        this.abrv = Text.gradient("AZ", this.firstColor, this.secondColor, true);
        this.name = Text.gradient("Arizona\u202fWildcats", this.firstColor, this.secondColor, true);
        this.chest = Items.armor(Material.LEATHER_CHESTPLATE, red, "Jersey", "§7Home jersey");
        this.away = Items.armor(Material.LEATHER_LEGGINGS, red, "Pants", "§7Away pants");
        this.pants = Items.armor(Material.LEATHER_LEGGINGS, red, "Pants", "§7Home pants");
        this.boots = Items.armor(Material.LEATHER_BOOTS, grey, "Boots", "§7Team boots");
        this.block = Items.create(Material.BLUE_GLAZED_TERRACOTTA, this.name, "§7BIG‑12");
    }
}

