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

public final class TeamEasternWashington
        extends BaseTeam {
    public TeamEasternWashington() {
        int red = 13504292;
        int black = 0;
        int gray = 0x8A8A8D;
        this.firstColor = TextColor.color(red);
        this.secondColor = TextColor.color(gray);
        this.thirdColor = TextColor.color(black);
        this.abrv = Text.gradient("EWU", this.firstColor, this.thirdColor, true);
        this.name = Text.gradient("Eastern\u00a0Washington\u00a0Eagles", this.firstColor, this.thirdColor, true);
        this.chest = Items.armor(Material.LEATHER_CHESTPLATE, red, "Jersey", "§7Home jersey");
        this.away = Items.armor(Material.LEATHER_LEGGINGS, red, "Pants", "§7Away pants");
        this.pants = Items.armor(Material.LEATHER_LEGGINGS, gray, "Pants", "§7Home pants");
        this.boots = Items.armor(Material.LEATHER_BOOTS, black, "Boots", "§7Team boots");
        this.block = Items.create(Material.RED_GLAZED_TERRACOTTA, this.name, "§7Big\u202fSky");
    }
}

