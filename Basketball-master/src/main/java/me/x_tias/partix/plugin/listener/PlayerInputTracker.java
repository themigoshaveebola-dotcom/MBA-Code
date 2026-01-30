package me.x_tias.partix.plugin.listener;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;

import me.x_tias.partix.Partix;

/**
 * Tracks player movement input using ProtocolLib packet reading.
 * Detects WASD movement instantly by analyzing position packets.
 */
public class PlayerInputTracker {
    
    private static final Map<UUID, InputState> playerInputs = new ConcurrentHashMap<>();
    private static final Map<UUID, Location> lastLocations = new ConcurrentHashMap<>();
    
    public static class InputState {
        public boolean forward;   // W key
        public boolean backward;  // S key
        public boolean left;      // A key
        public boolean right;     // D key
        public boolean jump;      // Space
        public boolean sneak;     // Shift
        public boolean sprint;    // Double-tap W or Ctrl
        
        public InputState() {
            this.forward = false;
            this.backward = false;
            this.left = false;
            this.right = false;
            this.jump = false;
            this.sneak = false;
            this.sprint = false;
        }
    }
    
    /**
     * Register the packet listener to track player inputs
     */
    public static void register(Partix plugin) {
        // Listen to position packets for INSTANT movement detection
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(
                plugin, 
                PacketType.Play.Client.POSITION,
                PacketType.Play.Client.POSITION_LOOK
        ) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                Player player = event.getPlayer();
                UUID uuid = player.getUniqueId();
                
                // Get or create input state
                InputState state = playerInputs.computeIfAbsent(uuid, k -> new InputState());
                
                try {
                    // Read new position from packet
                    double newX = event.getPacket().getDoubles().read(0);
                    double newY = event.getPacket().getDoubles().read(1);
                    double newZ = event.getPacket().getDoubles().read(2);
                    
                    Location lastLoc = lastLocations.get(uuid);
                    
                    if (lastLoc != null) {
                        // Calculate movement vector
                        double deltaX = newX - lastLoc.getX();
                        double deltaZ = newZ - lastLoc.getZ();
                        
                        // Only process if there's horizontal movement
                        double movementSquared = deltaX * deltaX + deltaZ * deltaZ;
                        if (movementSquared > 0.0001) {
                            // Get player's facing direction
                            org.bukkit.util.Vector facing = player.getLocation().getDirection().setY(0).normalize();
                            org.bukkit.util.Vector right = new org.bukkit.util.Vector(-facing.getZ(), 0, facing.getX()).normalize();
                            
                            // Movement direction
                            org.bukkit.util.Vector movement = new org.bukkit.util.Vector(deltaX, 0, deltaZ).normalize();
                            
                            // Project movement onto facing and right vectors
                            double forwardDot = movement.dot(facing);
                            double rightDot = movement.dot(right);
                            
                            // Detect direction (threshold 0.5 = ~60 degrees)
                            state.forward = forwardDot > 0.5;
                            state.backward = forwardDot < -0.5;
                            state.right = rightDot > 0.5;
                            state.left = rightDot < -0.5;
                        } else {
                            // No movement
                            state.forward = false;
                            state.backward = false;
                            state.right = false;
                            state.left = false;
                        }
                    }
                    
                    // Store current location for next packet
                    lastLocations.put(uuid, new Location(player.getWorld(), newX, newY, newZ));
                    
                } catch (Exception e) {
                    // Ignore packet read errors
                }
                
                // Update jump/sneak/sprint from player state
                state.jump = !player.isOnGround() && player.getVelocity().getY() > 0.1;
                state.sneak = player.isSneaking();
                state.sprint = player.isSprinting();
            }
        });
    }
    
    /**
     * Get the current input state for a player
     */
    public static InputState getInput(Player player) {
        return playerInputs.computeIfAbsent(player.getUniqueId(), k -> new InputState());
    }
    
    /**
     * Remove player from tracking (call on disconnect)
     */
    public static void removePlayer(UUID uuid) {
        playerInputs.remove(uuid);
        lastLocations.remove(uuid);
    }
    
    /**
     * Clear all tracked inputs
     */
    public static void clear() {
        playerInputs.clear();
        lastLocations.clear();
    }
}
