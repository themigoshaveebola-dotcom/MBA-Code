/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  org.bukkit.Material
 *  org.bukkit.Sound
 *  org.bukkit.SoundCategory
 *  org.bukkit.entity.Player
 */
package me.x_tias.partix.plugin.team.gui;

import me.x_tias.partix.plugin.gui.GUI;
import me.x_tias.partix.plugin.gui.ItemButton;
import me.x_tias.partix.plugin.team.BaseTeam;
import me.x_tias.partix.util.Colour;
import me.x_tias.partix.util.Items;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

public final class CategoryGUI {
    private static ItemButton button(int slot, Material icon, String title, String[] lore, Consumer<Player> click) {
        return new ItemButton(slot, Items.get(Component.text(title).color(Colour.partix()), icon, 1, lore), click);
    }

    public static void open(Player viewer, boolean editingHome, Consumer<BaseTeam> callback) {
        ItemButton[] btn = new ItemButton[27];
        for (int i = 0; i < btn.length; ++i) {
            btn[i] = new ItemButton(i, GuiUtil.FILLER, p -> {
            });
        }
        btn[10] = CategoryGUI.button(10, Material.NETHER_STAR, "MBA Teams", new String[]{"§7Fantasy professional league"}, p -> PagedTeamGUI.open(p, PagedTeamGUI.Category.MBA, editingHome, callback));
        btn[12] = CategoryGUI.button(12, Material.ENDER_EYE, "MCAA Teams", new String[]{"§7College divisions"}, p -> DivisionGUI.open(p, editingHome, callback));
        btn[14] = CategoryGUI.button(14, Material.DIAMOND, "NBA Teams §6(VIP)", new String[]{"§7Real NBA franchises"}, p -> {
            if (!p.hasPermission("rank.vip")) {
                CategoryGUI.deny(p);
                return;
            }
            PagedTeamGUI.open(p, PagedTeamGUI.Category.NBA, editingHome, callback);
        });
        btn[16] = CategoryGUI.button(16, Material.CLOCK, "Retro Teams §6(VIP)", new String[]{"§7Throw‑back jerseys"}, p -> {
            if (!p.hasPermission("rank.vip")) {
                CategoryGUI.deny(p);
                return;
            }
            PagedTeamGUI.open(p, PagedTeamGUI.Category.RETRO, editingHome, callback);
        });
        new GUI("Select Team Category", 3, false, btn).openInventory(viewer);
    }

    private static void deny(Player p) {
        p.sendMessage("§cVIP rank required.");
        p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_FALL, SoundCategory.MASTER, 1.0f, 1.0f);
    }
}

