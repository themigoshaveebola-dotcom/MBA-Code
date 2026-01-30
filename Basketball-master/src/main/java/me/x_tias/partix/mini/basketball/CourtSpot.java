package me.x_tias.partix.mini.basketball;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Represents a physical spot on a basketball court where players can stand to queue.
 * Inspired by NBA 2K Park queue system.
 */
@Getter
public class CourtSpot {
    private final Location location;
    private final SpotType spotType;
    private final CourtQueue court;
    private final int spotIndex; // 0, 1, 2 for team spots
    
    @Setter
    private UUID occupiedBy; // null if empty
    
    public CourtSpot(Location location, SpotType spotType, CourtQueue court, int spotIndex) {
        this.location = location;
        this.spotType = spotType;
        this.court = court;
        this.spotIndex = spotIndex;
        this.occupiedBy = null;
        
        // Place initial red concrete block
        updateBlock();
    }
    
    /**
     * Check if a player is within range of this spot
     */
    public boolean isPlayerInRange(Player player) {
        if (player.getWorld() != location.getWorld()) {
            return false;
        }
        // Check if player is within ~1.5 block radius for easier activation
        return player.getLocation().distance(location) <= 1.5;
    }
    
    /**
     * Check if this spot is currently occupied
     */
    public boolean isOccupied() {
        return occupiedBy != null;
    }
    
    /**
     * Attempt to occupy this spot with a player
     */
    public boolean tryOccupy(UUID playerId) {
        if (isOccupied()) {
            return false;
        }
        occupiedBy = playerId;
        updateBlock();
        return true;
    }
    
    /**
     * Remove the player from this spot
     */
    public void vacate() {
        occupiedBy = null;
        updateBlock();
    }
    
    /**
     * Update the concrete block based on occupied status
     */
    public void updateBlock() {
        if (location.getWorld() == null) {
            return;
        }
        
        Location blockLoc = location.getBlock().getLocation();
        // Block color stays the same whether occupied or not - only particles change
        Material blockType = switch (spotType) {
            case TEAM1 -> Material.RED_CONCRETE;
            case PARTY -> Material.YELLOW_CONCRETE;
            case TEAM2 -> Material.BLUE_CONCRETE;
        };
        blockLoc.getBlock().setType(blockType);
    }
    
    /**
     * Spawn particles at this spot location
     */
    public void spawnParticles() {
        if (location.getWorld() == null) {
            return;
        }
        
        // Center position on block (add 0.5 to get block center)
        double centerX = location.getBlockX() + 0.5;
        double centerZ = location.getBlockZ() + 0.5;
        double particleY = location.getY() + 1.0;
        
        // Spawn particles in a circle pattern above the block
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
                    x,
                    particleY,
                    z,
                    1,
                    0, 0, 0,
                    0
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
                    x,
                    particleY,
                    z,
                    1,
                    0, 0, 0,
                    0,
                    new Particle.DustOptions(org.bukkit.Color.RED, 1.5f)
                );
            }
        }
    }
    
    /**
     * Remove the concrete block (cleanup when lobby is disabled)
     */
    public void removeBlock() {
        if (location.getWorld() != null) {
            location.getBlock().setType(Material.AIR);
        }
    }
    
    public enum SpotType {
        TEAM1,    // Left side spots
        PARTY,    // Middle/waiting spots
        TEAM2     // Right side spots
    }
}
