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

public final class TeamPittsburgh
        extends BaseTeam {
    public TeamPittsburgh() {
        int blue = 13716;
        int gold = 16758812;
        int white = 0xFFFFFF;
        this.firstColor = TextColor.color(blue);
        this.secondColor = TextColor.color(gold);
        this.thirdColor = TextColor.color(white);
        this.abrv = Text.gradient("PITT", this.firstColor, this.secondColor, true);
        this.name = Text.gradient("Pittsburgh\u00a0Panthers", this.firstColor, this.secondColor, true);
        this.chest = Items.armor(Material.LEATHER_CHESTPLATE, blue, "Jersey", "§7Home jersey");
        this.away = Items.armor(Material.LEATHER_LEGGINGS, blue, "Pants", "§7Away pants");
        this.pants = Items.armor(Material.LEATHER_LEGGINGS, gold, "Pants", "§7Home pants");
        this.boots = Items.armor(Material.LEATHER_BOOTS, white, "Boots", "§7Team boots");
        this.block = Items.create(Material.BLUE_DYE, this.name, "§7ACC");
    }
}

