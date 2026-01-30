/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  org.bukkit.Material
 *  org.bukkit.entity.Player
 */
package me.x_tias.partix.plugin.team.gui;

import me.x_tias.partix.plugin.gui.GUI;
import me.x_tias.partix.plugin.gui.ItemButton;
import me.x_tias.partix.plugin.team.BaseTeam;
import me.x_tias.partix.plugin.team.mba.*;
import me.x_tias.partix.plugin.team.nba.*;
import me.x_tias.partix.plugin.team.retro.TeamSupersonics;
import me.x_tias.partix.util.Colour;
import me.x_tias.partix.util.Items;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public final class PagedTeamGUI {
    public static void open(Player p, Category cat, boolean editingHome, Consumer<BaseTeam> callback) {
        List<BaseTeam> pool = switch (cat) {
            case Category.MBA -> PagedTeamGUI.mbaTeams();
            case Category.NBA -> PagedTeamGUI.nbaTeams();
            case Category.RETRO -> PagedTeamGUI.retroTeams();
        };
        PagedTeamGUI.openPage(p, "§8" + cat.name() + " Teams", pool, 0, editingHome, callback);
    }

    static void openDivisionPage(Player p, String div, int page, boolean editingHome, Consumer<BaseTeam> callback) {
        PagedTeamGUI.openPage(p, "§8" + div + " Teams", DivisionGUI.teamsOf(div), page, editingHome, callback);
    }

    private static void openPage(Player viewer, String title, List<BaseTeam> teams, int page, boolean editingHome, Consumer<BaseTeam> callback) {
        int pageSize = 28;
        int pages = (teams.size() - 1) / 28 + 1;
        int from = page * 28;
        int to = Math.min(from + 28, teams.size());
        List<BaseTeam> sub = teams.subList(from, to);
        Object[] btn = new ItemButton[54];
        Arrays.fill(btn, new ItemButton(0, GuiUtil.FILLER, p -> {
        }));
        int[] slots = new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
        for (int i = 0; i < sub.size(); ++i) {
            BaseTeam t = sub.get(i);
            btn[slots[i]] = new ItemButton(slots[i], t.block, pl -> {
                callback.accept(t);
                pl.closeInventory();
            });
        }
        if (page > 0) {
            btn[45] = PagedTeamGUI.nav(45, Material.ARROW, "Previous", p -> PagedTeamGUI.openPage(p, title, teams, page - 1, editingHome, callback));
        }
        if (page + 1 < pages) {
            btn[53] = PagedTeamGUI.nav(53, Material.ARROW, "Next", p -> PagedTeamGUI.openPage(p, title, teams, page + 1, editingHome, callback));
        }
        new GUI(title + "  §7(" + (page + 1) + "/" + pages + ")", 6, false, (ItemButton[]) btn).openInventory(viewer);
    }

    private static ItemButton nav(int slot, Material mat, String name, Consumer<Player> click) {
        return new ItemButton(slot, Items.get(Component.text(name).color(Colour.partix()), mat), click);
    }

    private static List<BaseTeam> mbaTeams() {
        return List.of(new Team64s(), new TeamAllays(), new TeamArrows(), new TeamMinecarts(), new TeamBows(), new TeamBreeze(), new TeamBuckets(), new TeamChickens(), new TeamCreepers(), new TeamDiamonds(), new TeamDiscs(), new TeamDragons(), new TeamFireworks(), new TeamGolems(), new TeamGuardians(), new TeamIngots(), new TeamMagma(), new TeamMending(), new TeamMooshrooms(), new TeamNetherite(), new TeamOaks(), new TeamOcelots(), new TeamOres(), new TeamPhantoms(), new TeamPortals(), new TeamShulkers(), new TeamStriders(), new TeamSwords(), new TeamTotems(), new TeamWithers());
    }

    private static List<BaseTeam> nbaTeams() {
        return List.of(new TeamHawks(), new TeamCeltics(), new TeamHornets(), new TeamNets(), new TeamBulls(), new TeamCavaliers(), new TeamMavericks(), new TeamNuggets(), new TeamPistons(), new TeamWarriors(), new TeamRockets(), new TeamPacers(), new TeamClippers(), new TeamLakers(), new TeamGrizzlies(), new TeamHeat(), new TeamBucks(), new TeamTimberwolves(), new TeamPelicans(), new TeamKnicks(), new TeamThunder(), new TeamMagic(), new Team76ers(), new TeamSuns(), new TeamTrailBlazers(), new TeamKings(), new TeamSpurs(), new TeamRaptors(), new TeamJazz(), new TeamWizards());
    }

    private static List<BaseTeam> retroTeams() {
        return List.of(new TeamSupersonics());
    }

    public enum Category {
        MBA,
        NBA,
        RETRO

    }
}

