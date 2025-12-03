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

public final class TeamSMU
        extends BaseTeam {
    public TeamSMU() {
        int red = 14297372;
        int blue = 22455;
        int white = 0xFFFFFF;
        this.firstColor = TextColor.color(red);
        this.secondColor = TextColor.color(blue);
        this.thirdColor = TextColor.color(white);
        this.abrv = Text.gradient("SMU", this.firstColor, this.secondColor, true);
        this.name = Text.gradient("SMU\u00a0Mustangs", this.firstColor, this.secondColor, true);
        this.chest = Items.armor(Material.LEATHER_CHESTPLATE, red, "Jersey", "§7Home jersey");
        this.away = Items.armor(Material.LEATHER_LEGGINGS, red, "Pants", "§7Away pants");
        this.pants = Items.armor(Material.LEATHER_LEGGINGS, blue, "Pants", "§7Home pants");
        this.boots = Items.armor(Material.LEATHER_BOOTS, white, "Boots", "§7Team boots");
        this.block = Items.create(Material.RED_CONCRETE, this.name, "§7ACC");
    }
}

