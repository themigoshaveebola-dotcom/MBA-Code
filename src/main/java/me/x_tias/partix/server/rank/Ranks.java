/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.format.NamedTextColor
 *  org.bukkit.scoreboard.Scoreboard
 *  org.bukkit.scoreboard.Team
 *  org.bukkit.scoreboard.Team$Option
 *  org.bukkit.scoreboard.Team$OptionStatus
 */
package me.x_tias.partix.server.rank;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class Ranks {
    @Getter
    private static Scoreboard scoreboard;
    @Getter
    private static Team admin;
    @Getter
    private static Team mod;
    @Getter
    private static Team media;
    @Getter
    private static Team pro;
    @Getter
    private static Team vip;
    private static Team def;

    public static void setup(Scoreboard s) {
        scoreboard = s;
        admin = Ranks.createTeam("aaa_admin", NamedTextColor.DARK_RED, "§cADMIN §f");
        mod = Ranks.createTeam("bbb_mod", NamedTextColor.BLUE, "§9Mod §f");
        media = Ranks.createTeam("ccc_media", NamedTextColor.LIGHT_PURPLE, "§dMedia §f");
        pro = Ranks.createTeam("ddd_pro", NamedTextColor.GOLD, "§6PRO §f");
        vip = Ranks.createTeam("eee_vip", NamedTextColor.GREEN, "§aVIP §f");
        def = Ranks.createTeam("fff_default", NamedTextColor.GRAY, "§7");
    }

    public static Team getDefault() {
        return def;
    }

    private static Team createTeam(String name, NamedTextColor color, String prefix) {
        Team team = scoreboard.getTeam(name) != null ? scoreboard.getTeam(name) : scoreboard.registerNewTeam(name);
        if (team != null) {
            team.color(color);
            team.prefix(Component.text(prefix));
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
        }
        return team;
    }
}

