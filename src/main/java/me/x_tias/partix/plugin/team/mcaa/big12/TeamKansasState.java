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

public final class TeamKansasState
        extends BaseTeam {
    public TeamKansasState() {
        int purple = 5318792;
        int silver = 10857647;
        int white = 0xFFFFFF;
        this.firstColor = TextColor.color(purple);
        this.secondColor = TextColor.color(silver);
        this.thirdColor = TextColor.color(white);
        this.abrv = Text.gradient("KSU", this.firstColor, this.secondColor, true);
        this.name = Text.gradient("Kansas\u202fState\u202fWildcats", this.firstColor, this.secondColor, true);
        this.chest = Items.armor(Material.LEATHER_CHESTPLATE, purple, "Jersey", "§7Home jersey");
        this.away = Items.armor(Material.LEATHER_LEGGINGS, purple, "Pants", "§7Away pants");
        this.pants = Items.armor(Material.LEATHER_LEGGINGS, purple, "Pants", "§7Home pants");
        this.boots = Items.armor(Material.LEATHER_BOOTS, silver, "Boots", "§7Team boots");
        this.block = Items.create(Material.PURPLE_GLAZED_TERRACOTTA, this.name, "§7BIG‑12");
    }
}

