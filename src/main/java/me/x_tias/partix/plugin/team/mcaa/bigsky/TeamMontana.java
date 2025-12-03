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

public final class TeamMontana
        extends BaseTeam {
    public TeamMontana() {
        int maroon = 5774647;
        int silver = 10987948;
        int black = 0;
        this.firstColor = TextColor.color(maroon);
        this.secondColor = TextColor.color(silver);
        this.thirdColor = TextColor.color(black);
        this.abrv = Text.gradient("UM", this.firstColor, this.secondColor, true);
        this.name = Text.gradient("Montana\u00a0Grizzlies", this.firstColor, this.secondColor, true);
        this.chest = Items.armor(Material.LEATHER_CHESTPLATE, maroon, "Jersey", "§7Home jersey");
        this.away = Items.armor(Material.LEATHER_LEGGINGS, maroon, "Pants", "§7Away pants");
        this.pants = Items.armor(Material.LEATHER_LEGGINGS, silver, "Pants", "§7Home pants");
        this.boots = Items.armor(Material.LEATHER_BOOTS, black, "Boots", "§7Team boots");
        this.block = Items.create(Material.PURPLE_GLAZED_TERRACOTTA, this.name, "§7Big\u202fSky");
    }
}

