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

public final class TeamNorthernArizona
        extends BaseTeam {
    public TeamNorthernArizona() {
        int blue = 13158;
        int gold = 0xFFCC00;
        int green = 1988145;
        this.firstColor = TextColor.color(blue);
        this.secondColor = TextColor.color(gold);
        this.thirdColor = TextColor.color(green);
        this.abrv = Text.gradient("NAU", this.secondColor, this.firstColor, true);
        this.name = Text.gradient("Northern\u00a0Arizona\u00a0Lumberjacks", this.secondColor, this.firstColor, true);
        this.chest = Items.armor(Material.LEATHER_CHESTPLATE, blue, "Jersey", "§7Home jersey");
        this.away = Items.armor(Material.LEATHER_LEGGINGS, blue, "Pants", "§7Away pants");
        this.pants = Items.armor(Material.LEATHER_LEGGINGS, gold, "Pants", "§7Home pants");
        this.boots = Items.armor(Material.LEATHER_BOOTS, gold, "Boots", "§7Team boots");
        this.block = Items.create(Material.BLUE_GLAZED_TERRACOTTA, this.name, "§7Big\u202fSky");
    }
}

