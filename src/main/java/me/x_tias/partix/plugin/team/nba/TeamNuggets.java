/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  net.kyori.adventure.text.format.TextColor
 *  org.bukkit.Material
 */
package me.x_tias.partix.plugin.team.nba;

import me.x_tias.partix.plugin.team.BaseTeam;
import me.x_tias.partix.util.Items;
import me.x_tias.partix.util.Text;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;

public class TeamNuggets
        extends BaseTeam {
    public TeamNuggets() {
        int main = 926272;
        int light = 0xFFFFFF;
        int dark = 16696612;
        this.firstColor = TextColor.color(main);
        this.secondColor = TextColor.color(light);
        this.thirdColor = TextColor.color(dark);
        this.abrv = Text.gradient("DEN", this.secondColor, this.firstColor, true);
        this.name = Text.gradient("Denver Nuggets", this.secondColor, this.firstColor, true);
        this.chest = Items.armor(Material.LEATHER_CHESTPLATE, main, "Jersey", "§r§7Your teams jersey");
        this.away = Items.armor(Material.LEATHER_LEGGINGS, main, "Pants", "§r§7Your teams away pants");
        this.pants = Items.armor(Material.LEATHER_LEGGINGS, light, "Pants", "§r§7Your teams pants");
        this.boots = Items.armor(Material.LEATHER_BOOTS, dark, "Boots", "§r§7Your teams boots");
        this.block = Items.create(Material.GOLD_NUGGET, this.name, "§r§7West");
    }
}

