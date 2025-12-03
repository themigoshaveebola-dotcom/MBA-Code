/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  org.bukkit.Material
 *  org.bukkit.inventory.ItemStack
 */
package me.x_tias.partix.plugin.team.gui;

import me.x_tias.partix.util.Items;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public final class GuiUtil {
    public static final ItemStack FILLER = Items.get(Component.text(" "), Material.BLACK_STAINED_GLASS_PANE, 1, " ");

    private GuiUtil() {
    }
}

