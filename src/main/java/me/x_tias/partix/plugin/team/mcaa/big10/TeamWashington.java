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

public final class TeamWashington
        extends BaseTeam {
    public TeamWashington() {
        int purple = 4927107;
        int gold = 15258260;
        int black = 0;
        this.firstColor = TextColor.color(purple);
        this.secondColor = TextColor.color(gold);
        this.thirdColor = TextColor.color(black);
        this.abrv = Text.gradient("UW", this.firstColor, this.secondColor, true);
        this.name = Text.gradient("Washington\u00a0Huskies", this.firstColor, this.secondColor, true);
        this.chest = Items.armor(Material.LEATHER_CHESTPLATE, purple, "Jersey", "§7Home jersey");
        this.away = Items.armor(Material.LEATHER_LEGGINGS, purple, "Pants", "§7Away pants");
        this.pants = Items.armor(Material.LEATHER_LEGGINGS, gold, "Pants", "§7Home pants");
        this.boots = Items.armor(Material.LEATHER_BOOTS, black, "Boots", "§7Team boots");
        this.block = Items.create(Material.PURPLE_CONCRETE_POWDER, this.name, "§7Big\u202fTen");
    }
}

