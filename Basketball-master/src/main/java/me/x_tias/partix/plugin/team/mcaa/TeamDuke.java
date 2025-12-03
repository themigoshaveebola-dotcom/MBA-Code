/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  net.kyori.adventure.text.format.TextColor
 *  org.bukkit.Material
 */
package me.x_tias.partix.plugin.team.mcaa;

import me.x_tias.partix.plugin.team.BaseTeam;
import me.x_tias.partix.util.Items;
import me.x_tias.partix.util.Text;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;

public final class TeamDuke
        extends BaseTeam {
    public TeamDuke() {
        int blue = 472740;
        int black = 0;
        int white = 0xFFFFFF;
        this.firstColor = TextColor.color(blue);
        this.secondColor = TextColor.color(black);
        this.thirdColor = TextColor.color(white);
        this.abrv = Text.gradient("DUK", this.firstColor, this.secondColor, true);
        this.name = Text.gradient("Duke\u00a0Blue\u00a0Devils", this.firstColor, this.secondColor, true);
        this.chest = Items.armor(Material.LEATHER_CHESTPLATE, blue, "Jersey", "§7Home jersey");
        this.away = Items.armor(Material.LEATHER_LEGGINGS, blue, "Pants", "§7Away pants");
        this.pants = Items.armor(Material.LEATHER_LEGGINGS, black, "Pants", "§7Home pants");
        this.boots = Items.armor(Material.LEATHER_BOOTS, white, "Boots", "§7Team boots");
        this.block = Items.create(Material.LAPIS_LAZULI, this.name, "§7ACC");
    }
}

