/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.TextComponent
 *  net.kyori.adventure.text.format.TextColor
 *  net.kyori.adventure.text.format.TextDecoration
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 */
package me.x_tias.partix.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import me.x_tias.partix.mini.basketball.BasketballGame;
import me.x_tias.partix.mini.game.PlayerStats;
import me.x_tias.partix.mini.game.PlayerStatsManager;
import me.x_tias.partix.plugin.athlete.Athlete;
import me.x_tias.partix.plugin.athlete.AthleteManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

@CommandAlias(value = "stats")
public class StatsCommand
        extends BaseCommand {
    @Default
    public void onStats(Player sender) {
        Component line;
        int fantasyPoints;
        int defDR;
        double defScore;
        double denominator;
        double numerator;
        int oppThrees;
        int oppPoints;
        int missedShots;
        int oppPossTimeSec;
        int oppThreeMade;
        int oppFGAtt;
        int oppFGMade;
        PlayerStats oppStats;
        String defName;
        UUID defOpp;
        String defendedInfo;
        double assistsPerPass;
        double threePTRate;
        double eFGPct;
        double threeFGPct;
        double totalFGPct;
        int threeAtt;
        int threeMade;
        int fgAtt;
        int fgMade;
        int passAttempts;
        int possTimeSec;
        int turnovers;
        int steals;
        int rebounds;
        int assists;
        int points;
        PlayerStats s;
        UUID uuid;
        Athlete ath = AthleteManager.get(sender.getUniqueId());
        if (ath == null) {
            sender.sendMessage(Component.text("§cYou’re not enrolled as an athlete in any game."));
            return;
        }
        if (!(ath.getPlace() instanceof BasketballGame game)) {
            sender.sendMessage(Component.text("§cYou’re not currently in a basketball match."));
            return;
        }
        PlayerStatsManager mgr = game.getStatsManager();
        sender.sendMessage(Component.text("§6=== Live Box Score ==="));
        sender.sendMessage(Component.text("§e– Home Team –"));
        for (Player p : game.getHomePlayers()) {
            uuid = p.getUniqueId();
            s = mgr.getPlayerStats(uuid);
            points = s.getPoints();
            assists = s.getAssists();
            rebounds = s.getRebounds();
            steals = s.getSteals();
            turnovers = s.getTurnovers();
            possTimeSec = (int) (s.getPossessionTime() / 1000L);
            passAttempts = s.getPassAttempts();
            fgMade = s.getFGMade();
            fgAtt = s.getFGAttempted();
            threeMade = s.get3FGMade();
            threeAtt = s.get3FGAttempted();
            totalFGPct = fgAtt == 0 ? 0.0 : (double) fgMade / (double) fgAtt * 100.0;
            threeFGPct = threeAtt == 0 ? 0.0 : (double) threeMade / (double) threeAtt * 100.0;
            eFGPct = fgAtt == 0 ? 0.0 : ((double) fgMade + 0.5 * (double) threeMade) / (double) fgAtt * 100.0;
            threePTRate = fgAtt == 0 ? 0.0 : (double) threeAtt / (double) fgAtt * 100.0;
            assistsPerPass = passAttempts > 0 ? (double) assists / (double) passAttempts : 0.0;
            defendedInfo = "None";
            defOpp = s.getTopContestedOpponent();
            if (defOpp != null) {
                Player defendedPlayer = Bukkit.getPlayer(defOpp);
                defName = defendedPlayer != null ? defendedPlayer.getName() : defOpp.toString();
                oppStats = mgr.getPlayerStats(defOpp);
                if (oppStats != null) {
                    oppFGMade = oppStats.getFGMade();
                    oppFGAtt = oppStats.getFGAttempted();
                    oppThreeMade = oppStats.get3FGMade();
                    oppPossTimeSec = (int) (oppStats.getPossessionTime() / 1000L);
                    missedShots = oppFGAtt - oppFGMade;
                    oppPoints = oppStats.getPoints();
                    oppThrees = oppStats.get3FGMade();
                    numerator = (double) missedShots + (double) oppPossTimeSec / 24.0;
                    denominator = oppPoints + oppThrees;
                    defScore = 0.0;
                    if (denominator > 0.0) {
                        defScore = 100.0 * numerator / denominator;
                    }
                    defDR = (int) Math.round(defScore);
                    defendedInfo = String.format("%s | DR: %d", defName, defDR);
                } else {
                    defendedInfo = defName;
                }
            }
            fantasyPoints = points + fgMade * 2 - fgAtt + threeMade + rebounds + assists * 2 + steals * 4 - turnovers * 2;
            line = Component.empty().append(Component.text(p.getName() + " | ").decorate(TextDecoration.BOLD).color(TextColor.color(43775))).append(Component.text("Points: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text("" + points).color(TextColor.color(65280))).append(Component.text(" | Assists: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text("" + assists).color(TextColor.color(65280))).append(Component.text(" | Rebounds: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text("" + rebounds).color(TextColor.color(65280))).append(Component.text(" | Steals: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text("" + steals).color(TextColor.color(65280))).append(Component.text(" | Turnovers: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text("" + turnovers).color(TextColor.color(65280))).append(Component.text(" | Possession: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text(possTimeSec + " sec").color(TextColor.color(65280))).append(Component.text(" | Defended By: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text(defendedInfo).color(TextColor.color(65280))).append(Component.text(" | Pass Attempts: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text("" + passAttempts).color(TextColor.color(65280))).append(Component.text(" | FG: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text(fgMade + "/" + fgAtt).color(TextColor.color(65280))).append(Component.text(" | 3FG: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text(threeMade + "/" + threeAtt).color(TextColor.color(65280))).append(Component.text(" | FG%: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text(String.format("%.1f", totalFGPct)).color(TextColor.color(65280))).append(Component.text(" | 3FG%: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text(String.format("%.1f", threeFGPct)).color(TextColor.color(65280))).append(Component.text(" | eFG%: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text(String.format("%.1f", eFGPct)).color(TextColor.color(65280))).append(Component.text(" | 3PT Rate: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text(String.format("%.1f%%", threePTRate)).color(TextColor.color(65280))).append(Component.text(" | Assists/Pass: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text(String.format("%.1f", assistsPerPass)).color(TextColor.color(65280))).append(Component.text(" | Fantasy Points: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text("" + fantasyPoints).color(TextColor.color(65280)));
            sender.sendMessage(line);
        }
        sender.sendMessage(Component.text("§e– Away Team –"));
        for (Player p : game.getAwayPlayers()) {
            uuid = p.getUniqueId();
            s = mgr.getPlayerStats(uuid);
            points = s.getPoints();
            assists = s.getAssists();
            rebounds = s.getRebounds();
            steals = s.getSteals();
            turnovers = s.getTurnovers();
            possTimeSec = (int) (s.getPossessionTime() / 1000L);
            passAttempts = s.getPassAttempts();
            fgMade = s.getFGMade();
            fgAtt = s.getFGAttempted();
            threeMade = s.get3FGMade();
            threeAtt = s.get3FGAttempted();
            totalFGPct = fgAtt == 0 ? 0.0 : (double) fgMade / (double) fgAtt * 100.0;
            threeFGPct = threeAtt == 0 ? 0.0 : (double) threeMade / (double) threeAtt * 100.0;
            eFGPct = fgAtt == 0 ? 0.0 : ((double) fgMade + 0.5 * (double) threeMade) / (double) fgAtt * 100.0;
            threePTRate = fgAtt == 0 ? 0.0 : (double) threeAtt / (double) fgAtt * 100.0;
            assistsPerPass = passAttempts > 0 ? (double) assists / (double) passAttempts : 0.0;
            defendedInfo = "None";
            defOpp = s.getTopContestedOpponent();
            if (defOpp != null) {
                Player defendedPlayer = Bukkit.getPlayer(defOpp);
                defName = defendedPlayer != null ? defendedPlayer.getName() : defOpp.toString();
                oppStats = mgr.getPlayerStats(defOpp);
                if (oppStats != null) {
                    oppFGMade = oppStats.getFGMade();
                    oppFGAtt = oppStats.getFGAttempted();
                    oppThreeMade = oppStats.get3FGMade();
                    oppPossTimeSec = (int) (oppStats.getPossessionTime() / 1000L);
                    missedShots = oppFGAtt - oppFGMade;
                    oppPoints = oppStats.getPoints();
                    oppThrees = oppStats.get3FGMade();
                    numerator = (double) missedShots + (double) oppPossTimeSec / 24.0;
                    denominator = oppPoints + oppThrees;
                    defScore = 0.0;
                    if (denominator > 0.0) {
                        defScore = 100.0 * numerator / denominator;
                    }
                    defDR = (int) Math.round(defScore);
                    defendedInfo = String.format("%s | DR: %d", defName, defDR);
                } else {
                    defendedInfo = defName;
                }
            }
            fantasyPoints = points + fgMade * 2 - fgAtt + threeMade + rebounds + assists * 2 + steals * 4 - turnovers * 2;
            line = Component.empty().append(Component.text(p.getName() + " | ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFAA00))).append(Component.text("Points: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text("" + points).color(TextColor.color(65280))).append(Component.text(" | Assists: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text("" + assists).color(TextColor.color(65280))).append(Component.text(" | Rebounds: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text("" + rebounds).color(TextColor.color(65280))).append(Component.text(" | Steals: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text("" + steals).color(TextColor.color(65280))).append(Component.text(" | Turnovers: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text("" + turnovers).color(TextColor.color(65280))).append(Component.text(" | Possession: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text(possTimeSec + " sec").color(TextColor.color(65280))).append(Component.text(" | Defended By: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text(defendedInfo).color(TextColor.color(65280))).append(Component.text(" | Pass Attempts: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text("" + passAttempts).color(TextColor.color(65280))).append(Component.text(" | FG: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text(fgMade + "/" + fgAtt).color(TextColor.color(65280))).append(Component.text(" | 3FG: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text(threeMade + "/" + threeAtt).color(TextColor.color(65280))).append(Component.text(" | FG%: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text(String.format("%.1f", totalFGPct)).color(TextColor.color(65280))).append(Component.text(" | 3FG%: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text(String.format("%.1f", threeFGPct)).color(TextColor.color(65280))).append(Component.text(" | eFG%: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text(String.format("%.1f", eFGPct)).color(TextColor.color(65280))).append(Component.text(" | 3PT Rate: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text(String.format("%.1f%%", threePTRate)).color(TextColor.color(65280))).append(Component.text(" | Assists/Pass: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text(String.format("%.1f", assistsPerPass)).color(TextColor.color(65280))).append(Component.text(" | Fantasy Points: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text("" + fantasyPoints).color(TextColor.color(65280)));
            sender.sendMessage(line);
        }
        sender.sendMessage(Component.text("§6======================"));
    }
}

