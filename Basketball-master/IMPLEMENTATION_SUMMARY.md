# 🏀 Basketball Leaderboard System - Implementation Summary

## ✅ What Was Added

### New Java Classes Created:

1. **SeasonStatLeaderboard.java** - Handles Season 1 stat leaderboards
   - Location: `src/main/java/me/x_tias/partix/season/SeasonStatLeaderboard.java`
   - Tracks: Points, Assists, Rebounds, Steals, Blocks, Wins, Games Played

2. **CareerStatLeaderboard.java** - Handles career/all-time stat leaderboards
   - Location: `src/main/java/me/x_tias/partix/season/CareerStatLeaderboard.java`
   - Tracks: Points, Assists, Rebounds, Steals, Blocks, Wins, Games Played

3. **LeaderboardManager.java** - Manages all leaderboard systems
   - Location: `src/main/java/me/x_tias/partix/season/LeaderboardManager.java`
   - Initializes and coordinates all leaderboards

4. **LeaderboardPlaceholder.java** - PlaceholderAPI expansion for holograms
   - Location: `src/main/java/me/x_tias/partix/season/LeaderboardPlaceholder.java`
   - Provides placeholders like `%basketball_season1_points_1%`

### Modified Files:

- **Partix.java** - Added leaderboard initialization in the `placeholders()` method

### Documentation Files Created:

1. **LEADERBOARD_SETUP.md** - Complete setup guide with detailed instructions
2. **HOLOGRAM_COMMANDS.md** - Ready-to-use commands for all 14 leaderboards

---

## 🎯 Features

### Display Format:
```
1. [RANK] Username - Value Stat
```

**Example:**
```
1. [ADMIN] posterizing - 1000 Points
2. [VIP] player123 - 950 Points
3. noobmaster69 - 500 Points
```

### Color Coding:
- **1st Place**: Gold (§6§l)
- **2nd Place**: White (§f§l)
- **3rd Place**: Red (§c§l)
- **4th-10th**: Yellow (§e)

### Team Tags Supported:
- [WAS] - Washington Withers
- [PHI] - Philadelphia 64s
- [CHI] - Chicago Bows
- [BKN] - Brooklyn Buckets
- [MIA] - Miami Magma Cubes
- [ATL] - Atlanta Allays
- [LAC] - LA Creepers
- [BOS] - Boston Breeze
- [FA] - Free Agent (default)

### Rank Tags Supported:
- [ADMIN], [MOD], [MEDIA]
- [COACH], [REF]
- [PRO], [VIP]

---

## 🔄 Auto-Update System

- Leaderboards automatically refresh **every 5 minutes** (6000 ticks)
- Updates happen asynchronously (no server lag)
- Pulls data directly from your existing PlayerDb database
- **Built-in countdown timer** showing time until next update
- No manual intervention needed

### Countdown Placeholders:
- `%basketball_update_countdown%` - Shows countdown as MM:SS (e.g., "4:32")
- `%basketball_update_seconds%` - Shows remaining seconds as number

---

## 📋 Available Leaderboards (14 Total)

### Season 1 Leaderboards (7):
1. Season 1 Points - `%basketball_season1_points_1%` through `_10%`
2. Season 1 Assists - `%basketball_season1_assists_1%` through `_10%`
3. Season 1 Rebounds - `%basketball_season1_rebounds_1%` through `_10%`
4. Season 1 Steals - `%basketball_season1_steals_1%` through `_10%`
5. Season 1 Blocks - `%basketball_season1_blocks_1%` through `_10%`
6. Season 1 Wins - `%basketball_season1_wins_1%` through `_10%`
7. Season 1 Games Played - `%basketball_season1_games_1%` through `_10%`

### Career Leaderboards (7):
1. Career Points - `%basketball_career_points_1%` through `_10%`
2. Career Assists - `%basketball_career_assists_1%` through `_10%`
3. Career Rebounds - `%basketball_career_rebounds_1%` through `_10%`
4. Career Steals - `%basketball_career_steals_1%` through `_10%`
5. Career Blocks - `%basketball_career_blocks_1%` through `_10%`
6. Career Wins - `%basketball_career_wins_1%` through `_10%`
7. Career Games Played - `%basketball_career_games_1%` through `_10%`

---

## 🚀 How to Use

### Step 1: Build & Install Your Plugin
```bash
./gradlew.bat build
```
Copy the JAR from `build/libs/` to your server's `plugins/` folder.

### Step 2: Install Required Plugins
1. **PlaceholderAPI** - https://www.spigotmc.org/resources/placeholderapi.6245/
2. **DecentHolograms** - https://www.spigotmc.org/resources/decentholograms.96927/

### Step 3: Restart Server
The leaderboards will automatically initialize on startup.

### Step 4: Create Holograms
Use the commands in `HOLOGRAM_COMMANDS.md` to create your holograms.

**Quick example:**
```
/dh create season1_points world 0 100 0
/dh addline season1_points &6&lSeason 1 Points Leaderboard
/dh addline season1_points &eNext update: %basketball_update_countdown%
/dh addline season1_points  
/dh addline season1_points %basketball_season1_points_1%
/dh addline season1_points %basketball_season1_points_2%
/dh addline season1_points %basketball_season1_points_3%
(continue for positions 4-10)
```

---

## 🎨 Customization Options

### Change Update Frequency:
In both `SeasonStatLeaderboard.java` and `CareerStatLeaderboard.java`:
```java0L);
                                                    ^      ^
                                            Initial delay  Update interval (ticks)
```
- 6000 ticks = 5 minutes (default)
- 3000 ticks = 2.5 minutes
- 12000 ticks = 10 minutes

Don't forget to also update the countdown initial value:
```java
private static int secondsUntilUpdate = 300; // Change to match interval
```s
- 6000 ticks = 5 minutes

### Change Colors:
Modify the `formatEntry()` method in either leaderboard class:
```java
String posColor = switch (position) {
    case 1 -> "§6§l"; // Change these color codes
    case 2 -> "§f§l";
    case 3 -> "§c§l";
    default -> "§e";
};
```

### Add More Team Abbreviations:
In the `getTeamTag()` method:
```java
if (teamName.contains("yourteam")) return "§c[TAG]";
```

---

## ✅ Compatibility

- ✅ Works with DecentHolograms
- ✅ Uses PlaceholderAPI for hologram integration
- ✅ Compatible with your existing PlayerDb system
- ✅ Handles offline players gracefully
- ✅ Async updates (no lag)
- ✅ Auto-refreshing (no manual updates needed)

---

## 🐛 Troubleshooting
5 minutes.

**Problem**: Team tags showing [FA] for everyone
- **Solution**: Team tags are disabled by default. Only rank and username are displayed.
**Problem**: Leaderboards show "---" for all positions
- **Solution**: Make sure players have stats in your database. Leaderboards update every 30 seconds.

**Problem**: Team tags showing [FA] for everyone
- **Solution**: Players need to be assigned to teams using your team system/commands

**Problem**: Holograms not appearing
- **Solution**: Check coordinates and world name. Use `/dh list` to see all holograms

**Problem**: Rank tags not showing
- **Solution**: Players need to have ranks assigned via permissions/scoreboard teams

---

## 📝 Notes

- All leaderboards use your existing database tables (`players` table)
- No additional database setup required
- Leaderboards work even if DecentHolograms is not installed (useful for future features)
- System is modular - you can disable specific leaderboards if needed
- Empty positions gracefully show as "---"

---

## 🎉 You're Done!

Your Basketball plugin now has 14 fully functional, auto-updating leaderboards ready for display!

Check `HOLOGRAM_COMMANDS.md` for ready-to-paste commands to create all holograms quickly.
