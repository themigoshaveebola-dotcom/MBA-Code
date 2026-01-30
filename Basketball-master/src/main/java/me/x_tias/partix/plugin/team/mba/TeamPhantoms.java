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

public class TeamPhantoms
        extends BaseTeam {
    public TeamPhantoms() {
        int main = 1904992;
        int light = 15032352;
        int dark = 0;
        this.firstColor = TextColor.color(main);
        this.secondColor = TextColor.color(light);
        this.thirdColor = TextColor.color(dark);
        this.abrv = Text.gradient("PHX", this.secondColor, this.firstColor, true);
        this.name = Text.gradient("Phoenix Phantoms", this.secondColor, this.firstColor, true);
        this.chest = Items.armor(Material.LEATHER_CHESTPLATE, main, "Jersey", "§r§7Your teams jersey");
        this.away = Items.armor(Material.LEATHER_LEGGINGS, main, "Pants", "§r§7Your teams away pants");
        this.pants = Items.armor(Material.LEATHER_LEGGINGS, dark, "Pants", "§r§7Your teams pants");
        this.boots = Items.armor(Material.LEATHER_BOOTS, light, "Boots", "§r§7Your teams boots");
        this.block = Items.create(Material.PHANTOM_MEMBRANE, this.name, "§r§7West");
    }
}

