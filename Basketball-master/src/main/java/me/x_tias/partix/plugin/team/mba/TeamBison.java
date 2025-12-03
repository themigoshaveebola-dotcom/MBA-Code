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

public final class TeamBison
        extends BaseTeam {
    public TeamBison() {
        int orange = 15434012;
        int blue = 15740;
        int coal = 0x1E1E1E;
        this.firstColor = TextColor.color(orange);
        this.secondColor = TextColor.color(blue);
        this.thirdColor = TextColor.color(coal);
        this.abrv = Text.gradient("BRX", this.firstColor, this.secondColor, true);
        this.name = Text.gradient("Bronx\u00a0Bison", this.firstColor, this.secondColor, true);
        this.chest = Items.armor(Material.LEATHER_CHESTPLATE, blue, "Jersey", "§7Home jersey");
        this.away = Items.armor(Material.LEATHER_LEGGINGS, blue, "Pants", "§7Away pants");
        this.pants = Items.armor(Material.LEATHER_LEGGINGS, orange, "Pants", "§7Home pants");
        this.boots = Items.armor(Material.LEATHER_BOOTS, coal, "Boots", "§7Team boots");
        this.block = Items.create(Material.ORANGE_GLAZED_TERRACOTTA, this.name, "§7West");
    }
}

