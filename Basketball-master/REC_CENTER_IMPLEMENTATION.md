# REC CENTER IMPLEMENTATION SUMMARY

## Overview
A new "Rec Center" game mode has been successfully implemented for the Basketball plugin. This mode provides 3v3 quarter-based games (4 quarters, 4 minutes each) with increased reward payouts compared to standard park games.

## Key Features

### 1. Separate Rec Lobby
- **Location**: New dedicated lobby at coordinates (1000.5, -59.0, 1000.5)
- **Purpose**: Isolated area for Rec Center queue management
- **Access**: Via Server Selector GUI from Main Lobby

### 2. Queue System
- **Game Type**: 3v3 only
- **Queue Size**: 6 players total (3 vs 3)
- **Party Support**: Full party system integration
  - Party leaders can queue their entire party
  - Maximum 3 players per party (enforced)
  - Solo queueing also supported
- **Queue Display**: Real-time action bar showing queue status (X/6 players)

### 3. Game Format
- **Duration**: 4 quarters × 4 minutes = 16 minutes total game time
- **Win Condition**: Highest score after 4 quarters
- **Overtime**: Supported if tied after regulation
- **Courts**: 5 dedicated Rec courts
  - Court 1: (1100.5, -60, 1000.5)
  - Court 2: (1200.5, -60, 1000.5)
  - Court 3: (1300.5, -60, 1000.5)
  - Court 4: (1400.5, -60, 1000.5)
  - Court 5: (1500.5, -60, 1000.5)

### 4. Reward System
**Rec Games (BIGGER PAYOUTS):**
- Winners: 25 coins (vs 10 for regular ranked)
- Losers: 10 coins (vs 5 for regular ranked)
- 2.5x multiplier on standard rewards

**Standard Park Games:**
- Winners: 10 coins
- Losers: 5 coins

### 5. Lobby Items
Players in Rec Lobby receive:
- **Diamond**: Queue for Rec game
- **Barrier**: Return to Main Lobby

## Files Modified

### New Files Created:
1. **RecLobby.java** (`src/main/java/me/x_tias/partix/mini/lobby/RecLobby.java`)
   - Complete lobby implementation with queue management
   - Party support and validation
   - Game creation and court assignment
   - Return-to-lobby logic after games

### Modified Files:
1. **Hub.java** (`src/main/java/me/x_tias/partix/mini/factories/Hub.java`)
   - Added `public static RecLobby recLobby = new RecLobby();`

2. **MainLobby.java** (`src/main/java/me/x_tias/partix/mini/lobby/MainLobby.java`)
   - Added Rec Center button to Server Selector GUI (slot 12)
   - Button displays: "Rec Center" with Diamond icon
   - Lore shows: "4 Quarter Games (4 min each)", "Bigger rewards!", "3v3 matches only"

3. **BasketballGame.java** (`src/main/java/me/x_tias/partix/mini/basketball/BasketballGame.java`)
   - Added `public boolean isRecGame = false;` flag
   - Modified reward calculations to check `isRecGame` flag
   - Winners get 25 coins instead of 10 for Rec games
   - Losers get 10 coins instead of 5 for Rec games

4. **Game.java** (`src/main/java/me/x_tias/partix/server/specific/Game.java`)
   - Modified `kickAll()` method to check for Rec games
   - Rec games return players to Rec lobby instead of main hub
   - Regular games continue to return to main hub

## How to Use

### For Players:
1. Open Server Selector (Nether Star in main lobby)
2. Click the Diamond item labeled "Rec Center"
3. You'll be teleported to the Rec lobby
4. Click the Diamond to queue for a Rec game
5. Wait for 5 other players (queue shows X/6)
6. Game automatically starts when 6 players are ready
7. After game ends, you're returned to Rec lobby
8. Click Barrier to return to Main Lobby

### For Party Leaders:
1. Form a party (max 3 players for Rec)
2. Go to Rec Center
3. Queue your party (only leader can queue)
4. Entire party joins game together

## Important Configuration Notes

### Court Locations
**⚠️ IMPORTANT**: The coordinates in RecLobby.java are placeholders. You MUST adjust these to match your actual server world:

```java
// Current coordinates (CHANGE THESE):
this.recLobbySpawn = new Location(Bukkit.getWorlds().getFirst(), 1000.5, -59.0, 1000.5, 180.0f, 0.0f);

// And the 5 court locations:
this.recCourts.add(new Location(..., 1100.5, -60, 1000.5));
// etc...
```

Make sure to:
1. Build 5 Rec courts in your world
2. Update the coordinates in RecLobby.java constructor
3. Update the Rec lobby spawn location

### Game Settings
The Rec game settings are defined in RecLobby.java:
- 4 quarters (periods = 4)
- 4 minutes per quarter (WinType.TIME_5 with amount = 4)
- 3v3 games (playersPerTeam = 3)
- Ranked competition type (for stat tracking)

## Testing Checklist

- [ ] Build Rec lobby area at the specified coordinates
- [ ] Build 5 Rec courts at the specified locations
- [ ] Update coordinates in RecLobby.java
- [ ] Test Server Selector GUI button
- [ ] Test solo queue (need 6 players)
- [ ] Test party queue (2-3 player parties)
- [ ] Verify 4-quarter gameplay
- [ ] Check reward payouts (25 for winners, 10 for losers)
- [ ] Test return to Rec lobby after game
- [ ] Test return to Main lobby from Rec lobby
- [ ] Verify queue action bar displays correctly

## Future Enhancements (Optional)

1. **Rec Statistics**: Add separate Rec-specific stats tracking
2. **Rec Leaderboards**: Create Rec-only leaderboards
3. **Rec Ranks**: Special ranking system for Rec players
4. **Court Selection**: Allow players to choose which Rec court
5. **Tournament Mode**: Rec tournament support
6. **Custom Quarter Lengths**: GUI to adjust quarter duration

## Differences from Park Games

| Feature | Park Games | Rec Games |
|---------|-----------|-----------|
| Game Type | First to 21 | 4 quarters @ 4 min each |
| Team Size | 1v1, 2v2, 3v3, 4v4 | 3v3 only |
| Duration | ~5-10 minutes | ~16-20 minutes |
| Win Reward | 10 coins | 25 coins (2.5x) |
| Loss Reward | 5 coins | 10 coins (2x) |
| Queue System | Multiple modes | Single Rec queue |
| Post-Game | Return to main hub | Return to Rec lobby |

## Support

If you encounter issues:
1. Check server console for errors
2. Verify all coordinates are correct
3. Ensure courts are properly built
4. Test with `/tp` commands to verify locations
5. Check that RecLobby is properly registered in Hub.java
