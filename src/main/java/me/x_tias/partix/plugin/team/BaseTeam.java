/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.format.TextColor
 *  org.bukkit.inventory.ItemStack
 */
package me.x_tias.partix.plugin.team;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.inventory.ItemStack;

public abstract class BaseTeam {
    public ItemStack block;
    public Component abrv;
    public Component name;
    public TextColor firstColor;
    public TextColor secondColor;
    public TextColor thirdColor;
    public ItemStack chest;
    public ItemStack away;
    public ItemStack pants;
    public ItemStack boots;
}

