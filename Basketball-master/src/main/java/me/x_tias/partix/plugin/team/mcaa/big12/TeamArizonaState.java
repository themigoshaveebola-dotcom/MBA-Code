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

public final class TeamArizonaState
        extends BaseTeam {
    public TeamArizonaState() {
        int maroon = 9184819;
        int gold = 16762407;
        int white = 0xFFFFFF;
        this.firstColor = TextColor.color(maroon);
        this.secondColor = TextColor.color(gold);
        this.thirdColor = TextColor.color(white);
        this.abrv = Text.gradient("ASU", this.firstColor, this.secondColor, true);
        this.name = Text.gradient("Arizona\u202fState\u202fSun\u202fDevils", this.firstColor, this.secondColor, true);
        this.chest = Items.armor(Material.LEATHER_CHESTPLATE, maroon, "Jersey", "§7Home jersey");
        this.away = Items.armor(Material.LEATHER_LEGGINGS, maroon, "Pants", "§7Away pants");
        this.pants = Items.armor(Material.LEATHER_LEGGINGS, maroon, "Pants", "§7Home pants");
        this.boots = Items.armor(Material.LEATHER_BOOTS, gold, "Boots", "§7Team boots");
        this.block = Items.create(Material.MAGENTA_GLAZED_TERRACOTTA, this.name, "§7BIG‑12");
    }
}

