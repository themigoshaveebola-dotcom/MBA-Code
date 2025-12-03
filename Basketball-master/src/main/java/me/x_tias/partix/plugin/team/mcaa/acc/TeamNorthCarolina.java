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

public class TeamNorthCarolina
        extends BaseTeam {
    public TeamNorthCarolina() {
        int main = 8105940;
        int light = 0xFFFFFF;
        int dark = 4947624;
        this.firstColor = TextColor.color(main);
        this.secondColor = TextColor.color(light);
        this.thirdColor = TextColor.color(dark);
        this.abrv = Text.gradient("UNC", this.secondColor, this.firstColor, true);
        this.name = Text.gradient("North\u202fCarolina Tar\u202fHeels", this.secondColor, this.firstColor, true);
        this.chest = Items.armor(Material.LEATHER_CHESTPLATE, main, "Jersey", "§7Your team's jersey");
        this.away = Items.armor(Material.LEATHER_LEGGINGS, main, "Pants", "§7Your team's away pants");
        this.pants = Items.armor(Material.LEATHER_LEGGINGS, light, "Pants", "§7Your team's pants");
        this.boots = Items.armor(Material.LEATHER_BOOTS, dark, "Boots", "§7Your team's boots");
        this.block = Items.create(Material.FIREWORK_STAR, this.name, "§7ACC");
    }
}

