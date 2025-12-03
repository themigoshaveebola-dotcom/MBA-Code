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

public final class TeamWeberState
        extends BaseTeam {
    public TeamWeberState() {
        int purple = 5188991;
        int silver = 10987948;
        int white = 0xFFFFFF;
        this.firstColor = TextColor.color(purple);
        this.secondColor = TextColor.color(silver);
        this.thirdColor = TextColor.color(white);
        this.abrv = Text.gradient("WSU", this.firstColor, this.secondColor, true);
        this.name = Text.gradient("Weber\u00a0State\u00a0Wildcats", this.firstColor, this.secondColor, true);
        this.chest = Items.armor(Material.LEATHER_CHESTPLATE, purple, "Jersey", "§7Home jersey");
        this.away = Items.armor(Material.LEATHER_LEGGINGS, purple, "Pants", "§7Away pants");
        this.pants = Items.armor(Material.LEATHER_LEGGINGS, silver, "Pants", "§7Home pants");
        this.boots = Items.armor(Material.LEATHER_BOOTS, purple, "Boots", "§7Team boots");
        this.block = Items.create(Material.PURPLE_GLAZED_TERRACOTTA, this.name, "§7Big\u202fSky");
    }
}

