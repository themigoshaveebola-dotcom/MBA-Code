/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.TextComponent
 *  net.kyori.adventure.text.format.TextDecoration
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.Sound
 *  org.bukkit.SoundCategory
 *  org.bukkit.World
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.player.PlayerInteractEvent
 *  org.bukkit.inventory.ItemFlag
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.PlayerInventory
 *  org.bukkit.inventory.meta.SkullMeta
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.potion.PotionEffect
 *  org.bukkit.potion.PotionEffectType
 *  org.bukkit.scoreboard.Team
 */
package me.x_tias.partix.mini.lobby;

import me.x_tias.partix.Partix;
import me.x_tias.partix.database.BasketballDb;
import me.x_tias.partix.database.PlayerDb;
import me.x_tias.partix.database.SeasonDb;
import me.x_tias.partix.mini.basketball.BasketballGame;
import me.x_tias.partix.mini.basketball.Stadium;
import me.x_tias.partix.mini.betting.BettingManager;
import me.x_tias.partix.mini.factories.Hub;
import me.x_tias.partix.mini.game.GoalGame;
import me.x_tias.partix.plugin.athlete.Athlete;
import me.x_tias.partix.plugin.athlete.AthleteManager;
import me.x_tias.partix.plugin.cosmetics.*;
import me.x_tias.partix.plugin.ball.event.PressRightClickEvent;
import me.x_tias.partix.plugin.gui.GUI;
import me.x_tias.partix.plugin.gui.ItemButton;
import me.x_tias.partix.plugin.party.Party;
import me.x_tias.partix.plugin.party.PartyFactory;
import me.x_tias.partix.plugin.settings.*;
import me.x_tias.partix.plugin.sidebar.Sidebar;
import me.x_tias.partix.server.rank.Ranks;
import me.x_tias.partix.server.specific.Lobby;
import me.x_tias.partix.util.Colour;
import me.x_tias.partix.util.Items;
import me.x_tias.partix.util.Message;
import me.x_tias.partix.util.Perm;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.UUID;
import org.bukkit.event.Listener;


public class MainLobby
        extends Lobby implements Listener {
    public static final ItemStack FILLER = Items.get(Component.text(" "), Material.BLACK_STAINED_GLASS_PANE, 1, " ");
    private final HashMap<Location, BasketballGame> games = new HashMap<>();
    private final Settings customSettings = new Settings(WinType.TIME_5, GameType.MANUAL, WaitType.MEDIUM, CompType.CASUAL, 2, false, false, false, 4, GameEffectType.NONE);
    int i = 0;

    public MainLobby() {
        Bukkit.getScheduler().runTaskLater(Partix.getInstance(), () -> {
        }, 72000L);
    }

    @Override
    public void onTick() {
        ++this.i;
        if (this.i > 300) {
            this.getAthletes().forEach(this::updateSidebar);
            this.i = 0;
        }
        if (this.i < 150) {
            this.updateBossBar("§b§lMinecraft Basketball Association §7§l> §f§lSeason 0");
        } else {
            this.updateBossBar("§b§lSUPPORT THE SERVER! §7§l> §f§l");
        }
    }

    public void updateSidebar(Athlete athlete) {
        Bukkit.getScheduler().runTaskAsynchronously(Partix.getInstance(), () -> {
            Player player = athlete.getPlayer();
            Team playerTeam = Ranks.getScoreboard().getEntryTeam(player.getName());
            String rankPrefix = playerTeam != null ? playerTeam.getPrefix() : "§7";
            int coins = PlayerDb.get(player.getUniqueId(), PlayerDb.Stat.COINS).join();
            int mbaBucks = PlayerDb.get(player.getUniqueId(), PlayerDb.Stat.MBA_BUCKS).join();
            int pts = SeasonDb.get(player.getUniqueId(), SeasonDb.Stat.POINTS).join();
            String div = pts >= 50000 ? "§6Gold" : "§7Silver";
            String points = String.valueOf(pts - (pts >= 50000 ? 50000 : 0));
            
            // Get season pass info
            int tier = PlayerDb.get(player.getUniqueId(), PlayerDb.Stat.SEASON_PASS_TIER).join();
            int exp = PlayerDb.get(player.getUniqueId(), PlayerDb.Stat.SEASON_PASS_EXP).join();
            int expInTier = exp % 1000; // Get current exp in tier (out of 1000)
            String seasonPassProgress = "§6" + tier + " §7(" + expInTier + "/1000)";
            
            Bukkit.getScheduler().runTask(Partix.getInstance(), () ->
                    Sidebar.set(player, Component.text("\uF808\uF808〩"), "                     ", "     ", " ", " ", "§c§lYour Info  ", "  §fName: §b" + player.getName(), "  §fRank: " + rankPrefix, "  §fVer: §b" + (player.getName().startsWith(".") ? "Bedrock" : "Java"), "     ", "§f§lYour Stats  ", "  §fCoins: §e" + coins, "  §fMBA Bucks: §a" + mbaBucks, "  §fPass: " + seasonPassProgress, "        ", "§9§lThis Season  ", "  §fDiv: §e" + div, "  §fPts: §a" + points, "                     ", "§7§.")
            );
        });
    }

    @Override
    public void onJoin(Athlete... athletes) {
        for (Athlete athlete : athletes) {
            Player player = athlete.getPlayer();
            PlayerDb.create(player.getUniqueId(), player.getName());
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 1000000, 5, false, false));
            player.teleport(new Location(Bukkit.getWorlds().getFirst(), 8.97, -59.0, 9.02, 180.0f, 0.0f));
            athlete.setSpectator(false);
            this.updateSidebar(athlete);
            player.getInventory().clear();
            this.giveItems(player);
            Bukkit.getScheduler().runTaskLater(Partix.getInstance(), () -> {
                PlayerDb.get(player.getUniqueId(), PlayerDb.Stat.DEFAULT_COSMETICS).thenAccept(defaultCosmetics -> {
                    if (defaultCosmetics == 0) {
                        PlayerDb.set(player.getUniqueId(), PlayerDb.Stat.TRAIL, 0);
                        PlayerDb.set(player.getUniqueId(), PlayerDb.Stat.EXPLOSION, 0);
                        PlayerDb.set(player.getUniqueId(), PlayerDb.Stat.GREEN_SOUND, 0);
                        PlayerDb.set(player.getUniqueId(), PlayerDb.Stat.DEFAULT_COSMETICS, 1);
                        Bukkit.getLogger().info("[DEBUG] Assigned default cosmetics to " + player.getName());
                        player.sendMessage("§aDefault cosmetics have been applied!");
                    }
                });
            }, 20L);
        }
    }

    @Override
    public void onQuit(Athlete... athletes) {
    }

    @Override
    public void giveItems(Player player) {
        PlayerInventory i = player.getInventory();
        i.setItem(0, Items.get(Message.itemName("Server Selector", "key.use", player), Material.NETHER_STAR));
        i.setItem(1, Items.get(Component.text("§6§lSeason Pass").append(Component.newline()).append(Component.text("§7Click to view rewards")), Material.ENCHANTED_BOOK));
    }

    @EventHandler
    public void onRightClick(PressRightClickEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemStack();
        
        if (item == null || item.getType() == Material.AIR) {
            return;
        }
        
        Bukkit.getLogger().info("[DEBUG] MainLobby right click: " + item.getType());
        
        if (item.getType() == Material.ENCHANTED_BOOK) {
            Bukkit.getLogger().info("[DEBUG] Opening Season Pass GUI for " + player.getName());
            me.x_tias.partix.plugin.seasonpass.SeasonPassManager.openSeasonPassGUI(player);
        }
    }
    public void openPlayerProfileGUI(Player player) {
        openPlayerProfileGUIForTarget(player, player.getUniqueId());
    }

    public void openPlayerProfileGUIForTarget(Player viewer, UUID targetUUID) {
        ItemButton[] buttons = new ItemButton[27];
        for (int i = 0; i < 27; ++i) {
            buttons[i] = new ItemButton(i, FILLER, p -> {});
        }

        // Career Record
        buttons[10] = new ItemButton(10, Items.get(
                Component.text("Career Record").color(Colour.partix()),
                Material.DIAMOND_SWORD,
                1,
                "§7View ranked record"
        ), p -> this.openCareerRecordGUI(p, targetUUID));

        // Career Statistics
        buttons[11] = new ItemButton(11, Items.get(
                Component.text("Career Statistics").color(Colour.partix()),
                Material.BOOK,
                1,
                "§7View total career stats"
        ), p -> this.openCareerStatsGUI(p, targetUUID));

        // Career Averages
        buttons[12] = new ItemButton(12, Items.get(
                Component.text("Career Averages").color(Colour.partix()),
                Material.GOLDEN_APPLE,
                1,
                "§7View stats per game"
        ), p -> this.openCareerAveragesGUI(p, targetUUID));

        // Current Season Stats
        buttons[14] = new ItemButton(14, Items.get(
                Component.text("Current Season Stats").color(Colour.partix()),
                Material.EMERALD,
                1,
                "§7View Season 1 statistics"
        ), p -> this.openSpecificSeasonStatsGUI(p, targetUUID, 1));

        // Current Season Record
        buttons[15] = new ItemButton(15, Items.get(
                Component.text("Current Season Record").color(Colour.partix()),
                Material.IRON_SWORD,
                1,
                "§7View Season 1 record"
        ), p -> this.openSpecificSeasonRecordGUI(p, targetUUID, 1));

        // Accolades (Championships & Achievements)
        buttons[16] = new ItemButton(16, Items.get(
                Component.text("Accolades").color(Colour.partix()),
                Material.GOLD_BLOCK,
                1,
                "§7View your rings & accolades"
        ), p -> this.openAccoladesGUI(p, targetUUID));

        // Cosmetics (moved from main menu to profile)
        buttons[22] = new ItemButton(22, Items.get(
                Component.text("Cosmetics").color(Colour.partix()),
                Material.FIREWORK_STAR,
                1,
                "§7Open Cosmetics Menu"
        ), p -> new CosmeticGUI(p));

        new GUI("§l§6Player Profile", 3, false, buttons).openInventory(viewer);
    }

    private void openCareerRecordGUI(Player player, UUID playerUUID) {
        player.sendMessage("§eLoading career record...");

        // Fetch ALL data asynchronously in parallel
        CompletableFuture<Integer> winsF = PlayerDb.get(playerUUID, PlayerDb.Stat.CAREER_WINS);
        CompletableFuture<Integer> lossesF = PlayerDb.get(playerUUID, PlayerDb.Stat.CAREER_LOSSES);
        CompletableFuture<Integer> gamesF = PlayerDb.get(playerUUID, PlayerDb.Stat.CAREER_GAMES_PLAYED);
        CompletableFuture<Integer> recWinsF = PlayerDb.get(playerUUID, PlayerDb.Stat.REC_WINS);
        CompletableFuture<Integer> recLossesF = PlayerDb.get(playerUUID, PlayerDb.Stat.REC_LOSSES);
        CompletableFuture<Integer> recGamesF = PlayerDb.get(playerUUID, PlayerDb.Stat.REC_GAMES);

        // Wait for ALL to complete, THEN build GUI on main thread
        CompletableFuture.allOf(winsF, lossesF, gamesF, recWinsF, recLossesF, recGamesF).thenAccept(v -> {
            int careerWins = winsF.join();
            int careerLosses = lossesF.join();
            int gamesPlayed = gamesF.join();
            int recWins = recWinsF.join();
            int recLosses = recLossesF.join();
            int recGames = recGamesF.join();
            double winRate = gamesPlayed > 0 ? ((double) careerWins / gamesPlayed) * 100 : 0;

            // Build GUI on main thread
            Bukkit.getScheduler().runTask(Partix.getInstance(), () -> {
                // Get target player name
                OfflinePlayer target = Bukkit.getOfflinePlayer(playerUUID);
                String targetName = target.getName() != null ? target.getName() : "Unknown";
                boolean isSelf = player.getUniqueId().equals(playerUUID);
                String possessive = isSelf ? "Your" : targetName + "'s";

                ItemButton[] buttons = new ItemButton[27];
                for (int i = 0; i < 27; ++i) {
                    buttons[i] = new ItemButton(i, FILLER, p -> {});
                }

                buttons[11] = new ItemButton(11, Items.get(
                        Component.text("Wins: " + careerWins).color(Colour.allow()),
                        Material.LIME_CONCRETE, 1,
                        "§a§oWins:",
                        "§a§o  Rec Wins: " + recWins
                ), p -> {});

                buttons[13] = new ItemButton(13, Items.get(
                        Component.text("Losses: " + careerLosses).color(Colour.deny()),
                        Material.RED_CONCRETE, 1,
                        "§c§oLosses:",
                        "§c§o  Rec Losses: " + recLosses
                ), p -> {});

                buttons[15] = new ItemButton(15, Items.get(
                        Component.text("Games: " + gamesPlayed).color(Colour.partix()),
                        Material.GOLD_BLOCK, 1,
                        "§6§oGames Played:",
                        "§6§o  Rec Games: " + recGames
                ), p -> {});

                buttons[22] = new ItemButton(22, Items.get(
                        Component.text("Win Rate: " + String.format("%.1f", winRate) + "%").color(Colour.partix()),
                        Material.GOLDEN_APPLE, 1,
                        "§7" + possessive + " win percentage"
                ), p -> {});

                buttons[26] = new ItemButton(26, Items.get(
                        Component.text("Back").color(Colour.partix()),
                        Material.ARROW
                ), p -> this.openPlayerProfileGUI(p));

                new GUI("§l§6Career Record", 3, false, buttons).openInventory(player);
            });
        }).exceptionally(ex -> {
            Bukkit.getLogger().severe("Error loading career record: " + ex.getMessage());
            player.sendMessage("§cError loading stats!");
            return null;
        });
    }

    private void openCareerStatsGUI(Player player, UUID playerUUID) {
        player.sendMessage("§eLoading career stats...");

        // Fetch all stats in parallel
        CompletableFuture<Integer> pointsF = PlayerDb.get(playerUUID, PlayerDb.Stat.CAREER_POINTS);
        CompletableFuture<Integer> assistsF = PlayerDb.get(playerUUID, PlayerDb.Stat.CAREER_ASSISTS);
        CompletableFuture<Integer> reboundsF = PlayerDb.get(playerUUID, PlayerDb.Stat.CAREER_REBOUNDS);
        CompletableFuture<Integer> stealsF = PlayerDb.get(playerUUID, PlayerDb.Stat.CAREER_STEALS);
        CompletableFuture<Integer> blocksF = PlayerDb.get(playerUUID, PlayerDb.Stat.CAREER_BLOCKS);
        CompletableFuture<Integer> turnoversF = PlayerDb.get(playerUUID, PlayerDb.Stat.CAREER_TURNOVERS);
        CompletableFuture<Integer> fgMadeF = PlayerDb.get(playerUUID, PlayerDb.Stat.CAREER_FG_MADE);
        CompletableFuture<Integer> fgAttF = PlayerDb.get(playerUUID, PlayerDb.Stat.CAREER_FG_ATTEMPTED);
        CompletableFuture<Integer> fg3MadeF = PlayerDb.get(playerUUID, PlayerDb.Stat.CAREER_3FG_MADE);
        CompletableFuture<Integer> fg3AttF = PlayerDb.get(playerUUID, PlayerDb.Stat.CAREER_3FG_ATTEMPTED);

        // Wait for ALL futures to complete
        CompletableFuture.allOf(pointsF, assistsF, reboundsF, stealsF, blocksF,
                turnoversF, fgMadeF, fgAttF, fg3MadeF, fg3AttF).thenAccept(v -> {

            int points = pointsF.join();
            int assists = assistsF.join();
            int rebounds = reboundsF.join();
            int steals = stealsF.join();
            int blocks = blocksF.join();
            int turnovers = turnoversF.join();
            int fgMade = fgMadeF.join();
            int fgAtt = fgAttF.join();
            int fg3Made = fg3MadeF.join();
            int fg3Att = fg3AttF.join();

            double fgPct = fgAtt > 0 ? ((double) fgMade / fgAtt) * 100 : 0;
            double fg3Pct = fg3Att > 0 ? ((double) fg3Made / fg3Att) * 100 : 0;

            Bukkit.getScheduler().runTask(Partix.getInstance(), () -> {
                ItemButton[] buttons = new ItemButton[45];
                for (int i = 0; i < 45; ++i) {
                    buttons[i] = new ItemButton(i, FILLER, p -> {});
                }

                buttons[10] = new ItemButton(10, Items.get(
                        Component.text("Points: " + points).color(Colour.allow()),
                        Material.DIAMOND, 1
                ), p -> {});

                buttons[11] = new ItemButton(11, Items.get(
                        Component.text("Assists: " + assists).color(Colour.allow()),
                        Material.EMERALD, 1
                ), p -> {});

                buttons[12] = new ItemButton(12, Items.get(
                        Component.text("Rebounds: " + rebounds).color(Colour.allow()),
                        Material.GOLD_BLOCK, 1
                ), p -> {});

                buttons[13] = new ItemButton(13, Items.get(
                        Component.text("Steals: " + steals).color(Colour.allow()),
                        Material.IRON_NUGGET, 1
                ), p -> {});

                buttons[14] = new ItemButton(14, Items.get(
                        Component.text("Blocks: " + blocks).color(Colour.allow()),
                        Material.OBSIDIAN, 1
                ), p -> {});

                buttons[15] = new ItemButton(15, Items.get(
                        Component.text("Turnovers: " + turnovers).color(Colour.deny()),
                        Material.REDSTONE_BLOCK, 1
                ), p -> {});

                buttons[19] = new ItemButton(19, Items.get(
                        Component.text("FG: " + fgMade + "/" + fgAtt).color(Colour.partix()),
                        Material.STONE, 1
                ), p -> {});

                buttons[20] = new ItemButton(20, Items.get(
                        Component.text("FG%: " + String.format("%.1f", fgPct) + "%").color(Colour.partix()),
                        Material.ANDESITE, 1
                ), p -> {});

                buttons[21] = new ItemButton(21, Items.get(
                        Component.text("3FG: " + fg3Made + "/" + fg3Att).color(Colour.partix()),
                        Material.DIORITE, 1
                ), p -> {});

                buttons[22] = new ItemButton(22, Items.get(
                        Component.text("3FG%: " + String.format("%.1f", fg3Pct) + "%").color(Colour.partix()),
                        Material.GRANITE, 1
                ), p -> {});

                buttons[44] = new ItemButton(44, Items.get(
                        Component.text("Back").color(Colour.partix()),
                        Material.ARROW
                ), p -> this.openPlayerProfileGUI(p));

                new GUI("§l§6Career Statistics", 5, false, buttons).openInventory(player);
            });
        }).exceptionally(ex -> {
            Bukkit.getLogger().severe("Error loading career stats: " + ex.getMessage());
            player.sendMessage("§cError loading stats!");
            return null;
        });
    }

    private void openCareerAveragesGUI(Player player, UUID playerUUID) {
        player.sendMessage("§eLoading career averages...");

        CompletableFuture<Integer> gamesF = PlayerDb.get(playerUUID, PlayerDb.Stat.CAREER_GAMES_PLAYED);

        gamesF.thenAccept(gamesPlayed -> {
            if (gamesPlayed == 0) {
                Bukkit.getScheduler().runTask(Partix.getInstance(), () -> {
                    ItemButton[] buttons = new ItemButton[27];
                    for (int i = 0; i < 27; ++i) {
                        buttons[i] = new ItemButton(i, FILLER, p -> {});
                    }

                    buttons[13] = new ItemButton(13, Items.get(
                            Component.text("No Games Played").color(Colour.deny()),
                            Material.BARRIER, 1
                    ), p -> {});

                    buttons[26] = new ItemButton(26, Items.get(
                            Component.text("Back").color(Colour.partix()),
                            Material.ARROW
                    ), p -> this.openPlayerProfileGUI(p));

                    new GUI("§l§6Career Averages", 3, false, buttons).openInventory(player);
                });
            } else {
                CompletableFuture<Integer> pointsF = PlayerDb.get(playerUUID, PlayerDb.Stat.CAREER_POINTS);
                CompletableFuture<Integer> assistsF = PlayerDb.get(playerUUID, PlayerDb.Stat.CAREER_ASSISTS);
                CompletableFuture<Integer> reboundsF = PlayerDb.get(playerUUID, PlayerDb.Stat.CAREER_REBOUNDS);
                CompletableFuture<Integer> stealsF = PlayerDb.get(playerUUID, PlayerDb.Stat.CAREER_STEALS);
                CompletableFuture<Integer> blocksF = PlayerDb.get(playerUUID, PlayerDb.Stat.CAREER_BLOCKS);

                CompletableFuture.allOf(pointsF, assistsF, reboundsF, stealsF, blocksF).thenAccept(v -> {
                    int points = pointsF.join();
                    int assists = assistsF.join();
                    int rebounds = reboundsF.join();
                    int steals = stealsF.join();
                    int blocks = blocksF.join();

                    double ppg = (double) points / gamesPlayed;
                    double apg = (double) assists / gamesPlayed;
                    double rpg = (double) rebounds / gamesPlayed;
                    double spg = (double) steals / gamesPlayed;
                    double bpg = (double) blocks / gamesPlayed;

                    Bukkit.getScheduler().runTask(Partix.getInstance(), () -> {
                        ItemButton[] buttons = new ItemButton[27];
                        for (int i = 0; i < 27; ++i) {
                            buttons[i] = new ItemButton(i, FILLER, p -> {});
                        }

                        buttons[10] = new ItemButton(10, Items.get(
                                Component.text("PPG: " + String.format("%.1f", ppg)).color(Colour.allow()),
                                Material.DIAMOND, 1
                        ), p -> {});

                        buttons[11] = new ItemButton(11, Items.get(
                                Component.text("APG: " + String.format("%.1f", apg)).color(Colour.allow()),
                                Material.EMERALD, 1
                        ), p -> {});

                        buttons[12] = new ItemButton(12, Items.get(
                                Component.text("RPG: " + String.format("%.1f", rpg)).color(Colour.allow()),
                                Material.GOLD_BLOCK, 1
                        ), p -> {});

                        buttons[14] = new ItemButton(14, Items.get(
                                Component.text("SPG: " + String.format("%.1f", spg)).color(Colour.allow()),
                                Material.IRON_NUGGET, 1
                        ), p -> {});

                        buttons[15] = new ItemButton(15, Items.get(
                                Component.text("BPG: " + String.format("%.1f", bpg)).color(Colour.allow()),
                                Material.OBSIDIAN, 1
                        ), p -> {});

                        buttons[26] = new ItemButton(26, Items.get(
                                Component.text("Back").color(Colour.partix()),
                                Material.ARROW
                        ), p -> this.openPlayerProfileGUI(p));

                        new GUI("§l§6Career Averages", 3, false, buttons).openInventory(player);
                    });
                });
            }
        });
    }

    private void openSeasonStatsGUI(Player player, UUID playerUUID) {
        // Season selection menu
        ItemButton[] buttons = new ItemButton[27];
        for (int i = 0; i < 27; ++i) {
            buttons[i] = new ItemButton(i, FILLER, p -> {});
        }

        // Current Season (Season 1) button
        buttons[11] = new ItemButton(11, Items.get(
                Component.text("Season 1 Stats (Current)").color(Colour.allow()),
                Material.EMERALD, 1,
                "§7View Season 1 statistics"
        ), p -> this.openSpecificSeasonStatsGUI(p, playerUUID, 1));

        // Season 3 button - Not Available
        buttons[13] = new ItemButton(13, Items.get(
                Component.text("Season 3 Stats").color(Colour.deny()),
                Material.BARRIER, 1,
                "§7Not available yet"
        ), p -> p.sendMessage("§cSeason 3 has not started yet!"));

        buttons[26] = new ItemButton(26, Items.get(
                Component.text("Back").color(Colour.partix()),
                Material.ARROW
        ), p -> this.openPlayerProfileGUI(p));

        new GUI("§l§6Season Statistics", 3, false, buttons).openInventory(player);
    }

    private void openSeasonRecordGUI(Player player, UUID playerUUID) {
        // Season selection menu
        ItemButton[] buttons = new ItemButton[27];
        for (int i = 0; i < 27; ++i) {
            buttons[i] = new ItemButton(i, FILLER, p -> {});
        }

        // Current Season (Season 1) button
        buttons[11] = new ItemButton(11, Items.get(
                Component.text("Season 1 Record (Current)").color(Colour.allow()),
                Material.DIAMOND_SWORD, 1,
                "§7View Season 1 record"
        ), p -> this.openSpecificSeasonRecordGUI(p, playerUUID, 1));

        // Season 3 button - Not Available
        buttons[13] = new ItemButton(13, Items.get(
                Component.text("Season 3 Record").color(Colour.deny()),
                Material.BARRIER, 1,
                "§7Not available yet"
        ), p -> p.sendMessage("§cSeason 3 has not started yet!"));

        buttons[26] = new ItemButton(26, Items.get(
                Component.text("Back").color(Colour.partix()),
                Material.ARROW
        ), p -> this.openPlayerProfileGUI(p));

        new GUI("§l§6Season Records", 3, false, buttons).openInventory(player);
    }

    private void openCurrentSeasonStatsGUI(Player player, UUID playerUUID) {
        player.sendMessage("§eLoading current season stats...");

        CompletableFuture<Integer> pointsF = SeasonDb.get(playerUUID, SeasonDb.Stat.POINTS);
        CompletableFuture<Integer> winsF = SeasonDb.get(playerUUID, SeasonDb.Stat.WINS);
        CompletableFuture<Integer> lossesF = SeasonDb.get(playerUUID, SeasonDb.Stat.LOSSES);

        // Wait for ALL to complete
        CompletableFuture.allOf(pointsF, winsF, lossesF).thenAccept(v -> {
            int points = pointsF.join();
            int wins = winsF.join();
            int losses = lossesF.join();

            Bukkit.getScheduler().runTask(Partix.getInstance(), () -> {
                ItemButton[] buttons = new ItemButton[27];
                for (int i = 0; i < 27; ++i) {
                    buttons[i] = new ItemButton(i, FILLER, p -> {});
                }

                buttons[11] = new ItemButton(11, Items.get(
                        Component.text("Season Points: " + points).color(Colour.partix()),
                        Material.GOLD_BLOCK, 1
                ), p -> {});

                buttons[13] = new ItemButton(13, Items.get(
                        Component.text("Wins: " + wins).color(Colour.allow()),
                        Material.LIME_CONCRETE, 1
                ), p -> {});

                buttons[15] = new ItemButton(15, Items.get(
                        Component.text("Losses: " + losses).color(Colour.deny()),
                        Material.RED_CONCRETE, 1
                ), p -> {});

                buttons[26] = new ItemButton(26, Items.get(
                        Component.text("Back").color(Colour.partix()),
                        Material.ARROW
                ), p -> this.openSeasonStatsGUI(p, playerUUID));

                new GUI("§l§6Current Season Stats", 3, false, buttons).openInventory(player);
            });
        }).exceptionally(ex -> {
            Bukkit.getLogger().severe("Error loading current season stats: " + ex.getMessage());
            player.sendMessage("§cError loading season stats!");
            return null;
        });
    }
    private void openSpecificSeasonRecordGUI(Player player, UUID playerUUID, int season) {
        player.sendMessage("§eLoading Season " + season + " record...");

        CompletableFuture<Integer> winsF = PlayerDb.get(playerUUID, PlayerDb.Stat.valueOf("SEASON_" + season + "_WINS"));
        CompletableFuture<Integer> lossesF = PlayerDb.get(playerUUID, PlayerDb.Stat.valueOf("SEASON_" + season + "_LOSSES"));
        CompletableFuture<Integer> gamesF = PlayerDb.get(playerUUID, PlayerDb.Stat.valueOf("SEASON_" + season + "_GAMES_PLAYED"));
        CompletableFuture<Integer> recWinsF = PlayerDb.get(playerUUID, PlayerDb.Stat.REC_WINS);
        CompletableFuture<Integer> recLossesF = PlayerDb.get(playerUUID, PlayerDb.Stat.REC_LOSSES);
        CompletableFuture<Integer> recGamesF = PlayerDb.get(playerUUID, PlayerDb.Stat.REC_GAMES);

        // Wait for ALL to complete
        CompletableFuture.allOf(winsF, lossesF, gamesF, recWinsF, recLossesF, recGamesF).thenAccept(v -> {
            int wins = winsF.join();
            int losses = lossesF.join();
            int gamesPlayed = gamesF.join();
            int recWins = recWinsF.join();
            int recLosses = recLossesF.join();
            int recGames = recGamesF.join();
            double winRate = gamesPlayed > 0 ? ((double) wins / gamesPlayed) * 100 : 0;

            Bukkit.getScheduler().runTask(Partix.getInstance(), () -> {
                ItemButton[] buttons = new ItemButton[27];
                for (int i = 0; i < 27; ++i) {
                    buttons[i] = new ItemButton(i, FILLER, p -> {});
                }

                buttons[11] = new ItemButton(11, Items.get(
                        Component.text("Wins: " + wins).color(Colour.allow()),
                        Material.LIME_CONCRETE, 1,
                        "§a§oWins:",
                        "§a§o  Rec Wins: " + recWins
                ), p -> {});

                buttons[13] = new ItemButton(13, Items.get(
                        Component.text("Losses: " + losses).color(Colour.deny()),
                        Material.RED_CONCRETE, 1,
                        "§c§oLosses:",
                        "§c§o  Rec Losses: " + recLosses
                ), p -> {});

                buttons[15] = new ItemButton(15, Items.get(
                        Component.text("Games: " + gamesPlayed).color(Colour.partix()),
                        Material.GOLD_BLOCK, 1,
                        "§6§oGames Played:",
                        "§6§o  Rec Games: " + recGames
                ), p -> {});

                buttons[22] = new ItemButton(22, Items.get(
                        Component.text("Win Rate: " + String.format("%.1f", winRate) + "%").color(Colour.partix()),
                        Material.GOLDEN_APPLE, 1
                ), p -> {});

                buttons[26] = new ItemButton(26, Items.get(
                        Component.text("Back").color(Colour.partix()),
                        Material.ARROW
                ), p -> this.openSeasonRecordGUI(p, playerUUID));

                new GUI("§l§6Season " + season + " Record", 3, false, buttons).openInventory(player);
            });
        }).exceptionally(ex -> {
            Bukkit.getLogger().severe("Error loading Season " + season + " record: " + ex.getMessage());
            player.sendMessage("§cError loading season record!");
            return null;
        });
    }

    private void openCurrentSeasonRecordGUI(Player player, UUID playerUUID) {
        player.sendMessage("§eLoading current season record...");

        CompletableFuture<Integer> winsF = SeasonDb.get(playerUUID, SeasonDb.Stat.WINS);
        CompletableFuture<Integer> lossesF = SeasonDb.get(playerUUID, SeasonDb.Stat.LOSSES);

        // Wait for ALL to complete
        CompletableFuture.allOf(winsF, lossesF).thenAccept(v -> {
            int wins = winsF.join();
            int losses = lossesF.join();
            int gamesPlayed = wins + losses;
            double winRate = gamesPlayed > 0 ? ((double) wins / gamesPlayed) * 100 : 0;

            Bukkit.getScheduler().runTask(Partix.getInstance(), () -> {
                ItemButton[] buttons = new ItemButton[27];
                for (int i = 0; i < 27; ++i) {
                    buttons[i] = new ItemButton(i, FILLER, p -> {});
                }

                buttons[11] = new ItemButton(11, Items.get(
                        Component.text("Wins: " + wins).color(Colour.allow()),
                        Material.LIME_CONCRETE, 1
                ), p -> {});

                buttons[13] = new ItemButton(13, Items.get(
                        Component.text("Losses: " + losses).color(Colour.deny()),
                        Material.RED_CONCRETE, 1
                ), p -> {});

                buttons[15] = new ItemButton(15, Items.get(
                        Component.text("Win Rate: " + String.format("%.1f", winRate) + "%").color(Colour.partix()),
                        Material.GOLDEN_APPLE, 1
                ), p -> {});

                buttons[26] = new ItemButton(26, Items.get(
                        Component.text("Back").color(Colour.partix()),
                        Material.ARROW
                ), p -> this.openSeasonRecordGUI(p, playerUUID));

                new GUI("§l§6Current Season Record", 3, false, buttons).openInventory(player);
            });
        }).exceptionally(ex -> {
            Bukkit.getLogger().severe("Error loading current season record: " + ex.getMessage());
            player.sendMessage("§cError loading season record!");
            return null;
        });
    }

    private void openSpecificSeasonStatsGUI(Player player, UUID playerUUID, int season) {
        player.sendMessage("§eLoading Season " + season + " stats...");

        CompletableFuture<Integer> pointsF = PlayerDb.get(playerUUID, PlayerDb.Stat.valueOf("SEASON_" + season + "_POINTS"));
        CompletableFuture<Integer> assistsF = PlayerDb.get(playerUUID, PlayerDb.Stat.valueOf("SEASON_" + season + "_ASSISTS"));
        CompletableFuture<Integer> reboundsF = PlayerDb.get(playerUUID, PlayerDb.Stat.valueOf("SEASON_" + season + "_REBOUNDS"));
        CompletableFuture<Integer> stealsF = PlayerDb.get(playerUUID, PlayerDb.Stat.valueOf("SEASON_" + season + "_STEALS"));
        CompletableFuture<Integer> blocksF = PlayerDb.get(playerUUID, PlayerDb.Stat.valueOf("SEASON_" + season + "_BLOCKS"));
        CompletableFuture<Integer> turnoversF = PlayerDb.get(playerUUID, PlayerDb.Stat.valueOf("SEASON_" + season + "_TURNOVERS"));
        CompletableFuture<Integer> fgMadeF = PlayerDb.get(playerUUID, PlayerDb.Stat.valueOf("SEASON_" + season + "_FG_MADE"));
        CompletableFuture<Integer> fgAttF = PlayerDb.get(playerUUID, PlayerDb.Stat.valueOf("SEASON_" + season + "_FG_ATTEMPTED"));
        CompletableFuture<Integer> fg3MadeF = PlayerDb.get(playerUUID, PlayerDb.Stat.valueOf("SEASON_" + season + "_3FG_MADE"));
        CompletableFuture<Integer> fg3AttF = PlayerDb.get(playerUUID, PlayerDb.Stat.valueOf("SEASON_" + season + "_3FG_ATTEMPTED"));

        // Wait for ALL to complete
        CompletableFuture.allOf(pointsF, assistsF, reboundsF, stealsF, blocksF,
                turnoversF, fgMadeF, fgAttF, fg3MadeF, fg3AttF).thenAccept(v -> {

            int points = pointsF.join();
            int assists = assistsF.join();
            int rebounds = reboundsF.join();
            int steals = stealsF.join();
            int blocks = blocksF.join();
            int turnovers = turnoversF.join();
            int fgMade = fgMadeF.join();
            int fgAtt = fgAttF.join();
            int fg3Made = fg3MadeF.join();
            int fg3Att = fg3AttF.join();

            double fgPct = fgAtt > 0 ? ((double) fgMade / fgAtt) * 100 : 0;
            double fg3Pct = fg3Att > 0 ? ((double) fg3Made / fg3Att) * 100 : 0;

            Bukkit.getScheduler().runTask(Partix.getInstance(), () -> {
                ItemButton[] buttons = new ItemButton[45];
                for (int i = 0; i < 45; ++i) {
                    buttons[i] = new ItemButton(i, FILLER, p -> {});
                }

                buttons[10] = new ItemButton(10, Items.get(
                        Component.text("Points: " + points).color(Colour.allow()),
                        Material.DIAMOND, 1
                ), p -> {});

                buttons[11] = new ItemButton(11, Items.get(
                        Component.text("Assists: " + assists).color(Colour.allow()),
                        Material.EMERALD, 1
                ), p -> {});

                buttons[12] = new ItemButton(12, Items.get(
                        Component.text("Rebounds: " + rebounds).color(Colour.allow()),
                        Material.GOLD_BLOCK, 1
                ), p -> {});

                buttons[13] = new ItemButton(13, Items.get(
                        Component.text("Steals: " + steals).color(Colour.allow()),
                        Material.IRON_NUGGET, 1
                ), p -> {});

                buttons[14] = new ItemButton(14, Items.get(
                        Component.text("Blocks: " + blocks).color(Colour.allow()),
                        Material.OBSIDIAN, 1
                ), p -> {});

                buttons[15] = new ItemButton(15, Items.get(
                        Component.text("Turnovers: " + turnovers).color(Colour.deny()),
                        Material.REDSTONE_BLOCK, 1
                ), p -> {});

                buttons[19] = new ItemButton(19, Items.get(
                        Component.text("FG: " + fgMade + "/" + fgAtt).color(Colour.partix()),
                        Material.STONE, 1
                ), p -> {});

                buttons[20] = new ItemButton(20, Items.get(
                        Component.text("FG%: " + String.format("%.1f", fgPct) + "%").color(Colour.partix()),
                        Material.ANDESITE, 1
                ), p -> {});

                buttons[21] = new ItemButton(21, Items.get(
                        Component.text("3FG: " + fg3Made + "/" + fg3Att).color(Colour.partix()),
                        Material.DIORITE, 1
                ), p -> {});

                buttons[22] = new ItemButton(22, Items.get(
                        Component.text("3FG%: " + String.format("%.1f", fg3Pct) + "%").color(Colour.partix()),
                        Material.GRANITE, 1
                ), p -> {});

                buttons[44] = new ItemButton(44, Items.get(
                        Component.text("Back").color(Colour.partix()),
                        Material.ARROW
                ), p -> this.openSeasonStatsGUI(p, playerUUID));

                new GUI("§l§6Season " + season + " Statistics", 5, false, buttons).openInventory(player);
            });
        }).exceptionally(ex -> {
            Bukkit.getLogger().severe("Error loading Season " + season + " stats: " + ex.getMessage());
            player.sendMessage("§cError loading season stats!");
            return null;
        });
    }

    private void openAccoladesGUI(Player player, UUID playerUUID) {
        player.sendMessage("§eLoading accolades...");

        PlayerDb.get(playerUUID, PlayerDb.Stat.CHAMPIONSHIPS).thenAccept(championships -> {
            PlayerDb.getString(playerUUID, PlayerDb.Stat.CHAMPIONSHIP_RINGS).thenAccept(ringsJson -> {
                PlayerDb.getString(playerUUID, PlayerDb.Stat.ACCOLADES).thenAccept(accoladesJson -> {
                    Bukkit.getScheduler().runTask(Partix.getInstance(), () -> {
                        ItemButton[] buttons = new ItemButton[27];
                        for (int i = 0; i < 27; ++i) {
                            buttons[i] = new ItemButton(i, FILLER, p -> {});
                        }

                        // Parse ring names
                        List<String> ringNames = new ArrayList<>();
                        if (ringsJson != null && !ringsJson.isEmpty() && !ringsJson.equals("{}")) {
                            try {
                                com.google.gson.Gson gson = new com.google.gson.Gson();
                                java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<String>>(){}.getType();
                                List<String> parsed = gson.fromJson(ringsJson, listType);
                                if (parsed != null) {
                                    ringNames = parsed;
                                }
                            } catch (Exception e) {
                                // Ignore parsing errors
                            }
                        }

                        // Parse accolades
                        List<String> accoladesList = new ArrayList<>();
                        if (accoladesJson != null && !accoladesJson.isEmpty() && !accoladesJson.equals("{}")) {
                            try {
                                com.google.gson.Gson gson = new com.google.gson.Gson();
                                java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<String>>(){}.getType();
                                List<String> parsed = gson.fromJson(accoladesJson, listType);
                                if (parsed != null) {
                                    accoladesList = parsed;
                                }
                            } catch (Exception e) {
                                // Ignore parsing errors
                            }
                        }

                        // Championship Rings button (left side)
                        List<String> ringsLore = new ArrayList<>();
                        ringsLore.add("§7Championship rings won");
                        if (!ringNames.isEmpty()) {
                            ringsLore.add("§r");
                            for (String ringName : ringNames) {
                                ringsLore.add("§e" + ringName);
                            }
                        }

                        buttons[11] = new ItemButton(11, Items.get(
                                Component.text("Championship Rings: " + championships).color(Colour.partix()),
                                Material.GOLD_BLOCK, 1,
                                ringsLore.toArray(new String[0])
                        ), p -> {});

                        // Accolades button (right side)
                        List<String> accoladesLore = new ArrayList<>();
                        accoladesLore.add("§7Special achievements & honors");
                        if (!accoladesList.isEmpty()) {
                            accoladesLore.add("§r");
                            for (String accolade : accoladesList) {
                                accoladesLore.add("§b" + accolade);
                            }
                        }

                        buttons[15] = new ItemButton(15, Items.get(
                                Component.text("Accolades: " + accoladesList.size()).color(Colour.partix()),
                                Material.DIAMOND, 1,
                                accoladesLore.toArray(new String[0])
                        ), p -> {});

                        buttons[26] = new ItemButton(26, Items.get(
                                Component.text("Back").color(Colour.partix()),
                                Material.ARROW
                        ), p -> this.openPlayerProfileGUI(p));

                        new GUI("§l§6Accolades", 3, false, buttons).openInventory(player);
                    });
                });
            });
        });
    }

    public void openServerSelectorGUI(Player player) {
        int size = 54;
        ItemButton[] buttons = new ItemButton[54];
        
        // Initialize all slots with texture paper (model_data 1111)
        for (int i = 0; i < 54; ++i) {
            ItemStack textureItem = new ItemStack(Material.PAPER);
            textureItem.editMeta(meta -> {
                meta.setCustomModelData(1111);
                meta.displayName(Component.text(" "));
            });
            buttons[i] = new ItemButton(i, textureItem, p -> {});
        }
        
        // Section 1: Park Queue (slots 0-2, 9-11, 18-20)
        ItemStack parkQueueIcon = new ItemStack(Material.PAPER);
        parkQueueIcon.editMeta(meta -> {
            meta.setCustomModelData(1111);
            meta.displayName(Component.text("§6Park Queue"));
            meta.lore(List.of(Component.text("§7Join the Park Queue")));
        });
        int[] parkSlots = {0, 1, 2, 9, 10, 11, 18, 19, 20};
        for (int slot : parkSlots) {
            buttons[slot] = new ItemButton(slot, parkQueueIcon, p -> Hub.basketballLobby.openGameSelectorGUI(p));
        }
        
        // Section 2: Rec Center (slots 3-5, 12-14, 21-23)
        ItemStack recIcon = new ItemStack(Material.PAPER);
        recIcon.editMeta(meta -> {
            meta.setCustomModelData(1111);
            meta.displayName(Component.text("§6Rec Center"));
            meta.lore(List.of(
                Component.text("§74 Quarter Games (4 min each)"),
                Component.text("§7Bigger rewards!"),
                Component.text("§73v3 matches only")
            ));
        });
        int[] recSlots = {3, 4, 5, 12, 13, 14, 21, 22, 23};
        for (int slot : recSlots) {
            buttons[slot] = new ItemButton(slot, recIcon, p -> {
                Athlete athlete = AthleteManager.get(p.getUniqueId());
                Hub.recLobby.join(athlete);
            });
        }
        
        // Section 3: My Court (slots 6-8, 15-17, 24-26)
        ItemStack myCourtIcon = new ItemStack(Material.PAPER);
        myCourtIcon.editMeta(meta -> {
            meta.setCustomModelData(1111);
            meta.displayName(Component.text("§6My Court"));
            meta.lore(List.of(Component.text("§7Create or join custom games")));
        });
        int[] myCourtSlots = {6, 7, 8, 15, 16, 17, 24, 25, 26};
        for (int slot : myCourtSlots) {
            buttons[slot] = new ItemButton(slot, myCourtIcon, p -> this.openCustomGamesGUI(p));
        }
        
        // Section 4: Profile (slots 27-29, 36-38, 45-47)
        ItemStack profileIcon = new ItemStack(Material.PAPER);
        profileIcon.editMeta(meta -> {
            meta.setCustomModelData(1111);
            meta.displayName(Component.text("§6Profile"));
            meta.lore(List.of(Component.text("§7View your stats & settings")));
        });
        int[] profileSlots = {27, 28, 29, 36, 37, 38, 45, 46, 47};
        for (int slot : profileSlots) {
            buttons[slot] = new ItemButton(slot, profileIcon, p -> this.openPlayerProfileGUI(p));
        }
        
        // Section 5: Server Store (slots 30-32, 39-41, 48-50)
        ItemStack storeIcon = new ItemStack(Material.PAPER);
        storeIcon.editMeta(meta -> {
            meta.setCustomModelData(1111);
            meta.displayName(Component.text("§6Server Store"));
            meta.lore(List.of(Component.text("§7Visit our Store")));
        });
        int[] storeSlots = {30, 31, 32, 39, 40, 41, 48, 49, 50};
        for (int slot : storeSlots) {
            buttons[slot] = new ItemButton(slot, storeIcon, p -> p.sendMessage("§aVisit our Store: §Coming Soon!"));
        }
        
        // Section 6: Discord (slots 33-35, 42-44, 51-53)
        ItemStack discordIcon = new ItemStack(Material.PAPER);
        discordIcon.editMeta(meta -> {
            meta.setCustomModelData(1111);
            meta.displayName(Component.text("§6Discord"));
            meta.lore(List.of(Component.text("§7Get our discord link")));
        });
        int[] discordSlots = {33, 34, 35, 42, 43, 44, 51, 52, 53};
        for (int slot : discordSlots) {
            buttons[slot] = new ItemButton(slot, discordIcon, p -> p.sendMessage("§aJoin our Discord: §https://discord.gg/yra3gjNRpD"));
        }
        
        new GUI(":offset_-1::menu:", 6, false, buttons).openInventory(player);
    }

    private void registerFramed(ItemButton[] btns, int centreSlot, ItemStack icon, Consumer<Player> click) {
        int[] ring;
        for (int off : ring = new int[]{-10, -9, -8, -1, 1, 8, 9, 10}) {
            int slot = centreSlot + off;
            if (slot < 0 || slot >= btns.length) continue;
            btns[slot] = new ItemButton(slot, FILLER, p -> {
            });
        }
        btns[centreSlot] = new ItemButton(centreSlot, icon, click);
    }

    private ItemStack createIcon(Material mat, Component name, String... lore) {
        return Items.get(name, mat, 1, lore);
    }

    public void openCustomGamesGUI(Player player) {
        int slot;
        List<BasketballGame> games = Hub.basketballLobby.getGames().stream().filter(g -> g.owner != null).toList();
        int rows = 6;
        ItemButton[] buttons = new ItemButton[54];
        for (slot = 0; slot < buttons.length; ++slot) {
            buttons[slot] = new ItemButton(slot, FILLER, p -> {});
        }
        for (slot = 0; slot < Math.min(45, games.size()); ++slot) {
            BasketballGame g2 = games.get(slot);
            buttons[slot] = new ItemButton(slot, Items.getPlayerHead(g2.owner), pl -> {
                Athlete a = AthleteManager.get(pl.getUniqueId());
                g2.join(a);
                g2.joinTeam(pl, GoalGame.Team.SPECTATOR);
            });
        }

        // ✅ REMOVED VIP REQUIREMENT - MyCourts now FREE for everyone!
        buttons[47] = new ItemButton(47, Items.get(
                Component.text("Create MyCourt").color(Colour.partix()),
                Material.EMERALD, 1,
                " ",
                "§a§lFREE for everyone!",
                "§6§lCLICK TO CREATE!"
        ), pla -> {
            // Check if player already has a game
            if (games.stream().anyMatch(g -> g.owner != null && g.owner.equals(pla.getUniqueId()))) {
                pla.playSound(pla.getLocation(), Sound.ENTITY_ARMOR_STAND_BREAK, SoundCategory.MASTER, 100.0f, 1.0f);
                pla.sendMessage(Message.alreadyCreatedGame());
                return;
            }

            this.attemptJoin(AthleteManager.get(pla.getUniqueId()), athletes -> {
                BasketballGame g = Hub.basketballLobby.findAvailableGame(true);
                if (g == null) {
                    pla.sendMessage("§cNo custom courts available right now!");
                    pla.playSound(pla.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.MASTER, 1.0f, 1.0f);
                    pla.closeInventory();
                    return;
                }
                g.owner = pla.getUniqueId();
                g.join(athletes);
                CosmeticSound sound = AthleteManager.get(pla.getUniqueId()).getGreenSound();
                pla.sendMessage(sound != null ? "§aGreen Sound in use: " + sound.getName() : "§cNo Green Sound equipped! Using default.");
            });
        });

        buttons[49] = new ItemButton(49, Items.get(
                Component.text("Create Court").color(Colour.partix()),
                Material.ENDER_EYE, 1,
                " ",
                "§6§lFREE for everyone!",
                "§6§lCLICK TO CREATE!"
        ), pla -> {
            ItemButton[] sizeBtns = new ItemButton[27];
            for (int i = 0; i < sizeBtns.length; ++i) {
                sizeBtns[i] = new ItemButton(i, Items.get(Component.text(" "), Material.BLACK_STAINED_GLASS_PANE, 1, " "), p -> {});
            }
            sizeBtns[11] = new ItemButton(11, Items.get(
                    Component.text("3v3 Arena").color(Colour.partix()),
                    Material.SLIME_BALL, 1,
                    "§7Create a 3v3 Arena"
            ), click -> {
                Hub.basketballLobby.createRandomDefaultArenaGame(pla);
                pla.closeInventory();
            });
            sizeBtns[15] = new ItemButton(15, Items.get(
                    Component.text("4v4 Arena").color(Colour.partix()),
                    Material.SLIME_BALL, 1,
                    "§7Create a 4v4 Arena"
            ), click -> {
                Hub.basketballLobby.createRandomDefaultArenaGame4v4(pla);
                pla.closeInventory();
            });
            new GUI("Choose Arena Size", 3, false, sizeBtns).openInventory(pla);
        });

        buttons[51] = new ItemButton(51, Items.get(
                Component.text("Arena Selector").color(Colour.partix()),
                Material.DIAMOND, 1,
                "§7Browse stadium folders"
        ), pla -> Hub.basketballLobby.openArenaCategorySelectorGUI(pla));

        new GUI("Custom Games > Basketball", 6, false, buttons).openInventory(player);
    }

    private void attemptJoin(Athlete athlete, Consumer<Athlete[]> join) {
        if (athlete.getParty() < 0) {
            join.accept(new Athlete[]{athlete});
        } else {
            Party party = PartyFactory.get(athlete.getParty());
            if (party.leader.equals(athlete.getPlayer().getUniqueId())) {
                join.accept(party.toList().toArray(this.getAthletes().toArray(new Athlete[0])));
            } else {
                athlete.getPlayer().sendMessage(Message.onlyPartyLeader());
            }
        }
    }

    @Override
    public void clickItem(Player player, ItemStack itemStack) {
        Athlete athlete = AthleteManager.get(player.getUniqueId());
        if (athlete == null) {
            return;
        }
        if (itemStack.getType() == Material.NETHER_STAR) {
            this.openServerSelectorGUI(player);
            return;
        }
        // In MainLobby.java clickItem() method
        if (itemStack.getType().equals(Material.PLAYER_HEAD)) {
            // Open profile instead of showing "coming soon"
            this.openPlayerProfileGUI(player);
            return;
        }
        if (itemStack.getType().equals(Material.KNOWLEDGE_BOOK)) {
            new GUI(PlayerDb.getName(player.getUniqueId()) + "'s Statistics", 3, false, new ItemButton(11, Items.get(Component.text("Basketball").color(Colour.partix()), Material.SLIME_BALL, 1, "§r§fMVPs: §e" + BasketballDb.get(player.getUniqueId(), BasketballDb.Stat.MVP), "§r§7Games Won: §e" + BasketballDb.get(player.getUniqueId(), BasketballDb.Stat.WINS), "§r§7Games Lost: §e" + BasketballDb.get(player.getUniqueId(), BasketballDb.Stat.LOSSES), "§r§7Total Points: §e" + BasketballDb.get(player.getUniqueId(), BasketballDb.Stat.POINTS), "§r§7Total Threes: §e" + BasketballDb.get(player.getUniqueId(), BasketballDb.Stat.THREES)), p -> {
            }), new ItemButton(13, Items.get(Component.text("Click").color(Colour.partix()), Material.BARRIER), p -> p.sendMessage("Coming soon")), new ItemButton(15, Items.get(Component.text("Coming Soon").color(Colour.partix()), Material.BARRIER), p -> p.sendMessage("Coming soon"))).openInventory(player);
        }
        if (itemStack.getType().equals(Material.BEACON)) {
            new GUI("Custom Games > Select Type", 3, true,
                    new ItemButton(11, Items.get(Component.text("Basketball").color(Colour.partix()), Material.SLIME_BALL), p -> {
                        List<BasketballGame> games = Hub.basketballLobby.getGames().stream().filter(g -> g.owner != null).toList();
                        ItemButton[] buttons = new ItemButton[49];

                        if (!games.isEmpty()) {
                            for (int x = 0; x < 45; ++x) {
                                if (x >= games.size()) continue;
                                BasketballGame g2 = games.get(x);
                                buttons[x] = new ItemButton(x, Items.getPlayerHead(g2.owner), pl -> {
                                    Athlete gameAthlete = AthleteManager.get(pl.getUniqueId());
                                    g2.join(gameAthlete);
                                    g2.joinTeam(pl, GoalGame.Team.SPECTATOR);
                                });
                            }
                        }

                        // ✅ REMOVED VIP REQUIREMENT - MyCourts now FREE!
                        buttons[46] = new ItemButton(47, Items.get(
                                Component.text("Create MyCourt").color(Colour.partix()),
                                Material.EMERALD, 1,
                                " ",
                                "§a§lFREE for everyone!",
                                "§6§lCLICK TO CREATE!"
                        ), pla -> {
                            if (games.stream().anyMatch(g -> g.owner != null && g.owner.equals(pla.getUniqueId()))) {
                                pla.playSound(pla.getLocation(), Sound.ENTITY_ARMOR_STAND_BREAK, SoundCategory.MASTER, 100.0f, 1.0f);
                                pla.sendMessage(Message.alreadyCreatedGame());
                                return;
                            }
                            this.attemptJoin(AthleteManager.get(pla.getUniqueId()), athletes -> {
                                BasketballGame game = Hub.basketballLobby.findAvailableGame(true);
                                if (game == null) {
                                    pla.sendMessage("§cNo custom courts available right now! Please try again later.");
                                    pla.playSound(pla.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.MASTER, 1.0f, 1.0f);
                                    pla.closeInventory();
                                    return;
                                }
                                game.owner = pla.getUniqueId();
                                game.join(athletes);
                                pla.sendMessage(Message.purchaseSuccess("Custom Game Server", 0));
                            });
                        });

                        buttons[47] = new ItemButton(49, Items.get(
                                Component.text("Create Court").color(Colour.partix()),
                                Material.ENDER_EYE, 1,
                                " ",
                                "§6§lFREE for everyone!",
                                "§6§lCLICK TO CREATE!"
                        ), pla -> new GUI("Choose Arena Size", 3, false,
                                new ItemButton(11, Items.get(
                                        Component.text("3v3 Arena").color(Colour.partix()),
                                        Material.SLIME_BALL, 1,
                                        "§7Create a 3v3 Arena"
                                ), click -> {
                                    Hub.basketballLobby.createRandomDefaultArenaGame(pla);
                                    pla.closeInventory();
                                }),
                                new ItemButton(15, Items.get(
                                        Component.text("4v4 Arena").color(Colour.partix()),
                                        Material.SLIME_BALL, 1,
                                        "§7Create a 4v4 Arena"
                                ), click -> {
                                    Hub.basketballLobby.createRandomDefaultArenaGame4v4(pla);
                                    pla.closeInventory();
                                })
                        ).openInventory(pla));

                        buttons[48] = new ItemButton(51, Items.get(
                                Component.text("Arena Selector").color(Colour.partix()),
                                Material.DIAMOND, 1,
                                "§fChoose a basketball arena"
                        ), pla -> Hub.basketballLobby.openArenaSelectionGUI(pla));

                        new GUI("Custom Games > Basketball", 6, false, buttons).openInventory(player);
                    }),
                    new ItemButton(13, Items.get(Component.text("Coming Soon").color(Colour.partix()), Material.BARRIER), p -> p.sendMessage("Coming soon")),
                    new ItemButton(15, Items.get(Component.text("Coming Soon").color(Colour.partix()), Material.BARRIER), p -> p.sendMessage("Coming soon"))
            ).openInventory(player);
        }
        if (itemStack.getType().equals(Material.EMERALD)) {
            new GUI("Daily Item Shop | " + ItemShop.getTimeRemaining(), 5, true, new ItemButton(11, ItemShop.defaultTrail.getGUIItem(), p -> {
                CosmeticHolder holder = ItemShop.defaultTrail;
                int cost = holder.getRarity().getCost();
                PlayerDb.get(player.getUniqueId(), PlayerDb.Stat.COINS).thenAccept(coins -> {
                    if (coins >= cost) {
                        Perm.add(player, holder.getPermission());
                        PlayerDb.remove(player.getUniqueId(), PlayerDb.Stat.COINS, cost);
                        player.sendMessage(Message.purchaseSuccess(holder.getName(), holder.getRarity().getCost()));
                        player.playSound(player.getLocation(), holder.getRarity().equals(CosmeticRarity.LEGENDARY) ? Sound.UI_TOAST_CHALLENGE_COMPLETE : Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1.0f, 1.0f);
                        player.closeInventory();
                    } else {
                        player.sendMessage(Message.needCoins(cost - coins));
                        player.playSound(player.getLocation(), Sound.ENTITY_ARMOR_STAND_BREAK, SoundCategory.MASTER, 1.0f, 1.0f);
                    }
                });
            }), new ItemButton(13, ItemShop.defaultExplosion.getGUIItem(), p -> {
                CosmeticHolder holder = ItemShop.defaultExplosion;
                int cost = holder.getRarity().getCost();
                PlayerDb.get(player.getUniqueId(), PlayerDb.Stat.COINS).thenAccept(coins -> {
                    if (coins >= cost) {
                        Perm.add(player, holder.getPermission());
                        PlayerDb.remove(player.getUniqueId(), PlayerDb.Stat.COINS, cost);
                        player.sendMessage(Message.purchaseSuccess(holder.getName(), holder.getRarity().getCost()));
                        player.playSound(player.getLocation(), holder.getRarity().equals(CosmeticRarity.LEGENDARY) ? Sound.UI_TOAST_CHALLENGE_COMPLETE : Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1.0f, 1.0f);
                        player.closeInventory();
                    } else {
                        player.sendMessage(Message.needCoins(cost - coins));
                        player.playSound(player.getLocation(), Sound.ENTITY_ARMOR_STAND_BREAK, SoundCategory.MASTER, 1.0f, 1.0f);
                    }
                });
            }), new ItemButton(15, ItemShop.defaultBorder.getGUIItem(), p -> {
                PlayerDb.get(player.getUniqueId(), PlayerDb.Stat.COINS).thenAccept(coins -> {
                    Integer greenSoundId;
                    Athlete clickedAthlete = AthleteManager.get(p.getUniqueId());
                    CosmeticHolder holder = ItemShop.defaultBorder;
                    int cost = holder.getRarity().getCost();
                    if (!p.hasPermission(holder.getPermission())) {
                        if (coins >= cost) {
                            Perm.add(p, holder.getPermission());
                            PlayerDb.remove(p.getUniqueId(), PlayerDb.Stat.COINS, cost);
                            p.sendMessage(Message.purchaseSuccess(holder.getName(), holder.getRarity().getCost()));
                        } else {
                            p.sendMessage(Message.needCoins(cost - coins));
                            p.playSound(p.getLocation(), Sound.ENTITY_ARMOR_STAND_BREAK, SoundCategory.MASTER, 1.0f, 1.0f);
                            return;
                        }
                    }
                    if ((greenSoundId = Cosmetics.greenSounds.entrySet().stream().filter(entry -> entry.getValue().equals(holder)).map(Map.Entry::getKey).findFirst().orElse(null)) == null) {
                        p.sendMessage("❌ Sound selection failed. Please report this issue.");
                        return;
                    }
                    CosmeticSound selectedSound = Cosmetics.greenSounds.get(greenSoundId);
                    if (selectedSound == null) {
                        p.sendMessage("❌ Could not find the selected sound. Please try again.");
                        return;
                    }
                    clickedAthlete.setGreenSound(selectedSound);
                    p.sendMessage("✅ You have equipped the **" + selectedSound.getName() + "** Green Sound");
                    p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.MASTER, 1.0f, 1.0f);
                    if (clickedAthlete.getPlace() instanceof GoalGame) {
                        ((GoalGame) clickedAthlete.getPlace()).updateArmor();
                    }
                    p.closeInventory();
                });
            }), new ItemButton(37, ItemShop.vipTrail.getGUIItem(), p -> {
                PlayerDb.get(player.getUniqueId(), PlayerDb.Stat.COINS).thenAccept(coins -> {
                    CosmeticHolder holder = ItemShop.vipTrail;
                    int cost = holder.getRarity().getCost();
                    if (player.hasPermission("rank.vip")) {
                        if (coins >= cost) {
                            Perm.add(player, holder.getPermission());
                            PlayerDb.remove(player.getUniqueId(), PlayerDb.Stat.COINS, cost);
                            player.sendMessage(Message.purchaseSuccess(holder.getName(), holder.getRarity().getCost()));
                            player.playSound(player.getLocation(), holder.getRarity().equals(CosmeticRarity.LEGENDARY) ? Sound.UI_TOAST_CHALLENGE_COMPLETE : Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1.0f, 1.0f);
                            player.closeInventory();
                        } else {
                            player.sendMessage(Message.needCoins(cost - coins));
                            player.playSound(player.getLocation(), Sound.ENTITY_ARMOR_STAND_BREAK, SoundCategory.MASTER, 1.0f, 1.0f);
                        }
                    } else {
                        player.sendMessage(Message.needVIP());
                        player.playSound(player.getLocation(), Sound.ENTITY_ARMOR_STAND_BREAK, SoundCategory.MASTER, 1.0f, 1.0f);
                    }
                });
            }), new ItemButton(38, ItemShop.vipExplosion.getGUIItem(), p -> {
                PlayerDb.get(player.getUniqueId(), PlayerDb.Stat.COINS).thenAccept(coins -> {
                    CosmeticHolder holder = ItemShop.vipExplosion;
                    int cost = holder.getRarity().getCost();
                    if (player.hasPermission("rank.vip")) {
                        if (coins >= cost) {
                            Perm.add(player, holder.getPermission());
                            PlayerDb.remove(player.getUniqueId(), PlayerDb.Stat.COINS, cost);
                            player.sendMessage(Message.purchaseSuccess(holder.getName(), holder.getRarity().getCost()));
                            player.playSound(player.getLocation(), holder.getRarity().equals(CosmeticRarity.LEGENDARY) ? Sound.UI_TOAST_CHALLENGE_COMPLETE : Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1.0f, 1.0f);
                            player.closeInventory();
                        } else {
                            player.sendMessage(Message.needCoins(cost - coins));
                            player.playSound(player.getLocation(), Sound.ENTITY_ARMOR_STAND_BREAK, SoundCategory.MASTER, 1.0f, 1.0f);
                        }
                    } else {
                        player.sendMessage(Message.needVIP());
                        player.playSound(player.getLocation(), Sound.ENTITY_ARMOR_STAND_BREAK, SoundCategory.MASTER, 1.0f, 1.0f);
                    }
                });
            }), new ItemButton(39, ItemShop.vipBorder.getGUIItem(), p -> {
                PlayerDb.get(player.getUniqueId(), PlayerDb.Stat.COINS).thenAccept(coins -> {
                    CosmeticHolder holder = ItemShop.vipBorder;
                    int cost = holder.getRarity().getCost();
                    if (player.hasPermission("rank.vip")) {
                        if (coins >= cost) {
                            Perm.add(player, holder.getPermission());
                            PlayerDb.remove(player.getUniqueId(), PlayerDb.Stat.COINS, cost);
                            player.sendMessage(Message.purchaseSuccess(holder.getName(), holder.getRarity().getCost()));
                            player.playSound(player.getLocation(), holder.getRarity().equals(CosmeticRarity.LEGENDARY) ? Sound.UI_TOAST_CHALLENGE_COMPLETE : Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1.0f, 1.0f);
                            player.closeInventory();
                        } else {
                            player.sendMessage(Message.needCoins(cost - coins));
                            player.playSound(player.getLocation(), Sound.ENTITY_ARMOR_STAND_BREAK, SoundCategory.MASTER, 1.0f, 1.0f);
                        }
                    } else {
                        player.sendMessage(Message.needVIP());
                        player.playSound(player.getLocation(), Sound.ENTITY_ARMOR_STAND_BREAK, SoundCategory.MASTER, 1.0f, 1.0f);
                    }
                });
            }), new ItemButton(41, ItemShop.proTrail.getGUIItem(), p -> {
                PlayerDb.get(player.getUniqueId(), PlayerDb.Stat.COINS).thenAccept(coins -> {
                    CosmeticHolder holder = ItemShop.proTrail;
                    int cost = holder.getRarity().getCost();
                    if (player.hasPermission("rank.pro")) {
                        if (coins >= cost) {
                            Perm.add(player, holder.getPermission());
                            PlayerDb.remove(player.getUniqueId(), PlayerDb.Stat.COINS, cost);
                            player.sendMessage(Message.purchaseSuccess(holder.getName(), holder.getRarity().getCost()));
                            player.playSound(player.getLocation(), holder.getRarity().equals(CosmeticRarity.LEGENDARY) ? Sound.UI_TOAST_CHALLENGE_COMPLETE : Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1.0f, 1.0f);
                            player.closeInventory();
                        } else {
                            player.sendMessage(Message.needCoins(cost - coins));
                            player.playSound(player.getLocation(), Sound.ENTITY_ARMOR_STAND_BREAK, SoundCategory.MASTER, 1.0f, 1.0f);
                        }
                    } else {
                        player.sendMessage(Message.needPRO());
                        player.playSound(player.getLocation(), Sound.ENTITY_ARMOR_STAND_BREAK, SoundCategory.MASTER, 1.0f, 1.0f);
                    }
                });
            }), new ItemButton(42, ItemShop.proExplosion.getGUIItem(), p -> {
                PlayerDb.get(player.getUniqueId(), PlayerDb.Stat.COINS).thenAccept(coins -> {
                    CosmeticHolder holder = ItemShop.proExplosion;
                    int cost = holder.getRarity().getCost();
                    if (player.hasPermission("rank.pro")) {
                        if (coins >= cost) {
                            Perm.add(player, holder.getPermission());
                            PlayerDb.remove(player.getUniqueId(), PlayerDb.Stat.COINS, cost);
                            player.sendMessage(Message.purchaseSuccess(holder.getName(), holder.getRarity().getCost()));
                            player.playSound(player.getLocation(), holder.getRarity().equals(CosmeticRarity.LEGENDARY) ? Sound.UI_TOAST_CHALLENGE_COMPLETE : Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1.0f, 1.0f);
                            player.closeInventory();
                        } else {
                            player.sendMessage(Message.needCoins(cost - coins));
                            player.playSound(player.getLocation(), Sound.ENTITY_ARMOR_STAND_BREAK, SoundCategory.MASTER, 1.0f, 1.0f);
                        }
                    } else {
                        player.sendMessage(Message.needPRO());
                        player.playSound(player.getLocation(), Sound.ENTITY_ARMOR_STAND_BREAK, SoundCategory.MASTER, 1.0f, 1.0f);
                    }
                });
            }), new ItemButton(43, ItemShop.proBorder.getGUIItem(), p -> {
                PlayerDb.get(player.getUniqueId(), PlayerDb.Stat.COINS).thenAccept(coins -> {
                    CosmeticHolder holder = ItemShop.proBorder;
                    int cost = holder.getRarity().getCost();
                    if (player.hasPermission("rank.pro")) {
                        if (coins >= cost) {
                            Perm.add(player, holder.getPermission());
                            PlayerDb.remove(player.getUniqueId(), PlayerDb.Stat.COINS, cost);
                            player.sendMessage(Message.purchaseSuccess(holder.getName(), holder.getRarity().getCost()));
                            player.playSound(player.getLocation(), holder.getRarity().equals(CosmeticRarity.LEGENDARY) ? Sound.UI_TOAST_CHALLENGE_COMPLETE : Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1.0f, 1.0f);
                            player.closeInventory();
                        } else {
                            player.sendMessage(Message.needCoins(cost - coins));
                            player.playSound(player.getLocation(), Sound.ENTITY_ARMOR_STAND_BREAK, SoundCategory.MASTER, 1.0f, 1.0f);
                        }
                    } else {
                        player.sendMessage(Message.needPRO());
                        player.playSound(player.getLocation(), Sound.ENTITY_ARMOR_STAND_BREAK, SoundCategory.MASTER, 1.0f, 1.0f);
                    }
                });
            })).openInventory(player);
        } else if (itemStack.getType().equals(Material.ENDER_CHEST)) {
            new CosmeticGUI(player);
        }
    }

    private String formatLocation(Location location) {
        return String.format("X: %.1f, Y: %.1f, Z: %.1f", location.getX(), location.getY(), location.getZ());
    }

    public void createGameInStadium(Player player, Stadium stadium) {
        if (stadium == null) {
            player.sendMessage("§cInvalid stadium.");
            return;
        }
        Location location = stadium.getLocation();
        if (location == null) {
            player.sendMessage("§cStadium location is null.");
            return;
        }
        if (this.games.containsKey(location)) {
            player.sendMessage("§cA game is already in progress at this stadium!");
            return;
        }
        BasketballGame game = new BasketballGame(this.customSettings, location, 26.0, 2.8, 0.45, 0.475, 0.575);
        this.games.put(location, game);
        game.owner = player.getUniqueId();
        Athlete athlete = AthleteManager.get(player.getUniqueId());
        game.join(athlete);
        game.joinTeam(player, GoalGame.Team.HOME);
        this.equipPlayerForGame(player, true);
        player.sendMessage("§aGame created at stadium: §e" + stadium.getName());
    }

    private void equipPlayerForGame(Player player, boolean b) {
        PlayerInventory inventory = player.getInventory();
        inventory.clear();
        inventory.setItem(0, Items.get(Message.itemName("Team Selector", "key.use", player), Material.COMPASS));
        inventory.setItem(1, Items.get(Message.itemName("Scoreboard Viewer", "key.use", player), Material.KNOWLEDGE_BOOK));
        player.sendMessage("§aYou have been equipped with game items!");
    }

    private boolean canUseArenaSelector(Player player) {
        return player.hasPermission("rank.admin") || player.hasPermission("rank.pro");
    }
}

