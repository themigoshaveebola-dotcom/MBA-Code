package me.x_tias.partix.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import me.x_tias.partix.mini.basketball.BasketballGame;
import me.x_tias.partix.mini.game.GoalGame;
import me.x_tias.partix.plugin.athlete.Athlete;
import me.x_tias.partix.plugin.athlete.AthleteManager;
import me.x_tias.partix.plugin.settings.CompType;
import me.x_tias.partix.util.Colour;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

@CommandAlias("bench|unbench|timeout|to")
public class CoachCommand extends BaseCommand {

    @CommandAlias("bench")
    @Syntax("<player>")
    @CommandCompletion("@players")
    public void onBench(Player sender, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(Component.text("Usage: /bench <player>").color(Colour.deny()));
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            sender.sendMessage(Component.text("Player not found or offline.").color(Colour.deny()));
            return;
        }

        // Get sender's current game
        Athlete senderAthlete = AthleteManager.get(sender.getUniqueId());
        if (senderAthlete == null || !(senderAthlete.getPlace() instanceof BasketballGame)) {
            sender.sendMessage(Component.text("You must be in a basketball game to use this command.").color(Colour.deny()));
            return;
        }

        BasketballGame game = (BasketballGame) senderAthlete.getPlace();

        // Check if sender is a coach
        if (!game.isCoach(sender.getUniqueId())) {
            sender.sendMessage(Component.text("Only coaches can bench players!").color(Colour.deny()));
            return;
        }

        // Get coach's team
        GoalGame.Team coachTeam = game.getCoachTeam(sender.getUniqueId());
        if (coachTeam == null) {
            sender.sendMessage(Component.text("Error: Could not determine your team.").color(Colour.deny()));
            return;
        }

        // Get target's current game
        Athlete targetAthlete = AthleteManager.get(target.getUniqueId());
        if (targetAthlete == null || !targetAthlete.getPlace().equals(game)) {
            sender.sendMessage(Component.text(target.getName() + " is not in your game.").color(Colour.deny()));
            return;
        }

        // Check if target is on the same team
        GoalGame.Team targetTeam = game.getTeamOf(target);
        if (targetTeam != coachTeam) {
            sender.sendMessage(Component.text("You can only bench players on your own team!").color(Colour.deny()));
            return;
        }

        // Check if already benched
        if (game.isInBench(target)) {
            sender.sendMessage(Component.text(target.getName() + " is already on the bench.").color(Colour.deny()));
            return;
        }

        // Bench the player
        game.enterBench(target);
        game.sendMessage(Component.text("🪑 " + target.getName() + " has been benched by coach " + sender.getName()).color(Colour.partix()));
        target.sendMessage(Component.text("You have been benched by your coach.").color(Colour.text()));
        sender.sendMessage(Component.text("Successfully benched " + target.getName()).color(Colour.allow()));
    }

    @CommandAlias("unbench")
    @Syntax("<player>")
    @CommandCompletion("@players")
    public void onUnbench(Player sender, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(Component.text("Usage: /unbench <player>").color(Colour.deny()));
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            sender.sendMessage(Component.text("Player not found or offline.").color(Colour.deny()));
            return;
        }

        // Get sender's current game
        Athlete senderAthlete = AthleteManager.get(sender.getUniqueId());
        if (senderAthlete == null || !(senderAthlete.getPlace() instanceof BasketballGame)) {
            sender.sendMessage(Component.text("You must be in a basketball game to use this command.").color(Colour.deny()));
            return;
        }

        BasketballGame game = (BasketballGame) senderAthlete.getPlace();

        // Check if sender is a coach
        if (!game.isCoach(sender.getUniqueId())) {
            sender.sendMessage(Component.text("Only coaches can unbench players!").color(Colour.deny()));
            return;
        }

        // Get coach's team
        GoalGame.Team coachTeam = game.getCoachTeam(sender.getUniqueId());
        if (coachTeam == null) {
            sender.sendMessage(Component.text("Error: Could not determine your team.").color(Colour.deny()));
            return;
        }

        // Get target's current game
        Athlete targetAthlete = AthleteManager.get(target.getUniqueId());
        if (targetAthlete == null || !targetAthlete.getPlace().equals(game)) {
            sender.sendMessage(Component.text(target.getName() + " is not in your game.").color(Colour.deny()));
            return;
        }

        // Check if target is on the same team
        GoalGame.Team targetTeam = game.getTeamOf(target);
        if (targetTeam != coachTeam) {
            sender.sendMessage(Component.text("You can only unbench players on your own team!").color(Colour.deny()));
            return;
        }

        // Check if actually benched
        if (!game.isInBench(target)) {
            sender.sendMessage(Component.text(target.getName() + " is not on the bench.").color(Colour.deny()));
            return;
        }

        // Unbench the player
        game.leaveBench(target);
        game.sendMessage(Component.text("▶️ " + target.getName() + " has been unbenched by coach " + sender.getName()).color(Colour.partix()));
        target.sendMessage(Component.text("You have been unbenched by your coach. Get back in!").color(Colour.allow()));
        sender.sendMessage(Component.text("Successfully unbenched " + target.getName()).color(Colour.allow()));
    }

    @CommandAlias("timeout|to")
    public void onTimeout(Player sender) {
        // Get sender's current game
        Athlete senderAthlete = AthleteManager.get(sender.getUniqueId());
        if (senderAthlete == null || !(senderAthlete.getPlace() instanceof BasketballGame)) {
            sender.sendMessage(Component.text("You must be in a basketball game to use this command.").color(Colour.deny()));
            return;
        }

        BasketballGame game = (BasketballGame) senderAthlete.getPlace();

        // Check if sender is a coach
        if (!game.isCoach(sender.getUniqueId())) {
            sender.sendMessage(Component.text("Only coaches can call timeouts!").color(Colour.deny()));
            return;
        }

        // Get coach's team
        GoalGame.Team coachTeam = game.getCoachTeam(sender.getUniqueId());
        if (coachTeam == null) {
            sender.sendMessage(Component.text("Error: Could not determine your team.").color(Colour.deny()));
            return;
        }

        // Check if game state allows timeouts
        if (game.getState() != GoalGame.State.REGULATION) {
            sender.sendMessage(Component.text("Timeouts can only be called during regulation play!").color(Colour.deny()));
            return;
        }

        // Check if ranked game (ranked games don't allow timeouts)
        if (game.settings.compType == CompType.RANKED) {
            sender.sendMessage(Component.text("Timeouts are not available in ranked games!").color(Colour.deny()));
            return;
        }

        // Check if court length is 26.0 (pro-am games don't allow timeouts)
        if (game.settings.compType == CompType.RANKED) {
            sender.sendMessage(Component.text("Timeouts are not available in ranked games!").color(Colour.deny()));
            return;
        }

        // Check remaining timeouts
        int remainingTimeouts = coachTeam == GoalGame.Team.HOME ? game.getHomeTimeouts() : game.getAwayTimeouts();

        if (remainingTimeouts <= 0) {
            sender.sendMessage(Component.text("Your team has no timeouts remaining!").color(Colour.deny()));
            return;
        }

        // Success message before calling timeout
        sender.sendMessage(Component.text("✓ Calling timeout for " + coachTeam.name() + " team (" + remainingTimeouts + " remaining)").color(Colour.allow()));

        // Call timeout for the coach's team
        game.callTimeout(coachTeam);
    }

    @CommandAlias("skip")
    public void onSkip(Player sender) {
        // Get sender's current game
        Athlete senderAthlete = AthleteManager.get(sender.getUniqueId());
        if (senderAthlete == null || !(senderAthlete.getPlace() instanceof BasketballGame)) {
            sender.sendMessage(Component.text("You must be in a basketball game to use this command.").color(Colour.deny()));
            return;
        }

        BasketballGame game = (BasketballGame) senderAthlete.getPlace();

        // Check if sender is a coach
        if (!game.isCoach(sender.getUniqueId())) {
            sender.sendMessage(Component.text("Only coaches can skip timeouts!").color(Colour.deny()));
            return;
        }

        // Get coach's team
        GoalGame.Team coachTeam = game.getCoachTeam(sender.getUniqueId());
        if (coachTeam == null) {
            sender.sendMessage(Component.text("Error: Could not determine your team.").color(Colour.deny()));
            return;
        }

        // Check if game is in stoppage (timeout in progress)
        if (game.getState() != GoalGame.State.STOPPAGE) {
            sender.sendMessage(Component.text("No timeout is currently in progress!").color(Colour.deny()));
            return;
        }

        // Check if the coach's team called the timeout
        // We need to determine which team called the timeout by checking the remaining timeouts
        // The team with fewer timeouts called the timeout
        int homeTimeouts = game.getHomeTimeouts();
        int awayTimeouts = game.getAwayTimeouts();

        GoalGame.Team timeoutCaller;
        if (homeTimeouts < awayTimeouts) {
            timeoutCaller = GoalGame.Team.HOME;
        } else if (awayTimeouts < homeTimeouts) {
            timeoutCaller = GoalGame.Team.AWAY;
        } else {
            // Both teams have same timeouts - can't determine who called it
            sender.sendMessage(Component.text("Error: Could not determine which team called the timeout.").color(Colour.deny()));
            return;
        }

        // Verify the coach's team is the one that called the timeout
        if (coachTeam != timeoutCaller) {
            sender.sendMessage(Component.text("Only the team that called the timeout can skip it!").color(Colour.deny()));
            return;
        }

        // Skip timeout to 5 seconds
        game.skipTimeoutToTen();
        sender.sendMessage(Component.text("⚡ Skipped timeout to 10 seconds").color(Colour.allow()));
        game.sendMessage(Component.text("⏭ Coach " + sender.getName() + " skipped timeout to 10 seconds").color(Colour.partix()));
    }
}