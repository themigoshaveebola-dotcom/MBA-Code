# Location-Based Music System - Setup Guide

## Overview
This system plays background music based on player location with smart game state detection:
- **RecLobby**: Music plays continuously
- **Arenas**: Music plays only when NOT in regulation/overtime (plays during pregame, timeouts, halftime, postgame, etc.)

## Step 1: Add Music to Your Resource Pack

1. Place your music files in: `assets/minecraft/sounds/` in your resource pack
   - Example structure:
     ```
     assets/minecraft/sounds/
       ├── lobbymusic/
       │   ├── chill1.ogg
       │   ├── chill2.ogg
       │   └── vibe1.ogg
       └── arenamusic/
           ├── hype1.ogg
           ├── intense1.ogg
           └── action1.ogg
     ```

2. Update your `sounds.json` file:
   ```json
   {
     "lobbymusic.chill1": {
       "sounds": ["lobbymusic/chill1"]
     },
     "lobbymusic.chill2": {
       "sounds": ["lobbymusic/chill2"]
     },
     "lobbymusic.vibe1": {
       "sounds": ["lobbymusic/vibe1"]
     },
     "arenamusic.hype1": {
       "sounds": ["arenamusic/hype1"]
     },
     "arenamusic.intense1": {
       "sounds": ["arenamusic/intense1"]
     },
     "arenamusic.action1": {
       "sounds": ["arenamusic/action1"]
     }
   }
   ```

## Step 2: Register the LocationMusicManager

Add this to your `Partix.java` in the `listeners()` method:

```java
private void listeners() {
    // ... existing listeners ...
    this.getServer().getPluginManager().registerEvents(new LocationMusicManager(), this);
}
```

Also, add a field to store the manager instance at the top of the Partix class:

```java
@Getter
private LocationMusicManager locationMusicManager;
```

And initialize it in `onEnable()` before calling `listeners()`:

```java
public void onEnable() {
    // ... existing code ...
    this.locationMusicManager = new LocationMusicManager();
    // ... rest of code ...
}
```

Then update the listeners method to use the instance:

```java
private void listeners() {
    // ... existing listeners ...
    this.getServer().getPluginManager().registerEvents(this.locationMusicManager, this);
}
```

## Step 3: Start Music When Players Join RecLobby

In `RecLobby.java`, update the `onJoin` method:

```java
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
        
        // START MUSIC
        Partix.getInstance().getLocationMusicManager().startLocationMusic(player);
    }
}
```

## Step 4: Start Music When Players Join Arena Games

In `BasketballGame.java`, find the `onJoin` method and add music start:

```java
@Override
public void onJoin(Athlete... athletes) {
    // ... existing join code ...
    
    for (Athlete athlete : athletes) {
        Player player = athlete.getPlayer();
        // ... existing code ...
        
        // START MUSIC
        Partix.getInstance().getLocationMusicManager().startLocationMusic(player);
    }
}
```

## Step 5: Update Music When Game States Change

In `BasketballGame.java`, find the `setState` method and add music updates:

```java
public void setState(GoalGame.State state) {
    GoalGame.State oldState = this.getState();
    super.setState(state);
    
    // Update music when transitioning to/from regulation/overtime
    if (oldState != state) {
        // If transitioning TO regulation/overtime, pause music
        if (state == GoalGame.State.REGULATION || state == GoalGame.State.OVERTIME) {
            Partix.getInstance().getLocationMusicManager().updateGameMusic(this);
        }
        // If transitioning FROM regulation/overtime to anything else, resume music
        else if (oldState == GoalGame.State.REGULATION || oldState == GoalGame.State.OVERTIME) {
            Partix.getInstance().getLocationMusicManager().updateGameMusic(this);
        }
    }
}
```

## Step 6: Update Music During Timeouts

The system automatically handles timeouts! Since timeouts set the game state to `STOPPAGE`, music will automatically resume. When the timeout ends and the game returns to `REGULATION`, music will automatically stop.

This is already handled by the state change detection in Step 5.

## Step 7: Stop Music When Players Leave

In `BasketballGame.java`, update the `onQuit` method:

```java
@Override
public void onQuit(Athlete... athletes) {
    for (Athlete athlete : athletes) {
        Player player = athlete.getPlayer();
        // ... existing quit code ...
        
        // STOP MUSIC
        Partix.getInstance().getLocationMusicManager().stopLocationMusic(player);
    }
}
```

Same for `RecLobby.java`:

```java
@Override
public void onQuit(Athlete... athletes) {
    for (Athlete athlete : athletes) {
        // ... existing code ...
        Player player = athlete.getPlayer();
        
        // STOP MUSIC
        Partix.getInstance().getLocationMusicManager().stopLocationMusic(player);
    }
}
```

## Step 8: Customize Playlists

Edit the playlists in `LocationMusicManager.java`:

```java
// Rec Lobby playlist
private final List<String> recLobbyPlaylist = Arrays.asList(
    "lobbymusic.chill1",    // ← Replace with your sound identifiers
    "lobbymusic.chill2",
    "lobbymusic.vibe1"
);

// Arena playlist (plays during non-game states)
private final List<String> arenaPlaylist = Arrays.asList(
    "arenamusic.hype1",     // ← Replace with your sound identifiers
    "arenamusic.intense1",
    "arenamusic.action1"
);
```

## How It Works

### Game State Detection
- **REGULATION/OVERTIME**: Music paused (game is actively being played)
- **STOPPAGE**: Music plays (includes timeouts, fouls, out of bounds)
- **PREGAME**: Music plays (warmup period)
- **HALFTIME**: Music plays (between quarters/halves)
- **FINAL**: Music plays (after game ends)

### Music Behavior
- Checks player location every 2 seconds
- Automatically switches playlists when moving between areas
- Prevents the same track from playing twice in a row
- Tracks play for ~60 seconds before switching (adjust in code if needed)
- Uses `SoundCategory.RECORDS` so it doesn't interfere with game sounds
- Volume set to 50% (adjust in code: `0.5f` parameter)

## Troubleshooting

1. **Music not playing?**
   - Check that your sound identifiers match your sounds.json
   - Verify players have the resource pack loaded
   - Check console for errors

2. **Music too loud/quiet?**
   - Adjust volume in `playRandomTrack()` method: `player.playSound(..., 0.5f, ...)`
   - 1.0f = 100%, 0.5f = 50%, 0.3f = 30%, etc.

3. **Music not stopping during regulation?**
   - Verify game state is being set correctly
   - Check that `setState()` method was modified in Step 5

4. **Tracks too short/long?**
   - Adjust the delay in `playRandomTrack()`: `1200L` = 60 seconds
   - Calculate: seconds × 20 = ticks (e.g., 90 seconds = 1800L)

## Testing Commands

You can test the system with these debug commands (optional):
- Check player's current place: `/debug place <player>`
- Check game state: `/debug gamestate <player>`
- Force music update: Call `locationMusicManager.updateGameMusic(game)` manually
