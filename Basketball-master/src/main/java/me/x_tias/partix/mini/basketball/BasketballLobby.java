/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.Sound
 *  org.bukkit.SoundCategory
 *  org.bukkit.World
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.player.PlayerInteractEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.plugin.Plugin
 */
package me.x_tias.partix.mini.basketball;

import co.aikar.commands.ACFBukkitUtil;
import lombok.Getter;
import me.x_tias.partix.Partix;
import me.x_tias.partix.database.BasketballDb;
import me.x_tias.partix.database.SeasonDb;
import me.x_tias.partix.mini.factories.Hub;
import me.x_tias.partix.mini.game.GoalGame;
import me.x_tias.partix.plugin.athlete.Athlete;
import me.x_tias.partix.plugin.athlete.AthleteManager;
import me.x_tias.partix.plugin.gui.GUI;
import me.x_tias.partix.plugin.gui.ItemButton;
import me.x_tias.partix.plugin.party.Party;
import me.x_tias.partix.plugin.party.PartyFactory;
import me.x_tias.partix.plugin.settings.*;
import me.x_tias.partix.server.specific.Lobby;
import me.x_tias.partix.util.Colour;
import me.x_tias.partix.util.Items;
import me.x_tias.partix.util.Message;
import net.kyori.adventure.text.Component;
import me.x_tias.partix.database.PlayerDb;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class BasketballLobby
        extends Lobby implements Listener {
    private static final int REC_TEAM_SIZE = 4;
    private static final int[] STADIUM_SLOTS = new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
    private static final ItemStack FILLER_PANE = Items.get(Component.text(" "), Material.BLACK_STAINED_GLASS_PANE, 1, " ");
    private final List<Location> maps = new ArrayList<>();
    private final List<Location> arenas = new ArrayList<>();
    private final HashMap<Location, BasketballGame> games = new HashMap<>();
    private final HashMap<Athlete, Double> skill = new HashMap<>();
    private final Map<Location, UUID> stadiumOwners = new HashMap<>();
    private final HashMap<Location, String> arenaNames = new HashMap<>();
    private final HashMap<String, Material> arenaDisplayBlocks = new HashMap<>();
    private final HashMap<UUID, Team> teamStorage = new HashMap<>();
    private final List<Team> waitingTeams = new ArrayList<>();
    private final List<Athlete> waitingExtras = new ArrayList<>();
    private final List<Athlete> queue4v4 = new ArrayList<>();
    private final List<Athlete> queue2v2 = new ArrayList<>();
    private final List<Athlete> queue3v3 = new ArrayList<>();
    private final List<Athlete> queue1v1 = new ArrayList<>();
    private final List<Location> myCourts = new ArrayList<>();
    private final List<Location> rankedCourts = new ArrayList<>();
    private final List<Location> rankedHalfCourts = new ArrayList<>();
    @Getter
    private final List<Location> defaultArenas = new ArrayList<>();
    private final List<Stadium> stadiums = new ArrayList<>();
    private final List<Location> recCourts = new ArrayList<>();
    private final List<Location> fourVfourDefaultCourts = new ArrayList<>();
    private final List<Stadium> mbaStadiums = new ArrayList<>();
    private final List<Stadium> mcaaStadiums = new ArrayList<>();
    private final List<Stadium> retroStadiums = new ArrayList<>();
    private final Settings gameSettings;
    private final Settings customSettings = new Settings(WinType.TIME_5, GameType.MANUAL, WaitType.MEDIUM, CompType.CASUAL, 2, false, false, false, 4, GameEffectType.NONE);
    private final Settings recSettings = new Settings(WinType.TIME_5, GameType.AUTOMATIC, WaitType.MEDIUM, CompType.RANKED, 4, false, false, false, 2, GameEffectType.NONE);
    @Getter
    private final List<Location> customArenas = new ArrayList<>();
    private final List<Stadium> customStadiums = new ArrayList<>();
    private final HashMap<UUID, Integer> playerQueueNotifier = new HashMap<>();
    private final int countdown = 170;
    
    // 2K Park-style physical queue spots
    private final List<CourtQueue> twosCourts = new ArrayList<>();
    private final List<CourtQueue> threesCourts = new ArrayList<>();
    private BukkitTask particleTask;
    private final HashMap<UUID, CourtSpot> playerSpots = new HashMap<>();
    private final HashMap<Location, CourtQueue> gameCourtMapping = new HashMap<>(); // Track which court a game is on

    public BasketballLobby() {
        // Initialize ranked game settings for 2v2/3v3 - First to 21 with win by 2
        this.gameSettings = new Settings(
                WinType.FIRST_TO,      // Changed from TIME_5
                GameType.AUTOMATIC,
                WaitType.SHORT,
                CompType.RANKED,
                2,                     // playersPerTeam
                true,                  // Shot clock enabled
                false,
                false,
                1,
                GameEffectType.NONE
        );

        // Set to 21 points with win by 2 (just like 1v1)
        this.gameSettings.winType.amount = 21;
        this.gameSettings.winType.winByTwo = true;
        this.rankedCourts.add(new Location(Bukkit.getWorlds().getFirst(), 42.5, -61, 196.5));
        this.rankedCourts.add(new Location(Bukkit.getWorlds().getFirst(), -24.5, -61, 196.5));
        this.rankedCourts.add(new Location(Bukkit.getWorlds().getFirst(), -24.5, -61, 111.5));
        this.rankedCourts.add(new Location(Bukkit.getWorlds().getFirst(), 42.5, -61, 111.5));

        this.rankedHalfCourts.add(new Location(Bukkit.getWorlds().getFirst(), 145.5, -61, 384));


        this.myCourts.add(new Location(Bukkit.getWorlds().getFirst(), 285.5, -60, -509.5));
        this.myCourts.add(new Location(Bukkit.getWorlds().getFirst(), -226.5, -60, -509.5));
        this.myCourts.add(new Location(Bukkit.getWorlds().getFirst(), 798.5, -60, 3.5));
        this.myCourts.add(new Location(Bukkit.getWorlds().getFirst(), 798.5, -60, 472.5));
        this.myCourts.add(new Location(Bukkit.getWorlds().getFirst(), 798.5, -60, 963.5));


//        this.myCourts.add(new Location(Bukkit.getWorlds().getFirst(), 98.5, 0.0, 263.5));
//        this.myCourts.add(new Location(Bukkit.getWorlds().getFirst(), 230.5, 0.0, 259.5));
//        this.myCourts.add(new Location(Bukkit.getWorlds().getFirst(), 248.5, 0.0, 356.5));
//        this.myCourts.add(new Location(Bukkit.getWorlds().getFirst(), 117.5, 0.0, 362.5));
//        this.rankedCourts.add(new Location(Bukkit.getWorlds().getFirst(), -285.5, 31.0, 103.5));
//        this.rankedCourts.add(new Location(Bukkit.getWorlds().getFirst(), -296.5, 27.0, 163.5));
//        this.rankedCourts.add(new Location(Bukkit.getWorlds().getFirst(), -340.5, 0.0, 8.5));
//        this.rankedCourts.add(new Location(Bukkit.getWorlds().getFirst(), -91.5, 0.0, 50.5));
//        this.rankedCourts.add(new Location(Bukkit.getWorlds().getFirst(), 57.5, 0.0, 50.5));
//        this.rankedCourts.add(new Location(Bukkit.getWorlds().getFirst(), 248.5, 0.0, 65.5));
//        this.rankedCourts.add(new Location(Bukkit.getWorlds().getFirst(), 379.5, 0.0, 65.5));
//        this.rankedCourts.add(new Location(Bukkit.getWorlds().getFirst(), 509.5, 0.0, 65.5));
//        this.recCourts.add(new Location(Bukkit.getWorlds().getFirst(), -122.5, 0.0, 294.5));
//        this.recCourts.add(new Location(Bukkit.getWorlds().getFirst(), -126.5, 0.0, 381.5));
//        this.recCourts.add(new Location(Bukkit.getWorlds().getFirst(), -130.5, 0.0, 471.5));
//        this.defaultArenas.add(new Location(Bukkit.getWorlds().getFirst(), 401.5, 0.0, 295.5));
//        this.defaultArenas.add(new Location(Bukkit.getWorlds().getFirst(), 400.5, 0.0, 411.5));
//        this.defaultArenas.add(new Location(Bukkit.getWorlds().getFirst(), 528.5, 0.0, 297.5));
//        this.fourVfourDefaultCourts.add(new Location(Bukkit.getWorlds().getFirst(), -283.5, 0.0, 390.5));
//        this.fourVfourDefaultCourts.add(new Location(Bukkit.getWorlds().getFirst(), -283.5, 0.0, 467.5));

        this.addCustomStadium("§l§9Washington §cWithers", Material.BLUE_GLAZED_TERRACOTTA,
                new Location(Bukkit.getWorlds().getFirst(), 448.5, -60, 969.5), Stadium.Category.MBA);

        this.addCustomStadium("§l§cPhiladelphia §964s", Material.RED_GLAZED_TERRACOTTA,
                new Location(Bukkit.getWorlds().getFirst(), -42.5, -60, 969.5), Stadium.Category.MBA);

        this.addCustomStadium("§l§cChicago §0Bows", Material.RED_GLAZED_TERRACOTTA,
                new Location(Bukkit.getWorlds().getFirst(), -532.5, -60, 969.5), Stadium.Category.MBA);

        this.addCustomStadium("§l§0Brooklyn §fBuckets", Material.BLACK_GLAZED_TERRACOTTA,
                new Location(Bukkit.getWorlds().getFirst(), -532.5, -60, 458.5), Stadium.Category.MBA);

        this.addCustomStadium("§l§4Miami §6Magma Cubes", Material.RED_GLAZED_TERRACOTTA,
                new Location(Bukkit.getWorlds().getFirst(), -576.5, -60, -504.5), Stadium.Category.MBA);

        this.addCustomStadium("§l§9Golden State §6Guardians", Material.BLUE_GLAZED_TERRACOTTA,
                new Location(Bukkit.getWorlds().getFirst(), -576.5, -60, -13.5), Stadium.Category.MBA);

        this.addCustomStadium("§l§cAtlanta §9Allays", Material.RED_GLAZED_TERRACOTTA,
                new Location(Bukkit.getWorlds().getFirst(), 576.5, -60, -504.5), Stadium.Category.MBA);

        this.addCustomStadium("§l§5LA §6Creepers", Material.PURPLE_GLAZED_TERRACOTTA,
                new Location(Bukkit.getWorlds().getFirst(), 576.5, -60, -13.5), Stadium.Category.MBA);

        this.addCustomStadium("§l§9Boston §fBreeze", Material.CYAN_GLAZED_TERRACOTTA,
                new Location(Bukkit.getWorlds().getFirst(), 190.5, -60, -1000.5), Stadium.Category.MBA);
        this.maps.addAll(this.myCourts);
        
        // Initialize 2K Park-style queue spots
        initializePhysicalQueueSpots();
        
        // Start particle spawning task
        startParticleTask();
        
        // Register this as an event listener for PlayerMoveEvent and PlayerToggleSneakEvent
        Bukkit.getPluginManager().registerEvents(this, Partix.getInstance());
        Bukkit.getLogger().info("[Park Queue] Registered BasketballLobby as event listener");
    }
    private void addArena(Location location, String arenaName, Material displayBlock) {
        this.arenas.add(location);
        this.arenaNames.put(location, arenaName);
        this.arenaDisplayBlocks.put(arenaName, displayBlock);
    }

    private Stadium getStadiumAtLocation(Location location) {
        for (Stadium stadium : this.customStadiums) {
            Location stadiumLoc = stadium.getLocation();

            if (stadiumLoc.getWorld().equals(location.getWorld()) &&
                    Math.abs(stadiumLoc.getX() - location.getX()) < 1.0 &&
                    Math.abs(stadiumLoc.getY() - location.getY()) < 1.0 &&
                    Math.abs(stadiumLoc.getZ() - location.getZ()) < 1.0) {
                return stadium;
            }
        }
        return null;
    }

    public BasketballGame findAvailableRecCourt() {
        ArrayList<Location> freeRecCourts = new ArrayList<>(this.recCourts);
        freeRecCourts.removeAll(this.games.keySet());
        if (freeRecCourts.isEmpty()) {
            return null;
        }
        Location randomLoc = freeRecCourts.get(new Random().nextInt(freeRecCourts.size()));
        BasketballGame game = new BasketballGame(this.recSettings, randomLoc, 26.0, 2.8, 0.45, 0.475, 0.575);
        this.games.put(randomLoc, game);
        return game;
    }

    public BasketballGame findAvailableHalfCourt() {
        ArrayList<Location> freeHalfCourts = new ArrayList<>(this.rankedHalfCourts);
        freeHalfCourts.removeAll(this.games.keySet());

        if (freeHalfCourts.isEmpty()) {
            return null;
        }

        Location randomLoc = freeHalfCourts.get(new Random().nextInt(freeHalfCourts.size()));

        // CREATE CUSTOM SETTINGS FOR 1V1: FIRST TO 21 WITH WIN BY 2
        Settings oneVOneSettings = new Settings(
                WinType.FIRST_TO,        // Use FIRST_TO enum (not TIME_5)
                GameType.AUTOMATIC,
                WaitType.SHORT,
                CompType.RANKED,
                1,                       // playersPerTeam = 1 (THIS IS THE KEY!)
                true,                    // Shot clock enabled (12 seconds)
                false,
                false,
                1,
                GameEffectType.NONE
        );

        // Set first to 21 with win by 2 rule
        oneVOneSettings.winType.amount = 21;
        oneVOneSettings.winType.winByTwo = true;

        // CREATE GAME WITH 13.0 DISTANCE AND 1 PLAYER PER TEAM
        BasketballGame game = new BasketballGame(
                oneVOneSettings,    // Custom 1v1 settings
                randomLoc,
                13.0,               // Half court distance
                2.8,
                0.45,
                0.475,
                0.575
        );

        // VERIFY THE FLAG IS SET
        if (!game.isHalfCourt1v1) {
            Bukkit.getLogger().warning("ERROR: 1v1 game flag not set! Check settings.playersPerTeam");
        } else {
            Bukkit.getLogger().info("✓ 1V1 HALFCOURT GAME CREATED - Flag is TRUE");
        }

        this.games.put(randomLoc, game);
        return game;
    }

    public void createRandomDefaultArenaGame4v4(Player player) {
        ArrayList<Location> freeList = new ArrayList<>(this.fourVfourDefaultCourts);
        freeList.removeAll(this.games.keySet());
        if (freeList.isEmpty()) {
            player.sendMessage("§cNo free 4v4 default arenas available right now!");
            return;
        }
        Location randomLoc = freeList.get(new Random().nextInt(freeList.size()));
        BasketballGame game = new BasketballGame(this.customSettings, randomLoc, 26.0, 2.8, 0.45, 0.475, 0.575);
        game.owner = player.getUniqueId();
        this.games.put(randomLoc, game);
        Athlete athlete = AthleteManager.get(player.getUniqueId());
        game.join(athlete);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.AMBIENT, 1.0f, 1.0f);
    }

    public void openArenaSelectionGUI(Player player) {
        ArrayList<Location> allArenas = new ArrayList<>(this.arenas);
        int guiSize = ((allArenas.size() - 1) / 9 + 1) * 9;
        ItemButton[] buttons = new ItemButton[allArenas.size()];
        for (int i = 0; i < allArenas.size(); ++i) {
            Location arena = allArenas.get(i);
            boolean isOccupied = this.games.containsKey(arena);
            String arenaName = this.arenaNames.getOrDefault(arena, "Unnamed Arena");
            Material displayBlock = isOccupied ? Material.BARRIER : this.arenaDisplayBlocks.getOrDefault(arenaName, Material.STONE);
            ItemStack arenaItem = Items.get(Component.text(arenaName).color(Colour.partix()), displayBlock, 1, isOccupied ? "§cThis arena is currently occupied." : "§aClick to join this arena!");
            buttons[i] = new ItemButton(i, arenaItem, clicker -> {
                if (isOccupied) {
                    player.sendMessage("§cThis arena is already in use. Please select another.");
                } else {
                    this.createGameInArena(player, arena);
                }
            });
        }
        new GUI("Select an Arena", guiSize / 9, false, buttons).openInventory(player);
    }

    public void createRandomDefaultArenaGame(Player player) {
        ArrayList<Location> freeList = new ArrayList<>(Hub.basketballLobby.getDefaultArenas());
        freeList.removeAll(Hub.basketballLobby.getGames().stream().map(BasketballGame::getLocation).toList());
        if (freeList.isEmpty()) {
            player.sendMessage("§cNo free default arenas available right now!");
            return;
        }
        Location randomLoc = freeList.get(new Random().nextInt(freeList.size()));
        BasketballGame game = new BasketballGame(this.customSettings, randomLoc, 26.0, 2.8, 0.45, 0.475, 0.575);
        game.owner = player.getUniqueId();
        Hub.basketballLobby.getGamesMap().put(randomLoc, game);
        Athlete athlete = AthleteManager.get(player.getUniqueId());
        game.join(athlete);
        player.sendMessage("§aYour Default Arena has been created at: " + randomLoc);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.AMBIENT, 1.0f, 1.0f);
    }

    public BasketballGame findAvailableRankedCourt() {
        ArrayList<Location> freeRanked = new ArrayList<>(this.rankedCourts);
        freeRanked.removeAll(this.games.keySet());
        if (freeRanked.isEmpty()) {
            return null;
        }
        Location randomLoc = freeRanked.get(new Random().nextInt(freeRanked.size()));
        BasketballGame game = new BasketballGame(this.gameSettings, randomLoc, 26.0, 2.8, 0.45, 0.475, 0.575);
        this.games.put(randomLoc, game);
        return game;
    }

    public HashMap<Location, BasketballGame> getGamesMap() {
        return this.games;
    }

    public void openArenaCategorySelectorGUI(Player player) {
        // Check if player has Coach rank for MBA arenas
        boolean hasCoachRank = player.hasPermission("basketball.mba.create") || player.hasPermission("rank.coach");

        ItemButton[] btn = new ItemButton[27];
        for (int i = 0; i < btn.length; ++i) {
            btn[i] = new ItemButton(i, FILLER_PANE, p -> {});
        }

        BiConsumer<Integer, Stadium.Category> cat = (slot, category) -> {
            Material iconMat = switch (category) {
                case Stadium.Category.MBA -> Material.NETHER_STAR;
                case Stadium.Category.MCAA -> Material.ENDER_EYE;
                case Stadium.Category.RETRO -> Material.CLOCK;
            };

            Component title = Component.text(switch (category) {
                case Stadium.Category.MBA -> "MBA Stadiums";
                case Stadium.Category.MCAA -> "MCAA Stadiums";
                case Stadium.Category.RETRO -> "Retro Stadiums";
            }).color(Colour.partix());

            // MBA restriction logic
            if (category == Stadium.Category.MBA && !hasCoachRank) {
                // Show locked MBA button
                btn[slot] = new ItemButton(slot,
                        Items.get(title, Material.BARRIER, 1,
                                "§c§l⚠ Coach Rank Required!",
                                "§7MBA arenas require §e§lCoach §7rank",
                                "§7to create games.",
                                " ",
                                "§7Ask a staff member for access."),
                        p -> {
                            p.sendMessage("§c§l⚠ MBA Arenas Restricted!");
                            p.sendMessage("§cYou need §e§lCoach §crank to access MBA arenas.");
                            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, SoundCategory.MASTER, 1.0f, 1.0f);
                        });
            } else {
                // Show normal button (unlocked or non-MBA)
                String loreText = (category == Stadium.Category.MBA) ? "§a§lCoach Access! §7Browse MBA stadiums" : "§7Browse this folder";
                btn[slot] = new ItemButton(slot, Items.get(title, iconMat, 1, loreText),
                        p -> this.openStadiumFolderGUI(p, category));
            }
        };

        cat.accept(11, Stadium.Category.MBA);
        cat.accept(13, Stadium.Category.MCAA);
        cat.accept(15, Stadium.Category.RETRO);
        new GUI("Arena Selector", 3, false, btn).openInventory(player);
    }

    public void openStadiumFolderGUI(Player player, Stadium.Category category) {
        List<Stadium> list;
        switch (category) {
            default: {
                throw new IncompatibleClassChangeError();
            }
            case MBA: {
                list = this.mbaStadiums;
                break;
            }
            case MCAA: {
                list = this.mcaaStadiums;
                break;
            }
            case RETRO: {
                list = this.retroStadiums;
            }
        }
        if (list.isEmpty()) {
            player.sendMessage("§cNo stadiums in this folder yet!");
            return;
        }
        ItemButton[] btn = new ItemButton[45];
        for (int i = 0; i < btn.length; ++i) {
            btn[i] = new ItemButton(i, FILLER_PANE, p -> {
            });
        }
        int shown = Math.min(STADIUM_SLOTS.length, list.size());
        for (int idx = 0; idx < shown; ++idx) {
            Stadium s = list.get(idx);
            boolean occupied = s.hasActiveGame();
            ItemStack icon = Items.get(Component.text(s.getName()).color(Colour.partix()), occupied ? Material.BARRIER : s.getBlock(), 1, occupied ? "§cOccupied – try later" : "§aClick to create game");
            int slot = STADIUM_SLOTS[idx];
            btn[slot] = new ItemButton(slot, icon, clicker -> {
                if (occupied) {
                    clicker.sendMessage("§cThat stadium is already running a game!");
                    return;
                }
                this.createGameInStadium(clicker, s);
                clicker.closeInventory();
            });
        }
        if (list.size() > STADIUM_SLOTS.length) {
            player.sendMessage("§eOnly the first 28 stadiums are shown in this page.");
        }
        new GUI("§8" + category.name() + " Stadiums", 5, false, btn).openInventory(player);
    }

    private void createBasketballGame(Player player, Location loc, int courtDistance) {
        if (this.games.containsKey(loc)) {
            player.sendMessage("§cA game is already in progress at this arena!");
            return;
        }
        BasketballGame game = new BasketballGame(this.customSettings, loc, courtDistance, 2.8, 0.45, 0.475, 0.575);
        this.games.put(loc, game);
        Athlete athlete = AthleteManager.get(player.getUniqueId());
        game.owner = player.getUniqueId();
        game.join(athlete);
        athlete.setSpectator(true);
        player.getInventory().clear();
        player.getActivePotionEffects().clear();
        player.getInventory().setItem(7, Items.get(Component.text("Game Settings").color(Colour.partix()), Material.CHEST));
        player.getInventory().setItem(8, Items.get(Component.text("Change Team").color(Colour.partix()), Material.GRAY_DYE));
        player.teleport(loc.clone().add(0.0, 12.0, 0.0));
        player.sendMessage(Message.joinTeam("spectators"));
        String niceName = this.arenaNames.getOrDefault(loc, "Unnamed Arena");
        player.sendMessage("§aGame created in arena: §e" + niceName);
    }

    public void createGameInArena(Player player, Location location) {
        this.createBasketballGame(player, location, 26);
    }

    public void createGameAtLocation(Player player, Location location) {
        if (this.games.containsKey(location)) {
            player.sendMessage("§cA game is already in progress at this arena!");
            return;
        }
        BasketballGame game = new BasketballGame(this.customSettings, location, 26.0, 2.8, 0.45, 0.475, 0.575);
        this.games.put(location, game);
        Athlete athlete = AthleteManager.get(player.getUniqueId());
        game.owner = player.getUniqueId();
        game.join(athlete);
        game.joinTeam(player, GoalGame.Team.SPECTATOR);
        this.equipPlayerForGame(player);
        player.teleport(location);
        player.sendMessage("§aYou have successfully joined arena: " + ACFBukkitUtil.formatLocation(location));
    }

    @Override
    public void clickItem(Player player, ItemStack itemStack) {
        Athlete athlete = AthleteManager.get(player.getUniqueId());
        if (athlete == null) {
            return;
        }

        if (itemStack.getType() == Material.NETHER_STAR) {
            Bukkit.getScheduler().runTaskLater(Partix.getInstance(), () -> Hub.basketballLobby.openGameSelectorGUI(player), 1L);
        }

        // ===== GAME SETTINGS WITH MBA-ONLY COACH RESTRICTION =====
        if (itemStack.getType() == Material.CHEST) {
            // Find player's current game
            BasketballGame game = this.games.values().stream()
                    .filter(g -> g.getPlayers().contains(player))
                    .findFirst()
                    .orElse(null);

            if (game == null) {
                player.sendMessage("§cYou must be in a game to access settings!");
                return;
            }

            Location gameLoc = game.getLocation();

            // Method 1: Check using Stadium.Category (RECOMMENDED)
            Stadium stadium = getStadiumAtLocation(gameLoc);
            boolean isMBA = (stadium != null && stadium.getCategory() == Stadium.Category.MBA);

            // OR Method 2: Check using mbaStadiums list
            // boolean isMBA = this.isMBAStadium(gameLoc);

            // OR Method 3: Check using hardcoded coordinates (fastest)
            // boolean isMBA = this.isMBAStadiumHardcoded(gameLoc);

            if (isMBA) {
                // MBA ARENA - Require Coach permission
                if (!player.hasPermission("basketball.gamesettings.mba") && !player.hasPermission("rank.coach")) {
                    player.sendMessage("§c§l⚠ MBA Arenas Only!");
                    player.sendMessage("§cYou need §e§lCoach §crank to access game settings in MBA arenas.");
                    player.sendMessage("§7Ask a staff member for the Coach rank.");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, SoundCategory.MASTER, 1.0f, 1.0f);
                    return;
                }

                // Player has Coach permission - allow access
                player.sendMessage("§a§lCoach Access Granted!");
                game.openTeamManagementGUI(player);  // ✅ FIXED
            } else {
                // NON-MBA COURT - Allow everyone (or add custom permission check here)
                player.sendMessage("§7Opening game settings...");
                game.openTeamManagementGUI(player);  // ✅ FIXED
            }
        }
    }


    public void addCustomStadium(String name, Material block, Location loc, Stadium.Category folder) {
        Stadium s = new Stadium(name, block, loc, folder);
        this.customStadiums.add(s);
        switch (folder) {
            case MBA: {
                this.mbaStadiums.add(s);
                break;
            }
            case MCAA: {
                this.mcaaStadiums.add(s);
                break;
            }
            case RETRO: {
                this.retroStadiums.add(s);
            }
        }
    }

    public void addCustomStadium(String name, Material block, Location loc) {
        this.addCustomStadium(name, block, loc, Stadium.Category.MBA);
    }

    public List<Stadium> getCustomStadiums() {
        Bukkit.getLogger().info("Returning " + this.customStadiums.size() + " custom stadiums.");
        return new ArrayList<>(this.customStadiums);
    }
    public List<Location> getMyCourts() {
        return this.myCourts;
    }
    public void saveStadiums(File file) {
        YamlConfiguration config = new YamlConfiguration();
        for (int i = 0; i < this.customStadiums.size(); ++i) {
            Stadium stadium = this.customStadiums.get(i);
            String path = "stadiums." + i;
            config.set(path + ".name", stadium.getName());
            config.set(path + ".block", stadium.getBlock().name());
            config.set(path + ".world", stadium.getLocation().getWorld().getName());
            config.set(path + ".x", stadium.getLocation().getX());
            config.set(path + ".y", stadium.getLocation().getY());
            config.set(path + ".z", stadium.getLocation().getZ());
        }
        try {
            config.save(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadStadiums(File file) {
        if (!file.exists()) {
            Bukkit.getLogger().warning("Stadiums file does not exist: " + file.getAbsolutePath());
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (!config.contains("stadiums")) {
            Bukkit.getLogger().warning("No stadiums found in configuration.");
            return;
        }
        for (String key : config.getConfigurationSection("stadiums").getKeys(false)) {
            String name = config.getString("stadiums." + key + ".name");
            Material block = Material.valueOf(config.getString("stadiums." + key + ".block"));
            World world = Bukkit.getWorld(config.getString("stadiums." + key + ".world"));
            double x = config.getDouble("stadiums." + key + ".x");
            double y = config.getDouble("stadiums." + key + ".y");
            double z = config.getDouble("stadiums." + key + ".z");
            if (world == null) {
                Bukkit.getLogger().warning("Invalid world for stadium: " + name);
                continue;
            }
            Location location = new Location(world, x, y, z);
            this.addCustomStadium(name, block, location);
            Bukkit.getLogger().info("Loaded stadium: " + name + " at " + location);
        }
    }

    public List<BasketballGame> getGames() {
        return new ArrayList<>(this.games.values().stream().toList());
    }

    @Override
    public void onTick() {
        this.games.values().forEach(BasketballGame::onTick);
        
        if (this.countdown > 5) {
            if (this.isReady()) {
                if (this.countdown > 70) {
                    this.updateBossBar("§ePreparing to Generate Teams..", Math.min(1.0, Math.max(0.0, (double) this.countdown / 170.0)));
                } else if (this.countdown > 30) {
                    this.updateBossBar("§6Starting Team Generation..", Math.min(1.0, Math.max(0.0, (double) this.countdown / 170.0)));
                } else {
                    this.updateBossBar("§fGenerating.. §7" + this.waitingExtras.get(new Random().nextInt(this.waitingExtras.size())).getPlayer().getName() + ", " + this.waitingExtras.get(new Random().nextInt(this.waitingExtras.size())).getPlayer().getName() + " vs " + this.waitingExtras.get(new Random().nextInt(this.waitingExtras.size())).getPlayer().getName() + ", " + this.waitingExtras.get(new Random().nextInt(this.waitingExtras.size())).getPlayer().getName());
                }
            } else {
                this.updateBossBar("§cWaiting for more Players..", 1.0);
            }
        }
        this.cleanGames();
    }

    private void cleanGames() {
        new HashMap<>(this.games).forEach((location, game) -> {
            if (game.getPlayers().isEmpty()) {
                game.reset();
                this.games.remove(location);
            }
        });
    }

    public void cleanup() {
        for (BasketballGame game : this.games.values()) {
            game.reset();
        }
    }

    private boolean isReady() {
        int teams = this.waitingTeams.size();
        int extras = this.waitingExtras.size();
        int total = (int) Math.floor((double) extras / 2.0) + teams;
        return total > 1;
    }

    private boolean isMBAStadium(Location location) {
        // Check if location matches any MBA stadium
        for (Stadium stadium : this.mbaStadiums) {
            Location stadiumLoc = stadium.getLocation();

            // Compare locations (accounting for floating point precision)
            if (stadiumLoc.getWorld().equals(location.getWorld()) &&
                    Math.abs(stadiumLoc.getX() - location.getX()) < 1.0 &&
                    Math.abs(stadiumLoc.getY() - location.getY()) < 1.0 &&
                    Math.abs(stadiumLoc.getZ() - location.getZ()) < 1.0) {
                return true;
            }
        }
        return false;
    }

    private void joinMatch(BasketballGame game, Team home, Team away) {
        this.updateBossBar("§eSending to Match..", 0.0);
        for (Athlete athlete : home.getAthletes()) {
            game.join(athlete);
            game.joinTeam(athlete.getPlayer(), GoalGame.Team.HOME);
        }
        for (Athlete athlete : away.getAthletes()) {
            game.join(athlete);
            game.joinTeam(athlete.getPlayer(), GoalGame.Team.AWAY);
        }

        // For 1v1: Skip jump ball
        if (game instanceof BasketballGame) {
            BasketballGame bbGame = (BasketballGame) game;
            if (bbGame.isHalfCourt1v1) {
                bbGame.start1v1Game();
            } else {
                game.startCountdown(GoalGame.State.FACEOFF, 15);
            }
        } else {
            game.startCountdown(GoalGame.State.FACEOFF, 15);
        }
    }

    public BasketballGame findAvailableGame(boolean custom) {
        ArrayList<Location> a = new ArrayList<>(this.maps);
        a.removeAll(this.games.keySet());
        Optional<Location> possibleGame = a.stream().findFirst();
        if (possibleGame.isPresent()) {
            Location l = possibleGame.get();
            BasketballGame game = this.create(l, custom, false);
            this.games.put(l, game);
            return game;
        }
        return null;
    }

    public BasketballGame findAvailableArena() {
        ArrayList<Location> a = new ArrayList<>(this.arenas);
        a.removeAll(this.games.keySet());
        Optional<Location> possibleGame = a.stream().findFirst();
        if (possibleGame.isPresent()) {
            Location l = possibleGame.get();
            BasketballGame game = this.create(l, true, true);
            this.games.put(l, game);
            return game;
        }
        return null;
    }

    private BasketballGame create(Location l, boolean custom, boolean arena) {
        return new BasketballGame(custom ? this.customSettings : this.gameSettings, l, 26.0, 2.8, 0.45, 0.475, 0.575);
    }

    @Override
    public void onJoin(Athlete... athletes) {
        for (Athlete athlete : athletes) {
            Player player = athlete.getPlayer();
            UUID uuid = player.getUniqueId();

            // REMOVE ANY STALE QUEUE ENTRIES FROM PREVIOUS SESSION
            this.removePlayerFromAllQueues(uuid);
            this.playerQueueNotifier.remove(uuid);

            PlayerDb.create(uuid, player.getName());
            athlete.setSpectator(true);
            player.teleport(new Location(Bukkit.getWorlds().getFirst(), 0.5, -58.0, 0.5));
            this.generateSkill(athlete).thenAccept(skillValue -> this.skill.put(athlete, skillValue));
            this.waitingExtras.add(athlete);
        }
    }

    @Override
    public void onQuit(Athlete... athletes) {
        for (Athlete athlete : athletes) {
            Player player = athlete.getPlayer();
            if (player == null) continue;

            UUID uuid = player.getUniqueId();

            // Remove from ALL queues FIRST
            this.removePlayerFromAllQueues(uuid);
            this.removeCancelItem(player);
            this.playerQueueNotifier.remove(uuid);

            // Then handle game cleanup
            this.games.keySet().forEach(location -> {
                BasketballGame game = this.games.get(location);
                List<Player> players = game.getPlayers();
                if (this.stadiumOwners.getOrDefault(location, uuid).equals(uuid)) {
                    players.remove(player);
                    if (players.isEmpty()) {
                        this.stadiumOwners.remove(location);
                        this.games.remove(location);
                        Bukkit.getLogger().info("Stadium at " + location + " is now unclaimed.");
                    }
                }
            });

            // Update GUI for remaining players
            Bukkit.getScheduler().runTaskLater(Partix.getInstance(),
                    this::updateGameSelectorGUI, 5L);
        }
    }

    @Override
    public void giveItems(Player player) {
        player.getInventory().clear();
        player.getInventory().setItem(8, Items.get(Message.itemName("Return to Hub", "key.use", player), Material.RED_BED));
        player.sendMessage("§aYou have been equipped with the necessary items.");
    }

    private CompletableFuture<Double> generateSkill(Athlete athlete) {
        Player player = athlete.getPlayer();
        UUID uuid = player.getUniqueId();
        final CompletableFuture<Double> skillFuture = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(Partix.getInstance(), () -> {

            double seasonWins = SeasonDb.get(uuid, SeasonDb.Stat.WINS).join();
            double seasonLosses = SeasonDb.get(uuid, SeasonDb.Stat.LOSSES).join();
            double seasonPoints = SeasonDb.get(uuid, SeasonDb.Stat.POINTS).join();
            double sportWins = BasketballDb.get(uuid, BasketballDb.Stat.WINS).join();
            double sportLosses = BasketballDb.get(uuid, BasketballDb.Stat.LOSSES).join();
            double sportMVP = BasketballDb.get(uuid, BasketballDb.Stat.MVP).join();
            double seasonGames = seasonWins + seasonLosses;
            double sportGames = sportWins + sportLosses;
            double season = seasonPoints / (seasonGames * 3.0) * 0.3;
            double sport = sportWins / sportGames * 0.4;
            double mvp = sportMVP / sportGames * 0.3;
            double wins = Math.min(35.0, seasonWins) / 35.0 * 0.4;
            skillFuture.complete(Math.min(Math.max((season + sport + mvp + wins) * 10.0, 0.19) + Math.random() * 0.1, 9.99));
        });

        return skillFuture;
    }

    public void createCustomStadium(Location location, Player player) {
    }

    public void openCustomStadiumsGUI(Player player) {
        List<Stadium> stadiums = this.getCustomStadiums();
        if (stadiums.isEmpty()) {
            player.sendMessage("§cNo custom stadiums available.");
            return;
        }
        int[] availableSlots = new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
        ItemButton[] stadiumButtons = new ItemButton[stadiums.size()];
        for (int i = 0; i < stadiums.size(); ++i) {
            if (i >= availableSlots.length) {
                player.sendMessage("§cToo many stadiums! Only the first 28 will be shown.");
                break;
            }
            Stadium stadium = stadiums.get(i);
            ItemStack stadiumItem = Items.get(Component.text(stadium.getName()).color(Colour.partix()), stadium.getBlock(), 1, "§7Location: §e" + ACFBukkitUtil.formatLocation(stadium.getLocation()), "§aClick to create or join a game here!");
            stadiumButtons[i] = new ItemButton(availableSlots[i], stadiumItem, stadiumPlayer -> Hub.basketballLobby.createGameInStadium(stadiumPlayer, stadium.getName()));
        }
        new GUI("Select a Stadium", 5, false, stadiumButtons).openInventory(player);
    }

    public void createGameInStadium(Player player, Stadium stadium) {
        if (stadium == null) {
            player.sendMessage("§cInvalid stadium.");
            return;
        }
        Location loc = stadium.getLocation();
        this.arenaNames.put(loc, stadium.getName());
        this.createBasketballGame(player, loc, 26);
    }

    public void createGameInStadium(Player player, String name) {
        this.findStadiumByName(name).ifPresentOrElse(s -> this.createGameInStadium(player, s), () -> player.sendMessage("§cStadium not found: " + name));
    }

    private void equipPlayerForGame(Player player) {
        player.getInventory().clear();
        player.getInventory().addItem(Items.get(Component.text("Team Selector").color(Colour.partix()), Material.GRAY_DYE, 1, "§7Select your team"));
        player.getInventory().addItem(Items.get(Component.text("Game Settings").color(Colour.partix()), Material.CHEST, 1, "§7Manage game settings"));
        player.sendMessage("§aYou have been equipped with game management items.");
    }

    public void handleCustomStadiumInteraction(Player player, Stadium stadium) {
        Location location = stadium.getLocation();
        if (this.games.containsKey(location)) {
            BasketballGame game = this.games.get(location);
            Athlete athlete = AthleteManager.get(player.getUniqueId());
            game.join(athlete);
            game.joinTeam(player, GoalGame.Team.SPECTATOR);
            player.sendMessage("§aYou have joined the game at §e" + stadium.getName());
            player.teleport(location);
            return;
        }
        BasketballGame game = this.create(location, true, false);
        this.games.put(location, game);
        game.owner = player.getUniqueId();
        Athlete athlete = AthleteManager.get(player.getUniqueId());
        game.join(athlete);
        game.joinTeam(player, GoalGame.Team.HOME);
        game.setup(this.customSettings, location, 2.8, 0.45, 0.475, 0.575, 26.0);
        this.equipPlayerForGame(player);
        player.sendMessage("§aYou have created and joined a new game at stadium: §e" + stadium.getName());
        player.teleport(location);
    }

    public Optional<Stadium> findStadiumByName(String name) {
        return this.customStadiums.stream().filter(stadium -> stadium.getName().equalsIgnoreCase(name)).findFirst();
    }

    public void joinQueue(Player player, int mode) {
        int requiredPlayers;
        List<Athlete> queue;
        int partySize;
        UUID playerId = player.getUniqueId();
        Athlete athlete = AthleteManager.get(playerId);
        if (athlete == null) {
            athlete = AthleteManager.create(player);
            Bukkit.getLogger().warning("WARNING: Athlete was missing for " + player.getName() + ". Created new Athlete.");
        }
        if (this.isPlayerQueued(athlete)) {
            player.sendMessage("§cYou are already in a queue. Leave your current queue before joining another.");
            return;
        }
        
        // NEW: Check if player is on a physical queue spot
        if (playerSpots.containsKey(playerId)) {
            player.sendMessage("§c§l» §cYou're on a physical queue spot! Leave it first (shift).");
            return;
        }
        
        Party party = PartyFactory.get(athlete.getParty());
        int n = partySize = party != null ? party.count() : 1;
        if (partySize == 2 && mode != 2) {
            player.sendMessage("§cYour party has 2 players. You can only queue for §e2v2§c.");
            return;
        }
        if (partySize == 3 && mode != 3) {
            player.sendMessage("§cYour party has 3 players. You can only queue for §e3v3§c.");
            return;
        }
        if (partySize == 4 && mode != 4) {
            player.sendMessage("§cYour party has 4 players. You can only queue for §e4v4§c.");
            return;
        }
        if (partySize > 4) {
            player.sendMessage("§cYour party is too large. Maximum party size is 4 for matchmaking.");
            return;
        }
        if (mode == 1) {
            queue = this.queue1v1;
            requiredPlayers = 2;
        } else if (mode == 2) {
            queue = this.queue2v2;
            requiredPlayers = 4;
        } else if (mode == 3) {
            queue = this.queue3v3;
            requiredPlayers = 6;
        } else if (mode == 4) {
            queue = this.queue4v4;
            requiredPlayers = 8;
        } else {
            player.sendMessage("§cInvalid game mode.");
            return;
        }
        if (party != null && queue.size() + party.toList().size() > requiredPlayers) {
            player.sendMessage("§cYour party is too large to queue right now.");
            return;
        }
        if (party != null) {
            queue.addAll(party.toList());
        } else {
            queue.add(athlete);
        }
        Bukkit.getLogger().info("DEBUG: " + player.getName() + " joined " + mode + "v" + mode + " queue. Queue size: " + queue.size());
        player.sendMessage("§aYou have joined the queue for " + mode + "v" + mode + "!");
        player.closeInventory();
        this.updateGameSelectorGUI();
        if (queue.size() >= requiredPlayers) {
            this.startMatchmaking(queue, mode);
        }
    }

    private void sendQueueUpdate(Player player, int mode, int current, int required) {
        player.sendActionBar(Component.text("§eQueue: " + current + "/" + required + " for " + mode + "v" + mode));
    }

    private void startMatchmaking(List<Athlete> queue, int mode) {
        BasketballGame game;
        int requiredPlayers;
        if (mode == 1) {
            requiredPlayers = 2;
            game = this.findAvailableHalfCourt();
        } else if (mode == 2) {
            requiredPlayers = 4;
            game = this.findAvailableRankedCourt();
        } else if (mode == 3) {
            requiredPlayers = 6;
            game = this.findAvailableRankedCourt();
        } else if (mode == 4) {
            requiredPlayers = 8;
            game = this.findAvailableRecCourt();
        } else {
            return;
        }
        if (queue.size() < requiredPlayers) {
            return;
        }
        if (game == null) {
            queue.forEach(a -> a.getPlayer().sendMessage("§cNo available courts! Try again later."));
            return;
        }
        ArrayList<Athlete> selectedPlayers = new ArrayList<>(queue.subList(0, requiredPlayers));
        queue.removeAll(selectedPlayers);
        ArrayList<Athlete> team1 = new ArrayList<>();
        ArrayList<Athlete> team2 = new ArrayList<>();
        HashSet<UUID> assignedPlayers = new HashSet<>();
        for (Athlete athlete : selectedPlayers) {
            Party party;
            if (assignedPlayers.contains(athlete.getPlayer().getUniqueId()) || (party = athlete.getParty() > 0 ? PartyFactory.get(athlete.getParty()) : null) == null)
                continue;
            List<Athlete> partyMembers = party.toList();
            if (team1.size() <= team2.size()) {
                team1.addAll(partyMembers);
            } else {
                team2.addAll(partyMembers);
            }
            assignedPlayers.addAll(partyMembers.stream().map(p -> p.getPlayer().getUniqueId()).toList());
        }
        for (Athlete athlete : selectedPlayers) {
            if (assignedPlayers.contains(athlete.getPlayer().getUniqueId())) continue;
            if (team1.size() < requiredPlayers / 2) {
                team1.add(athlete);
            } else {
                team2.add(athlete);
            }
            assignedPlayers.add(athlete.getPlayer().getUniqueId());
        }
        if (team1.size() != team2.size()) {
            team1.clear();
            team2.clear();
            Collections.shuffle(selectedPlayers);
            for (int i = 0; i < selectedPlayers.size(); ++i) {
                if (i < requiredPlayers / 2) {
                    team1.add(selectedPlayers.get(i));
                    continue;
                }
                team2.add(selectedPlayers.get(i));
            }
        }
        for (Athlete a2 : team1) {
            game.join(a2);
            game.joinTeam(a2.getPlayer(), GoalGame.Team.HOME);
        }
        for (Athlete a2 : team2) {
            game.join(a2);
            game.joinTeam(a2.getPlayer(), GoalGame.Team.AWAY);
        }
// For 1v1: Skip jump ball and go straight to game start
        if (game instanceof BasketballGame) {
            BasketballGame bbGame = (BasketballGame) game;
            if (bbGame.isHalfCourt1v1) {
                bbGame.start1v1Game();  // Skip jump ball for 1v1
            } else {
                game.startCountdown(GoalGame.State.FACEOFF, 15);  // Normal jump ball for 2v2, 3v3, 4v4
            }
        } else {
            game.startCountdown(GoalGame.State.FACEOFF, 15);
        }
    }

    public void leaveQueue(Player player) {
        UUID playerId = player.getUniqueId();
        Athlete athlete = AthleteManager.get(playerId);

        if (athlete == null) {
            // Even if athlete is null, clean up by UUID
            this.removePlayerFromAllQueues(playerId);
            player.sendMessage("§cYou have left the queue.");
            return;
        }

        int partyId = athlete.getParty();
        Party party = partyId > 0 ? PartyFactory.get(partyId) : null;

        // Remove player by UUID
        this.removePlayerFromAllQueues(playerId);

        // If player is in a party, remove all party members from queues too
        if (party != null) {
            for (Athlete a : party.toList()) {
                if (a != null && a.getPlayer() != null) {
                    this.removePlayerFromAllQueues(a.getPlayer().getUniqueId());
                }
            }
        }

        Bukkit.getLogger().info("DEBUG: " + player.getName() + " left all queues.");
        player.sendMessage("§cYou have left the queue.");
        Bukkit.getScheduler().runTaskLater(Partix.getInstance(), this::updateGameSelectorGUI, 5L);
    }

    private void removeFromQueue(List<Athlete> queue, Athlete athlete) {
        queue.remove(athlete);
    }

    private void removeCancelItem(Player player) {
        if (player.getInventory().getItem(8) != null && player.getInventory().getItem(8).getType() == Material.BARRIER) {
            player.getInventory().setItem(8, null);
        }
    }

    public void updateGameSelectorGUI() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.getOpenInventory().getTitle().equals("§l§6Game Selector")) continue;
            this.openGameSelectorGUI(player);
        }
    }

    private void registerFramed(ItemButton[] buttons, int centreSlot, ItemStack icon, Consumer<Player> click) {
        int[] ring;
        for (int off : ring = new int[]{-10, -9, -8, -1, 1, 8, 9, 10}) {
            int slot = centreSlot + off;
            if (slot < 0 || slot >= buttons.length) continue;
            buttons[slot] = new ItemButton(slot, FILLER_PANE, p -> {
            });
        }
        buttons[centreSlot] = new ItemButton(centreSlot, icon, click);
    }

    private ItemStack icon(Material mat, Component name, String... lore) {
        return Items.get(name, mat, 1, lore);
    }

    public void openGameSelectorGUI(Player player) {
        int size = 27;
        ItemButton[] buttons = new ItemButton[27];
        
        // Create invisible/empty texture item (like main menu uses)
        ItemStack emptyTexture = new ItemStack(Material.PAPER);
        emptyTexture.editMeta(meta -> {
            meta.setCustomModelData(1111);  // Uses the "empty" item from ItemsAdder
            meta.displayName(Component.text(" "));
        });
        
        // Initialize all slots with invisible texture
        for (int i = 0; i < 27; ++i) {
            buttons[i] = new ItemButton(i, emptyTexture, p -> {});
        }
        
        // Section 1: 1v1 Mode (slots 0-2, 9-11, 18-20) - invisible but clickable
        Athlete athlete = AthleteManager.get(player.getUniqueId());
        boolean isQueued = this.isPlayerQueued(athlete);
        
        ItemStack mode1v1 = new ItemStack(Material.PAPER);
        mode1v1.editMeta(meta -> {
            meta.setCustomModelData(1111);  // Keep invisible
            if (isQueued) {
                meta.displayName(Component.text("§l§c⚔ 1v1 Mode §7(In Queue)"));
                meta.lore(List.of(
                    Component.text("§7Face off in a 1v1 duel!"),
                    Component.text(" "),
                    Component.text("§eQueue: " + this.queue1v1.size() + "/2 players"),
                    Component.text(" "),
                    Component.text("§cClick to leave queue")
                ));
            } else {
                meta.displayName(Component.text("§l§c⚔ 1v1 Mode"));
                meta.lore(List.of(
                    Component.text("§7Face off in a 1v1 duel!"),
                    Component.text(" "),
                    Component.text("§eQueue: " + this.queue1v1.size() + "/2 players")
                ));
            }
        });
        int[] slots1v1 = {0, 1, 2, 9, 10, 11, 18, 19, 20};
        for (int slot : slots1v1) {
            buttons[slot] = new ItemButton(slot, mode1v1, p -> {
                Athlete a = AthleteManager.get(p.getUniqueId());
                if (this.isPlayerQueued(a)) {
                    this.leaveQueue(p);
                } else {
                    this.joinQueue(p, 1);
                }
            });
        }
        
        // Section 2: 2v2 Mode (slots 3-5, 12-14, 21-23)
        ItemStack mode2v2 = new ItemStack(Material.PAPER);
        mode2v2.editMeta(meta -> {
            meta.setCustomModelData(1111);  // Keep invisible
            meta.displayName(Component.text("§l§62v2 Park Queue"));
            meta.lore(List.of(
                Component.text("§7Stand on a spot at a 2v2 court!"),
                Component.text("§7Find the courts with colored particles"),
                Component.text("§7§oGreen = Occupied, Red = Available")
            ));
        });
        int[] slots2v2 = {3, 4, 5, 12, 13, 14, 21, 22, 23};
        for (int slot : slots2v2) {
            buttons[slot] = new ItemButton(slot, mode2v2, p -> {
                p.sendMessage("§a§l» §aHead to the 2v2 courts and stand on a spot!");
                p.sendMessage("§7Courts are marked with particle effects.");
                p.closeInventory();
            });
        }
        
        // Section 3: 3v3 Mode (slots 6-8, 15-17, 24-26)
        ItemStack mode3v3 = new ItemStack(Material.PAPER);
        mode3v3.editMeta(meta -> {
            meta.setCustomModelData(1111);  // Keep invisible
            meta.displayName(Component.text("§l§b3v3 Park Queue"));
            meta.lore(List.of(
                Component.text("§7Stand on a spot at a 3v3 court!"),
                Component.text("§7Find the courts with colored particles"),
                Component.text("§7§oGreen = Occupied, Red = Available")
            ));
        });
        int[] slots3v3 = {6, 7, 8, 15, 16, 17, 24, 25, 26};
        for (int slot : slots3v3) {
            buttons[slot] = new ItemButton(slot, mode3v3, p -> {
                p.sendMessage("§a§l» §aHead to the 3v3 courts and stand on a spot!");
                p.sendMessage("§7Courts are marked with particle effects.");
                p.closeInventory();
            });
        }
        
        // Use font image in title like main menu
        new GUI(":ranked_hud:", 3, false, buttons).openInventory(player);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getItem() != null && event.getItem().getType() == Material.NETHER_STAR) {
            return;
        }
        event.setCancelled(true);
    }

    public void startQueueNotifier(Player player, int mode) {
        UUID playerId = player.getUniqueId();
        if (this.playerQueueNotifier.containsKey(playerId)) {
            return;
        }
        this.playerQueueNotifier.put(playerId, mode);
        Bukkit.getScheduler().runTaskTimer(Partix.getInstance(), () -> {
            if (!player.isOnline() || !this.playerQueueNotifier.containsKey(playerId)) {
                player.sendActionBar(Component.text(""));
                this.playerQueueNotifier.remove(playerId);
                Bukkit.getScheduler().cancelTasks(Partix.getInstance());
                return;
            }
            int queueSize = this.getQueueSizeForMode(mode);
            int requiredPlayers = this.getRequiredPlayersForMode(mode);
            player.sendActionBar(Component.text("§eQueue: " + queueSize + "/" + requiredPlayers + " players waiting..."));
        }, 0L, 20L);
    }


    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Remove by UUID instead of Athlete object reference
        this.removePlayerFromAllQueues(playerId);
        
        // Remove from physical queue spots
        this.removeFromPhysicalQueue(playerId);

        // Clean up notifier
        this.playerQueueNotifier.remove(playerId);

        // Update GUI for other players
        Bukkit.getScheduler().runTaskLater(Partix.getInstance(), this::updateGameSelectorGUI, 5L);
    }
    
    /**
     * Handle player sneaking to leave a spot
     */
    @EventHandler
    public void onPlayerSneak(org.bukkit.event.player.PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) {
            return; // Only handle when starting to sneak
        }
        
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        CourtSpot currentSpot = playerSpots.get(playerId);
        if (currentSpot != null) {
            // Player is on a spot and wants to leave
            currentSpot.vacate();
            playerSpots.remove(playerId);
            
            // Teleport player 2 blocks west (negative X direction)
            Location exitLocation = currentSpot.getLocation().clone();
            exitLocation.setX(exitLocation.getX() - 2.0); // 2 blocks west
            exitLocation.setY(exitLocation.getY() + 1.0); // 1 block up
            player.teleport(exitLocation);
            
            player.sendMessage("§c§l» §cYou left the queue spot!");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.MASTER, 1.0f, 0.5f);
        }
    }
    
    /**
     * Prevent queue spot blocks from being broken (allow creative mode)
     */
    @EventHandler
    public void onBlockBreak(org.bukkit.event.block.BlockBreakEvent event) {
        Player player = event.getPlayer();
        Location blockLoc = event.getBlock().getLocation();
        
        // Allow creative mode players to break blocks
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) {
            // Check if this is a queue spot block and mark it for re-placement
            for (CourtQueue court : twosCourts) {
                for (CourtSpot spot : court.getSpots()) {
                    if (isSameBlock(spot.getLocation(), blockLoc)) {
                        // Schedule block replacement after a short delay
                        Bukkit.getScheduler().runTaskLater(Partix.getInstance(), () -> {
                            spot.updateBlock();
                        }, 1L);
                        return;
                    }
                }
            }
            for (CourtQueue court : threesCourts) {
                for (CourtSpot spot : court.getSpots()) {
                    if (isSameBlock(spot.getLocation(), blockLoc)) {
                        // Schedule block replacement after a short delay
                        Bukkit.getScheduler().runTaskLater(Partix.getInstance(), () -> {
                            spot.updateBlock();
                        }, 1L);
                        return;
                    }
                }
            }
            return; // Not a queue spot, allow breaking
        }
        
        // Check if block is a queue spot block and prevent breaking
        for (CourtQueue court : twosCourts) {
            for (CourtSpot spot : court.getSpots()) {
                if (isSameBlock(spot.getLocation(), blockLoc)) {
                    event.setCancelled(true);
                    player.sendMessage("§c§l» §cYou can't break queue spot blocks!");
                    return;
                }
            }
        }
        for (CourtQueue court : threesCourts) {
            for (CourtSpot spot : court.getSpots()) {
                if (isSameBlock(spot.getLocation(), blockLoc)) {
                    event.setCancelled(true);
                    player.sendMessage("§c§l» §cYou can't break queue spot blocks!");
                    return;
                }
            }
        }
    }
    
    /**
     * Check if two locations refer to the same block
     */
    private boolean isSameBlock(Location loc1, Location loc2) {
        return loc1.getWorld() == loc2.getWorld() &&
               loc1.getBlockX() == loc2.getBlockX() &&
               loc1.getBlockY() == loc2.getBlockY() &&
               loc1.getBlockZ() == loc2.getBlockZ();
    }

    private void removePlayerFromAllQueues(UUID playerId) {
        // Remove from all queues by UUID (not by Athlete object)
        int removed1v1 = this.queue1v1.size();
        this.queue1v1.removeIf(a -> a == null || a.getPlayer() == null || a.getPlayer().getUniqueId().equals(playerId));
        removed1v1 -= this.queue1v1.size();

        int removed2v2 = this.queue2v2.size();
        this.queue2v2.removeIf(a -> a == null || a.getPlayer() == null || a.getPlayer().getUniqueId().equals(playerId));
        removed2v2 -= this.queue2v2.size();

        int removed3v3 = this.queue3v3.size();
        this.queue3v3.removeIf(a -> a == null || a.getPlayer() == null || a.getPlayer().getUniqueId().equals(playerId));
        removed3v3 -= this.queue3v3.size();

        int removed4v4 = this.queue4v4.size();
        this.queue4v4.removeIf(a -> a == null || a.getPlayer() == null || a.getPlayer().getUniqueId().equals(playerId));
        removed4v4 -= this.queue4v4.size();

        this.waitingExtras.removeIf(a -> a == null || a.getPlayer() == null || a.getPlayer().getUniqueId().equals(playerId));
        this.waitingTeams.removeIf(team -> team.getAthletes().stream().anyMatch(a ->
                a == null || a.getPlayer() == null || a.getPlayer().getUniqueId().equals(playerId)));

        int totalRemoved = removed1v1 + removed2v2 + removed3v3 + removed4v4;
        if (totalRemoved > 0) {
            Bukkit.getLogger().info("DEBUG: Removed player " + playerId + " from " + totalRemoved + " queue(s)");
        }
    }

    public boolean isPlayerQueued(Athlete athlete) {
        if (athlete == null || athlete.getPlayer() == null) {
            return false;
        }

        UUID playerId = athlete.getPlayer().getUniqueId();

        // Clean up null AND offline player entries first
        this.queue1v1.removeIf(a -> a == null || a.getPlayer() == null || !a.getPlayer().isOnline());
        this.queue2v2.removeIf(a -> a == null || a.getPlayer() == null || !a.getPlayer().isOnline());
        this.queue3v3.removeIf(a -> a == null || a.getPlayer() == null || !a.getPlayer().isOnline());
        this.queue4v4.removeIf(a -> a == null || a.getPlayer() == null || !a.getPlayer().isOnline());
        this.waitingExtras.removeIf(a -> a == null || a.getPlayer() == null || !a.getPlayer().isOnline());

        // Check by UUID instead of object reference
        boolean queued = this.queueContainsUUID(this.queue1v1, playerId) ||
                this.queueContainsUUID(this.queue2v2, playerId) ||
                this.queueContainsUUID(this.queue3v3, playerId) ||
                this.queueContainsUUID(this.queue4v4, playerId) ||
                this.waitingExtras.stream().anyMatch(a ->
                        a.getPlayer() != null && a.getPlayer().getUniqueId().equals(playerId));

        return queued;
    }
    private boolean queueContainsUUID(List<Athlete> queue, UUID uuid) {
        return queue.stream().anyMatch(a -> a.getPlayer().getUniqueId().equals(uuid));
    }

    public int getQueueSizeForMode(int mode) {
        if (mode == 1) {
            return this.queue1v1.size();
        }
        if (mode == 2) {
            return this.queue2v2.size();
        }
        if (mode == 3) {
            return this.queue3v3.size();
        }
        if (mode == 4) {
            return this.queue4v4.size();
        }
        return 0;
    }

    private int getRequiredPlayersForMode(int mode) {
        if (mode == 1) {
            return 2;
        }
        if (mode == 2) {
            return 4;
        }
        if (mode == 3) {
            return 6;
        }
        if (mode == 4) {
            return 8;
        }
        return 0;
    }

    // ===== 2K PARK PHYSICAL QUEUE SYSTEM METHODS =====
    
    /**
     * Initialize physical queue spots for 2v2 and 3v3 courts
     */
    private void initializePhysicalQueueSpots() {
        // Allocate 2 courts for 2v2 (first 2 ranked courts)
        if (rankedCourts.size() >= 4) {
            twosCourts.add(new CourtQueue(rankedCourts.get(0), CourtQueue.CourtType.TWOS));
            twosCourts.add(new CourtQueue(rankedCourts.get(1), CourtQueue.CourtType.TWOS));
            
            // Allocate 2 courts for 3v3 (last 2 ranked courts)
            threesCourts.add(new CourtQueue(rankedCourts.get(2), CourtQueue.CourtType.THREES));
            threesCourts.add(new CourtQueue(rankedCourts.get(3), CourtQueue.CourtType.THREES));
            
            Bukkit.getLogger().info("[Park Queue] Initialized 2 courts for 2v2 and 2 courts for 3v3 with physical spots");
        } else {
            Bukkit.getLogger().warning("[Park Queue] Not enough ranked courts to initialize physical spots!");
        }
    }
    
    /**
     * Start the particle spawning task for all spots
     */
    private void startParticleTask() {
        particleTask = Bukkit.getScheduler().runTaskTimer(Partix.getInstance(), () -> {
            // Spawn particles for all 2v2 court spots
            for (CourtQueue court : twosCourts) {
                court.spawnAllParticles();
            }
            
            // Spawn particles for all 3v3 court spots
            for (CourtQueue court : threesCourts) {
                court.spawnAllParticles();
            }
            
            // Set y velocity to 0 for all players on spots to prevent jumping/falling
            for (Map.Entry<UUID, CourtSpot> entry : playerSpots.entrySet()) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null && player.isOnline()) {
                    Vector velocity = player.getVelocity();
                    velocity.setY(0);
                    player.setVelocity(velocity);
                }
            }
        }, 0L, 2L); // Run every 2 ticks (0.1 seconds) for more consistent visual effect
    }
    
    /**
     * Handle player movement - check if they're entering spots and freeze them on spots
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        // Check if player moved to a new block (optimization)
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockY() == event.getTo().getBlockY() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        
        // Check if player is currently on a spot
        CourtSpot currentSpot = playerSpots.get(playerId);
        
        if (currentSpot != null) {
            // Player is on a spot - freeze them if they try to move too far
            if (!currentSpot.isPlayerInRange(player)) {
                // Teleport player back to spot center (1 block up)
                Location spotCenter = currentSpot.getLocation().clone();
                spotCenter.setY(spotCenter.getY() + 1.0); // Add 1 block height
                spotCenter.setYaw(player.getLocation().getYaw());
                spotCenter.setPitch(player.getLocation().getPitch());
                event.setTo(spotCenter);
                
                if (Math.random() < 0.1) { // Only send message 10% of the time to avoid spam
                    player.sendMessage("§e§l» §eYou're frozen on the spot! §7Shift to leave.");
                }
            }
            return; // Don't check for new spots if already on one
        }
        
        // Player is not on a spot - check if they entered one
        CourtSpot nearbySpot = findNearbyAvailableSpot(player);
        if (nearbySpot != null && !nearbySpot.isOccupied()) {
            // FIX: Prevent players already in a game from stepping on spots
            Athlete athlete = AthleteManager.get(playerId);
            if (athlete != null && athlete.getPlace() instanceof BasketballGame) {
                BasketballGame currentGame = (BasketballGame) athlete.getPlace();
                // Don't allow joining spot if in an active game (not in FINAL state)
                if (currentGame.getState() != GoalGame.State.FINAL) {
                    return;
                }
            }
            
            // NEW: Check if player is already in another queue (1v1)
            if (this.isPlayerQueued(athlete)) {
                player.sendMessage("§c§l» §cYou're already in another queue! Leave it first.");
                return;
            }
            
            // Check if it's a party spot - enforce party requirements
            if (nearbySpot.getSpotType() == CourtSpot.SpotType.PARTY) {
                if (!canOccupyPartySpot(player, nearbySpot.getCourt())) {
                    return; // Player doesn't meet party spot requirements
                }
            }
            
            if (nearbySpot.tryOccupy(playerId)) {
                playerSpots.put(playerId, nearbySpot);
                player.sendMessage("§a§l» §aYou're now on a queue spot! " + getSpotMessage(nearbySpot));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.MASTER, 1.0f, 1.5f);
                
                // If party leader joins a party spot, automatically add party members to nearby spots
                handlePartyLeaderJoin(player, nearbySpot);
                
                // Check if ready to start game
                checkAndStartGame(nearbySpot.getCourt());
            }
        }
    }
    
    /**
     * Check if a player can occupy a party spot (must be party leader with correct party size)
     */
    private boolean canOccupyPartySpot(Player player, CourtQueue court) {
        Athlete athlete = AthleteManager.get(player.getUniqueId());
        if (athlete == null) {
            player.sendMessage("§c§l» §cParty spots require a party!");
            return false;
        }
        
        Party party = PartyFactory.get(athlete.getParty());
        if (party == null) {
            player.sendMessage("§c§l» §cParty spots require a party!");
            return false;
        }
        
        // Must be party leader
        if (!party.leader.equals(player.getUniqueId())) {
            player.sendMessage("§c§l» §cOnly the party leader can queue the party!");
            return false;
        }
        
        // Check party size matches court type
        int partySize = party.toList().size();
        int requiredSize = court.getPlayersPerTeam();
        
        if (partySize != requiredSize) {
            player.sendMessage("§c§l» §cYou need exactly " + requiredSize + " players in your party for this court!");
            return false;
        }
        
        return true;
    }
    
    /**
     * When a party leader joins a party spot, automatically teleport and place all party members
     */
    private void handlePartyLeaderJoin(Player player, CourtSpot leaderSpot) {
        if (leaderSpot.getSpotType() != CourtSpot.SpotType.PARTY) {
            return; // Only handle party spots
        }
        
        Athlete athlete = AthleteManager.get(player.getUniqueId());
        if (athlete == null) {
            return;
        }
        
        Party party = PartyFactory.get(athlete.getParty());
        if (party == null || !party.leader.equals(player.getUniqueId())) {
            return; // Not a party leader
        }
        
        List<Athlete> partyMembers = party.toList();
        if (partyMembers.size() <= 1) {
            return; // Solo or just the leader
        }
        
        CourtQueue court = leaderSpot.getCourt();
        List<CourtSpot> partySpots = court.getSpotsByType(CourtSpot.SpotType.PARTY);
        
        // Try to place party members on adjacent party spots
        int placedMembers = 0;
        for (Athlete member : partyMembers) {
            if (member.equals(athlete)) {
                continue; // Skip the leader (already placed)
            }
            
            Player memberPlayer = member.getPlayer();
            if (memberPlayer == null || !memberPlayer.isOnline()) {
                continue;
            }
            
            // Find an empty party spot
            for (CourtSpot spot : partySpots) {
                if (!spot.isOccupied()) {
                    // Teleport party member to the spot (1 block up)
                    Location spotLoc = spot.getLocation().clone();
                    spotLoc.setY(spotLoc.getY() + 1.0);
                    memberPlayer.teleport(spotLoc);
                    
                    spot.tryOccupy(memberPlayer.getUniqueId());
                    playerSpots.put(memberPlayer.getUniqueId(), spot);
                    memberPlayer.sendMessage("§a§l» §aYour party leader queued you! " + getSpotMessage(spot));
                    memberPlayer.playSound(memberPlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.MASTER, 1.0f, 1.5f);
                    placedMembers++;
                    break;
                }
            }
        }
        
        if (placedMembers > 0) {
            player.sendMessage("§a§l» §aYour party members have been placed on spots!");
        }
    }
    
    /**
     * Find a nearby available spot for a player
     */
    private CourtSpot findNearbyAvailableSpot(Player player) {
        // Check 2v2 courts
        for (CourtQueue court : twosCourts) {
            CourtSpot spot = court.getSpotForPlayer(player);
            if (spot != null && !spot.isOccupied()) {
                return spot;
            }
        }
        
        // Check 3v3 courts
        for (CourtQueue court : threesCourts) {
            CourtSpot spot = court.getSpotForPlayer(player);
            if (spot != null && !spot.isOccupied()) {
                return spot;
            }
        }
        
        return null;
    }
    
    /**
     * Get a descriptive message for what spot the player is on
     */
    private String getSpotMessage(CourtSpot spot) {
        String courtType = spot.getCourt().getPlayersPerTeam() == 2 ? "2v2" : "3v3";
        String spotType = switch (spot.getSpotType()) {
            case TEAM1 -> "§eTeam 1";
            case PARTY -> "§dWaiting/Party";
            case TEAM2 -> "§bTeam 2";
        };
        return "§7(" + courtType + " - " + spotType + "§7)";
    }
    
    /**
     * Check if a court is ready to start and initiate the game
     */
    private void checkAndStartGame(CourtQueue court) {
        if (!court.isReadyToStart()) {
            return;
        }
        
        List<Player> team1Players = court.getTeam1Players();
        List<Player> team2Players = court.getTeam2Players();
        
        // Validate all players are still online and available
        if (team1Players.size() != court.getPlayersPerTeam() || 
            team2Players.size() != court.getPlayersPerTeam()) {
            return;
        }
        
        // Convert to Athletes
        List<Athlete> team1Athletes = team1Players.stream()
            .map(p -> AthleteManager.get(p.getUniqueId()))
            .filter(a -> a != null)
            .collect(java.util.stream.Collectors.toList());
            
        List<Athlete> team2Athletes = team2Players.stream()
            .map(p -> AthleteManager.get(p.getUniqueId()))
            .filter(a -> a != null)
            .collect(java.util.stream.Collectors.toList());
        
        if (team1Athletes.size() != court.getPlayersPerTeam() || 
            team2Athletes.size() != court.getPlayersPerTeam()) {
            return;
        }
        
        // Create the game
        startPhysicalQueueGame(court, team1Athletes, team2Athletes);
    }
    
    /**
     * Start a game from the physical queue system
     */
    private void startPhysicalQueueGame(CourtQueue courtQueue, List<Athlete> team1, List<Athlete> team2) {
        // Find or create a game instance for this court
        BasketballGame game = null;
        Location courtLoc = courtQueue.getCourtLocation();
        
        // Check if there's already a stopped game at this location
        if (games.containsKey(courtLoc)) {
            BasketballGame existingGame = games.get(courtLoc);
            // Check if the game state allows reuse (check for finished state)
            if (existingGame.getState() == GoalGame.State.FINAL) {
                games.remove(courtLoc); // Remove old game
            }
        }
        
        // Create new game with proper parameters
        Settings settings = gameSettings.copy();
        settings.playersPerTeam = courtQueue.getPlayersPerTeam();
        // Court dimensions: 26.0 length, 2.8 Y distance, 0.45 X length, 0.475 Z width, 0.575 Y height
        game = new BasketballGame(settings, courtLoc, 26.0, 2.8, 0.45, 0.475, 0.575);
        game.isPhysicalQueueGame = true; // Mark as physical queue game
        games.put(courtLoc, game);
        
        // CRITICAL FIX: Remove all players from spot tracking so they're no longer frozen
        for (Athlete athlete : team1) {
            playerSpots.remove(athlete.getPlayer().getUniqueId());
        }
        for (Athlete athlete : team2) {
            playerSpots.remove(athlete.getPlayer().getUniqueId());
        }
        
        // Join players to the game
        for (Athlete athlete : team1) {
            game.join(athlete);
            game.joinTeam(athlete.getPlayer(), GoalGame.Team.HOME);
        }
        
        for (Athlete athlete : team2) {
            game.join(athlete);
            game.joinTeam(athlete.getPlayer(), GoalGame.Team.AWAY);
        }
        
        // DON'T clear the court spots yet - keep them for winners to return to
        // courtQueue.clearAllSpots(); // Commented out
        
        // Keep players in tracking so we know which court they came from
        // We'll handle this in the game end callback
        
        // Store the court queue reference for this game so we can handle winner/loser logic
        gameCourtMapping.put(courtLoc, courtQueue);
        
        // Notify players
        for (Player p : game.getPlayers()) {
            p.sendMessage("§a§l» §aGame starting! Winners stay on the spot!");
        }
        
        // Start the game
        game.start();
        
        Bukkit.getLogger().info("[Park Queue] Started " + courtQueue.getPlayersPerTeam() + "v" + 
                               courtQueue.getPlayersPerTeam() + " game from physical queue");
    }
    
    /**
     * Remove a player from all physical queue spots
     */
    private void removeFromPhysicalQueue(UUID playerId) {
        CourtSpot spot = playerSpots.remove(playerId);
        if (spot != null) {
            spot.vacate();
        }
        
        // Also check all courts to be safe
        for (CourtQueue court : twosCourts) {
            court.removePlayer(playerId);
        }
        for (CourtQueue court : threesCourts) {
            court.removePlayer(playerId);
        }
    }
    
    /**
     * Called when a physical queue game ends - handles winner-stays, loser-leaves logic
     */
    public void onPhysicalQueueGameEnd(BasketballGame game, Athlete athlete) {
        Player player = athlete.getPlayer();
        Location courtLoc = game.getLocation();
        
        // Get the court queue for this game
        CourtQueue courtQueue = gameCourtMapping.get(courtLoc);
        if (courtQueue == null) {
            // Fallback: send to spawn if no court mapping found
            Location spawn = new Location(courtLoc.getWorld(), 9.5, -61.0, 154.5);
            player.teleport(spawn);
            Hub.basketballLobby.join(athlete);
            return;
        }
        
        // Determine if player is a winner or loser
        GoalGame.Team winningTeam = game.getHomeScore() > game.getAwayScore() ? 
            GoalGame.Team.HOME : GoalGame.Team.AWAY;
        
        List<Player> homePlayers = game.getHomePlayers();
        boolean isHome = homePlayers.contains(player);
        GoalGame.Team playerTeam = isHome ? GoalGame.Team.HOME : GoalGame.Team.AWAY;
        boolean isWinner = playerTeam == winningTeam;
        
        if (isWinner) {
            // Winner - teleport back to spot
            List<CourtSpot> winnerSpots = winningTeam == GoalGame.Team.HOME ? 
                courtQueue.getSpotsByType(CourtSpot.SpotType.TEAM1) : 
                courtQueue.getSpotsByType(CourtSpot.SpotType.TEAM2);
            
            // Find first available winner spot
            CourtSpot targetSpot = null;
            for (CourtSpot spot : winnerSpots) {
                if (!spot.isOccupied() || spot.getOccupiedBy().equals(player.getUniqueId())) {
                    targetSpot = spot;
                    break;
                }
            }
            
            if (targetSpot != null) {
                targetSpot.vacate();
                targetSpot.tryOccupy(player.getUniqueId());
                playerSpots.put(player.getUniqueId(), targetSpot);
                
                // Teleport to spot
                Location spotLoc = targetSpot.getLocation().clone();
                spotLoc.setY(spotLoc.getY() + 1.0);
                player.teleport(spotLoc);
                player.sendMessage("§a§l» §aYou won! Back on the spot - defend your court!");
                Hub.basketballLobby.join(athlete);
            } else {
                // No spot available, send to spawn
                Location spawn = new Location(courtLoc.getWorld(), 9.5, -61.0, 154.5);
                player.teleport(spawn);
                player.sendMessage("§a§l» §aYou won!");
                Hub.basketballLobby.join(athlete);
            }
        } else {
            // Loser - teleport to spawn area
            Location loserSpawn = new Location(courtLoc.getWorld(), 9.5, -61.0, 154.5);
            player.teleport(loserSpawn);
            player.sendMessage("§c§l» §cYou lost! Better luck next time.");
            playerSpots.remove(player.getUniqueId());
            Hub.basketballLobby.join(athlete);
        }
        
        // Clear loser spots if this is the last player being processed
        // This will be handled by checking if game has no more players
        Bukkit.getScheduler().runTaskLater(Partix.getInstance(), () -> {
            if (game.getPlayers().isEmpty()) {
                List<CourtSpot> loserSpots = winningTeam == GoalGame.Team.HOME ? 
                    courtQueue.getSpotsByType(CourtSpot.SpotType.TEAM2) : 
                    courtQueue.getSpotsByType(CourtSpot.SpotType.TEAM1);
                for (CourtSpot spot : loserSpots) {
                    if (!playerSpots.containsValue(spot)) {
                        spot.vacate();
                    }
                }
                gameCourtMapping.remove(courtLoc);
                Bukkit.getLogger().info("[Park Queue] Game ended - winners stay, losers kicked");
            }
        }, 5L);
    }

    @Getter
    private class Team {
        private final List<Athlete> athletes = new ArrayList<>();
        private int skill;

        public Team(Athlete... athletes) {
            Bukkit.getScheduler().runTaskAsynchronously(Partix.getInstance(), () -> {
                if (athletes.length > 0) {
                    this.athletes.addAll(Arrays.stream(athletes).toList());
                    for (Athlete athlete : athletes) {
                        if (athlete == null) continue;
                        this.skill = (int) ((double) this.skill + BasketballLobby.this.generateSkill(athlete).join());
                    }
                    this.skill /= athletes.length;
                } else {
                    this.skill = 255;
                }
            });
        }

    }
}

