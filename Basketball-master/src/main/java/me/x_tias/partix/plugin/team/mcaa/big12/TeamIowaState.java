/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  net.kyori.adventure.text.format.TextColor
 *  org.bukkit.Material
 */
package me.x_tias.partix.plugin.team.mcaa.big12;

import me.x_tias.partix.plugin.team.BaseTeam;
import me.x_tias.partix.util.Items;
import me.x_tias.partix.util.Text;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;

public final class TeamIowaState
        extends BaseTeam {
    public TeamIowaState() {
        int cardinal = 13111342;
        int gold = 15842888;
        int grey = 9276554;
        this.firstColor = TextColor.color(cardinal);
        this.secondColor = TextColor.color(gold);
        this.thirdColor = TextColor.color(grey);
        this.abrv = Text.gradient("ISU", this.firstColor, this.secondColor, true);
        this.name = Text.gradient("Iowa\u202fState\u202fCyclones", this.firstColor, this.secondColor, true);
        this.chest = Items.armor(Material.LEATHER_CHESTPLATE, cardinal, "Jersey", "§7Home jersey");
        this.away = Items.armor(Material.LEATHER_LEGGINGS, cardinal, "Pants", "§7Away pants");
        this.pants = Items.armor(Material.LEATHER_LEGGINGS, cardinal, "Pants", "§7Home pants");
        this.boots = Items.armor(Material.LEATHER_BOOTS, grey, "Boots", "§7Team boots");
        this.block = Items.create(Material.RED_GLAZED_TERRACOTTA, this.name, "§7BIG‑12");
    }
}

