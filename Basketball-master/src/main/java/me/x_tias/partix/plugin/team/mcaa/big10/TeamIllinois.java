/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  net.kyori.adventure.text.format.TextColor
 *  org.bukkit.Material
 */
package me.x_tias.partix.plugin.team.mcaa.big10;

import me.x_tias.partix.plugin.team.BaseTeam;
import me.x_tias.partix.util.Items;
import me.x_tias.partix.util.Text;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;

public final class TeamIllinois
        extends BaseTeam {
    public TeamIllinois() {
        int primary = 15223335;
        int secondary = 1255755;
        int accent = 10165827;
        this.firstColor = TextColor.color(primary);
        this.secondColor = TextColor.color(secondary);
        this.thirdColor = TextColor.color(accent);
        this.abrv = Text.gradient("ILL", this.firstColor, this.secondColor, true);
        this.name = Text.gradient("Illinois\u00a0Fighting\u00a0Illini", this.firstColor, this.secondColor, true);
        this.chest = Items.armor(Material.LEATHER_CHESTPLATE, secondary, "Jersey", "§7Home jersey");
        this.away = Items.armor(Material.LEATHER_LEGGINGS, secondary, "Pants", "§7Away pants");
        this.pants = Items.armor(Material.LEATHER_LEGGINGS, primary, "Pants", "§7Home pants");
        this.boots = Items.armor(Material.LEATHER_BOOTS, accent, "Boots", "§7Team boots");
        this.block = Items.create(Material.ORANGE_TERRACOTTA, this.name, "§7Big\u00a0Ten");
    }
}

