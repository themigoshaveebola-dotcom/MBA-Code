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

public class TeamGuardians
        extends BaseTeam {
    public TeamGuardians() {
        int main = 1917578;
        int light = 16762668;
        int dark = 0xFFFFFF;
        this.firstColor = TextColor.color(main);
        this.secondColor = TextColor.color(light);
        this.thirdColor = TextColor.color(dark);
        this.abrv = Text.gradient("GSW", this.secondColor, this.firstColor, true);
        this.name = Text.gradient("Golden State Guardians", this.secondColor, this.firstColor, true);
        this.chest = Items.armor(Material.LEATHER_CHESTPLATE, main, "Jersey", "§r§7Your teams jersey");
        this.away = Items.armor(Material.LEATHER_LEGGINGS, main, "Pants", "§r§7Your teams away pants");
        this.pants = Items.armor(Material.LEATHER_LEGGINGS, dark, "Pants", "§r§7Your teams pants");
        this.boots = Items.armor(Material.LEATHER_BOOTS, light, "Boots", "§r§7Your teams boots");
        this.block = Items.create(Material.GUARDIAN_SPAWN_EGG, this.name, "§r§7West");
    }
}

