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

public final class TeamVirginia
        extends BaseTeam {
    public TeamVirginia() {
        int navy = 2305355;
        int orange = 15036928;
        int white = 0xFFFFFF;
        this.firstColor = TextColor.color(navy);
        this.secondColor = TextColor.color(orange);
        this.thirdColor = TextColor.color(white);
        this.abrv = Text.gradient("UVA", this.firstColor, this.secondColor, true);
        this.name = Text.gradient("Virginia\u00a0Cavaliers", this.firstColor, this.secondColor, true);
        this.chest = Items.armor(Material.LEATHER_CHESTPLATE, navy, "Jersey", "§7Home jersey");
        this.away = Items.armor(Material.LEATHER_LEGGINGS, navy, "Pants", "§7Away pants");
        this.pants = Items.armor(Material.LEATHER_LEGGINGS, orange, "Pants", "§7Home pants");
        this.boots = Items.armor(Material.LEATHER_BOOTS, white, "Boots", "§7Team boots");
        this.block = Items.create(Material.NETHERITE_SCRAP, this.name, "§7ACC");
    }
}

