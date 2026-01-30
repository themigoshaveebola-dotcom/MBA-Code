package me.x_tias.partix.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import me.x_tias.partix.mini.basketball.BasketballGame;
import me.x_tias.partix.mini.basketball.BasketballLobby;
import me.x_tias.partix.mini.factories.Hub;
import me.x_tias.partix.mini.game.GoalGame;
import me.x_tias.partix.plugin.athlete.Athlete;
import me.x_tias.partix.plugin.athlete.AthleteManager;
import me.x_tias.partix.util.Colour;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

@CommandAlias("coach|uncoach")
public class RefereeCommand extends BaseCommand {

    @CommandAlias("coach")
    @CommandPermission("rank.referee")
    @Syntax("<player> <home|away>")
    @CommandCompletion("@players home|away")
    public void onCoach(Player sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(Component.text("Usage: /coach <player> <home|away>").color(Colour.deny()));
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            sender.sendMessage(Component.text("Player not found or offline.").color(Colour.deny()));
            return;
        }

        String teamName = args[1].toLowerCase();
        if (!teamName.equals("home") && !teamName.equals("away")) {
            sender.sendMessage(Component.text("Team must be 'home' or 'away'.").color(Colour.deny()));
            return;
        }

        // Get target's current game
        Athlete targetAthlete = AthleteManager.get(target.getUniqueId());
        if (targetAthlete == null || !(targetAthlete.getPlace() instanceof BasketballGame)) {
            sender.sendMessage(Component.text(target.getName() + " is not in a basketball game.").color(Colour.deny()));
            return;
        }

        BasketballGame game = (BasketballGame) targetAthlete.getPlace();

        // Check if game is custom/arena (not ranked)
        // Method 1: Check if it's NOT a ranked game
        if (!isCustomGame(game)) {
            sender.sendMessage(Component.text("Coaches can only be assigned in custom/arena games, not ranked games!").color(Colour.deny()));
            return;
        }

        GoalGame.Team team = teamName.equals("home") ? GoalGame.Team.HOME : GoalGame.Team.AWAY;

        // Check if there's already a coach for this team
        UUID existingCoach = game.getCoach(team);
        if (existingCoach != null) {
            Player existingCoachPlayer = Bukkit.getPlayer(existingCoach);
            String existingName = existingCoachPlayer != null ? existingCoachPlayer.getName() : "Unknown";
            sender.sendMessage(Component.text("Team " + teamName + " already has a coach: " + existingName).color(Colour.deny()));
            sender.sendMessage(Component.text("Use /uncoach " + existingName + " first.").color(Colour.text()));
            return;
        }

        // Assign coach
        game.setCoach(team, target.getUniqueId());

        game.sendMessage(Component.text("🎯 " + target.getName() + " is now the coach for " + teamName.toUpperCase() + " team!").color(Colour.partix()));
        target.sendMessage(Component.text("You are now the coach! Use /bench, /unbench, and /timeout commands.").color(Colour.allow()));
        sender.sendMessage(Component.text("Successfully assigned " + target.getName() + " as coach for " + teamName + " team.").color(Colour.allow()));
    }

    @CommandAlias("uncoach")
    @CommandPermission("rank.referee")
    @Syntax("<player>")
    @CommandCompletion("@players")
    public void onUncoach(Player sender, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(Component.text("Usage: /uncoach <player>").color(Colour.deny()));
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            sender.sendMessage(Component.text("Player not found or offline.").color(Colour.deny()));
            return;
        }

        // Get target's current game
        Athlete targetAthlete = AthleteManager.get(target.getUniqueId());
        if (targetAthlete == null || !(targetAthlete.getPlace() instanceof BasketballGame)) {
            sender.sendMessage(Component.text(target.getName() + " is not in a basketball game.").color(Colour.deny()));
            return;
        }

        BasketballGame game = (BasketballGame) targetAthlete.getPlace();

        // Check if player is actually a coach
        if (!game.isCoach(target.getUniqueId())) {
            sender.sendMessage(Component.text(target.getName() + " is not a coach.").color(Colour.deny()));
            return;
        }

        // Remove coach
        game.removeCoach(target.getUniqueId());

        game.sendMessage(Component.text("🎯 " + target.getName() + " is no longer a coach.").color(Colour.partix()));
        target.sendMessage(Component.text("You have been removed as coach.").color(Colour.deny()));
        sender.sendMessage(Component.text("Successfully removed " + target.getName() + " as coach.").color(Colour.allow()));
    }

    /**
     * Check if a game is a custom/arena game (not ranked)
     *
     * Custom games have these characteristics:
     * - settings.compType != RANKED
     * - OR settings.gameType == MANUAL
     * - OR located in myCourts
     */
    private boolean isCustomGame(BasketballGame game) {
        // Method 1: Check game settings (most reliable)
        if (game.settings.compType != me.x_tias.partix.plugin.settings.CompType.RANKED) {
            return true; // It's a custom/casual game
        }

        if (game.settings.gameType == me.x_tias.partix.plugin.settings.GameType.MANUAL) {
            return true; // Manual games are custom
        }

        // Method 2: Check if court distance matches custom courts (32.0 = custom arenas)
        if (game.getCourtLength() == 26.0) {
            return true;
        }

        // Method 3: Check location against known myCourts coordinates
        Location gameLoc = game.getLocation();
        return isInMyCourts(gameLoc);
    }

    /**
     * Check if a location matches myCourts coordinates
     * Based on BasketballLobby constructor lines 81-85
     */
    private boolean isInMyCourts(Location location) {
        // myCourts locations from BasketballLobby:
        // this.myCourts.add(new Location(Bukkit.getWorlds().getFirst(), 285.5, -60, -509.5));
        // this.myCourts.add(new Location(Bukkit.getWorlds().getFirst(), -226.5, -60, -509.5));
        // this.myCourts.add(new Location(Bukkit.getWorlds().getFirst(), 798.5, -60, 3.5));
        // this.myCourts.add(new Location(Bukkit.getWorlds().getFirst(), 798.5, -60, 472.5));
        // this.myCourts.add(new Location(Bukkit.getWorlds().getFirst(), 798.5, -60, 963.5));

        double x = location.getX();
        double z = location.getZ();

        // Check each myCourt location (allowing small margin for floating point)
        return (Math.abs(x - 285.5) < 1.0 && Math.abs(z + 509.5) < 1.0) ||
                (Math.abs(x + 226.5) < 1.0 && Math.abs(z + 509.5) < 1.0) ||
                (Math.abs(x - 798.5) < 1.0 && Math.abs(z - 3.5) < 1.0) ||
                (Math.abs(x - 798.5) < 1.0 && Math.abs(z - 472.5) < 1.0) ||
                (Math.abs(x - 798.5) < 1.0 && Math.abs(z - 963.5) < 1.0);
    }
}