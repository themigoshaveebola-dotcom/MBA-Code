/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.World
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 *  org.bukkit.scheduler.BukkitRunnable
 */
package me.x_tias.partix.proam;

import me.x_tias.partix.Partix;
import me.x_tias.partix.mini.basketball.BasketballGame;
import me.x_tias.partix.mini.factories.Hub;
import me.x_tias.partix.mini.game.GoalGame;
import me.x_tias.partix.plugin.athlete.AthleteManager;
import me.x_tias.partix.plugin.settings.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class ProAmGameManager {
    private static final Location PRO_AM_SPAWN = new Location(Bukkit.getWorlds().getFirst(), -273.5, -60.0, 74.5);
    private static final Location COURT_3V3 = new Location(Bukkit.getWorlds().getFirst(), -176.5, -60.0, 23.5);
    private static final Location COURT_4V4 = new Location(Bukkit.getWorlds().getFirst(), -273.5, -60.0, 84.5);
    private static final List<Location> COURTS_3V3 = List.of(new Location(Bukkit.getWorlds().getFirst(), -176.5, -60.0, 23.5));
    private static final List<Location> COURTS_4V4 = List.of(new Location(Bukkit.getWorlds().getFirst(), -273.5, -60.0, 84.5));
    private static final Set<Location> activeCourts = new HashSet<>();
    private final ProAmManager proAmManager;

    public ProAmGameManager(JavaPlugin plugin, ProAmManager proAmManager) {
        this.proAmManager = proAmManager;
    }

    public void joinQueue(ProAmTeam team, int teamSize, Player player) {
        team.addToQueue(teamSize, player.getUniqueId());
        Bukkit.getLogger().info("Player " + player.getName() + " joined " + teamSize + "v" + teamSize + " queue for team " + team.getName());
        for (Player p : team.getOnlineMembers()) {
            p.sendMessage("Player " + player.getName() + " joined the " + teamSize + "v" + teamSize + " queue.");
        }
    }

    public void cancelQueue(ProAmTeam team, int teamSize, Player player) {
        team.removeFromQueue(teamSize, player.getUniqueId());
        Bukkit.getLogger().info("Player " + player.getName() + " left " + teamSize + "v" + teamSize + " queue for team " + team.getName());
        for (Player p : team.getOnlineMembers()) {
            p.sendMessage("Player " + player.getName() + " left the " + teamSize + "v" + teamSize + " queue.");
        }
    }

    public void confirmQueue(ProAmTeam team, int teamSize) {
        Map<Integer, List<ProAmTeam>> qTeams = this.proAmManager.getQueuedTeams();
        qTeams.computeIfAbsent(teamSize, k -> new ArrayList<>());
        if (!qTeams.get(teamSize).contains(team)) {
            qTeams.get(teamSize).add(team);
        }
        for (Player p : team.getOnlineMembers()) {
            p.sendMessage("Your team is now queued for a " + teamSize + "v" + teamSize + " match.");
        }
        if (qTeams.get(teamSize).size() >= 2) {
            ProAmTeam team1 = qTeams.get(teamSize).get(0);
            ProAmTeam team2 = qTeams.get(teamSize).get(1);
            qTeams.get(teamSize).remove(team1);
            qTeams.get(teamSize).remove(team2);
            this.startMatch(team1, team2, teamSize);
        }
    }

    public void startMatch(ProAmTeam team1, ProAmTeam team2, final int teamSize) {
        ArrayList<Location> availableCourts = teamSize == 3 ? new ArrayList<>(COURTS_3V3) : new ArrayList<>(COURTS_4V4);
        availableCourts.removeIf(activeCourts::contains);
        final Location courtLocation = !availableCourts.isEmpty() ? availableCourts.get(new Random().nextInt(availableCourts.size())) : (teamSize == 3 ? COURTS_3V3.getFirst() : COURTS_4V4.getFirst());
        activeCourts.add(courtLocation);
        Settings settings = new Settings(WinType.TIME_5, GameType.AUTOMATIC, WaitType.MEDIUM, CompType.RANKED, teamSize, false, false, false, 2, GameEffectType.NONE);
        final BasketballGame game = teamSize == 3 ? new BasketballGame(settings, courtLocation, 26.0, 2.8, 0.45, 0.475, 0.575) : new BasketballGame(settings, courtLocation, 30.0, 2.8, 0.45, 0.475, 0.575);
        game.setCustomProperty("proam", true);
        Location homeSpawn = courtLocation.clone().add(5.0, 0.0, 0.0);
        Location awaySpawn = courtLocation.clone().add(-5.0, 0.0, 0.0);
        List<Player> homePlayers = team1.getOnlineMembers();
        for (Player p : homePlayers) {
            game.join(AthleteManager.get(p.getUniqueId()));
            game.joinTeam(p, GoalGame.Team.HOME);
            p.teleport(homeSpawn);
        }
        List<Player> awayPlayers = team2.getOnlineMembers();
        for (Player p : awayPlayers) {
            game.join(AthleteManager.get(p.getUniqueId()));
            game.joinTeam(p, GoalGame.Team.AWAY);
            p.teleport(awaySpawn);
        }
        game.setTeamName(GoalGame.Team.HOME, team1.getName());
        game.setTeamJerseys(GoalGame.Team.HOME, team1.getChestplateColor(), team1.getLeggingsColor(), team1.getBootsColor());
        game.setTeamName(GoalGame.Team.AWAY, team2.getName());
        game.setTeamJerseys(GoalGame.Team.AWAY, "WHITE", team2.getLeggingsColor(), team2.getBootsColor());
        game.startCountdown(GoalGame.State.FACEOFF, 10);
        new BukkitRunnable() {

            public void run() {
                game.onTick();
                if (game.getState() == GoalGame.State.FINAL) {
                    GoalGame.Team winner = game.getHomeScore() > game.getAwayScore() ? GoalGame.Team.HOME : GoalGame.Team.AWAY;
                    ProAmGameManager.this.handleProAmGameEnd(game, winner, teamSize);
                    activeCourts.remove(courtLocation);
                    this.cancel();
                }
            }
        }.runTaskTimer(Partix.getInstance(), 0L, 1L);
    }

    private void handleProAmGameEnd(BasketballGame game, GoalGame.Team winner, int teamSize) {
        int homeScore = game.getHomeScore();
        int awayScore = game.getAwayScore();
        int margin = Math.abs(homeScore - awayScore);
        double K = 30.0;
        List<Player> homePlayers = game.getHomePlayers();
        List<Player> awayPlayers = game.getAwayPlayers();
        if (homePlayers.isEmpty() || awayPlayers.isEmpty()) {
            return;
        }
        ProAmTeam teamHome = this.proAmManager.getTeam(homePlayers.getFirst());
        ProAmTeam teamAway = this.proAmManager.getTeam(awayPlayers.getFirst());
        if (teamHome == null || teamAway == null) {
            return;
        }
        int eloHome = teamHome.getElo();
        int eloAway = teamAway.getElo();
        double expectedHome = 1.0 / (1.0 + Math.pow(10.0, (double) (eloAway - eloHome) / 400.0));
        double expectedAway = 1.0 / (1.0 + Math.pow(10.0, (double) (eloHome - eloAway) / 400.0));
        double actualHome = winner == GoalGame.Team.HOME ? 1.0 : 0.0;
        double newEloHome = Math.round((double) eloHome + K * (actualHome - expectedHome) * (1.0 + (double) margin / 10.0));
        double newEloAway = Math.round((double) eloAway + K * (1.0 - actualHome - expectedAway) * (1.0 + (double) margin / 10.0));
        teamHome.setElo((int) newEloHome);
        teamAway.setElo((int) newEloAway);
        teamHome.setQueued(false);
        teamAway.setQueued(false);
        teamHome.clearQueue(1);
        teamHome.clearQueue(3);
        teamHome.clearQueue(4);
        teamAway.clearQueue(1);
        teamAway.clearQueue(3);
        teamAway.clearQueue(4);
        Location proAmLobby = new Location(Bukkit.getWorlds().getFirst(), -125.5, -60.0, 29.5);
        for (Player p : homePlayers) {
            p.sendMessage("§aYour team’s new ELO: " + newEloHome);
            Hub.hub.join(AthleteManager.get(p.getUniqueId()));
            p.teleport(proAmLobby);
            p.closeInventory();
        }
        for (Player p : awayPlayers) {
            p.sendMessage("§aYour team’s new ELO: " + newEloAway);
            Hub.hub.join(AthleteManager.get(p.getUniqueId()));
            p.teleport(proAmLobby);
            p.closeInventory();
        }
        Location usedCourt = (Location) game.getCustomProperty("courtLocation");
        if (usedCourt != null) {
            activeCourts.remove(usedCourt);
        }
        this.proAmManager.saveTeams();
    }

    private void teleportToProAmLobby(Player player) {
        Location proAmLobby = new Location(Bukkit.getWorlds().getFirst(), -125.5, -60.0, 29.5);
        Hub.hub.join(AthleteManager.get(player.getUniqueId()));
        player.sendMessage("§cYou have left Pro Am mode and returned to the lobby!");
        player.teleport(proAmLobby);
        player.closeInventory();
    }

    public void cancelTeamQueue(ProAmTeam team, Player player) {
        if (!team.isLeader(player.getUniqueId())) {
            team.getQueuedPlayers(3).remove(player.getUniqueId());
            team.getQueuedPlayers(4).remove(player.getUniqueId());
            player.sendMessage("§cYou have been removed from the queue.");
        } else {
            team.clearQueue(3);
            team.clearQueue(4);
            team.setQueued(false);
            this.proAmManager.getQueuedTeams().get(3).remove(team);
            this.proAmManager.getQueuedTeams().get(4).remove(team);
            player.sendMessage("§cYour team's queued status has been canceled.");
            for (Player p : team.getOnlineMembers()) {
                p.sendMessage("§cTeam queue canceled by owner.");
            }
        }
    }
}

