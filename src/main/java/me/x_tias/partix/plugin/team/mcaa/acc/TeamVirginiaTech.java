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

public final class TeamVirginiaTech
        extends BaseTeam {
    public TeamVirginiaTech() {
        int maroon = 6488113;
        int orange = 13583392;
        int white = 0xFFFFFF;
        this.firstColor = TextColor.color(maroon);
        this.secondColor = TextColor.color(orange);
        this.thirdColor = TextColor.color(white);
        this.abrv = Text.gradient("VT", this.firstColor, this.secondColor, true);
        this.name = Text.gradient("Virginia\u00a0Tech\u00a0Hokies", this.firstColor, this.secondColor, true);
        this.chest = Items.armor(Material.LEATHER_CHESTPLATE, maroon, "Jersey", "§7Home jersey");
        this.away = Items.armor(Material.LEATHER_LEGGINGS, maroon, "Pants", "§7Away pants");
        this.pants = Items.armor(Material.LEATHER_LEGGINGS, orange, "Pants", "§7Home pants");
        this.boots = Items.armor(Material.LEATHER_BOOTS, white, "Boots", "§7Team boots");
        this.block = Items.create(Material.MAGENTA_DYE, this.name, "§7ACC");
    }
}

