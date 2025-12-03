/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.Sound
 *  org.bukkit.SoundCategory
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.ConsoleCommandSender
 *  org.bukkit.entity.Player
 */
package me.x_tias.partix.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Subcommand;
import me.x_tias.partix.database.BasketballDb;
import me.x_tias.partix.database.PlayerDb;
import me.x_tias.partix.database.SeasonDb;
import me.x_tias.partix.mini.lobby.MainLobby;
import me.x_tias.partix.plugin.athlete.Athlete;
import me.x_tias.partix.plugin.athlete.AthleteManager;
import me.x_tias.partix.server.Place;
import me.x_tias.partix.util.Message;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Objects;

@CommandAlias(value = "partix")
@CommandPermission(value = "rank.admin")
public class PartixCommand
        extends BaseCommand {
    @Subcommand(value = "coins add")
    public void onCoinAdd(CommandSender sender, String[] args) {
        if (sender.isOp() || sender.hasPermission("rank.admin")) {
            if (args.length == 2) {
                OfflinePlayer op = Bukkit.getOfflinePlayer(args[0]);
                if (op.hasPlayedBefore()) {
                    int m = Integer.parseInt(args[1]);
                    if (m > 0) {
                        Player player;
                        PlayerDb.add(op.getUniqueId(), PlayerDb.Stat.COINS, m);
                        sender.sendMessage("Successfully gave " + op.getName() + " " + m + " Coin(s)!");
                        if (op.isOnline() && (player = op.getPlayer()) != null) {
                            Athlete athlete = AthleteManager.get(player.getUniqueId());
                            Place place = athlete.getPlace();
                            if (place instanceof MainLobby lobby) {
                                lobby.updateSidebar(athlete);
                            }
                            player.sendMessage(Message.receiveCoins(m));
                            player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, SoundCategory.MASTER, 100.0f, 1.0f);
                        }
                    } else {
                        sender.sendMessage("/partix coins add <player> <amount>");
                    }
                } else {
                    sender.sendMessage("Unknown player");
                }
            } else {
                sender.sendMessage("/partix coins add <player> <amount>");
            }
        }
    }

    @Subcommand(value = "coins remove")
    public void onCoinRemove(CommandSender sender, String[] args) {
        if (sender.isOp() || sender.hasPermission("rank.admin")) {
            if (args.length == 2) {
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
                if (target.hasPlayedBefore() || target.isOnline()) {
                    int amount;
                    try {
                        amount = Integer.parseInt(args[1]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage("§cInvalid number provided.");
                        return;
                    }
                    if (amount > 0) {
                        PlayerDb.get(target.getUniqueId(), PlayerDb.Stat.COINS).thenAccept(currentCoins -> {
                            Player targetPlayer;
                            int newCoins = currentCoins - amount;
                            if (newCoins < 0) {
                                newCoins = 0;
                            }
                            PlayerDb.set(target.getUniqueId(), PlayerDb.Stat.COINS, newCoins);
                            sender.sendMessage("§aSuccessfully removed " + amount + " coin(s) from " + target.getName() + "!");
                            if (target.isOnline() && (targetPlayer = target.getPlayer()) != null) {
                                targetPlayer.sendMessage("§c" + amount + " coin(s) have been taken from your balance.");
                            }
                        });
                    } else {
                        sender.sendMessage("§cPlease provide a positive amount.");
                    }
                } else {
                    sender.sendMessage("§cUnknown player.");
                }
            } else {
                sender.sendMessage("Usage: /partix coins remove <player> <amount>");
            }
        } else {
            sender.sendMessage("§cYou do not have permission to run this command.");
        }
    }

    @Subcommand(value = "coins get")
    public void onCoinGet(CommandSender sender, String[] args) {
        if (sender.isOp() || sender.hasPermission("rank.mod")) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(args[0]);
            if (args.length == 1) {
                if (op.hasPlayedBefore() || op.isOnline()) {
                    PlayerDb.get(op.getUniqueId(), PlayerDb.Stat.COINS).thenAccept(coins -> {
                        sender.sendMessage(op.getName() + " has " + coins + " Coin(s)!");
                    });
                } else {
                    sender.sendMessage("/partix coins get <player>");
                }
            } else {
                sender.sendMessage("Unknown player");
            }
        } else {
            sender.sendMessage("/partix coins get <player>");
        }
    }

    @Subcommand(value = "seasons top")
    public void onSeasonGet(CommandSender sender) {
        if (sender.isOp() || sender.hasPermission("rank.admin")) {
            SeasonDb.getTop(SeasonDb.Stat.POINTS, 25).thenAccept(top -> {
                for (int i : top.keySet()) {
                    sender.sendMessage(i + ". " + Objects.requireNonNull(Bukkit.getOfflinePlayer(top.get(i))).getName() + " - " + SeasonDb.get(top.get(i), SeasonDb.Stat.POINTS));
                }
            });
        }
    }

    @Subcommand(value = "seasons trophies")
    public void onSeasonTrophies(CommandSender sender) {
        if (sender.isOp() || sender.hasPermission("rank.admin")) {
            PlayerDb.getTop(PlayerDb.Stat.CHAMPIONSHIPS, 30).thenAccept(top -> {
                for (int i : top.keySet()) {
                    sender.sendMessage(i + ". " + Objects.requireNonNull(Bukkit.getOfflinePlayer(top.get(i))).getName() + " - " + PlayerDb.get(top.get(i), PlayerDb.Stat.CHAMPIONSHIPS));
                }
            });
        }
    }

    @Subcommand(value = "seasons gold")
    public void onSeasonGold(CommandSender sender) {
        if (sender.isOp() || sender.hasPermission("rank.admin")) {
            PlayerDb.getTop(PlayerDb.Stat.SEASONS_GOLD, 30).thenAccept(top -> {
                for (int i : top.keySet()) {
                    sender.sendMessage(i + ". " + Objects.requireNonNull(Bukkit.getOfflinePlayer(top.get(i))).getName() + " - " + PlayerDb.get(top.get(i), PlayerDb.Stat.SEASONS_GOLD));
                }
            });
        }
    }

    @Subcommand(value = "stats basketball reset")
    public void onStatsReset(CommandSender sender) {
        if (sender instanceof ConsoleCommandSender c) {
            if (sender.isOp() || sender.hasPermission("rank.admin")) {
                BasketballDb.setAll(BasketballDb.Stat.POINTS, 0);
                BasketballDb.setAll(BasketballDb.Stat.WINS, 0);
                BasketballDb.setAll(BasketballDb.Stat.LOSSES, 0);
                sender.sendMessage("all basketball points, wins, and losses have been reset!");
            }
        }
    }

    @Subcommand(value = "rank set")
    public void onRankSet(CommandSender sender, String[] args) {
        if (sender.isOp() || sender.hasPermission("rank.admin")) {
            if (args.length == 2) {
                OfflinePlayer op = Bukkit.getOfflinePlayer(args[0]);
                if (op.hasPlayedBefore() || op.isOnline()) {
                    Athlete athlete;
                    Place place;
                    Player player;
                    String name = op.getName();
                    String rank = args[1].toLowerCase(Locale.ROOT);
                    String[] validRanks = new String[]{"admin", "mod", "media", "pro", "vip", "default"};
                    boolean isValid = false;
                    for (String r : validRanks) {
                        if (!r.equals(rank)) continue;
                        isValid = true;
                        break;
                    }
                    if (!isValid) {
                        sender.sendMessage("Invalid rank: " + args[1]);
                        return;
                    }
                    sender.sendMessage("Setting user " + name + " group to " + rank);
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + name + " group set " + rank);
                    if (op.isOnline() && (player = op.getPlayer()) != null && (place = (athlete = AthleteManager.get(player.getUniqueId())).getPlace()) instanceof MainLobby) {
                        MainLobby l = (MainLobby) place;
                        l.updateSidebar(athlete);
                        athlete.updateRank();
                    }
                } else {
                    sender.sendMessage("Unknown player");
                }
            } else {
                sender.sendMessage("Usage: /partix rank set <player> <rank>");
            }
        } else {
            sender.sendMessage("You do not have permission to run this command.");
        }
    }
}

