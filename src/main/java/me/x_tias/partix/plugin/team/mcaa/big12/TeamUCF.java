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

public final class TeamUCF
        extends BaseTeam {
    public TeamUCF() {
        int gold = 11770718;
        int black = 0;
        int white = 0xFFFFFF;
        this.firstColor = TextColor.color(gold);
        this.secondColor = TextColor.color(black);
        this.thirdColor = TextColor.color(white);
        this.abrv = Text.gradient("UCF", this.firstColor, this.secondColor, true);
        this.name = Text.gradient("UCF\u202fKnights", this.firstColor, this.secondColor, true);
        this.chest = Items.armor(Material.LEATHER_CHESTPLATE, gold, "Jersey", "§7Home jersey");
        this.away = Items.armor(Material.LEATHER_LEGGINGS, gold, "Pants", "§7Away pants");
        this.pants = Items.armor(Material.LEATHER_LEGGINGS, gold, "Pants", "§7Home pants");
        this.boots = Items.armor(Material.LEATHER_BOOTS, black, "Boots", "§7Team boots");
        this.block = Items.create(Material.YELLOW_GLAZED_TERRACOTTA, this.name, "§7BIG‑12");
    }
}

