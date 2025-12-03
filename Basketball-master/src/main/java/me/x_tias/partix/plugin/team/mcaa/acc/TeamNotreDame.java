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

public final class TeamNotreDame
        extends BaseTeam {
    public TeamNotreDame() {
        int navy = 795456;
        int gold = 13211392;
        int white = 0xFFFFFF;
        this.firstColor = TextColor.color(navy);
        this.secondColor = TextColor.color(gold);
        this.thirdColor = TextColor.color(white);
        this.abrv = Text.gradient("ND", this.firstColor, this.secondColor, true);
        this.name = Text.gradient("Notre\u00a0Dame\u00a0Fighting\u00a0Irish", this.firstColor, this.secondColor, true);
        this.chest = Items.armor(Material.LEATHER_CHESTPLATE, navy, "Jersey", "§7Home jersey");
        this.away = Items.armor(Material.LEATHER_LEGGINGS, navy, "Pants", "§7Away pants");
        this.pants = Items.armor(Material.LEATHER_LEGGINGS, gold, "Pants", "§7Home pants");
        this.boots = Items.armor(Material.LEATHER_BOOTS, white, "Boots", "§7Team boots");
        this.block = Items.create(Material.NAUTILUS_SHELL, this.name, "§7ACC");
    }
}

