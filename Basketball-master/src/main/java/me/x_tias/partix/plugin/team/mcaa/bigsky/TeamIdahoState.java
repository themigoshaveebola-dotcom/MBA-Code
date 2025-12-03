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

public final class TeamIdahoState
        extends BaseTeam {
    public TeamIdahoState() {
        int orange = 0xF77F00;
        int black = 0;
        int gray = 0x8A8A8D;
        this.firstColor = TextColor.color(orange);
        this.secondColor = TextColor.color(black);
        this.thirdColor = TextColor.color(gray);
        this.abrv = Text.gradient("ISU", this.firstColor, this.secondColor, true);
        this.name = Text.gradient("Idaho\u00a0State\u00a0Bengals", this.firstColor, this.secondColor, true);
        this.chest = Items.armor(Material.LEATHER_CHESTPLATE, orange, "Jersey", "§7Home jersey");
        this.away = Items.armor(Material.LEATHER_LEGGINGS, black, "Pants", "§7Away pants");
        this.pants = Items.armor(Material.LEATHER_LEGGINGS, orange, "Pants", "§7Home pants");
        this.boots = Items.armor(Material.LEATHER_BOOTS, black, "Boots", "§7Team boots");
        this.block = Items.create(Material.ORANGE_GLAZED_TERRACOTTA, this.name, "§7Big\u202fSky");
    }
}

