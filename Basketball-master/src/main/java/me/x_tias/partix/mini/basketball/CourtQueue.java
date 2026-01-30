package me.x_tias.partix.mini.basketball;

import lombok.Getter;
import me.x_tias.partix.plugin.athlete.Athlete;
import me.x_tias.partix.plugin.athlete.AthleteManager;
import me.x_tias.partix.plugin.party.Party;
import me.x_tias.partix.plugin.party.PartyFactory;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Manages a single basketball court with its physical queue spots.
 * Each court has spots for two teams and a party/waiting area.
 */
@Getter
public class CourtQueue {
    private final Location courtLocation;
    private final CourtType courtType;
    private final List<CourtSpot> spots;
    private final int playersPerTeam;
    
    public CourtQueue(Location courtLocation, CourtType courtType) {
        this.courtLocation = courtLocation;
        this.courtType = courtType;
        this.playersPerTeam = courtType == CourtType.TWOS ? 2 : 3;
        this.spots = new ArrayList<>();
        
        initializeSpots();
    }
    
    /**
     * Initialize the spots around the court
     * Layout for 3v3: [T1][T1][T1] [P][P][P] [T2][T2][T2]
     * Layout for 2v2: [T1][T1] [P][P] [T2][T2]
     */
    private void initializeSpots() {
        // Position spots 18 blocks away from the sideline of the court (5 + 13 additional)
        // Court is centered at courtLocation, sideline is approximately at X +/- 7 blocks
        double spotSpacing = 2.0; // Exactly 2 blocks between each spot
        double sidelineOffset = 7.0; // Distance from center to sideline (approximate)
        double baseX = Math.floor(courtLocation.getX() - sidelineOffset - 18.0) + 0.5; // 18 blocks outside, centered on block
        double baseZ = Math.floor(courtLocation.getZ()) + 0.5; // Aligned with court center Z, centered on block
        double baseY = Math.floor(courtLocation.getY() - 1.0); // 1 block down
        
        // Calculate starting position to center the spots along the Z axis
        int totalSpots = playersPerTeam * 3; // team1 + party + team2
        double startZ = baseZ - (spotSpacing * (totalSpots - 1) / 2.0);
        
        // Create Team 1 spots (along the sideline, spaced in Z direction)
        for (int i = 0; i < playersPerTeam; i++) {
            Location spotLoc = new Location(
                courtLocation.getWorld(),
                baseX,
                baseY,
                startZ + (i * spotSpacing)
            );
            spots.add(new CourtSpot(spotLoc, CourtSpot.SpotType.TEAM1, this, i));
        }
        
        // Create Party/Waiting spots (middle group)
        for (int i = 0; i < playersPerTeam; i++) {
            Location spotLoc = new Location(
                courtLocation.getWorld(),
                baseX,
                baseY,
                startZ + ((playersPerTeam + i) * spotSpacing)
            );
            spots.add(new CourtSpot(spotLoc, CourtSpot.SpotType.PARTY, this, i));
        }
        
        // Create Team 2 spots (last group)
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
    
    /**
     * Get all spots (for debugging)
     */
    public List<CourtSpot> getSpots() {
        return spots;
    }
    
    /**
     * Get all spots of a specific type
     */
    public List<CourtSpot> getSpotsByType(CourtSpot.SpotType type) {
        return spots.stream()
            .filter(spot -> spot.getSpotType() == type)
            .collect(Collectors.toList());
    }
    
    /**
     * Find which spot a player is standing on, if any
     */
    public CourtSpot getSpotForPlayer(Player player) {
        for (CourtSpot spot : spots) {
            if (spot.isPlayerInRange(player)) {
                return spot;
            }
        }
        return null;
    }
    
    /**
     * Get the spot occupied by a specific player UUID
     */
    public CourtSpot getSpotByOccupant(UUID playerId) {
        for (CourtSpot spot : spots) {
            if (spot.isOccupied() && spot.getOccupiedBy().equals(playerId)) {
                return spot;
            }
        }
        return null;
    }
    
    /**
     * Remove a player from any spot they're occupying
     */
    public void removePlayer(UUID playerId) {
        CourtSpot spot = getSpotByOccupant(playerId);
        if (spot != null) {
            spot.vacate();
        }
    }
    
    /**
     * Check if all spots are filled and ready to start a game
     */
    public boolean isReadyToStart() {
        // Check if all team spots are filled
        long filledTeam1 = getSpotsByType(CourtSpot.SpotType.TEAM1).stream()
            .filter(CourtSpot::isOccupied)
            .count();
        
        long filledTeam2 = getSpotsByType(CourtSpot.SpotType.TEAM2).stream()
            .filter(CourtSpot::isOccupied)
            .count();
        
        return filledTeam1 == playersPerTeam && filledTeam2 == playersPerTeam;
    }
    
    /**
     * Get all players currently on spots
     */
    public List<Player> getAllPlayersOnSpots() {
        return spots.stream()
            .filter(CourtSpot::isOccupied)
            .map(spot -> Bukkit.getPlayer(spot.getOccupiedBy()))
            .filter(p -> p != null && p.isOnline())
            .collect(Collectors.toList());
    }
    
    /**
     * Check if a player is party leader and standing on a party spot
     */
    public boolean isPartyLeaderOnPartySpot(Player player) {
        Athlete athlete = AthleteManager.get(player.getUniqueId());
        if (athlete == null) {
            return false;
        }
        
        Party party = PartyFactory.get(athlete.getParty());
        if (party == null || !party.leader.equals(player.getUniqueId())) {
            return false;
        }
        
        CourtSpot spot = getSpotByOccupant(player.getUniqueId());
        return spot != null && spot.getSpotType() == CourtSpot.SpotType.PARTY;
    }
    
    /**
     * Get all players on team1 spots
     */
    public List<Player> getTeam1Players() {
        return getSpotsByType(CourtSpot.SpotType.TEAM1).stream()
            .filter(CourtSpot::isOccupied)
            .map(spot -> Bukkit.getPlayer(spot.getOccupiedBy()))
            .filter(p -> p != null && p.isOnline())
            .collect(Collectors.toList());
    }
    
    /**
     * Get all players on team2 spots
     */
    public List<Player> getTeam2Players() {
        return getSpotsByType(CourtSpot.SpotType.TEAM2).stream()
            .filter(CourtSpot::isOccupied)
            .map(spot -> Bukkit.getPlayer(spot.getOccupiedBy()))
            .filter(p -> p != null && p.isOnline())
            .collect(Collectors.toList());
    }
    
    /**
     * Clear all spots (when game starts or is cancelled)
     */
    public void clearAllSpots() {
        spots.forEach(CourtSpot::vacate);
    }
    
    /**
     * Spawn particles for all spots
     */
    public void spawnAllParticles() {
        spots.forEach(CourtSpot::spawnParticles);
    }
    
    /**
     * Remove all blocks (cleanup on disable)
     */
    public void removeAllBlocks() {
        spots.forEach(CourtSpot::removeBlock);
    }
    
    public enum CourtType {
        TWOS,   // 2v2 court
        THREES  // 3v3 court
    }
}
