package me.x_tias.partix.season;

import lombok.Getter;
import me.x_tias.partix.Partix;
import me.x_tias.partix.database.PlayerDb;
import me.x_tias.partix.server.rank.Ranks;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles Season 1 stat leaderboards for various statistics
 * Format: [TEAM] [RANK] Username - Stat
 * Example: [CHI] [ADMIN] posterizing - 1000 points
 */
public class SeasonStatLeaderboard {
    
    @Getter
    private static final Map<PlayerDb.Stat, Map<Integer, String>> leaderboards = new ConcurrentHashMap<>();
    
    @Getter
    private static int secondsUntilUpdate = 300; // 5 minutes in seconds
    
    // Season 1 stats to track
    private static final PlayerDb.Stat[] SEASON_STATS = {
        PlayerDb.Stat.SEASON_1_POINTS,
        PlayerDb.Stat.SEASON_1_ASSISTS,
        PlayerDb.Stat.SEASON_1_REBOUNDS,
        PlayerDb.Stat.SEASON_1_STEALS,
        PlayerDb.Stat.SEASON_1_BLOCKS,
        PlayerDb.Stat.SEASON_1_WINS,
        PlayerDb.Stat.SEASON_1_GAMES_PLAYED
    };
    
    public static void setup() {
        // Initialize leaderboard maps for each stat
        for (PlayerDb.Stat stat : SEASON_STATS) {
            leaderboards.put(stat, new ConcurrentHashMap<>());
        }
        
        // Update leaderboards every 5 minutes (6000 ticks)
        new BukkitRunnable() {
            @Override
            public void run() {
                updateAllLeaderboards();
                secondsUntilUpdate = 300; // Reset countdown
            }
        }.runTaskTimerAsynchronously(Partix.getInstance(), 30L, 6000L);
        
        // Countdown timer (updates every second)
        new BukkitRunnable() {
            @Override
            public void run() {
                if (secondsUntilUpdate > 0) {
                    secondsUntilUpdate--;
                }
            }
        }.runTaskTimerAsynchronously(Partix.getInstance(), 20L, 20L);
    }
    
    private static void updateAllLeaderboards() {
        for (PlayerDb.Stat stat : SEASON_STATS) {
            updateLeaderboard(stat);
        }
    }
    
    private static void updateLeaderboard(PlayerDb.Stat stat) {
        PlayerDb.getTop(stat, 10).thenAccept(topPlayers -> {
            Map<Integer, String> leaderboard = leaderboards.get(stat);
            leaderboard.clear();
            
            for (int i = 1; i <= 10; i++) {
                final int position = i;
                UUID playerUUID = topPlayers.get(i);
                
                if (playerUUID == null) {
                    leaderboard.put(position, formatEntry(position, null, null, 0, stat));
                } else {
                    OfflinePlayer player = Bukkit.getOfflinePlayer(playerUUID);
                    final String playerName = player.hasPlayedBefore() || player.isOnline() 
                        ? player.getName() 
                        : "Unknown";
                    
                    // Get rank info
                    final String rankTag = getRankTag(playerName);
                    
                    // Get the stat value
                    PlayerDb.get(playerUUID, stat).thenAccept(statValue -> {
                        leaderboard.put(position, formatEntry(position, rankTag, playerName, statValue, stat));
                    });
                }
            }
        });
    }
    
    /**
     * Formats a leaderboard entry
     * Format: #. [RANK] Username - Value StatName
     */
    private static String formatEntry(int position, String rankTag, String playerName, int value, PlayerDb.Stat stat) {
        if (playerName == null) {
            return "§7" + position + ". §7---";
        }
        
        StringBuilder entry = new StringBuilder();
        
        // Position with color
        String posColor = switch (position) {
            case 1 -> "§6§l"; // Gold
            case 2 -> "§f§l"; // White
            case 3 -> "§c§l"; // Red
            default -> "§e";  // Yellow
        };
        
        entry.append(posColor).append(position).append(". §r");
        
        // Rank tag (if player has a rank)
        if (rankTag != null && !rankTag.isEmpty()) {
            entry.append(rankTag).append(" ");
        }
        
        // Player name
        entry.append("§f").append(playerName);
        
        // Stat value
        entry.append(" §7- §e").append(value).append(" §7").append(getStatDisplayName(stat));
        
        return entry.toString();
    }
    
    /**
     * Gets the team tag for a player (e.g., [CHI], [LAL])
     */
    private static String getTeamTag(String playerName) {
        if (Ranks.getScoreboard() == null) return "[FA]";
        
        Team team = Ranks.getScoreboard().getEntryTeam(playerName);
        if (team == null) return "§7[FA]"; // Free Agent
        
        // Extract team abbreviation from team name
        String teamName = team.getName().toLowerCase();
        
        // MBA Teams
        if (teamName.contains("washington")) return "§9[WAS]";
        if (teamName.contains("philadelphia")) return "§c[PHI]";
        if (teamName.contains("chicago")) return "§c[CHI]";
        if (teamName.contains("brooklyn")) return "§0[BKN]";
        if (teamName.contains("miami")) return "§4[MIA]";
        if (teamName.contains("atlanta")) return "§c[ATL]";
        if (teamName.contains("lacreepers")) return "§5[LAC]";
        if (teamName.contains("boston")) return "§a[BOS]";
        
        return "§7[FA]"; // Default to Free Agent if not recognized
    }
    
    /**
     * Gets the rank tag for a player (e.g., [ADMIN], [VIP])
     */
    private static String getRankTag(String playerName) {
        if (Ranks.getScoreboard() == null) return null;
        
        Team team = Ranks.getScoreboard().getEntryTeam(playerName);
        if (team == null) return null;
        
        String teamName = team.getName().toLowerCase();
        
        // Check for staff/special ranks (these take priority over team tags)
        if (teamName.contains("admin")) return "§c§l[ADMIN]§r";
        if (teamName.contains("mod")) return "§9§l[MOD]§r";
        if (teamName.contains("media")) return "§d§l[MEDIA]§r";
        if (teamName.contains("coach")) return "§c§l[COACH]§r";
        if (teamName.contains("referee")) return "§0§l[REF]§r";
        if (teamName.contains("pro")) return "§6§l[PRO]§r";
        if (teamName.contains("vip")) return "§a§l[VIP]§r";
        
        return null; // No special rank
    }
    
    /**
     * Gets the display name for a stat
     */
    private static String getStatDisplayName(PlayerDb.Stat stat) {
        return switch (stat) {
            case SEASON_1_POINTS -> "Points";
            case SEASON_1_ASSISTS -> "Assists";
            case SEASON_1_REBOUNDS -> "Rebounds";
            case SEASON_1_STEALS -> "Steals";
            case SEASON_1_BLOCKS -> "Blocks";
            case SEASON_1_WINS -> "Wins";
            case SEASON_1_GAMES_PLAYED -> "Games";
            default -> "Stats";
        };
    }
    
    /**
     * Gets the title for a leaderboard
     */
    public static String getLeaderboardTitle(PlayerDb.Stat stat) {
        return switch (stat) {
            case SEASON_1_POINTS -> "§6§lSeason 1 Points Leaderboard";
            case SEASON_1_ASSISTS -> "§6§lSeason 1 Assists Leaderboard";
            case SEASON_1_REBOUNDS -> "§6§lSeason 1 Rebounds Leaderboard";
            case SEASON_1_STEALS -> "§6§lSeason 1 Steals Leaderboard";
            case SEASON_1_BLOCKS -> "§6§lSeason 1 Blocks Leaderboard";
            case SEASON_1_WINS -> "§6§lSeason 1 Wins Leaderboard";
            case SEASON_1_GAMES_PLAYED -> "§6§lSeason 1 Games Played";
            default -> "§6§lSeason 1 Leaderboard";
        };
    }
    
    /**
     * Gets a specific leaderboard entry
     */
    public static String getEntry(PlayerDb.Stat stat, int position) {
        Map<Integer, String> leaderboard = leaderboards.get(stat);
        if (leaderboard == null) return "§7" + position + ". §7---";
        return leaderboard.getOrDefault(position, "§7" + position + ". §7---");
    }
    
    /**
     * Gets all entries for a leaderboard (1-10)
     */
    public static List<String> getAllEntries(PlayerDb.Stat stat) {
        List<String> entries = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            entries.add(getEntry(stat, i));
        }
        return entries;
    }
}
