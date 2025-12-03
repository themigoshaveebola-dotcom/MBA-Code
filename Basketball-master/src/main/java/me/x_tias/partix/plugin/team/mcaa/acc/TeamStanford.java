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

public final class TeamStanford
        extends BaseTeam {
    public TeamStanford() {
        int cardinal = 9180437;
        int white = 0xFFFFFF;
        int black = 0;
        this.firstColor = TextColor.color(cardinal);
        this.secondColor = TextColor.color(white);
        this.thirdColor = TextColor.color(black);
        this.abrv = Text.gradient("STAN", this.firstColor, this.secondColor, true);
        this.name = Text.gradient("Stanford\u00a0Cardinal", this.firstColor, this.secondColor, true);
        this.chest = Items.armor(Material.LEATHER_CHESTPLATE, cardinal, "Jersey", "§7Home jersey");
        this.away = Items.armor(Material.LEATHER_LEGGINGS, cardinal, "Pants", "§7Away pants");
        this.pants = Items.armor(Material.LEATHER_LEGGINGS, white, "Pants", "§7Home pants");
        this.boots = Items.armor(Material.LEATHER_BOOTS, black, "Boots", "§7Team boots");
        this.block = Items.create(Material.RED_CONCRETE, this.name, "§7ACC");
    }
}

