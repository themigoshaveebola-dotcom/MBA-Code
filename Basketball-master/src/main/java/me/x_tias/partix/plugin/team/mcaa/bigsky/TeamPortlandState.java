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

public final class TeamPortlandState
        extends BaseTeam {
    public TeamPortlandState() {
        int green = 19776;
        int white = 0xFFFFFF;
        int gray = 0x8A8A8D;
        this.firstColor = TextColor.color(green);
        this.secondColor = TextColor.color(white);
        this.thirdColor = TextColor.color(gray);
        this.abrv = Text.gradient("PSU", this.firstColor, this.secondColor, true);
        this.name = Text.gradient("Portland\u00a0State\u00a0Vikings", this.firstColor, this.secondColor, true);
        this.chest = Items.armor(Material.LEATHER_CHESTPLATE, green, "Jersey", "§7Home jersey");
        this.away = Items.armor(Material.LEATHER_LEGGINGS, green, "Pants", "§7Away pants");
        this.pants = Items.armor(Material.LEATHER_LEGGINGS, white, "Pants", "§7Home pants");
        this.boots = Items.armor(Material.LEATHER_BOOTS, green, "Boots", "§7Team boots");
        this.block = Items.create(Material.GREEN_GLAZED_TERRACOTTA, this.name, "§7Big\u202fSky");
    }
}

