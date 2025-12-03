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

public final class TeamMiami
        extends BaseTeam {
    public TeamMiami() {
        int green = 20528;
        int orange = 16020257;
        int white = 0xFFFFFF;
        this.firstColor = TextColor.color(green);
        this.secondColor = TextColor.color(orange);
        this.thirdColor = TextColor.color(white);
        this.abrv = Text.gradient("MIA", this.firstColor, this.secondColor, true);
        this.name = Text.gradient("Miami\u00a0Hurricanes", this.firstColor, this.secondColor, true);
        this.chest = Items.armor(Material.LEATHER_CHESTPLATE, green, "Jersey", "§7Home jersey");
        this.away = Items.armor(Material.LEATHER_LEGGINGS, green, "Pants", "§7Away pants");
        this.pants = Items.armor(Material.LEATHER_LEGGINGS, orange, "Pants", "§7Home pants");
        this.boots = Items.armor(Material.LEATHER_BOOTS, white, "Boots", "§7Team boots");
        this.block = Items.create(Material.GREEN_DYE, this.name, "§7ACC");
    }
}

