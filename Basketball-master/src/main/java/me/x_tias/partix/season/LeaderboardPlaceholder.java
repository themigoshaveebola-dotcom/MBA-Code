package me.x_tias.partix.season;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.x_tias.partix.database.PlayerDb;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

/**
 * PlaceholderAPI expansion for Basketball leaderboards
 * Provides placeholders for DecentHolograms and other plugins
 * 
 * Usage examples:
 * %basketball_season1_points_1% - First place in Season 1 points
 * %basketball_career_assists_5% - Fifth place in career assists
 */
public class LeaderboardPlaceholder extends PlaceholderExpansion {

    @Override
    public @NotNull String getIdentifier() {
        return "basketball";
    }

    @Override
    public @NotNull String getAuthor() {
        return "x_tias";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true; // Keep this expansion loaded across reloads
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        // Season 1 Points Leaderboard
        if (params.startsWith("season1_points_")) {
            try {
                int position = Integer.parseInt(params.substring(15));
                return SeasonStatLeaderboard.getEntry(PlayerDb.Stat.SEASON_1_POINTS, position);
            } catch (NumberFormatException e) {
                return "§cInvalid position";
            }
        }
        
        // Season 1 Assists Leaderboard
        if (params.startsWith("season1_assists_")) {
            try {
                int position = Integer.parseInt(params.substring(16));
                return SeasonStatLeaderboard.getEntry(PlayerDb.Stat.SEASON_1_ASSISTS, position);
            } catch (NumberFormatException e) {
                return "§cInvalid position";
            }
        }
        
        // Season 1 Rebounds Leaderboard
        if (params.startsWith("season1_rebounds_")) {
            try {
                int position = Integer.parseInt(params.substring(17));
                return SeasonStatLeaderboard.getEntry(PlayerDb.Stat.SEASON_1_REBOUNDS, position);
            } catch (NumberFormatException e) {
                return "§cInvalid position";
            }
        }
        
        // Season 1 Steals Leaderboard
        if (params.startsWith("season1_steals_")) {
            try {
                int position = Integer.parseInt(params.substring(15));
                return SeasonStatLeaderboard.getEntry(PlayerDb.Stat.SEASON_1_STEALS, position);
            } catch (NumberFormatException e) {
                return "§cInvalid position";
            }
        }
        
        // Season 1 Blocks Leaderboard
        if (params.startsWith("season1_blocks_")) {
            try {
                int position = Integer.parseInt(params.substring(15));
                return SeasonStatLeaderboard.getEntry(PlayerDb.Stat.SEASON_1_BLOCKS, position);
            } catch (NumberFormatException e) {
                return "§cInvalid position";
            }
        }
        
        // Season 1 Wins Leaderboard
        if (params.startsWith("season1_wins_")) {
            try {
                int position = Integer.parseInt(params.substring(13));
                return SeasonStatLeaderboard.getEntry(PlayerDb.Stat.SEASON_1_WINS, position);
            } catch (NumberFormatException e) {
                return "§cInvalid position";
            }
        }
        
        // Season 1 Games Played Leaderboard
        if (params.startsWith("season1_games_")) {
            try {
                int position = Integer.parseInt(params.substring(14));
                return SeasonStatLeaderboard.getEntry(PlayerDb.Stat.SEASON_1_GAMES_PLAYED, position);
            } catch (NumberFormatException e) {
                return "§cInvalid position";
            }
        }
        
        // Career Points Leaderboard
        if (params.startsWith("career_points_")) {
            try {
                int position = Integer.parseInt(params.substring(14));
                return CareerStatLeaderboard.getEntry(PlayerDb.Stat.CAREER_POINTS, position);
            } catch (NumberFormatException e) {
                return "§cInvalid position";
            }
        }
        
        // Career Assists Leaderboard
        if (params.startsWith("career_assists_")) {
            try {
                int position = Integer.parseInt(params.substring(15));
                return CareerStatLeaderboard.getEntry(PlayerDb.Stat.CAREER_ASSISTS, position);
            } catch (NumberFormatException e) {
                return "§cInvalid position";
            }
        }
        
        // Career Rebounds Leaderboard
        if (params.startsWith("career_rebounds_")) {
            try {
                int position = Integer.parseInt(params.substring(16));
                return CareerStatLeaderboard.getEntry(PlayerDb.Stat.CAREER_REBOUNDS, position);
            } catch (NumberFormatException e) {
                return "§cInvalid position";
            }
        }
        
        // Career Steals Leaderboard
        if (params.startsWith("career_steals_")) {
            try {
                int position = Integer.parseInt(params.substring(14));
                return CareerStatLeaderboard.getEntry(PlayerDb.Stat.CAREER_STEALS, position);
            } catch (NumberFormatException e) {
                return "§cInvalid position";
            }
        }
        
        // Career Blocks Leaderboard
        if (params.startsWith("career_blocks_")) {
            try {
                int position = Integer.parseInt(params.substring(14));
                return CareerStatLeaderboard.getEntry(PlayerDb.Stat.CAREER_BLOCKS, position);
            } catch (NumberFormatException e) {
                return "§cInvalid position";
            }
        }
        
        // Career Wins Leaderboard
        if (params.startsWith("career_wins_")) {
            try {
                int position = Integer.parseInt(params.substring(12));
                return CareerStatLeaderboard.getEntry(PlayerDb.Stat.CAREER_WINS, position);
            } catch (NumberFormatException e) {
                return "§cInvalid position";
            }
        }
        
        // Career Games Played Leaderboard
        if (params.startsWith("career_games_")) {
            try {
                int position = Integer.parseInt(params.substring(13));
                return CareerStatLeaderboard.getEntry(PlayerDb.Stat.CAREER_GAMES_PLAYED, position);
            } catch (NumberFormatException e) {
                return "§cInvalid position";
            }
        }
        
        // Rec Wins Leaderboard
        if (params.startsWith("rec_wins_")) {
            try {
                int position = Integer.parseInt(params.substring(9));
                return LeaderboardManager.getRecEntry(PlayerDb.Stat.REC_WINS, position);
            } catch (NumberFormatException e) {
                return "§cInvalid position";
            }
        }
        
        // Rec Games Played Leaderboard
        if (params.startsWith("rec_games_")) {
            try {
                int position = Integer.parseInt(params.substring(10));
                return LeaderboardManager.getRecEntry(PlayerDb.Stat.REC_GAMES, position);
            } catch (NumberFormatException e) {
                return "§cInvalid position";
            }
        }
        
        // Leaderboard titles
        if (params.equals("season1_points_title")) {
            return SeasonStatLeaderboard.getLeaderboardTitle(PlayerDb.Stat.SEASON_1_POINTS);
        }
        if (params.equals("career_points_title")) {
            return CareerStatLeaderboard.getLeaderboardTitle(PlayerDb.Stat.CAREER_POINTS);
        }
        if (params.equals("rec_wins_title")) {
            return LeaderboardManager.getRecTitle(PlayerDb.Stat.REC_WINS);
        }
        if (params.equals("rec_games_title")) {
            return LeaderboardManager.getRecTitle(PlayerDb.Stat.REC_GAMES);
        }
        // Add more title placeholders as needed...
        
        // Countdown placeholders
        if (params.equals("update_countdown")) {
            return LeaderboardManager.getCountdownFormatted();
        }
        if (params.equals("update_seconds")) {
            return String.valueOf(LeaderboardManager.getSecondsUntilUpdate());
        }
        
        return null; // Placeholder not found
    }
}
