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

public class TeamBows
        extends BaseTeam {
    public TeamBows() {
        int main = 0xCE1141;
        int light = 0xFFFFFF;
        int dark = 0;
        this.firstColor = TextColor.color(main);
        this.secondColor = TextColor.color(light);
        this.thirdColor = TextColor.color(dark);
        this.abrv = Text.gradient("CHI", this.secondColor, this.firstColor, true);
        this.name = Text.gradient("Chicago Bows", this.secondColor, this.firstColor, true);
        this.chest = Items.armor(Material.LEATHER_CHESTPLATE, main, "Jersey", "§r§7Your teams jersey");
        this.away = Items.armor(Material.LEATHER_LEGGINGS, main, "Pants", "§r§7Your teams away pants");
        this.pants = Items.armor(Material.LEATHER_LEGGINGS, main, "Pants", "§r§7Your teams pants");
        this.boots = Items.armor(Material.LEATHER_BOOTS, dark, "Boots", "§r§7Your teams boots");
        this.block = Items.create(Material.BOW, this.name, "§r§7East");
    }
}

