/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.TextComponent
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
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class CategoryTeamGUI {
    private CategoryTeamGUI() {
    }

    public static void open(Player viewer, boolean editingHome, Consumer<BaseTeam> teamChosen) {
        ItemButton[] btn = new ItemButton[27];
        for (int i = 0; i < btn.length; ++i) {
            btn[i] = new ItemButton(i, GuiUtil.FILLER, p -> {
            });
        }
        BiConsumer<Integer, Runnable> put = (slot, action) -> {
            int n = slot;
            int n2 = slot;
            TextComponent textComponent = Component.text(" ");
            Component component = textComponent.append(Component.text(switch (slot) {
                case 10 -> "MBA Teams";
                case 12 -> "MCAA Teams";
                case 14 -> "NBA Teams";
                case 16 -> "Retro Teams";
                default -> "?";
            }).color(Colour.partix()));
            Material material = switch (slot) {
                case 10 -> Material.NETHER_STAR;
                case 12 -> Material.BOOK;
                case 14, 16 -> Material.DIAMOND;
                default -> Material.BARRIER;
            };
            String[] stringArray = new String[1];
            stringArray[0] = switch (slot) {
                case 10 -> "§7Fantasy MBA franchises";
                case 12 -> "§7College divisions";
                case 14, 16 -> "§7VIP only";
                default -> "";
            };
            btn[n] = new ItemButton(n2, Items.get(component, material, 1, stringArray), p -> action.run());
        };
        put.accept(10, () -> PagedTeamGUI.open(viewer, PagedTeamGUI.Category.MBA, editingHome, teamChosen));
        put.accept(12, () -> DivisionGUI.open(viewer, editingHome, teamChosen));
        put.accept(14, () -> {
            if (!viewer.hasPermission("rank.vip")) {
                viewer.sendMessage("§cVIP rank required to use NBA teams.");
                viewer.playSound(viewer.getLocation(), Sound.BLOCK_ANVIL_FALL, SoundCategory.MASTER, 1.0f, 1.0f);
                return;
            }
            PagedTeamGUI.open(viewer, PagedTeamGUI.Category.NBA, editingHome, teamChosen);
        });
        put.accept(16, () -> {
            if (!viewer.hasPermission("rank.vip")) {
                viewer.sendMessage("§cVIP rank required to use Retro teams.");
                viewer.playSound(viewer.getLocation(), Sound.BLOCK_ANVIL_FALL, SoundCategory.MASTER, 1.0f, 1.0f);
                return;
            }
            PagedTeamGUI.open(viewer, PagedTeamGUI.Category.RETRO, editingHome, teamChosen);
        });
        new GUI("Select Team Folder", 3, false, btn).openInventory(viewer);
    }
}

