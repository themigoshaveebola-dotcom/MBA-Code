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

public final class TeamFlamingoFlyers
        extends BaseTeam {
    public TeamFlamingoFlyers() {
        int pink = 16073375;
        int aqua = 6017279;
        int dark = 666954;
        this.firstColor = TextColor.color(pink);
        this.secondColor = TextColor.color(aqua);
        this.thirdColor = TextColor.color(dark);
        this.abrv = Text.gradient("MFF", this.firstColor, this.secondColor, true);
        this.name = Text.gradient("Miami\u00a0Flamingo\u00a0Flyers", this.firstColor, this.secondColor, true);
        this.chest = Items.armor(Material.LEATHER_CHESTPLATE, pink, "Jersey", "§7Home jersey");
        this.away = Items.armor(Material.LEATHER_LEGGINGS, pink, "Pants", "§7Away pants");
        this.pants = Items.armor(Material.LEATHER_LEGGINGS, aqua, "Pants", "§7Home pants");
        this.boots = Items.armor(Material.LEATHER_BOOTS, dark, "Boots", "§7Team boots");
        this.block = Items.create(Material.PINK_GLAZED_TERRACOTTA, this.name, "§7East");
    }
}

