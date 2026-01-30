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

public class TeamDragons
        extends BaseTeam {
    public TeamDragons() {
        int main = 1917626;
        int light = 13111342;
        int dark = 12501186;
        this.firstColor = TextColor.color(main);
        this.secondColor = TextColor.color(light);
        this.thirdColor = TextColor.color(dark);
        this.abrv = Text.gradient("DET", this.secondColor, this.firstColor, true);
        this.name = Text.gradient("Detroit Dragons", this.secondColor, this.firstColor, true);
        this.chest = Items.armor(Material.LEATHER_CHESTPLATE, main, "Jersey", "§r§7Your teams jersey");
        this.away = Items.armor(Material.LEATHER_LEGGINGS, main, "Jersey", "§r§7Your teams away jersey");
        this.pants = Items.armor(Material.LEATHER_LEGGINGS, dark, "Pants", "§r§7Your teams pants");
        this.boots = Items.armor(Material.LEATHER_BOOTS, light, "Boots", "§r§7Your teams boots");
        this.block = Items.create(Material.DRAGON_HEAD, this.name, "§r§7East");
    }
}

