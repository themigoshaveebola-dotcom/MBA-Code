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
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class BasketballLobby
        extends Lobby {
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
    @Getter
    private final List<Location> defaultArenas = new ArrayList<>();
    private final List<Stadium> stadiums = new ArrayList<>();
    private final List<Location> recCourts = new ArrayList<>();
    private final List<Location> fourVfourDefaultCourts = new ArrayList<>();
    private final List<Stadium> mbaStadiums = new ArrayList<>();
    private final List<Stadium> mcaaStadiums = new ArrayList<>();
    private final List<Stadium> retroStadiums = new ArrayList<>();
    private final Settings gameSettings = new Settings(WinType.TIME_5, GameType.AUTOMATIC, WaitType.SHORT, CompType.RANKED, 2, true, false, false, 1, GameEffectType.NONE);
    private final Settings customSettings = new Settings(WinType.TIME_5, GameType.MANUAL, WaitType.MEDIUM, CompType.CASUAL, 2, false, false, false, 4, GameEffectType.NONE);
    private final Settings recSettings = new Settings(WinType.TIME_5, GameType.AUTOMATIC, WaitType.MEDIUM, CompType.RANKED, 4, false, false, false, 2, GameEffectType.NONE);
    @Getter
    private final List<Location> customArenas = new ArrayList<>();
    private final List<Stadium> customStadiums = new ArrayList<>();
    private final HashMap<UUID, Integer> playerQueueNotifier = new HashMap<>();
    private final int countdown = 170;

    public BasketballLobby() {
        this.rankedCourts.add(new Location(Bukkit.getWorlds().getFirst(), 42.5, -61, 196.5));
        this.rankedCourts.add(new Location(Bukkit.getWorlds().getFirst(), -24.5, -61, 196.5));
        this.rankedCourts.add(new Location(Bukkit.getWorlds().getFirst(), -24.5, -61, 111.5));
        this.rankedCourts.add(new Location(Bukkit.getWorlds().getFirst(), 42.5, -61, 111.5));

        this.myCourts.add(new Location(Bukkit.getWorlds().getFirst(), 285.5, -62, -509.5));
        this.myCourts.add(new Location(Bukkit.getWorlds().getFirst(), -226.5, -62, -509.5));
        this.myCourts.add(new Location(Bukkit.getWorlds().getFirst(), 798.5, -62, 3.5));
        this.myCourts.add(new Location(Bukkit.getWorlds().getFirst(), 798.5, -62, 472.5));
        this.myCourts.add(new Location(Bukkit.getWorlds().getFirst(), 798.5, -62, 963.5));


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

        this.addCustomStadium("§l§6Bronx §9Bison", Material.ORANGE_GLAZED_TERRACOTTA, new Location(Bukkit.getWorlds().getFirst(), 448.5, -63, 963.5), Stadium.Category.MBA);
        this.addCustomStadium("§l§dMiami Flamingo §bFlyers", Material.PINK_GLAZED_TERRACOTTA, new Location(Bukkit.getWorlds().getFirst(), -42.5, -63, 963.5), Stadium.Category.MBA);
        this.addCustomStadium("§l§cBoston §fPatriots §9", Material.BLUE_GLAZED_TERRACOTTA, new Location(Bukkit.getWorlds().getFirst(), -532.5, -63, 963.5), Stadium.Category.MBA);
        this.addCustomStadium("§l§0Cleveland §cBandits", Material.BLACK_GLAZED_TERRACOTTA, new Location(Bukkit.getWorlds().getFirst(), -532.5, -63, 452.5), Stadium.Category.MBA);
        this.addCustomStadium("§l§0Washington §6Cerberus §8", Material.BLACK_GLAZED_TERRACOTTA, new Location(Bukkit.getWorlds().getFirst(), -576.5, -63, -510.5), Stadium.Category.MBA);
        this.addCustomStadium("§l§bNashville §9Koalas §7", Material.LIGHT_BLUE_GLAZED_TERRACOTTA, new Location(Bukkit.getWorlds().getFirst(), -576.5, -63, -19.5), Stadium.Category.MBA);
        this.maps.addAll(this.myCourts);
    }

    private void addArena(Location location, String arenaName, Material displayBlock) {
        this.arenas.add(location);
        this.arenaNames.put(location, arenaName);
        this.arenaDisplayBlocks.put(arenaName, displayBlock);
    }

    public BasketballGame findAvailableRecCourt() {
        ArrayList<Location> freeRecCourts = new ArrayList<>(this.recCourts);
        freeRecCourts.removeAll(this.games.keySet());
        if (freeRecCourts.isEmpty()) {
            return null;
        }
        Location randomLoc = freeRecCourts.get(new Random().nextInt(freeRecCourts.size()));
        BasketballGame game = new BasketballGame(this.recSettings, randomLoc, 32.0, 2.8, 0.45, 0.475, 0.575);
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
        BasketballGame game = new BasketballGame(this.customSettings, randomLoc, 32.0, 2.8, 0.45, 0.475, 0.575);
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
        if (!player.hasPermission("rank.vip")) {
            player.sendMessage("§cYou need §aVIP §cto browse the stadium folders!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, SoundCategory.MASTER, 1.0f, 1.0f);
            return;
        }
        ItemButton[] btn = new ItemButton[27];
        for (int i = 0; i < btn.length; ++i) {
            btn[i] = new ItemButton(i, FILLER_PANE, p -> {
            });
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
            btn[slot] = new ItemButton(slot, Items.get(title, iconMat, 1, "§7Browse this folder"), p -> this.openStadiumFolderGUI(p, category));
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
        game.startCountdown(GoalGame.State.FACEOFF, 15);
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
            PlayerDb.create(player.getUniqueId(), player.getName());
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
            Hub.basketballLobby.leaveQueue(player);
            this.removeCancelItem(player);
            UUID uuid = player.getUniqueId();
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
        this.createBasketballGame(player, loc, 32);
    }

    public void createGameInStadium(Player player, String name) {
        this.findStadiumByName(name).ifPresentOrElse(s -> this.createGameInStadium(player, s), () -> player.sendMessage("§cStadium not found: " + name));
    }

    private void equipPlayerForGame(Player player) {
        player.getInventory().clear();
        player.getInventory().addItem(Items.get(Component.text("Team Selector").color(Colour.partix()), Material.GRAY_DYE, 1, "§7Select your team"));
        player.getInventory().addItem(Items.get(Component.text("Bench Control").color(Colour.partix()), Material.OAK_STAIRS, 1, "§7Enter or leave the bench"));
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
            game = this.findAvailableRankedCourt();
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
        game.startCountdown(GoalGame.State.FACEOFF, 15);
    }

    public void leaveQueue(Player player) {
        Athlete athlete = AthleteManager.get(player.getUniqueId());
        if (athlete == null) {
            return;
        }
        int partyId = athlete.getParty();
        Party party = partyId > 0 ? PartyFactory.get(partyId) : null;

        // Remove from ALL queues (including 1v1 which was missing!)
        this.removeFromQueue(this.queue1v1, athlete);
        this.removeFromQueue(this.queue2v2, athlete);
        this.removeFromQueue(this.queue3v3, athlete);
        this.removeFromQueue(this.queue4v4, athlete);

        this.waitingExtras.remove(athlete);
        this.waitingTeams.removeIf(team -> team.getAthletes().contains(athlete));

        // If player is in a party, remove all party members from queues too
        if (party != null) {
            for (Athlete a : party.toList()) {
                this.removeFromQueue(this.queue1v1, a);
                this.removeFromQueue(this.queue2v2, a);
                this.removeFromQueue(this.queue3v3, a);
                this.removeFromQueue(this.queue4v4, a);
                this.waitingExtras.remove(a);
                this.waitingTeams.removeIf(team -> team.getAthletes().contains(a));
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
        int rows = 3;
        int size = 27;
        ItemButton[] buttons = new ItemButton[27];
        for (int i = 0; i < 27; ++i) {
            buttons[i] = new ItemButton(i, FILLER_PANE, p -> {
            });
        }
        this.registerFramed(buttons, 10, this.icon(Material.IRON_INGOT, Component.text("§l§c⚔ 1v1 Mode"), "§7Face off in a 1v1 duel! (Rewards DISABLED)", " ", "§eQueue: " + this.queue1v1.size() + "/2 players"), p -> this.joinQueue(p, 1));
        this.registerFramed(buttons, 12, this.icon(Material.GOLD_INGOT, Component.text("§l§6⚔ 2v2 Mode"), "§7Team up for a 2v2 battle!", " ", "§eQueue: " + this.queue2v2.size() + "/4 players"), p -> this.joinQueue(p, 2));
        this.registerFramed(buttons, 14, this.icon(Material.DIAMOND, Component.text("§l§b⚔ 3v3 Mode"), "§7Join a team-based 3v3 showdown!", " ", "§eQueue: " + this.queue3v3.size() + "/6 players"), p -> this.joinQueue(p, 3));
        this.registerFramed(buttons, 16, this.icon(Material.EMERALD, Component.text("§l§a⚔ 4v4 Mode"), "§7Join a 4v4 match on a rec court!", " ", "§eQueue: " + this.queue4v4.size() + "/8 players"), p -> this.joinQueue(p, 4));
        Athlete athlete = AthleteManager.get(player.getUniqueId());
        if (this.isPlayerQueued(athlete)) {
            buttons[22] = new ItemButton(22, this.icon(Material.BARRIER, Component.text("§cLeave Queue"), "§7Click to leave the queue"), this::leaveQueue);
        }
        new GUI("§l§6Game Selector", 3, false, buttons).openInventory(player);
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
        Athlete athlete = AthleteManager.get(player.getUniqueId());
        if (athlete != null) {
            this.leaveQueue(player);
        }
        this.playerQueueNotifier.remove(player.getUniqueId());
        Bukkit.getScheduler().runTaskLater(Partix.getInstance(), this::updateGameSelectorGUI, 5L);
    }

    public boolean isPlayerQueued(Athlete athlete) {
        if (athlete == null) {
            return false;
        }
        this.queue4v4.removeIf(a -> a == null || a.getPlayer() == null);
        this.queue2v2.removeIf(a -> a == null || a.getPlayer() == null);
        this.queue3v3.removeIf(a -> a == null || a.getPlayer() == null);
        this.waitingExtras.removeIf(a -> a == null || a.getPlayer() == null);
        boolean queued = this.queueContainsUUID(this.queue4v4, athlete.getPlayer().getUniqueId()) ||
                this.queueContainsUUID(this.queue2v2, athlete.getPlayer().getUniqueId()) ||
                this.queueContainsUUID(this.queue3v3, athlete.getPlayer().getUniqueId()) ||
                this.queueContainsUUID(this.queue1v1 , athlete.getPlayer().getUniqueId()) ||
                this.waitingExtras.stream().anyMatch(a -> a.getPlayer().getUniqueId().equals(athlete.getPlayer().getUniqueId()));
        Bukkit.getLogger().info("DEBUG: isPlayerQueued for " + athlete.getPlayer().getName() + ": " + queued);
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

