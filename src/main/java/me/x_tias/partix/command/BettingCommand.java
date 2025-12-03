/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 */
package me.x_tias.partix.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Subcommand;
import me.x_tias.partix.Partix;
import me.x_tias.partix.database.PlayerDb;
import me.x_tias.partix.mini.betting.BettingManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@CommandAlias(value = "betting")
@CommandPermission(value = "partix.admin")
public class BettingCommand
        extends BaseCommand {
    private static final List<Integer> VALID_SLOTS = Arrays.asList(2, 5, 8, 11, 14, 17, 20, 23, 26, 29, 32, 35);
    private final Partix plugin;

    public BettingCommand(Partix plugin) {
        this.plugin = plugin;
    }

    @Subcommand(value = "adminbookview")
    public void onAdminBookView(Player player) {
        Map<String, Map<String, Integer>> stats = BettingManager.getPickStats();
        if (stats.isEmpty()) {
            player.sendMessage("\ud83d\udcd6 No picks have been made yet.");
            return;
        }
        player.sendMessage("\ud83d\udcca **Betting Statistics:**");
        for (Map.Entry<String, Map<String, Integer>> entry : stats.entrySet()) {
            String pickKey = entry.getKey();
            Map<String, Integer> options = entry.getValue();
            BettingManager.BetPick pick = BettingManager.getPickByKey(pickKey);
            if (pick == null) continue;
            player.sendMessage("\ud83d\udd39 **" + pickKey + " – " + pick.question + "**");
            int totalVotes = options.values().stream().mapToInt(i -> i).sum();
            for (Map.Entry<String, Integer> opt : options.entrySet()) {
                int percentage = totalVotes == 0 ? 0 : opt.getValue() * 100 / totalVotes;
                player.sendMessage("  - " + opt.getKey() + ": " + percentage + "% (" + opt.getValue() + " votes)");
            }
        }
    }

    @Subcommand(value = "lockpicks")
    public void onLockPicksNow(Player sender) {
        BettingManager.lockAllPicksGlobally();
        Bukkit.broadcastMessage("§cAll picks have been globally locked by an administrator.");
    }

    @Subcommand(value = "schedulelock")
    @CommandCompletion(value = "@nothing")
    public void scheduleLockPicks(Player sender, int minutes) {
        if (minutes <= 0) {
            sender.sendMessage("❌ You must specify a positive number of minutes.");
            return;
        }
        long ticks = (long) minutes * 60L * 20L;
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            BettingManager.lockAllPicksGlobally();
            Bukkit.broadcastMessage("§cAll picks have been automatically locked after " + minutes + " minutes!");
        }, ticks);
        sender.sendMessage("⏳ Picks will automatically lock in " + minutes + " minute(s).");
    }

    @Subcommand(value = "correctpick")
    @CommandCompletion(value = "@nothing")
    public void onCorrectPick(Player sender, String pickKey, String side) {
        BettingManager.BetPick pick = BettingManager.getPickByKey(pickKey);
        if (pick == null) {
            sender.sendMessage("❌ Invalid pickKey: " + pickKey);
            return;
        }
        if (!side.equalsIgnoreCase("left") && !side.equalsIgnoreCase("right")) {
            sender.sendMessage("❌ Second argument must be either “left” or “right.”");
            return;
        }
        BettingManager.setPickCorrectSide(pickKey, side.toLowerCase());
        sender.sendMessage("✅ Set correct side of “" + pickKey + "” to “" + side.toLowerCase() + ".”");
    }

    @Subcommand(value = "endcurrentpicks")
    @CommandCompletion(value = "@nothing")
    public void onEndCurrentPicks(CommandSender sender, String gameId) {
        BettingManager.endCurrentPicks(sender, gameId);
    }

    @Subcommand(value = "mbabucks add")
    @CommandCompletion(value = "@players")
    public void onAddMBABucks(Player sender, String targetName, int amount) {
        if (amount <= 0) {
            sender.sendMessage("❌ You must add at least 1 MBABuck.");
            return;
        }
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            sender.sendMessage("❌ Player not found: " + targetName);
            return;
        }
        UUID targetId = target.getUniqueId();
        PlayerDb.add(targetId, PlayerDb.Stat.MBA_BUCKS, amount);
        sender.sendMessage("✅ Added " + amount + " MBABucks to " + target.getName());
        target.sendMessage("\ud83d\udcb0 You received " + amount + " MBABucks!");
    }

    @Subcommand(value = "mbabucks remove")
    @CommandCompletion(value = "@players")
    public void onRemoveMBABucks(Player sender, String targetName, int amount) {
        if (amount <= 0) {
            sender.sendMessage("❌ You must remove at least 1 MBABuck.");
            return;
        }
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            sender.sendMessage("❌ Player not found: " + targetName);
            return;
        }
        UUID targetId = target.getUniqueId();
        PlayerDb.get(targetId, PlayerDb.Stat.MBA_BUCKS).thenAccept(currentBucks -> {
            if (currentBucks < amount) {
                sender.sendMessage("❌ " + target.getName() + " only has " + currentBucks + " MBABucks.");
                return;
            }
            PlayerDb.remove(targetId, PlayerDb.Stat.MBA_BUCKS, amount);
            sender.sendMessage("✅ Removed " + amount + " MBABucks from " + target.getName());
            target.sendMessage("\ud83d\udcb0 You lost " + amount + " MBABucks!");
        });
    }

    @Subcommand(value = "pending")
    public void onPendingBets(CommandSender sender) {
        Map<UUID, List<BettingManager.PlayerBet>> allBets = BettingManager.getPlayerBets();
        if (allBets.isEmpty()) {
            sender.sendMessage("\ud83d\udcd6 No pending bets found.");
            return;
        }
        for (Map.Entry<UUID, List<BettingManager.PlayerBet>> entry : allBets.entrySet()) {
            UUID playerId = entry.getKey();
            List<BettingManager.PlayerBet> bets = entry.getValue();
            OfflinePlayer target = Bukkit.getOfflinePlayer(playerId);
            sender.sendMessage("\ud83d\udcca Pending Bets for " + target.getName() + ":");
            for (BettingManager.PlayerBet bet : bets) {
                sender.sendMessage(" - Total Bet: " + bet.totalBet + " MBA Bucks");
                sender.sendMessage("   Parlay: " + (bet.parlayed ? "✔️" : "❌"));
                sender.sendMessage("   Potential Payout: " + bet.getPotentialPayout() + " MBA Bucks");
                for (BettingManager.ParlayPick pick : bet.picks) {
                    sender.sendMessage("      * " + pick.question + " → " + pick.chosenSide + " (PickIndex " + pick.pickIndex + ")");
                }
            }
        }
    }

    @Subcommand(value = "reload")
    @CommandCompletion(value = "@nothing")
    public void onReload(Player sender) {
        this.plugin.reloadConfig();
        BettingManager.loadGamesFromConfig(this.plugin.getConfig());
        sender.sendMessage("§a[Betting] Config reloaded successfully.");
    }
}

