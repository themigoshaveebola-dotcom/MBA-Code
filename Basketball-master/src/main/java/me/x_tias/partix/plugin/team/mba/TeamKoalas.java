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

public final class TeamKoalas
        extends BaseTeam {
    public TeamKoalas() {
        int lightBlue = 0x66CCFF;
        int navy = 11871;
        int grey = 12436166;
        this.firstColor = TextColor.color(lightBlue);
        this.secondColor = TextColor.color(navy);
        this.thirdColor = TextColor.color(grey);
        this.abrv = Text.gradient("NSH", this.firstColor, this.secondColor, true);
        this.name = Text.gradient("Nashville\u00a0Koalas", this.firstColor, this.secondColor, true);
        this.chest = Items.armor(Material.LEATHER_CHESTPLATE, navy, "Jersey", "§7Home jersey");
        this.away = Items.armor(Material.LEATHER_LEGGINGS, navy, "Pants", "§7Away pants");
        this.pants = Items.armor(Material.LEATHER_LEGGINGS, navy, "Pants", "§7Home pants");
        this.boots = Items.armor(Material.LEATHER_BOOTS, 16766208, "Boots", "§7Team boots");
        this.block = Items.create(Material.LIGHT_BLUE_GLAZED_TERRACOTTA, this.name, "§7East");
    }
}

