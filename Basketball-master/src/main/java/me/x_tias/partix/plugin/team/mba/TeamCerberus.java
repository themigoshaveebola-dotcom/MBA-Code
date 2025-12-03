/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  net.kyori.adventure.text.format.TextColor
 *  org.bukkit.Material
 */
package me.x_tias.partix.plugin.team.mba;

import me.x_tias.partix.plugin.team.BaseTeam;
import me.x_tias.partix.util.Items;
import me.x_tias.partix.util.Text;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;

public final class TeamCerberus
        extends BaseTeam {
    public TeamCerberus() {
        int black = 657930;
        int gold = 13938487;
        int grey = 0x6C6C6C;
        this.firstColor = TextColor.color(black);
        this.secondColor = TextColor.color(gold);
        this.thirdColor = TextColor.color(grey);
        this.abrv = Text.gradient("WSC", this.secondColor, this.firstColor, true);
        this.name = Text.gradient("Washington\u00a0Cerberus", this.secondColor, this.firstColor, true);
        this.chest = Items.armor(Material.LEATHER_CHESTPLATE, black, "Jersey", "§7Home jersey");
        this.away = Items.armor(Material.LEATHER_LEGGINGS, black, "Pants", "§7Away pants");
        this.pants = Items.armor(Material.LEATHER_LEGGINGS, gold, "Pants", "§7Home pants");
        this.boots = Items.armor(Material.LEATHER_BOOTS, grey, "Boots", "§7Team boots");
        this.block = Items.create(Material.BLACK_GLAZED_TERRACOTTA, this.name, "§7West");
    }
}

