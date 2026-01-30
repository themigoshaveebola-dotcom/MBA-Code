package me.x_tias.partix.plugin.listener;

import me.x_tias.partix.Partix;
import me.x_tias.partix.mini.basketball.BasketballGame;
import me.x_tias.partix.mini.game.GoalGame;
import me.x_tias.partix.mini.lobby.RecLobby;
import me.x_tias.partix.plugin.athlete.Athlete;
import me.x_tias.partix.plugin.athlete.AthleteManager;
import me.x_tias.partix.server.Place;
import org.bukkit.Bukkit;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Manages location-based music playback for different areas of the server.
 * - Plays music in RecLobby continuously
 * - Plays music in arenas only when NOT in regulation/overtime
 * - Pauses arena music during regulation/overtime
 * - Resumes music during timeouts
 * - Stops music when timeout ends
 */
public class LocationMusicManager implements Listener {
    private final Map<UUID, String> currentlyPlayingTrack = new HashMap<>();
    private final Map<UUID, BukkitTask> musicTasks = new HashMap<>();
    private final Map<UUID, Boolean> musicPaused = new HashMap<>();
    private final Map<UUID, Integer> playlistIndex = new HashMap<>(); // Track position in playlist
    
    // Define playlists for each area - CUSTOMIZE THESE WITH YOUR SOUND IDENTIFIERS
    private final List<String> recLobbyPlaylist = Arrays.asList(
        "banditdontoliver",
        "idontneednohelp",
        "iknow",
        "sofresh",
        "babyboo"
    );
    
    private final List<String> arenaPlaylist = Arrays.asList(
        "banditdontoliver",
        "idontneednohelp",
        "iknow",
        "babyboo",
        "sofresh"
    );
    
    // Track durations in seconds - ADJUST THESE TO MATCH YOUR ACTUAL TRACK LENGTHS
    private final Map<String, Integer> trackDurations = new HashMap<String, Integer>() {{
        // Rec Lobby tracks
        put("banditdontoliver", 147);
        put("idontneednohelp", 136);
        put("iknow", 213);
        put("sofresh", 240);
        put("babyboo", 226);
        
        // Arena tracks
        put("BANDITDONTOLIVER", 147);
        put("idontneednohelp", 136);
        put("IKNOW", 213);
        put("SOFRESH", 240);
        put("babyboo", 226);
    }};
    
    /**
     * Start tracking a player's location and playing appropriate music
     */
    public void startLocationMusic(Player player) {
        UUID uuid = player.getUniqueId();
        
        // Cancel existing task if any
        stopLocationMusic(player);
        
        // Check player's location every 2 seconds
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    this.cancel();
                    cleanup(uuid);
                    return;
                }
                
                updateMusicForPlayer(player);
            }
        }.runTaskTimer(Partix.getInstance(), 0L, 40L); // Check every 2 seconds (40 ticks)
        
        musicTasks.put(uuid, task);
    }
    
    /**
     * Stop all music tracking for a player
     */
    public void stopLocationMusic(Player player) {
        UUID uuid = player.getUniqueId();
        
        // Cancel task
        BukkitTask task = musicTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
        
        // Stop current music
        String current = currentlyPlayingTrack.remove(uuid);
        if (current != null) {
            player.stopSound(current, SoundCategory.RECORDS);
        }
        
        musicPaused.remove(uuid);
    }
    
    /**
     * Update music based on player's current location and game state
     */
    private void updateMusicForPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        Athlete athlete = AthleteManager.get(uuid);
        
        if (athlete == null) {
            return;
        }
        
        Place place = athlete.getPlace();
        List<String> playlist = null;
        boolean shouldPlay = false;
        
        // Determine location and whether music should play
        if (place instanceof RecLobby) {
            // Always play music in RecLobby
            playlist = recLobbyPlaylist;
            shouldPlay = true;
            
        } else if (place instanceof BasketballGame) {
            BasketballGame game = (BasketballGame) place;
            GoalGame.State state = game.getState();
            
            // Skip music for ranked games and 1v1 games
            boolean isRanked = game.settings.compType == me.x_tias.partix.plugin.settings.CompType.RANKED;
            boolean is1v1 = game.isHalfCourt1v1 || game.settings.playersPerTeam == 1;
            
            // Don't play music in ranked or 1v1 games
            if (isRanked || is1v1) {
                shouldPlay = false;
            }
            // Play music in arenas ONLY when NOT in regulation/overtime
            // Music plays during: PREGAME, STOPPAGE (includes timeouts), FINAL, etc.
            else if (state != GoalGame.State.REGULATION && state != GoalGame.State.OVERTIME) {
                playlist = arenaPlaylist;
                shouldPlay = true;
            } else {
                // In regulation/overtime - pause/stop music
                shouldPlay = false;
            }
        }
        
        // Handle music playback
        if (shouldPlay && playlist != null) {
            // Music should be playing
            if (Boolean.TRUE.equals(musicPaused.get(uuid))) {
                // Was paused, now resume (play next track in sequence)
                musicPaused.put(uuid, false);
                playNextTrack(player, playlist);
            } else if (currentlyPlayingTrack.get(uuid) == null) {
                // No music playing, start from random position in playlist
                int randomStart = new Random().nextInt(playlist.size());
                playlistIndex.put(uuid, randomStart);
                playNextTrack(player, playlist);
            }
            // If music is already playing, let it continue
            
        } else {
            // Music should NOT be playing (in regulation/overtime)
            if (!Boolean.TRUE.equals(musicPaused.get(uuid)) && currentlyPlayingTrack.get(uuid) != null) {
                // Was playing, now pause/stop
                stopCurrentMusic(player);
                musicPaused.put(uuid, true);
            }
        }
    }
    
    /**
     * Play the next track in sequence from the playlist
     */
    private void playNextTrack(Player player, List<String> playlist) {
        if (playlist == null || playlist.isEmpty()) {
            return;
        }
        
        UUID uuid = player.getUniqueId();
        String current = currentlyPlayingTrack.get(uuid);
        
        // Stop current track if playing
        if (current != null) {
            player.stopSound(current, SoundCategory.RECORDS);
        }
        
        // Get next track in sequence
        int index = playlistIndex.getOrDefault(uuid, 0);
        if (index >= playlist.size()) {
            index = 0; // Loop back to start
        }
        
        String newTrack = playlist.get(index);
        
        // Move to next track for next time
        playlistIndex.put(uuid, index + 1);
        
        // Play new track
        // Use RECORDS category for music (doesn't interfere with game sounds)
        // Volume: 0.5f (50%) - adjust to your preference
        // Pitch: 1.0f (normal speed)
        player.playSound(player.getLocation(), newTrack, SoundCategory.RECORDS, 0.5f, 1.0f);
        
        currentlyPlayingTrack.put(uuid, newTrack);
        
        // Schedule next track based on this track's duration
        int duration = trackDurations.getOrDefault(newTrack, 60); // Default to 60 seconds if not specified
        long delayTicks = duration * 20L; // Convert seconds to ticks (20 ticks = 1 second)
        
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline() && newTrack.equals(currentlyPlayingTrack.get(uuid))) {
                    // Track finished, play next one
                    Athlete athlete = AthleteManager.get(uuid);
                    if (athlete != null) {
                        Place place = athlete.getPlace();
                        List<String> currentPlaylist = null;
                        
                        if (place instanceof RecLobby) {
                            currentPlaylist = recLobbyPlaylist;
                        } else if (place instanceof BasketballGame) {
                            BasketballGame game = (BasketballGame) place;
                            GoalGame.State state = game.getState();
                            if (state != GoalGame.State.REGULATION && state != GoalGame.State.OVERTIME) {
                                currentPlaylist = arenaPlaylist;
                            }
                        }
                        
                        // Play next track if still in same location
                        if (currentPlaylist != null && currentPlaylist.equals(playlist)) {
                            playNextTrack(player, currentPlaylist);
                        }
                    }
                }
        playlistIndex.remove(uuid);
            }
        }.runTaskLater(Partix.getInstance(), delayTicks);
    }
    
    /**
     * Stop the currently playing music for a player
     */
    private void stopCurrentMusic(Player player) {
        UUID uuid = player.getUniqueId();
        String current = currentlyPlayingTrack.remove(uuid);
        
        if (current != null) {
            player.stopSound(current, SoundCategory.RECORDS);
        }
    }
    
    /**
     * Clean up all tracking data for a player
     */
    private void cleanup(UUID uuid) {
        musicTasks.remove(uuid);
        currentlyPlayingTrack.remove(uuid);
        musicPaused.remove(uuid);
    }
    
    /**
     * Handle player disconnect
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        stopLocationMusic(event.getPlayer());
    }
    
    /**
     * Force update music for all players in a specific game
     * Useful for triggering music changes when game state changes
     */
    public void updateGameMusic(BasketballGame game) {
        for (Player player : game.getPlayers()) {
            updateMusicForPlayer(player);
        }
    }
    
    /**
     * Force stop all music for players in a game
     */
    public void stopGameMusic(BasketballGame game) {
        for (Player player : game.getPlayers()) {
            stopCurrentMusic(player);
        }
    }
}
