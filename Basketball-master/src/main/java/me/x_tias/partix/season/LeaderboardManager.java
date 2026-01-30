package me.x_tias.partix.season;

import me.x_tias.partix.Partix;
import me.x_tias.partix.database.PlayerDb;
import org.bukkit.Bukkit;

/**
 * Manager class to initialize and control all stat leaderboards
 * This includes both Season and Career leaderboards
 */
public class LeaderboardManager {
    
    /**
     * Initializes all leaderboard systems
     * Call this in your main plugin's onEnable() method
     */
    public static void setup() {
        Bukkit.getLogger().info("[Basketball] Initializing leaderboard systems...");
        
        // Setup Season 1 stat leaderboards
        SeasonStatLeaderboard.setup();
        Bukkit.getLogger().info("[Basketball] ✓ Season 1 leaderboards initialized");
        
        // Setup Career stat leaderboards
        CareerStatLeaderboard.setup();
        Bukkit.getLogger().info("[Basketball] ✓ Career leaderboards initialized");
        
        // Setup Rec Center stat leaderboards
        RecStatLeaderboard.setup();
        Bukkit.getLogger().info("[Basketball] ✓ Rec Center leaderboards initialized");
        
        Bukkit.getLogger().info("[Basketball] All leaderboards are now active!");
    }
    
    /**
     * Gets a formatted line for a Season 1 leaderboard hologram
     * @param stat The stat type (e.g., SEASON_1_POINTS)
     * @param position The position (1-10)
     * @return Formatted leaderboard line
     */
    public static String getSeasonEntry(PlayerDb.Stat stat, int position) {
        return SeasonStatLeaderboard.getEntry(stat, position);
    }
    
    /**
     * Gets a formatted line for a Career leaderboard hologram
     * @param stat The stat type (e.g., CAREER_POINTS)
     * @param position The position (1-10)
     * @return Formatted leaderboard line
     */
    public static String getCareerEntry(PlayerDb.Stat stat, int position) {
        return CareerStatLeaderboard.getEntry(stat, position);
    }
    
    /**
     * Gets the title for a Season 1 leaderboard
     */
    public static String getSeasonTitle(PlayerDb.Stat stat) {
        return SeasonStatLeaderboard.getLeaderboardTitle(stat);
    }
    
    /**
     * Gets the title for a Career leaderboard
     */
    public static String getCareerTitle(PlayerDb.Stat stat) {
        return CareerStatLeaderboard.getLeaderboardTitle(stat);
    }
    
    /**
     * Gets a formatted line for a Rec Center leaderboard hologram
     * @param stat The stat type (e.g., REC_WINS)
     * @param position The position (1-10)
     * @return Formatted leaderboard line
     */
    public static String getRecEntry(PlayerDb.Stat stat, int position) {
        return RecStatLeaderboard.getEntry(stat, position);
    }
    
    /**
     * Gets the title for a Rec Center leaderboard
     */
    public static String getRecTitle(PlayerDb.Stat stat) {
        return RecStatLeaderboard.getLeaderboardTitle(stat);
    }
    
    /**
     * Gets the countdown until next leaderboard update (in seconds)
     * @return Seconds until next update
     */
    public static int getSecondsUntilUpdate() {
        return SeasonStatLeaderboard.getSecondsUntilUpdate();
    }
    
    /**
     * Gets formatted countdown string (MM:SS)
     * @return Formatted countdown string
     */
    public static String getCountdownFormatted() {
        int seconds = SeasonStatLeaderboard.getSecondsUntilUpdate();
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format("%d:%02d", minutes, secs);
    }
}
