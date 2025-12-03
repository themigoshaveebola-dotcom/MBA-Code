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

public final class TeamPennState
        extends BaseTeam {
    public TeamPennState() {
        int navy = 14437;
        int white = 0xFFFFFF;
        int gray = 8555151;
        this.firstColor = TextColor.color(navy);
        this.secondColor = TextColor.color(white);
        this.thirdColor = TextColor.color(gray);
        this.abrv = Text.gradient("PSU", this.firstColor, this.secondColor, true);
        this.name = Text.gradient("Penn\u00a0State\u00a0Nittany\u00a0Lions", this.firstColor, this.secondColor, true);
        this.chest = Items.armor(Material.LEATHER_CHESTPLATE, navy, "Jersey", "§7Home jersey");
        this.away = Items.armor(Material.LEATHER_LEGGINGS, navy, "Pants", "§7Away pants");
        this.pants = Items.armor(Material.LEATHER_LEGGINGS, white, "Pants", "§7Home pants");
        this.boots = Items.armor(Material.LEATHER_BOOTS, gray, "Boots", "§7Team boots");
        this.block = Items.create(Material.BLUE_CONCRETE, this.name, "§7Big\u202fTen");
    }
}

