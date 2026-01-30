# Basketball Leaderboard Setup with DecentHolograms

## Overview
Your Basketball plugin now has 14 different leaderboards (7 Season 1 + 7 Career) that update automatically every 30 seconds.

## Leaderboard Format
Each leaderboard displays the top 10 players in this format:
```
1. [TEAM] [RANK] Username - Value Stat
```

Example:
```
1. [CHI] [ADMIN] posterizing - 1000 Points
2. [LAL] [VIP] player123 - 950 Points
3. [FA] noobmaster69 - 500 Points
```

## Available Leaderboards

### Season 1 Leaderboards:
- Season 1 Points
- Season 1 Assists
- Season 1 Rebounds
- Season 1 Steals
- Season 1 Blocks
- Season 1 Wins
- Season 1 Games Played

### Career Leaderboards:
- Career Points
- Career Assists
- Career Rebounds
- Career Steals
- Career Blocks
- Career Wins
- Career Games Played

---

## Setup Instructions

### Step 1: Install DecentHolograms
1. Download DecentHolograms from: https://www.spigotmc.org/resources/decentholograms.96927/
2. Place the JAR file in your `plugins/` folder
3. Restart your server

### Step 2: Create Hologram Files

DecentHolograms stores holograms in YAML files. Create files in `plugins/DecentHolograms/holograms/` folder.

#### Example: Season 1 Points Leaderboard

Create: `plugins/DecentHolograms/holograms/season1_points.yml`

```yaml
season1_points:
  enabled: true
  location:
    world: world
    x: 0.5
    y: 100.0
    z: 0.5
  down-origin: false
  pages:
    1:
      lines:
        - content: '&6&lSeason 1 Points Leaderboard'
          height: 0.3
        - content: ''
          height: 0.2
        - content: '{season1_points_1}'
          height: 0.25
        - content: '{season1_points_2}'
          height: 0.25
        - content: '{season1_points_3}'
          height: 0.25
        - content: '{season1_points_4}'
          height: 0.25
        - content: '{season1_points_5}'
          height: 0.25
        - content: '{season1_points_6}'
          height: 0.25
        - content: '{season1_points_7}'
          height: 0.25
        - content: '{season1_points_8}'
          height: 0.25
        - content: '{season1_points_9}'
          height: 0.25
        - content: '{season1_points_10}'
          height: 0.25
```

### Step 3: Create PlaceholderAPI Extension

You need to create a PlaceholderAPI expansion to bridge your leaderboard data to DecentHolograms.

Create: `src/main/java/me/x_tias/partix/season/LeaderboardPlaceholder.java`

```java
package me.x_tias.partix.season;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.x_tias.partix.database.PlayerDb;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

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
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        // Season 1 Points Leaderboard
        if (params.startsWith("season1_points_")) {
            int position = Integer.parseInt(params.substring(15));
            return SeasonStatLeaderboard.getEntry(PlayerDb.Stat.SEASON_1_POINTS, position);
        }
        
        // Season 1 Assists Leaderboard
        if (params.startsWith("season1_assists_")) {
            int position = Integer.parseInt(params.substring(16));
            return SeasonStatLeaderboard.getEntry(PlayerDb.Stat.SEASON_1_ASSISTS, position);
        }
        
        // Season 1 Rebounds Leaderboard
        if (params.startsWith("season1_rebounds_")) {
            int position = Integer.parseInt(params.substring(17));
            return SeasonStatLeaderboard.getEntry(PlayerDb.Stat.SEASON_1_REBOUNDS, position);
        }
        
        // Season 1 Steals Leaderboard
        if (params.startsWith("season1_steals_")) {
            int position = Integer.parseInt(params.substring(15));
            return SeasonStatLeaderboard.getEntry(PlayerDb.Stat.SEASON_1_STEALS, position);
        }
        
        // Season 1 Blocks Leaderboard
        if (params.startsWith("season1_blocks_")) {
            int position = Integer.parseInt(params.substring(15));
            return SeasonStatLeaderboard.getEntry(PlayerDb.Stat.SEASON_1_BLOCKS, position);
        }
        
        // Season 1 Wins Leaderboard
        if (params.startsWith("season1_wins_")) {
            int position = Integer.parseInt(params.substring(13));
            return SeasonStatLeaderboard.getEntry(PlayerDb.Stat.SEASON_1_WINS, position);
        }
        
        // Season 1 Games Played Leaderboard
        if (params.startsWith("season1_games_")) {
            int position = Integer.parseInt(params.substring(14));
            return SeasonStatLeaderboard.getEntry(PlayerDb.Stat.SEASON_1_GAMES_PLAYED, position);
        }
        
        // Career Points Leaderboard
        if (params.startsWith("career_points_")) {
            int position = Integer.parseInt(params.substring(14));
            return CareerStatLeaderboard.getEntry(PlayerDb.Stat.CAREER_POINTS, position);
        }
        
        // Career Assists Leaderboard
        if (params.startsWith("career_assists_")) {
            int position = Integer.parseInt(params.substring(15));
            return CareerStatLeaderboard.getEntry(PlayerDb.Stat.CAREER_ASSISTS, position);
        }
        
        // Career Rebounds Leaderboard
        if (params.startsWith("career_rebounds_")) {
            int position = Integer.parseInt(params.substring(16));
            return CareerStatLeaderboard.getEntry(PlayerDb.Stat.CAREER_REBOUNDS, position);
        }
        
        // Career Steals Leaderboard
        if (params.startsWith("career_steals_")) {
            int position = Integer.parseInt(params.substring(14));
            return CareerStatLeaderboard.getEntry(PlayerDb.Stat.CAREER_STEALS, position);
        }
        
        // Career Blocks Leaderboard
        if (params.startsWith("career_blocks_")) {
            int position = Integer.parseInt(params.substring(14));
            return CareerStatLeaderboard.getEntry(PlayerDb.Stat.CAREER_BLOCKS, position);
        }
        
        // Career Wins Leaderboard
        if (params.startsWith("career_wins_")) {
            int position = Integer.parseInt(params.substring(12));
            return CareerStatLeaderboard.getEntry(PlayerDb.Stat.CAREER_WINS, position);
        }
        
        // Career Games Played Leaderboard
        if (params.startsWith("career_games_")) {
            int position = Integer.parseInt(params.substring(13));
            return CareerStatLeaderboard.getEntry(PlayerDb.Stat.CAREER_GAMES_PLAYED, position);
        }
        
        return null;
    }
}
```

### Step 4: Register the PlaceholderAPI Extension

Add this to your `Partix.java` in the `placeholders()` method:

```java
new LeaderboardPlaceholder().register();
```

### Step 5: Add PlaceholderAPI Dependency

Make sure you have PlaceholderAPI as a dependency in your `build.gradle.kts`:

```kotlin
dependencies {
    compileOnly("me.clip:placeholderapi:2.11.5")
}
```

And in your `plugin.yml`, add:

```yaml
depend: [PlaceholderAPI]
```

---

## Creating All 14 Leaderboards

Here are the placeholder patterns for each leaderboard:

### Season 1 Leaderboards:
1. **Points**: `%basketball_season1_points_1%` through `%basketball_season1_points_10%`
2. **Assists**: `%basketball_season1_assists_1%` through `%basketball_season1_assists_10%`
3. **Rebounds**: `%basketball_season1_rebounds_1%` through `%basketball_season1_rebounds_10%`
4. **Steals**: `%basketball_season1_steals_1%` through `%basketball_season1_steals_10%`
5. **Blocks**: `%basketball_season1_blocks_1%` through `%basketball_season1_blocks_10%`
6. **Wins**: `%basketball_season1_wins_1%` through `%basketball_season1_wins_10%`
7. **Games**: `%basketball_season1_games_1%` through `%basketball_season1_games_10%`

### Career Leaderboards:
1. **Points**: `%basketball_career_points_1%` through `%basketball_career_points_10%`
2. **Assists**: `%basketball_career_assists_1%` through `%basketball_career_assists_10%`
3. **Rebounds**: `%basketball_career_rebounds_1%` through `%basketball_career_rebounds_10%`
4. **Steals**: `%basketball_career_steals_1%` through `%basketball_career_steals_10%`
5. **Blocks**: `%basketball_career_blocks_1%` through `%basketball_career_blocks_10%`
6. **Wins**: `%basketball_career_wins_1%` through `%basketball_career_wins_10%`
7. **Games**: `%basketball_career_games_1%` through `%basketball_career_games_10%`

---

## Quick Setup Commands

After creating the hologram files, use these commands in-game:

1. Reload DecentHolograms: `/dh reload`
2. Create hologram: `/dh create <name> <world> <x> <y> <z>`
3. List holograms: `/dh list`
4. Delete hologram: `/dh delete <name>`
5. Teleport to hologram: `/dh teleport <name>`

---

## Example: Creating All Holograms via Commands

```
/dh create season1_points world 0 100 0
/dh addline season1_points &6&lSeason 1 Points Leaderboard
/dh addline season1_points 
/dh addline season1_points %basketball_season1_points_1%
/dh addline season1_points %basketball_season1_points_2%
/dh addline season1_points %basketball_season1_points_3%
/dh addline season1_points %basketball_season1_points_4%
/dh addline season1_points %basketball_season1_points_5%
/dh addline season1_points %basketball_season1_points_6%
/dh addline season1_points %basketball_season1_points_7%
/dh addline season1_points %basketball_season1_points_8%
/dh addline season1_points %basketball_season1_points_9%
/dh addline season1_points %basketball_season1_points_10%
```

Repeat for each leaderboard type!

---

## Troubleshooting

1. **Placeholders showing as text**: Make sure PlaceholderAPI is installed and the LeaderboardPlaceholder is registered
2. **Leaderboards not updating**: They update every 30 seconds automatically
3. **Holograms not appearing**: Check the coordinates and world name in the hologram config
4. **Team tags showing [FA]**: Players need to be assigned to teams using your team system

---

## Customization

You can customize the appearance by editing:
- Colors in `SeasonStatLeaderboard.java` and `CareerStatLeaderboard.java`
- Team abbreviations in the `getTeamTag()` method
- Rank tags in the `getRankTag()` method
- Update frequency (currently 600 ticks = 30 seconds) in the BukkitRunnable

---

## Notes

- Leaderboards automatically update every 30 seconds
- The system handles offline players gracefully
- Empty positions show as "---"
- Top 3 positions have special colored formatting (Gold, White, Red)
- All data is pulled from your existing PlayerDb database
