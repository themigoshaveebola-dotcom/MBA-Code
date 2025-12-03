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

public final class TeamOhioState
        extends BaseTeam {
    public TeamOhioState() {
        int scarlet = 0xBB0000;
        int gray = 0x666666;
        int black = 0;
        this.firstColor = TextColor.color(scarlet);
        this.secondColor = TextColor.color(gray);
        this.thirdColor = TextColor.color(black);
        this.abrv = Text.gradient("OSU", this.firstColor, this.secondColor, true);
        this.name = Text.gradient("Ohio\u00a0State\u00a0Buckeyes", this.firstColor, this.secondColor, true);
        this.chest = Items.armor(Material.LEATHER_CHESTPLATE, scarlet, "Jersey", "§7Home jersey");
        this.away = Items.armor(Material.LEATHER_LEGGINGS, scarlet, "Pants", "§7Away pants");
        this.pants = Items.armor(Material.LEATHER_LEGGINGS, gray, "Pants", "§7Home pants");
        this.boots = Items.armor(Material.LEATHER_BOOTS, black, "Boots", "§7Team boots");
        this.block = Items.create(Material.REDSTONE_BLOCK, this.name, "§7Big\u202fTen");
    }
}

