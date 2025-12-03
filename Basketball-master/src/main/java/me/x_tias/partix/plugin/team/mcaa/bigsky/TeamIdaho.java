/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  net.kyori.adventure.text.format.TextColor
 *  org.bukkit.Material
 */
package me.x_tias.partix.plugin.team.mcaa.bigsky;

import me.x_tias.partix.plugin.team.BaseTeam;
import me.x_tias.partix.util.Items;
import me.x_tias.partix.util.Text;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;

public final class TeamIdaho
        extends BaseTeam {
    public TeamIdaho() {
        int gold = 12162907;
        int black = 0;
        int gray = 0x8A8A8D;
        this.firstColor = TextColor.color(gold);
        this.secondColor = TextColor.color(black);
        this.thirdColor = TextColor.color(gray);
        this.abrv = Text.gradient("ID", this.firstColor, this.secondColor, true);
        this.name = Text.gradient("Idaho\u00a0Vandals", this.firstColor, this.secondColor, true);
        this.chest = Items.armor(Material.LEATHER_CHESTPLATE, gold, "Jersey", "§7Home jersey");
        this.away = Items.armor(Material.LEATHER_LEGGINGS, black, "Pants", "§7Away pants");
        this.pants = Items.armor(Material.LEATHER_LEGGINGS, gold, "Pants", "§7Home pants");
        this.boots = Items.armor(Material.LEATHER_BOOTS, gold, "Boots", "§7Team boots");
        this.block = Items.create(Material.YELLOW_GLAZED_TERRACOTTA, this.name, "§7Big\u202fSky");
    }
}

