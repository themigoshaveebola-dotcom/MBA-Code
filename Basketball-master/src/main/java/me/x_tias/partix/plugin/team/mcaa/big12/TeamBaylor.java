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

public final class TeamBaylor
        extends BaseTeam {
    public TeamBaylor() {
        int green = 1394484;
        int gold = 16758812;
        int grey = 10857647;
        this.firstColor = TextColor.color(green);
        this.secondColor = TextColor.color(gold);
        this.thirdColor = TextColor.color(grey);
        this.abrv = Text.gradient("BU", this.firstColor, this.secondColor, true);
        this.name = Text.gradient("Baylor\u202fBears", this.firstColor, this.secondColor, true);
        this.chest = Items.armor(Material.LEATHER_CHESTPLATE, green, "Jersey", "§7Home jersey");
        this.away = Items.armor(Material.LEATHER_LEGGINGS, green, "Pants", "§7Away pants");
        this.pants = Items.armor(Material.LEATHER_LEGGINGS, green, "Pants", "§7Home pants");
        this.boots = Items.armor(Material.LEATHER_BOOTS, gold, "Boots", "§7Team boots");
        this.block = Items.create(Material.GREEN_GLAZED_TERRACOTTA, this.name, "§7BIG‑12");
    }
}

