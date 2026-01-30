/*
 * Rec Lobby - A separate lobby for Recreational basketball games
 * Features quarter-based gameplay (4 quarters, 4 minutes each) instead of first to 21
 */
package me.x_tias.partix.mini.lobby;

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
import me.x_tias.partix.server.specific.Lobby;
import me.x_tias.partix.util.Colour;
import me.x_tias.partix.util.Items;
import me.x_tias.partix.util.Message;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.*;

public class RecLobby extends Lobby {
    public static final ItemStack FILLER = Items.get(Component.text(" "), Material.BLACK_STAINED_GLASS_PANE, 1, " ");
    
    // Rec-specific settings: 4 quarters, 4 minutes each, bigger payouts
    private final Settings recSettings = new Settings(
        WinType.TIME_5,           // Will be modified to 4 minutes per quarter
        GameType.AUTOMATIC, 
        WaitType.MEDIUM, 
        CompType.RANKED,          // Ranked competition type for better rewards
        3,                        // 3v3 only
        false, 
        false, 
        false, 
        4,                        // 4 quarters
        GameEffectType.NONE
    );
    
    // Rec court locations (5 courts)
    private final List<Location> recCourts = new ArrayList<>();
    
    // Queue system for 3v3 Rec games
    private final List<Athlete> recQueue = new ArrayList<>();
    private final HashMap<Location, BasketballGame> recGames = new HashMap<>();
    private final HashMap<UUID, Integer> playerQueueNotifier = new HashMap<>();
    
    // Spawn location for Rec lobby
    private final Location recLobbySpawn;
    
    // Boss bar counter
    int i = 0;
    
    public RecLobby() {
        // Set Rec lobby spawn location (adjust coordinates as needed for your world)
        this.recLobbySpawn = new Location(Bukkit.getWorlds().getFirst(), -171, -62, -121, 180.0f, 0.0f);
        
        // Initialize 5 Rec courts (adjust coordinates as needed)
        // These should be separate from ranked and MyCourt locations
        this.recCourts.add(new Location(Bukkit.getWorlds().getFirst(), 1100.5, -60, 1000.5));
        this.recCourts.add(new Location(Bukkit.getWorlds().getFirst(), 1200.5, -60, 1000.5));
        this.recCourts.add(new Location(Bukkit.getWorlds().getFirst(), 1300.5, -60, 1000.5));
        this.recCourts.add(new Location(Bukkit.getWorlds().getFirst(), 1400.5, -60, 1000.5));
        this.recCourts.add(new Location(Bukkit.getWorlds().getFirst(), 1500.5, -60, 1000.5));
        
        // Adjust the settings for 4 minute quarters
        this.recSettings.winType.amount = 4;  // 4 minutes per quarter
        this.recSettings.winType.timed = true;
    }
    
    @Override
    public void onJoin(Athlete... athletes) {
        for (Athlete athlete : athletes) {
            Player player = athlete.getPlayer();
            player.teleport(this.recLobbySpawn);
            athlete.setSpectator(false);
            player.getInventory().clear();
            this.giveItems(player);
            player.sendMessage("§6§l» §eWelcome to the §6REC CENTER §e«");
            player.sendMessage("§7Play 4-quarter games (4 minutes each) for bigger rewards!");
            player.sendMessage("§73v3 games only - Queue up with your party or solo!");
            
            // Start location-based music
            me.x_tias.partix.Partix.getInstance().getLocationMusicManager().startLocationMusic(player);
        }
    }
    
    @Override
    public void onQuit(Athlete... athletes) {
        for (Athlete athlete : athletes) {
            Player player = athlete.getPlayer();
            // Remove from queue if they disconnect
            this.recQueue.remove(athlete);
            this.playerQueueNotifier.remove(player.getUniqueId());
            
            // Stop music
            me.x_tias.partix.Partix.getInstance().getLocationMusicManager().stopLocationMusic(player);
        }
    }
    
    @Override
    public void giveItems(Player player) {
        PlayerInventory inv = player.getInventory();
        inv.setItem(0, Items.get(Message.itemName("Queue for Rec", "key.use", player), Material.DIAMOND));
        inv.setItem(8, Items.get(Message.itemName("Return to Main Lobby", "key.use", player), Material.BARRIER));
    }
    
    @Override
    public void onTick() {
        ++this.i;
        if (this.i < 150) {
            this.updateBossBar("§b§lMinecraft Basketball Association §7§l> §f§lSeason 0");
        } else {
            this.updateBossBar("§b§lSUPPORT THE SERVER! §7§l> §f§l");
        }
        if (this.i > 300) {
            this.i = 0;
        }
        
        // Check if we have enough players to start a Rec game (6 players for 3v3)
        if (this.recQueue.size() >= 6) {
            this.startRecGame();
        }
    }
    
    @Override
    public void clickItem(Player player, ItemStack itemStack) {
        Athlete athlete = AthleteManager.get(player.getUniqueId());
        if (athlete == null) {
            return;
        }
        
        // Queue for Rec (Diamond)
        if (itemStack.getType() == Material.DIAMOND) {
            this.joinRecQueue(player);
        }
        
        // Return to Main Lobby (Barrier)
        if (itemStack.getType() == Material.BARRIER) {
            this.returnToMainLobby(athlete);
        }
    }
    
    private void joinRecQueue(Player player) {
        Athlete athlete = AthleteManager.get(player.getUniqueId());
        
        // Check if already in queue - if so, leave instead
        if (this.recQueue.contains(athlete)) {
            this.leaveRecQueue(player);
            return;
        }
        
        // Handle party queueing
        if (athlete.getParty() >= 0) {
            Party party = PartyFactory.get(athlete.getParty());
            
            // Only party leader can queue
            if (!party.leader.equals(player.getUniqueId())) {
                player.sendMessage(Message.onlyPartyLeader());
                return;
            }
            
            // Check party size (max 3 for 3v3)
            if (party.toList().size() > 3) {
                player.sendMessage("§cParty too large! Maximum 3 players for Rec 3v3.");
                return;
            }
            
            // Add entire party to queue
            for (Athlete partyMember : party.toList()) {
                if (!this.recQueue.contains(partyMember)) {
                    this.recQueue.add(partyMember);
                    partyMember.getPlayer().sendMessage("§a§l» §aYour party has joined the Rec queue! §e(" + this.recQueue.size() + "/6)");
                    partyMember.getPlayer().playSound(partyMember.getPlayer().getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.MASTER, 1.0f, 1.5f);
                    this.startQueueNotifier(partyMember.getPlayer());
                }
            }
        } else {
            // Solo queue
            this.recQueue.add(athlete);
            player.sendMessage("§a§l» §aYou have joined the Rec queue! §e(" + this.recQueue.size() + "/6)");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.MASTER, 1.0f, 1.5f);
            this.startQueueNotifier(player);
        }
    }
    
    private void leaveRecQueue(Player player) {
        Athlete athlete = AthleteManager.get(player.getUniqueId());
        
        if (this.recQueue.remove(athlete)) {
            player.sendMessage("§c§l» §cYou have left the Rec queue.");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.MASTER, 1.0f, 0.5f);
            this.playerQueueNotifier.remove(player.getUniqueId());
        } else {
            player.sendMessage("§cYou are not in the Rec queue!");
        }
    }
    
    private void returnToMainLobby(Athlete athlete) {
        Player player = athlete.getPlayer();
        
        // Remove from queue if in it
        this.recQueue.remove(athlete);
        this.playerQueueNotifier.remove(player.getUniqueId());
        
        // Join main lobby
        Hub.hub.join(athlete);
        player.sendMessage("§a§l» §aReturned to Main Lobby");
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.MASTER, 1.0f, 1.0f);
    }
    
    private void startRecGame() {
        // Get 6 players from queue
        List<Athlete> players = new ArrayList<>(this.recQueue.subList(0, 6));
        this.recQueue.removeAll(players);
        
        // Stop notifiers
        for (Athlete athlete : players) {
            this.playerQueueNotifier.remove(athlete.getPlayer().getUniqueId());
        }
        
        // Find available Rec court
        BasketballGame game = this.findAvailableRecCourt();
        
        if (game == null) {
            // No courts available, put players back in queue
            this.recQueue.addAll(0, players);
            for (Athlete athlete : players) {
                athlete.getPlayer().sendMessage("§cNo Rec courts available! You remain in queue.");
            }
            return;
        }
        
        // Join all players to the game
        game.join(players.toArray(new Athlete[0]));
        
        // Notify players
        for (Athlete athlete : players) {
            Player p = athlete.getPlayer();
            p.sendMessage("§a§l» §aRec game starting! §6(4 quarters, 4 minutes each)");
            p.sendMessage("§7Bigger rewards for winning this match!");
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1.0f, 1.0f);
        }
    }
    
    private BasketballGame findAvailableRecCourt() {
        ArrayList<Location> freeCourts = new ArrayList<>(this.recCourts);
        freeCourts.removeAll(this.recGames.keySet());
        
        if (freeCourts.isEmpty()) {
            return null;
        }
        
        Location randomLoc = freeCourts.get(new Random().nextInt(freeCourts.size()));
        BasketballGame game = new BasketballGame(this.recSettings, randomLoc, 26.0, 2.8, 0.45, 0.475, 0.575);
        game.isRecGame = true;  // Mark as Rec game for special handling
        this.recGames.put(randomLoc, game);
        
        return game;
    }
    
    private void startQueueNotifier(Player player) {
        UUID playerId = player.getUniqueId();
        if (this.playerQueueNotifier.containsKey(playerId)) {
            return;
        }
        
        this.playerQueueNotifier.put(playerId, 1);
        
        Bukkit.getScheduler().runTaskTimer(Partix.getInstance(), () -> {
            if (!player.isOnline() || !this.playerQueueNotifier.containsKey(playerId)) {
                player.sendActionBar(Component.text(""));
                this.playerQueueNotifier.remove(playerId);
                return;
            }
            
            // Don't show queue notification if player is in an active game
            Athlete athlete = AthleteManager.get(playerId);
            if (athlete != null && athlete.getPlace() instanceof BasketballGame) {
                // Player is in a game - don't override their action bar
                return;
            }
            
            int queueSize = this.recQueue.size();
            player.sendActionBar(Component.text("§6§lREC QUEUE: §e" + queueSize + "/6 players waiting..."));
        }, 0L, 20L);
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Athlete athlete = AthleteManager.get(player.getUniqueId());
        
        if (athlete != null) {
            this.recQueue.remove(athlete);
            this.playerQueueNotifier.remove(player.getUniqueId());
        }
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Prevent item use except for lobby items
        event.setCancelled(true);
    }
    
    // Called when a Rec game ends to return players here
    public void onRecGameEnd(Athlete... athletes) {
        for (Athlete athlete : athletes) {
            Player player = athlete.getPlayer();
            this.join(athlete);
            player.sendMessage("§6§l» §eReturning to Rec Center...");
        }
    }
    
    public Location getRecLobbySpawn() {
        return this.recLobbySpawn.clone();
    }
    
    public boolean isRecGame(BasketballGame game) {
        return this.recGames.containsValue(game);
    }
    
    public void removeRecGame(Location location) {
        this.recGames.remove(location);
    }
}
