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
import me.x_tias.partix.plugin.team.mcaa.TeamDuke;
import me.x_tias.partix.plugin.team.mcaa.acc.*;
import me.x_tias.partix.plugin.team.mcaa.big10.*;
import me.x_tias.partix.plugin.team.mcaa.big12.*;
import me.x_tias.partix.plugin.team.mcaa.bigsky.*;
import me.x_tias.partix.plugin.team.mcaa.sec.TeamAlabama;
import me.x_tias.partix.plugin.team.mcaa.sec.TeamGeorgia;
import me.x_tias.partix.util.Colour;
import me.x_tias.partix.util.Items;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class DivisionGUI {
    private static final Map<String, List<BaseTeam>> DIVISIONS = new LinkedHashMap<>();

    static {
        DIVISIONS.put("SEC", List.of(new TeamAlabama(), new TeamGeorgia()));
        DIVISIONS.put("BIG‑10", List.of(new TeamIndiana(), new TeamIllinois(), new TeamIowa(), new TeamMaryland(), new TeamMichigan(), new TeamMichiganState(), new TeamMinnesota(), new TeamNebraska(), new TeamNorthwestern(), new TeamOhioState(), new TeamPennState(), new TeamPurdue(), new TeamRutgers(), new TeamWisconsin(), new TeamOregon(), new TeamUCLA(), new TeamUSC(), new TeamWashington()));
        DIVISIONS.put("ACC", List.of(new TeamBostonCollege(), new TeamCalifornia(), new TeamClemson(), new TeamDuke(), new TeamFloridaState(), new TeamGeorgiaTech(), new TeamLouisville(), new TeamMiami(), new TeamNorthCarolina(), new TeamNCState(), new TeamNotreDame(), new TeamPittsburgh(), new TeamStanford(), new TeamSyracuse(), new TeamVirginia(), new TeamVirginiaTech(), new TeamWakeForest(), new TeamSMU()));
        DIVISIONS.put("BIG‑12", List.of(new TeamHouston(), new TeamTexasTech(), new TeamArizona(), new TeamBYU(), new TeamIowaState(), new TeamKansas(), new TeamBaylor(), new TeamWestVirginia(), new TeamTCU(), new TeamKansasState(), new TeamUtah(), new TeamOklahomaState(), new TeamCincinnati(), new TeamUCF(), new TeamArizonaState(), new TeamColorado()));
        DIVISIONS.put("BIG‑SKY", List.of(new TeamEasternWashington(), new TeamIdaho(), new TeamIdahoState(), new TeamMontana(), new TeamMontanaState(), new TeamNorthernArizona(), new TeamNorthernColorado(), new TeamPortlandState(), new TeamSacramentoState(), new TeamWeberState()));
    }

    public static void open(Player viewer, boolean editingHome, Consumer<BaseTeam> callback) {
        int[] LAYOUT = new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
        Object[] btn = new ItemButton[54];
        Arrays.fill(btn, new ItemButton(0, GuiUtil.FILLER, p -> {
        }));
        int i = 0;
        for (Map.Entry<String, List<BaseTeam>> entry : DIVISIONS.entrySet()) {
            if (i >= LAYOUT.length) break;
            int slot = LAYOUT[i++];
            String divName = entry.getKey();
            List<BaseTeam> teamBucket = entry.getValue();
            btn[slot] = new ItemButton(slot, Items.get(Component.text(divName).color(Colour.partix()), Material.BOOK, 1, "§7" + teamBucket.size() + " teams"), p -> PagedTeamGUI.openDivisionPage(p, divName, 0, editingHome, callback));
        }
        new GUI("MCAA Divisions", 3, false, (ItemButton[]) btn).openInventory(viewer);
    }

    static List<BaseTeam> teamsOf(String div) {
        return DIVISIONS.get(div);
    }
}

