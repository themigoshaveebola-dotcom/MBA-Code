# Rec Center Quick Setup Guide

## Step 1: Build the Locations

You need to build the following locations in your Minecraft world:

### Rec Lobby
- **Coordinates**: X: 1000.5, Y: -59.0, Z: 1000.5
- **Purpose**: Where players queue for Rec games
- **Features**: 
  - Spawn platform
  - Signs/holograms explaining Rec Center
  - Decorations showing "4 Quarters, Bigger Rewards"

### 5 Rec Courts
Build 5 basketball courts (same size as your ranked courts: 26 blocks long) at:
1. X: 1100.5, Y: -60, Z: 1000.5
2. X: 1200.5, Y: -60, Z: 1000.5
3. X: 1300.5, Y: -60, Z: 1000.5
4. X: 1400.5, Y: -60, Z: 1000.5
5. X: 1500.5, Y: -60, Z: 1000.5

## Step 2: Update Coordinates (If Needed)

If you want to use different coordinates, edit `RecLobby.java`:

```java
// Line ~57: Change lobby spawn
this.recLobbySpawn = new Location(
    Bukkit.getWorlds().getFirst(), 
    YOUR_X, YOUR_Y, YOUR_Z,  // <-- Change these
    180.0f, 0.0f
);

// Lines ~60-64: Change court locations
this.recCourts.add(new Location(Bukkit.getWorlds().getFirst(), YOUR_X, YOUR_Y, YOUR_Z));
// ... repeat for all 5 courts
```

## Step 3: Compile and Deploy

```bash
cd Basketball-master
./gradlew.bat build
```

Copy the generated JAR from `build/libs/` to your server's plugins folder.

## Step 4: Test

1. Start your server
2. Join the server
3. Open Server Selector (Nether Star)
4. Click the Diamond labeled "Rec Center"
5. You should teleport to the Rec lobby
6. Click Diamond to queue (need 6 total players to start)

## Quick Commands for Testing

Use these commands to teleport and verify locations:

```
/tp @p 1000.5 -59.0 1000.5     # Rec lobby
/tp @p 1100.5 -60 1000.5       # Court 1
/tp @p 1200.5 -60 1000.5       # Court 2
/tp @p 1300.5 -60 1000.5       # Court 3
/tp @p 1400.5 -60 1000.5       # Court 4
/tp @p 1500.5 -60 1000.5       # Court 5
```

## Troubleshooting

**Problem**: Can't see Rec button in Server Selector
- **Solution**: Make sure you rebuilt the plugin after changes

**Problem**: Queue doesn't start game
- **Solution**: Need exactly 6 players in queue (3v3)

**Problem**: Wrong teleport location
- **Solution**: Update coordinates in RecLobby.java constructor

**Problem**: Players stuck in Rec lobby
- **Solution**: Click Barrier item to return to main lobby

## Configuration Options

You can adjust these settings in `RecLobby.java`:

```java
// Change quarter length (currently 4 minutes)
this.recSettings.winType.amount = 4;  // Change to 3, 5, etc.

// Change team size (currently 3v3)
playersPerTeam = 3,  // Change to 2 for 2v2, 4 for 4v4, etc.

// Change number of quarters
periods = 4,  // Change to 2 for halves, 3 for periods, etc.
```

**Note**: If you change team size from 3, also update the queue size check in `startRecGame()`:
```java
if (this.recQueue.size() >= 6) {  // Change 6 to match (playersPerTeam * 2)
```

## Reward Adjustments

To change reward amounts, edit `BasketballGame.java` around lines 1677 and 1686:

```java
// Winners
int winReward = this.isRecGame ? 25 : 10;  // Change 25 to your desired amount

// Losers
int loseReward = this.isRecGame ? 10 : 5;  // Change 10 to your desired amount
```

## Next Steps

After basic setup works:
- [ ] Add decorative elements to Rec lobby
- [ ] Consider adding holograms for queue status
- [ ] Add leaderboards for Rec stats
- [ ] Create special Rec-only cosmetics/rewards
- [ ] Build spectator areas around Rec courts
