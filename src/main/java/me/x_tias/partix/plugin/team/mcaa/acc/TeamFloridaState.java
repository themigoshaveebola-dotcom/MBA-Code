/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  net.kyori.adventure.text.format.TextColor
 *  org.bukkit.Material
 */
package me.x_tias.partix.plugin.team.mcaa.acc;

import me.x_tias.partix.plugin.team.BaseTeam;
import me.x_tias.partix.util.Items;
import me.x_tias.partix.util.Text;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;

public final class TeamFloridaState
        extends BaseTeam {
    public TeamFloridaState() {
        int garnet = 7876416;
        int gold = 13547656;
        int white = 0xFFFFFF;
        this.firstColor = TextColor.color(garnet);
        this.secondColor = TextColor.color(gold);
        this.thirdColor = TextColor.color(white);
        this.abrv = Text.gradient("FSU", this.firstColor, this.secondColor, true);
        this.name = Text.gradient("Florida\u00a0State\u00a0Seminoles", this.firstColor, this.secondColor, true);
        this.chest = Items.armor(Material.LEATHER_CHESTPLATE, garnet, "Jersey", "§7Home jersey");
        this.away = Items.armor(Material.LEATHER_LEGGINGS, garnet, "Pants", "§7Away pants");
        this.pants = Items.armor(Material.LEATHER_LEGGINGS, gold, "Pants", "§7Home pants");
        this.boots = Items.armor(Material.LEATHER_BOOTS, white, "Boots", "§7Team boots");
        this.block = Items.create(Material.GOLD_INGOT, this.name, "§7ACC");
    }
}

