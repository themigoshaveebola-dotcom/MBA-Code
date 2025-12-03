/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  net.kyori.adventure.text.format.TextColor
 *  org.bukkit.Material
 */
package me.x_tias.partix.plugin.team.mcaa.big10;

import me.x_tias.partix.plugin.team.BaseTeam;
import me.x_tias.partix.util.Items;
import me.x_tias.partix.util.Text;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;

public final class TeamMaryland
        extends BaseTeam {
    public TeamMaryland() {
        int primary = 14694974;
        int secondary = 16762668;
        int accent = 0;
        this.firstColor = TextColor.color(primary);
        this.secondColor = TextColor.color(secondary);
        this.thirdColor = TextColor.color(accent);
        this.abrv = Text.gradient("UMD", this.firstColor, this.secondColor, true);
        this.name = Text.gradient("Maryland\u00a0Terrapins", this.firstColor, this.secondColor, true);
        this.chest = Items.armor(Material.LEATHER_CHESTPLATE, primary, "Jersey", "§7Home jersey");
        this.away = Items.armor(Material.LEATHER_LEGGINGS, primary, "Pants", "§7Away pants");
        this.pants = Items.armor(Material.LEATHER_LEGGINGS, secondary, "Pants", "§7Home pants");
        this.boots = Items.armor(Material.LEATHER_BOOTS, accent, "Boots", "§7Team boots");
        this.block = Items.create(Material.RED_TERRACOTTA, this.name, "§7Big\u00a0Ten");
    }
}

