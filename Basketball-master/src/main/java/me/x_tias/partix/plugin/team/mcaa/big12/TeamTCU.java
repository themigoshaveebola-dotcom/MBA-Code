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

public final class TeamTCU
        extends BaseTeam {
    public TeamTCU() {
        int purple = 5052793;
        int black = 0;
        int grey = 10857647;
        this.firstColor = TextColor.color(purple);
        this.secondColor = TextColor.color(black);
        this.thirdColor = TextColor.color(grey);
        this.abrv = Text.gradient("TCU", this.firstColor, this.secondColor, true);
        this.name = Text.gradient("TCU\u202fHorned\u202fFrogs", this.firstColor, this.secondColor, true);
        this.chest = Items.armor(Material.LEATHER_CHESTPLATE, purple, "Jersey", "§7Home jersey");
        this.away = Items.armor(Material.LEATHER_LEGGINGS, purple, "Pants", "§7Away pants");
        this.pants = Items.armor(Material.LEATHER_LEGGINGS, purple, "Pants", "§7Home pants");
        this.boots = Items.armor(Material.LEATHER_BOOTS, grey, "Boots", "§7Team boots");
        this.block = Items.create(Material.PURPLE_GLAZED_TERRACOTTA, this.name, "§7BIG‑12");
    }
}

