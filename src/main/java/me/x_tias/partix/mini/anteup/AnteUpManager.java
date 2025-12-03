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
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.Action
 *  org.bukkit.event.player.PlayerInteractEvent
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.PlayerInventory
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 *  org.bukkit.potion.PotionEffect
 *  org.bukkit.scheduler.BukkitRunnable
 */
package me.x_tias.partix.mini.anteup;

import me.x_tias.partix.Partix;
import me.x_tias.partix.database.PlayerDb;
import me.x_tias.partix.mini.basketball.BasketballGame;
import me.x_tias.partix.mini.factories.Hub;
import me.x_tias.partix.mini.game.GoalGame;
import me.x_tias.partix.plugin.athlete.Athlete;
import me.x_tias.partix.plugin.athlete.AthleteManager;
import me.x_tias.partix.plugin.gui.GUI;
import me.x_tias.partix.plugin.gui.ItemButton;
import me.x_tias.partix.plugin.party.Party;
import me.x_tias.partix.plugin.party.PartyFactory;
import me.x_tias.partix.plugin.settings.*;
import me.x_tias.partix.util.Colour;
import me.x_tias.partix.util.Items;
import me.x_tias.partix.util.Message;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class AnteUpManager
        implements Listener {
    private static final Location ANTE_UP_SPAWN = new Location(Bukkit.getWorlds().getFirst(), 23.5, -31.0, 135.5);
    private static final Location COURT_100 = new Location(Bukkit.getWorlds().getFirst(), 59.5, -31.0, 106.5);
    private static final Location COURT_500 = new Location(Bukkit.getWorlds().getFirst(), 59.5, -31.0, 165.5);
    private static final Location COURT_1000 = new Location(Bukkit.getWorlds().getFirst(), 123.5, -31.0, 106.5);
    private static final Location COURT_2500 = new Location(Bukkit.getWorlds().getFirst(), 123.5, -31.0, 165.5);
    private static final Map<QueueKey, List<UUID>> queueMap = new HashMap<>();
    private static final Location ANTE_UP_EXIT = new Location(Bukkit.getWorlds().getFirst(), 101.5, -60.0, 10.5);
    private static final Set<Location> activeCourts = new HashSet<>();
    private static final Map<AnteOption, Boolean> isCourtBusy = new HashMap<>();

    static {
        for (AnteOption ao : AnteOption.values()) {
            isCourtBusy.put(ao, false);
        }
    }

    public AnteUpManager(JavaPlugin plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public static void handleAnteUpPayout(BasketballGame game, GoalGame.Team winner) {
        List<Player> losersList;
        List<Player> winnersList;
        Object anteObj = game.getCustomProperty("anteAmount");
        if (!(anteObj instanceof Integer)) {
            return;
        }
        int ante = (Integer) anteObj;
        int teamSize = (Integer) game.getCustomPropertyOrDefault("matchSize", 1);
        if (winner == GoalGame.Team.HOME) {
            winnersList = game.getHomePlayers();
            losersList = game.getAwayPlayers();
        } else {
            winnersList = game.getAwayPlayers();
            losersList = game.getHomePlayers();
        }
        if (winnersList.isEmpty()) {
            return;
        }
        int amountPerWinner = teamSize * ante;
        for (Player w : winnersList) {
            PlayerDb.add(w.getUniqueId(), PlayerDb.Stat.MBA_BUCKS, amountPerWinner);
            w.sendMessage("§aYou won " + amountPerWinner + " MBA Bucks from Ante Up! GG!");
            AnteUpManager.openMbaWinningsWheelGUI(w, amountPerWinner);
            AnteUpManager.resetPlayerState(w);
        }
        for (Player l : losersList) {
            l.sendMessage("§cYou lost your " + ante + " MBA Bucks ante. Better luck next time!");
            AnteUpManager.resetPlayerState(l);
        }
    }

    public static void resetPlayerState(Player player) {
        player.getInventory().clear();
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        player.teleport(ANTE_UP_SPAWN);
        ItemStack anteDiamond = Items.get(Component.text("Ante Up Queue").color(Colour.partix()), Material.DIAMOND);
        player.getInventory().setItem(0, anteDiamond);
        Athlete athlete = AthleteManager.get(player.getUniqueId());
        if (athlete != null) {
            Hub.hub.updateSidebar(athlete);
        }
    }

    public static void openMbaWinningsWheelGUI(final Player player, final int baseMbaWinnings) {
        final GUI spinGUI = new GUI("MBA Winnings Multiplier", 3, false);
        for (int slot = 0; slot < 27; ++slot) {
            if (slot == 13) continue;
            Material bg = (slot / 9 + slot % 9) % 2 == 0 ? Material.RED_STAINED_GLASS_PANE : Material.BLACK_STAINED_GLASS_PANE;
            spinGUI.addButton(new ItemButton(slot, Items.get(Component.empty(), bg), p -> {
            }));
        }
        spinGUI.addButton(new ItemButton(13, Items.get(Component.text("Spinning..."), Material.NETHER_STAR), p -> {
        }));
        spinGUI.openInventory(player);
        final List<MbaMultiplierPrize> prizes = Arrays.asList(new MbaMultiplierPrize("§7x1 Multiplier", 1.0, Material.COAL, 800), new MbaMultiplierPrize("§bx1.5 Multiplier", 1.5, Material.GOLD_INGOT, 100), new MbaMultiplierPrize("§a§lx2 Multiplier!", 2.0, Material.DIAMOND, 75), new MbaMultiplierPrize("§6§lJACKPOT x10!!!", 10.0, Material.TOTEM_OF_UNDYING, 25));
        int totalWeight = 1000;
        new BukkitRunnable() {
            int iterations = 20;

            public void run() {
                if (this.iterations <= 0) {
                    this.cancel();
                    return;
                }
                MbaMultiplierPrize current = AnteUpManager.pickWeightedMbaPrize(prizes, 1000);
                spinGUI.addButton(new ItemButton(13, current.toItem(baseMbaWinnings), p -> {
                }));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.MASTER, 1.0f, 1.0f);
                --this.iterations;
                if (this.iterations == 0) {
                    double multiplier = current.multiplier;
                    int finalMBA = (int) Math.round((double) baseMbaWinnings * multiplier);
                    PlayerDb.get(player.getUniqueId(), PlayerDb.Stat.MBA_BUCKS).thenAccept(oldBalance -> {
                        PlayerDb.set(player.getUniqueId(), PlayerDb.Stat.MBA_BUCKS, oldBalance + finalMBA);
                    });

                    if (multiplier == 1.0) {
                        player.sendMessage("§cNo multiplier – you keep x1: " + baseMbaWinnings + " MBA Bucks total.");
                    } else {
                        player.sendMessage("§aMultiplier: " + current.name + " => §b" + finalMBA + " MBA Bucks total!");
                    }
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1.5f, 1.0f);
                    new BukkitRunnable() {

                        public void run() {
                            player.closeInventory();
                        }
                    }.runTaskLater(Partix.getInstance(), 60L);
                    this.cancel();
                }
            }
        }.runTaskTimer(Partix.getInstance(), 0L, 5L);
    }

    private static MbaMultiplierPrize pickWeightedMbaPrize(List<MbaMultiplierPrize> prizes, int totalWeight) {
        int rand = new Random().nextInt(totalWeight);
        int sum = 0;
        for (MbaMultiplierPrize p : prizes) {
            if (rand >= (sum += p.weight)) continue;
            return p;
        }
        return prizes.getFirst();
    }

    private static Map<GoalGame.Team, List<Player>> formTeams(List<Player> players, int teamSize) {
        ArrayList<Player> team1 = new ArrayList<>();
        ArrayList<Player> team2 = new ArrayList<>();
        ArrayList<Player> soloPlayers = new ArrayList<>();
        HashMap<Integer, List<Player>> partyGroups = new HashMap<>();
        for (Player player : players) {
            Athlete athlete = AthleteManager.get(player.getUniqueId());
            if (athlete != null && athlete.getParty() >= 0) {
                int partyId = athlete.getParty();
                partyGroups.computeIfAbsent(partyId, k -> new ArrayList<>()).add(player);
                continue;
            }
            soloPlayers.add(player);
        }
        for (List<Player> party : partyGroups.values()) {
            if (team1.size() <= team2.size()) {
                team1.addAll(party);
                continue;
            }
            team2.addAll(party);
        }
        for (Player player : soloPlayers) {
            if (team1.size() < teamSize) {
                team1.add(player);
                continue;
            }
            team2.add(player);
        }
        HashMap<GoalGame.Team, List<Player>> teams = new HashMap<>();
        teams.put(GoalGame.Team.HOME, team1);
        teams.put(GoalGame.Team.AWAY, team2);
        return teams;
    }

    @EventHandler
    public void onDiamondClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack hand = event.getPlayer().getInventory().getItemInMainHand();
        if (hand == null || hand.getType() != Material.DIAMOND) {
            return;
        }
        if (!hand.hasItemMeta() || !hand.getItemMeta().getDisplayName().contains("Ante Up Queue")) {
            return;
        }
        event.setCancelled(true);
        this.openAnteSelectionGUI(event.getPlayer());
    }

    @EventHandler
    public void onBarrelInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (!event.hasBlock() || event.getClickedBlock() == null) {
            return;
        }
        Material blockType = event.getClickedBlock().getType();
        if (blockType == Material.BARREL) {
            event.setCancelled(true);
            this.openEnterLeaveGUI(event.getPlayer());
        }
    }

    private void openEnterLeaveGUI(Player player) {
        new GUI("Ante Up Options", 1, false, new ItemButton(0, Items.get(Component.text("Enter Ante Up").color(Colour.allow()), Material.LIME_CONCRETE, 1, "§7Click to teleport & get the diamond"), p -> {
            p.teleport(ANTE_UP_SPAWN);
            p.getInventory().clear();
            ItemStack anteDiamond = Items.get(Component.text("Ante Up Queue").color(Colour.partix()), Material.DIAMOND);
            p.getInventory().setItem(0, anteDiamond);
            p.sendMessage("§aYou have entered Ante Up mode!");
            p.closeInventory();
        }), new ItemButton(1, Items.get(Component.text("Leave Ante Up").color(Colour.deny()), Material.RED_CONCRETE, 1, "§7Click to leave Ante Up and return outside"), p -> {
            this.leaveQueue(p);
            p.teleport(ANTE_UP_EXIT);
            p.getInventory().clear();
            Hub.hub.join(AthleteManager.get(p.getUniqueId()));
            p.sendMessage("§cYou have left Ante Up mode and returned to the lobby!");
            p.closeInventory();
        })).openInventory(player);
    }

    private void giveLobbyItems(Player player) {
        PlayerInventory inv = player.getInventory();
        inv.setItem(0, Items.get(Message.itemName("Game Selector", "key.use", player), Material.NETHER_STAR));
        inv.setItem(1, Items.get(Message.itemName("Custom Games", "key.use", player), Material.BEACON));
        inv.setItem(7, Items.get(Message.itemName("Prediction Book", "key.use", player), Material.BOOK));
        inv.setItem(8, Items.get(Message.itemName("Your Cosmetics", "key.use", player), Material.ENDER_CHEST));
    }

    private void openAnteSelectionGUI(Player player) {
        new GUI("Select Your Ante", 1, false, new ItemButton(0, Items.get(Component.text("100 MBA Bucks").color(Colour.partix()), Material.EMERALD, 1, "§7Click to queue for 100 MBA Bucks"), p -> this.openMatchSizeGUI(p, AnteOption.ANTE_100)), new ItemButton(1, Items.get(Component.text("500 MBA Bucks").color(Colour.partix()), Material.EMERALD, 1, "§7Click to queue for 500 MBA Bucks"), p -> this.openMatchSizeGUI(p, AnteOption.ANTE_500)), new ItemButton(2, Items.get(Component.text("1000 MBA Bucks").color(Colour.partix()), Material.EMERALD, 1, "§7Click to queue for 1000 MBA Bucks"), p -> this.openMatchSizeGUI(p, AnteOption.ANTE_1000)), new ItemButton(3, Items.get(Component.text("2500 MBA Bucks").color(Colour.partix()), Material.EMERALD, 1, "§7Click to queue for 2500 MBA Bucks"), p -> this.openMatchSizeGUI(p, AnteOption.ANTE_2500))).openInventory(player);
    }

    private void openMatchSizeGUI(Player player, AnteOption ante) {
        int queued1v1 = this.getQueueSize(ante, MatchSize.ONE_VS_ONE);
        int queued2v2 = this.getQueueSize(ante, MatchSize.TWO_VS_TWO);
        int queued3v3 = this.getQueueSize(ante, MatchSize.THREE_VS_THREE);
        boolean party1v1 = this.isPartyQueued(ante, MatchSize.ONE_VS_ONE);
        boolean party2v2 = this.isPartyQueued(ante, MatchSize.TWO_VS_TWO);
        boolean party3v3 = this.isPartyQueued(ante, MatchSize.THREE_VS_THREE);
        new GUI("Select Match Size (" + ante.cost + ")", 1, false, new ItemButton(0, Items.get(Component.text("1v1").color(Colour.partix()), Material.IRON_INGOT, 1, "§7Currently queued: §e" + queued1v1, "§7Party queued: §e" + (party1v1 ? "Yes" : "No"), "§7Click to queue for a 1v1 on the " + ante.cost + " MBA Bucks court"), p -> this.attemptJoinQueue(p, ante, MatchSize.ONE_VS_ONE)), new ItemButton(1, Items.get(Component.text("2v2").color(Colour.partix()), Material.GOLD_INGOT, 1, "§7Currently queued: §e" + queued2v2, "§7Party queued: §e" + (party2v2 ? "Yes" : "No"), "§7Click to queue for a 2v2 on the " + ante.cost + " MBA Bucks court"), p -> this.attemptJoinQueue(p, ante, MatchSize.TWO_VS_TWO)), new ItemButton(2, Items.get(Component.text("3v3").color(Colour.partix()), Material.DIAMOND, 1, "§7Currently queued: §e" + queued3v3, "§7Party queued: §e" + (party3v3 ? "Yes" : "No"), "§7Click to queue for a 3v3 on the " + ante.cost + " MBA Bucks court"), p -> this.attemptJoinQueue(p, ante, MatchSize.THREE_VS_THREE)), new ItemButton(8, Items.get(Component.text("Cancel Queue").color(Colour.deny()), Material.BARRIER, 1, "§7Click to leave any current Ante Up queue"), p -> {
            this.leaveQueue(p);
            p.sendMessage("§cYou have left the Ante Up queue (if you were in one).");
            p.closeInventory();
        })).openInventory(player);
    }

    private int getQueueSize(AnteOption ante, MatchSize size) {
        QueueKey key = new QueueKey(ante, size);
        List<UUID> list = queueMap.get(key);
        return list == null ? 0 : list.size();
    }

    private boolean isPartyQueued(AnteOption ante, MatchSize size) {
        QueueKey key = new QueueKey(ante, size);
        List<UUID> queued = queueMap.get(key);
        if (queued == null || queued.isEmpty()) {
            return false;
        }
        HashMap<Integer, Integer> partyCounts = new HashMap<>();
        for (UUID id : queued) {
            int partyId;
            Athlete ath = AthleteManager.get(id);
            if (ath == null || (partyId = ath.getParty()) < 0) continue;
            partyCounts.put(partyId, partyCounts.getOrDefault(partyId, 0) + 1);
        }
        for (int count : partyCounts.values()) {
            if (count < 2) continue;
            return true;
        }
        return false;
    }

    private void attemptJoinQueue(Player player, AnteOption ante, MatchSize size) {
        this.removePlayerFromAllQueues(player);
        Athlete athlete = AthleteManager.get(player.getUniqueId());
        if (athlete == null) {
            return;
        }
        int neededMBA = ante.cost;
        PlayerDb.get(player.getUniqueId(), PlayerDb.Stat.MBA_BUCKS).thenAccept(playerMBA -> {
            if (playerMBA < neededMBA) {
                player.sendMessage("§cYou need at least " + neededMBA + " MBA Bucks to queue for this!");
                return;
            }
            if (athlete.getParty() >= 0) {
                Party party = PartyFactory.get(athlete.getParty());
                if (party.leader.equals(player.getUniqueId())) {
                    List<Athlete> partyMembers = party.toList();
                    if (partyMembers.size() > size.teamSize) {
                        player.sendMessage("§cYour party has " + partyMembers.size() + " members but this is a " + size.teamSize + "v" + size.teamSize + " match. Your party won’t fit on one team!");
                        return;
                    }
                    if (this.isPlayerAlreadyQueued(player)) {
                        player.sendMessage("§cYou are already queued for an Ante Up match!");
                        return;
                    }
                    for (Athlete mem : partyMembers) {
                        int memMBA = PlayerDb.get(mem.getPlayer().getUniqueId(), PlayerDb.Stat.MBA_BUCKS).join();
                        if (memMBA >= neededMBA) continue;
                        player.sendMessage("§cParty member §e" + mem.getName() + " §cdoes not have enough MBA Bucks!");
                        return;
                    }
                    for (Athlete mem : partyMembers) {
                        this.queuePlayer(mem.getPlayer(), ante, size);
                    }
                    this.broadcastQueued(player.getName(), partyMembers.size(), ante, size);
                } else {
                    player.sendMessage(Message.onlyPartyLeader());
                }
            } else {
                this.queuePlayer(player, ante, size);
                this.broadcastQueued(player.getName(), 1, ante, size);
            }
            player.closeInventory();
        });
    }

    private void broadcastQueued(String playerName, int count, AnteOption ante, MatchSize size) {
        Bukkit.broadcastMessage("§e" + playerName + "§a (+" + (count - 1) + " more if party) queued for " + size.teamSize + "v" + size.teamSize + " [Ante: " + ante.cost + " MBA Bucks]");
    }

    private void queuePlayer(Player player, AnteOption ante, MatchSize size) {
        QueueKey key = new QueueKey(ante, size);
        queueMap.putIfAbsent(key, new ArrayList<>());
        List<UUID> queued = queueMap.get(key);
        if (!queued.contains(player.getUniqueId())) {
            queued.add(player.getUniqueId());
            player.sendMessage("§aYou have queued for " + size.teamSize + "v" + size.teamSize + " at the " + ante.cost + " MBA Bucks court!");
        }
        this.tryMatchPlayers(key);
    }

    private void tryMatchPlayers(QueueKey key) {
        List<UUID> queued = queueMap.get(key);
        if (queued == null) {
            return;
        }
        int requiredPlayers = key.size.teamSize * 2;
        if (queued.size() < requiredPlayers) {
            return;
        }
        ArrayList<UUID> selected = new ArrayList<>(queued.subList(0, requiredPlayers));
        queued.removeAll(selected);
        ArrayList<Player> selectedPlayers = new ArrayList<>();
        for (UUID id : selected) {
            Player p = Bukkit.getPlayer(id);
            if (p == null || !p.isOnline()) continue;
            selectedPlayers.add(p);
        }
        if (selectedPlayers.size() < requiredPlayers) {
            queued.addAll(selected);
            return;
        }
        this.startMatchmakingAnteUp(selectedPlayers, key.ante, key.size);
    }

    private boolean isCourtActive(Location court) {
        return activeCourts.contains(court);
    }

    private void startMatchmakingAnteUp(List<Player> players, AnteOption ante, MatchSize matchSize) {
        for (Player player : players) {
            this.removePlayerFromAllQueues(player);
        }
        Map<GoalGame.Team, List<Player>> teams = AnteUpManager.formTeams(players, matchSize.teamSize);
        List<Location> allCourts = Arrays.asList(COURT_100, COURT_500, COURT_1000, COURT_2500);
        ArrayList<Location> availableCourts = new ArrayList<>();
        for (Location court : allCourts) {
            if (activeCourts.contains(court)) continue;
            availableCourts.add(court);
        }
        if (availableCourts.isEmpty()) {
            players.forEach(p -> p.sendMessage("§cNo available court for this ante."));
            Partix.getInstance().getLogger().info("[AnteUp] No available court found for ante game.");
            return;
        }
        final Location courtLocation = availableCourts.get(new Random().nextInt(availableCourts.size()));
        activeCourts.add(courtLocation);
        Partix.getInstance().getLogger().info("[AnteUp] Court " + courtLocation + " has been marked as active.");
        Settings settings = new Settings(WinType.TIME_5, GameType.AUTOMATIC, WaitType.SHORT, CompType.RANKED, matchSize.teamSize, false, false, false, 1, GameEffectType.NONE);
        final BasketballGame game = new BasketballGame(settings, courtLocation, 26.0, 2.8, 0.45, 0.475, 0.575);
        game.setCustomProperty("anteUp", true);
        game.setCustomProperty("anteAmount", ante.cost);
        game.setCustomProperty("matchSize", matchSize.teamSize);
        for (Player player : players) {
            PlayerDb.remove(player.getUniqueId(), PlayerDb.Stat.MBA_BUCKS, ante.cost);
            player.sendMessage("§c- " + ante.cost + " MBA Bucks deducted.");
        }
        for (Player player : teams.get(GoalGame.Team.HOME)) {
            game.join(AthleteManager.get(player.getUniqueId()));
            game.joinTeam(player, GoalGame.Team.HOME);
        }
        for (Player player : teams.get(GoalGame.Team.AWAY)) {
            game.join(AthleteManager.get(player.getUniqueId()));
            game.joinTeam(player, GoalGame.Team.AWAY);
        }
        players.forEach(p -> p.teleport(courtLocation));
        game.startCountdown(GoalGame.State.FACEOFF, 15);
        new BukkitRunnable() {
            boolean hasResetOccurred = false;

            public void run() {
                game.onTick();
                if (game.getState().equals(GoalGame.State.FINAL) && !this.hasResetOccurred) {
                    this.hasResetOccurred = true;
                    game.startCountdown(GoalGame.State.FINAL, game.settings.waitType.med);
                    new BukkitRunnable() {

                        public void run() {
                            for (Player p : game.getPlayers()) {
                                AnteUpManager.resetPlayerState(p);
                            }
                            game.reset();
                            activeCourts.remove(courtLocation);
                            Partix.getInstance().getLogger().info("[AnteUp] Court " + courtLocation + " has been marked as inactive.");
                        }
                    }.runTaskLater(Partix.getInstance(), 400L);
                    this.cancel();
                }
            }
        }.runTaskTimer(Partix.getInstance(), 0L, 1L);
    }

    private void removePlayerFromAllQueues(Player player) {
        UUID uuid = player.getUniqueId();
        for (List<UUID> list : queueMap.values()) {
            list.remove(uuid);
        }
    }

    private Location getCourtLocation(AnteOption ante) {
        return switch (ante) {
            case ANTE_100 -> COURT_100;
            case ANTE_500 -> COURT_500;
            case ANTE_1000 -> COURT_1000;
            case ANTE_2500 -> COURT_2500;
        };
    }

    public void leaveQueue(Player player) {
        for (Map.Entry<QueueKey, List<UUID>> entry : queueMap.entrySet()) {
            List<UUID> list = entry.getValue();
            if (!list.remove(player.getUniqueId())) continue;
            player.sendMessage("§cYou have left the Ante Up queue for " + entry.getKey().size.teamSize + "v" + entry.getKey().size.teamSize + " [Ante: " + entry.getKey().ante.cost + "]");
            break;
        }
    }

    private boolean isPlayerAlreadyQueued(Player player) {
        UUID uuid = player.getUniqueId();
        for (List<UUID> list : queueMap.values()) {
            if (!list.contains(uuid)) continue;
            return true;
        }
        return false;
    }

    public enum MatchSize {
        ONE_VS_ONE(1),
        TWO_VS_TWO(2),
        THREE_VS_THREE(3);

        public final int teamSize;

        MatchSize(int teamSize) {
            this.teamSize = teamSize;
        }
    }

    public enum AnteOption {
        ANTE_100(100),
        ANTE_500(500),
        ANTE_1000(1000),
        ANTE_2500(2500);

        public final int cost;

        AnteOption(int cost) {
            this.cost = cost;
        }
    }

    private static class QueueKey {
        public final AnteOption ante;
        public final MatchSize size;

        public QueueKey(AnteOption ante, MatchSize size) {
            this.ante = ante;
            this.size = size;
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof QueueKey other)) {
                return false;
            }
            return this.ante == other.ante && this.size == other.size;
        }

        public int hashCode() {
            return Objects.hash(this.ante, this.size);
        }
    }

    private static class MbaMultiplierPrize {
        final String name;
        final double multiplier;
        final Material mat;
        final int weight;

        MbaMultiplierPrize(String name, double multiplier, Material mat, int weight) {
            this.name = name;
            this.multiplier = multiplier;
            this.mat = mat;
            this.weight = weight;
        }

        ItemStack toItem(int baseMBA) {
            int potential = (int) Math.round((double) baseMBA * this.multiplier);
            return Items.get(Component.text(this.name + " => " + potential + " MBA Bucks"), this.mat);
        }
    }
}

