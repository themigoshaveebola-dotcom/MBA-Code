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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
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
            this.updateBossBar("§6§lMinecraft Basketball Association §7§l> §f§lSeason 3");
        } else {
            this.updateBossBar("§e§lSUPPORT THE SERVER! §7§l> §f§lhttps:/minecraftbasketball.tebex.io/");
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
            
            // Season Pass Info
            String seasonPassProgress = me.x_tias.partix.plugin.seasonpass.SeasonPassManager.getTierProgress(player.getUniqueId());
            
            Bukkit.getScheduler().runTask(Partix.getInstance(), () ->
                    Sidebar.set(player, Component.text("  MBA  ").color(Colour.partix()).decorate(TextDecoration.BOLD), " ", "§c§lYour Info  ", "  §fName: §b" + player.getName(), "  §fRank: " + rankPrefix, "  §fVer: §b" + (player.getName().startsWith(".") ? "Bedrock" : "Java"), "     ", "§f§lYour Stats  ", "  §fCoins: §e" + coins, "  §fMBA Bucks: §a" + mbaBucks, "        ", "§9§lThis Season  ", "  §fDiv: §e" + div, "  §fPts: §a" + points, "  §fPass: " + seasonPassProgress, "                     ", "§7§.")
            );
        });
    }

    @Override
    public void onJoin(Athlete... athletes) {
        for (Athlete athlete : athletes) {
            Player player = athlete.getPlayer();
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 1000000, 5, false, false));
            player.teleport(new Location(Bukkit.getWorlds().getFirst(), -17.5, 1.0, 173.5, 180.0f, 0.0f));
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
        i.setItem(1, Items.get(Message.itemName("Season Pass", "key.use", player), Material.ENCHANTED_BOOK));
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.ENCHANTED_BOOK && event.getAction().toString().contains("RIGHT_CLICK")) {
            me.x_tias.partix.plugin.seasonpass.SeasonPassManager.openSeasonPassGUI(player);
        }
    }

    public void openServerSelectorGUI(Player player) {
        int rows = 6;
        int size = 54;
        ItemButton[] buttons = new ItemButton[54];
        for (int i = 0; i < 54; ++i) {
            buttons[i] = new ItemButton(i, FILLER, p -> {
            });
        }
        ItemStack rankedIcon = Items.get(Component.text("Ranked Queue").color(Colour.partix()), Material.DIAMOND_SWORD, 1, "§7Join the Ranked Queue");
        rankedIcon.editMeta(m -> m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES));
        this.registerFramed(buttons, 10, rankedIcon, p -> Hub.basketballLobby.openGameSelectorGUI(p));
        this.registerFramed(buttons, 13, this.createIcon(Material.SLIME_BALL, Component.text("Custom Games").color(Colour.partix()), "§7Select a custom game"), this::openCustomGamesGUI);
        ItemStack profileHead = new ItemStack(Material.PLAYER_HEAD);
        profileHead.editMeta(meta -> {
            SkullMeta sm = (SkullMeta) meta;
            sm.setOwningPlayer(player);
            sm.displayName(Component.text("Profile").color(Colour.partix()));
            sm.lore(List.of(Component.text("§7View your stats & settings")));
        });
        this.registerFramed(buttons, 16, profileHead, p -> p.sendMessage("§eProfile feature coming soon!"));
        this.registerFramed(buttons, 37, this.createIcon(Material.WRITABLE_BOOK, Component.text("Discord").color(Colour.partix()), "§7Get our Discord link"), p -> p.sendMessage("§aJoin our Discord: §https://discord.gg/yra3gjNRpD"));
        this.registerFramed(buttons, 40, this.createIcon(Material.EMERALD, Component.text("Server Store").color(Colour.partix()), "§7Visit our Store"), p -> p.sendMessage("§aVisit our Store: §Coming Soon"));
        this.registerFramed(buttons, 43, this.createIcon(Material.FIREWORK_STAR, Component.text("Cosmetics").color(Colour.partix()), "§7Open Cosmetics Menu"), p -> new CosmeticGUI(p));
        new GUI("Server Selector", 6, false, buttons).openInventory(player);
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
            buttons[slot] = new ItemButton(slot, FILLER, p -> {
            });
        }
        for (slot = 0; slot < Math.min(45, games.size()); ++slot) {
            BasketballGame g2 = games.get(slot);
            buttons[slot] = new ItemButton(slot, Items.getPlayerHead(g2.owner), pl -> {
                Athlete a = AthleteManager.get(pl.getUniqueId());
                g2.join(a);
                g2.joinTeam(pl, GoalGame.Team.SPECTATOR);
            });
        }
        buttons[47] = new ItemButton(47, Items.get(Component.text("Create MyCourt").color(Colour.partix()), Material.EMERALD, 1, " ", "§6Requires: §aVIP Rank", "§6§lCLICK TO CREATE!"), pla -> {
            if (!pla.hasPermission("rank.vip")) {
                pla.sendMessage("§cYou must be VIP to create a MyCourt!");
                pla.playSound(pla.getLocation(), Sound.ENTITY_VILLAGER_NO, SoundCategory.MASTER, 1.0f, 1.0f);
                return;
            }
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
        buttons[49] = new ItemButton(49, Items.get(Component.text("Create Court").color(Colour.partix()), Material.ENDER_EYE, 1, " ", "§6§lFREE for everyone!", "§6§lCLICK TO CREATE!"), pla -> {
            ItemButton[] sizeBtns = new ItemButton[27];
            for (int i = 0; i < sizeBtns.length; ++i) {
                sizeBtns[i] = new ItemButton(i, Items.get(Component.text(" "), Material.BLACK_STAINED_GLASS_PANE, 1, " "), p -> {
                });
            }
            sizeBtns[11] = new ItemButton(11, Items.get(Component.text("3v3 Arena").color(Colour.partix()), Material.SLIME_BALL, 1, "§7Create a 3v3 Arena"), click -> {
                Hub.basketballLobby.createRandomDefaultArenaGame(pla);
                pla.closeInventory();
            });
            sizeBtns[15] = new ItemButton(15, Items.get(Component.text("4v4 Arena").color(Colour.partix()), Material.SLIME_BALL, 1, "§7Create a 4v4 Arena"), click -> {
                Hub.basketballLobby.createRandomDefaultArenaGame4v4(pla);
                pla.closeInventory();
            });
            new GUI("Choose Arena Size", 3, false, sizeBtns).openInventory(pla);
        });
        buttons[51] = new ItemButton(51, Items.get(Component.text("Arena Selector").color(Colour.partix()), Material.DIAMOND, 1, "§7Browse stadium folders"), pla -> Hub.basketballLobby.openArenaCategorySelectorGUI(pla));
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
        if (itemStack.getType().equals(Material.KNOWLEDGE_BOOK)) {
            new GUI(PlayerDb.getName(player.getUniqueId()) + "'s Statistics", 3, false, new ItemButton(11, Items.get(Component.text("Basketball").color(Colour.partix()), Material.SLIME_BALL, 1, "§r§fMVPs: §e" + BasketballDb.get(player.getUniqueId(), BasketballDb.Stat.MVP), "§r§7Games Won: §e" + BasketballDb.get(player.getUniqueId(), BasketballDb.Stat.WINS), "§r§7Games Lost: §e" + BasketballDb.get(player.getUniqueId(), BasketballDb.Stat.LOSSES), "§r§7Total Points: §e" + BasketballDb.get(player.getUniqueId(), BasketballDb.Stat.POINTS), "§r§7Total Threes: §e" + BasketballDb.get(player.getUniqueId(), BasketballDb.Stat.THREES)), p -> {
            }), new ItemButton(13, Items.get(Component.text("Coming Soon").color(Colour.partix()), Material.BARRIER), p -> p.sendMessage("Coming soon")), new ItemButton(15, Items.get(Component.text("Coming Soon").color(Colour.partix()), Material.BARRIER), p -> p.sendMessage("Coming soon"))).openInventory(player);
        }
        if (itemStack.getType().equals(Material.BEACON)) {
            new GUI("Custom Games > Select Type", 3, true, new ItemButton(11, Items.get(Component.text("Basketball").color(Colour.partix()), Material.SLIME_BALL), p -> {
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
                int normalCost = Hub.basketballLobby.getGames().size() > 2 ? 999999999 : 0;
                buttons[46] = new ItemButton(47, Items.get(Component.text("Create MyCourt").color(Colour.partix()), Material.EMERALD, 1, " ", "§6Requires: §aVIP Rank", "§6§lCLICK TO CREATE!"), pla -> {
                    if (!pla.hasPermission("rank.vip")) {
                        pla.sendMessage("§cYou must be VIP to create a MyCourt!");
                        pla.playSound(pla.getLocation(), Sound.ENTITY_VILLAGER_NO, SoundCategory.MASTER, 1.0f, 1.0f);
                        return;
                    }
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
                buttons[47] = new ItemButton(49, Items.get(Component.text("Create Court").color(Colour.partix()), Material.ENDER_EYE, 1, " ", "§6§lFREE for everyone!", "§6§lCLICK TO CREATE!"), pla -> new GUI("Choose Arena Size", 3, false, new ItemButton(11, Items.get(Component.text("3v3 Arena").color(Colour.partix()), Material.SLIME_BALL, 1, "§7Create a 3v3 Arena"), click -> {
                    Hub.basketballLobby.createRandomDefaultArenaGame(pla);
                    pla.closeInventory();
                }), new ItemButton(15, Items.get(Component.text("4v4 Arena").color(Colour.partix()), Material.SLIME_BALL, 1, "§7Create a 4v4 Arena"), click -> {
                    Hub.basketballLobby.createRandomDefaultArenaGame4v4(pla);
                    pla.closeInventory();
                })).openInventory(pla));
                buttons[48] = new ItemButton(51, Items.get(Component.text("Arena Selector").color(Colour.partix()), Material.DIAMOND, 1, "§fChoose a basketball arena"), pla -> Hub.basketballLobby.openArenaSelectionGUI(pla));
                new GUI("Custom Games > Basketball", 6, false, buttons).openInventory(player);
            }), new ItemButton(13, Items.get(Component.text("Coming Soon").color(Colour.partix()), Material.BARRIER), p -> p.sendMessage("Coming soon")), new ItemButton(15, Items.get(Component.text("Coming Soon").color(Colour.partix()), Material.BARRIER), p -> p.sendMessage("Coming soon"))).openInventory(player);
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

