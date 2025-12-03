/*
 * Decompiled with CFR 0.152.
 */
package me.x_tias.partix.plugin.team;

import me.x_tias.partix.plugin.gui.GUI;
import me.x_tias.partix.plugin.gui.ItemButton;
import me.x_tias.partix.plugin.team.nba.*;

import java.util.function.Consumer;

public class TeamGUI {
    public static GUI get(String team, Consumer<BaseTeam> run) {
        return new GUI("Select " + team + " Team: ", 6, false, new ItemButton(0, new TeamHawks().block, player -> run.accept(new TeamHawks())), new ItemButton(1, new TeamCeltics().block, player -> run.accept(new TeamCeltics())), new ItemButton(2, new TeamHornets().block, player -> run.accept(new TeamHornets())), new ItemButton(3, new TeamNets().block, player -> run.accept(new TeamNets())), new ItemButton(4, new TeamBulls().block, player -> run.accept(new TeamBulls())), new ItemButton(5, new TeamCavaliers().block, player -> run.accept(new TeamCavaliers())), new ItemButton(6, new TeamMavericks().block, player -> run.accept(new TeamMavericks())), new ItemButton(7, new TeamNuggets().block, player -> run.accept(new TeamNuggets())), new ItemButton(8, new TeamPistons().block, player -> run.accept(new TeamPistons())), new ItemButton(9, new TeamWarriors().block, player -> run.accept(new TeamWarriors())), new ItemButton(10, new TeamRockets().block, player -> run.accept(new TeamRockets())), new ItemButton(11, new TeamPacers().block, player -> run.accept(new TeamPacers())), new ItemButton(12, new TeamClippers().block, player -> run.accept(new TeamClippers())), new ItemButton(13, new TeamLakers().block, player -> run.accept(new TeamLakers())), new ItemButton(14, new TeamGrizzlies().block, player -> run.accept(new TeamGrizzlies())), new ItemButton(15, new TeamHeat().block, player -> run.accept(new TeamHeat())), new ItemButton(16, new TeamBucks().block, player -> run.accept(new TeamBucks())), new ItemButton(17, new TeamTimberwolves().block, player -> run.accept(new TeamTimberwolves())), new ItemButton(18, new TeamPelicans().block, player -> run.accept(new TeamPelicans())), new ItemButton(19, new TeamKnicks().block, player -> run.accept(new TeamKnicks())), new ItemButton(20, new TeamThunder().block, player -> run.accept(new TeamThunder())), new ItemButton(21, new TeamMagic().block, player -> run.accept(new TeamMagic())), new ItemButton(22, new Team76ers().block, player -> run.accept(new Team76ers())), new ItemButton(23, new TeamSuns().block, player -> run.accept(new TeamSuns())), new ItemButton(24, new TeamTrailBlazers().block, player -> run.accept(new TeamTrailBlazers())), new ItemButton(25, new TeamKings().block, player -> run.accept(new TeamKings())), new ItemButton(26, new TeamSpurs().block, player -> run.accept(new TeamSpurs())), new ItemButton(27, new TeamRaptors().block, player -> run.accept(new TeamRaptors())), new ItemButton(28, new TeamJazz().block, player -> run.accept(new TeamJazz())), new ItemButton(29, new TeamWizards().block, player -> run.accept(new TeamWizards())));
    }
}

