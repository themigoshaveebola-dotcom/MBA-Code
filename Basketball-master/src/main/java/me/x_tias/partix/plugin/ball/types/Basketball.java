package me.x_tias.partix.plugin.ball.types;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import me.x_tias.partix.plugin.athlete.Athlete;
import me.x_tias.partix.plugin.athlete.AthleteManager;
import me.x_tias.partix.plugin.cosmetics.CosmeticSound;
import me.x_tias.partix.plugin.listener.PlayerInputTracker;
import org.bukkit.util.BoundingBox;
import me.x_tias.partix.Partix;
import java.util.HashMap;
import java.util.Map;
import me.x_tias.partix.database.PlayerDb;
import me.x_tias.partix.mini.basketball.BasketballGame;
import me.x_tias.partix.mini.basketball.ScreenManager;
import me.x_tias.partix.mini.game.GoalGame;
import me.x_tias.partix.mini.game.PlayerStats;
import me.x_tias.partix.plugin.ball.BallFactory;
import me.x_tias.partix.plugin.ball.BallType;
import me.x_tias.partix.plugin.cosmetics.CosmeticBallTrail;
import me.x_tias.partix.plugin.cosmetics.Cosmetics;
import me.x_tias.partix.util.Colour;
import me.x_tias.partix.util.Position;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.TitlePart;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.*;

public class Basketball
        extends me.x_tias.partix.plugin.ball.Ball {
    private static final int STEAL_IMMUNITY_DURATION = 20;
    
    // Shot Make Formula Constants
    private static final double CONTEST_STRENGTH = 0.7;
    private static final double GREEN_WINDOW_THRESHOLD = 0.03;  // 3% timing error tolerance
    private static final int PERFECT_TIMING_MIN = 7;  // accuracy 7-8 = green
    private static final double LIGHT_CONTEST_THRESHOLD = 0.2;  // 20% contest or less
    
    @Getter
    private final BasketballGame game;
    private final Map<UUID, Integer> contestTime = new HashMap<>();
    private double meterStartContestPercentage = 0.0; // Contest calculated when meter starts (for jump shots)
    public int delay = 0;
    public boolean isShouldPreventScore() {
        return this.shouldPreventScore;
    }
    public boolean isLobPass() {
        return this.isLobPass;
    }
    @Getter
    private PassMode passMode = PassMode.BULLET; // Default to bullet pass
    private Map<UUID, Map<Integer, UUID>> teamTrackingAssignments = new HashMap<>();  // Player UUID → (hotkey → teammate UUID)
    private Map<UUID, Long> directionLockUntil = new HashMap<>();  // UUID → lock expiry time
    private Map<UUID, Vector> lockedDirection = new HashMap<>();  // UUID → locked velocity direction
    private UUID currentTrackPassTarget = null;  // Currently being tracked for interception


    // Add this enum at the end of the Basketball class:
    public enum PassMode {
        BULLET("Bullet Pass"),
        LOB("Lob Pass");

        private final String displayName;

        PassMode(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public PassMode toggle() {
            return switch (this) {
                case BULLET -> LOB;
                case LOB -> BULLET;  // ← Only toggles between these two
            };
        }
    }
    private boolean isLobPass = false;  // Track if current pass is a lob pass
    private boolean dunkMeterStartedAirborne = false;
    private UUID lobPasserUUID = null;  // Track who threw the lob pass
    private boolean perfectShot = false;
    public boolean layupScored = false; // NEW: Track if this layup already scored
    private boolean layupScoreDetected = false;
    private boolean guaranteedMiss = false;
    private boolean wasShot = false; // Track if ball was released as a shot
    public boolean shouldPreventScore = false;
    private long lastMovementTime = 0L; // When player last moved with ball
    private boolean hasMovedWithBall = false; // Has player moved since catching ball
    private static final long STANDING_STILL_DURATION = 500L; // 0.5 seconds - shorter window for off-dribble detection
    @Getter
    private double shotDistance = 0.0;
    private Location shotLocation = null;

    private Map<UUID, Long> defenderBlockAttempts = new HashMap<>();
    private Map<UUID, Long> pokeStealImmunity = new HashMap<>(); // Collision immunity after poke steal
    private Map<UUID, Long> stolenFromClickCooldown = new HashMap<>(); // 1.5s click cooldown after being stolen from
    private Map<UUID, Boolean> stolenClickRollResult = new HashMap<>(); // 20% roll result (prevents spam-click abuse)
    private Map<UUID, Long> lastBallPickupTime = new HashMap<>(); // Track when player picked up ball (1s steal protection)
    private Map<UUID, Long> stolenFromDashLockout = new HashMap<>(); // 3s dash lockout after being poke stealed
    private Map<UUID, Long> ankleBreakRecovery = new HashMap<>(); // ADD THIS
    private Map<UUID, Long> sittingAnimationLock = new HashMap<>(); // Prevents shift dismount during animations
    private int globalStealCooldown = 0; // 4-second cooldown after any steal
    private Map<UUID, Long> defenderProximityTracking = new HashMap<>(); // Tracks when defenders were last near ball handler (for late closeout detection)
    // ===== ANKLE BREAK SYSTEM =====
    private Map<UUID, Long> ankleBreakImmunity = new HashMap<>(); // Tracks 3s immunity
    private UUID stealAttemptDefender = null; // Tracks who attempted the steal
    private UUID originalShotterUUID = null;  // Who took the shot before block
    private boolean isBlockedShot = false;    // Flag to prevent blocker possession
    private long blockTime = 0L;              // When the block occurred
    private long stealAttemptTime = 0L; // When the steal was attempted
    private Map<UUID, Long> stealAttemptsOnBallHandler = new HashMap<>(); // Ball handler's dribble window
    private boolean stealMissedThisTick = false; // Track if steal missed this tick

    private Map<UUID, Long> dashCooldowns = new HashMap<>(); // Track dash cooldown expiry time per player
    private Map<UUID, Long> lastDashTime = new HashMap<>(); // Track when dash was last used
    private Map<UUID, Integer> dashJumpLock = new HashMap<>(); // Prevent jumping after dash
    private static final long DASH_COOLDOWN_MS = 5000L; // 5 seconds in milliseconds
    private static final int DASH_SLOWNESS_DURATION = 60; // 3 seconds in ticks
    private static final int DASH_JUMP_LOCK_TICKS = 10; // 0.5 seconds jump lock after dash

    private Map<UUID, Long> layupBlockAttempts = new HashMap<>(); // Add this field at the top with other fields
    private static final double LAYUP_BLOCK_RADIUS = 4.5; // 3 blocks range

    // Ground steal check
    private static final double STEAL_HITBOX_RADIUS = 2.5; // Increased from ~1.5
    private static final int STEAL_STUN_DURATION = 10; // 0.5 seconds in ticks
    private static final int STEAL_COOLDOWN_AFTER_MISS = 15; // 0.75 seconds in ticks
    private static final int ANKLE_BREAK_IMMUNITY_TICKS = 60; // 3 seconds in ticks
    private static final int DRIBBLE_COUNTER_WINDOW = 6; // 0.3 seconds in ticks
    private static final double ANKLE_BREAK_SUCCESS_CHANCE = 0.4; // 40% chance
    private static final int ANKLE_BREAK_FREEZE_MIN = 20; // 1 second minimum
    private static final int ANKLE_BREAK_FREEZE_MAX = 60; // 3 seconds maximum
    private Player posterizedDefender = null;
    private UUID lastPossessionBeforePoke = null; // Track who had possession before ball was poked
    public boolean wasPoked = false; // Track if ball was just poked loose
    private boolean dunkMeterActive = false;
    private int dunkMeterAccuracy = 0;
    private int dunkMeterWait = 0;
    private int stepbackDelay = 0; // NEW: Add this line
    private boolean hopstepActive = false; // Track if player is in hopstep animation
    // Hopstep travel tracking
    private boolean hopstepTravelCheck = false;
    private Location hopstepStartLocation = null;
    private double hopstepDistanceTraveled = 0.0;
    private int hopstepCompletionTicks = 0; // Ticks since hopstep started
    // Location tracking for accurate WASD detection
    private Location lastPlayerLocation = null;
    private long lastLocationUpdate = 0;
    private int passDelay = 0;
    private int catchDelay = 0; // NEW: Delay after catching ball
    private UUID justDunkedPlayer = null; // Track who just completed a dunk attempt
    private boolean dunkMeterForward = true;
    public boolean isDunkAttempt = false;
    private UUID lastOwnerUUID = null;
    private UUID lastPossessorUUID = null; // Track actual possession, not just touches
    private long perfectShotStartTime = 0L;
    private int ownerTicks;
    private int handYaw = 50;
    private int powerCrossoverCooldown = 0;
    private int handModifier = 5;
    private int hesiDelay = 0;
    public UUID lastShotBlockerUUID = null;
    public boolean isLayupAttempt = false;
    private boolean hesiActive = false;
    private int hesiAnimationTicks = 0;
    private int btbDelay = 0;
    private boolean btbActive = false;
    private int btbAnimationTicks = 0;
    private int btbTargetHand = 0;
    @Getter
    private boolean threeEligible = false;
    private int accuracy = 0;
    private int accuracyWait = 0;
    private int stealImmunityTicks = 0;
    @Setter
    @Getter

    private UUID assister;
    @Setter
    @Getter
    private boolean reboundEligible = false;
    private final boolean hadPossessionOnGround = false;
    private final boolean travelCalled = false;
    private boolean wasInSlabZone = false;
    @Setter
    @Getter
    private boolean shotAttemptRegistered = false;

    private DistanceZone distanceZone = DistanceZone.SHORT;

    private double shotContestPercentage;

    private boolean forwardBar = true;


    public Basketball(Location location, BasketballGame game) {
        super(location, game, BallType.BASKETBALL, 0.4, 0.2, 0.2, 0.015, 0.025, 0.35, 0.01, 0.265, false, false, 2.0, Color.fromRGB(14970945), Color.BLACK);
        this.game = game;
        this.ownerTicks = 405;
    }

    @Override
    public Component getControls(Player player) {
        String leftClick = "Pass";
        String rightClick = "Shoot";
        String dropItem = this.canDunk(player) ? "Dunk!" : "Layup";
        String btb = "BTB";
        String hesi = "Hesi";
        String crossover = "Crossover";
        String stepback = "Stepback";

        Component lc = Component.text("[", Colour.blackBorder()).append(Component.keybind("key.attack", Colour.border()).append(Component.text("]", Colour.blackBorder())).append(Component.text(" " + leftClick + ", ", Colour.darkBorder())));
        Component rc = Component.text("[", Colour.blackBorder()).append(Component.keybind("key.use", Colour.border()).append(Component.text("]", Colour.blackBorder())).append(Component.text(" " + rightClick + ", ", Colour.darkBorder())));
        Component di = Component.text("[", Colour.blackBorder()).append(Component.keybind("key.drop", Colour.border()).append(Component.text("]", Colour.blackBorder())).append(Component.text(" " + dropItem + ", ", Colour.darkBorder())));
        Component bb = Component.text("[", Colour.blackBorder()).append(Component.keybind("key.hotbar.2", Colour.border()).append(Component.text("]", Colour.blackBorder())).append(Component.text(" " + btb + ", ", Colour.darkBorder())));
        Component hs = Component.text("[", Colour.blackBorder()).append(Component.keybind("key.hotbar.3", Colour.border()).append(Component.text("]", Colour.blackBorder())).append(Component.text(" " + hesi + ", ", Colour.darkBorder())));
        Component co = Component.text("[", Colour.blackBorder()).append(Component.keybind("key.hotbar.4", Colour.border()).append(Component.text("]", Colour.blackBorder())).append(Component.text(" " + crossover + ", ", Colour.darkBorder())));
        Component sb = Component.text("[", Colour.blackBorder()).append(Component.keybind("key.hotbar.5", Colour.border()).append(Component.text("]", Colour.blackBorder())).append(Component.text(" " + stepback, Colour.darkBorder())));

        return lc.append(rc).append(di).append(bb).append(hs).append(co).append(sb);
    }

    public void throwBall(Player player) {
        if (this.getCurrentDamager() != null && this.getCurrentDamager() == player) {
            // DEBUG: Check if inbound state is stale (player marked as inbounder but shouldn't be)
            if (this.game.inboundingActive && this.game.inbounder != null) {
                // Check if this is a stale inbound state by seeing if the inbound pass already happened
                if (this.game.inboundPassTime > 0 && 
                    (System.currentTimeMillis() - this.game.inboundPassTime) > 2000L) {
                    // Inbound pass happened more than 2 seconds ago - this is stale, clear it
                    System.out.println("CLEARING STALE INBOUND STATE - inbound pass was " + 
                        (System.currentTimeMillis() - this.game.inboundPassTime) + "ms ago");
                    this.game.inboundingActive = false;
                    this.game.inbounder = null;
                    this.game.inboundTouchedByInbounder = false;
                    this.game.inbounderHasReleased = false;
                }
            }
            
            // PREVENT SHOOTING DURING INBOUND
            if (this.game.inboundingActive && player.equals(this.game.inbounder)) {
                player.sendMessage(Component.text("You can't shoot while inbounding!").color(Colour.deny()));
                return; // ADDED: Exit early to prevent shooting
            }
            
            // DEBUG: Check if player is wrongly flagged
            if (this.game.inboundingActive && !player.equals(this.game.inbounder)) {
                System.out.println("DEBUG INBOUND BUG: inboundingActive=true but player " + player.getName() + " is NOT the inbounder!");
                System.out.println("  Current inbounder: " + (this.game.inbounder != null ? this.game.inbounder.getName() : "NULL"));
                System.out.println("  This shouldn't prevent shooting for non-inbounders");
            }

            if (!player.isOnGround()) {
                this.executeThrow(player);
            } else {
                player.sendMessage("§cYou must be in the air to shoot!");
            }

            // MOVED: This check should only happen AFTER successful throw
            if (this.game.inboundingActive && player.equals(this.game.inbounder)) {
                this.game.dropInboundBarrierButKeepClockFrozen();
            }
        }
    }

    // Initialize hotkey assignments for all players at game start
    public void initializeAllTeammateHotkeys(BasketballGame game) {
        System.out.println("=== INITIALIZING TEAMMATE HOTKEYS ===");
        teamTrackingAssignments.clear();
        
        // Initialize for home team
        List<Player> homePlayers = game.getHomePlayers();
        System.out.println("Home team players: " + homePlayers.size());
        for (Player player : homePlayers) {
            assignTeammateHotkeysForPlayer(player, homePlayers);
        }
        
        // Initialize for away team
        List<Player> awayPlayers = game.getAwayPlayers();
        System.out.println("Away team players: " + awayPlayers.size());
        for (Player player : awayPlayers) {
            assignTeammateHotkeysForPlayer(player, awayPlayers);
        }
        
        System.out.println("=== HOTKEY INITIALIZATION COMPLETE ===");
        System.out.println("Total players with assignments: " + teamTrackingAssignments.size());
        System.out.println("Players: " + teamTrackingAssignments.keySet());
    }

    // Helper method to assign hotkeys for a specific player based on their team
    private void assignTeammateHotkeysForPlayer(Player player, List<Player> teamPlayers) {
        // Get all OTHER players on this team (exclude self)
        List<Player> otherTeammates = new ArrayList<>();
        for (Player teammate : teamPlayers) {
            if (!teammate.equals(player)) {
                otherTeammates.add(teammate);
            }
        }

        // Edge case: 1v1 or only 1 player on team
        if (otherTeammates.isEmpty()) {
            System.out.println(player.getName() + " has no teammates to assign hotkeys");
            return;
        }

        // Create this player's hotkey map
        Map<Integer, UUID> playerHotkeys = new HashMap<>();

        // Edge case: Only 1 teammate (2v2, 2v1, etc.)
        if (otherTeammates.size() == 1) {
            // Only assign one hotkey
            playerHotkeys.put(2, otherTeammates.get(0).getUniqueId());
            System.out.println(player.getName() + " -> " + otherTeammates.get(0).getName() + " on Key 2");
        }
        // Normal case: 2+ teammates (3v3, 5v5, etc.)
        else if (otherTeammates.size() >= 2) {
            // Assign first teammate to slot 2
            playerHotkeys.put(2, otherTeammates.get(0).getUniqueId());
            System.out.println(player.getName() + " -> " + otherTeammates.get(0).getName() + " on Key 2");

            // Assign second teammate to slot 3
            playerHotkeys.put(3, otherTeammates.get(1).getUniqueId());
            System.out.println(player.getName() + " -> " + otherTeammates.get(1).getName() + " on Key 3");
        }

        // Store this player's hotkey mappings
        teamTrackingAssignments.put(player.getUniqueId(), playerHotkeys);
    }
    
    // DEPRECATED: Kept for backward compatibility but should not be called during possession changes
    @Deprecated
    public void assignTeammateHotkey(Player player, BasketballGame game) {
        // This method is now deprecated - hotkeys should be initialized once at game start
        // and only updated when players are subbed in/out
        System.out.println("WARNING: assignTeammateHotkey called during possession change - hotkeys should be stable");
    }


    public Player getTeammateByHotkey(int hotkey, Player passer) {
        BasketballGame game = this.game;

        // If game is null or passer is null, return null
        if (game == null || passer == null) {
            System.out.println("DEBUG: getTeammateByHotkey - game or passer is null");
            return null;
        }

        // Get this passer's hotkey assignments
        Map<Integer, UUID> passerHotkeys = teamTrackingAssignments.get(passer.getUniqueId());
        if (passerHotkeys == null) {
            System.out.println("DEBUG: No hotkey assignments found for " + passer.getName() + " (UUID: " + passer.getUniqueId() + ")");
            System.out.println("DEBUG: teamTrackingAssignments size: " + teamTrackingAssignments.size());
            System.out.println("DEBUG: teamTrackingAssignments keys: " + teamTrackingAssignments.keySet());
            return null;
        }

        // Get the teammate UUID assigned to this hotkey
        UUID teammateUUID = passerHotkeys.get(hotkey);
        if (teammateUUID == null) {
            System.out.println("DEBUG: No teammate assigned to hotkey " + hotkey + " for " + passer.getName());
            System.out.println("DEBUG: Available hotkeys: " + passerHotkeys.keySet());
            return null;
        }

        // Get the actual Player object
        Player teammate = Bukkit.getPlayer(teammateUUID);
        if (teammate == null) {
            System.out.println("DEBUG: Teammate with UUID " + teammateUUID + " is not online");
        } else {
            System.out.println("DEBUG: Found teammate " + teammate.getName() + " for hotkey " + hotkey);
        }
        return teammate;
    }

    private InterceptData calculateInterceptPoint(Player passer, Player receiver) {
        Vector receiverVelocity = receiver.getVelocity();
        double horizontalSpeed = Math.sqrt(
                receiverVelocity.getX() * receiverVelocity.getX() +
                        receiverVelocity.getZ() * receiverVelocity.getZ()
        );

        System.out.println("=== INTERCEPT CALCULATION ===");

        // Use receiver's actual Y position + chest height offset
        // Don't use getHighestBlockYAt as it can return incorrect values (like roofs)
        double chestHeight = receiver.getLocation().getY() + 0.5;

        // Check if receiver is sprinting or walking
        boolean isMoving = receiver.isSprinting() || horizontalSpeed > 0.01;

        // DYNAMIC: Adjust ball speed based on distance
        // Close passes are faster, long passes are slower for better control
        double passerToReceiverDistance = passer.getLocation().distance(receiver.getLocation());
        double TRACK_PASS_SPEED;
        
        if (passerToReceiverDistance <= 8.0) {
            // Close range: faster passes (1.2 blocks/tick)
            TRACK_PASS_SPEED = 1.2;
        } else if (passerToReceiverDistance <= 15.0) {
            // Medium range: normal speed (0.9 blocks/tick)
            TRACK_PASS_SPEED = 0.9;
        } else {
            // Long range: slower passes for better accuracy (0.6 blocks/tick)
            TRACK_PASS_SPEED = 0.6;
        }

        if (!isMoving) {
            Location standingTarget = receiver.getLocation().clone();
            standingTarget.setY(chestHeight);
            lockReceiverInPlace(receiver, standingTarget);
            System.out.println("→ Receiver is STANDING STILL");

            // Calculate ball travel time to standing receiver
            double distance = passer.getLocation().distance(standingTarget);
            double travelTime = distance / TRACK_PASS_SPEED;

            return new InterceptData(standingTarget, travelTime);
        }

        System.out.println("→ Receiver is MOVING - calculating lead pass");

        // Get receiver's movement direction
        Vector receiverDirection;
        double receiverSpeedPerTick;

        if (horizontalSpeed < 0.05) {
            receiverDirection = receiver.getLocation().getDirection().clone();
            receiverDirection.setY(0);
            receiverDirection.normalize();
            receiverSpeedPerTick = 0.38;
        } else {
            receiverDirection = receiverVelocity.clone();
            receiverDirection.setY(0);
            receiverDirection.normalize();
            receiverSpeedPerTick = horizontalSpeed;
        }

        // ITERATIVE SOLUTION - Find where receiver and ball meet
        Location interceptPoint = receiver.getLocation().clone();
        double bestTime = 0;

        for (int iteration = 0; iteration < 15; iteration++) {
            // Lead pass calculation - realistic lead that receiver can catch in stride
            // With fixed ball speed, this creates consistent lead distances
            double leadMultiplier = 1.25;  // Tuned for realistic catch timing with fixed speed
            Location predictedReceiverPos = receiver.getLocation().clone()
                    .add(receiverDirection.clone().multiply(receiverSpeedPerTick * bestTime * leadMultiplier));

            double ballTravelDistance = passer.getLocation().distance(predictedReceiverPos);
            double ballTravelTime = ballTravelDistance / TRACK_PASS_SPEED;

            if (Math.abs(ballTravelTime - bestTime) < 0.5) {
                interceptPoint = predictedReceiverPos;
                break;
            }

            bestTime = ballTravelTime;
            interceptPoint = predictedReceiverPos;
        }

        // FIXED: Always use chest height regardless of receiver's current Y position
        interceptPoint.setY(chestHeight);

        System.out.println("Ball arrives in " + String.format("%.1f", bestTime) + " ticks at chest height");
        System.out.println("Lead distance: " + String.format("%.2f", receiver.getLocation().distance(interceptPoint)) + " blocks");

        return new InterceptData(interceptPoint, bestTime);
    }

    // Add this helper class
    private static class InterceptData {
        final Location location;
        final double travelTime; // in ticks

        InterceptData(Location location, double travelTime) {
            this.location = location;
            this.travelTime = travelTime;
        }
    }

    private void lockReceiverInPlace(Player receiver, Location lockLocation) {
        UUID receiverUUID = receiver.getUniqueId();

        // Lock for 3 seconds max
        long lockDuration = 1000L;
        directionLockUntil.put(receiverUUID, System.currentTimeMillis() + lockDuration);

        // Store ZERO vector to keep them frozen
        lockedDirection.put(receiverUUID, new Vector(0, 0, 0));

        receiver.sendActionBar(Component.text("⬇ RECEIVING PASS - STAY PUT ⬇")
                .color(NamedTextColor.YELLOW)
                .decorate(TextDecoration.BOLD));

        System.out.println("Locked " + receiver.getName() + " in place for standing track pass");
    }
    private double calculateTrackPassArcHeight(double horizontalDistance) {
        // Lower arc for faster, more direct passes
        if (horizontalDistance <= 5) {
            return 0.3;  // Very low arc for close passes
        } else if (horizontalDistance <= 10) {
            return 0.5 + ((horizontalDistance - 5) / 20.0);  // Gradual increase
        } else if (horizontalDistance <= 15) {
            return 0.75 + ((horizontalDistance - 10) / 25.0);
        } else {
            return 1.0 + ((horizontalDistance - 15) / 30.0);  // Cap at reasonable height
        }
    }

    private void lockReceiverDirection(Player receiver, Vector receiverVelocity, Location ballLandingSpot, double ballTravelTime) {
        UUID receiverUUID = receiver.getUniqueId();

        // Calculate distance receiver needs to travel
        double distanceToIntercept = receiver.getLocation().distance(ballLandingSpot);

        // Calculate required speed to reach intercept point when ball arrives
        // ballTravelTime is in ticks, so we need blocks per tick
        double requiredSpeed = distanceToIntercept / ballTravelTime;

        // Cap speed at realistic values (sprinting = ~0.28 blocks/tick)
        double maxSpeed = 0.32; // Slightly faster than sprint for dramatic effect
        double minSpeed = 0.15; // Minimum movement speed
        requiredSpeed = Math.max(minSpeed, Math.min(maxSpeed, requiredSpeed));

        System.out.println("Receiver needs to travel " + String.format("%.2f", distanceToIntercept) +
                " blocks in " + String.format("%.1f", ballTravelTime) + " ticks");
        System.out.println("Required speed: " + String.format("%.4f", requiredSpeed) + " blocks/tick");

        // Lock duration matches ball travel time
        long lockDuration = (long) (ballTravelTime * 50); // Convert ticks to ms
        lockDuration = Math.min(lockDuration, 3000L); // Max 3 seconds

        // Calculate direction TO THE INTERCEPT POINT
        Vector dragDirection = ballLandingSpot.toVector()
                .subtract(receiver.getLocation().toVector())
                .normalize()
                .multiply(requiredSpeed); // Apply calculated speed

        lockedDirection.put(receiverUUID, dragDirection);
        directionLockUntil.put(receiverUUID, System.currentTimeMillis() + lockDuration);

        receiver.sendActionBar(Component.text("⬆ RECEIVING PASS ⬆")
                .color(NamedTextColor.GREEN)
                .decorate(TextDecoration.BOLD));

        System.out.println("Dragging " + receiver.getName() + " at " +
                String.format("%.4f", requiredSpeed) + " blocks/tick for " +
                lockDuration + "ms to reach intercept point");
    }

    public boolean trackPass(Player passer, Player receiver) {
        if (receiver == null || this.getCurrentDamager() == null ||
                !this.getCurrentDamager().equals(passer)) {
            return false;
        }

        if (this.catchDelay > 0) {
            passer.playSound(passer.getLocation(), Sound.ENTITY_ARMOR_STAND_BREAK,
                    SoundCategory.MASTER, 100.0f, 1.0f);
            return false;
        }

        if (this.delay < 1 && this.passDelay < 1) {
            Location passerLoc = passer.getEyeLocation().clone();
            passerLoc.subtract(0.0, 0.5, 0.0);

            // Calculate intercept point and travel time
            InterceptData interceptData = calculateInterceptPoint(passer, receiver);
            Location interceptPoint = interceptData.location;
            double ballTravelTime = interceptData.travelTime;

            System.out.println("=== TRACK PASS START ===");
            System.out.println("Passer: " + passer.getName() + " → Receiver: " + receiver.getName());
            System.out.println("Ball will arrive in: " + String.format("%.1f", ballTravelTime) + " ticks");

            // Calculate arc
            double horizontalDist = Math.sqrt(
                    Math.pow(interceptPoint.getX() - passerLoc.getX(), 2) +
                            Math.pow(interceptPoint.getZ() - passerLoc.getZ(), 2)
            );

            double arcHeight;
            if (horizontalDist <= 5) {
                arcHeight = 0.2;
            } else if (horizontalDist <= 10) {
                arcHeight = 0.35 + ((horizontalDist - 5) / 30.0);
            } else if (horizontalDist <= 15) {
                arcHeight = 0.5 + ((horizontalDist - 10) / 25.0);
            } else {
                arcHeight = 0.7 + ((horizontalDist - 15) / 35.0);
            }

            double midX = (passerLoc.getX() + interceptPoint.getX()) / 2;
            double midY = Math.max(passerLoc.getY(), interceptPoint.getY()) + arcHeight;
            double midZ = (passerLoc.getZ() + interceptPoint.getZ()) / 2;

            Location p1 = new Location(passerLoc.getWorld(), midX, midY, midZ);
            List<Location> bezierPoints = bezierCurve(40, passerLoc, p1, interceptPoint);

            System.out.println("DEBUG TRACK PASS: Start=" + passerLoc.getX() + "," + passerLoc.getY() + "," + passerLoc.getZ());
            System.out.println("DEBUG TRACK PASS: End=" + interceptPoint.getX() + "," + interceptPoint.getY() + "," + interceptPoint.getZ());
            System.out.println("DEBUG TRACK PASS: First bezier point=" + bezierPoints.get(0).getX() + "," + bezierPoints.get(0).getY() + "," + bezierPoints.get(0).getZ());

            // Lock receiver's movement toward intercept point with calculated speed
            lockReceiverDirection(receiver, receiver.getVelocity(), interceptPoint, ballTravelTime);
            currentTrackPassTarget = receiver.getUniqueId();

            setLocked(true);
            final Player finalReceiver = receiver;
            final Location finalInterceptPoint = interceptPoint;

            new BukkitRunnable() {
                int index = 0;
                int ticksInFlight = 0;
                boolean ballReachedEnd = false;

                @Override
                public void run() {
                    ticksInFlight++;

                    if (!isValid()) {
                        setLocked(false);
                        currentTrackPassTarget = null;
                        directionLockUntil.remove(finalReceiver.getUniqueId());
                        lockedDirection.remove(finalReceiver.getUniqueId());
                        cancel();
                        return;
                    }

                    Location ballLoc = this.getBallLocation();

                    // Check if receiver caught it
                    double distanceToReceiver = finalReceiver.getLocation().distance(ballLoc);
                    if (distanceToReceiver < 2.2) {
                        System.out.println(finalReceiver.getName() + " CAUGHT THE PASS!");
                        setDamager(finalReceiver);
                        
                        // Clear inbound state if the new possessor is not the inbounder
                        if (Basketball.this.game instanceof BasketballGame bbGame) {
                            bbGame.clearInboundStateIfNotInbounder(finalReceiver);
                        }
                        
                        setLocked(false);
                        currentTrackPassTarget = null;
                        directionLockUntil.remove(finalReceiver.getUniqueId());
                        lockedDirection.remove(finalReceiver.getUniqueId());
                        finalReceiver.playSound(finalReceiver.getLocation(), Sound.ENTITY_ITEM_PICKUP,
                                SoundCategory.MASTER, 1.0f, 1.2f);
                        cancel();
                        return;
                    }

                    // Check for defender interception
                    for (Player defender : finalReceiver.getWorld().getPlayers()) {
                        if (game.getTeamOf(defender) == null || game.getTeamOf(finalReceiver) == null) continue;
                        if (game.getTeamOf(defender).equals(game.getTeamOf(finalReceiver))) continue;

                        if (defender.getLocation().distance(ballLoc) < 1.5) {
                            System.out.println(defender.getName() + " INTERCEPTED!");
                            setDamager(defender);
                            
                            // Clear inbound state if the new possessor is not the inbounder
                            if (Basketball.this.game instanceof BasketballGame bbGame) {
                                bbGame.clearInboundStateIfNotInbounder(defender);
                            }
                            
                            setLocked(false);
                            currentTrackPassTarget = null;
                            directionLockUntil.remove(finalReceiver.getUniqueId());
                            lockedDirection.remove(finalReceiver.getUniqueId());
                            cancel();
                            return;
                        }
                    }

                    // Determine ball location
                    if (!ballReachedEnd && index < bezierPoints.size()) {
                        // Still on the bezier curve
                        ballLoc = bezierPoints.get(index);
                        index += 2;
                    } else {
                        // Bezier curve ended - ball continues in straight line toward intercept point
                        if (!ballReachedEnd) {
                            ballReachedEnd = true;
                            System.out.println("Bezier curve ended at tick " + ticksInFlight + ", continuing toward intercept...");
                        }

                        // Calculate direction from last bezier point to intercept point
                        Location lastBezierPoint = bezierPoints.get(bezierPoints.size() - 1);
                        Vector continueDirection = finalInterceptPoint.toVector()
                                .subtract(lastBezierPoint.toVector()).normalize();

                        // Move ball forward with reduced speed for better control (0.8 blocks/tick instead of 1.0)
                        double ticksSinceBezierEnd = ticksInFlight - (bezierPoints.size() / 2);
                        double distanceTraveled = 0.8 * ticksSinceBezierEnd;
                        Vector movement = continueDirection.clone().multiply(distanceTraveled);
                        ballLoc = lastBezierPoint.clone().add(movement);
                        
                        // Safety check: if we've passed the intercept point by more than 3 blocks, stop
                        double distanceToIntercept = ballLoc.distance(finalInterceptPoint);
                        if (distanceToIntercept > 3.0) {
                            // Check if we're past the intercept point (dot product check)
                            Vector ballToIntercept = finalInterceptPoint.toVector().subtract(ballLoc.toVector());
                            double dotProduct = ballToIntercept.normalize().dot(continueDirection);
                            
                            // If dot product is negative, we've passed the point and are moving away
                            if (dotProduct < 0) {
                                System.out.println("Ball passed intercept point by " + String.format("%.1f", distanceToIntercept) + " blocks, stopping");
                                ballLoc = finalInterceptPoint.clone();
                                setVelocity(new Vector(0, 0, 0));
                                endPhysics(ballLoc);
                                setLocked(false);
                                currentTrackPassTarget = null;
                                directionLockUntil.remove(finalReceiver.getUniqueId());
                                lockedDirection.remove(finalReceiver.getUniqueId());
                                cancel();
                                return;
                            }
                        }

                        // Check if ball went BELOW the actual floor surface - this means it went through
                        double floorY = game.getArenaBox().getMinY();
                        Block blockBelow = ballLoc.getBlock().getRelative(BlockFace.DOWN);
                        if (!blockBelow.getType().isAir()) {
                            floorY = blockBelow.getY() + 1.0; // Surface of the solid block below
                        }
                        
                        if (ballLoc.getY() < floorY) {
                            System.out.println("Track pass went through floor at Y=" + ballLoc.getY() + " (floor=" + floorY + "), dropping ball");
                            ballLoc.setY(floorY + 0.1);
                            setVelocity(new Vector(0, 0, 0));
                            endPhysics(ballLoc);
                            setLocked(false);
                            currentTrackPassTarget = null;
                            directionLockUntil.remove(finalReceiver.getUniqueId());
                            lockedDirection.remove(finalReceiver.getUniqueId());
                            cancel();
                            return;
                        }
                    }

                    // Check if we've exceeded maximum flight time (15 seconds = 300 ticks)
                    if (ticksInFlight > 300) {
                        System.out.println("Track pass exceeded max flight time, ending");
                        setLocked(false);
                        currentTrackPassTarget = null;
                        directionLockUntil.remove(finalReceiver.getUniqueId());
                        lockedDirection.remove(finalReceiver.getUniqueId());
                        cancel();
                        return;
                    }

                    setVelocity(new Vector());
                    endPhysics(ballLoc);
                }

                private Location getBallLocation() {
                    return Basketball.this.getLocation();
                }
            }.runTaskTimer(Partix.getInstance(), 1L, 1L);

            this.game.startAssistTimer(passer.getUniqueId());
            if (this.game.getState().equals(GoalGame.State.REGULATION) ||
                    this.game.getState().equals(GoalGame.State.OVERTIME)) {
                this.game.getStatsManager().getPlayerStats(passer.getUniqueId()).incrementPassAttempts();
            }

            this.wasShot = false;

            // NEW: Call onInboundPass() if inbounder just threw the track pass
            if (this.game.inboundingActive && passer.equals(this.game.inbounder)) {
                this.game.onInboundPass();
                System.out.println("Inbound track pass detected - starting 1 second OOB grace period");
            }

            this.giveaway();
            this.threeEligible = false;
            this.delay = 5;

            return true;
        }

        return false;
    }
    public Location getTargetHoop(Player player) {
        if (player.getLocation().clone().add(player.getLocation().getDirection().multiply(1.0)).getZ() < this.game.getCenter().getZ()) {
            return this.game.getAwayNet().clone().getCenter().toLocation(player.getWorld()).clone();
        }
        return this.game.getHomeNet().clone().getCenter().toLocation(player.getWorld()).clone();
    }

    private void executeThrow(Player player) {
        // Disable press prevention when ball is thrown (pass or shot)
        if (this.game instanceof BasketballGame bbGame) {
            bbGame.pressPrevention = false;
            bbGame.restrictedTeam = null;
        }

        // Reset layup scoring flag on new shot attempt
        this.layupScoreDetected = false;
        this.isLayupAttempt = false;
        this.shouldPreventScore = false;
        this.layupScored = false;
        this.isBlockedShot = false; // Reset blocked shot flag
        this.currentShotMissType = MissType.NONE; // Reset miss type
        this.wasShot = true; // ← ADD THIS: Mark this as a shot

        float pitch = Math.min(145.0f, Math.max(90.0f, 90.0f + Math.abs(player.getLocation().getPitch()))) - 90.0f;
        this.setLocation(player.getEyeLocation().add(player.getLocation().getDirection().multiply(1.425)));
        Location th = player.getLocation().clone();

        // Use contest calculated at meter start for jump shots, calculate fresh for dunks
        if (this.isDunkAttempt) {
            // For dunks, calculate contest at release (old behavior)
            this.shotContestPercentage = calculateContest(player);
        } else {
            // For jump shots, use the contest from when the meter started
            this.shotContestPercentage = this.meterStartContestPercentage;
            System.out.println("Using meter-start contest for jump shot: " + String.format("%.1f%%", this.shotContestPercentage * 100));
        }
        final GoalGame.Team opponentTeam = this.game.getTeamOf(player) == GoalGame.Team.HOME
                ? GoalGame.Team.AWAY
                : GoalGame.Team.HOME;
        final Location targetHoop = opponentTeam == GoalGame.Team.HOME
                ? this.game.getHomeNet().getCenter().toLocation(player.getWorld())
                : this.game.getAwayNet().getCenter().toLocation(player.getWorld());

        double distanceToHoop = player.getLocation().distance(targetHoop);

        ShotType shotType = determineShotType(player, distanceToHoop);
        double greenThreshold = shotType.getGreenThreshold();
        boolean isMovingShot = shotType.isMovingShot();
        updateGreenWindow(distanceToHoop);

        final Block centerBlock = game.getCenter().getBlock();
        final Location playerLoc = player.getLocation().clone();

        final int yBelow = centerBlock.getLocation().clone().subtract(0, 3, 0).getBlockY();
        final Block blockBelow = centerBlock.getWorld().getBlockAt(playerLoc.getBlockX(), yBelow, playerLoc.getBlockZ());
        this.threeEligible = !blockBelow.getType().isAir();

        for (double y = 0.5; y < 4.0; y += 0.5) {
            Location checkLoc = player.getLocation().subtract(0.0, y, 0.0);
            Block block = checkLoc.getBlock();
            Material material = block.getType();
            if (!material.isSolid() || material.isAir()) continue;

            block.setType(this.threeEligible ? Material.RED_CONCRETE : Material.YELLOW_CONCRETE);
            Bukkit.getScheduler().runTaskLater(Partix.getInstance(), () -> block.setType(material), 22L);
            break;
        }

        // Check if this is a layup attempt
        this.isLayupAttempt = this.isLayupRange(player);

        // Direction check for JUMP SHOTS ONLY (not layups/dunks)
        if (!this.isLayupAttempt) {
            Vector toHoop = targetHoop.toVector().subtract(playerLoc.toVector());
            toHoop.setY(0);
            toHoop.normalize();
            
            Vector playerDirection = playerLoc.getDirection().clone();
            playerDirection.setY(0);
            playerDirection.normalize();

            double dotProduct = playerDirection.dot(toHoop);

            // STRICT: Must be looking toward the hoop (0.9703 = ~14 degrees)
            if (dotProduct < 0.9703) {
                player.sendMessage(Component.text("You must face the hoop to shoot!").color(Colour.deny()));
                this.forceThrow();
                return;
            }
            
            // NEW: Prevent looking down to avoid contest - must be looking up enough to see the rim
            float shootPitch = player.getLocation().getPitch();
            // Pitch: -90 = straight up, 0 = horizon, 90 = straight down
            // Require pitch between -70 (almost straight up) and 0 (horizon) - must look at or above horizon
            if (shootPitch > 0 || shootPitch < -70) {
                player.sendMessage(Component.text("You must look at the rim to shoot!").color(Colour.deny()));
                this.forceThrow();
                return;
            }
        }

        float yaw = player.getLocation().getYaw();

        // CAPTURE SHOT DISTANCE AT TIME OF SHOT
        this.shotDistance = player.getLocation().distance(targetHoop);

        // ========== NEW SHOT FORMULA ==========
        boolean perfect = false;
        this.guaranteedMiss = false;
        this.shouldPreventScore = false;

        // Use the new formula to calculate shot success
        boolean shotMade = calculateShotMake(
            this.accuracy,                    // Meter accuracy (0-8)
            this.shotContestPercentage,       // Contest value (0.0-1.0)
            this.shotDistance,                // Distance to hoop
            this.isLayupAttempt,              // Is this a layup?
            isMovingShot                      // Is player moving?
        );

        if (shotMade) {
            perfect = true;
        } else {
            this.guaranteedMiss = true;
            this.shouldPreventScore = true;
            
            // Determine miss type for feedback
            if (this.shotContestPercentage >= 0.90) {
                this.currentShotMissType = MissType.SMOTHERED;
                player.sendMessage(Component.text("SMOTHERED DEFENSE!").color(Colour.deny()).decorate(TextDecoration.BOLD));
            } else if (this.shotContestPercentage >= 0.65) {
                this.currentShotMissType = MissType.CONTESTED;
                player.sendMessage(Component.text("HEAVILY CONTESTED!").color(Colour.deny()).decorate(TextDecoration.BOLD));
            } else if (this.shotContestPercentage >= 0.45) {
                this.currentShotMissType = MissType.CONTESTED;
                player.sendMessage(Component.text("CONTESTED!").color(Colour.deny()));
            } else if (distanceToHoop > 27) {
                this.currentShotMissType = MissType.TOO_DEEP;
                player.sendMessage(Component.text("Too deep!").color(Colour.deny()));
            } else if (distanceToHoop > 24) {
                this.currentShotMissType = MissType.DEEP_SHOT;
                player.sendMessage(Component.text("Deep Shot").color(Colour.deny()));
            } else if (this.isLayupAttempt) {
                this.currentShotMissType = this.accuracy >= 1 ? MissType.YELLOW_LAYUP : MissType.RED_LAYUP;
            } else {
                this.currentShotMissType = this.accuracy >= 4 ? MissType.YELLOW_SHOT : MissType.RED_SHOT;
            }
        }

        final String percentage = String.format("%.2f", this.shotContestPercentage * 100.0f);
        Component contestDisplay = MiniMessage.miniMessage().deserialize("<green>" + percentage + "% Contested");

        for (Player p : this.game.getPlayers()) {
            p.showTitle(Title.title(
                    Component.empty(),
                    contestDisplay,
                    Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ofMillis(350L))
            ));
        }

        if (perfect) {
            this.perfectShot = true;
            this.perfectShotStartTime = System.currentTimeMillis();

            // NEW: Play green sound immediately on perfect release (only in regulation/overtime)
            if (this.game.getState().equals(GoalGame.State.REGULATION) ||
                    this.game.getState().equals(GoalGame.State.OVERTIME)) {

                Athlete athlete = AthleteManager.get(player.getUniqueId());
                CosmeticSound greenSound = athlete != null ? athlete.getGreenSound() : CosmeticSound.NO_SOUND;

                if (greenSound != CosmeticSound.NO_SOUND &&
                        greenSound.getSoundIdentifier() != null &&
                        !greenSound.getSoundIdentifier().isEmpty()) {
                    player.getWorld().playSound(player.getLocation(),
                            greenSound.getSoundIdentifier(),
                            SoundCategory.PLAYERS, 3.5f, 1.0f);
                }
            }
        } else {
            this.perfectShot = false;
        }

        this.game.onShotAttempt(player, this.threeEligible);

        if (shotType != ShotType.LAYUP) {
            String shotTypeText = shotType.getDisplayName();
            NamedTextColor color = isMovingShot ? NamedTextColor.YELLOW : NamedTextColor.GREEN;
            player.sendMessage(Component.text(shotTypeText).color(color));
        }

        // ========== LAYUP HANDLING ==========
        if (this.isLayupAttempt) {
            player.sendMessage(Component.text("Layup!").color(Colour.allow()));

            if (this.game.getState().equals(GoalGame.State.REGULATION) ||
                    this.game.getState().equals(GoalGame.State.OVERTIME)) {
                PlayerStats stats = this.game.getStatsManager().getPlayerStats(player.getUniqueId());
                stats.incrementFGAttempted();

                // NEW: Play green sound on perfect layup release
                if (this.perfectShot) {
                    Athlete athlete = AthleteManager.get(player.getUniqueId());
                    CosmeticSound greenSound = athlete != null ? athlete.getGreenSound() : CosmeticSound.NO_SOUND;

                    if (greenSound != CosmeticSound.NO_SOUND &&
                            greenSound.getSoundIdentifier() != null &&
                            !greenSound.getSoundIdentifier().isEmpty()) {
                        player.getWorld().playSound(player.getLocation(),
                                greenSound.getSoundIdentifier(),
                                SoundCategory.PLAYERS, 3.5f, 1.0f);
                    }
                }
            }

            Location loc1 = playerLoc.clone();
            Location backboardLoc = getBackboardLocation(player);

            double midX = (loc1.getX() + backboardLoc.getX()) / 2;
            double midY = ((loc1.getY() + backboardLoc.getY()) / 2) + 3.5;
            double midZ = (loc1.getZ() + backboardLoc.getZ()) / 2;

            final Location p1 = new Location(loc1.getWorld(), midX, midY, midZ);
            final List<Location> bezierPoints = bezierCurve(35, loc1, p1, backboardLoc);

            setLocked(true);
            new BukkitRunnable() {
                int index = 0;
                int stuckTicks = 0;
                Location lastLocation = null;
                boolean scored = false;

                @Override
                public void run() {
                    if (index >= bezierPoints.size() || !isValid()) {
                        setLocked(false);
                        if (!scored) {
                            setReboundEligible(true);
                        }
                        cancel();
                        return;
                    }

                    Location currentLoc = getLocation();
                    if (lastLocation != null && currentLoc.distance(lastLocation) < 0.1) {
                        stuckTicks++;
                        if (stuckTicks > 10) {
                            setLocked(false);
                            setVelocity(new Vector(0, -0.3, 0));
                            cancel();
                            return;
                        }
                    } else {
                        stuckTicks = 0;
                    }

                    int amountToIncrement = Math.max(2, (index / 20) + 1);

                    setVelocity(new Vector());
                    Location nextLoc = bezierPoints.get(index);
                    endPhysics(nextLoc);

                    if (!scored && isBallNearGoal(getTargetHoop(player))) {
                        double distToHoop = nextLoc.distance(getTargetHoop(player));

                        if (distToHoop < 0.6 && nextLoc.getY() <= getTargetHoop(player).getY()) {
                            if (perfectShot && !shouldPreventScore) {
                                scored = true;
                                layupScored = true;
                                GoalGame.Team scorerTeam = game.getHomePlayers().contains(player) ? GoalGame.Team.HOME : GoalGame.Team.AWAY;
                                game.goal(scorerTeam);
                                remove();
                                cancel();
                                return;
                            }
                        }
                    }

                    lastLocation = currentLoc.clone();
                    index += amountToIncrement;
                }
            }.runTaskTimer(Partix.getInstance(), 1L, 1L);

            this.lastOwnerUUID = player.getUniqueId();

        }
        else if (this.perfectShot || this.guaranteedMiss) {
            if (this.perfectShot) {
                player.sendMessage("Perfect shot");
            }

            Location loc1 = playerLoc.clone();
            Location loc2 = targetHoop.clone();

            // FIXED: For perfect shots, aim 1 block deeper into the hoop
            if (this.perfectShot) {
                Vector toHoop = loc2.toVector().subtract(loc1.toVector()).normalize();
                loc2.add(toHoop.multiply(1.0));
            }

            loc2.setY(loc2.getY() - 0.5);

            Vector shotVector = loc2.toVector().subtract(loc1.toVector());
            double horizontalDist = Math.sqrt(shotVector.getX() * shotVector.getX() + shotVector.getZ() * shotVector.getZ());

            // FIX: Pull mid-range shots 0.75 blocks shorter
            if (horizontalDist > 8 && horizontalDist <= 16) {
                Vector toHoop = loc2.toVector().subtract(loc1.toVector()).normalize();
                loc2.subtract(toHoop.multiply(0.75));
            }

            // Calculate arc height
            double arcHeight;
            if (horizontalDist <= 15) {
                arcHeight = 7.0 + (horizontalDist / 20.0);
            } else if (horizontalDist <= 20) {
                arcHeight = 8.0 + ((horizontalDist - 15.0) / 3.5);
            } else if (horizontalDist <= 24) {
                arcHeight = 10.5 + ((horizontalDist - 20.0) / 2.0);
            } else {
                arcHeight = 12.5 + ((horizontalDist - 24.0) / 1.5);
            }

            // Calculate arc offset ratio
            double arcOffsetRatio;
            if (horizontalDist <= 15) {
                arcOffsetRatio = 0.55;
            } else if (horizontalDist <= 20) {
                arcOffsetRatio = 0.60;
            } else if (horizontalDist <= 24) {
                arcOffsetRatio = 0.65;
            } else {
                arcOffsetRatio = 0.70;
            }

            double midX = loc1.getX() + (loc2.getX() - loc1.getX()) * arcOffsetRatio;
            double midY = Math.max(loc1.getY(), loc2.getY()) + arcHeight;
            double midZ = loc1.getZ() + (loc2.getZ() - loc1.getZ()) * arcOffsetRatio;

            final Location p1 = new Location(loc1.getWorld(), midX, midY, midZ);

            // Check backboard interference
            if (doesPathIntersectBackboard(loc1, targetHoop, player)) {
                Vector awayFromBackboard = targetHoop.toVector().subtract(loc1.toVector()).normalize();
                awayFromBackboard.setX(awayFromBackboard.getX() + (Math.random() - 0.5) * 0.3);
                awayFromBackboard.setZ(awayFromBackboard.getZ() + (Math.random() - 0.5) * 0.3);
                awayFromBackboard.setY(0.2);
                this.setVelocity(awayFromBackboard.multiply(0.5));
                this.lastOwnerUUID = player.getUniqueId();
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 3, true, false));
                this.delay = 10;
                this.setStealDelay(0);
                this.giveaway();
                return;
            }

            Location missTarget = calculateRealisticMissTarget(targetHoop, loc1, this.currentShotMissType, horizontalDist);
            Location finalTarget = this.guaranteedMiss ? missTarget : loc2;

            final List<Location> bezierPoints = bezierCurve(60, loc1, p1, finalTarget);

            setLocked(true);
            new BukkitRunnable() {
                int index = 0;
                Location previousLoc = null;
                boolean hasHitRim = false;

                @Override
                public void run() {
                    if (index >= bezierPoints.size() || !isValid()) {
                        setLocked(false);
                        cancel();
                        return;
                    }

                    Location nextLoc = bezierPoints.get(index);

                    // ===== ADD THIS: Check backboard collision FIRST (only for misses, not perfect shots) =====
                    if (previousLoc != null && guaranteedMiss) {
                        Location backboard = getBackboardLocation(player);
                        if (backboard != null) {
                            // Check if ball is getting close to backboard
                            double distToBackboard = nextLoc.distance(backboard);

                            if (distToBackboard < 1.5) {
                                // Ball is near backboard - check if it's going through it
                                Vector ballDirection = nextLoc.toVector().subtract(previousLoc.toVector()).normalize();
                                Vector toBackboard = backboard.toVector().subtract(nextLoc.toVector()).normalize();

                                double dotProduct = ballDirection.dot(toBackboard);

                                // If ball is moving TOWARD backboard (dot product > 0.5)
                                if (dotProduct > 0.5) {
                                    System.out.println("!!! BALL HEADING TOWARD BACKBOARD - BOUNCING OFF !!!");

                                    setLocked(false);

                                    // Calculate bounce AWAY from backboard
                                    Vector awayFromBackboard = nextLoc.toVector().subtract(backboard.toVector()).normalize();
                                    awayFromBackboard.setY(0);
                                    awayFromBackboard.normalize();

                                    // Move ball to safe position (in front of backboard)
                                    Location safeLoc = backboard.clone().add(awayFromBackboard.multiply(1.0));
                                    safeLoc.setY(nextLoc.getY());
                                    setLocation(safeLoc);

                                    // Apply bounce velocity
                                    Vector bounceVelocity = awayFromBackboard.clone().multiply(0.35);
                                    bounceVelocity.setY(-0.2);
                                    setVelocity(bounceVelocity);

                                    getLocation().getWorld().playSound(getLocation(),
                                            Sound.BLOCK_WOOD_HIT, SoundCategory.MASTER, 0.8f, 0.9f);

                                    System.out.println("Ball bounced off backboard at: " + safeLoc);
                                    cancel();
                                    return;
                                }
                            }
                        }
                    }

                    int amountToIncrement = Math.max(3, (index / 30) + 1);

                    if (guaranteedMiss && !hasHitRim) {
                        double distToMissTarget = nextLoc.distance(missTarget);
                        double distToActualHoop = nextLoc.distance(targetHoop);

                        // Debug: Print distance for deep shots
                        if (currentShotMissType == MissType.DEEP_SHOT || currentShotMissType == MissType.TOO_DEEP) {
                            System.out.println("Deep shot check - distToHoop: " + distToActualHoop + ", distToMissTarget: " + distToMissTarget);
                        }

                        // ===== INCREASED TO 2.0 - Catch deep 3s =====
                        if (distToMissTarget < 2.0 || distToActualHoop < 2.0) {
                            hasHitRim = true;
                            setLocked(false);

                            // ALL MISSES HIT RIM - Apply bounce physics
                            Vector bounceDir = calculateRimBounce(nextLoc, targetHoop, missTarget, currentShotMissType, loc1);

                            Vector awayFromRim = nextLoc.toVector().subtract(targetHoop.toVector()).normalize();
                            Location safeLoc = nextLoc.clone().add(awayFromRim.multiply(0.4));
                            setLocation(safeLoc);

                            setVelocity(bounceDir);

                            getLocation().getWorld().playSound(getLocation(),
                                    Sound.BLOCK_ANVIL_LAND, SoundCategory.MASTER, 0.6f, 1.2f);

                            System.out.println("Miss hit rim - bounce velocity: " + bounceDir);

                            cancel();
                            return;
                        }
                    }

                    setVelocity(new Vector());
                    endPhysics(nextLoc);
                    previousLoc = nextLoc.clone();
                    index += amountToIncrement;
                }
            }.runTaskTimer(Partix.getInstance(), 1L, 1L);
        }
        // ========== NON-PERFECT SHOT (contested threshold met or off-dribble red) ==========
        else {
            if (this.shotContestPercentage >= greenThreshold) {
                perfect = false;
                this.guaranteedMiss = true;
                this.shouldPreventScore = true;

                if (this.shotContestPercentage >= 0.65) {
                    this.currentShotMissType = MissType.CONTESTED;
                } else {
                    this.currentShotMissType = MissType.YELLOW_SHOT;
                }

                Location loc1 = playerLoc.clone();
                Location loc2 = targetHoop.clone();
                loc2.setY(loc2.getY() - 0.5);

                Vector shotVector = loc2.toVector().subtract(loc1.toVector());
                double horizontalDist = Math.sqrt(shotVector.getX() * shotVector.getX() + shotVector.getZ() * shotVector.getZ());

                // FIX: Pull mid-range shots 0.75 blocks shorter
                if (horizontalDist > 8 && horizontalDist <= 16) {
                    Vector toHoop = loc2.toVector().subtract(loc1.toVector()).normalize();
                    loc2.subtract(toHoop.multiply(0.75));
                }

                double arcHeight;
                if (horizontalDist <= 15) {
                    arcHeight = 7.0 + (horizontalDist / 20.0);
                } else if (horizontalDist <= 20) {
                    arcHeight = 8.0 + ((horizontalDist - 15.0) / 3.5);
                } else if (horizontalDist <= 24) {
                    arcHeight = 10.5 + ((horizontalDist - 20.0) / 2.0);
                } else {
                    arcHeight = 12.5 + ((horizontalDist - 24.0) / 1.5);
                }

                double arcOffsetRatio;
                if (horizontalDist <= 15) {
                    arcOffsetRatio = 0.55;
                } else if (horizontalDist <= 20) {
                    arcOffsetRatio = 0.60;
                } else if (horizontalDist <= 24) {
                    arcOffsetRatio = 0.65;
                } else {
                    arcOffsetRatio = 0.70;
                }

                double midX = loc1.getX() + (loc2.getX() - loc1.getX()) * arcOffsetRatio;
                double midY = Math.max(loc1.getY(), loc2.getY()) + arcHeight;
                double midZ = loc1.getZ() + (loc2.getZ() - loc1.getZ()) * arcOffsetRatio;

                final Location p1 = new Location(loc1.getWorld(), midX, midY, midZ);

                Location missTarget = calculateRealisticMissTarget(targetHoop, loc1, this.currentShotMissType, horizontalDist);
                final List<Location> bezierPoints = bezierCurve(60, loc1, p1, missTarget);

                setLocked(true);
                new BukkitRunnable() {
                    int index = 0;
                    Location previousLoc = null;
                    boolean hasHitRim = false;

                    @Override
                    public void run() {
                        if (index >= bezierPoints.size() || !isValid()) {
                            setLocked(false);
                            cancel();
                            return;
                        }

                        Location nextLoc = bezierPoints.get(index);

                        // ===== CHECK BACKBOARD COLLISION DURING FLIGHT =====
// Replace your backboard collision check with this improved version:
                        if (previousLoc != null) {
                            Location backboard = getBackboardLocation(player);
                            if (backboard != null) {
                                // Check if ball is getting close to backboard
                                double distToBackboard = nextLoc.distance(backboard);

                                if (distToBackboard < 1.5) {
                                    // Ball is near backboard - check if it's going through it
                                    Vector ballDirection = nextLoc.toVector().subtract(previousLoc.toVector()).normalize();
                                    Vector toBackboard = backboard.toVector().subtract(nextLoc.toVector()).normalize();

                                    double dotProduct = ballDirection.dot(toBackboard);

                                    // If ball is moving TOWARD backboard (dot product > 0.5)
                                    if (dotProduct > 0.5) {
                                        System.out.println("!!! BALL HEADING TOWARD BACKBOARD - BOUNCING OFF !!!");

                                        setLocked(false);

                                        // Calculate bounce AWAY from backboard
                                        Vector awayFromBackboard = nextLoc.toVector().subtract(backboard.toVector()).normalize();
                                        awayFromBackboard.setY(0);
                                        awayFromBackboard.normalize();

                                        // Move ball to safe position (in front of backboard)
                                        Location safeLoc = backboard.clone().add(awayFromBackboard.multiply(1.0));
                                        safeLoc.setY(nextLoc.getY());
                                        setLocation(safeLoc);

                                        // Apply bounce velocity
                                        Vector bounceVelocity = awayFromBackboard.clone().multiply(0.35);
                                        bounceVelocity.setY(-0.2);
                                        setVelocity(bounceVelocity);

                                        getLocation().getWorld().playSound(getLocation(),
                                                Sound.BLOCK_WOOD_HIT, SoundCategory.MASTER, 0.8f, 0.9f);

                                        System.out.println("Ball bounced off backboard at: " + safeLoc);
                                        cancel();
                                        return;
                                    }
                                }
                            }
                        }

                        int amountToIncrement = Math.max(3, (index / 30) + 1);

                        if (!hasHitRim) {
                            double distToMissTarget = nextLoc.distance(missTarget);
                            double distToActualHoop = nextLoc.distance(targetHoop);

                            // Debug: Print distance for deep shots
                            if (currentShotMissType == MissType.DEEP_SHOT || currentShotMissType == MissType.TOO_DEEP) {
                                System.out.println("Deep shot check - distToHoop: " + distToActualHoop + ", distToMissTarget: " + distToMissTarget);
                            }

                            if (distToMissTarget < 2.0 || distToActualHoop < 2.0) {
                                hasHitRim = true;
                                setLocked(false);

                                // ALL MISSES HIT RIM - Apply bounce physics
                                Vector bounceDir = calculateRimBounce(nextLoc, targetHoop, missTarget, currentShotMissType, loc1);

                                Vector awayFromRim = nextLoc.toVector().subtract(targetHoop.toVector()).normalize();
                                Location safeLoc = nextLoc.clone().add(awayFromRim.multiply(0.4));
                                setLocation(safeLoc);

                                setVelocity(bounceDir);

                                getLocation().getWorld().playSound(getLocation(),
                                        Sound.BLOCK_ANVIL_LAND, SoundCategory.MASTER, 0.6f, 1.2f);

                                System.out.println("Miss hit rim - bounce velocity: " + bounceDir);

                                cancel();
                                return;
                            }
                        }

                        setVelocity(new Vector());
                        endPhysics(nextLoc);
                        previousLoc = nextLoc.clone();
                        index += amountToIncrement;
                    }
                }.runTaskTimer(Partix.getInstance(), 1L, 1L);
            } else {
                // Off-dribble red shot - guaranteed miss
                this.guaranteedMiss = true;
                this.shouldPreventScore = true;
                this.currentShotMissType = MissType.RED_SHOT;

                Location loc1 = playerLoc.clone();
                Location loc2 = targetHoop.clone();
                loc2.setY(loc2.getY() - 0.5);

                Vector shotVector = loc2.toVector().subtract(loc1.toVector());
                double horizontalDist = Math.sqrt(shotVector.getX() * shotVector.getX() + shotVector.getZ() * shotVector.getZ());

                // FIX: Pull mid-range shots 0.75 blocks shorter
                if (horizontalDist > 8 && horizontalDist <= 16) {
                    Vector toHoop = loc2.toVector().subtract(loc1.toVector()).normalize();
                    loc2.subtract(toHoop.multiply(0.75));
                }

                double arcHeight;
                if (horizontalDist <= 15) {
                    arcHeight = 7.0 + (horizontalDist / 20.0);
                } else if (horizontalDist <= 20) {
                    arcHeight = 8.0 + ((horizontalDist - 15.0) / 3.5);
                } else if (horizontalDist <= 24) {
                    arcHeight = 10.5 + ((horizontalDist - 20.0) / 2.0);
                } else {
                    arcHeight = 12.5 + ((horizontalDist - 24.0) / 1.5);
                }

                double arcOffsetRatio;
                if (horizontalDist <= 15) {
                    arcOffsetRatio = 0.55;
                } else if (horizontalDist <= 20) {
                    arcOffsetRatio = 0.60;
                } else if (horizontalDist <= 24) {
                    arcOffsetRatio = 0.65;
                } else {
                    arcOffsetRatio = 0.70;
                }

                double midX = loc1.getX() + (loc2.getX() - loc1.getX()) * arcOffsetRatio;
                double midY = Math.max(loc1.getY(), loc2.getY()) + arcHeight;
                double midZ = loc1.getZ() + (loc2.getZ() - loc1.getZ()) * arcOffsetRatio;

                final Location p1 = new Location(loc1.getWorld(), midX, midY, midZ);

                Location missTarget = calculateRealisticMissTarget(targetHoop, loc1, this.currentShotMissType, horizontalDist);
                final List<Location> bezierPoints = bezierCurve(60, loc1, p1, missTarget);

                setLocked(true);
                new BukkitRunnable() {
                    int index = 0;
                    Location previousLoc = null;
                    boolean hasHitRim = false;

                    @Override
                    public void run() {
                        if (index >= bezierPoints.size() || !isValid()) {
                            setLocked(false);
                            cancel();
                            return;
                        }

                        Location nextLoc = bezierPoints.get(index);

                        // ===== CHECK BACKBOARD COLLISION DURING FLIGHT =====
// Replace your backboard collision check with this improved version:
                        if (previousLoc != null) {
                            Location backboard = getBackboardLocation(player);
                            if (backboard != null) {
                                // Check if ball is getting close to backboard
                                double distToBackboard = nextLoc.distance(backboard);

                                if (distToBackboard < 1.5) {
                                    // Ball is near backboard - check if it's going through it
                                    Vector ballDirection = nextLoc.toVector().subtract(previousLoc.toVector()).normalize();
                                    Vector toBackboard = backboard.toVector().subtract(nextLoc.toVector()).normalize();

                                    double dotProduct = ballDirection.dot(toBackboard);

                                    // If ball is moving TOWARD backboard (dot product > 0.5)
                                    if (dotProduct > 0.5) {
                                        System.out.println("!!! BALL HEADING TOWARD BACKBOARD - BOUNCING OFF !!!");

                                        setLocked(false);

                                        // Calculate bounce AWAY from backboard
                                        Vector awayFromBackboard = nextLoc.toVector().subtract(backboard.toVector()).normalize();
                                        awayFromBackboard.setY(0);
                                        awayFromBackboard.normalize();

                                        // Move ball to safe position (in front of backboard)
                                        Location safeLoc = backboard.clone().add(awayFromBackboard.multiply(1.0));
                                        safeLoc.setY(nextLoc.getY());
                                        setLocation(safeLoc);

                                        // Apply bounce velocity
                                        Vector bounceVelocity = awayFromBackboard.clone().multiply(0.35);
                                        bounceVelocity.setY(-0.2);
                                        setVelocity(bounceVelocity);

                                        getLocation().getWorld().playSound(getLocation(),
                                                Sound.BLOCK_WOOD_HIT, SoundCategory.MASTER, 0.8f, 0.9f);

                                        System.out.println("Ball bounced off backboard at: " + safeLoc);
                                        cancel();
                                        return;
                                    }
                                }
                            }
                        }

                        int amountToIncrement = Math.max(3, (index / 30) + 1);

                        if (!hasHitRim) {
                            double distToMissTarget = nextLoc.distance(missTarget);
                            double distToActualHoop = nextLoc.distance(targetHoop);

                            // Debug: Print distance for deep shots
                            if (currentShotMissType == MissType.DEEP_SHOT || currentShotMissType == MissType.TOO_DEEP) {
                                System.out.println("Deep shot check - distToHoop: " + distToActualHoop + ", distToMissTarget: " + distToMissTarget);
                            }

                            if (distToMissTarget < 2.0 || distToActualHoop < 2.0) {
                                hasHitRim = true;
                                setLocked(false);

                                // ALL MISSES HIT RIM - Apply bounce physics
                                Vector bounceDir = calculateRimBounce(nextLoc, targetHoop, missTarget, currentShotMissType, loc1);

                                Vector awayFromRim = nextLoc.toVector().subtract(targetHoop.toVector()).normalize();
                                Location safeLoc = nextLoc.clone().add(awayFromRim.multiply(0.4));
                                setLocation(safeLoc);

                                setVelocity(bounceDir);

                                getLocation().getWorld().playSound(getLocation(),
                                        Sound.BLOCK_ANVIL_LAND, SoundCategory.MASTER, 0.6f, 1.2f);

                                System.out.println("Miss hit rim - bounce velocity: " + bounceDir);

                                cancel();
                                return;
                            }
                        }

                        setVelocity(new Vector());
                        endPhysics(nextLoc);
                        previousLoc = nextLoc.clone();
                        index += amountToIncrement;
                    }
                }.runTaskTimer(Partix.getInstance(), 1L, 1L);

                System.out.println("Off-dribble shot using bezier miss animation");
            }
        }

        this.lastOwnerUUID = player.getUniqueId();
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 3, true, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 30, 128, true, false));
        this.delay = 10;
        this.setStealDelay(0);
        this.giveaway();
    }


    private Location bezierPoint(float t, Location p0, Location p1, Location p2) {
        final float a = (1-t)*(1-t);
        final float b = 2*(1-t)*t;
        final float c = t*t;

        return p0.clone().multiply(a).add(p1.clone().multiply(b)).add(p2.clone().multiply(c));
    }

    private List<Location> bezierCurve(int segmentCount, Location p0, Location p1, Location p2) {
        List<Location> points = new ArrayList<>();
        for (int i = 1; i < segmentCount; i++) {
            float t = i / (float) segmentCount;
            points.add(bezierPoint(t, p0, p1, p2));
        }

        return points;
    }

    private enum MissType {
        NONE,
        SMOTHERED,      // 90%+ contest
        CONTESTED,      // 65-90% or 45-65% contest
        YELLOW_LAYUP,   // Yellow window layup
        RED_LAYUP,      // Red window layup
        YELLOW_SHOT,    // Yellow window mid/three
        RED_SHOT,       // Red window mid/three
        DEEP_SHOT,      // 24-27 blocks
        TOO_DEEP,       // 27+ blocks
        SHORT           // Didn't reach rim
    }

    // Add this instance variable with your other fields:
    private MissType currentShotMissType = MissType.NONE;

    private Location calculateRealisticMissTarget(Location hoop, Location shooterLoc, MissType missType, double horizontalDist) {
        Random rand = new Random();
        Location missTarget = hoop.clone();

        Vector shooterToHoop = hoop.toVector().subtract(shooterLoc.toVector()).normalize();
        Vector perpendicular = new Vector(-shooterToHoop.getZ(), 0, shooterToHoop.getX()).normalize();

        switch (missType) {
            case SMOTHERED:
                // Heavily smothered - weak shot but still reaches rim (front or sides only)
                double smotheredRoll = rand.nextDouble();
                if (smotheredRoll < 0.4) {
                    // Front rim (weak)
                    missTarget.add(shooterToHoop.clone().multiply(-0.55));
                    missTarget.setY(hoop.getY() + 0.05);
                } else if (smotheredRoll < 0.7) {
                    // Left side rim (very weak angle)
                    missTarget.add(perpendicular.clone().multiply(-0.75));
                    missTarget.add(shooterToHoop.clone().multiply(-0.35));
                    missTarget.setY(hoop.getY() + 0.1);
                } else {
                    // Right side rim (very weak angle)
                    missTarget.add(perpendicular.clone().multiply(0.75));
                    missTarget.add(shooterToHoop.clone().multiply(-0.35));
                    missTarget.setY(hoop.getY() + 0.1);
                }
                break;

            case CONTESTED:
                // Heavily contested - hits rim but with poor accuracy (front or sides only)
                double contestedRoll = rand.nextDouble();
                if (contestedRoll < 0.33) {
                    // Front rim
                    missTarget.add(shooterToHoop.clone().multiply(-0.55));
                    missTarget.setY(hoop.getY() + 0.1);
                } else if (contestedRoll < 0.66) {
                    // Left side rim
                    missTarget.add(perpendicular.clone().multiply(-0.68));
                    missTarget.add(shooterToHoop.clone().multiply(-0.25));
                    missTarget.setY(hoop.getY() + 0.2);
                } else {
                    // Right side rim
                    missTarget.add(perpendicular.clone().multiply(0.68));
                    missTarget.add(shooterToHoop.clone().multiply(-0.25));
                    missTarget.setY(hoop.getY() + 0.2);
                }
                break;

            case YELLOW_LAYUP:
                // Decent layup - hits rim softly (left, right, or front - NO BACK)
                double yellowLayupRoll = rand.nextDouble();
                if (yellowLayupRoll < 0.33) {
                    // Left rim
                    missTarget.add(perpendicular.clone().multiply(-0.75));
                    missTarget.setY(hoop.getY() + 0.1);
                } else if (yellowLayupRoll < 0.66) {
                    // Right rim
                    missTarget.add(perpendicular.clone().multiply(0.75));
                    missTarget.setY(hoop.getY() + 0.1);
                } else {
                    // Front rim
                    missTarget.add(shooterToHoop.clone().multiply(-0.55));
                    missTarget.setY(hoop.getY() + 0.05);
                }
                break;

            case YELLOW_SHOT:
                // Decent shot - hits rim (left, right, or front - NO BACK)
                double yellowRoll = rand.nextDouble();
                if (yellowRoll < 0.33) {
                    // Left rim
                    missTarget.add(perpendicular.clone().multiply(-0.58));
                    missTarget.setY(hoop.getY() + 0.2);
                } else if (yellowRoll < 0.66) {
                    // Right rim
                    missTarget.add(perpendicular.clone().multiply(0.58));
                    missTarget.setY(hoop.getY() + 0.2);
                } else {
                    // Front rim
                    missTarget.add(shooterToHoop.clone().multiply(-0.48));
                    missTarget.setY(hoop.getY() + 0.15);
                }
                break;

            case RED_LAYUP:
                // Poor layup - more erratic rim hits (front or sides only - NO BACK)
                double redLayupRoll = rand.nextDouble();
                if (redLayupRoll < 0.33) {
                    // Way left
                    missTarget.add(perpendicular.clone().multiply(-0.95));
                    missTarget.setY(hoop.getY() + 0.15);
                } else if (redLayupRoll < 0.66) {
                    // Way right
                    missTarget.add(perpendicular.clone().multiply(0.95));
                    missTarget.setY(hoop.getY() + 0.15);
                } else {
                    // Front rim
                    missTarget.add(shooterToHoop.clone().multiply(-0.75));
                    missTarget.setY(hoop.getY() + 0.1);
                }
                break;

            case RED_SHOT:
                // Poor shot - more erratic rim hits (front or sides only - NO BACK)
                double redRoll = rand.nextDouble();
                if (redRoll < 0.5) {
                    // Way left rim
                    missTarget.add(perpendicular.clone().multiply(-0.75));
                    missTarget.setY(hoop.getY() + (rand.nextDouble() * 0.3));
                } else if (redRoll < 1.0) {
                    // Way right rim
                    missTarget.add(perpendicular.clone().multiply(0.75));
                    missTarget.setY(hoop.getY() + (rand.nextDouble() * 0.3));
                }
                // REMOVED: Front/back rim logic - now only sides
                break;

            case DEEP_SHOT:
                // 24-27 blocks - hits rim but more erratic (front or sides only - NO BACK)
                double deepRoll = rand.nextDouble();
                if (deepRoll < 0.4) {
                    // Front rim
                    missTarget.add(shooterToHoop.clone().multiply(-0.55));
                    missTarget.setY(hoop.getY() + 0.1);
                } else if (deepRoll < 0.7) {
                    // Left side rim
                    missTarget.add(perpendicular.clone().multiply(-0.68));
                    missTarget.add(shooterToHoop.clone().multiply(-0.25));
                    missTarget.setY(hoop.getY() + 0.15);
                } else {
                    // Right side rim
                    missTarget.add(perpendicular.clone().multiply(0.68));
                    missTarget.add(shooterToHoop.clone().multiply(-0.25));
                    missTarget.setY(hoop.getY() + 0.15);
                }
                break;

            case TOO_DEEP:
                // 27+ blocks - shot is too strong, still hits front/sides (front or sides only - NO BACK)
                double tooDeepRoll = rand.nextDouble();
                if (tooDeepRoll < 0.4) {
                    // Front rim (hard)
                    missTarget.add(shooterToHoop.clone().multiply(-0.48));
                    missTarget.setY(hoop.getY() + 0.25);
                } else if (tooDeepRoll < 0.7) {
                    // Left side rim (hard)
                    missTarget.add(perpendicular.clone().multiply(-0.68));
                    missTarget.add(shooterToHoop.clone().multiply(-0.25));
                    missTarget.setY(hoop.getY() + 0.2);
                } else {
                    // Right side rim (hard)
                    missTarget.add(perpendicular.clone().multiply(0.68));
                    missTarget.add(shooterToHoop.clone().multiply(-0.25));
                    missTarget.setY(hoop.getY() + 0.2);
                }
                break;

            case SHORT:
                // Didn't reach rim
                missTarget.add(shooterToHoop.clone().multiply(-2.5));
                missTarget.setY(hoop.getY() - 1.8);
                break;

            default:
                // Generic miss - random rim location (front or sides only - NO BACK)
                if (rand.nextBoolean()) {
                    // Side miss
                    missTarget.add(perpendicular.clone().multiply(rand.nextBoolean() ? 0.58 : -0.58));
                    missTarget.setY(hoop.getY() + 0.2);
                } else {
                    // Front miss
                    missTarget.add(shooterToHoop.clone().multiply(-0.55));
                    missTarget.setY(hoop.getY() + 0.2);
                }
                break;
        }

        return missTarget;
    }

    private Vector calculateRimBounce(Location ballLoc, Location hoop, Location missTarget, MissType missType, Location shooterLoc) {
        Random rand = new Random();
        Vector bounceDir = new Vector(0, 0, 0);

        Vector shooterToHoop = hoop.toVector().subtract(shooterLoc.toVector()).normalize();
        Vector ballToHoop = hoop.toVector().subtract(ballLoc.toVector());

        boolean isLayupMiss = (missType == MissType.YELLOW_LAYUP || missType == MissType.RED_LAYUP);
        
        // Random chance (30%) to bounce in a completely random direction for unpredictability
        boolean randomDirectionBounce = rand.nextDouble() < 0.30;

        switch (missType) {
            case SMOTHERED:
                bounceDir = shooterToHoop.clone().multiply(-0.25); // REDUCED from -0.4
                bounceDir.setX(bounceDir.getX() + (rand.nextDouble() - 0.5) * 0.15); // REDUCED from 0.2
                bounceDir.setZ(bounceDir.getZ() + (rand.nextDouble() - 0.5) * 0.15); // REDUCED from 0.2
                bounceDir.setY(0.18 + rand.nextDouble() * 0.08); // REDUCED from 0.25 + 0.12
                bounceDir.multiply(0.10); // REDUCED from 0.16
                break;

            case CONTESTED:
                bounceDir = shooterToHoop.clone().multiply(-0.35); // REDUCED from -0.5
                bounceDir.setX(bounceDir.getX() + (rand.nextDouble() - 0.5) * 0.15); // REDUCED from 0.2
                bounceDir.setZ(bounceDir.getZ() + (rand.nextDouble() - 0.5) * 0.15); // REDUCED from 0.2
                bounceDir.setY(0.20 + rand.nextDouble() * 0.08); // REDUCED from 0.28 + 0.12
                bounceDir.multiply(0.10); // REDUCED from 0.16
                break;

            case YELLOW_LAYUP:
                double distXLayup = missTarget.getX() - hoop.getX();
                double distZLayup = missTarget.getZ() - hoop.getZ();

                bounceDir.setX(distXLayup * 0.4 + (rand.nextDouble() - 0.5) * 0.15); // REDUCED from 0.6 + 0.2
                bounceDir.setZ(distZLayup * 0.4 + (rand.nextDouble() - 0.5) * 0.15); // REDUCED from 0.6 + 0.2
                bounceDir.setY(0.18 + rand.nextDouble() * 0.08); // REDUCED from 0.25 + 0.12
                bounceDir.multiply(0.10); // REDUCED from 0.16
                break;

            case YELLOW_SHOT:
                double distX = missTarget.getX() - hoop.getX();
                double distZ = missTarget.getZ() - hoop.getZ();

                bounceDir.setX(distX * 0.5 + (rand.nextDouble() - 0.5) * 0.15); // REDUCED from 0.7 + 0.2
                bounceDir.setZ(distZ * 0.5 + (rand.nextDouble() - 0.5) * 0.15); // REDUCED from 0.7 + 0.2
                bounceDir.setY(0.20 + rand.nextDouble() * 0.08); // REDUCED from 0.28 + 0.12
                bounceDir.multiply(0.10); // REDUCED from 0.16
                break;

            case RED_LAYUP:
                bounceDir = ballToHoop.clone().normalize();
                bounceDir.setX(bounceDir.getX() * -0.4 + (rand.nextDouble() - 0.5) * 0.2); // REDUCED from -0.6 + 0.3
                bounceDir.setZ(bounceDir.getZ() * -0.4 + (rand.nextDouble() - 0.5) * 0.2); // REDUCED from -0.6 + 0.3
                bounceDir.setY(0.18 + rand.nextDouble() * 0.08); // REDUCED from 0.25 + 0.12
                bounceDir.multiply(0.10); // REDUCED from 0.16
                break;

            case RED_SHOT:
                bounceDir = ballToHoop.clone().normalize();
                bounceDir.setX(bounceDir.getX() * -0.5 + (rand.nextDouble() - 0.5) * 0.2); // REDUCED from -0.7 + 0.3
                bounceDir.setZ(bounceDir.getZ() * -0.5 + (rand.nextDouble() - 0.5) * 0.2); // REDUCED from -0.7 + 0.3
                bounceDir.setY(0.20 + rand.nextDouble() * 0.10); // REDUCED from 0.28 + 0.14
                bounceDir.multiply(0.10); // REDUCED from 0.16
                break;

            case DEEP_SHOT:
                bounceDir = shooterToHoop.clone();
                bounceDir.setY(0);
                bounceDir.normalize();
                bounceDir.setX(bounceDir.getX() + (rand.nextDouble() - 0.5) * 0.3);
                bounceDir.setZ(bounceDir.getZ() + (rand.nextDouble() - 0.5) * 0.3);
                bounceDir.multiply(0.25); // INCREASED from 0.08 for stronger deep shot bounces
                bounceDir.setY(0.35 + rand.nextDouble() * 0.15); // INCREASED for more pop
                break;

            case TOO_DEEP:
                bounceDir = shooterToHoop.clone();
                bounceDir.setY(0);
                bounceDir.normalize();
                bounceDir.setX(bounceDir.getX() + (rand.nextDouble() - 0.5) * 0.35);
                bounceDir.setZ(bounceDir.getZ() + (rand.nextDouble() - 0.5) * 0.35);
                bounceDir.multiply(0.28); // INCREASED from 0.09 for stronger very deep shot bounces
                bounceDir.setY(0.40 + rand.nextDouble() * 0.15); // INCREASED for more pop
                break;

            case SHORT:
                bounceDir.setY(-0.3);
                bounceDir.setX((rand.nextDouble() - 0.5) * 0.15); // REDUCED from 0.2
                bounceDir.setZ((rand.nextDouble() - 0.5) * 0.15); // REDUCED from 0.2
                break;

            default:
                bounceDir = ballLoc.toVector().subtract(hoop.toVector()).normalize();
                bounceDir.setX(bounceDir.getX() + (rand.nextDouble() - 0.5) * 0.2); // REDUCED from 0.3
                bounceDir.setZ(bounceDir.getZ() + (rand.nextDouble() - 0.5) * 0.2); // REDUCED from 0.3
                bounceDir.setY(0.18 + rand.nextDouble() * 0.08); // REDUCED from 0.25 + 0.12
                bounceDir.multiply(0.10); // REDUCED from 0.16
                break;
        }

        // Random direction bounce - bounce anywhere EXCEPT towards backboard
        if (randomDirectionBounce) {
            // Calculate direction from hoop to shooter (away from backboard)
            Vector awayFromBackboard = shooterLoc.toVector().subtract(hoop.toVector());
            awayFromBackboard.setY(0);
            awayFromBackboard.normalize();
            
            // Get angle of the "away from backboard" direction
            double backboardAngle = Math.atan2(awayFromBackboard.getZ(), awayFromBackboard.getX());
            
            // Generate random angle in a 270-degree cone (avoiding 90 degrees toward backboard)
            // Randomly pick from the 270-degree range away from backboard
            double randomOffset = (rand.nextDouble() * 1.5 * Math.PI) - (0.75 * Math.PI); // -135 to +135 degrees
            double angle = backboardAngle + randomOffset;
            
            double horizontalSpeed = 0.08 + rand.nextDouble() * 0.12; // Random speed between 0.08 and 0.20
            
            bounceDir.setX(Math.cos(angle) * horizontalSpeed);
            bounceDir.setZ(Math.sin(angle) * horizontalSpeed);
            bounceDir.setY(0.15 + rand.nextDouble() * 0.15); // Random Y between 0.15 and 0.30
        } else {
            // Normal behavior - Ensure bounce is AWAY from rim center
            Vector awayFromRim = ballLoc.toVector().subtract(hoop.toVector());
            awayFromRim.setY(0);

            Vector horizontalBounce = bounceDir.clone();
            horizontalBounce.setY(0);

            if (horizontalBounce.dot(awayFromRim) < 0) {
                bounceDir.setX(-bounceDir.getX());
                bounceDir.setZ(-bounceDir.getZ());
            }

            // REDUCED minimum velocity for closer bounces
            double minVelocity = isLayupMiss ? 0.12 : 0.18; // REDUCED from 0.20 and 0.30
            if (bounceDir.length() < minVelocity) {
                bounceDir.normalize().multiply(minVelocity);
            }
        }
        
        // Add variance to bounce distance (multiply by random factor 0.7 to 1.3)
        double distanceVariance = 0.7 + rand.nextDouble() * 0.6;
        bounceDir.multiply(distanceVariance);

        return bounceDir;
    }

    private MissType determineMissType(int accuracy, double distance) {
        // Based on accuracy and distance, determine what kind of miss
        if (accuracy <= 2) {
            // Very poor timing - might be short
            return distance > 20 ? MissType.TOO_DEEP : MissType.RED_SHOT;
        } else if (accuracy <= 4) {
            // Poor timing
            return MissType.YELLOW_SHOT;
        } else {
            // Decent timing but still missed
            return MissType.YELLOW_SHOT;
        }
    }
    private Vector calculateRealisticMissBounce(Location ballLoc, Location rimLoc, Vector incomingVelocity, MissType missType, Location shooterLoc) {
        Random rand = new Random();

        // Normalize incoming velocity to understand shot direction
        Vector shotDirection = incomingVelocity.clone().normalize();
        Vector toShooter = shooterLoc.toVector().subtract(rimLoc.toVector()).normalize();
        toShooter.setY(0);

        Vector bounceVelocity;

        switch (missType) {
            case SMOTHERED:
            case CONTESTED:
                // Heavy contest - weak bounce, mostly drops
                bounceVelocity = toShooter.clone().multiply(0.25 + rand.nextDouble() * 0.15);
                bounceVelocity.setY(0.1 + rand.nextDouble() * 0.1);
                break;

            case YELLOW_SHOT:
            case RED_SHOT:
                // Poor release - determine front/back/side rim
                double rimHitType = rand.nextDouble();

                if (rimHitType < 0.4) {
                    // Front rim - bounce back
                    bounceVelocity = toShooter.clone().multiply(0.35 + rand.nextDouble() * 0.15);
                    bounceVelocity.setY(0.15 + rand.nextDouble() * 0.1);
                } else if (rimHitType < 0.7) {
                    // Side rim - bounce perpendicular
                    Vector perpendicular = new Vector(-toShooter.getZ(), 0, toShooter.getX()).normalize();
                    if (rand.nextBoolean()) perpendicular.multiply(-1);
                    bounceVelocity = perpendicular.multiply(0.3 + rand.nextDouble() * 0.1);
                    bounceVelocity.add(toShooter.clone().multiply(0.1)); // Slight back component
                    bounceVelocity.setY(0.12 + rand.nextDouble() * 0.08);
                } else {
                    // Back rim - bounce forward
                    bounceVelocity = toShooter.clone().multiply(-1).multiply(0.3 + rand.nextDouble() * 0.15);
                    bounceVelocity.setY(0.18 + rand.nextDouble() * 0.1);
                }
                break;

            case DEEP_SHOT:
            case TOO_DEEP:
                // Shot too long - usually back rim, bounce forward
                bounceVelocity = toShooter.clone().multiply(-1).multiply(0.4 + rand.nextDouble() * 0.2);
                bounceVelocity.setY(0.2 + rand.nextDouble() * 0.15);
                break;

            default:
                // Generic miss - moderate bounce back
                bounceVelocity = toShooter.clone().multiply(0.35 + rand.nextDouble() * 0.15);
                bounceVelocity.setY(0.15 + rand.nextDouble() * 0.12);
                break;
        }

        // Ensure minimum speed so ball doesn't get stuck
        if (bounceVelocity.length() < 0.3) {
            bounceVelocity.normalize().multiply(0.3);
        }

        return bounceVelocity;
    }
    public boolean attemptLayupBlock(Player blocker) {
        // Verify this is a layup attempt in flight
        if (!this.isLayupAttempt || this.getCurrentDamager() != null) {
            return false; // Ball must be in flight and be a layup
        }

        Player shooter = this.getLastDamager();
        if (shooter == null) {
            return false;
        }

        // Can't block your own layup
        if (blocker.equals(shooter)) {
            return false;
        }

        // Verify teams are different
        GoalGame.Team shooterTeam = this.game.getTeamOf(shooter);
        GoalGame.Team blockerTeam = this.game.getTeamOf(blocker);

        if (shooterTeam == null || blockerTeam == null ||
                shooterTeam.equals(blockerTeam) || blockerTeam == GoalGame.Team.SPECTATOR) {
            return false;
        }

        // Check distance to ball
        double distance = blocker.getLocation().distance(this.getLocation());
        if (distance > LAYUP_BLOCK_RADIUS) {
            blocker.sendMessage(Component.text("Too far to block!").color(Colour.deny()));
            return false;
        }


        // ===== HEIGHT CHECK: Blocker should be airborne for better block chance =====
        final double floorY = game.getArenaBox().getMinY() + 2.5;
        boolean isAirborne = blocker.getLocation().getY() > (floorY + 0.5);

        // Calculate height advantage
        double heightDiff = blocker.getLocation().getY() - this.getLocation().getY();

        // Block success calculation
        double blockChance;

        if (isAirborne) {
            // Airborne blocks have better chance
            if (heightDiff > -0.5 && heightDiff < 1.5) {
                // Perfect height - 85% success
                blockChance = 0.85;
            } else if (heightDiff >= 1.5) {
                // Jumped too high - 60% success
                blockChance = 0.60;
            } else {
                // Jumped too late - 50% success
                blockChance = 0.50;
            }
        } else {
            // Grounded blocks are harder
            blockChance = 0.35;
        }

        // Distance penalty - closer is better
        double distanceFactor = 1.0 - (distance / LAYUP_BLOCK_RADIUS) * 0.3;
        blockChance *= distanceFactor;

        // Roll for success
        if (Math.random() > blockChance) {
            // Block attempt FAILED
            blocker.sendMessage(Component.text("Block attempt missed!").color(Colour.deny()));
            blocker.playSound(blocker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_WEAK, SoundCategory.MASTER, 0.5f, 0.8f);
            return false;
        }

        // ===== BLOCK SUCCESSFUL! =====
        this.removeCurrentDamager();

        // CRITICAL FIX: Calculate block direction AWAY from backboard and hoop
        Location targetHoop = this.getTargetHoop(shooter);
        Location backboard = getBackboardLocation(shooter);

        // Direction away from hoop (opposite of shot direction)
        Vector awayFromHoop = this.getLocation().toVector().subtract(targetHoop.toVector()).normalize();

        // Also check backboard direction
        Vector awayFromBackboard = this.getLocation().toVector().subtract(backboard.toVector()).normalize();

        // Use blocker's direction as base, but ensure it's away from hoop/backboard
        Vector blockDirection = blocker.getLocation().getDirection().normalize();

        // If block direction is toward hoop or backboard, reverse it
        if (blockDirection.dot(awayFromHoop) < 0) {
            blockDirection = awayFromHoop.clone();
        }

        // Add some lateral randomness
        blockDirection.setX(blockDirection.getX() + (Math.random() - 0.5) * 0.3);
        blockDirection.setZ(blockDirection.getZ() + (Math.random() - 0.5) * 0.3);
        blockDirection.normalize();

        // Set upward angle
        blockDirection.setY(0.25);
        blockDirection.normalize();

        // Apply block velocity (stronger to get it away from backboard)
        this.setVelocity(blockDirection.multiply(0.55), 0.25);

        // Messages and sounds
        blocker.sendMessage(Component.text("LAYUP BLOCKED!").color(Colour.allow()).decorate(TextDecoration.BOLD));
        shooter.sendMessage(Component.text("Your layup was blocked by " + blocker.getName() + "!")
                .color(Colour.deny()).decorate(TextDecoration.BOLD));

        // Sound effects
        blocker.playSound(blocker.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, SoundCategory.MASTER, 1.0f, 1.2f);
        shooter.playSound(shooter.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, SoundCategory.MASTER, 1.0f, 0.8f);


        // Increment block stats
        if (this.game.getState().equals(GoalGame.State.REGULATION) ||
                this.game.getState().equals(GoalGame.State.OVERTIME)) {
            PlayerStats blockerStats = this.game.getStatsManager().getPlayerStats(blocker.getUniqueId());
            blockerStats.incrementBlocks();
        }

        // Mark ball as NOT rebound-eligible (for OOB)
        this.setReboundEligible(false);

        // Cancel layup attempt
        this.isLayupAttempt = false;
        this.isBlockedShot = true; // Mark as blocked to prevent backboard animation
        this.setLocked(false); // Unlock ball physics

        // Store blocker for OOB possession
        this.lastShotBlockerUUID = blocker.getUniqueId();

        // Track this block attempt
        layupBlockAttempts.put(blocker.getUniqueId(), System.currentTimeMillis());

        return true;
    }

    public void registerLayupBlockAttempt(Player blocker) {
        // Only register if layup is in flight
        if (!this.isLayupAttempt || this.getCurrentDamager() != null) {
            return;
        }

        // Check cooldown (prevent spam)
        Long lastAttempt = layupBlockAttempts.get(blocker.getUniqueId());
        if (lastAttempt != null) {
            long timeSince = System.currentTimeMillis() - lastAttempt;
            if (timeSince < 500L) { // 0.5 second cooldown
                return;
            }
        }

        // Attempt the block
        attemptLayupBlock(blocker);
    }

    public boolean attemptCollisionSteal(Player defender) {
        // Check if there's currently a ball handler
        if (this.getCurrentDamager() == null) {
            return false;
        }

        Player ballHandler = this.getCurrentDamager();

        // Can't steal from yourself
        if (defender.equals(ballHandler)) {
            return false;
        }

        // Verify teams are different
        GoalGame.Team defenderTeam = this.game.getTeamOf(defender);
        GoalGame.Team handlerTeam = this.game.getTeamOf(ballHandler);

        if (defenderTeam == null || handlerTeam == null || defenderTeam.equals(handlerTeam) ||
                defenderTeam == GoalGame.Team.SPECTATOR) {
            return false;
        }

        // CRITICAL: Don't allow collision steals from the inbounder during inbound sequences
        if (this.game.inboundingActive && ballHandler.equals(this.game.inbounder)) {
            return false;
        }

        // Check if defender is on the ground (collision steals only work on ground)
        if (!defender.isOnGround()) {
            return false;
        }

        // ===== FIX: CHECK GLOBAL STEAL COOLDOWN =====
        if (this.globalStealCooldown > 0) {
            return false;
        }

        // Check steal immunity on the ball handler
        if (this.stealImmunityTicks > 0) {
            return false;
        }

        // Check if there's a steal delay still active
        if (this.getStealDelay() > 0) {
            return false;
        }

        // Distance check - must be very close (collision range)
        double distance = defender.getLocation().distance(ballHandler.getLocation());
        if (distance > 2.0) {
            return false;
        }

        // Check for inbound immunity - prevent steals during inbound
        if (this.game instanceof BasketballGame bbGame && bbGame.isOutOfBoundsImmunity()) {
            System.out.println("Collision steal blocked - inbound immunity active");
            return false;
        }

        // ===== NEW: 1-SECOND STEAL PROTECTION AFTER PICKUP =====
        // Check if ball handler just picked up the ball (within 1 second)
        // BUT: Inbound protection overrides this, so we only apply it when NOT inbounding
        if (!this.game.inboundingActive) {
            UUID handlerId = ballHandler.getUniqueId();
            if (lastBallPickupTime.containsKey(handlerId)) {
                long pickupTime = lastBallPickupTime.get(handlerId);
                long timeSincePickup = System.currentTimeMillis() - pickupTime;
                
                if (timeSincePickup < 1000L) { // Less than 1 second
                    System.out.println("Collision steal blocked - " + ballHandler.getName() + 
                        " has steal protection for " + (1000 - timeSincePickup) + "ms more");
                    return false;
                }
            }
        }

        // ===== POKE STEAL LOGIC =====
        // ALWAYS POKE - 100% chance ball gets poked loose (no snatch)
        this.removeCurrentDamager();

        // Ball just drops straight down to the ground (no directional poke)
        setLocked(true);
        this.setVelocity(new Vector(0, 0, 0)); // Stop all movement

        Bukkit.getScheduler().runTaskLater(Partix.getInstance(), () -> {
            // After 0.3 seconds, unfreeze and let ball drop
            setLocked(false);
            this.setVelocity(new Vector(0, -0.1, 0));
        }, 6L);

        // Play sound effects
        ballHandler.playSound(ballHandler.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP,
                SoundCategory.MASTER, 100.0f, 0.8f);
        defender.playSound(defender.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP,
                SoundCategory.MASTER, 100.0f, 1.2f);

        // Apply slowness to ball handler
        ballHandler.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0));

        // Send messages
        defender.sendMessage(Component.text("Collision Steal!").color(Colour.allow())
                .decorate(TextDecoration.BOLD));
        ballHandler.sendMessage(Component.text("Ball poked!").color(Colour.deny()));

        // Track steal/turnover stats
        if ((this.game.getState().equals(GoalGame.State.REGULATION) ||
                this.game.getState().equals(GoalGame.State.OVERTIME)) &&
                this.game.getHomePlayers().contains(ballHandler) !=
                        this.game.getHomePlayers().contains(defender)) {
            PlayerStats defenderStats = this.game.getStatsManager().getPlayerStats(defender.getUniqueId());
            defenderStats.incrementSteals();
            PlayerStats handlerStats = this.game.getStatsManager().getPlayerStats(ballHandler.getUniqueId());
            handlerStats.incrementTurnovers();
        }

        // CRITICAL: Track poke data for OOB possession
        this.lastPossessionBeforePoke = ballHandler.getUniqueId();
        this.wasPoked = true;

        // Add 1-second pickup cooldown for the victim (collision immunity)
        long collisionCooldownExpiry = System.currentTimeMillis() + 1000L;
        pokeStealImmunity.put(ballHandler.getUniqueId(), collisionCooldownExpiry);
        
        // Add 1.5-second CLICK cooldown for the victim (can't pick up by clicking)
        long clickCooldownExpiry = System.currentTimeMillis() + 1500L;
        stolenFromClickCooldown.put(ballHandler.getUniqueId(), clickCooldownExpiry);
        stolenClickRollResult.remove(ballHandler.getUniqueId()); // Clear any previous roll
        
        // Add 3-second DASH lockout for the victim (can't use dash button)
        long dashLockoutExpiry = System.currentTimeMillis() + 3000L;
        stolenFromDashLockout.put(ballHandler.getUniqueId(), dashLockoutExpiry);
        
        System.out.println(ballHandler.getName() + " has 1.5-second CLICK cooldown and 3-second DASH lockout after poke steal");

        this.setStealDelay(10);
        this.delay = 10;
        this.accuracy = 0;
        this.threeEligible = false;

        // ===== FIX: SET GLOBAL STEAL COOLDOWN =====
        this.globalStealCooldown = 80; // 4 seconds (80 ticks)
        System.out.println("Collision steal successful - 4 second global cooldown activated");

        return true;
    }
    private void updateGreenWindow(double distance) {
        if (distance <= 8) { // Close Range
            this.distanceZone = DistanceZone.SHORT;
        } else if (distance <= 16) { // Mid Range
            this.distanceZone = DistanceZone.MEDIUM;
        } else { // Very Long Range
            this.distanceZone = DistanceZone.LONG;
        }
    }

    private double calculateContest(Player shooter) {
        if (game == null) return 0.0;
        GoalGame.Team shooterTeam = game.getTeamOf(shooter);
        if (shooterTeam == null) return 0.0;

        List<Double> defenderContests = new ArrayList<>();
        final double baseContestRadius = 6.0;

        for (Player onlinePlayer : shooter.getWorld().getPlayers()) {
            GoalGame.Team onlinePlayerTeam = game.getTeamOf(onlinePlayer);

            if (onlinePlayerTeam == null || onlinePlayerTeam == GoalGame.Team.SPECTATOR) {
                continue;
            }

            GoalGame.Team playerTeam = game.getTeamOf(onlinePlayer);

            if (playerTeam == null || playerTeam.equals(shooterTeam)) {
                continue;
            }

            // Skip defenders who are ankle broken
            if (isAnkleBroken(onlinePlayer)) {
                System.out.println(onlinePlayer.getName() + " is ANKLE BROKEN - can't contest shot!");
                continue;
            }

            double distance = onlinePlayer.getLocation().distance(shooter.getLocation());

            if (distance > baseContestRadius) {
                continue;
            }

            // Base contest calculation (buffed)
            double baseContest = 0.60 - (distance / baseContestRadius * 0.45);
            if (baseContest < 0.0) baseContest = 0.0;

            // ===== LATENCY-FAIR DIRECTIONAL CALCULATION =====
            Vector shooterDirection = shooter.getLocation().getDirection().clone();
            shooterDirection.setY(0); // Horizontal only
            shooterDirection.normalize();

            Vector toDefender = onlinePlayer.getLocation().toVector()
                    .subtract(shooter.getLocation().toVector());
            toDefender.setY(0);
            toDefender.normalize();

            double dotProduct = shooterDirection.dot(toDefender);

            // Calculate ACTUAL distance behind shooter (in blocks)
            Vector behindShooter = shooterDirection.clone().multiply(-1); // Direction behind shooter
            double distanceBehind = onlinePlayer.getLocation().toVector()
                    .subtract(shooter.getLocation().toVector())
                    .dot(behindShooter); // Positive = behind, Negative = in front

            double directionalMultiplier;

            if (dotProduct > 0.3) {
                // ===== DEFENDER IS IN FRONT =====
                // Full contest bonus (1.3x to 1.6x)
                directionalMultiplier = 1.3 + (dotProduct * 0.3);
                System.out.println(onlinePlayer.getName() + " is IN FRONT - Full contest (×" +
                        String.format("%.2f", directionalMultiplier) + ")");
            }
            else if (dotProduct < -0.3) {
                // ===== DEFENDER IS BEHIND =====
                // Check if within 1.5 block "latency buffer"
                if (distanceBehind <= 1.5) {
                    // Within latency buffer - treat as if defender is on the side (70% contest)
                    directionalMultiplier = 0.7;
                    System.out.println(onlinePlayer.getName() + " is BEHIND but within 1.5 blocks (" +
                            String.format("%.2f", distanceBehind) + " blocks) - Latency buffer applied (×0.70)");
                } else {
                    // More than 1.5 blocks behind - severe penalty (5% contest only)
                    directionalMultiplier = 0.05;
                    System.out.println(onlinePlayer.getName() + " is TOO FAR BEHIND (" +
                            String.format("%.2f", distanceBehind) + " blocks) - Minimal contest (×0.05)");
                }
            }
            else {
                // ===== DEFENDER IS TO THE SIDE =====
                // Standard side contest (70%)
                directionalMultiplier = 0.7;
                System.out.println(onlinePlayer.getName() + " is TO THE SIDE - Standard contest (×0.70)");
            }

            // ===== HEIGHT ADVANTAGE CALCULATION (unchanged) =====
            double heightMultiplier = 1.0;
            double defenderY = onlinePlayer.getLocation().getY();
            double shooterY = shooter.getLocation().getY();
            double floorY = game.getArenaBox().getMinY() + 2.65;

            if (defenderY > (floorY + 0.5)) {
                double heightDiff = defenderY - shooterY;

                // Perfect jump timing bonus
                if (heightDiff > -0.5 && heightDiff < 1.5) {
                    heightMultiplier = 1.6;
                    System.out.println(onlinePlayer.getName() + " got a PERFECT JUMP CONTEST!");
                } else if (heightDiff >= 1.5) {
                    heightMultiplier = 1.15;
                } else {
                    heightMultiplier = 1.3;
                }
            } else {
                // Standing defense buff
                if (distance < 2.0) {
                    heightMultiplier = 1.25;
                } else if (distance < 3.5) {
                    heightMultiplier = 1.15;
                } else {
                    heightMultiplier = 1.05;
                }
            }

            // ===== LATE CLOSEOUT PENALTY =====
            double closeoutMultiplier = 1.0;
            
            // Check if defender was already guarding (near shooter in last 1 second)
            Long lastNearTime = defenderProximityTracking.get(onlinePlayer.getUniqueId());
            long currentTime = System.currentTimeMillis();
            boolean wasAlreadyGuarding = lastNearTime != null && (currentTime - lastNearTime) < 1000L; // Within last 1 second

            if (!wasAlreadyGuarding) {
                // Defender wasn't near the shooter recently - this is a late closeout
                Vector defenderVelocity = onlinePlayer.getVelocity();
                double defenderSpeed = Math.sqrt(defenderVelocity.getX() * defenderVelocity.getX() +
                        defenderVelocity.getZ() * defenderVelocity.getZ());

                // Check if defender is moving quickly (rushing to contest)
                if (defenderSpeed > 0.15) {
                    // Apply penalty based on speed - faster closeout = worse contest
                    if (defenderSpeed > 0.35) {
                        closeoutMultiplier = 0.5; // 50% contest for very late/fast closeout
                        System.out.println(onlinePlayer.getName() + " LATE CLOSEOUT (fast) - wasn't guarding, contest reduced to 50%");
                    } else if (defenderSpeed > 0.25) {
                        closeoutMultiplier = 0.65; // 65% contest for moderate late closeout
                        System.out.println(onlinePlayer.getName() + " LATE CLOSEOUT (moderate) - wasn't guarding, contest reduced to 65%");
                    } else {
                        closeoutMultiplier = 0.80; // 80% contest for slight late closeout
                        System.out.println(onlinePlayer.getName() + " LATE CLOSEOUT (slight) - wasn't guarding, contest reduced to 80%");
                    }
                } else {
                    // Not moving fast but still wasn't guarding - mild penalty
                    closeoutMultiplier = 0.85;
                    System.out.println(onlinePlayer.getName() + " wasn't guarding initially - mild contest reduction to 85%");
                }
            } else {
                System.out.println(onlinePlayer.getName() + " was already guarding - no late closeout penalty");
            }

            // ===== CALCULATE FINAL CONTEST FOR THIS DEFENDER =====
            double contestValue = baseContest * directionalMultiplier * heightMultiplier * closeoutMultiplier;

            if (contestValue > 0.05) {
                defenderContests.add(contestValue);
                System.out.println(onlinePlayer.getName() + " contesting shooter: base=" +
                        String.format("%.2f", baseContest) + " directional=" +
                        String.format("%.2f", directionalMultiplier) + " height=" +
                        String.format("%.2f", heightMultiplier) + " closeout=" +
                        String.format("%.2f", closeoutMultiplier) + " TOTAL=" +
                        String.format("%.2f", contestValue));
            }
        }

        if (defenderContests.isEmpty()) {
            System.out.println("No defenders contesting - contest = 0%");
            return 0.0;
        }

        // Sort contests from highest to lowest
        defenderContests.sort(Collections.reverseOrder());

        double totalContest = 0.0;

        if (defenderContests.size() == 1) {
            // Single defender - full contest value
            totalContest = defenderContests.get(0);
            System.out.println("Single defender contest: " + String.format("%.1f%%", totalContest * 100));
        } else {
            // Multiple defenders - primary defender gets full value, others add diminishing value
            totalContest = defenderContests.get(0);

            // Secondary defenders add 40% of their value
            for (int i = 1; i < defenderContests.size(); i++) {
                double additionalContest = defenderContests.get(i) * 0.40;
                totalContest += additionalContest;

                System.out.println("Defender #" + (i + 1) + " adding " +
                        String.format("%.1f%%", additionalContest * 100) + " contest (stacked)");
            }
            System.out.println("Total stacked contest: " + String.format("%.1f%%", totalContest * 100));
        }

        // Cap contest at 1.0 (100%)
        return Math.min(totalContest, 1.0);
    }

    /**
     * Calculates and stores the contest percentage when the shot meter starts.
     * Called when player begins holding right-click (meter start).
     * For jump shots only - dunks calculate contest at release.
     */
    public void calculateAndStoreContestAtMeterStart(Player player) {
        this.meterStartContestPercentage = calculateContest(player);
        System.out.println("Contest calculated at METER START: " + String.format("%.1f%%", this.meterStartContestPercentage * 100));
    }

    public void forceThrow() {
        if (this.getCurrentDamager() != null) {
            Player player = this.getCurrentDamager();
            this.setLocation(player.getEyeLocation().add(player.getLocation().getDirection().multiply(0.45)));
            this.setVelocity(player, player.getLocation().getDirection().multiply(0.6));
            this.giveaway();
            this.threeEligible = false;
            this.delay = 10;
            this.giveaway();
        }
    }

    public boolean pass(Player player) {
        if (this.catchDelay > 0) {
            player.playSound(player.getLocation(), Sound.ENTITY_ARMOR_STAND_BREAK, SoundCategory.MASTER, 100.0f, 1.0f);
            return false;
        }
        
        // Check for stale inbound state
        if (this.game.inboundingActive && this.game.inbounder != null && this.game.inboundPassTime > 0) {
            if ((System.currentTimeMillis() - this.game.inboundPassTime) > 2000L) {
                System.out.println("CLEARING STALE INBOUND STATE in pass() - inbound pass was " + 
                    (System.currentTimeMillis() - this.game.inboundPassTime) + "ms ago");
                this.game.inboundingActive = false;
                this.game.inbounder = null;
                this.game.inboundTouchedByInbounder = false;
                this.game.inbounderHasReleased = false;
            }
        }
        
        if (this.delay < 1 && this.passDelay < 1 && this.getCurrentDamager() != null && this.getCurrentDamager().equals(player)) {
            Location location = player.getEyeLocation().clone();
            location.subtract(0.0, 0.5, 0.0);
            Vector direction = player.getLocation().getDirection().clone();

            if (player.isSneaking()) {
                direction.setX(-direction.getX());
                direction.setZ(-direction.getZ());
            }

            // DIFFERENT PASS SPEEDS BASED ON MODE
            double passSpeed;
            double verticalComponent;

            if (this.passMode == PassMode.BULLET) {
                // Bullet pass: Normal speed, normal arc
                passSpeed = 0.915;
                verticalComponent = 0.09;
            } else {
                // Lob pass: Speed and arc vary with aim angle
                float pitch = player.getLocation().getPitch();

                if (pitch < -20) {
                    // Looking up - high lob (slower, higher arc)
                    // pitch -90 (straight up) to -20 (slightly up)
                    float normalizedPitch = (pitch + 90) / 70.0f; // 0 to 1 range
                    passSpeed = 0.55 + (normalizedPitch * 0.20); // 0.55 to 0.75

                    // Calculate vertical component for high arc (max 4 blocks above player)
                    double targetApexY = player.getLocation().getY() + Math.min(3.5, 3.0 + Math.abs(pitch) / 15.0);
                    double currentY = player.getEyeLocation().getY() - 0.5;
                    double heightDifference = targetApexY - currentY;

                    // Vertical component capped to not exceed 4 blocks height
                    verticalComponent = Math.min(0.5, Math.max(0.3, heightDifference / 10.0));
                } else if (pitch >= -20 && pitch <= 20) {
                    // Looking straight - medium lob
                    passSpeed = 0.75;
                    verticalComponent = 0.35;
                } else {
                    // Looking down - low/flat lob (faster, lower arc)
                    // pitch 20 to 90 (straight down)
                    float normalizedPitch = (pitch - 20) / 70.0f; // 0 to 1 range
                    passSpeed = 0.75 + (normalizedPitch * 0.15); // 0.75 to 0.90

                    // Lower arc when looking down
                    verticalComponent = Math.max(0.15, 0.35 - (normalizedPitch * 0.20)); // 0.35 to 0.15
                }
            }

            if (this.game.getCourtLength() == 26.0) {
                passSpeed *= 1.1;
            }

            this.setLocation(location.add(direction.clone().multiply(0.45)));
            this.setVelocity(player, direction.clone().multiply(passSpeed), verticalComponent);

            this.game.startAssistTimer(player.getUniqueId());

            // FIXED: Set lob flags AFTER velocity is set (not dependent on passer's height)
            if (this.passMode == PassMode.LOB) {
                this.isLobPass = true;
                this.lobPasserUUID = player.getUniqueId();
                System.out.println("Lob pass thrown by " + player.getName());
            } else {
                this.isLobPass = false;
                this.lobPasserUUID = null;
            }

            this.wasShot = false; // ← ADD THIS: Mark this as NOT a shot

            // NEW: Notify game if inbounder is passing
            if (this.game.inboundingActive && player.equals(this.game.inbounder)) {
                this.game.onInboundPass();
                System.out.println("Inbound pass detected - starting 1 second OOB grace period");
            }

            if (this.game.getState().equals(GoalGame.State.REGULATION) || this.game.getState().equals(GoalGame.State.OVERTIME)) {
                this.game.getStatsManager().getPlayerStats(player.getUniqueId()).incrementPassAttempts();
            }

            this.giveaway();
            this.threeEligible = false;
            this.delay = 1; // 0.05 seconds (1 tick)

            if (this.game.inboundingActive && player.equals(this.game.inbounder)) {
                this.game.dropInboundBarrierButKeepClockFrozen();
            }
            return true;
        }
        return false;
    }


    public boolean canDunk(Player player) {
        Location location = this.getTargetHoop(player).clone();
        location.setY(player.getLocation().getY());
        double dis = location.distance(player.getLocation());

        // INCREASED DUNK RANGE - was 0.525 to 3.0, now 0.525 to 5.0
        // Vertical height check - player must be high enough
        boolean highEnough = player.getLocation().getY() > this.game.getCenter().getY() + 0.333;
        boolean inRange = dis > 0.525 && dis < 9.0;

        return highEnough && inRange;
    }

    // ALSO - Make sure isLayupRange doesn't conflict:
    public boolean isLayupRange(Player player) {
        Location targetHoop = this.getTargetHoop(player);
        double distance = player.getLocation().distance(targetHoop);

        // Layup range: close to rim (within 7 blocks) - increased from 6
        // This CAN overlap with dunk range - that's OK!
        return distance <= 7.0;
    }

    private ShotType determineShotType(Player player, double distanceToHoop) {
        // Layups/paint shots are NOT affected by standing/moving
        if (distanceToHoop <= 8) {
            return ShotType.LAYUP;
        }

        // Use PlayerInputTracker to detect if player is pressing movement keys
        // This is the same method used for stepbacks and provides accurate movement detection
        PlayerInputTracker.InputState input = PlayerInputTracker.getInput(player);
        
        // Check if player is actively pressing any movement key (WASD)
        boolean isMoving = input.forward || input.backward || input.left || input.right;
        
        // Calculate if this is a standing shot
        long timeSinceMovement = System.currentTimeMillis() - this.lastMovementTime;
        
        // If player is currently moving OR moved recently (within 0.5 seconds), it's off-dribble
        boolean isOffDribble = isMoving || 
                              (this.hasMovedWithBall && timeSinceMovement < STANDING_STILL_DURATION);

        // Mid Range (8-16 blocks)
        if (distanceToHoop <= 16) {
            return isOffDribble ? ShotType.MOVING_MID : ShotType.STANDING_MID;
        }
        // Three Point Range (16+ blocks)
        else {
            return isOffDribble ? ShotType.MOVING_THREE : ShotType.STANDING_THREE;
        }
    }

    private Location getBackboardLocation(Player player) {
        if (player == null) return null;

        Location targetHoop = this.getTargetHoop(player);
        if (targetHoop == null) return null;

        Location backboard = targetHoop.clone();

        if (targetHoop.getZ() < this.game.getCenter().getZ()) {
            // Away net - backboard is further negative Z
            backboard.add(0, 0.5, -1.2);
        } else {
            // Home net - backboard is further positive Z
            backboard.add(0, 0.5, 1.2);
        }

        return backboard;
    }

    public boolean dunk(Player player) {
        if (this.getCurrentDamager() != null && this.getCurrentDamager().equals(player)) {
            // Check for stale inbound state
            if (this.game.inboundingActive && this.game.inbounder != null && this.game.inboundPassTime > 0) {
                if ((System.currentTimeMillis() - this.game.inboundPassTime) > 2000L) {
                    System.out.println("CLEARING STALE INBOUND STATE in dunk() - inbound pass was " + 
                        (System.currentTimeMillis() - this.game.inboundPassTime) + "ms ago");
                    this.game.inboundingActive = false;
                    this.game.inbounder = null;
                    this.game.inboundTouchedByInbounder = false;
                    this.game.inbounderHasReleased = false;
                }
            }
            
            // PREVENT LAYUPS/DUNKS DURING INBOUND
            if (this.game.inboundingActive && player.equals(this.game.inbounder)) {
                player.sendMessage(Component.text("You can't layup/dunk while inbounding!").color(Colour.deny()));
                return false;
            }

            // SECOND TAP - Meter is already active, execute dunk
            if (this.dunkMeterActive) {
                System.out.println("Q TAP - Executing dunk at accuracy: " + this.dunkMeterAccuracy);
                return this.executeDunk(player);
            }

            // FIRST TAP - Check if player is in dunk range (distance only, NOT HEIGHT)
            Location targetHoop = this.getTargetHoop(player);
            Location hoopLocation = targetHoop.clone();
            hoopLocation.setY(player.getLocation().getY()); // Ignore Y for distance check
            double horizontalDistance = hoopLocation.distance(player.getLocation());

            // Must be within 9 blocks horizontally
            if (horizontalDistance > 9) {
                player.sendMessage(Component.text("Too far from rim!").color(Colour.deny()));
                return false;
            }

            // Start the meter - NO HEIGHT CHECK HERE!
            // Height is only checked in executeDunk() when the meter is released
            this.dunkMeterActive = true;
            this.dunkMeterAccuracy = 0;
            this.dunkMeterWait = 0;
            this.dunkMeterForward = true;
            this.isDunkAttempt = true;
            this.dunkMeterStartedAirborne = !player.isOnGround();

            System.out.println("FIRST Q TAP - Dunk meter started for " + player.getName() + " (distance: " + horizontalDistance + ")");
            player.sendMessage(Component.text("DUNK METER ACTIVE! Jump and tap Q again!", NamedTextColor.GREEN));
            return true;
        }

        return false;
    }


    public boolean executeDunk(Player player) {
        if (!this.dunkMeterActive || !this.isDunkAttempt) {
            return false;
        }

        if (this.getCurrentDamager() == null || !this.getCurrentDamager().equals(player)) {
            this.dunkMeterActive = false;
            this.dunkMeterAccuracy = 0;
            this.isDunkAttempt = false;
            return false;
        }

        // ← HEIGHT/DISTANCE CHECK - Now PREVENTS attempt instead of forcing miss
        Location targetHoop = this.getTargetHoop(player);
        Location hoopLocation = targetHoop.clone();
        hoopLocation.setY(player.getLocation().getY());
        double horizontalDistance = hoopLocation.distance(player.getLocation());

        boolean highEnough = player.getLocation().getY() > this.game.getCenter().getY() + 0.333;
        boolean inRange = horizontalDistance > 0.525 && horizontalDistance < 3.0;

        // NEW: If not in valid dunk range, CANCEL the attempt and notify player
        if (!highEnough) {
            player.sendMessage(Component.text("Too low to dunk!").color(Colour.deny()));
            this.dunkMeterActive = false;
            this.dunkMeterAccuracy = 0;
            this.isDunkAttempt = false;
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, SoundCategory.MASTER, 0.5f, 1.0f);
            return false; // Don't execute dunk
        }

        if (horizontalDistance >= 3.0) {
            player.sendMessage(Component.text("Too far to dunk!").color(Colour.deny()));
            this.dunkMeterActive = false;
            this.dunkMeterAccuracy = 0;
            this.isDunkAttempt = false;
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, SoundCategory.MASTER, 0.5f, 1.0f);
            return false; // Don't execute dunk
        }

        // CHECK FOR BLOCK FIRST (before anything else)
        BlockResult blockCheck = checkForDunkBlock(player);

        if (blockCheck.wasBlocked()) {
            // DUNK WAS BLOCKED!
            Player blocker = blockCheck.getBlocker();

            // Calculate block direction (away from dunker, toward blocker's direction)
            Vector blockDirection = blocker.getLocation().getDirection().normalize();
            blockDirection.setY(0.3); // Slight upward angle
            this.removeCurrentDamager();

            // Set ball velocity in block direction
            this.setLocation(player.getEyeLocation().add(player.getLocation().getDirection().multiply(0.5)));
            this.setVelocity(blockDirection.multiply(0.5), 0.3);

            // Messages and sounds
            player.sendMessage(Component.text("BLOCKED!").color(Colour.deny()).decorate(TextDecoration.BOLD));
            blocker.sendMessage(Component.text("BLOCK!").color(Colour.allow()).decorate(TextDecoration.BOLD));

            blocker.playSound(blocker.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, SoundCategory.MASTER, 1.0f, 1.2f);
            player.playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, SoundCategory.MASTER, 1.0f, 0.8f);

            // Increment block stats
            if (this.game.getState().equals(GoalGame.State.REGULATION) || this.game.getState().equals(GoalGame.State.OVERTIME)) {
                PlayerStats blockerStats = this.game.getStatsManager().getPlayerStats(blocker.getUniqueId());
                blockerStats.incrementBlocks();
            }

            // Mark ball as NOT rebound-eligible so OOB check works properly
            this.setReboundEligible(false);

            // CRITICAL FIX: Prevent dunker from immediately recatching the blocked ball
            // Add 1.5 second pickup cooldown for the blocked dunker
            long cooldownExpiry = System.currentTimeMillis() + 1500L;
            pokeStealImmunity.put(player.getUniqueId(), cooldownExpiry);
            System.out.println(player.getName() + " has 1.5-second pickup cooldown after blocked dunk");

            // Reset dunk state
            this.dunkMeterActive = false;
            this.dunkMeterAccuracy = 0;
            this.isDunkAttempt = false;
            this.threeEligible = false;
            this.giveaway();
            this.delay = 15;

            defenderBlockAttempts.clear();
            return true;
        }

        // NO BLOCK - Continue with normal dunk logic
        if (!this.isShotAttemptRegistered() && (this.game.getState().equals(GoalGame.State.REGULATION) || this.game.getState().equals(GoalGame.State.OVERTIME))) {
            // Track dunk attempt in FG stats
            PlayerStats stats = this.game.getStatsManager().getPlayerStats(player.getUniqueId());
            stats.incrementFGAttempted();

            this.setShotAttemptRegistered(true);
        }

        Location target = this.getTargetHoop(player);

        DunkContestResult contestResult = calculateDunkContest(player);

        // USE METER ACCURACY to determine dunk success (no more forced miss)
        boolean successful = calculateDunkSuccessWithMeter(this.dunkMeterAccuracy, contestResult);

        // FIXED: Display the DUNK contest percentage, not the last jump shot's contest
        final String percentage = String.format("%.2f", contestResult.getContestValue() * 100.0f);
        Component contestDisplay = MiniMessage.miniMessage().deserialize("<green>" + percentage + "% Contested");

        // Show to EVERYONE on the court
        for (Player p : this.game.getPlayers()) {
            p.showTitle(Title.title(
                    Component.empty(),
                    contestDisplay,
                    Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ofMillis(350L))
            ));
        }

        if (successful && contestResult.shouldPosterize() && contestResult.getClosestDefender() != null) {
            applyPosterizer(player, contestResult.getClosestDefender());
        }

        Vector fly = player.getLocation().getDirection().multiply(-1.08);
        fly.setY(0.55);
        player.setVelocity(fly);

        // NEW: Reset scoring flags BEFORE setting location
        this.layupScored = false;
        this.shouldPreventScore = false;
        this.layupScoreDetected = false;

        this.setLocation(target.clone().add(0.0, 0.85, 0.0));

        // FIXED: Different animations for make vs miss
        if (successful) {
            // MAKE - Ball goes straight down through hoop
            this.setVelocity(player, player.getLocation().getDirection().normalize().multiply(0.05), -0.2);
            player.sendMessage(Component.text("DUNK!").color(Colour.allow()).decorate(TextDecoration.BOLD));
            player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.MASTER, 0.3f, 1.8f);

            // NEW: Play green sound on successful dunk (only in regulation/overtime)
            if (this.game.getState().equals(GoalGame.State.REGULATION) ||
                    this.game.getState().equals(GoalGame.State.OVERTIME)) {

                Athlete athlete = AthleteManager.get(player.getUniqueId());
                CosmeticSound greenSound = athlete != null ? athlete.getGreenSound() : CosmeticSound.NO_SOUND;

                if (greenSound != CosmeticSound.NO_SOUND &&
                        greenSound.getSoundIdentifier() != null &&
                        !greenSound.getSoundIdentifier().isEmpty()) {
                    player.getWorld().playSound(player.getLocation(),
                            greenSound.getSoundIdentifier(),
                            SoundCategory.PLAYERS, 3.5f, 1.0f);
                }
            }

            // NEW: Mark as dunk attempt so goal() knows to score it
            this.isDunkAttempt = true;
        } else {
            // MISS - Ball bounces off rim/backboard
            Vector missDirection = player.getLocation().getDirection().normalize();

            // Add randomness to miss direction
            Random rand = new Random();
            double missType = rand.nextDouble();

            if (missType < 0.33) {
                // Miss left
                missDirection.setX(missDirection.getX() - 0.4);
            } else if (missType < 0.66) {
                // Miss right
                missDirection.setX(missDirection.getX() + 0.4);
            } else {
                // Miss backward (hit front of rim)
                missDirection.multiply(-0.3);
            }

            // Add some vertical bounce
            missDirection.setY(0.2 + rand.nextDouble() * 0.2);

            this.setVelocity(missDirection.multiply(0.6));

            player.sendMessage(Component.text("Missed Dunk!").color(Colour.deny()).decorate(TextDecoration.BOLD));
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, SoundCategory.MASTER, 0.5f, 0.8f);

            // NEW: Prevent scoring on missed dunk
            this.shouldPreventScore = true;
            this.isDunkAttempt = false;
        }

        this.dunkMeterActive = false;
        this.dunkMeterAccuracy = 0;
        this.threeEligible = false;
        this.giveaway();
        this.delay = 15;

        return true;
    }
    private void steal(Player player) {
        if (!BallFactory.hasBall(player)) {
            if (this.getCurrentDamager() == null) {
                // Standard catch radius for all loose balls
                double catchRadius = 4.0;

                // Check if player is within catch radius
                double distanceToBall = player.getLocation().distance(this.getLocation());
                if (distanceToBall > catchRadius) {
                    return; // Too far to catch
                }

                // FIXED: Use setDamager instead of just setting it
                this.setDamager(player);

                // Clear inbound state if the new possessor is not the inbounder
                if (this.game instanceof BasketballGame bbGame) {
                    bbGame.clearInboundStateIfNotInbounder(player);
                }

                this.stealImmunityTicks = 40;
                this.setStealDelay(10);
                this.delay = 10;
                this.accuracy = 0;
                this.threeEligible = false;
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.MASTER, 100.0f, 1.2f);
                player.getInventory().setHeldItemSlot(0);
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 10, 0));
                return; // ADDED: Exit after successful loose ball pickup
            }
            if (this.getCurrentDamager() != null && this.getCurrentDamager() != player) {
                Player oldOwner = this.getCurrentDamager();

                // PREVENT STEALING FROM INBOUNDER
                if (this.game.inboundingActive && oldOwner.equals(this.game.inbounder)) {
                    player.playSound(player.getLocation(), Sound.ENTITY_ARMOR_STAND_BREAK,
                            SoundCategory.MASTER, 100.0f, 1.0f);
                    return;
                }

                // Check for inbound immunity - prevent steals during inbound
                if (this.game instanceof BasketballGame bbGame && bbGame.isOutOfBoundsImmunity()) {
                    System.out.println("Snatch steal blocked - inbound immunity active");
                    return;
                }

                // Track steal stats (only for regulated games)
                if ((this.game.getState().equals(GoalGame.State.REGULATION) || this.game.getState().equals(GoalGame.State.OVERTIME)) &&
                        this.game.getHomePlayers().contains(oldOwner) != this.game.getHomePlayers().contains(player)) {
                    PlayerStats newStats = this.game.getStatsManager().getPlayerStats(player.getUniqueId());
                    newStats.incrementSteals();
                    PlayerStats oldStats = this.game.getStatsManager().getPlayerStats(oldOwner.getUniqueId());
                    oldStats.incrementTurnovers();
                }

                oldOwner.playSound(oldOwner.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.MASTER, 100.0f, 0.8f);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.MASTER, 100.0f, 1.2f);
                oldOwner.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0));

                if (Math.random() > 0.25) {
                    if (Math.random() > 0.15) {
                        // POKED STEAL - Ball is loose
                        this.removeCurrentDamager();
                        this.setVelocity(player.getEyeLocation().getDirection().multiply(0.2));
                        player.sendMessage("Ball poked!");
                        this.accuracy = 0;
                        this.setStealDelay(10);
                        this.delay = 10;
                        this.globalStealCooldown = 80;  // 4 seconds
                        return; // ADDED: Exit after poke
                    } else {
                        // SUCCESSFUL STEAL - Player gains possession
                        this.setDamager(player);
                        
                        // Clear inbound state if the new possessor is not the inbounder
                        if (this.game instanceof BasketballGame bbGame) {
                            bbGame.clearInboundStateIfNotInbounder(player);
                        }
                        
                        this.stealImmunityTicks = 20;
                        this.accuracy = 0;
                        player.getInventory().setHeldItemSlot(0);
                        player.sendMessage("Snatched the ball!");

                        // Handle 1v1 position reset after steal
                        this.handleSteal1v1(player);

                        this.setStealDelay(10);
                        this.delay = 10;
                        this.globalStealCooldown = 80;  // 4 seconds
                        return; // ADDED: Exit after successful steal
                    }
                }
                this.setStealDelay(10);
                this.delay = 10;
                this.globalStealCooldown = 80;  // 4 seconds
            }
        }
    }

    private boolean doesPathIntersectBackboard(Location shootLocation, Location targetHoop, Player shooter) {
        // Null checks first
        if (shootLocation == null || targetHoop == null || shootLocation.getWorld() == null) {
            return false;
        }

        Location backboard = getBackboardLocation(shooter);
        if (backboard == null || !backboard.getWorld().equals(shootLocation.getWorld())) {
            return false;
        }

        // Backboard parameters
        double backboardThickness = 0.5;
        double backboardRadius = 1.5;

        try {
            // Calculate direction
            Vector shootToHoop = targetHoop.toVector().subtract(shootLocation.toVector());

            // Sanity check - make sure vector is valid
            if (!isValidVector(shootToHoop)) {
                System.out.println("Invalid shootToHoop vector, skipping backboard check");
                return false;
            }

            double totalDistance = shootToHoop.length();

            // If distance is 0 or too small, no intersection possible
            if (totalDistance < 0.1) {
                return false;
            }

            Vector direction = shootToHoop.normalize();

            // Sanity check - make sure direction is valid
            if (!isValidVector(direction)) {
                System.out.println("Invalid direction vector, skipping backboard check");
                return false;
            }

            // Sample points along the path from shooter to hoop
            for (double t = 0; t <= totalDistance; t += 0.2) {
                Vector offset = direction.clone().multiply(t);

                // Sanity check on offset
                if (!isValidVector(offset)) {
                    System.out.println("Invalid offset at t=" + t + ", stopping backboard check");
                    break;
                }

                Location samplePoint = shootLocation.clone().add(offset);

                // Sanity check on sample point
                if (!isValidLocation(samplePoint)) {
                    System.out.println("Invalid sample point at t=" + t);
                    break;
                }

                // Check if this point is within the backboard collision box
                if (isPointNearBackboard(samplePoint, backboard, backboardThickness, backboardRadius)) {
                    System.out.println("Path intersects backboard at distance t=" + t);
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            System.out.println("Exception during backboard check: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    private boolean isValidVector(Vector v) {
        if (v == null) return false;
        return !Double.isNaN(v.getX()) && !Double.isNaN(v.getY()) && !Double.isNaN(v.getZ()) &&
                !Double.isInfinite(v.getX()) && !Double.isInfinite(v.getY()) && !Double.isInfinite(v.getZ());
    }
    public void onDefenderClickedBallHandler(Player defender, Player ballHandler) {
        // This is called when a defender left-clicks the ball handler


        // NEW: PREVENT STEALING FROM INBOUNDER
        if (this.game.inboundingActive && ballHandler.equals(this.game.inbounder)) {
            defender.playSound(defender.getLocation(), Sound.ENTITY_ARMOR_STAND_BREAK,
                    SoundCategory.MASTER, 100.0f, 1.0f);
            return;
        }

        // Verify ball handler actually has the ball
        if (this.getCurrentDamager() == null || !this.getCurrentDamager().equals(ballHandler)) {
            return;
        }

        // Verify teams are different
        GoalGame.Team defenderTeam = this.game.getTeamOf(defender);
        GoalGame.Team handlerTeam = this.game.getTeamOf(ballHandler);

        if (defenderTeam == null || handlerTeam == null ||
                defenderTeam.equals(handlerTeam) ||
                defenderTeam == GoalGame.Team.SPECTATOR) {
            return;
        }

        // ===== FIX: CHECK GLOBAL STEAL COOLDOWN FIRST =====
        if (this.globalStealCooldown > 0) {
            int secondsLeft = (int) Math.ceil(this.globalStealCooldown / 20.0);
            defender.sendActionBar(Component.text("Steal on cooldown: " + secondsLeft + "s")
                    .color(NamedTextColor.RED));
            defender.playSound(defender.getLocation(), Sound.ENTITY_ARMOR_STAND_BREAK,
                    SoundCategory.MASTER, 100.0f, 1.0f);
            return;
        }

        // Check distance
        double distance = defender.getLocation().distance(ballHandler.getLocation());
        if (distance > 4.5) {
            return;
        }

        // Check if defender is on ground
        if (!defender.isOnGround()) {
            return;
        }

        // Check if ball handler is on ground (no interference with dunks/layups)
        if (!ballHandler.isOnGround()) {
            return;
        }

        // NEW: Check if defender actually clicked close to the BALL
        double distanceToBall = defender.getLocation().distance(this.getLocation());
        boolean clickedNearBall = distanceToBall <= 2.5;

        if (clickedNearBall) {
            // ===== STEAL ATTEMPT: Defender clicked the ball =====

            // Calculate steal success chance based on distance
            double baseStealChance;

            if (distanceToBall <= 1.5) {
                baseStealChance = 0.20; // 20% at very close range
            } else if (distanceToBall <= 2.0) {
                baseStealChance = 0.10; // 10% at medium range
            } else {
                baseStealChance = 0.04; // 4% at max range (2.5 blocks)
            }

            // Reduce steal chance if ball handler is moving
            Vector handlerVelocity = ballHandler.getVelocity();
            double handlerSpeed = Math.sqrt(handlerVelocity.getX() * handlerVelocity.getX() +
                    handlerVelocity.getZ() * handlerVelocity.getZ());

            if (handlerSpeed > 0.2) {
                baseStealChance *= 0.7; // 30% penalty if ball handler is moving fast
            }

            // Roll for steal success
            boolean stealSuccessful = Math.random() < baseStealChance;

            if (stealSuccessful) {
                // ===== SUCCESSFUL STEAL =====

                // Check for inbound immunity - prevent steals during inbound
                if (this.game instanceof BasketballGame bbGame && bbGame.isOutOfBoundsImmunity()) {
                    System.out.println("Left-click steal blocked - inbound immunity active");
                    return;
                }

                // Track steal/turnover stats
                if ((this.game.getState().equals(GoalGame.State.REGULATION) ||
                        this.game.getState().equals(GoalGame.State.OVERTIME)) &&
                        this.game.getHomePlayers().contains(ballHandler) != this.game.getHomePlayers().contains(defender)) {
                    PlayerStats defenderStats = this.game.getStatsManager().getPlayerStats(defender.getUniqueId());
                    defenderStats.incrementSteals();
                    PlayerStats handlerStats = this.game.getStatsManager().getPlayerStats(ballHandler.getUniqueId());
                    handlerStats.incrementTurnovers();
                }
                this.game.pressPrevention = false;
                this.game.restrictedTeam = null;

                ballHandler.playSound(ballHandler.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP,
                        SoundCategory.MASTER, 100.0f, 0.8f);
                defender.playSound(defender.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP,
                        SoundCategory.MASTER, 100.0f, 1.2f);
                ballHandler.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0));

                // ALWAYS POKE - Ball gets poked loose
                this.removeCurrentDamager();
                this.setVelocity(defender.getEyeLocation().getDirection().multiply(0.2));
                defender.sendMessage(Component.text("Ball poked!").color(Colour.allow()));
                this.accuracy = 0;

                // Track poke data for OOB possession
                this.lastPossessionBeforePoke = ballHandler.getUniqueId();
                this.wasPoked = true;

                this.setStealDelay(10);
                this.delay = 10;

                // ===== FIX: SET GLOBAL STEAL COOLDOWN =====
                this.globalStealCooldown = 80; // 4 seconds (80 ticks)
                System.out.println("Successful steal - 4 second global cooldown activated");
            } else {
                // ===== FAILED STEAL ATTEMPT =====

                // Record this as a steal attempt for ankle breaker detection
                this.recordStealAttempt(defender);

                // Apply brief slowness to defender (punishment for failed steal)
                defender.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 27, 1, true, false));

                // Visual/audio feedback
                defender.sendActionBar(Component.text("Steal missed!").color(NamedTextColor.RED));
                defender.playSound(defender.getLocation(), Sound.ENTITY_PLAYER_ATTACK_WEAK,
                        SoundCategory.MASTER, 0.5f, 0.8f);

                // Small cooldown before next attempt
                this.setStealDelay(15);

                System.out.println(defender.getName() + " FAILED steal attempt on " +
                        ballHandler.getName() + " (chance was " +
                        String.format("%.1f%%", baseStealChance * 100) + ")");
            }
        } else {
            // ===== MISSED STEAL - Clicked ball handler but NOT the ball =====
            this.recordStealAttempt(defender);
            this.game.pressPrevention = false;
            this.game.restrictedTeam = null;

            // Visual/audio feedback
            defender.sendActionBar(Component.text("Missed steal!").color(NamedTextColor.RED));
            defender.playSound(defender.getLocation(), Sound.ENTITY_PLAYER_ATTACK_WEAK,
                    SoundCategory.MASTER, 0.5f, 0.8f);

            System.out.println(defender.getName() + " MISSED STEAL on " + ballHandler.getName() +
                    " - ankle breaker window active");
        }
    }

    public boolean dash(Player player) {
        // NEW: Use ProtocolLib to check if player is pressing jump key
        PlayerInputTracker.InputState input = PlayerInputTracker.getInput(player);
        if (input.jump) {
            player.sendActionBar(Component.text("Can't dash while jumping!")
                    .color(NamedTextColor.RED));
            return false;
        }
        
        // Can't dash while in the air
        if (!player.isOnGround()) {
            player.sendActionBar(Component.text("You must be on the ground to dash!")
                    .color(NamedTextColor.RED));
            return false;
        }

        // Check if player has ball (can't dash with ball)
        if (this.getCurrentDamager() != null && this.getCurrentDamager().equals(player)) {
            player.sendActionBar(Component.text("You can't dash while holding the ball!")
                    .color(NamedTextColor.RED));
            return false;
        }

        // NEW: Check if player is on DEFENSE (not offense)
        Player ballHandler = this.getCurrentDamager();
        if (ballHandler != null) {
            GoalGame.Team ballHandlerTeam = this.game.getTeamOf(ballHandler);
            GoalGame.Team playerTeam = this.game.getTeamOf(player);

            // If player is on same team as ball handler, they're on offense - can't dash
            if (ballHandlerTeam != null && playerTeam != null && ballHandlerTeam.equals(playerTeam)) {
                player.sendActionBar(Component.text("Dash is only available on defense!")
                        .color(NamedTextColor.RED));
                return false;
            }
        }

        UUID playerId = player.getUniqueId();

        // Check if locked out from poke steal
        if (stolenFromDashLockout.containsKey(playerId)) {
            long lockoutExpiry = stolenFromDashLockout.get(playerId);
            long currentTime = System.currentTimeMillis();
            
            if (currentTime < lockoutExpiry) {
                long remainingMs = lockoutExpiry - currentTime;
                int secondsLeft = (int) Math.ceil(remainingMs / 1000.0);
                player.sendActionBar(Component.text("Dash locked after poke steal: " + secondsLeft + "s")
                        .color(NamedTextColor.RED));
                return false;
            } else {
                // Lockout expired, remove it
                stolenFromDashLockout.remove(playerId);
            }
        }

        // Check cooldown
        if (dashCooldowns.containsKey(playerId)) {
            long cooldownExpiry = dashCooldowns.get(playerId);
            long currentTime = System.currentTimeMillis();
            
            if (currentTime < cooldownExpiry) {
                long remainingMs = cooldownExpiry - currentTime;
                int secondsLeft = (int) Math.ceil(remainingMs / 1000.0);
                player.sendActionBar(Component.text("Dash on cooldown: " + secondsLeft + "s")
                        .color(NamedTextColor.RED));
                return false;
            } else {
                // Cooldown expired, remove it
                dashCooldowns.remove(playerId);
            }
        }

        // Execute dash - propel player 3 blocks forward
        Vector dashDirection = player.getLocation().getDirection();
        dashDirection.setY(0); // Keep it horizontal only
        dashDirection.normalize(); // Normalize AFTER setting Y to 0 for consistent distance
        dashDirection.multiply(0.85); // Reduced to ~3 block dash
        dashDirection.setY(0.35); // Add slight upward component

        player.setVelocity(dashDirection);

        // Apply slowness debuff - CHANGED to Slowness II (was Slowness III)
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, DASH_SLOWNESS_DURATION, 1, true, false));

        // Set cooldown
        long currentTime = System.currentTimeMillis();
        dashCooldowns.put(playerId, currentTime + DASH_COOLDOWN_MS);
        lastDashTime.put(playerId, currentTime);
        dashJumpLock.put(playerId, DASH_JUMP_LOCK_TICKS); // Lock jumping for 0.5 seconds

        // Visual/audio feedback
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.MASTER, 0.5f, 1.5f);
        player.sendActionBar(Component.text("DASH!").color(Colour.allow()).decorate(TextDecoration.BOLD));

        // Spawn particles
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 20, 0.3, 0.3, 0.3, 0.1);

        return true;
    }
    
    public boolean isDashJumpLocked(Player player) {
        return dashJumpLock.getOrDefault(player.getUniqueId(), 0) > 0;
    }
    
    private boolean isValidLocation(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        double x = loc.getX();
        double y = loc.getY();
        double z = loc.getZ();
        return !Double.isNaN(x) && !Double.isNaN(y) && !Double.isNaN(z) &&
                !Double.isInfinite(x) && !Double.isInfinite(y) && !Double.isInfinite(z);
    }
    private boolean isPointNearBackboard(Location point, Location backboardCenter, double thickness, double radius) {
        if (point == null || backboardCenter == null) return false;

        // Sanity check coordinates
        if (!isValidLocation(point) || !isValidLocation(backboardCenter)) {
            return false;
        }

        double distX = Math.abs(point.getX() - backboardCenter.getX());
        double distY = Math.abs(point.getY() - backboardCenter.getY());
        double distZ = Math.abs(point.getZ() - backboardCenter.getZ());

        // Check for NaN results
        if (Double.isNaN(distX) || Double.isNaN(distY) || Double.isNaN(distZ)) {
            return false;
        }

        return distX <= radius && distY <= radius && distZ <= thickness;
    }

    /**
     * Convert meter accuracy (0-8 scale) to timing error (0.0-1.0 scale)
     * Green (7-8): 0.0 - 0.03 error
     * Yellow (4-6): 0.04 - 0.35 error
     * Red (0-3): 0.36 - 1.0 error
     */
    private double calculateTimingError(int meterAccuracy) {
        if (meterAccuracy >= 7) {
            // Green window - minimal error
            return (8 - meterAccuracy) * 0.015;  // 7→0.015, 8→0.0
        } else if (meterAccuracy >= 4) {
            // Yellow window - moderate error
            return 0.04 + ((6 - meterAccuracy) * 0.105);  // 6→0.04, 4→0.25
        } else {
            // Red window - heavy error
            return 0.36 + ((3 - meterAccuracy) * 0.21);  // 3→0.36, 0→1.0
        }
    }
    
    /**
     * Get base shot make chance based on distance and shot type
     */
    private double getBaseShotChance(double distance, boolean isLayup, boolean isMoving) {
        if (isLayup) {
            return 0.85;  // Open layup
        }
        
        if (distance <= 10.0) {
            // Close range / paint
            return 0.80;
        } else if (distance <= 15.0) {
            // Midrange
            return isMoving ? 0.68 : 0.75;
        } else if (distance <= 22.0) {
            // Three point range
            return isMoving ? 0.50 : 0.60;
        } else {
            // Deep three / logo shot
            return isMoving ? 0.35 : 0.45;
        }
    }
    
    /**
     * Calculate if shot should go in using new formula
     * Formula: FinalChance = Base * (1 - Contest*0.7) * (1 - TimingError²)
     * Perfect timing rule: Green + lightly contested = high% (scaled by distance)
     */
    private boolean calculateShotMake(int meterAccuracy, double contestPercentage, 
                                      double distance, boolean isLayup, boolean isMoving) {
        // Step 1: Get inputs
        double baseChance = getBaseShotChance(distance, isLayup, isMoving);
        double timingError = calculateTimingError(meterAccuracy);
        
        // Step 2: Calculate multipliers
        double contestMultiplier = 1.0 - (contestPercentage * CONTEST_STRENGTH);
        double timingMultiplier = 1.0 - (timingError * timingError);  // Square the error
        
        // Step 3: Calculate final chance
        double finalChance = baseChance * contestMultiplier * timingMultiplier;
        finalChance = Math.max(0.0, Math.min(finalChance, 0.95));  // Clamp to [0.0, 0.95]
        
        // Step 4: Perfect timing rule (green + lightly contested) - DISTANCE SCALED
        if (timingError <= GREEN_WINDOW_THRESHOLD && contestPercentage <= LIGHT_CONTEST_THRESHOLD) {
            // Scale perfect timing bonus by distance
            if (isLayup || distance <= 10.0) {
                finalChance = 0.98;  // Close range: near-guaranteed
            } else if (distance <= 15.0) {
                finalChance = 0.92;  // Midrange: very high
            } else if (distance <= 22.0) {
                finalChance = 0.90;  // Normal three: very high
            } else if (distance <= 27.0) {
                finalChance = 0.70;  // Deep three: good but risky
            } else {
                finalChance = 0.55;  // Logo range: still tough even on green
            }
        }
        
        // Debug logging
        System.out.println("[SHOT CALC] Distance: " + String.format("%.2f", distance) + 
                         " | Base: " + String.format("%.2f", baseChance * 100) + "%" +
                         " | Contest: " + String.format("%.2f", contestPercentage * 100) + "%" +
                         " | Timing: " + meterAccuracy + "/8" +
                         " | Final: " + String.format("%.2f", finalChance * 100) + "%");
        
        // Step 5: Roll the dice
        return Math.random() < finalChance;
    }

    private boolean calculateDunkSuccessWithMeter(int accuracy, DunkContestResult contestResult) {
        double contestPercentage = contestResult.getContestValue();

        // Use new shot formula for dunks
        // Dunks are treated as very close range shots (distance ~2.0)
        boolean isMoving = this.getCurrentDamager() != null && 
                          this.getCurrentDamager().getVelocity().length() > 0.2;
        
        // Use new formula (dunks count as super close shots)
        boolean shouldMake = calculateShotMake(accuracy, contestPercentage, 2.0, false, isMoving);
        
        // Posterizer override - guaranteed make
        if (contestResult.shouldPosterize()) {
            shouldMake = true;
        }
        
        return shouldMake;
    }
    private BlockResult checkForDunkBlock(Player dunker) {
        if (game == null) return new BlockResult(false, null);

        GoalGame.Team dunkerTeam = game.getTeamOf(dunker);
        if (dunkerTeam == null) return new BlockResult(false, null);

        final double blockRadius = 3.5;
        final double floorY = game.getArenaBox().getMinY() + 2.65;

        for (Player defender : dunker.getWorld().getPlayers()) {
            GoalGame.Team defenderTeam = game.getTeamOf(defender);

            if (defenderTeam != null && !defenderTeam.equals(dunkerTeam) && defenderTeam != GoalGame.Team.SPECTATOR) {
                double distance = defender.getLocation().distance(dunker.getLocation());

                if (distance > blockRadius) continue;

                boolean isAirborne = defender.getLocation().getY() > (floorY + 0.5);
                boolean clickedToBlock = defenderBlockAttempts.containsKey(defender.getUniqueId());

                // ONLY airborne blocks allowed
                if (isAirborne && clickedToBlock) {
                    long clickTime = defenderBlockAttempts.get(defender.getUniqueId());
                    long timeSinceClick = System.currentTimeMillis() - clickTime;

                    double heightDiff = defender.getLocation().getY() - dunker.getLocation().getY();

                    // Perfect timing - very high success rate
                    if (timeSinceClick < 400 && heightDiff > -0.6 && heightDiff < 1.4) {
                        if (Math.random() < 0.90) {
                            return new BlockResult(true, defender);
                        }
                    }
                    // Good timing - still high success
                    else if (timeSinceClick < 600 && heightDiff > -0.8 && heightDiff < 1.6) {
                        if (Math.random() < 0.70) {
                            return new BlockResult(true, defender);
                        }
                    }
                    // Late timing but still possible
                    else if (timeSinceClick < 800 && heightDiff > -1.0 && heightDiff < 1.8) {
                        if (Math.random() < 0.45) {
                            return new BlockResult(true, defender);
                        }
                    }
                }
            }
        }

        return new BlockResult(false, null);
    }
    public boolean crossover(Player player) {
        if (this.getCurrentDamager() != null && Math.abs(player.getVelocity().getY()) < 0.1 && player.isOnGround() && this.getCurrentDamager().equals(player)) {
            // NEW: Check for ankle break during crossover

            this.checkAnkleBreakerOnDribbleMove(player);

            if (this.btbActive) {
                return false;
            }

            int nextHand = this.handYaw + this.handModifier;
            if (!(nextHand < 20 && nextHand > -20)) {
                // Check if player is sprinting AND power crossover is available
                boolean isSprinting = player.isSprinting();
                boolean canPowerCrossover = this.powerCrossoverCooldown <= 0;

                double distance;
                if (isSprinting && canPowerCrossover) {
                    distance = 0.65;  // Power crossover (faster, longer cooldown)
                    this.powerCrossoverCooldown = 60; // 3 second cooldown (60 ticks)
                } else {
                    distance = 0.4;  // Normal crossover (no cooldown)
                }

                if (this.handModifier == 0) {
                    this.handModifier = 5;
                    player.setVelocity(Position.stabilize(player, 70.0f, 0.0).getDirection().multiply(distance * 1.5));
                    this.delay = 1;
                } else if (this.handModifier == 5) {
                    this.handModifier = -5;
                    this.delay = 1;
                    player.setVelocity(Position.stabilize(player, -70.0f, 0.0).getDirection().multiply(distance * 1.5));
                } else if (this.handModifier == -5) {
                    this.delay = 1;
                    player.setVelocity(Position.stabilize(player, 70.0f, 0.0).getDirection().multiply(distance * 1.5));
                    this.handModifier = 5;
                }

                return true;
            }
        }
        return false;
    }

    public boolean behindTheBack(Player player) {
        if (this.getCurrentDamager() != null && Math.abs(player.getVelocity().getY()) < 0.1 && player.isOnGround() && this.getCurrentDamager().equals(player)) {
            // NEW: Check for ankle break during BTB
            this.checkAnkleBreakerOnDribbleMove(player);

            int nextHand = this.handYaw + this.handModifier;
            if (!(nextHand < 49 && nextHand > -49)) {
                // Check BTB-specific cooldown
                if (this.btbDelay > 0) {
                    return false;
                }

                // Start BTB animation
                this.btbActive = true;
                this.btbAnimationTicks = 0;

                // Player movement - dash to the side
                if (this.handModifier == 5) {
                    // Ball on right, going to left
                    this.btbTargetHand = -5;
                    player.setVelocity(Position.stabilize(player, -70.0f, 0.0).getDirection().multiply(0.8));
                } else if (this.handModifier == -5) {
                    // Ball on left, going to right
                    this.btbTargetHand = 5;
                    player.setVelocity(Position.stabilize(player, 70.0f, 0.0).getDirection().multiply(0.8));
                } else if (this.handModifier == 0) {
                    // Ball centered, go to right
                    this.btbTargetHand = 5;
                    player.setVelocity(Position.stabilize(player, 70.0f, 0.0).getDirection().multiply(0.8));
                }

                this.delay = 10;
                this.btbDelay = 40;
                return true;
            }
        }
        return false;
    }

    public boolean hesi(Player player) {
        if (this.getCurrentDamager() != null && Math.abs(player.getVelocity().getY()) < 0.1 && player.isOnGround() && this.getCurrentDamager().equals(player)) {
            // NEW: Check for ankle break during hesi
            this.checkAnkleBreakerOnDribbleMove(player);

            if (this.btbActive) {
                return false;
            }

            int nextHand = this.handYaw + this.handModifier;
            if (!(nextHand < 49 && nextHand > -49)) {
                // Check hesi-specific cooldown
                if (this.hesiDelay > 0) {
                    return false;
                }

                // Start hesi animation (in-and-out)
                this.hesiActive = true;
                this.hesiAnimationTicks = 0;

                if (this.handModifier == 5) {
                    player.setVelocity(Position.stabilize(player, 70.0f, 0.0).getDirection().multiply(0.7));
                    this.delay = 2;
                    this.hesiDelay = 32; // 1.6 second cooldown
                } else if (this.handModifier == -5) {
                    player.setVelocity(Position.stabilize(player, -70.0f, 0.0).getDirection().multiply(0.7));
                    this.delay = 2;
                    this.hesiDelay = 32;
                } else if (this.handModifier == 0) {
                    player.setVelocity(Position.stabilize(player, 70.0f, 0.0).getDirection().multiply(0.7));
                    this.delay = 2;
                    this.hesiDelay = 32;
                }
                return true;
            }
        }
        return false;
    }

    public void giveaway() {
        this.removeCurrentDamager();
        // RESET shot tracking when ball is released
        this.shotDistance = 0.0;
        // CLEAR dunk travel flag when ball is released
        this.justDunkedPlayer = null;
    }

    private void runStealImmunity() {
        if (this.stealImmunityTicks > 0) {
            --this.stealImmunityTicks;
        }
    }

    private void handleSteal1v1(Player stealer) {
        if (this.game instanceof BasketballGame && ((BasketballGame) this.game).isHalfCourt1v1) {
            BasketballGame bbGame = (BasketballGame) this.game;
            // The player who stole should get ball with offensive advantage
            System.out.println("1v1 Steal: " + stealer.getName() + " stole the ball");
            bbGame.resetHalfCourt1v1PositionsWithOffense(stealer);
        }
    }

    public void takeBall(Player player) {
        // PREVENT STEALING FROM INBOUNDER (but only from opposing team)
        if (this.getCurrentDamager() != null && this.game.inboundingActive) {
            Player currentHolder = this.getCurrentDamager();
            if (currentHolder.equals(this.game.inbounder)) {
                GoalGame.Team holderTeam = this.game.getTeamOf(currentHolder);
                GoalGame.Team playerTeam = this.game.getTeamOf(player);

                if (holderTeam != null && playerTeam != null && !holderTeam.equals(playerTeam)) {
                    player.playSound(player.getLocation(), Sound.ENTITY_ARMOR_STAND_BREAK,
                            SoundCategory.MASTER, 100.0f, 1.0f);
                    player.sendMessage(Component.text("Can't steal from inbounder!").color(Colour.deny()));
                    return;
                }
            }
        }

        if (this.stealImmunityTicks > 0 && this.getCurrentDamager() != null &&
                !this.getCurrentDamager().equals(player)) {
            player.playSound(player.getLocation(), Sound.ENTITY_ARMOR_STAND_BREAK,
                    SoundCategory.MASTER, 100.0f, 1.0f);
            return;
        }

        // ===== FIX: CHECK GLOBAL STEAL COOLDOWN =====
        if (this.globalStealCooldown > 0 && this.getCurrentDamager() != null &&
                !this.getCurrentDamager().equals(player)) {
            int secondsLeft = (int) Math.ceil(this.globalStealCooldown / 20.0);
            player.playSound(player.getLocation(), Sound.ENTITY_ARMOR_STAND_BREAK,
                    SoundCategory.MASTER, 100.0f, 1.0f);
            return;
        }

        if (this.delay < 1 && this.getStealDelay() < 1) {
            if (this.getLocation().getY() > player.getEyeLocation().getY() - 0.1 &&
                    this.getVelocity().getY() < 0.0 && this.getCurrentDamager() == null) {
                player.playSound(player.getLocation(), Sound.ENTITY_ARMOR_STAND_BREAK,
                        SoundCategory.MASTER, 100.0f, 1.0f);
                this.setStealDelay(10);
                return;
            }

            // USE THE OLD STEAL METHOD
            this.steal(player);

            // HANDLE REBOUND PICKUP
            if (this.isReboundEligible()) {
                this.setReboundEligible(false);
                if (this.game.getState().equals(GoalGame.State.REGULATION) ||
                        this.game.getState().equals(GoalGame.State.OVERTIME)) {
                    PlayerStats stats = this.game.getStatsManager().getPlayerStats(player.getUniqueId());
                    stats.incrementRebounds();
                    player.sendMessage(Component.text("Rebound!").color(Colour.allow()));
                    System.out.println("Debug: " + player.getName() + " grabbed a rebound! Total rebounds: " +
                            stats.getRebounds());
                }
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
                this.passDelay = 5;
                this.catchDelay = 10;
                
                // Grant 3.5 second steal immunity after rebound
                this.stealImmunityTicks = 70;
            }

            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 10, 1));
        } else {
            player.playSound(player.getLocation(), Sound.ENTITY_ARMOR_STAND_BREAK,
                    SoundCategory.MASTER, 100.0f, 1.0f);
        }
    }

    public void error(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_ARMOR_STAND_BREAK, SoundCategory.MASTER, 100.0f, 1.0f);
    }
    public void stepback(Player player) {
        if (this.getCurrentDamager() != null && Math.abs(player.getVelocity().getY()) < 0.1 && player.isOnGround() && this.getCurrentDamager().equals(player)) {

            // Check stepback cooldown
            if (this.stepbackDelay > 0) {
                player.sendActionBar(Component.text("Stepback on cooldown: " + (this.stepbackDelay / 20) + "s")
                        .color(NamedTextColor.RED));
                return;
            }
            
            // Check if player is looking in general direction of target hoop
            Location targetHoop = this.getTargetHoop(player);
            Vector toHoop = targetHoop.toVector().subtract(player.getLocation().toVector()).setY(0).normalize();
            Vector playerDirection = player.getLocation().getDirection().clone().setY(0).normalize();
            double dotProduct = playerDirection.dot(toHoop);
            
            // Stricter threshold: must be facing within ~80 degrees of hoop (dot > 0.2)
            if (dotProduct < 0.2) {
                player.sendActionBar(Component.text("Must face target hoop to stepback")
                        .color(NamedTextColor.RED));
                return;
            }

            // Get player's current input state from ProtocolLib
            PlayerInputTracker.InputState input = PlayerInputTracker.getInput(player);
            
            // Get player's facing direction for relative movement
            Location currentLoc = player.getLocation();
            Vector facingDir = currentLoc.getDirection().clone().setY(0).normalize();
            Vector rightDir = new Vector(-facingDir.getZ(), 0, facingDir.getX()).normalize();
            
            // Detect which key is being pressed and execute corresponding move
            if (input.forward) {
                // FORWARD (W) - HOPSTEP
                executeHopstep(player, facingDir, rightDir);
                return;
            } else if (input.backward) {
                // BACKWARD (S) - Standard stepback
                this.delay = 10;
                player.setVelocity(facingDir.multiply(-0.75));
                
                Bukkit.getScheduler().runTaskLater(Partix.getInstance(), () -> {
                    if (this.getCurrentDamager() != null && this.getCurrentDamager().equals(player)) {
                        this.game.setStepbacked(player.getUniqueId());
                    }
                }, 10L);
                
                this.stepbackDelay = 20;
                return;
            } else if (input.left) {
                // LEFT (A) - Left stepback
                this.delay = 10;
                Vector leftDir = rightDir.clone().multiply(-1);
                player.setVelocity(leftDir.multiply(0.75));
                
                Bukkit.getScheduler().runTaskLater(Partix.getInstance(), () -> {
                    if (this.getCurrentDamager() != null && this.getCurrentDamager().equals(player)) {
                        this.game.setStepbacked(player.getUniqueId());
                    }
                }, 10L);
                
                this.stepbackDelay = 20;
                return;
            } else if (input.right) {
                // RIGHT (D) - Right stepback
                this.delay = 10;
                player.setVelocity(rightDir.multiply(0.75));
                
                Bukkit.getScheduler().runTaskLater(Partix.getInstance(), () -> {
                    if (this.getCurrentDamager() != null && this.getCurrentDamager().equals(player)) {
                        this.game.setStepbacked(player.getUniqueId());
                    }
                }, 10L);
                
                this.stepbackDelay = 20;
                return;
            }
            
            // No movement key pressed - do nothing
        }
    }
    
    private void executeHopstep(Player player, Vector facingDir, Vector rightDir) {
        // HOPSTEP: Move forward and to the side based on ball hand, pop up in air
        this.delay = 10;
        this.hopstepActive = true;
        
        // Determine direction and switch hands using same logic as crossover
        Vector hopstepDir;
        if (this.handModifier == 0) {
            // Ball centered - hopstep forward-RIGHT, switch to RIGHT hand
            this.handModifier = 5;
            this.handYaw = 0;
            hopstepDir = facingDir.clone().add(rightDir).normalize();
        } else if (this.handModifier == 5) {
            // Ball in RIGHT hand - hopstep forward-LEFT, switch to LEFT hand
            this.handModifier = -5;
            this.handYaw = 0;
            Vector leftDir = rightDir.clone().multiply(-1);
            hopstepDir = facingDir.clone().add(leftDir).normalize();
        } else {
            // Ball in LEFT hand - hopstep forward-RIGHT, switch to RIGHT hand
            this.handModifier = 5;
            this.handYaw = 0;
            hopstepDir = facingDir.clone().add(rightDir).normalize();
        }
        
        // Apply horizontal movement (2 blocks max)
        hopstepDir.multiply(0.7);
        
        // Add vertical component (half jump height = ~0.21)
        hopstepDir.setY(0.21);
        
        player.setVelocity(hopstepDir);
        
        // Start travel tracking - player can walk 2 blocks before it's a travel
        this.hopstepTravelCheck = true;
        this.hopstepStartLocation = null; // Will be set when they land
        this.hopstepDistanceTraveled = 0.0;
        this.hopstepCompletionTicks = 0; // Reset counter
        
        this.stepbackDelay = 20;
    }

    public boolean collides(Player player) {
        if (this.stealImmunityTicks > 0 && this.getCurrentDamager() != null && !this.getCurrentDamager().equals(player)) {
            return false;
        }
        if (BallFactory.hasBall(player)) {
            return true;
        }

        final double floorY = game.getArenaBox().getMinY() + 2.5;

        // ===== BLOCKING LOGIC - Only active when ball is in the air AND was recently shot =====
        if (this.getCurrentDamager() == null && this.getLastDamager() != null && this.delay > 0) {
            // Ball was recently released (shot/passed) and is in flight
            if (player.getLocation().getY() > floorY && this.getLocation().getY() > floorY + 0.5) {

                Player shooter = this.getLastDamager();
                GoalGame.Team shooterTeam = game.getTeamOf(shooter);
                GoalGame.Team blockerTeam = game.getTeamOf(player);

                // Only allow block if teams are different and blocker isn't a spectator
                if (shooterTeam != null && blockerTeam != null &&
                        !shooterTeam.equals(blockerTeam) &&
                        blockerTeam != GoalGame.Team.SPECTATOR) {

                    Location main = this.getLocation().clone();
                    main.setPitch(0.0f);
                    main.setYaw(main.getYaw() + (float)(Math.random() * 7.0 - Math.random() * 7.0));
                    this.setVelocity(main.getDirection().multiply(-0.6));

                    // Increment block stats
                    if (this.game.getState().equals(GoalGame.State.REGULATION) || this.game.getState().equals(GoalGame.State.OVERTIME)) {
                        PlayerStats blockerStats = this.game.getStatsManager().getPlayerStats(player.getUniqueId());
                        blockerStats.incrementBlocks();
                    }

                    player.sendMessage(Component.text("BLOCK!").color(Colour.allow()).decorate(TextDecoration.BOLD));
                    if (shooter != null) {
                        shooter.sendMessage(Component.text("BLOCKED by " + player.getName() + "!").color(Colour.deny()).decorate(TextDecoration.BOLD));
                    }

                    player.playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, SoundCategory.MASTER, 1.0f, 1.2f);

                    this.lastShotBlockerUUID = player.getUniqueId();

                    return false; // Block successful, don't give possession
                }
            }
        }

        // ===== LAYUP BLOCKING LOGIC =====
        if (this.isLayupAttempt && this.getCurrentDamager() == null && this.getLastDamager() != null) {
            Player shooter = this.getLastDamager();
            GoalGame.Team shooterTeam = game.getTeamOf(shooter);
            GoalGame.Team blockerTeam = game.getTeamOf(player);

            if (shooterTeam != null && blockerTeam != null &&
                    !shooterTeam.equals(blockerTeam) &&
                    blockerTeam != GoalGame.Team.SPECTATOR) {

                double ballHeight = this.getLocation().getY();
                double floorY2 = game.getArenaBox().getMinY() + 2.5;

                if (ballHeight > floorY2 + 0.5 && ballHeight < floorY2 + 4.0) {
                    double distance = player.getLocation().distance(this.getLocation());

                    if (distance <= 2.0) {
                        Vector blockDirection = player.getLocation().getDirection().normalize();
                        blockDirection.setY(0.3);

                        this.setVelocity(blockDirection.multiply(0.45), 0.25);

                        player.sendMessage(Component.text("LAYUP BLOCKED!").color(Colour.allow()).decorate(TextDecoration.BOLD));
                        shooter.sendMessage(Component.text("Layup blocked by " + player.getName() + "!").color(Colour.deny()).decorate(TextDecoration.BOLD));

                        player.playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, SoundCategory.MASTER, 1.0f, 1.2f);
                        shooter.playSound(shooter.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, SoundCategory.MASTER, 1.0f, 0.8f);

                        if (this.game.getState().equals(GoalGame.State.REGULATION) ||
                                this.game.getState().equals(GoalGame.State.OVERTIME)) {
                            PlayerStats blockerStats = this.game.getStatsManager().getPlayerStats(player.getUniqueId());
                            blockerStats.incrementBlocks();
                        }

                        this.setReboundEligible(false);
                        this.isLayupAttempt = false;
                        this.setLocked(false);
                        this.lastShotBlockerUUID = player.getUniqueId();

                        return false; // Block successful, don't give possession
                    }
                }
            }
        }

        // ===== COLLISION STEAL LOGIC =====
        if (this.getCurrentDamager() != null && this.getCurrentDamager().isOnGround()) {
            Player ballHandler = this.getCurrentDamager();
            GoalGame.Team handlerTeam = game.getTeamOf(ballHandler);
            GoalGame.Team defenderTeam = game.getTeamOf(player);

            if (handlerTeam != null && defenderTeam != null &&
                    !handlerTeam.equals(defenderTeam) &&
                    !player.equals(ballHandler) &&
                    defenderTeam != GoalGame.Team.SPECTATOR) {

                if (game.inboundingActive && ballHandler.equals(game.inbounder)) {
                    return false; // Inbounder protected
                }

                if (this.attemptCollisionSteal(player)) {
                    return false; // Steal successful, ball is loose
                } else {
                    return false; // Steal failed, don't give possession
                }
            }
        }

        // ===== NORMAL POSSESSION PICKUP =====
        // If we get here: no damager, no recent shot, no blocking opportunity
        if (this.getCurrentDamager() == null) {
            this.setDamager(player);
            this.stealImmunityTicks = 20;
            return true; // Give possession
        }

        // Default: don't give possession
        return false;
    }

    @Override
    public void setDamager(Player player) {
        // CRITICAL: Capture lob flags FIRST before anything clears them
        boolean isLobCatch = this.isLobPass;
        UUID captureLobPasserUUID = this.lobPasserUUID;
        Player lobPasser = captureLobPasserUUID != null ? Bukkit.getPlayer(captureLobPasserUUID) : null;

        System.out.println("=== SETDAMAGER DEBUG ===");
        System.out.println("Player catching: " + player.getName());
        System.out.println("isLobPass flag: " + isLobCatch);
        System.out.println("lobPasser: " + (lobPasser != null ? lobPasser.getName() : "NULL"));

        // Check if offensive player (same team as passer)
        boolean isOffensivePlayer = false;
        if (lobPasser != null) {
            GoalGame.Team passerTeam = this.game.getTeamOf(lobPasser);
            GoalGame.Team catcherTeam = this.game.getTeamOf(player);
            System.out.println("Passer team: " + (passerTeam != null ? passerTeam.name() : "NULL"));
            System.out.println("Catcher team: " + (catcherTeam != null ? catcherTeam.name() : "NULL"));
            isOffensivePlayer = passerTeam != null && catcherTeam != null && passerTeam.equals(catcherTeam);
            System.out.println("isOffensivePlayer: " + isOffensivePlayer);
        }

        this.game.setStepbacked(null);
        
        // Clear hopstep travel tracking when picking up ball
        this.hopstepTravelCheck = false;
        this.hopstepStartLocation = null;
        this.hopstepCompletionTicks = 0;
        
        super.setDamager(player);
        
        // Prevent immediate travel detection when picking up ball while in air (landing from jump)
        this.delay = 10; // 0.5 seconds of travel immunity after ball pickup

        // ===== MARK PLAYER AS INBOUNDER WHEN THEY PICK UP BALL =====
        // CRITICAL FIX: Only set a player as the inbounder if they DON'T already have possession
        // This prevents players from being stuck as "inbounder" after the inbound pass completes
        if (this.game.inboundingActive && this.game.inboundingTeam != null && this.getCurrentDamager() == null) {
            GoalGame.Team playerTeam = this.game.getTeamOf(player);
            // Check if player is on the inbounding team
            if (playerTeam != null && playerTeam.equals(this.game.inboundingTeam)) {
                this.game.inbounder = player; // Mark this player as the inbounder
                System.out.println("Set " + player.getName() + " as inbounder (picking up ball at inbound spot)");
            }
        } else if (this.game.inboundingActive && this.game.inbounder != null && !this.game.inbounder.equals(player)) {
            // If a DIFFERENT player is getting possession while inbounding is active, clear the inbound state
            System.out.println("Clearing inbound state - " + player.getName() + " received ball from inbounder " + this.game.inbounder.getName());
            this.game.clearInboundStateIfNotInbounder(player);
        }

        this.setCurrentDamager(player);
        this.shouldPreventScore = false;
        this.justDunkedPlayer = null;
        this.catchDelay = 5;
        this.layupScored = false;
        this.isLayupAttempt = false;
        this.shotDistance = 0.0;

        // Clear direction locks when possession changes
        this.directionLockUntil.clear();
        this.lockedDirection.clear();
        this.currentTrackPassTarget = null;
        // REMOVED: assignTeammateHotkey - hotkeys are now stable throughout the game

        // Reset pass mode to BULLET when picking up ball
        this.passMode = PassMode.BULLET;
        player.sendActionBar(Component.text("Pass Mode: ").color(NamedTextColor.WHITE)
                .append(Component.text(this.passMode.getDisplayName()).color(NamedTextColor.GREEN)));

        // CRITICAL FIX FOR POKE POSSESSION:
        if (this.wasPoked && this.lastPossessionBeforePoke != null) {
            if (player.getUniqueId().equals(this.lastPossessionBeforePoke)) {
                this.lastPossessorUUID = player.getUniqueId();
                this.wasPoked = false;
                this.lastPossessionBeforePoke = null;
                System.out.println(player.getName() + " recovered their own poked ball - possession changed");
            } else {
                this.lastPossessorUUID = player.getUniqueId();
                System.out.println(player.getName() + " (poker) picked up poked ball - victim still has OOB possession");
            }
        } else {
            this.lastPossessorUUID = player.getUniqueId();
            this.wasPoked = false;
            this.lastPossessionBeforePoke = null;
        }

        // ===== AUTO-START DUNK METER FOR LOB CATCHES =====
        // This runs IMMEDIATELY when possession is given, not waiting for collision
        // CRITICAL: Use captured flags, not the instance variables (which may be cleared)
        if (isLobCatch && isOffensivePlayer) {
            System.out.println("LOB CATCH DETECTED - Checking conditions...");

            // CONDITION 1: Player must be AIRBORNE when catching
            final double floorY = game.getArenaBox().getMinY() + 2.5;
            boolean isAirborne = player.getLocation().getY() > (floorY + 0.5);
            System.out.println("Player height: " + player.getLocation().getY() + ", Floor: " + (floorY + 0.5) + ", Airborne: " + isAirborne);

            if (!isAirborne) {
                // Player caught on ground - no auto dunk meter
                System.out.println(player.getName() + " caught lob on ground - no auto dunk meter");
            } else {
                // CONDITION 2: Check if player is in dunk range (ONLY if different player caught it)
                boolean isDifferentPlayer = lobPasser != null && !player.equals(lobPasser);

                if (isDifferentPlayer) {
                    Location targetHoop = this.getTargetHoop(player);
                    Location hoopLocation = targetHoop.clone();
                    hoopLocation.setY(player.getLocation().getY());
                    double horizontalDistance = hoopLocation.distance(player.getLocation());
                    System.out.println("Horizontal distance to hoop: " + String.format("%.2f", horizontalDistance));

                    if (horizontalDistance > 9) {
                        // Too far from rim - no auto dunk meter
                        System.out.println(player.getName() + " caught lob too far from rim (" +
                                String.format("%.2f", horizontalDistance) + " blocks) - no auto dunk meter");
                    } else {
                        // ALL CONDITIONS MET: Airborne + In dunk range + On offense + Different player
                        System.out.println("ALL CONDITIONS MET - ACTIVATING DUNK METER");
                        this.dunkMeterActive = true;
                        this.dunkMeterAccuracy = 0;
                        this.dunkMeterWait = 0;
                        this.dunkMeterForward = true;
                        this.isDunkAttempt = true;

                        player.sendMessage(Component.text("LOB DUNK! Jump and press Q to finish!", NamedTextColor.GOLD)
                                .decorate(TextDecoration.BOLD));
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING,
                                SoundCategory.MASTER, 1.0f, 2.0f);

                        System.out.println("Auto-started dunk meter for " + player.getName() +
                                " (lob catch - airborne, in range " + String.format("%.2f", horizontalDistance) + " blocks)");
                    }
                } else {
                    // Same player threw and caught - allow self-lob dunk meter without distance check
                    System.out.println("Self-lob caught - ACTIVATING DUNK METER (no distance check)");
                    this.dunkMeterActive = true;
                    this.dunkMeterAccuracy = 0;
                    this.dunkMeterWait = 0;
                    this.dunkMeterForward = true;
                    this.isDunkAttempt = true;

                    player.sendMessage(Component.text("LOB DUNK! Jump and press Q to finish!", NamedTextColor.GOLD)
                            .decorate(TextDecoration.BOLD));
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING,
                            SoundCategory.MASTER, 1.0f, 2.0f);
                }
            }
        } else {
            System.out.println("LOB CATCH CHECK FAILED - isLobCatch: " + isLobCatch + ", isOffensivePlayer: " + isOffensivePlayer);
        }

        // NOW clear lob flags AFTER dunk meter check
        this.isLobPass = false;
        this.lobPasserUUID = null;

        this.wasShot = false;
    }

    // ===== ADD THIS METHOD TO TOGGLE PASS MODE: =====
    public void togglePassMode(Player player) {
        if (this.getCurrentDamager() == null || !this.getCurrentDamager().equals(player)) {
            return;
        }

        this.passMode = this.passMode.toggle();

        // REMOVED: The action bar display that was conflicting with shot clock
        // Just play a sound to confirm the toggle
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 0.5f, 1.0f);

        // Optional: Send a subtle message instead (less intrusive than action bar)
        player.sendMessage(Component.text("Pass Mode: ").color(NamedTextColor.WHITE)
                .append(Component.text(this.passMode.getDisplayName()).color(NamedTextColor.GREEN)));
    }

    private void detectTravel() {
        Player damager = this.getCurrentDamager();

        // CRITICAL: Detect if player jumped with dunk meter but landed without releasing
        if (damager != null && this.dunkMeterActive) {
            // Check if they jumped (were airborne) and came back down
            if (damager.getVelocity().getY() < -0.1 && damager.isOnGround()) {
                // They jumped with dunk meter active and landed without releasing Q - TRAVEL!
                damager.sendTitlePart(TitlePart.TITLE, Component.text(" "));
                damager.sendTitlePart(TitlePart.SUBTITLE, Component.text("Travel! (Failed dunk attempt)").style(Style.style(Colour.deny(), TextDecoration.BOLD)));

                // Cancel the dunk meter
                this.dunkMeterActive = false;
                this.dunkMeterAccuracy = 0;
                this.isDunkAttempt = false;

                // Handle travel based on game state
                this.handleTravel(damager);

                System.out.println("Travel called - player jumped for dunk but landed without releasing");
                return;
            }
        }

        // Regular travel detection
        if (this.delay < 1 && this.stealImmunityTicks < 1 && damager != null && !damager.isOnGround() && damager.getVelocity().getY() < -0.19) {

            // DON'T call travel during dunk attempts (while meter is active)
            if (this.isDunkAttempt || this.dunkMeterActive) {
                return;
            }

            // Check if player is close to the ground (within 1.5 blocks)
            double distanceToGround = damager.getLocation().getY() - damager.getLocation().getBlock().getY();

            // Only call travel if they're falling AND close to ground (not during high jumps/dunks)
            if (distanceToGround <= 1.5) {
                damager.sendTitlePart(TitlePart.TITLE, Component.text(" "));
                damager.sendTitlePart(TitlePart.SUBTITLE, Component.text("Travel!").style(Style.style(Colour.deny(), TextDecoration.BOLD)));
                
                // Handle travel based on game state
                this.handleTravel(damager);
                
                if (this.game.inboundingActive && damager.equals(this.game.inbounder)) {
                    this.game.dropInboundBarrierButKeepClockFrozen();
                }
            }
        }
    }
    
    private void handleTravel(Player traveler) {
        // Check if we're in a live game (not pregame)
        if (this.game instanceof BasketballGame bbGame) {
            GoalGame.State state = bbGame.getState();
            
            // During live game - trigger turnover
            if (state.equals(GoalGame.State.REGULATION) || state.equals(GoalGame.State.OVERTIME)) {
                System.out.println("Travel during live game - triggering turnover");
                
                // Add turnover stat
                bbGame.getStatsManager().getPlayerStats(traveler.getUniqueId()).incrementTurnovers();
                
                // Handle based on game type
                if (bbGame.isHalfCourt1v1) {
                    // For 1v1: Reset with other player getting ball
                    List<Player> allPlayers = new ArrayList<>();
                    allPlayers.addAll(bbGame.getHomePlayers());
                    allPlayers.addAll(bbGame.getAwayPlayers());
                    
                    if (allPlayers.size() == 2) {
                        Player otherPlayer = allPlayers.get(0).equals(traveler) ?
                                allPlayers.get(1) : allPlayers.get(0);
                        
                        System.out.println("1v1 Travel: " + traveler.getName() + " → Ball to " + otherPlayer.getName());
                        bbGame.resetHalfCourt1v1PositionsWithOffense(otherPlayer);
                        return;
                    }
                }
                
                // For regular games: Determine which team gets the ball (opposite of traveler's team)
                GoalGame.Team inboundingTeam;
                if (bbGame.getHomePlayers().contains(traveler)) {
                    inboundingTeam = GoalGame.Team.AWAY;
                } else {
                    inboundingTeam = GoalGame.Team.HOME;
                }
                
                // Use the public method to handle travel turnover inbound
                bbGame.handleTravelTurnover(inboundingTeam);
                return;
            }
        }
        
        // Pregame or other states - just throw the ball
        this.forceThrow();
    }
    private void runDelay() {
        if (this.delay > 0) {
            --this.delay;
        }
        if (this.hesiDelay > 0) {
            --this.hesiDelay;
        }
        if (this.powerCrossoverCooldown > 0) {
            --this.powerCrossoverCooldown;
        }
        if (this.btbDelay > 0) {
            --this.btbDelay;
        }
        if (this.passDelay > 0) {
            --this.passDelay;
        }
        if (this.globalStealCooldown > 0) {
            --this.globalStealCooldown;
        }

        // Track defender proximity for late closeout detection
        if (this.getCurrentDamager() != null) {
            Player ballHandler = this.getCurrentDamager();
            GoalGame.Team handlerTeam = game.getTeamOf(ballHandler);
            
            if (handlerTeam != null) {
                for (Player player : ballHandler.getWorld().getPlayers()) {
                    GoalGame.Team playerTeam = game.getTeamOf(player);
                    if (playerTeam != null && !playerTeam.equals(handlerTeam) && playerTeam != GoalGame.Team.SPECTATOR) {
                        double distance = player.getLocation().distance(ballHandler.getLocation());
                        if (distance <= 5.0) {
                            // Defender is near - update timestamp
                            defenderProximityTracking.put(player.getUniqueId(), System.currentTimeMillis());
                        }
                    }
                }
            }
        }
        // NEW: Decrement catch delay
        if (this.catchDelay > 0) {
            --this.catchDelay;

        }
        // NEW: Add this line to decrement stepback cooldown
        if (this.stepbackDelay > 0) {
            --this.stepbackDelay;


        }
    }

    private void modifyHand() {
        // Don't modify hand position during special animations (but allow during hopstep)
        if (this.btbActive || this.hesiActive) {
            return;
        }

        int nextHand = this.handYaw + this.handModifier;
        if (nextHand < 50 && nextHand > -50) {
            this.handYaw = nextHand;
        }
    }

    public void markReboundEligible() {
        this.reboundEligible = true;
    }

    private boolean isLocationBehindBackboard(Location ballLoc, Player shooter) {
        Location backboard = getBackboardLocation(shooter);
        Location targetHoop = getTargetHoop(shooter);
        Location centerCourt = this.game.getCenter();

        // Determine which direction is "behind" the backboard
        boolean isAwayNet = targetHoop.getZ() < centerCourt.getZ();

        double backboardZ = backboard.getZ();
        double ballZ = ballLoc.getZ();
        double hoopZ = targetHoop.getZ();

        // Check if ball is on the wrong side of the backboard
        if (isAwayNet) {
            // Away net: backboard is at lower Z, ball shouldn't go below it
            return ballZ < (backboardZ - 0.2); // 0.2 tolerance
        } else {
            // Home net: backboard is at higher Z, ball shouldn't go above it
            return ballZ > (backboardZ + 0.2); // 0.2 tolerance
        }
    }

    private void checkBackboardCollision() {
        // Only check during shots (ball in air, no damager)
        if (this.getCurrentDamager() != null) {
            return; // Not in flight
        }

        Location ballLoc = this.getLocation();
        if (ballLoc == null || ballLoc.getWorld() == null) {
            return;
        }

        // Get both hoop locations and calculate backboard positions
        Location homeHoopCenter = this.game.getHomeNet().getCenter().toLocation(ballLoc.getWorld());
        Location awayHoopCenter = this.game.getAwayNet().getCenter().toLocation(ballLoc.getWorld());

        // Backboard positions (behind the rims)
        Location homeBackboard = homeHoopCenter.clone().add(0, 0.5, 1.2);
        Location awayBackboard = awayHoopCenter.clone().add(0, 0.5, -1.2);

        double backboardThickness = 0.5; // INCREASED from 0.3
        double backboardRadius = 1.2; // INCREASED from 1.0

        // Check home backboard collision
        if (isCollisionWithBackboard(ballLoc, homeBackboard, backboardThickness, backboardRadius)) {
            // CRITICAL: Check if ball is going THROUGH backboard (not bouncing off front)
            if (isBallGoingThroughBackboard(ballLoc, homeBackboard, homeHoopCenter)) {
                // Stop the ball completely
                this.setVelocity(new Vector(0, -0.5, 0)); // Just drop it
                return;
            }
            handleBackboardBounce(ballLoc, homeBackboard);
            return;
        }

        // Check away backboard collision
        if (isCollisionWithBackboard(ballLoc, awayBackboard, backboardThickness, backboardRadius)) {
            if (isBallGoingThroughBackboard(ballLoc, awayBackboard, awayHoopCenter)) {
                this.setVelocity(new Vector(0, -0.5, 0));
                return;
            }
            handleBackboardBounce(ballLoc, awayBackboard);
            return;
        }
    }

    private boolean isBallGoingThroughBackboard(Location ballLoc, Location backboardLoc, Location hoopLoc) {
        // Check if ball is behind the backboard (on wrong side)
        double ballZ = ballLoc.getZ();
        double backboardZ = backboardLoc.getZ();
        double hoopZ = hoopLoc.getZ();

        // If backboard is between ball and hoop, ball is on correct side
        // If ball is between backboard and "behind", ball went through

        if (hoopZ > backboardZ) {
            // Home net: hoop is at higher Z than backboard
            return ballZ > backboardZ + 0.3; // Ball is behind backboard
        } else {
            // Away net: hoop is at lower Z than backboard
            return ballZ < backboardZ - 0.3; // Ball is behind backboard
        }
    }

    private boolean isCollisionWithBackboard(Location ballLoc, Location backboardLoc, double thickness, double radius) {
        // Check if ball is within the backboard collision zone
        double distX = Math.abs(ballLoc.getX() - backboardLoc.getX());
        double distY = Math.abs(ballLoc.getY() - backboardLoc.getY());
        double distZ = Math.abs(ballLoc.getZ() - backboardLoc.getZ());

        // Backboard collision box: thin in Z direction, wide in X and Y
        return distX <= radius && distY <= radius && distZ <= thickness;
    }

    private void handleBackboardBounce(Location ballLoc, Location backboardLoc) {
        // Get current velocity
        Vector velocity = this.getVelocity().clone();

        // Determine which direction to bounce based on ball position relative to backboard
        double relativeZ = ballLoc.getZ() - backboardLoc.getZ();

        // Reverse Z velocity (bounce off backboard)
        if (Math.abs(relativeZ) < 0.15) {
            // Ball is hitting the backboard directly
            velocity.setZ(-velocity.getZ() * 0.6); // Reverse with dampening
        }

        // Reduce other velocity components slightly
        velocity.setX(velocity.getX() * 0.85);
        velocity.setY(Math.max(velocity.getY() * 0.9, -0.3)); // Keep some downward momentum

        // Apply bounced velocity
        this.setVelocity(velocity);

        // Play sound effect
        this.getLocation().getWorld().playSound(this.getLocation(),
                Sound.BLOCK_WOOD_HIT, SoundCategory.MASTER, 0.7f, 0.9f);
    }
    private void detectReboundPickup() {
        if (!this.game.getState().equals(GoalGame.State.REGULATION)) {
            return;
        }
        if (this.isReboundEligible() && this.getCurrentDamager() != null) {
            Player rebounder = this.getCurrentDamager();
            PlayerStats stats = this.game.getStatsManager().getPlayerStats(rebounder.getUniqueId());
            stats.incrementRebounds();
            this.game.getStatsManager().updatePlayerStats(rebounder.getUniqueId(), stats);
            rebounder.sendMessage(Component.text("Rebound!").color(Colour.allow()));
            rebounder.playSound(rebounder.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
            this.setReboundEligible(false);

            // CRITICAL: Prevent defensive rebound from accidentally scoring
            this.shouldPreventScore = true;

            this.game.cancelAssistTimer();
        }
    }

    public boolean isBallNearGoal(Location goalLocation) {
        Location ballLocation = this.getLocation();
        if (ballLocation == null || goalLocation == null || !ballLocation.getWorld().equals(goalLocation.getWorld())) {
            return false;
        }
        double proximityThreshold = 1.0;
        return ballLocation.distance(goalLocation) <= proximityThreshold;
    }

    private void postModify() {
        this.detectTravel();
        this.modifyHand();

    }

    private void animateHesi() {
        if (!this.hesiActive) {
            return;
        }

        this.hesiAnimationTicks++;

        // REVERTED: Hesi animation lasts 8 ticks (0.4 seconds)
        if (this.hesiAnimationTicks >= 8) {
            this.hesiActive = false;
            this.hesiAnimationTicks = 0;
        }
    }
    public boolean onDunkRelease(Player player) {
        if (this.dunkMeterActive && this.getCurrentDamager() != null && this.getCurrentDamager().equals(player)) {
            System.out.println("Q RELEASED - Executing dunk!");
            return this.executeDunk(player);
        }
        return false;
    }
    private void animateBTB() {
        if (!this.btbActive) {
            return;
        }

        this.btbAnimationTicks++;

        // REVERTED: BTB animation lasts 12 ticks (0.6 seconds)
        if (this.btbAnimationTicks >= 12) {
            this.btbActive = false;
            this.handModifier = this.btbTargetHand;
            this.handYaw = 0;
            this.btbAnimationTicks = 0;
        }
    }
    private void displayAccuracy() {
        if (this.getCurrentDamager() != null) {
            final Player damager = this.getCurrentDamager();
            if (!Partix.getInstance().getRightClickManager().isHolding(damager)) return;

            final GoalGame.Team opponentTeam = this.game.getTeamOf(damager) == GoalGame.Team.HOME
                    ? GoalGame.Team.AWAY
                    : GoalGame.Team.HOME;
            final Location targetHoop = opponentTeam == GoalGame.Team.HOME
                    ? this.game.getHomeNet().getCenter().toLocation(damager.getWorld())
                    : this.game.getAwayNet().getCenter().toLocation(damager.getWorld());

            double distance = this.getCurrentDamager().getLocation().distance(targetHoop);
            updateGreenWindow(distance);

            String configKey = distanceZone.name() + "." + this.accuracy;
            String unicode = Partix.getInstance().getUnicodeConfig().getConfig().getString(configKey);

            TextComponent a = Component.text(unicode, NamedTextColor.WHITE).shadowColor(ShadowColor.none());

            this.getCurrentDamager().sendTitlePart(TitlePart.TITLE, a);
            this.getCurrentDamager().sendTitlePart(TitlePart.SUBTITLE, Component.text("   "));
            this.getCurrentDamager().sendTitlePart(TitlePart.TIMES, Title.Times.times(Duration.ofMillis(0L), Duration.ofMillis(100L), Duration.ofMillis(350L)));
        }
    }

    // Also add debug to displayDunkMeter():
    private void displayDunkMeter() {
        if (this.getCurrentDamager() == null || !this.dunkMeterActive) {
            return;
        }

        final Player damager = this.getCurrentDamager();

        // Use LONG distance zone config (harder green window)
        String configKey = "LONG." + this.dunkMeterAccuracy;
        String unicode = Partix.getInstance().getUnicodeConfig().getConfig().getString(configKey);

        if (unicode == null) {
            unicode = "||||||||".substring(0, Math.min(this.dunkMeterAccuracy + 1, 8));
        }

        TextComponent meter = Component.text(unicode, NamedTextColor.WHITE).shadowColor(ShadowColor.none());

        // Send ONLY the meter (no subtitle)
        damager.sendTitlePart(TitlePart.TITLE, meter);
        damager.sendTitlePart(TitlePart.SUBTITLE, Component.empty()); // Empty subtitle
        damager.sendTitlePart(TitlePart.TIMES, Title.Times.times(
                Duration.ofMillis(0L),
                Duration.ofMillis(100L),
                Duration.ofMillis(350L)
        ));
    }


    private void nextAccuracy() {
        List<Player> home = this.game.getHomePlayers();
        List<Player> away = this.game.getAwayPlayers();
        List<Player> defenders = home.contains(this.getCurrentDamager()) ? away : home;
        List<Player> onBall = this.getLocation().clone().add(this.getCurrentDamager().getLocation().getDirection()).getNearbyPlayers(2.5).stream().filter(defenders::contains).toList();
        if (this.game.getState().equals(GoalGame.State.REGULATION) || this.game.getState().equals(GoalGame.State.OVERTIME)) {
            for (Player defender : onBall) {
                PlayerStats ps = this.game.getStatsManager().getPlayerStats(this.getCurrentDamager().getUniqueId());
                if (ps == null) continue;
                ps.addContestTime(defender.getUniqueId(), 1);
            }
        }
        onBall.forEach(player -> {
            player.sendTitlePart(TitlePart.TITLE, Component.text("   "));
            player.sendTitlePart(TitlePart.SUBTITLE, Component.text(".,.,.").color(Colour.deny()));
            player.sendTitlePart(TitlePart.TIMES, Title.Times.times(Duration.ofMillis(0L), Duration.ofMillis(100L), Duration.ofMillis(350L)));
        });

        final Player damager = this.getCurrentDamager();
        if (Partix.getInstance().getRightClickManager().isHolding(damager)) {
            if (this.accuracy == 0 && !forwardBar) {
                Partix.getInstance().getRightClickManager().removeHolding(damager);
                this.executeThrow(damager);
                return;
            }

            if (forwardBar)
                ++this.accuracy;
            else
                --this.accuracy;


            if (this.accuracy >= 8) {
                this.forwardBar = false;
                // The line "this.best = 6;" has been removed.
            }
        }else{
            this.accuracy = 0;
            this.forwardBar = true;
        }
    }

    // Replace modify() with this debugged version:
    @Override
    public void modify() {
        
        ScreenManager.INSTANCE.tickActiveScreens(this.game);

        if (this.getCurrentDamager() != null) {
            // ===== NEW CODE STARTS HERE =====
            // Track player movement for standing vs off-dribble detection
            Player possessor = this.getCurrentDamager();
            if (this.isDunkAttempt && this.dunkMeterActive &&
                    this.dunkMeterStartedAirborne && possessor.isOnGround()) {
                System.out.println(possessor.getName() + " landed on ground - clearing dunk meter");
                this.dunkMeterActive = false;
                this.dunkMeterAccuracy = 0;
                this.isDunkAttempt = false;
                this.dunkMeterStartedAirborne = false;
            }
            Vector velocity = possessor.getVelocity();
            double horizontalSpeed = Math.sqrt(velocity.getX() * velocity.getX() + velocity.getZ() * velocity.getZ());

            if (horizontalSpeed > 0.15) { // Player is moving
                this.lastMovementTime = System.currentTimeMillis();
                this.hasMovedWithBall = true;
            }
            // ===== NEW CODE ENDS HERE =====
            if (stealAttemptDefender != null) {
                long timeSinceSteals = System.currentTimeMillis() - stealAttemptTime;
                if (timeSinceSteals > 300L) {
                    clearStealAttempt();
                }
            }

            this.animateBTB();
            this.animateHesi();

            Player poss = this.getCurrentDamager();
            if (poss == null) return;
            
            // Update location tracking for stepback direction detection
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastLocationUpdate > 50) { // Update every 50ms
                lastPlayerLocation = poss.getLocation().clone();
                lastLocationUpdate = currentTime;
            }
            
            // Check hopstep travel - if player walks 2+ blocks on ground after hopstep
            if (hopstepTravelCheck) {
                // Increment ticks and wait for hopstep to complete (10 ticks = 0.5 seconds)
                hopstepCompletionTicks++;
                
                if (hopstepCompletionTicks < 10) {
                    // Still in hopstep animation - don't track yet
                    return;
                }
                
                if (poss.isOnGround()) {
                    // Player just landed - set the start location from where they landed
                    if (hopstepStartLocation == null) {
                        hopstepStartLocation = poss.getLocation().clone();
                    } else {
                        // Track distance from landing spot
                        Location currentLoc = poss.getLocation();
                        double horizontalDist = Math.sqrt(
                            Math.pow(currentLoc.getX() - hopstepStartLocation.getX(), 2) +
                            Math.pow(currentLoc.getZ() - hopstepStartLocation.getZ(), 2)
                        );
                        
                        if (horizontalDist >= 2.0) {
                            // Player walked 2+ blocks on ground without jumping - TRAVEL
                            hopstepTravelCheck = false;
                            hopstepStartLocation = null;
                            hopstepCompletionTicks = 0;
                            
                            // Show travel message
                            poss.sendTitlePart(TitlePart.TITLE, Component.text(" "));
                            poss.sendTitlePart(TitlePart.SUBTITLE, Component.text("Travel! (Hopstep)")
                                .style(Style.style(Colour.deny(), TextDecoration.BOLD)));
                            
                            handleTravel(poss);
                            return;
                        }
                    }
                } else {
                    // Player is in air - only clear if they've already landed once (jumped AFTER landing from hopstep)
                    if (hopstepStartLocation != null) {
                        hopstepTravelCheck = false;
                        hopstepStartLocation = null;
                        hopstepCompletionTicks = 0;
                    }
                    // If hopstepStartLocation is null, they're still in the air from the original hopstep - don't clear
                }
            }

            // CRITICAL: Handle dunk meter OR regular accuracy
            if (this.dunkMeterActive) {
                // Check if player moved too far from rim - cancel meter if so
                Location targetHoop = this.getTargetHoop(poss);
                Location hoopLocation = targetHoop.clone();
                hoopLocation.setY(poss.getLocation().getY());
                double horizontalDistance = hoopLocation.distance(poss.getLocation());
                
                if (horizontalDistance > 9) {
                    this.dunkMeterActive = false;
                    this.dunkMeterAccuracy = 0;
                    this.isDunkAttempt = false;
                    poss.sendMessage(Component.text("Dunk meter cancelled - too far from rim!").color(Colour.deny()));
                } else {
                    this.nextDunkMeterAccuracy();
                    this.displayDunkMeter(); // Always call when meter is active
                }
            } else {
                this.nextAccuracy();
                this.displayAccuracy();
            }

            // Calculate ball position
            Location l;
            if (this.btbActive) {
                double progress = this.btbAnimationTicks / 12.0;
                int currentAngle;

                if (this.btbTargetHand == -5) {
                    if (progress < 0.5) {
                        currentAngle = (int)(70 + (110 * (progress * 2)));
                    } else {
                        double secondHalf = (progress - 0.5) * 2;
                        currentAngle = (int)(180 + (250 * secondHalf));
                        if (currentAngle > 360) currentAngle = currentAngle - 360;
                    }
                } else {
                    if (progress < 0.5) {
                        currentAngle = (int)(-70 + (250 * (progress * 2)));
                    } else {
                        currentAngle = (int)(180 - (110 * ((progress - 0.5) * 2)));
                    }
                }

                l = Position.stabilize(poss, currentAngle, 0.75);
                l.setY(poss.getEyeLocation().getY() - 0.75);
            } else if (this.hesiActive) {
                double progress = this.hesiAnimationTicks / 8.0;
                int baseAngle = this.handYaw + this.handModifier;
                int currentAngle;

                if (progress < 0.5) {
                    int exaggeration = (int)(30 * (progress * 2));
                    currentAngle = baseAngle + (this.handModifier > 0 ? exaggeration : -exaggeration);
                } else {
                    double secondHalf = (progress - 0.5) * 2;
                    int exaggeration = (int)(30 * (1 - secondHalf));
                    currentAngle = baseAngle + (this.handModifier > 0 ? exaggeration : -exaggeration);
                }

                l = Position.stabilize(poss, currentAngle, 0.75);
                l.setY(poss.getEyeLocation().getY() - 0.75);
            } else {
                l = Position.stabilize(poss, this.handYaw, 0.75);
                l.setY(poss.getEyeLocation().getY() - 0.75);
            }

            // Normal dribbling
            double bounceSpeed = Math.max((0.234 + (Math.abs(poss.getVelocity().getX()) + Math.abs(poss.getVelocity().getZ())) * 3.0) * -1.5, -1.2);

            if (this.getLocation().getY() > poss.getEyeLocation().getY() - 0.75) {
                if (this.getLocation().getY() > poss.getEyeLocation().getY() + 0.75) {
                    this.setLocation(l);
                }
                this.setVertical(bounceSpeed);
            }
            if (this.getSpeed() < 0.075) {
                this.setLocation(l);
                this.setVertical(bounceSpeed);
            }
            this.spawnOldBallParticles();
            this.setHorizontal(l);
            this.postModify();

        } else {
            // Ball is in flight
            if (this.isDunkAttempt && this.dunkMeterActive) {
                final double floorY = game.getArenaBox().getMinY() + 2.5;
                if (this.getLocation().getY() <= floorY) {
                    System.out.println("Lob ball landed on ground - clearing dunk meter");
                    this.dunkMeterActive = false;
                    this.dunkMeterAccuracy = 0;
                    this.isDunkAttempt = false;
                }
            }

            // BALL TRAIL LOGIC - Apply on all perfect shots while ball is in flight and not blocked
            if (this.perfectShot && this.lastOwnerUUID != null && this.getCurrentDamager() == null) {
                PlayerDb.get(this.lastOwnerUUID, PlayerDb.Stat.BALL_TRAIL).thenAccept(ballTrailKey -> {
                    CosmeticBallTrail ballTrail = Cosmetics.ballTrails.get(ballTrailKey);
                    if (ballTrail != null && !ballTrail.getKey().equals("balltrail.default")) {
                        ballTrail.applyEffect(this.getLocation());
                    }
                });
            }
        }

        if (this.ownerTicks < 405) {
            this.ownerTicks = 405;
        }
        this.runStealImmunity();
        this.checkReboundEligibility();
        this.detectReboundPickup();
        this.detectMissedShotBySlab();
        this.checkSlabZone();
        this.runDelay();
        this.checkBackboardCollision();
        this.handleLayupBackboardBounce();
        this.updateDirectionLocks();  // NEW: Apply direction locking for track pass receivers

        // Decrement dash jump lock (still tick-based for precise control)
        for (Player p : this.game.getPlayers()) {
            UUID playerId = p.getUniqueId();
            if (dashJumpLock.containsKey(playerId)) {
                int remaining = dashJumpLock.get(playerId);
                if (remaining > 0) {
                    dashJumpLock.put(playerId, remaining - 1);
                } else {
                    dashJumpLock.remove(playerId);
                }
            }
        }
    }

    private void nextDunkMeterAccuracy() {
        if (!this.dunkMeterActive) {
            return;
        }

        // Meter advances every tick (oscillates 0→8→0)
        if (this.dunkMeterForward) {
            this.dunkMeterAccuracy++;
            if (this.dunkMeterAccuracy >= 8) {
                this.dunkMeterAccuracy = 8;
                this.dunkMeterForward = false;
            }
        } else {
            this.dunkMeterAccuracy--;
            if (this.dunkMeterAccuracy <= 0) {
                this.dunkMeterAccuracy = 0;
                this.dunkMeterForward = true;
            }
        }
    }

    private void handleLayupBackboardBounce() {
        if (!this.isLayupAttempt || this.getCurrentDamager() != null || this.isBlockedShot) {
            return;
        }

        // Check if ball is near backboard location
        if (this.lastOwnerUUID != null) {
            Player shooter = Bukkit.getPlayer(this.lastOwnerUUID);
            if (shooter != null) {
                Location backboard = getBackboardLocation(shooter);

                // Check if ball is close to backboard
                if (this.getLocation().distance(backboard) < 1.5) {
                    Location targetHoop = this.getTargetHoop(shooter);

                    // Play backboard sound
                    this.getLocation().getWorld().playSound(this.getLocation(),
                            Sound.BLOCK_WOOD_HIT, SoundCategory.MASTER, 0.8f, 0.9f);

                    if (this.perfectShot) {
                        // GREEN LAYUP - Animate from backboard to INSIDE the hoop
                        Location currentLoc = this.getLocation().clone();

                        // Target BELOW the rim (inside net) so ball goes down through hoop
                        Location hoopTarget = targetHoop.clone();
                        hoopTarget.setY(targetHoop.getY() - 0.5); // Go THROUGH the hoop

                        // Create small arc from backboard down into hoop
                        double midX = (currentLoc.getX() + hoopTarget.getX()) / 2;
                        double midY = Math.max(currentLoc.getY(), targetHoop.getY()) + 0.3; // Peak slightly above rim
                        double midZ = (currentLoc.getZ() + hoopTarget.getZ()) / 2;

                        final Location p1 = new Location(currentLoc.getWorld(), midX, midY, midZ);

                        // Shorter bezier for quicker backboard->rim animation
                        final List<Location> bezierPoints = bezierCurve(30, currentLoc, p1, hoopTarget);

                        setLocked(true);
                        new BukkitRunnable() {
                            int index = 0;
                            @Override
                            public void run() {
                                if (index >= bezierPoints.size() || !isValid()) {
                                    setLocked(false);
                                    cancel();
                                    return;
                                }

                                // Faster animation from backboard to rim
                                int amountToIncrement = Math.max(2, (index / 15) + 1);

                                setVelocity(new Vector());
                                endPhysics(bezierPoints.get(index));
                                index += amountToIncrement;
                            }
                        }.runTaskTimer(Partix.getInstance(), 1L, 1L);

                    } else {
                        // MISSED LAYUP - Bounce off backboard away from hoop
                        Vector awayFromHoop = this.getLocation().toVector().subtract(targetHoop.toVector());
                        awayFromHoop.normalize();

                        // Add randomness to miss direction
                        awayFromHoop.setX(awayFromHoop.getX() + (Math.random() - 0.5) * 0.3);
                        awayFromHoop.setZ(awayFromHoop.getZ() + (Math.random() - 0.5) * 0.3);

                        this.setVelocity(awayFromHoop.multiply(0.4), 0.2);
                    }

                    // No longer a layup attempt after backboard bounce
                    this.isLayupAttempt = false;
                }
            }
        }
    }

    private boolean doesPathCrossBackboard(Location from, Location to, Location backboardCenter) {
        if (from == null || to == null || backboardCenter == null) {
            return false;
        }

        if (!from.getWorld().equals(backboardCenter.getWorld())) {
            return false;
        }

        // Determine backboard orientation based on court setup
        double centerZ = this.game.getCenter().getZ();
        double backboardZ = backboardCenter.getZ();

        // Check if backboard is perpendicular to Z axis
        boolean isZBackboard = Math.abs(backboardZ - centerZ) > 0.5;

        if (!isZBackboard) {
            return false;
        }

        // Get the Z coordinates of the path segment
        double fromZ = from.getZ();
        double toZ = to.getZ();

        // ===== FIX: Check for division by zero =====
        double deltaZ = toZ - fromZ;

        if (Math.abs(deltaZ) < 0.001) {
            // Ball is moving horizontally (parallel to backboard)
            // Check if it's already AT the backboard plane
            if (Math.abs(fromZ - backboardZ) < 0.3) {
                // Ball is at backboard level - check X and Y
                double distX = Math.abs(from.getX() - backboardCenter.getX());
                double distY = Math.abs(from.getY() - backboardCenter.getY());
                return distX <= 1.2 && distY <= 1.2;
            }
            return false; // Not at backboard level
        }

        // Check if segment crosses the backboard plane
        boolean crosses = (fromZ < backboardZ && toZ > backboardZ) ||
                (fromZ > backboardZ && toZ < backboardZ);

        if (!crosses) return false;

        // Calculate intersection point with backboard plane
        double t = (backboardZ - fromZ) / deltaZ;

        // Safety check for invalid t values (shouldn't happen now, but just in case)
        if (Double.isNaN(t) || Double.isInfinite(t) || t < 0 || t > 1) {
            System.out.println("Invalid t value: " + t);
            return false;
        }

        double intersectX = from.getX() + t * (to.getX() - from.getX());
        double intersectY = from.getY() + t * (to.getY() - from.getY());

        // Check if intersection is within backboard bounds
        double distX = Math.abs(intersectX - backboardCenter.getX());
        double distY = Math.abs(intersectY - backboardCenter.getY());

        boolean hitBackboard = distX <= 1.2 && distY <= 1.2;

        if (hitBackboard) {
            System.out.println("Valid backboard intersection at X:" + intersectX + " Y:" + intersectY);
        }

        return hitBackboard;
    }
    private void handleLayupBackboardHit(Player shooter, Location hitLocation) {
        setLocked(false);
        this.isLayupAttempt = false;

        // Play backboard sound
        hitLocation.getWorld().playSound(hitLocation, Sound.BLOCK_WOOD_HIT, SoundCategory.MASTER, 0.8f, 0.9f);

        if (this.perfectShot) {
            // GREEN LAYUP - Bounce off backboard toward rim
            Location targetHoop = this.getTargetHoop(shooter);
            Vector toHoop = targetHoop.toVector().subtract(hitLocation.toVector()).normalize();
            toHoop.setY(-0.15); // Slight downward angle toward rim
            this.setVelocity(toHoop.multiply(0.4));
        } else {
            // MISSED LAYUP - Bounce off backboard away from hoop
            Location targetHoop = this.getTargetHoop(shooter);
            Vector awayFromHoop = hitLocation.toVector().subtract(targetHoop.toVector()).normalize();
            awayFromHoop.setX(awayFromHoop.getX() + (Math.random() - 0.5) * 0.3);
            awayFromHoop.setZ(awayFromHoop.getZ() + (Math.random() - 0.5) * 0.3);
            this.setVelocity(awayFromHoop.multiply(0.4), 0.2);
        }
    }

    @Override
    protected void setCurrentDamager(Player player) {
        // Check if player is on stolen-from click cooldown
        long currentTime = System.currentTimeMillis();
        UUID playerId = player.getUniqueId();
        
        if (stolenFromClickCooldown.containsKey(playerId)) {
            long cooldownExpiry = stolenFromClickCooldown.get(playerId);
            
            // Still on cooldown
            if (currentTime < cooldownExpiry) {
                // Check if we already rolled the 20% chance for this cooldown period
                if (!stolenClickRollResult.containsKey(playerId)) {
                    // First click attempt - roll the 20% chance ONCE
                    boolean allowPickup = new Random().nextDouble() < 0.20; // 20% chance
                    stolenClickRollResult.put(playerId, allowPickup);
                    
                    if (allowPickup) {
                        System.out.println(player.getName() + " got LUCKY! 20% chance succeeded - can pick up ball");
                    } else {
                        System.out.println(player.getName() + " DENIED by 20% roll - must wait " + 
                            ((cooldownExpiry - currentTime) / 1000.0) + " more seconds");
                    }
                }
                
                // Use the stored roll result
                boolean canPickup = stolenClickRollResult.get(playerId);
                
                if (!canPickup) {
                    // Blocked from picking up
                    long remaining = (cooldownExpiry - currentTime);
                    player.sendMessage(Component.text("You can't pick up the ball yet! (" + 
                        String.format("%.1f", remaining / 1000.0) + "s remaining)").color(Colour.deny()));
                    return; // PREVENT PICKUP
                }
                // If canPickup is true, fall through and allow the pickup
            } else {
                // Cooldown expired - clean up
                stolenFromClickCooldown.remove(playerId);
                stolenClickRollResult.remove(playerId);
                System.out.println(player.getName() + " click cooldown expired - can pick up ball normally");
            }
        }
        
        // Proceed with normal pickup
        super.setCurrentDamager(player);
        
        // ===== TRACK PICKUP TIME FOR 1-SECOND STEAL PROTECTION =====
        // Record when this player picked up the ball
        // During inbounds, the inbound protection takes priority
        lastBallPickupTime.put(player.getUniqueId(), System.currentTimeMillis());
        
        if (game.getState().equals(GoalGame.State.INBOUND_WAIT)) {
            game.startCountdown(GoalGame.State.INBOUND, 10);
        } else if (game.getState().equals(GoalGame.State.INBOUND)) {
            game.forceEndCountdown();
        }
    }

    @Override
    public void remove() {
        super.remove();
    }

    @Override
    public void removeCurrentDamager() {
        super.removeCurrentDamager();
        this.game.setStepbacked(null);

        // NEW: Clear track pass target when ball is released
        this.currentTrackPassTarget = null;
    }

    private void checkReboundEligibility() {
        Location ballLocation = this.getLocation();
        if (ballLocation == null || ballLocation.getWorld() == null) {
            this.setReboundEligible(false);
            return;
        }
        boolean nearSlab = false;
        for (int x = -3; x <= 3; ++x) {
            for (int y = -2; y <= 2; ++y) {
                for (int z = -3; z <= 3; ++z) {
                    Block block = ballLocation.clone().add(x, y, z).getBlock();
                    Material blockType = block.getType();
                    if (blockType != Material.QUARTZ_BLOCK && blockType != Material.RED_STAINED_GLASS) continue;
                    nearSlab = true;
                    break;
                }
                if (nearSlab) break;
            }
            if (nearSlab) break;
        }
        if (nearSlab) {
            this.setReboundEligible(true);
            if (this.game.getShotClockTicks() < 240) {
                this.game.resetShotClockTo12();
            }
        }
    }

    private void spawnOldBallParticles() {
        Location ballLocation = this.getLocation();
        if (ballLocation == null || ballLocation.getWorld() == null) {
        }
    }

    @NotNull
    private World getWorld() {
        Location location = this.getLocation();
        if (location != null && location.getWorld() != null) {
            return location.getWorld();
        }
        throw new IllegalStateException("Basketball location or world is not set.");
    }

    private void detectMissedShotBySlab() {
        Player shooter = this.getLastDamager();
        if (shooter == null) {
            return;
        }
        if (!this.game.isShotActive(shooter.getUniqueId())) {
            return;
        }
        Location ballLocation = this.getLocation();
        if (ballLocation == null || ballLocation.getWorld() == null) {
            return;
        }
        boolean nearSlab = false;
        for (int x = -3; x <= 3; ++x) {
            for (double y = -2.5; y <= 3.0; y += 1.0) {
                for (int z = -3; z <= 3; ++z) {
                    Block block = ballLocation.clone().add(x, y, z).getBlock();
                    Material blockType = block.getType();
                    if (blockType != Material.QUARTZ_BLOCK && blockType != Material.RED_STAINED_GLASS) continue;
                    nearSlab = true;
                    break;
                }
                if (nearSlab) break;
            }
            if (nearSlab) break;
        }
        if (nearSlab) {
            if (this.game.getShotClockTicks() < 240) {
                this.game.resetShotClockTo12();
            }
        }
    }

    private void checkSlabZone() {
        Player shooter;
        Location ballLocation = this.getLocation();
        if (ballLocation == null || ballLocation.getWorld() == null) {
            return;
        }
        boolean inSlabZone = false;
        for (int x = -3; x <= 3; ++x) {
            for (double y = -2.1; y <= 2.1; y += 1.0) {
                for (int z = -3; z <= 3; ++z) {
                    Block block = ballLocation.clone().add(x, y, z).getBlock();
                    Material blockType = block.getType();
                    if (blockType != Material.QUARTZ_BLOCK && blockType != Material.RED_STAINED_GLASS) continue;
                    inSlabZone = true;
                    break;
                }
                if (inSlabZone) break;
            }
            if (inSlabZone) break;
        }
        if (inSlabZone) {
            this.setReboundEligible(true);
        }
        if ((shooter = this.getLastDamager()) == null) {
            this.wasInSlabZone = inSlabZone;
            return;
        }
        UUID shooterId = shooter.getUniqueId();
        boolean activeShot = this.game.isShotActive(shooterId);
        this.wasInSlabZone = inSlabZone;
    }

    public void handleBallInterception(Player opponent) {
        boolean newTakerIsHome;
        boolean oldOwnerIsHome;
        this.game.cancelAssistTimer();
        Player lastDamager = this.getLastDamager();
        if (lastDamager != null && (oldOwnerIsHome = this.game.getHomePlayers().contains(lastDamager)) != (newTakerIsHome = this.game.getHomePlayers().contains(opponent))) {
            PlayerStats stats = this.game.getStatsManager().getPlayerStats(opponent.getUniqueId());
            stats.incrementSteals();
        }
    }

    public void addContestTime(UUID opponent, int time) {
        this.contestTime.merge(opponent, time, Integer::sum);
    }

    public UUID getTopContestedOpponent() {
        return this.contestTime.entrySet().stream().max(Comparator.comparingInt(Map.Entry::getValue)).map(Map.Entry::getKey).orElse(null);
    }

    public void resetContestTime() {
        this.contestTime.clear();
    }

    private boolean isPerfectShot() {
        return this.accuracy == 0;
    }
    
    // Check if player is currently locked in a sitting animation
    public boolean isPlayerInSittingAnimation(Player player) {
        Long lockUntil = sittingAnimationLock.get(player.getUniqueId());
        if (lockUntil == null) {
            return false;
        }
        
        // Check if lock has expired
        if (System.currentTimeMillis() >= lockUntil) {
            sittingAnimationLock.remove(player.getUniqueId());
            return false;
        }
        
        return true;
    }
    
    private void grantAnkleBreakImmunity(Player defender) {
        long immunityUntil = System.currentTimeMillis() + 3000L; // 3 seconds
        ankleBreakImmunity.put(defender.getUniqueId(), immunityUntil);
    }
    private boolean isValidStealAttempt(Player defender, Player ballHandler) {
        // Must be on ground
        if (!defender.isOnGround()) {
            return false;
        }

        // Must be within hitbox range
        double distance = defender.getLocation().distance(ballHandler.getLocation());
        return distance <= STEAL_HITBOX_RADIUS;
    }
    private void applyStealMissStun(Player defender) {
        // Slowness for 0.5 seconds
        defender.addPotionEffect(
                new PotionEffect(PotionEffectType.SLOWNESS, STEAL_STUN_DURATION, 2, true, false)
        );

        // Add cooldown for next steal attempt
        this.stealImmunityTicks = STEAL_COOLDOWN_AFTER_MISS;

        // Visual/audio feedback
        defender.playSound(defender.getLocation(), Sound.ENTITY_ARROW_HIT, SoundCategory.MASTER, 0.7f, 0.8f);
        defender.sendMessage(Component.text("Steal attempt failed!").color(Colour.deny()));
    }
    private void triggerAnkleBreakerAnimation(Player defender, Player ballHandler) {
        // Check immunity
        if (hasAnkleBreakImmunity(defender)) {
            return;
        }



        // Get randomized freeze duration (1-3 seconds)
        int freezeDuration = ANKLE_BREAK_FREEZE_MIN +
                new Random().nextInt(ANKLE_BREAK_FREEZE_MAX - ANKLE_BREAK_FREEZE_MIN + 1);
// Mark defender as ankle broken (can't contest)
        long recoveryTime = System.currentTimeMillis() + (freezeDuration * 50L);
        ankleBreakRecovery.put(defender.getUniqueId(), recoveryTime);
        // Calculate knockback direction (away from ball handler)
        // Since defender is on ground, give them strong horizontal + upward velocity
        Vector knockback = ballHandler.getLocation().getDirection().normalize();
        knockback.setY(0); // Remove any vertical component first
        knockback.normalize(); // Re-normalize to ensure consistent horizontal direction
        knockback.multiply(0.5); // Strong horizontal push
        knockback.setY(0.15); // Add upward component to launch them into the air

        // Apply knockback to launch defender
        defender.setVelocity(knockback);

        // Send messages
        defender.sendMessage(Component.text("ANKLES BROKEN!").color(Colour.deny()).decorate(TextDecoration.BOLD));
        ballHandler.sendMessage(Component.text("Broke " + defender.getName() + "'s ankles!").color(Colour.allow()).decorate(TextDecoration.BOLD));

        // Sound effect
        for (Player nearby : defender.getWorld().getPlayers()) {
            if (nearby.getLocation().distance(defender.getLocation()) < 30) {
                nearby.playSound(defender.getLocation(), Sound.ENTITY_ITEM_BREAK, SoundCategory.MASTER, 0.8f, 0.6f);
            }
        }

        // Wait for defender to be launched into air and then land
        BukkitRunnable groundChecker = new BukkitRunnable() {
            int checkCount = 0;
            boolean wasAirborne = false;

            @Override
            public void run() {
                checkCount++;

                // Track if defender became airborne
                if (!defender.isOnGround()) {
                    wasAirborne = true;
                }

                // Only sit them down AFTER they were airborne and landed
                if (wasAirborne && defender.isOnGround()) {
                    this.cancel();

                    // Spawn pig 1 block BELOW ground level so defender sits flush with floor
                    Location groundLoc = defender.getLocation().clone();
                    groundLoc.setY(Math.floor(groundLoc.getY()) - 1.0);

                    org.bukkit.entity.Pig sitPig = defender.getLocation().getWorld().spawn(groundLoc, org.bukkit.entity.Pig.class);
                    sitPig.setAI(false);
                    sitPig.setInvulnerable(true);
                    sitPig.setInvisible(true);
                    sitPig.setSilent(true);
                    sitPig.setGravity(false);

                    // Small delay to ensure pig is spawned before adding passenger
                    Bukkit.getScheduler().runTaskLater(Partix.getInstance(), () -> {
                        sitPig.addPassenger(defender);
                        defender.sendMessage(Component.text("You're on the ground!").color(Colour.deny()).decorate(TextDecoration.BOLD));
                        
                        // Lock player in sitting animation - prevent shift dismount
                        sittingAnimationLock.put(defender.getUniqueId(), System.currentTimeMillis() + (freezeDuration * 50L));
                    }, 1L);

                    // Apply slowness while sitting
                    defender.addPotionEffect(
                            new PotionEffect(PotionEffectType.SLOWNESS, freezeDuration, 3, true, false)
                    );

                    // Remove pig after freeze duration
                    Bukkit.getScheduler().runTaskLater(Partix.getInstance(), () -> {
                        sitPig.remove();
                        defender.sendMessage(Component.text("Get up!").color(Colour.allow()));

                        // Remove ankle broken status and sitting lock
                        ankleBreakRecovery.remove(defender.getUniqueId());
                        sittingAnimationLock.remove(defender.getUniqueId());
                    }, freezeDuration);
                }

                // Timeout after 5 seconds
                if (checkCount > 100) {
                    this.cancel();
                }
            }
        };

        groundChecker.runTaskTimer(Partix.getInstance(), 1L, 1L);

        // Grant immunity for 3 seconds
        grantAnkleBreakImmunity(defender);
    }

    public void checkAnkleBreakerOnDribbleMove(Player ballHandler) {
        // Check if there's an active steal attempt on this player
        if (stealAttemptDefender == null) {
            return;
        }

        Player defender = Bukkit.getPlayer(stealAttemptDefender);
        if (defender == null) {
            stealAttemptDefender = null;
            return;
        }

        long timeSinceSteals = System.currentTimeMillis() - stealAttemptTime;

        if (timeSinceSteals > 300L) {
            stealAttemptDefender = null;
            return;
        }

        if (hasAnkleBreakImmunity(defender)) {
            return;
        }

        // ===== NEW: Check if defender is in FRONT of ball handler =====
        Vector ballHandlerDirection = ballHandler.getLocation().getDirection().normalize();
        Vector toDefender = defender.getLocation().toVector()
                .subtract(ballHandler.getLocation().toVector()).normalize();

        double dotProduct = ballHandlerDirection.dot(toDefender);

        // Defender must be in front (dot product > 0.3)
        if (dotProduct < 0.3) {
            // Defender is behind - NO ANKLE BREAKER
            System.out.println("Defender " + defender.getName() + " is behind " +
                    ballHandler.getName() + " - no ankle breaker");
            return;
        }
        // ===== END CHECK =====

        // 40% chance to trigger ankle break
        if (Math.random() < ANKLE_BREAK_SUCCESS_CHANCE) {
            triggerAnkleBreakerAnimation(defender, ballHandler);
        }
    }
    private void recordStealAttempt(Player defender) {
        stealAttemptDefender = defender.getUniqueId();
        stealAttemptTime = System.currentTimeMillis();
    }
    private void clearStealAttempt() {
        stealAttemptDefender = null;
        stealAttemptTime = 0L;
    }
    private boolean hasAnkleBreakImmunity(Player defender) {
        Long immunityExpiry = ankleBreakImmunity.get(defender.getUniqueId());
        if (immunityExpiry == null) return false;

        long currentTime = System.currentTimeMillis();
        if (currentTime >= immunityExpiry) {
            ankleBreakImmunity.remove(defender.getUniqueId());
            return false;
        }
        return true;
    }
    private boolean isAnkleBroken(Player defender) {
        Long recoveryTime = ankleBreakRecovery.get(defender.getUniqueId());
        if (recoveryTime == null) return false;

        long currentTime = System.currentTimeMillis();
        if (currentTime >= recoveryTime) {
            ankleBreakRecovery.remove(defender.getUniqueId());
            return false;
        }
        return true;
    }
    public void clearPerfectShot() {
    }

    public UUID getTrueLastPossessor() {
        // If shot was blocked, return the SHOOTER'S UUID (not the blocker's)
        // The shooter's team should get the ball back on OOB
        if (this.lastShotBlockerUUID != null) {
            Player shooter = this.getLastDamager();
            if (shooter != null) {
                System.out.println("Shot was blocked - shooter's team keeps possession: " + shooter.getName());
                return shooter.getUniqueId();
            }
        }

        // If ball was poked, return who had it BEFORE the poke (the victim)
        if (this.wasPoked && this.lastPossessionBeforePoke != null) {
            System.out.println("Ball was poked - true last possessor (victim) is: " + Bukkit.getPlayer(this.lastPossessionBeforePoke).getName());
            return this.lastPossessionBeforePoke;
        }

        // ===== CHECK FOR LOB PASS IN FLIGHT =====
        if (this.isLobPass && this.lobPasserUUID != null) {
            System.out.println("Lob pass in flight - passer is: " + Bukkit.getPlayer(this.lobPasserUUID).getName());
            return this.lobPasserUUID;
        }

        // ===== CHECK FOR TRACK PASS IN FLIGHT =====
        if (this.currentTrackPassTarget != null) {
            Player lastDamager = this.getLastDamager();
            if (lastDamager != null) {
                System.out.println("Track pass in flight - passer is: " + lastDamager.getName());
                return lastDamager.getUniqueId();
            }
        }

        // Otherwise return the actual last possessor
        if (this.lastPossessorUUID != null) {
            System.out.println("Normal possession - last possessor is: " + Bukkit.getPlayer(this.lastPossessorUUID).getName());
            return this.lastPossessorUUID;
        }

        // ===== FINAL FALLBACK: Use getLastDamager() =====
        Player lastDamager = this.getLastDamager();
        if (lastDamager != null) {
            System.out.println("FALLBACK: Using lastDamager - " + lastDamager.getName());
            return lastDamager.getUniqueId();
        }

        return null;
    }

    // And add a method to clear the blocker flag when needed:
    public void clearBlockerFlag() {
        this.lastShotBlockerUUID = null;
    }

    public void clearPokeFlags() {
        this.wasPoked = false;
        this.lastPossessionBeforePoke = null;
    }
// ADD ALL OF THESE METHODS AFTER clearPerfectShot()

    public void registerDefenderBlockAttempt(Player defender) {
        if (this.isDunkAttempt && this.getCurrentDamager() != null) {
            Player dunker = this.getCurrentDamager();
            GoalGame.Team dunkerTeam = game.getTeamOf(dunker);
            GoalGame.Team defenderTeam = game.getTeamOf(defender);

            if (dunkerTeam != null && defenderTeam != null && !dunkerTeam.equals(defenderTeam)) {
                double distance = defender.getLocation().distance(dunker.getLocation());
                if (distance <= 3.0) {
                    this.defenderBlockAttempts.put(defender.getUniqueId(), System.currentTimeMillis());
                    defender.sendMessage(Component.text("Block attempt!").color(Colour.allow()));
                }
            }
        }
    }



    private DunkContestResult calculateDunkContest(Player dunker) {
        if (game == null) return new DunkContestResult(0.0, null, false);

        GoalGame.Team dunkerTeam = game.getTeamOf(dunker);
        if (dunkerTeam == null) return new DunkContestResult(0.0, null, false);

        Player closestDefender = null;
        double highestContest = 0.0;
        boolean shouldPosterize = false;
        double closestDistance = Double.MAX_VALUE;

        final double contestRadius = 3.0;
        final double floorY = game.getArenaBox().getMinY() + 2.65;

        for (Player defender : dunker.getWorld().getPlayers()) {
            GoalGame.Team defenderTeam = game.getTeamOf(defender);

            if (defenderTeam != null && !defenderTeam.equals(dunkerTeam) && defenderTeam != GoalGame.Team.SPECTATOR) {
                double distance = defender.getLocation().distance(dunker.getLocation());

                if (distance > contestRadius) continue;

                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestDefender = defender;
                }

                boolean isAirborne = defender.getLocation().getY() > (floorY + 0.5);
                boolean clickedToBlock = defenderBlockAttempts.containsKey(defender.getUniqueId());

                double contestValue = 0.0;
                boolean badTiming = false;

                if (isAirborne) {
                    // JUMPING DEFENDER - HIGHER CONTEST BUT MORE RISK
                    double heightDiff = defender.getLocation().getY() - dunker.getLocation().getY();

                    if (clickedToBlock) {
                        long clickTime = defenderBlockAttempts.get(defender.getUniqueId());
                        long timeSinceClick = System.currentTimeMillis() - clickTime;

                        // Perfect timing - VERY STRONG CONTEST (BUFFED)
                        if (timeSinceClick < 300 && heightDiff > -0.3 && heightDiff < 1.0) {
                            contestValue = 0.95 - (distance / contestRadius) * 0.2; // BUFFED from 0.90
                        }
                        // Good timing - STRONG CONTEST (BUFFED)
                        else if (timeSinceClick < 600) {
                            contestValue = 0.75 - (distance / contestRadius) * 0.2; // BUFFED from 0.65
                            badTiming = true;
                        }
                        // Bad timing - MODERATE CONTEST (BUFFED)
                        else {
                            contestValue = 0.40; // BUFFED from 0.25
                            badTiming = true;
                        }
                    } else {
                        // Jumped without clicking - DECENT CONTEST (BUFFED)
                        if (heightDiff > -0.5 && heightDiff < 1.2) {
                            contestValue = 0.65 - (distance / contestRadius) * 0.25; // BUFFED from 0.55
                        } else {
                            contestValue = 0.40; // BUFFED from 0.30
                        }
                        badTiming = true;
                    }
                } else {
                    // GROUNDED DEFENDER - MODERATE CONTEST (BUFFED)
                    contestValue = 0.35 - (distance / contestRadius) * 0.15; // BUFFED from 0.20
                }

                // POSTERIZER CONDITIONS - Airborne defenders at higher risk!

                // Condition 1: Jumped with bad timing (most common)
                if (isAirborne && badTiming && distance < 2.8 && this.dunkMeterAccuracy >= 4) {
                    shouldPosterize = true;
                }

                // Condition 2: Green release + close + defender jumped (high risk)
                if (isAirborne && distance < 2.5 && this.dunkMeterAccuracy >= 6 && this.dunkMeterAccuracy <= 8) {
                    shouldPosterize = true;
                }

                // Condition 3: Grounded defender (lower posterizer risk)
                if (!isAirborne && distance < 1.8 && this.dunkMeterAccuracy >= 6) {
                    shouldPosterize = true;
                }

                if (contestValue > highestContest) {
                    highestContest = contestValue;
                }
            }
        }

        defenderBlockAttempts.clear();

        return new DunkContestResult(Math.min(highestContest, 1.0), closestDefender, shouldPosterize);
    }



    private void applyPosterizer(Player dunker, Player victim) {
        if (victim == null) return;

        Location victimLoc = victim.getLocation();

        // Calculate knockback direction (away from dunker)
        Vector knockback = dunker.getLocation().getDirection().normalize().multiply(1.2);
        knockback.setY(0.4); // Upward component so they fly back

        // Apply knockback immediately (while in air)
        victim.setVelocity(knockback);

        dunker.sendMessage(Component.text("POSTERIZED " + victim.getName() + "!").color(Colour.allow()).decorate(TextDecoration.BOLD));
        victim.sendMessage(Component.text("You got POSTERIZED!").color(Colour.deny()).decorate(TextDecoration.BOLD));

        for (Player nearby : victim.getWorld().getPlayers()) {
            if (nearby.getLocation().distance(victim.getLocation()) < 30) {
                nearby.playSound(victim.getLocation(), Sound.ENTITY_SHEEP_AMBIENT, SoundCategory.MASTER, 1.0f, 0.8f);
            }
        }

        // Wait for victim to hit the ground, then make them sit
        BukkitRunnable groundChecker = new BukkitRunnable() {
            int checkCount = 0;

            @Override
            public void run() {
                checkCount++;

                // Check if victim hit the ground or timeout
                if (victim.isOnGround() || checkCount > 100) { // 100 ticks = 5 seconds max
                    this.cancel();

                    // CHANGED: Spawn pig 1 block BELOW ground level so victim sits flush with floor
                    Location groundLoc = victim.getLocation().clone();
                    groundLoc.setY(Math.floor(groundLoc.getY()) - 1.0); // CHANGED: -1.0 instead of floor only

                    // Victim hit the ground - make them sit for 2 seconds
                    org.bukkit.entity.Pig sitPig = victim.getLocation().getWorld().spawn(groundLoc, org.bukkit.entity.Pig.class);
                    sitPig.setAI(false);
                    sitPig.setInvulnerable(true);
                    sitPig.setInvisible(true);
                    sitPig.setSilent(true);
                    sitPig.setGravity(false);

                    // Small delay to ensure pig is spawned before adding passenger
                    Bukkit.getScheduler().runTaskLater(Partix.getInstance(), () -> {
                        if (victim.isOnGround()) {
                            sitPig.addPassenger(victim);
                            victim.sendMessage(Component.text("You're on the ground!").color(Colour.deny()).decorate(TextDecoration.BOLD));
                            
                            // Lock player in sitting animation - prevent shift dismount
                            sittingAnimationLock.put(victim.getUniqueId(), System.currentTimeMillis() + (40 * 50L)); // 40 ticks = 2 seconds
                        }
                    }, 1L);

                    // Remove pig after 2 seconds (40 ticks)
                    Bukkit.getScheduler().runTaskLater(Partix.getInstance(), () -> {
                        sitPig.remove();
                        victim.sendMessage(Component.text("Get up!").color(Colour.allow()));
                        
                        // Remove sitting animation lock
                        sittingAnimationLock.remove(victim.getUniqueId());
                    }, 40L);
                }
            }
        };

        groundChecker.runTaskTimer(Partix.getInstance(), 1L, 1L);
    }

    public void setDunkMeterActive(boolean active) {
        this.dunkMeterActive = active;
        if (!active) {
            this.dunkMeterAccuracy = 0;
            this.isDunkAttempt = false;
        }
    }

    public boolean getDunkMeterActive() {
        return this.dunkMeterActive;
    }

    @Setter
    public class Ball {
        private boolean reboundEligible = false;

        public void onScored() {
            this.reboundEligible = false;
        }

        public void markReboundEligible() {
            this.reboundEligible = true;
        }
    }

    @RequiredArgsConstructor
    @Getter
    enum DistanceZone {
        SHORT(5, 2),
        MEDIUM(6, 3),
        LONG(7, 4);

        private final int requiredGreenAccuracy;
        private final int yellowAccuracyStart;
    }

    private static class DunkContestResult {
        private final double contestValue;
        private final Player closestDefender;
        private final boolean shouldPosterize;

        public DunkContestResult(double contestValue, Player closestDefender, boolean shouldPosterize) {
            this.contestValue = contestValue;
            this.closestDefender = closestDefender;
            this.shouldPosterize = shouldPosterize;
        }

        public double getContestValue() { return contestValue; }
        public Player getClosestDefender() { return closestDefender; }
        public boolean shouldPosterize() { return shouldPosterize; }
    }
    private static class BlockResult {
        private final boolean wasBlocked;
        private final Player blocker;

        public BlockResult(boolean wasBlocked, Player blocker) {
            this.wasBlocked = wasBlocked;
            this.blocker = blocker;
        }

        public boolean wasBlocked() { return wasBlocked; }
        public Player getBlocker() { return blocker; }
    }

    public Player getLastPossessor() {
        if (this.lastPossessorUUID == null) return null;
        return Bukkit.getPlayer(this.lastPossessorUUID);
    }
    private void updateDirectionLocks() {
        long currentTime = System.currentTimeMillis();

        for (Player player : this.game.getPlayers()) {
            UUID playerUUID = player.getUniqueId();
            Long lockExpiry = directionLockUntil.get(playerUUID);

            if (lockExpiry == null || currentTime >= lockExpiry) {
                // Lock expired - clean up
                directionLockUntil.remove(playerUUID);
                lockedDirection.remove(playerUUID);
                continue;
            }

            // Lock is still active
            Vector dragVelocity = lockedDirection.get(playerUUID);
            if (dragVelocity == null) continue;

            // Check if this is a STANDING LOCK (zero vector)
            if (dragVelocity.lengthSquared() < 0.01) {
                // STANDING PLAYER - Keep them frozen
                player.setVelocity(new Vector(0, player.getVelocity().getY(), 0));

                // Check if ball is close enough to catch
                double distanceToBall = player.getLocation().distance(this.getLocation());
                if (distanceToBall < 1.8) {
                    directionLockUntil.remove(playerUUID);
                    lockedDirection.remove(playerUUID);
                    currentTrackPassTarget = null;

                    if (this.getCurrentDamager() == null) {
                        this.setDamager(player);
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP,
                                SoundCategory.MASTER, 1.0f, 1.2f);
                    }
                }
                continue;
            }

            // MOVING PLAYER - Apply drag velocity to move them toward intercept point
            Vector currentVelocity = player.getVelocity();
            Vector newVelocity = dragVelocity.clone();
            newVelocity.setY(currentVelocity.getY()); // Preserve vertical velocity (gravity/jumping)

            player.setVelocity(newVelocity);

            // Check if ball is close enough to catch
            double distanceToBall = player.getLocation().distance(this.getLocation());
            if (distanceToBall < 2.2) {
                directionLockUntil.remove(playerUUID);
                lockedDirection.remove(playerUUID);
                currentTrackPassTarget = null;

                if (this.getCurrentDamager() == null) {
                    this.setDamager(player);
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP,
                            SoundCategory.MASTER, 1.0f, 1.2f);
                    System.out.println(player.getName() + " caught track pass!");
                }
            }
        }
    }
    @RequiredArgsConstructor
    @Getter
    enum ShotType {
        LAYUP(0.40, false),           // Not affected by standing/moving
        STANDING_MID(0.50, false),    // 50% green threshold
        MOVING_MID(0.40, true),       // 45% green threshold
        STANDING_THREE(0.45, false),  // 45% green threshold
        MOVING_THREE(0.35, true);     // 35% green threshold

        private final double greenThreshold;
        private final boolean isMovingShot;

        public String getDisplayName() {
            switch (this) {
                case STANDING_MID:
                case STANDING_THREE:
                    return "Standing Shot";
                case MOVING_MID:
                case MOVING_THREE:
                    return "Off-Dribble Shot";
                default:
                    return "Layup";
            }
        }
    }

} // End of Basketball class