# Physical Queue Spot System Documentation

## Overview
This is an NBA 2K Park-style physical queue system where players physically walk onto colored concrete blocks to queue for basketball games. The system supports 2v2 and 3v3 courts with winner-stays mechanics.

## Architecture

### File Structure
```
src/main/java/me/x_tias/partix/mini/basketball/
├── CourtSpot.java          - Individual queue spot
├── CourtQueue.java         - Manages all spots for one court
└── BasketballLobby.java    - Main orchestrator
```

---

## 1. CourtSpot.java
**Purpose**: Represents a single physical spot where a player can stand to queue.

### Class Structure
```java
package me.x_tias.partix.mini.basketball;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import java.util.UUID;

@Getter
public class CourtSpot {
    private final Location location;
    private final SpotType spotType;
    private final CourtQueue court;
    private final int spotIndex; // 0, 1, 2 for team spots
    
    @Setter
    private UUID occupiedBy; // null if empty
    
    public enum SpotType {
        TEAM1,    // Left side spots (Red concrete)
        PARTY,    // Middle/waiting spots (Yellow concrete)
        TEAM2     // Right side spots (Blue concrete)
    }
}
```

### Key Methods

**Constructor**
```java
public CourtSpot(Location location, SpotType spotType, CourtQueue court, int spotIndex) {
    this.location = location;
    this.spotType = spotType;
    this.court = court;
    this.spotIndex = spotIndex;
    this.occupiedBy = null;
    updateBlock(); // Place initial concrete block
}
```

**Range Detection**
```java
public boolean isPlayerInRange(Player player) {
    if (player.getWorld() != location.getWorld()) {
        return false;
    }
    return player.getLocation().distance(location) <= 1.5;
}
```

**Occupation Management**
```java
public boolean isOccupied() {
    return occupiedBy != null;
}

public boolean tryOccupy(UUID playerId) {
    if (isOccupied()) {
        return false;
    }
    occupiedBy = playerId;
    updateBlock();
    return true;
}

public void vacate() {
    occupiedBy = null;
    updateBlock();
}
```

**Visual Block Updates**
```java
public void updateBlock() {
    if (location.getWorld() == null) return;
    
    Location blockLoc = location.getBlock().getLocation();
    Material blockType = switch (spotType) {
        case TEAM1 -> Material.RED_CONCRETE;
        case PARTY -> Material.YELLOW_CONCRETE;
        case TEAM2 -> Material.BLUE_CONCRETE;
    };
    blockLoc.getBlock().setType(blockType);
}
```

**Particle Effects**
```java
public void spawnParticles() {
    if (location.getWorld() == null) return;
    
    double centerX = location.getBlockX() + 0.5;
    double centerZ = location.getBlockZ() + 0.5;
    double particleY = location.getY() + 1.0;
    double radius = 0.5;
    int particleCount = 8;
    double angleStep = (2 * Math.PI) / particleCount;
    
    if (isOccupied()) {
        // Green particles when occupied - circular pattern
        for (int i = 0; i < particleCount; i++) {
            double angle = i * angleStep;
            double x = centerX + radius * Math.cos(angle);
            double z = centerZ + radius * Math.sin(angle);
            location.getWorld().spawnParticle(
                Particle.HAPPY_VILLAGER,
                x, particleY, z,
                1, 0, 0, 0, 0
            );
        }
    } else {
        // Red particles when empty - circular pattern
        for (int i = 0; i < particleCount; i++) {
            double angle = i * angleStep;
            double x = centerX + radius * Math.cos(angle);
            double z = centerZ + radius * Math.sin(angle);
            location.getWorld().spawnParticle(
                Particle.DUST,
                x, particleY, z,
                1, 0, 0, 0, 0,
                new Particle.DustOptions(org.bukkit.Color.RED, 1.5f)
            );
        }
    }
}
```

**Cleanup**
```java
public void removeBlock() {
    if (location.getWorld() != null) {
        location.getBlock().setType(Material.AIR);
    }
}
```

---

## 2. CourtQueue.java
**Purpose**: Manages all queue spots for a single basketball court.

### Class Structure
```java
package me.x_tias.partix.mini.basketball;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
public class CourtQueue {
    private final Location courtLocation;
    private final CourtType courtType;
    private final List<CourtSpot> spots;
    private final int playersPerTeam;
    
    public enum CourtType {
        TWOS,   // 2v2 court
        THREES  // 3v3 court
    }
}
```

### Spot Layout
```
For 3v3: [T1][T1][T1] [P][P][P] [T2][T2][T2]
For 2v2: [T1][T1] [P][P] [T2][T2]

T1 = Team 1 spots (Red)
P  = Party/Waiting spots (Yellow)
T2 = Team 2 spots (Blue)
```

### Key Methods

**Constructor & Initialization**
```java
public CourtQueue(Location courtLocation, CourtType courtType) {
    this.courtLocation = courtLocation;
    this.courtType = courtType;
    this.playersPerTeam = courtType == CourtType.TWOS ? 2 : 3;
    this.spots = new ArrayList<>();
    initializeSpots();
}

private void initializeSpots() {
    // Position spots 18 blocks away from the sideline
    double spotSpacing = 2.0; // 2 blocks between each spot
    double sidelineOffset = 7.0;
    double baseX = Math.floor(courtLocation.getX() - sidelineOffset - 18.0) + 0.5;
    double baseZ = Math.floor(courtLocation.getZ()) + 0.5;
    double baseY = Math.floor(courtLocation.getY() - 1.0);
    
    int totalSpots = playersPerTeam * 3; // team1 + party + team2
    double startZ = baseZ - (spotSpacing * (totalSpots - 1) / 2.0);
    
    // Create Team 1 spots
    for (int i = 0; i < playersPerTeam; i++) {
        Location spotLoc = new Location(
            courtLocation.getWorld(),
            baseX,
            baseY,
            startZ + (i * spotSpacing)
        );
        spots.add(new CourtSpot(spotLoc, CourtSpot.SpotType.TEAM1, this, i));
    }
    
    // Create Party/Waiting spots
    for (int i = 0; i < playersPerTeam; i++) {
        Location spotLoc = new Location(
            courtLocation.getWorld(),
            baseX,
            baseY,
            startZ + ((playersPerTeam + i) * spotSpacing)
        );
        spots.add(new CourtSpot(spotLoc, CourtSpot.SpotType.PARTY, this, i));
    }
    
    // Create Team 2 spots
    for (int i = 0; i < playersPerTeam; i++) {
        Location spotLoc = new Location(
            courtLocation.getWorld(),
            baseX,
            baseY,
            startZ + ((playersPerTeam * 2 + i) * spotSpacing)
        );
        spots.add(new CourtSpot(spotLoc, CourtSpot.SpotType.TEAM2, this, i));
    }
}
```

**Spot Queries**
```java
public List<CourtSpot> getSpotsByType(CourtSpot.SpotType type) {
    return spots.stream()
        .filter(spot -> spot.getSpotType() == type)
        .collect(Collectors.toList());
}

public CourtSpot getSpotForPlayer(Player player) {
    for (CourtSpot spot : spots) {
        if (spot.isPlayerInRange(player)) {
            return spot;
        }
    }
    return null;
}

public CourtSpot getSpotByOccupant(UUID playerId) {
    for (CourtSpot spot : spots) {
        if (spot.isOccupied() && spot.getOccupiedBy().equals(playerId)) {
            return spot;
        }
    }
    return null;
}
```

**Player Management**
```java
public void removePlayer(UUID playerId) {
    CourtSpot spot = getSpotByOccupant(playerId);
    if (spot != null) {
        spot.vacate();
    }
}

public List<Player> getAllPlayersOnSpots() {
    return spots.stream()
        .filter(CourtSpot::isOccupied)
        .map(spot -> Bukkit.getPlayer(spot.getOccupiedBy()))
        .filter(p -> p != null && p.isOnline())
        .collect(Collectors.toList());
}

public List<Player> getTeam1Players() {
    return getSpotsByType(CourtSpot.SpotType.TEAM1).stream()
        .filter(CourtSpot::isOccupied)
        .map(spot -> Bukkit.getPlayer(spot.getOccupiedBy()))
        .filter(p -> p != null && p.isOnline())
        .collect(Collectors.toList());
}

public List<Player> getTeam2Players() {
    return getSpotsByType(CourtSpot.SpotType.TEAM2).stream()
        .filter(CourtSpot::isOccupied)
        .map(spot -> Bukkit.getPlayer(spot.getOccupiedBy()))
        .filter(p -> p != null && p.isOnline())
        .collect(Collectors.toList());
}
```

**Game Ready Check**
```java
public boolean isReadyToStart() {
    long filledTeam1 = getSpotsByType(CourtSpot.SpotType.TEAM1).stream()
        .filter(CourtSpot::isOccupied)
        .count();
    
    long filledTeam2 = getSpotsByType(CourtSpot.SpotType.TEAM2).stream()
        .filter(CourtSpot::isOccupied)
        .count();
    
    return filledTeam1 == playersPerTeam && filledTeam2 == playersPerTeam;
}
```

**Utility Methods**
```java
public void clearAllSpots() {
    spots.forEach(CourtSpot::vacate);
}

public void spawnAllParticles() {
    spots.forEach(CourtSpot::spawnParticles);
}

public void removeAllBlocks() {
    spots.forEach(CourtSpot::removeBlock);
}
```

---

## 3. BasketballLobby.java Integration
**Purpose**: Main class that orchestrates the entire system with event handling and game management.

### Data Members to Add
```java
// In BasketballLobby class
private final List<CourtQueue> twosCourts = new ArrayList<>();
private final List<CourtQueue> threesCourts = new ArrayList<>();
private BukkitTask particleTask;
private final HashMap<UUID, CourtSpot> playerSpots = new HashMap<>();
private final HashMap<Location, CourtQueue> gameCourtMapping = new HashMap<>();
```

### Implementation in Constructor
```java
public BasketballLobby() {
    // ... existing initialization ...
    
    // Initialize 2K Park-style queue spots
    initializePhysicalQueueSpots();
    
    // Start particle spawning task
    startParticleTask();
    
    // Register this as an event listener for PlayerMoveEvent and PlayerToggleSneakEvent
    Bukkit.getPluginManager().registerEvents(this, Partix.getInstance());
    Bukkit.getLogger().info("[Park Queue] Registered BasketballLobby as event listener");
}
```

### Initialization Method
```java
private void initializePhysicalQueueSpots() {
    // Allocate 2 courts for 2v2 (first 2 ranked courts)
    if (rankedCourts.size() >= 4) {
        twosCourts.add(new CourtQueue(rankedCourts.get(0), CourtQueue.CourtType.TWOS));
        twosCourts.add(new CourtQueue(rankedCourts.get(1), CourtQueue.CourtType.TWOS));
        
        // Allocate 2 courts for 3v3 (last 2 ranked courts)
        threesCourts.add(new CourtQueue(rankedCourts.get(2), CourtQueue.CourtType.THREES));
        threesCourts.add(new CourtQueue(rankedCourts.get(3), CourtQueue.CourtType.THREES));
        
        Bukkit.getLogger().info("[Park Queue] Initialized 2 courts for 2v2 and 2 courts for 3v3 with physical spots");
    } else {
        Bukkit.getLogger().warning("[Park Queue] Not enough ranked courts to initialize physical spots!");
    }
}
```

### Particle Task
```java
private void startParticleTask() {
    particleTask = Bukkit.getScheduler().runTaskTimer(Partix.getInstance(), () -> {
        // Spawn particles for all 2v2 court spots
        for (CourtQueue court : twosCourts) {
            court.spawnAllParticles();
        }
        
        // Spawn particles for all 3v3 court spots
        for (CourtQueue court : threesCourts) {
            court.spawnAllParticles();
        }
    }, 0L, 2L); // Run every 2 ticks (0.1 seconds)
}
```

### Event Handler: Player Movement
```java
@EventHandler
public void onPlayerMove(PlayerMoveEvent event) {
    Player player = event.getPlayer();
    UUID playerId = player.getUniqueId();
    
    // Check if player moved to a new block (optimization)
    if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
        event.getFrom().getBlockY() == event.getTo().getBlockY() &&
        event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
        return;
    }
    
    // Check if player is currently on a spot
    CourtSpot currentSpot = playerSpots.get(playerId);
    
    if (currentSpot != null) {
        // Player is on a spot - freeze them if they try to move too far
        if (!currentSpot.isPlayerInRange(player)) {
            // Teleport player back to spot center (1 block up)
            Location spotCenter = currentSpot.getLocation().clone();
            spotCenter.setY(spotCenter.getY() + 1.0);
            spotCenter.setYaw(player.getLocation().getYaw());
            spotCenter.setPitch(player.getLocation().getPitch());
            event.setTo(spotCenter);
            
            if (Math.random() < 0.1) { // Only send message 10% of the time to avoid spam
                player.sendMessage("§e§l» §eYou're frozen on the spot! §7Shift to leave.");
            }
        }
        return; // Don't check for new spots if already on one
    }
    
    // Player is not on a spot - check if they entered one
    CourtSpot nearbySpot = findNearbyAvailableSpot(player);
    if (nearbySpot != null && !nearbySpot.isOccupied()) {
        // Prevent players already in a game from stepping on spots
        Athlete athlete = AthleteManager.get(playerId);
        if (athlete != null && athlete.getPlace() instanceof BasketballGame) {
            BasketballGame currentGame = (BasketballGame) athlete.getPlace();
            if (currentGame.getState() != GoalGame.State.FINAL) {
                return;
            }
        }
        
        // Check if player is already in another queue
        if (this.isPlayerQueued(athlete)) {
            player.sendMessage("§c§l» §cYou're already in another queue! Leave it first.");
            return;
        }
        
        // Check if it's a party spot - enforce party requirements
        if (nearbySpot.getSpotType() == CourtSpot.SpotType.PARTY) {
            if (!canOccupyPartySpot(player, nearbySpot.getCourt())) {
                return;
            }
        }
        
        if (nearbySpot.tryOccupy(playerId)) {
            playerSpots.put(playerId, nearbySpot);
            player.sendMessage("§a§l» §aYou're now on a queue spot! " + getSpotMessage(nearbySpot));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.MASTER, 1.0f, 1.5f);
            
            // If party leader joins a party spot, automatically add party members
            handlePartyLeaderJoin(player, nearbySpot);
            
            // Check if ready to start game
            checkAndStartGame(nearbySpot.getCourt());
        }
    }
}
```

### Event Handler: Sneaking to Leave
```java
@EventHandler
public void onPlayerSneak(org.bukkit.event.player.PlayerToggleSneakEvent event) {
    if (!event.isSneaking()) {
        return; // Only handle when starting to sneak
    }
    
    Player player = event.getPlayer();
    UUID playerId = player.getUniqueId();
    
    CourtSpot currentSpot = playerSpots.get(playerId);
    if (currentSpot != null) {
        // Player is on a spot and wants to leave
        currentSpot.vacate();
        playerSpots.remove(playerId);
        
        // Teleport player 2 blocks west (negative X direction)
        Location exitLocation = currentSpot.getLocation().clone();
        exitLocation.setX(exitLocation.getX() - 2.0);
        exitLocation.setY(exitLocation.getY() + 1.0);
        player.teleport(exitLocation);
        
        player.sendMessage("§c§l» §cYou left the queue spot!");
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.MASTER, 1.0f, 0.5f);
    }
}
```

### Event Handler: Block Break Protection
```java
@EventHandler
public void onBlockBreak(org.bukkit.event.block.BlockBreakEvent event) {
    Player player = event.getPlayer();
    Location blockLoc = event.getBlock().getLocation();
    
    // Allow creative mode players to break blocks
    if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) {
        // Check if this is a queue spot block and mark it for re-placement
        for (CourtQueue court : twosCourts) {
            for (CourtSpot spot : court.getSpots()) {
                if (isSameBlock(spot.getLocation(), blockLoc)) {
                    Bukkit.getScheduler().runTaskLater(Partix.getInstance(), () -> {
                        spot.updateBlock();
                    }, 1L);
                    return;
                }
            }
        }
        for (CourtQueue court : threesCourts) {
            for (CourtSpot spot : court.getSpots()) {
                if (isSameBlock(spot.getLocation(), blockLoc)) {
                    Bukkit.getScheduler().runTaskLater(Partix.getInstance(), () -> {
                        spot.updateBlock();
                    }, 1L);
                    return;
                }
            }
        }
        return;
    }
    
    // Check if block is a queue spot block and prevent breaking
    for (CourtQueue court : twosCourts) {
        for (CourtSpot spot : court.getSpots()) {
            if (isSameBlock(spot.getLocation(), blockLoc)) {
                event.setCancelled(true);
                player.sendMessage("§c§l» §cYou can't break queue spot blocks!");
                return;
            }
        }
    }
    for (CourtQueue court : threesCourts) {
        for (CourtSpot spot : court.getSpots()) {
            if (isSameBlock(spot.getLocation(), blockLoc)) {
                event.setCancelled(true);
                player.sendMessage("§c§l» §cYou can't break queue spot blocks!");
                return;
            }
        }
    }
}

private boolean isSameBlock(Location loc1, Location loc2) {
    return loc1.getWorld() == loc2.getWorld() &&
           loc1.getBlockX() == loc2.getBlockX() &&
           loc1.getBlockY() == loc2.getBlockY() &&
           loc1.getBlockZ() == loc2.getBlockZ();
}
```

### Party Spot Validation
```java
private boolean canOccupyPartySpot(Player player, CourtQueue court) {
    Athlete athlete = AthleteManager.get(player.getUniqueId());
    if (athlete == null) {
        player.sendMessage("§c§l» §cParty spots require a party!");
        return false;
    }
    
    Party party = PartyFactory.get(athlete.getParty());
    if (party == null) {
        player.sendMessage("§c§l» §cParty spots require a party!");
        return false;
    }
    
    // Must be party leader
    if (!party.leader.equals(player.getUniqueId())) {
        player.sendMessage("§c§l» §cOnly the party leader can queue the party!");
        return false;
    }
    
    // Check party size matches court type
    int partySize = party.toList().size();
    int requiredSize = court.getPlayersPerTeam();
    
    if (partySize != requiredSize) {
        player.sendMessage("§c§l» §cYou need exactly " + requiredSize + " players in your party for this court!");
        return false;
    }
    
    return true;
}
```

### Party Leader Auto-Placement
```java
private void handlePartyLeaderJoin(Player player, CourtSpot leaderSpot) {
    if (leaderSpot.getSpotType() != CourtSpot.SpotType.PARTY) {
        return; // Only handle party spots
    }
    
    Athlete athlete = AthleteManager.get(player.getUniqueId());
    if (athlete == null) return;
    
    Party party = PartyFactory.get(athlete.getParty());
    if (party == null || !party.leader.equals(player.getUniqueId())) {
        return; // Not a party leader
    }
    
    List<Athlete> partyMembers = party.toList();
    if (partyMembers.size() <= 1) {
        return; // Solo or just the leader
    }
    
    CourtQueue court = leaderSpot.getCourt();
    List<CourtSpot> partySpots = court.getSpotsByType(CourtSpot.SpotType.PARTY);
    
    // Try to place party members on adjacent party spots
    int placedMembers = 0;
    for (Athlete member : partyMembers) {
        if (member.equals(athlete)) {
            continue; // Skip the leader (already placed)
        }
        
        Player memberPlayer = member.getPlayer();
        if (memberPlayer == null || !memberPlayer.isOnline()) {
            continue;
        }
        
        // Find an empty party spot
        for (CourtSpot spot : partySpots) {
            if (!spot.isOccupied()) {
                // Teleport party member to the spot (1 block up)
                Location spotLoc = spot.getLocation().clone();
                spotLoc.setY(spotLoc.getY() + 1.0);
                memberPlayer.teleport(spotLoc);
                
                spot.tryOccupy(memberPlayer.getUniqueId());
                playerSpots.put(memberPlayer.getUniqueId(), spot);
                memberPlayer.sendMessage("§a§l» §aYour party leader queued you! " + getSpotMessage(spot));
                memberPlayer.playSound(memberPlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.MASTER, 1.0f, 1.5f);
                placedMembers++;
                break;
            }
        }
    }
    
    if (placedMembers > 0) {
        player.sendMessage("§a§l» §aYour party members have been placed on spots!");
    }
}
```

### Helper Methods
```java
private CourtSpot findNearbyAvailableSpot(Player player) {
    // Check 2v2 courts
    for (CourtQueue court : twosCourts) {
        CourtSpot spot = court.getSpotForPlayer(player);
        if (spot != null && !spot.isOccupied()) {
            return spot;
        }
    }
    
    // Check 3v3 courts
    for (CourtQueue court : threesCourts) {
        CourtSpot spot = court.getSpotForPlayer(player);
        if (spot != null && !spot.isOccupied()) {
            return spot;
        }
    }
    
    return null;
}

private String getSpotMessage(CourtSpot spot) {
    String courtType = spot.getCourt().getPlayersPerTeam() == 2 ? "2v2" : "3v3";
    String spotType = switch (spot.getSpotType()) {
        case TEAM1 -> "§eTeam 1";
        case PARTY -> "§dWaiting/Party";
        case TEAM2 -> "§bTeam 2";
    };
    return "§7(" + courtType + " - " + spotType + "§7)";
}
```

### Game Starting Logic
```java
private void checkAndStartGame(CourtQueue court) {
    if (!court.isReadyToStart()) {
        return;
    }
    
    List<Player> team1Players = court.getTeam1Players();
    List<Player> team2Players = court.getTeam2Players();
    
    // Validate all players are still online and available
    if (team1Players.size() != court.getPlayersPerTeam() || 
        team2Players.size() != court.getPlayersPerTeam()) {
        return;
    }
    
    // Convert to Athletes
    List<Athlete> team1Athletes = team1Players.stream()
        .map(p -> AthleteManager.get(p.getUniqueId()))
        .filter(a -> a != null)
        .collect(java.util.stream.Collectors.toList());
        
    List<Athlete> team2Athletes = team2Players.stream()
        .map(p -> AthleteManager.get(p.getUniqueId()))
        .filter(a -> a != null)
        .collect(java.util.stream.Collectors.toList());
    
    if (team1Athletes.size() != court.getPlayersPerTeam() || 
        team2Athletes.size() != court.getPlayersPerTeam()) {
        return;
    }
    
    // Create the game
    startPhysicalQueueGame(court, team1Athletes, team2Athletes);
}

private void startPhysicalQueueGame(CourtQueue courtQueue, List<Athlete> team1, List<Athlete> team2) {
    BasketballGame game = null;
    Location courtLoc = courtQueue.getCourtLocation();
    
    // Check if there's already a stopped game at this location
    if (games.containsKey(courtLoc)) {
        BasketballGame existingGame = games.get(courtLoc);
        if (existingGame.getState() == GoalGame.State.FINAL) {
            games.remove(courtLoc);
        }
    }
    
    // Create new game with proper parameters
    Settings settings = gameSettings.copy();
    settings.playersPerTeam = courtQueue.getPlayersPerTeam();
    game = new BasketballGame(settings, courtLoc, 26.0, 2.8, 0.45, 0.475, 0.575);
    game.isPhysicalQueueGame = true; // Mark as physical queue game
    games.put(courtLoc, game);
    
    // CRITICAL: Remove all players from spot tracking so they're no longer frozen
    for (Athlete athlete : team1) {
        playerSpots.remove(athlete.getPlayer().getUniqueId());
    }
    for (Athlete athlete : team2) {
        playerSpots.remove(athlete.getPlayer().getUniqueId());
    }
    
    // Join players to the game
    for (Athlete athlete : team1) {
        game.join(athlete);
        game.joinTeam(athlete.getPlayer(), GoalGame.Team.HOME);
    }
    
    for (Athlete athlete : team2) {
        game.join(athlete);
        game.joinTeam(athlete.getPlayer(), GoalGame.Team.AWAY);
    }
    
    // Store the court queue reference for this game
    gameCourtMapping.put(courtLoc, courtQueue);
    
    // Notify players
    for (Player p : game.getPlayers()) {
        p.sendMessage("§a§l» §aGame starting! Winners stay on the spot!");
    }
    
    // Start the game
    game.start();
    
    Bukkit.getLogger().info("[Park Queue] Started " + courtQueue.getPlayersPerTeam() + "v" + 
                           courtQueue.getPlayersPerTeam() + " game from physical queue");
}
```

### Winner-Stays Logic (Post-Game)
```java
public void onPhysicalQueueGameEnd(BasketballGame game, Athlete athlete) {
    Player player = athlete.getPlayer();
    Location courtLoc = game.getLocation();
    
    // Get the court queue for this game
    CourtQueue courtQueue = gameCourtMapping.get(courtLoc);
    if (courtQueue == null) {
        // Fallback: send to spawn if no court mapping found
        Location spawn = new Location(courtLoc.getWorld(), 9.5, -61.0, 154.5);
        player.teleport(spawn);
        Hub.basketballLobby.join(athlete);
        return;
    }
    
    // Determine if player is a winner or loser
    GoalGame.Team winningTeam = game.getHomeScore() > game.getAwayScore() ? 
        GoalGame.Team.HOME : GoalGame.Team.AWAY;
    
    List<Player> homePlayers = game.getHomePlayers();
    boolean isHome = homePlayers.contains(player);
    GoalGame.Team playerTeam = isHome ? GoalGame.Team.HOME : GoalGame.Team.AWAY;
    boolean isWinner = playerTeam == winningTeam;
    
    if (isWinner) {
        // Winner - teleport back to spot
        List<CourtSpot> winnerSpots = winningTeam == GoalGame.Team.HOME ? 
            courtQueue.getSpotsByType(CourtSpot.SpotType.TEAM1) : 
            courtQueue.getSpotsByType(CourtSpot.SpotType.TEAM2);
        
        // Find first available winner spot
        CourtSpot targetSpot = null;
        for (CourtSpot spot : winnerSpots) {
            if (!spot.isOccupied() || spot.getOccupiedBy().equals(player.getUniqueId())) {
                targetSpot = spot;
                break;
            }
        }
        
        if (targetSpot != null) {
            targetSpot.vacate();
            targetSpot.tryOccupy(player.getUniqueId());
            playerSpots.put(player.getUniqueId(), targetSpot);
            
            // Teleport to spot
            Location spotLoc = targetSpot.getLocation().clone();
            spotLoc.setY(spotLoc.getY() + 1.0);
            player.teleport(spotLoc);
            player.sendMessage("§a§l» §aYou won! Back on the spot - defend your court!");
            Hub.basketballLobby.join(athlete);
        } else {
            // No spot available, send to spawn
            Location spawn = new Location(courtLoc.getWorld(), 9.5, -61.0, 154.5);
            player.teleport(spawn);
            player.sendMessage("§a§l» §aYou won!");
            Hub.basketballLobby.join(athlete);
        }
    } else {
        // Loser - teleport to spawn area
        Location loserSpawn = new Location(courtLoc.getWorld(), 9.5, -61.0, 154.5);
        player.teleport(loserSpawn);
        player.sendMessage("§c§l» §cYou lost! Better luck next time.");
        playerSpots.remove(player.getUniqueId());
        Hub.basketballLobby.join(athlete);
    }
    
    // Clear loser spots if this is the last player being processed
    Bukkit.getScheduler().runTaskLater(Partix.getInstance(), () -> {
        if (game.getPlayers().isEmpty()) {
            List<CourtSpot> loserSpots = winningTeam == GoalGame.Team.HOME ? 
                courtQueue.getSpotsByType(CourtSpot.SpotType.TEAM2) : 
                courtQueue.getSpotsByType(CourtSpot.SpotType.TEAM1);
            for (CourtSpot spot : loserSpots) {
                if (!playerSpots.containsValue(spot)) {
                    spot.vacate();
                }
            }
            gameCourtMapping.remove(courtLoc);
            Bukkit.getLogger().info("[Park Queue] Game ended - winners stay, losers kicked");
        }
    }, 5L);
}
```

### Player Cleanup
```java
private void removeFromPhysicalQueue(UUID playerId) {
    CourtSpot spot = playerSpots.remove(playerId);
    if (spot != null) {
        spot.vacate();
    }
    
    // Also check all courts to be safe
    for (CourtQueue court : twosCourts) {
        court.removePlayer(playerId);
    }
    for (CourtQueue court : threesCourts) {
        court.removePlayer(playerId);
    }
}
```

---

## 4. Game.java Integration
**Location**: `src/main/java/me/x_tias/partix/server/specific/Game.java`

### Add to Game End Handler (around line 28-31)
```java
if (bbGame.isPhysicalQueueGame) {
    // Physical queue game - handle winner-stays logic
    // THIS CALLS THE METHOD IN BasketballLobby
    Hub.basketballLobby.onPhysicalQueueGameEnd(bbGame, athlete);
}
```

---

## 5. BasketballGame.java Addition
Add this field to mark physical queue games:
```java
public boolean isPhysicalQueueGame = false;
```

---

## Key Features Summary

### 1. **Visual System**
- Colored concrete blocks (Red/Yellow/Blue)
- Particle effects (Red when empty, Green when occupied)
- Circular particle pattern every 0.1 seconds

### 2. **Player Freezing**
- Players are "frozen" on spots via teleportation back to spot center
- Can only leave by shifting (sneaking)
- Teleports 2 blocks west when leaving

### 3. **Party Support**
- Yellow "Party" spots in the middle
- Only party leaders can join party spots
- Party size must match court type (2 for 2v2, 3 for 3v3)
- Party members auto-placed on adjacent spots

### 4. **Winner Stays**
- Winning team returns to their spots after game
- Losing team sent to spawn
- Spots remain occupied for winners

### 5. **Block Protection**
- Non-creative players can't break spot blocks
- Creative players can break, but blocks auto-replace

### 6. **Queue Conflicts**
- Prevents joining if already in another queue (1v1, etc.)
- Prevents joining if already in an active game

---

## Configuration Requirements

### Court Locations
The system requires at least 4 ranked courts in `BasketballLobby`:
```java
rankedCourts.add(new Location(world, x1, y1, z1)); // 2v2 Court 1
rankedCourts.add(new Location(world, x2, y2, z2)); // 2v2 Court 2
rankedCourts.add(new Location(world, x3, y3, z3)); // 3v3 Court 1
rankedCourts.add(new Location(world, x4, y4, z4)); // 3v3 Court 2
```

### Spot Positioning
- **18 blocks west** from court sideline
- **2 blocks spacing** between each spot
- **1 block below** court surface level
- Centered on block coordinates (+0.5)

---

## Dependencies
- Lombok (`@Getter`, `@Setter`)
- Bukkit/Spigot API
- Custom classes: `Athlete`, `AthleteManager`, `Party`, `PartyFactory`, `BasketballGame`, `GoalGame`

---

## Event Listeners Required
The `BasketballLobby` class must implement `Listener` and register these events:
- `PlayerMoveEvent` - Movement detection & freezing
- `PlayerToggleSneakEvent` - Leaving spots
- `BlockBreakEvent` - Block protection
- Standard lobby events (`PlayerQuitEvent`, etc.)

---

## Notes for Implementation
1. Make sure `BasketballLobby` implements `Listener`
2. Register event handlers in constructor
3. Add `isPhysicalQueueGame` field to `BasketballGame`
4. Hook into game end logic in `Game.java`
5. Adjust spawn locations to match your world
6. Test party functionality thoroughly
7. Ensure court locations are set correctly before initialization

---

## Testing Checklist
- [ ] Solo player can join Team 1/Team 2 spots
- [ ] Party leader with correct size can join Party spots
- [ ] Party members auto-placed when leader joins
- [ ] Players frozen on spots (can't move away)
- [ ] Shift to leave works correctly
- [ ] Game starts when both teams filled
- [ ] Winners return to spots after game
- [ ] Losers sent to spawn after game
- [ ] Particles render correctly
- [ ] Blocks can't be broken (survival mode)
- [ ] Blocks auto-replace (creative mode)
- [ ] No conflicts with other queue systems
