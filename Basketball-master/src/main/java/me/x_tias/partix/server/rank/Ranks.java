package me.x_tias.partix.server.rank;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class Ranks {
    @Getter
    private static Scoreboard scoreboard;

    // Staff Ranks
    @Getter
    private static Team admin;
    @Getter
    private static Team mod;
    @Getter
    private static Team media;

    // Special Roles
    @Getter
    private static Team coach;
    @Getter
    private static Team referee;

    // Subscription Ranks
    @Getter
    private static Team pro;
    @Getter
    private static Team vip;

    // MBA Team Ranks
    @Getter
    private static Team washingtonWithers;
    @Getter
    private static Team philadelphia64s;
    @Getter
    private static Team chicagoBows;
    @Getter
    private static Team brooklynBuckets;
    @Getter
    private static Team miamiMagmaCubes;
    @Getter
    private static Team atlantaAllays;
    @Getter
    private static Team laCreepers;
    @Getter
    private static Team bostonBreeze;

    // Default
    private static Team def;

    public static void setup(Scoreboard s) {
        scoreboard = s;

        // Staff Ranks (highest priority - 'a' prefix)
        admin = createTeam("aaa_admin", NamedTextColor.DARK_RED, "§c§lADMIN §r");
        mod = createTeam("bbb_mod", NamedTextColor.BLUE, "§9§lMOD §r");
        media = createTeam("ccc_media", NamedTextColor.LIGHT_PURPLE, "§d§lMEDIA §r");

        // Special Roles (second priority - 'd' prefix)
        coach = createTeam("ddd_coach", NamedTextColor.RED, "§c: §lCOACH §r");
        referee = createTeam("eee_referee", NamedTextColor.BLACK, "§0: §lREFEREE §r");

        // Subscription Ranks (third priority - 'f' prefix)
        pro = createTeam("fff_pro", NamedTextColor.GOLD, "§6§lPRO §r");
        vip = createTeam("ggg_vip", NamedTextColor.GREEN, "§a§lVIP §r");

        // MBA Teams (lower priority - 'h' prefix for teams)
        washingtonWithers = createTeam("hhh_washingtonwithers", NamedTextColor.BLUE, "§l§9: WASHINGTON §cWITHERS §r");
        philadelphia64s = createTeam("hhh_philadelphia64s", NamedTextColor.RED, "§l§c: PHILADELPHIA §964S §r");
        chicagoBows = createTeam("hhh_chicagobows", NamedTextColor.RED, "§l§c: CHICAGO §0BOWS §r");
        brooklynBuckets = createTeam("hhh_brooklynbuckets", NamedTextColor.BLACK, "§l§0: BROOKLYN §fBUCKETS §r");
        miamiMagmaCubes = createTeam("hhh_miamimagmacubes", NamedTextColor.DARK_RED, "§l§4: MIAMI §6MAGMA CUBES §r");
        atlantaAllays = createTeam("hhh_atlantaallays", NamedTextColor.RED, "§l§c: ATLANTA §9ALLAYS §r");
        laCreepers = createTeam("hhh_lacreepers", NamedTextColor.DARK_PURPLE, "§l§5: LA §6CREEPERS §r");
        bostonBreeze = createTeam("hhh_bostonbreeze", NamedTextColor.GREEN, "§l§a: BOSTON §fBREEZE §r");


        // Default (lowest priority)
        def = createTeam("iii_default", NamedTextColor.YELLOW, "§e: §lFREE AGENT §r");
    }

    public static Team getDefault() {
        return def;
    }

    // Helper method to get team by name (useful for commands)
    public static Team getTeamByName(String teamName) {
        return switch (teamName.toLowerCase()) {
            case "admin" -> admin;
            case "mod" -> mod;
            case "media" -> media;
            case "coach" -> coach;
            case "referee", "ref" -> referee;
            case "pro" -> pro;
            case "vip" -> vip;
            case "washingtonwithers" -> washingtonWithers;
            case "philadelphia64s" -> philadelphia64s;
            case "chicagobows" -> chicagoBows;
            case "brooklynbuckets" -> brooklynBuckets;
            case "miamimagmacubes" -> miamiMagmaCubes;
            case "atlantaallays" -> atlantaAllays;
            case "lacreepers" -> laCreepers;
            case "bostonbreeze" -> bostonBreeze;
            default -> def;
        };
    }

    private static Team createTeam(String name, NamedTextColor color, String prefix) {
        Team team = scoreboard.getTeam(name) != null ? scoreboard.getTeam(name) : scoreboard.registerNewTeam(name);
        if (team != null) {
            team.color(color);
            team.prefix(Component.text(prefix));
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
            team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER); // Prevent player collision
        }
        return team;
    }
}