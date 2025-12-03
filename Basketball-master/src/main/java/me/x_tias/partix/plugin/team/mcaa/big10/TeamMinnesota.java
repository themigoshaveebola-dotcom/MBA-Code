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

public final class TeamMinnesota
        extends BaseTeam {
    public TeamMinnesota() {
        int maroon = 7995417;
        int gold = 0xFFCC33;
        int black = 0;
        this.firstColor = TextColor.color(maroon);
        this.secondColor = TextColor.color(gold);
        this.thirdColor = TextColor.color(black);
        this.abrv = Text.gradient("MINN", this.firstColor, this.secondColor, true);
        this.name = Text.gradient("Minnesota\u00a0Golden\u00a0Gophers", this.firstColor, this.secondColor, true);
        this.chest = Items.armor(Material.LEATHER_CHESTPLATE, maroon, "Jersey", "§7Home jersey");
        this.away = Items.armor(Material.LEATHER_LEGGINGS, maroon, "Pants", "§7Away pants");
        this.pants = Items.armor(Material.LEATHER_LEGGINGS, gold, "Pants", "§7Home pants");
        this.boots = Items.armor(Material.LEATHER_BOOTS, black, "Boots", "§7Team boots");
        this.block = Items.create(Material.GOLD_BLOCK, this.name, "§7Big\u202fTen");
    }
}

