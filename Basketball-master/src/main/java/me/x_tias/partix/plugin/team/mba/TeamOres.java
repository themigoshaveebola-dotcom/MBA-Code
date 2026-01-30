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

public class TeamOres
        extends BaseTeam {
    public TeamOres() {
        int main = 795456;
        int light = 13111342;
        int dark = 8745293;
        this.firstColor = TextColor.color(main);
        this.secondColor = TextColor.color(light);
        this.thirdColor = TextColor.color(dark);
        this.abrv = Text.gradient("NOP", this.secondColor, this.firstColor, true);
        this.name = Text.gradient("New Orleans Ores", this.secondColor, this.firstColor, true);
        this.chest = Items.armor(Material.LEATHER_CHESTPLATE, main, "Jersey", "§r§7Your teams jersey");
        this.away = Items.armor(Material.LEATHER_LEGGINGS, main, "Pants", "§r§7Your teams away pants");
        this.pants = Items.armor(Material.LEATHER_LEGGINGS, light, "Pants", "§r§7Your teams pants");
        this.boots = Items.armor(Material.LEATHER_BOOTS, dark, "Boots", "§r§7Your teams boots");
        this.block = Items.create(Material.LAPIS_ORE, this.name, "§r§7West");
    }
}

