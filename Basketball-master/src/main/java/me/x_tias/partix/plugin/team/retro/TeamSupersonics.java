/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  net.kyori.adventure.text.format.TextColor
 *  org.bukkit.Material
 */
package me.x_tias.partix.plugin.team.retro;

import me.x_tias.partix.plugin.team.BaseTeam;
import me.x_tias.partix.util.Items;
import me.x_tias.partix.util.Text;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;

public final class TeamSupersonics
        extends BaseTeam {
    public TeamSupersonics() {
        int main = 25914;
        int accent = 16761893;
        int dark = 12544;
        this.firstColor = TextColor.color(main);
        this.secondColor = TextColor.color(accent);
        this.thirdColor = TextColor.color(dark);
        this.abrv = Text.gradient("SEA", this.secondColor, this.firstColor, true);
        this.name = Text.gradient("Seattle\u202fSuperSonics", this.secondColor, this.firstColor, true);
        this.chest = Items.armor(Material.LEATHER_CHESTPLATE, main, "Jersey", "§r§7Your team jersey");
        this.away = Items.armor(Material.LEATHER_LEGGINGS, main, "Pants", "§r§7Away pants");
        this.pants = Items.armor(Material.LEATHER_LEGGINGS, accent, "Pants", "§r§7Home pants");
        this.boots = Items.armor(Material.LEATHER_BOOTS, dark, "Shoes", "§r§7Team shoes");
        this.block = Items.create(Material.LIME_GLAZED_TERRACOTTA, this.name, "§r§7Retro");
    }
}

