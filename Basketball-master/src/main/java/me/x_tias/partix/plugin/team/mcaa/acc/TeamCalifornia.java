/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  net.kyori.adventure.text.format.TextColor
 *  org.bukkit.Material
 */
package me.x_tias.partix.plugin.team.mcaa.acc;

import me.x_tias.partix.plugin.team.BaseTeam;
import me.x_tias.partix.util.Items;
import me.x_tias.partix.util.Text;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;

public final class TeamCalifornia
        extends BaseTeam {
    public TeamCalifornia() {
        int blue = 12898;
        int gold = 16626965;
        int white = 0xFFFFFF;
        this.firstColor = TextColor.color(blue);
        this.secondColor = TextColor.color(gold);
        this.thirdColor = TextColor.color(white);
        this.abrv = Text.gradient("CAL", this.firstColor, this.secondColor, true);
        this.name = Text.gradient("California\u00a0Golden\u00a0Bears", this.firstColor, this.secondColor, true);
        this.chest = Items.armor(Material.LEATHER_CHESTPLATE, blue, "Jersey", "§7Home jersey");
        this.away = Items.armor(Material.LEATHER_LEGGINGS, blue, "Pants", "§7Away pants");
        this.pants = Items.armor(Material.LEATHER_LEGGINGS, gold, "Pants", "§7Home pants");
        this.boots = Items.armor(Material.LEATHER_BOOTS, white, "Boots", "§7Team boots");
        this.block = Items.create(Material.BLUE_CONCRETE, this.name, "§7ACC");
    }
}

