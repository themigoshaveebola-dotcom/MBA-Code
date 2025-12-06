package me.x_tias.partix.plugin.ball.types;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.bukkit.util.BoundingBox;
import me.x_tias.partix.Partix;
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
    @Getter
    private final BasketballGame game;
    private final Map<UUID, Integer> contestTime = new HashMap<>();
    public int delay = 0;
    public boolean isShouldPreventScore() {
        return this.shouldPreventScore;
    }
    @Getter
    private PassMode passMode = PassMode.BULLET; // Default to bullet pass

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
            return this == BULLET ? LOB : BULLET;
        }
    }

    private boolean perfectShot = false;
    public boolean layupScored = false; // NEW: Track if this layup already scored
    private boolean layupScoreDetected = false;
    private boolean guaranteedMiss = false;
    private boolean shouldPreventScore = false;
    private long lastMovementTime = 0L; // When player last moved with ball
    private boolean hasMovedWithBall = false; // Has player moved since catching ball
    private static final long STANDING_STILL_DURATION = 600L; // 0.6 seconds in milliseconds
    @Getter
    private double shotDistance = 0.0;
    private Location shotLocation = null;

    private Map<UUID, Long> defenderBlockAttempts = new HashMap<>();
    // ===== ANKLE BREAK SYSTEM =====
    private Map<UUID, Long> ankleBreakImmunity = new HashMap<>(); // Tracks 3s immunity
    private UUID stealAttemptDefender = null; // Tracks who attempted the steal
    private long stealAttemptTime = 0L; // When the steal was attempted
    private Map<UUID, Long> stealAttemptsOnBallHandler = new HashMap<>(); // Ball handler's dribble window
    private boolean stealMissedThisTick = false; // Track if steal missed this tick

    // Ground steal check
    private static final double STEAL_HITBOX_RADIUS = 2.5; // Increased from ~1.5
    private static final int STEAL_STUN_DURATION = 10; // 0.5 seconds in ticks
    private static final int STEAL_COOLDOWN_AFTER_MISS = 15; // 0.75 seconds in ticks
    private static final int ANKLE_BREAK_IMMUNITY_TICKS = 60; // 3 seconds in ticks
    private static final int DRIBBLE_COUNTER_WINDOW = 6; // 0.3 seconds in ticks
    private static final double ANKLE_BREAK_SUCCESS_CHANCE = 0.40; // 40% chance
    private static final int ANKLE_BREAK_FREEZE_MIN = 20; // 1 second minimum
    private static final int ANKLE_BREAK_FREEZE_MAX = 60; // 3 seconds maximum
    private Player posterizedDefender = null;
    private UUID lastPossessionBeforePoke = null; // Track who had possession before ball was poked
    public boolean wasPoked = false; // Track if ball was just poked loose
    private boolean dunkMeterActive = false;
    private int dunkMeterAccuracy = 0;
    private int dunkMeterWait = 0;
    private int passDelay = 0;
    private int catchDelay = 0; // NEW: Delay after catching ball
    private UUID justDunkedPlayer = null; // Track who just completed a dunk attempt
    private boolean dunkMeterForward = true;
    private boolean isDunkAttempt = false;
    private UUID lastOwnerUUID = null;
    private UUID lastPossessorUUID = null; // Track actual possession, not just touches
    private long perfectShotStartTime = 0L;
    private int ownerTicks;
    private int handYaw = 50;
    private int powerCrossoverCooldown = 0;
    private int handModifier = 5;
    private int hesiDelay = 0;
    private UUID lastShotBlockerUUID = null;
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
            // PREVENT SHOOTING DURING INBOUND
            if (this.game.inboundingActive && player.equals(this.game.inbounder)) {
                player.sendMessage(Component.text("You can't shoot while inbounding!").color(Colour.deny()));
                return; // ADDED: Exit early to prevent shooting
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

    public Location getTargetHoop(Player player) {
        if (player.getLocation().clone().add(player.getLocation().getDirection().multiply(1.0)).getZ() < this.game.getCenter().getZ()) {
            return this.game.getAwayNet().clone().getCenter().toLocation(player.getWorld()).clone();
        }
        return this.game.getHomeNet().clone().getCenter().toLocation(player.getWorld()).clone();
    }

    // Replace the executeThrow method with this updated version:
    private void executeThrow(Player player) {
        // Reset layup scoring flag on new shot attempt
        this.layupScoreDetected = false;
        this.isLayupAttempt = false;
        this.shouldPreventScore = false;


        float pitch = Math.min(145.0f, Math.max(90.0f, 90.0f + Math.abs(player.getLocation().getPitch()))) - 90.0f;
        this.setLocation(player.getEyeLocation().add(player.getLocation().getDirection().multiply(1.425)));
        Location th = player.getLocation().clone();

        this.shotContestPercentage = calculateContest(player);
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

        final int yBelow = centerBlock.getLocation().clone().subtract(0, 2, 0).getBlockY();
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

        // NEW: Direction check for JUMP SHOTS ONLY (not layups/dunks)
        if (!this.isLayupAttempt) {
            // Calculate direction to target hoop
            Vector toHoop = targetHoop.toVector().subtract(playerLoc.toVector()).normalize();
            // Get player's look direction (horizontal only - ignore vertical)
            Vector playerDirection = playerLoc.getDirection().clone();
            playerDirection.setY(0);
            playerDirection.normalize();

            // Calculate dot product (1.0 = same direction, -1.0 = opposite, 0 = perpendicular)
            double dotProduct = playerDirection.dot(toHoop);

            // Require player to be facing at least somewhat toward the hoop
            // dotProduct > 0.3 means within ~70 degrees of hoop direction
            if (dotProduct < 0.3) {
                player.sendMessage(Component.text("You must face the hoop to shoot!").color(Colour.deny()));
                this.forceThrow(); // Throw ball forward as a pass instead
                return;
            }
        }

        float yaw = player.getLocation().getYaw();

// CAPTURE SHOT DISTANCE AT TIME OF SHOT
        this.shotDistance = player.getLocation().distance(targetHoop);

        if (this.accuracy < distanceZone.getRequiredGreenAccuracy()) {
            float randomYawOffset = (float)(12 - this.accuracy) * 1.5f;
            yaw += (new Random().nextBoolean() ? randomYawOffset : -randomYawOffset);
        }
        th.setYaw(yaw);

// UPDATED executeThrow() method - DECREASED yellow/red shot success rates
// Replace the shot success determination section (around line 340) with this:

// Determine shot success (applies to both layups and regular shots)
        boolean perfect = false;
        this.guaranteedMiss = false; // Reset guaranteed miss flag
        this.shouldPreventScore = false; // Flag this shot to never count as a score

// ========== NEW: CHECK FOR 90%+ CONTEST FIRST ==========
        if (this.shotContestPercentage >= 0.90) {
            perfect = false;
            this.guaranteedMiss = true;
            this.shouldPreventScore = true;
            player.sendMessage(Component.text("SMOTHERED DEFENSE!").color(Colour.deny()).decorate(TextDecoration.BOLD));
        }
// ========== CHECK FOR HEAVY CONTEST (65-90%) ==========
        else if (this.shotContestPercentage >= 0.65) {
            perfect = false;
            this.guaranteedMiss = true;
            this.shouldPreventScore = true;
            player.sendMessage(Component.text("HEAVILY CONTESTED!").color(Colour.deny()).decorate(TextDecoration.BOLD));
        }
// LAYUP LOGIC - SIGNIFICANTLY BUFFED GREEN WINDOW
        else if (this.isLayupAttempt) {
            // Layups have much better green window
            if (this.accuracy >= 3) { // GREEN: 3-8
                perfect = true;
            } else if (this.accuracy >= 1) { // YELLOW: 1-2
                perfect = Math.random() <= 0.50; // DECREASED from 0.70 to 0.50 (50% chance)
                if (!perfect) this.guaranteedMiss = true;
                this.shouldPreventScore = true;
            } else { // RED: 0
                perfect = Math.random() <= 0.10; // DECREASED from 0.20 to 0.10 (10% chance)
                if (!perfect) this.guaranteedMiss = true;
                this.shouldPreventScore = true;
            }
        }
// Normal range shots (0-24 blocks)
        else if (distanceToHoop <= 25 && this.shotContestPercentage < greenThreshold) {
            if (this.accuracy >= distanceZone.getRequiredGreenAccuracy()) {
                perfect = true;
            } else if (this.accuracy >= distanceZone.getYellowAccuracyStart()) {
                // Off-dribble shots have LOWER success rate in yellow window
                // DECREASED: was 25% moving, 35% standing
                double yellowChance = isMovingShot ? 0.15 : 0.25; // NOW: 15% moving, 25% standing
                perfect = Math.random() <= yellowChance;
                if (!perfect) this.guaranteedMiss = true;
                this.shouldPreventScore = true;
            } else {
                // Red window - even worse for moving shots
                // DECREASED: was 2% moving, 5% standing
                double redChance = isMovingShot ? 0.01 : 0.02; // NOW: 1% moving, 2% standing
                perfect = Math.random() <= redChance;
                if (!perfect) this.guaranteedMiss = true;
                this.shouldPreventScore = true;
            }
        }
// Deep range shots (24-27 blocks)
        else if (distanceToHoop > 24 && distanceToHoop <= 27 && this.shotContestPercentage < greenThreshold) {
            if (this.accuracy >= distanceZone.getRequiredGreenAccuracy()) {
                // Even with green release, deep shots are harder
                // DECREASED: was 10% moving, 15% standing
                double deepGreenChance = isMovingShot ? 0.06 : 0.10; // NOW: 6% moving, 10% standing
                perfect = Math.random() <= deepGreenChance;
                if (!perfect) this.guaranteedMiss = true;
                this.shouldPreventScore = true;
            } else if (this.accuracy >= distanceZone.getYellowAccuracyStart()) {
                // DECREASED: was 3% moving, 5% standing
                double deepYellowChance = isMovingShot ? 0.01 : 0.02; // NOW: 1% moving, 2% standing
                perfect = Math.random() <= deepYellowChance;
                if (!perfect) this.guaranteedMiss = true;
                this.shouldPreventScore = true;
            } else {
                perfect = Math.random() <= 0.005; // DECREASED: was 1%, now 0.5%
                if (!perfect) this.guaranteedMiss = true;
                this.shouldPreventScore = true;
            }

            if (!perfect) {
                player.sendMessage(Component.text("Deep Shot").color(Colour.deny()));
            }
        }
// Way too deep (27+ blocks) OR heavily contested (45-65%)
        else if (distanceToHoop > 27 || this.shotContestPercentage >= 0.45) {
            perfect = false;
            this.guaranteedMiss = true;
            this.shouldPreventScore = true;
            if (distanceToHoop > 27) {
                player.sendMessage(Component.text("Too deep!").color(Colour.deny()));
            } else {
                player.sendMessage(Component.text("CONTESTED!").color(Colour.deny()));
            }
        }

        final String percentage = String.format("%.2f", this.shotContestPercentage * 100.0f);
        player.showTitle(Title.title(
                Component.empty(),
                MiniMessage.miniMessage().deserialize("<green>" + percentage + "% Contested"),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ofMillis(350L))
        ));

        if (perfect) {
            this.perfectShot = true;
            this.perfectShotStartTime = System.currentTimeMillis();
        } else {
            this.perfectShot = false;
        }

        this.game.onShotAttempt(player, this.threeEligible);

        if (shotType != ShotType.LAYUP) {
            String shotTypeText = shotType.getDisplayName();
            NamedTextColor color = isMovingShot ? NamedTextColor.YELLOW : NamedTextColor.GREEN;
            player.sendMessage(Component.text(shotTypeText).color(color));
        }

        if (this.isLayupAttempt) {
            player.sendMessage(Component.text("Layup!").color(Colour.allow()));

            if (this.game.getState().equals(GoalGame.State.REGULATION) ||
                    this.game.getState().equals(GoalGame.State.OVERTIME)) {
                PlayerStats stats = this.game.getStatsManager().getPlayerStats(player.getUniqueId());
                stats.incrementFGAttempted();
            }

            Location loc1 = playerLoc.clone();
            Location loc2 = targetHoop.clone(); // IMPORTANT: Animate to HOOP, not backboard

            double midX = (loc1.getX() + loc2.getX()) / 2;
            double midY = ((loc1.getY() + loc2.getY()) / 2) + 3.5; // Lower arc than jump shots
            double midZ = (loc1.getZ() + loc2.getZ()) / 2;

            final Location p1 = new Location(loc1.getWorld(), midX, midY, midZ);

            // Animate directly to hoop - let checkBackboardCollision() handle backboard
            final List<Location> bezierPoints = bezierCurve(50, loc1, p1, loc2);

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

                    int amountToIncrement = Math.max(3, (index / 20) + 1);

                    setVelocity(new Vector());
                    endPhysics(bezierPoints.get(index));
                    index += amountToIncrement;
                }
            }.runTaskTimer(Partix.getInstance(), 1L, 1L);

            this.lastOwnerUUID = player.getUniqueId();




        } else if (this.perfectShot) {
            player.sendMessage("Perfect shot");
            Location loc1 = playerLoc.clone();
            Location loc2 = targetHoop.clone();

            double midX = (loc1.getX() + loc2.getX()) / 2;
            double midY = ((loc1.getY() + loc2.getY()) / 2) + 7.5;
            double midZ = (loc1.getZ() + loc2.getZ()) / 2;

            final Location p1 = new Location(loc1.getWorld(), midX, midY, midZ);

// CHECK BACKBOARD COLLISION BEFORE STARTING BEZIER
            if (doesPathIntersectBackboard(loc1, targetHoop, player)) {
                // Ball would hit backboard - bounce it away instead
                System.out.println("DEBUG: Shot will hit backboard, bouncing ball");

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

            final List<Location> bezierPoints = bezierCurve(100, loc1, p1, loc2);

            setLocked(true);
            new BukkitRunnable() {
                int index = 0;
                Location previousLoc = null;

                @Override
                public void run() {
                    if (index >= bezierPoints.size() || !isValid()) {
                        setLocked(false);
                        cancel();
                        return;
                    }

                    Location nextLoc = bezierPoints.get(index);

                    // Check backboard collision for perfect shots too
                    if (previousLoc != null) {
                        Location backboard = getBackboardLocation(player);
                        if (doesPathCrossBackboard(previousLoc, nextLoc, backboard)) {
                            // Hit backboard - bounce toward rim
                            setLocked(false);
                            Vector toHoop = targetHoop.toVector().subtract(nextLoc.toVector()).normalize();
                            toHoop.setY(-0.15);
                            setVelocity(toHoop.multiply(0.4));
                            cancel();
                            return;
                        }
                    }

                    int amountToIncrement = Math.max(4, (index / 25) + 1);

                    setVelocity(new Vector());
                    endPhysics(nextLoc);
                    previousLoc = nextLoc.clone();
                    index += amountToIncrement;
                }
            }.runTaskTimer(Partix.getInstance(), 1L, 1L);


        } else if (this.guaranteedMiss) {
            player.sendMessage(Component.text("Bad Shot Attempt").color(Colour.deny()));
            Location loc1 = playerLoc.clone();
            Location hoopCenter = targetHoop.clone();

            Random rand = new Random();
            double missType = rand.nextDouble();

            // First: Ball hits rim/backboard area
            Location rimImpactPoint = hoopCenter.clone();

            if (missType < 0.25) {
                // Hit left side of rim
                rimImpactPoint.add(-0.6 - rand.nextDouble() * 0.3, -0.2, 0);
            } else if (missType < 0.5) {
                // Hit right side of rim
                rimImpactPoint.add(0.6 + rand.nextDouble() * 0.3, -0.2, 0);
            } else if (missType < 0.75) {
                // Hit front of rim
                Vector direction = hoopCenter.toVector().subtract(playerLoc.toVector()).normalize();
                rimImpactPoint.subtract(direction.multiply(0.8 + rand.nextDouble() * 0.3));
                rimImpactPoint.setY(hoopCenter.getY() - 0.3);
            } else {
                // Hit back of backboard
                Vector direction = hoopCenter.toVector().subtract(playerLoc.toVector()).normalize();
                rimImpactPoint.add(direction.multiply(0.5 + rand.nextDouble() * 0.3));
                rimImpactPoint.setY(hoopCenter.getY() + 0.2);
            }

            // Second: Ball bounces away from rim/backboard
            Location missTarget = rimImpactPoint.clone();

            if (missType < 0.25) {
                // Bounced left
                missTarget.add(-2.0 - rand.nextDouble() * 1.5, 1.0 + rand.nextDouble() * 0.5, rand.nextDouble() - 0.5);
            } else if (missType < 0.5) {
                // Bounced right
                missTarget.add(2.0 + rand.nextDouble() * 1.5, 1.0 + rand.nextDouble() * 0.5, rand.nextDouble() - 0.5);
            } else if (missType < 0.75) {
                // Bounced back toward player
                Vector direction = hoopCenter.toVector().subtract(playerLoc.toVector()).normalize();
                missTarget.subtract(direction.multiply(2.5 + rand.nextDouble() * 1.5));
                missTarget.setY(hoopCenter.getY() + 1.5);
            } else {
                // Bounced forward/away
                Vector direction = hoopCenter.toVector().subtract(playerLoc.toVector()).normalize();
                missTarget.add(direction.multiply(2.0 + rand.nextDouble() * 1.5));
                missTarget.setY(hoopCenter.getY() + 1.0);
            }

            // FIRST BEZIER: Player to rim/backboard (impact point)
            double midX1 = (loc1.getX() + rimImpactPoint.getX()) / 2;
            double midY1 = Math.max(loc1.getY(), rimImpactPoint.getY()) + 3.0;
            double midZ1 = (loc1.getZ() + rimImpactPoint.getZ()) / 2;

            final Location p1a = new Location(loc1.getWorld(), midX1, midY1, midZ1);
            final List<Location> bezierPoints1 = bezierCurve(50, loc1, p1a, rimImpactPoint);

            // SECOND BEZIER: Rim/backboard to final miss location (bounce)
            double midX2 = (rimImpactPoint.getX() + missTarget.getX()) / 2;
            double midY2 = Math.max(rimImpactPoint.getY(), missTarget.getY()) + 2.0;
            double midZ2 = (rimImpactPoint.getZ() + missTarget.getZ()) / 2;

            final Location p2a = new Location(loc1.getWorld(), midX2, midY2, midZ2);
            final List<Location> bezierPoints2 = bezierCurve(50, rimImpactPoint, p2a, missTarget);

            setLocked(true);
            new BukkitRunnable() {
                int index = 0;
                boolean onSecondCurve = false;

                @Override
                public void run() {
                    // First curve: travel to rim/backboard
                    if (!onSecondCurve) {
                        if (index >= bezierPoints1.size()) {
                            // Transition to second curve (bounce)
                            onSecondCurve = true;
                            index = 0;
                            return;
                        }

                        int amountToIncrement = Math.max(3, (index / 15) + 1);
                        setVelocity(new Vector());
                        endPhysics(bezierPoints1.get(index));
                        index += amountToIncrement;
                    }
                    // Second curve: bounce away from rim/backboard
                    else {
                        if (index >= bezierPoints2.size() || !isValid()) {
                            setLocked(false);
                            // Apply final velocity away from rim
                            Vector finalVelocity = new Vector(
                                    (Math.random() - 0.5) * 0.3,
                                    -0.2,
                                    (Math.random() - 0.5) * 0.3
                            );
                            setVelocity(finalVelocity);
                            cancel();
                            return;
                        }

                        int amountToIncrement = Math.max(3, (index / 15) + 1);
                        setVelocity(new Vector());
                        endPhysics(bezierPoints2.get(index));
                        index += amountToIncrement;
                    }
                }
            }.runTaskTimer(Partix.getInstance(), 1L, 1L);

        } else {
            // This branch should rarely/never be hit now
            Vector vector = th.getDirection().normalize().multiply(0.3875 + (1.0 - (double) pitch / 45.0) / 2.25);
            double ySpeed = 0.366;
            if (this.game.getCourtLength() == 32.0) {
                vector.multiply(1.1);
            }
            this.setVelocity(player, vector, ySpeed);
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

        if (defenderTeam == null || handlerTeam == null || defenderTeam.equals(handlerTeam) || defenderTeam == GoalGame.Team.SPECTATOR) {
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
        ballHandler.playSound(ballHandler.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.MASTER, 100.0f, 0.8f);
        defender.playSound(defender.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.MASTER, 100.0f, 1.2f);

        // Apply slowness to ball handler
        ballHandler.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0));

        // Send messages
        defender.sendMessage(Component.text("Collision Steal!").color(Colour.allow()).decorate(TextDecoration.BOLD));
        ballHandler.sendMessage(Component.text("Ball poked!").color(Colour.deny()));

        // Track steal/turnover stats
        if ((this.game.getState().equals(GoalGame.State.REGULATION) || this.game.getState().equals(GoalGame.State.OVERTIME)) &&
                this.game.getHomePlayers().contains(ballHandler) != this.game.getHomePlayers().contains(defender)) {
            PlayerStats defenderStats = this.game.getStatsManager().getPlayerStats(defender.getUniqueId());
            defenderStats.incrementSteals();
            PlayerStats handlerStats = this.game.getStatsManager().getPlayerStats(ballHandler.getUniqueId());
            handlerStats.incrementTurnovers();
        }

        // CRITICAL: Track poke data for OOB possession
        this.lastPossessionBeforePoke = ballHandler.getUniqueId();
        this.wasPoked = true;

        this.setStealDelay(10);
        this.delay = 10;
        this.accuracy = 0;
        this.threeEligible = false;

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

        // NEW: Track ALL defender contest values to stack them
        List<Double> defenderContests = new ArrayList<>();
        final double baseContestRadius = 6.0;

        for (Player onlinePlayer : shooter.getWorld().getPlayers()) {
            GoalGame.Team onlinePlayerTeam = game.getTeamOf(onlinePlayer);

            if (onlinePlayerTeam != GoalGame.Team.SPECTATOR) {
                GoalGame.Team playerTeam = game.getTeamOf(onlinePlayer);

                // Check if player is on the other team
                if (playerTeam != null && !playerTeam.equals(shooterTeam)) {
                    double distance = onlinePlayer.getLocation().distance(shooter.getLocation());

                    if (distance > baseContestRadius) {
                        continue; // Too far away
                    }

                    // Calculate directional modifier (front vs back contest)
                    Vector shooterDirection = shooter.getLocation().getDirection().normalize();
                    Vector toDefender = onlinePlayer.getLocation().toVector()
                            .subtract(shooter.getLocation().toVector()).normalize();

                    // Dot product: 1.0 = directly in front, -1.0 = directly behind
                    double dotProduct = shooterDirection.dot(toDefender);

                    // Directional multiplier
                    double directionalMultiplier;
                    if (dotProduct > 0.3) {
                        // Defender is in front (stronger contest)
                        directionalMultiplier = 1.0 + (dotProduct * 0.5); // Up to 1.5x multiplier
                    } else if (dotProduct < -0.3) {
                        // Defender is behind (weaker contest)
                        directionalMultiplier = 0.3; // Only 30% effectiveness from behind
                    } else {
                        // Defender is to the side
                        directionalMultiplier = 1.2; // Up to 1.2x effectiveness from side
                    }

                    // Base contest calculation (distance-based)
                    double baseContest = 1.0 - (distance / baseContestRadius);

                    // Height advantage bonus
                    double heightMultiplier = 1.0;
                    double defenderY = onlinePlayer.getLocation().getY();
                    double shooterY = shooter.getLocation().getY();
                    double floorY = game.getArenaBox().getMinY() + 2.65;

                    // If defender is airborne and at good height
                    if (defenderY > (floorY + 0.5)) {
                        double heightDiff = defenderY - shooterY;

                        // Jumping defender bonus (timing-based)
                        if (heightDiff > -0.5 && heightDiff < 1.5) {
                            // Defender is at good contest height
                            heightMultiplier = 1.4; // 40% bonus for good jump timing
                            System.out.println(onlinePlayer.getName() + " got a JUMP CONTEST!");
                        } else if (heightDiff >= 1.5) {
                            // Defender jumped too high/early
                            heightMultiplier = 1.2; // Still some bonus
                        } else {
                            // Standard airborne contest
                            heightMultiplier = 1.2;
                        }
                    }

                    // Calculate final contest value for THIS defender
                    double contestValue = baseContest * directionalMultiplier * heightMultiplier;

                    // NEW: Add to list instead of just tracking highest
                    if (contestValue > 0.1) { // Only count meaningful contests (10%+)
                        defenderContests.add(contestValue);
                    }
                }
            }
        }

        // NEW: STACKING LOGIC - Multiple defenders compound contest
        if (defenderContests.isEmpty()) {
            return 0.0;
        }

        // Sort contests from highest to lowest
        defenderContests.sort(Collections.reverseOrder());

        double totalContest = 0.0;

        if (defenderContests.size() == 1) {
            // Single defender - full contest value
            totalContest = defenderContests.get(0);
        } else {
            // Multiple defenders - primary defender gets full value
            totalContest = defenderContests.get(0);

            // Secondary defenders add diminishing value
            for (int i = 1; i < defenderContests.size(); i++) {
                // Each additional defender adds 50% of their contest value
                // This stacks but with diminishing returns
                double additionalContest = defenderContests.get(i) * 0.5;
                totalContest += additionalContest;

                System.out.println("Defender #" + (i + 1) + " adding " +
                        String.format("%.1f%%", additionalContest * 100) + " contest (stacked)");
            }
        }

        // Cap contest at 1.0 (100%)
        return Math.min(totalContest, 1.0);
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
                // Lob pass: FIXED - Check if player is throwing at HIGH angle (upward)
                // Negative pitch = looking up, Positive pitch = looking down
                if (player.getLocation().getPitch() < -20) { // CHANGED: was > -20, now < -20
                    passSpeed = 0.65; // Slower for lob
                    verticalComponent = 0.3; // Higher arc
                } else {
                    // Can't throw a lob pass downward
                    player.sendActionBar(Component.text("Lob passes must be thrown upward!").color(NamedTextColor.RED));
                    player.playSound(player.getLocation(), Sound.ENTITY_ARMOR_STAND_BREAK, SoundCategory.MASTER, 100.0f, 1.0f);
                    return false;
                }
            }

            if (this.game.getCourtLength() == 32.0) {
                passSpeed *= 1.1;
            }

            this.setLocation(location.add(direction.clone().multiply(0.45)));
            this.setVelocity(player, direction.clone().multiply(passSpeed), verticalComponent);

            this.game.startAssistTimer(player.getUniqueId());

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
            this.delay = 5;

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

        // Calculate if this is a standing shot
        long timeSinceMovement = System.currentTimeMillis() - this.lastMovementTime;
        boolean isStandingShot = !this.hasMovedWithBall || timeSinceMovement >= STANDING_STILL_DURATION;

        // Mid Range (8-16 blocks)
        if (distanceToHoop <= 16) {
            return isStandingShot ? ShotType.STANDING_MID : ShotType.MOVING_MID;
        }
        // Three Point Range (16+ blocks)
        else {
            return isStandingShot ? ShotType.STANDING_THREE : ShotType.MOVING_THREE;
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

            // PREVENT LAYUPS/DUNKS DURING INBOUND
            if (this.game.inboundingActive && player.equals(this.game.inbounder)) {
                player.sendMessage(Component.text("You can't layup/dunk while inbounding!").color(Colour.deny()));
                return false;
            }

            // SECOND TAP - Meter is already active, execute dunk
            if (this.dunkMeterActive) {
                System.out.println("SECOND Q TAP - Executing dunk at accuracy: " + this.dunkMeterAccuracy);
                return this.executeDunk(player);
            }

            // FIRST TAP - Check if player is in dunk range (distance only, NOT HEIGHT)
            Location targetHoop = this.getTargetHoop(player);
            Location hoopLocation = targetHoop.clone();
            hoopLocation.setY(player.getLocation().getY()); // Ignore Y for distance check
            double horizontalDistance = hoopLocation.distance(player.getLocation());

            // Must be within 5 blocks horizontally
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

            System.out.println("FIRST Q TAP - Dunk meter started for " + player.getName() + " (distance: " + horizontalDistance + ")");
            player.sendMessage(Component.text("DUNK METER ACTIVE! Jump and tap Q again!", NamedTextColor.GREEN));
            return true;
        }

        return false;
    }

    public boolean executeDunkOnRelease(Player player) {
        if (!this.dunkMeterActive || !this.isDunkAttempt) {
            return false;
        }

        if (this.getCurrentDamager() == null || !this.getCurrentDamager().equals(player)) {
            this.dunkMeterActive = false;
            this.dunkMeterAccuracy = 0;
            this.isDunkAttempt = false;
            return false;
        }

        // Use the CURRENT meter accuracy to determine dunk success
        return this.executeDunk(player);
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

        // ← HEIGHT CHECK IS HERE (on execution, not activation)
        Location targetHoop = this.getTargetHoop(player);
        Location hoopLocation = targetHoop.clone();
        hoopLocation.setY(player.getLocation().getY());
        double horizontalDistance = hoopLocation.distance(player.getLocation());

        boolean highEnough = player.getLocation().getY() > this.game.getCenter().getY() + 0.333;
        boolean inRange = horizontalDistance > 0.525 && horizontalDistance < 5.0;

        if (!highEnough || !inRange) {
            player.sendMessage(Component.text("Not high enough to dunk!").color(Colour.deny()));

            // Cancel dunk meter
            this.dunkMeterActive = false;
            this.dunkMeterAccuracy = 0;
            this.isDunkAttempt = false;
            return false;
        }

        // CHECK FOR BLOCK FIRST (before anything else)
        BlockResult blockCheck = checkForDunkBlock(player);

        if (blockCheck.wasBlocked()) {
            // DUNK WAS BLOCKED!
            Player blocker = blockCheck.getBlocker();

            // Calculate block direction (away from dunker, toward blocker's direction)
            Vector blockDirection = blocker.getLocation().getDirection().normalize();
            blockDirection.setY(0.3); // Slight upward angle

            // Set ball velocity in block direction
            this.setLocation(player.getEyeLocation().add(player.getLocation().getDirection().multiply(0.5)));
            this.setVelocity(blockDirection.multiply(0.8), 0.4);

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

            // ADDED: Mark ball as NOT rebound-eligible so OOB check works properly
            this.setReboundEligible(false);

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

        // Rest of executeDunk() stays the same...
        // (NO BLOCK - Continue with normal dunk logic, shot registration, contest calculation, etc.)

// NO BLOCK - Continue with normal dunk logic
        if (!this.isShotAttemptRegistered() && (this.game.getState().equals(GoalGame.State.REGULATION) || this.game.getState().equals(GoalGame.State.OVERTIME))) {
            // Track dunk attempt in FG stats
            PlayerStats stats = this.game.getStatsManager().getPlayerStats(player.getUniqueId());
            stats.incrementFGAttempted();

            this.setShotAttemptRegistered(true);
        }

        Location target = this.getTargetHoop(player);

        DunkContestResult contestResult = calculateDunkContest(player);

        // USE METER ACCURACY to determine dunk success
        boolean successful = calculateDunkSuccessWithMeter(this.dunkMeterAccuracy, contestResult);

        final String percentage = String.format("%.2f", contestResult.getContestValue() * 100.0f);
        player.showTitle(Title.title(
                Component.empty(),
                MiniMessage.miniMessage().deserialize("<green>" + percentage + "% Contested"),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ofMillis(350L))
        ));

        if (successful && contestResult.shouldPosterize() && contestResult.getClosestDefender() != null) {
            applyPosterizer(player, contestResult.getClosestDefender());
        }

        Vector fly = player.getLocation().getDirection().multiply(-1.08);
        fly.setY(0.55);
        player.setVelocity(fly);

        this.setLocation(target.clone().add(0.0, 0.85, 0.0));

        // FIXED: Different animations for make vs miss
        if (successful) {
            // MAKE - Ball goes straight down through hoop
            this.setVelocity(player, player.getLocation().getDirection().normalize().multiply(0.05), -0.2);
            player.sendMessage(Component.text("DUNK!").color(Colour.allow()).decorate(TextDecoration.BOLD));
            player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.MASTER, 0.3f, 1.8f);
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
            // Don't set justDunkedPlayer on a miss - only set it when player lands after successful dunk
        }


        this.dunkMeterActive = false;
        this.dunkMeterAccuracy = 0;
        this.isDunkAttempt = false;
        this.threeEligible = false;
        this.giveaway();
        this.delay = 15;

        // Only set justDunkedPlayer if dunk was SUCCESSFUL (to trigger travel on landing)
        if (successful) {
            this.justDunkedPlayer = player.getUniqueId();
        }

        return true;
    }
    private void steal(Player player) {
        if (!BallFactory.hasBall(player)) {
            if (this.getCurrentDamager() == null) {
                // Player had no ball - simple pickup
                this.setDamager(player);
                this.stealImmunityTicks = 20;
                this.setStealDelay(10);
                this.delay = 10;
                this.catchDelay = 20;
                this.accuracy = 0;
                this.threeEligible = false;
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.MASTER, 100.0f, 1.2f);
                player.getInventory().setHeldItemSlot(0);
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 10, 0));
                return;
            }
            if (this.getCurrentDamager() != player) {
                // Player is stealing from someone who has the ball
                Player oldOwner = this.getCurrentDamager();

                // Track steal/turnover stats
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

                // ALWAYS POKE - 100% chance ball gets poked loose (no snatch)
                this.removeCurrentDamager();
                this.setVelocity(player.getEyeLocation().getDirection().multiply(0.2));
                player.sendMessage("Ball poked!");
                this.accuracy = 0;

                // CRITICAL: Track poke data for OOB possession
                this.lastPossessionBeforePoke = oldOwner.getUniqueId();
                this.wasPoked = true;

                this.setStealDelay(10);
                this.delay = 10;
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

    private boolean calculateDunkSuccessWithMeter(int accuracy, DunkContestResult contestResult) {
        double contestPercentage = contestResult.getContestValue();

        // HARDER GREEN WINDOW: Now only 6-7 (was 5-8)
        boolean greenRelease = accuracy >= 6 && accuracy <= 7;
        // Yellow window: 4-5 and 8 (was 2-4)
        boolean yellowRelease = (accuracy >= 4 && accuracy <= 5) || accuracy == 8;
        // Red: 0-3 (was 0-1)

        double successChance;
        if (greenRelease) {
            // Green: 95% base, heavily reduced by contest
            successChance = 0.95 - (contestPercentage * 0.70);

            if (contestResult.shouldPosterize()) {
                successChance = 1.0; // Guaranteed on posterizer
            }
        } else if (yellowRelease) {
            // Yellow: 60% base, heavily reduced by contest
            successChance = 0.60 - (contestPercentage * 0.55);
        } else {
            // Red: 20% base, heavily reduced by contest
            successChance = 0.20 - (contestPercentage * 0.40);
        }

        return Math.random() < successChance;
    }
    private BlockResult checkForDunkBlock(Player dunker) {
        if (game == null) return new BlockResult(false, null);

        GoalGame.Team dunkerTeam = game.getTeamOf(dunker);
        if (dunkerTeam == null) return new BlockResult(false, null);

        final double blockRadius = 1.5;
        final double floorY = game.getArenaBox().getMinY() + 2.65;

        for (Player defender : dunker.getWorld().getPlayers()) {
            GoalGame.Team defenderTeam = game.getTeamOf(defender);

            if (defenderTeam != null && !defenderTeam.equals(dunkerTeam) && defenderTeam != GoalGame.Team.SPECTATOR) {
                double distance = defender.getLocation().distance(dunker.getLocation());

                if (distance > blockRadius) continue;

                boolean isAirborne = defender.getLocation().getY() > (floorY + 0.5);
                boolean clickedToBlock = defenderBlockAttempts.containsKey(defender.getUniqueId());

                if (isAirborne && clickedToBlock) {
                    long clickTime = defenderBlockAttempts.get(defender.getUniqueId());
                    long timeSinceClick = System.currentTimeMillis() - clickTime;

                    double heightDiff = defender.getLocation().getY() - dunker.getLocation().getY();

                    // Perfect block timing: clicked within 200ms, good height positioning
                    if (timeSinceClick < 200 && heightDiff > -0.2 && heightDiff < 0.8) {
                        // 70% chance to block with perfect timing
                        if (Math.random() < 0.70) {
                            return new BlockResult(true, defender);
                        }
                    }
                    // Good block timing: clicked within 400ms, decent height
                    else if (timeSinceClick < 400 && heightDiff > -0.4 && heightDiff < 1.0) {
                        // 40% chance to block
                        if (Math.random() < 0.40) {
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

            this.checkAnkleBreakerOnDribble(player);

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
            this.checkAnkleBreakerOnDribble(player);

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
            this.checkAnkleBreakerOnDribble(player);

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
    }

    private void runStealImmunity() {
        if (this.stealImmunityTicks > 0) {
            --this.stealImmunityTicks;
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
                    player.playSound(player.getLocation(), Sound.ENTITY_ARMOR_STAND_BREAK, SoundCategory.MASTER, 100.0f, 1.0f);
                    return;
                }
            }
        }

        if (this.stealImmunityTicks > 0 && this.getCurrentDamager() != null && !this.getCurrentDamager().equals(player)) {
            player.playSound(player.getLocation(), Sound.ENTITY_ARMOR_STAND_BREAK, SoundCategory.MASTER, 100.0f, 1.0f);
            return;
        }

        if (this.delay < 1 && this.getStealDelay() < 1) {
            if (this.getLocation().getY() > player.getEyeLocation().getY() - 0.1 && this.getVelocity().getY() < 0.0 && this.getCurrentDamager() == null) {
                player.playSound(player.getLocation(), Sound.ENTITY_ARMOR_STAND_BREAK, SoundCategory.MASTER, 100.0f, 1.0f);
                this.setStealDelay(10);
                return;
            }

            // USE THE OLD STEAL METHOD
            this.steal(player);

            // HANDLE REBOUND PICKUP
            if (this.isReboundEligible()) {
                this.setReboundEligible(false);
                if (this.game.getState().equals(GoalGame.State.REGULATION) || this.game.getState().equals(GoalGame.State.OVERTIME)) {
                    PlayerStats stats = this.game.getStatsManager().getPlayerStats(player.getUniqueId());
                    stats.incrementRebounds();
                    player.sendMessage(Component.text("Rebound!").color(Colour.allow()));
                    System.out.println("Debug: " + player.getName() + " grabbed a rebound! Total rebounds: " + stats.getRebounds());
                }
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
                this.passDelay = 10;
                this.catchDelay = 20;
            }

            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 10, 1));
        } else {
            player.playSound(player.getLocation(), Sound.ENTITY_ARMOR_STAND_BREAK, SoundCategory.MASTER, 100.0f, 1.0f);
        }
    }
    public void error(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_ARMOR_STAND_BREAK, SoundCategory.MASTER, 100.0f, 1.0f);
    }
    public void stepback(Player player) {
        if (this.getCurrentDamager() != null && Math.abs(player.getVelocity().getY()) < 0.1 && player.isOnGround() && this.getCurrentDamager().equals(player)) {
            this.delay = 10;
            player.setVelocity(Position.stabilize(player).getDirection().multiply(-1.1));

            Bukkit.getScheduler().runTaskLater(Partix.getInstance(), () -> {
                if (this.getCurrentDamager() == null || !this.getCurrentDamager().equals(player)) return;

                this.game.setStepbacked(player.getUniqueId());
            }, 10L);
        }
    }

    public boolean collides(Player player) {
        if (this.stealImmunityTicks > 0 && this.getCurrentDamager() != null && !this.getCurrentDamager().equals(player)) {
            return false;
        }
        if (BallFactory.hasBall(player)) {
            return true;
        }

        final double floorY = game.getArenaBox().getMinY() + 2.5;

        // BLOCKING LOGIC - Only active when ball is in the air AND was recently shot
        if (this.getCurrentDamager() == null && this.getLastDamager() != null && this.delay > 0) {
            // Ball was recently released (shot/passed) and is in flight
            if (player.getLocation().getY() > floorY && this.getLocation().getY() > floorY + 0.5) {

                // FIX: Check if blocker is on the OPPOSITE team from shooter
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
                    this.setVelocity(main.getDirection().multiply(-1.0));

                    // Increment block stats for the blocking player
                    if (this.game.getState().equals(GoalGame.State.REGULATION) || this.game.getState().equals(GoalGame.State.OVERTIME)) {
                        PlayerStats blockerStats = this.game.getStatsManager().getPlayerStats(player.getUniqueId());
                        blockerStats.incrementBlocks();
                    }

                    // Send messages
                    player.sendMessage(Component.text("BLOCK!").color(Colour.allow()).decorate(TextDecoration.BOLD));
                    if (shooter != null) {
                        shooter.sendMessage(Component.text("BLOCKED by " + player.getName() + "!").color(Colour.deny()).decorate(TextDecoration.BOLD));
                    }

                    // Play sound
                    player.playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, SoundCategory.MASTER, 1.0f, 1.2f);

                    // ADDED: Store the blocker for OOB possession
                    this.lastShotBlockerUUID = player.getUniqueId();

                    System.out.println("Blocked!");
                    return false;
                }
            }
        }

        if (this.isLayupAttempt && this.getCurrentDamager() == null && this.getLastDamager() != null) {
            // Layup is in flight
            Player shooter = this.getLastDamager();
            GoalGame.Team shooterTeam = game.getTeamOf(shooter);
            GoalGame.Team blockerTeam = game.getTeamOf(player);

            // Check if blocker is on opposite team
            if (shooterTeam != null && blockerTeam != null &&
                    !shooterTeam.equals(blockerTeam) &&
                    blockerTeam != GoalGame.Team.SPECTATOR) {

                // Check if ball is in blockable range (not too high, not on ground)
                double ballHeight = this.getLocation().getY();
                double floorY2 = game.getArenaBox().getMinY() + 2.5;

                if (ballHeight > floorY2 + 0.5 && ballHeight < floorY2 + 4.0) {
                    // Ball is at blockable height
                    double distance = player.getLocation().distance(this.getLocation());

                    // Block radius: 2.0 blocks (slightly larger than dunk block)
                    if (distance <= 2.0) {
                        // Calculate block direction
                        Vector blockDirection = player.getLocation().getDirection().normalize();
                        blockDirection.setY(0.3);

                        // Swat the ball away
                        this.setVelocity(blockDirection.multiply(0.7), 0.35);

                        // Messages and sounds
                        player.sendMessage(Component.text("LAYUP BLOCKED!").color(Colour.allow()).decorate(TextDecoration.BOLD));
                        shooter.sendMessage(Component.text("Layup blocked by " + player.getName() + "!").color(Colour.deny()).decorate(TextDecoration.BOLD));

                        player.playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, SoundCategory.MASTER, 1.0f, 1.2f);
                        shooter.playSound(shooter.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, SoundCategory.MASTER, 1.0f, 0.8f);

                        // Increment block stats
                        if (this.game.getState().equals(GoalGame.State.REGULATION) ||
                                this.game.getState().equals(GoalGame.State.OVERTIME)) {
                            PlayerStats blockerStats = this.game.getStatsManager().getPlayerStats(player.getUniqueId());
                            blockerStats.incrementBlocks();
                        }

                        // Mark ball as NOT rebound-eligible (for OOB)
                        this.setReboundEligible(false);

                        // Cancel layup attempt
                        this.isLayupAttempt = false;
                        this.setLocked(false); // Unlock ball physics

                        // Store blocker for OOB possession
                        this.lastShotBlockerUUID = player.getUniqueId();

                        return false; // Don't give possession
                    }
                }
            }
        }

        // ===== COLLISION STEAL LOGIC - MOVED UP AND FIXED =====
        // When defender collides with offensive player carrying the ball
        if (this.getCurrentDamager() != null && this.getCurrentDamager().isOnGround()) {
            Player ballHandler = this.getCurrentDamager();
            GoalGame.Team handlerTeam = game.getTeamOf(ballHandler);
            GoalGame.Team defenderTeam = game.getTeamOf(player);

            // Check if teams are different and player isn't the ball handler
            if (handlerTeam != null && defenderTeam != null &&
                    !handlerTeam.equals(defenderTeam) &&
                    !player.equals(ballHandler) &&
                    defenderTeam != GoalGame.Team.SPECTATOR) {

                // CRITICAL: Don't allow collision steals during inbound sequences
                if (game.inboundingActive && ballHandler.equals(game.inbounder)) {
                    // Inbounder is protected - no collision steal allowed
                    return false;
                }

                // Attempt collision steal
                if (this.attemptCollisionSteal(player)) {
                    // CRITICAL: After successful steal, don't give possession yet
                    // The ball is now loose on the ground
                    return false;
                } else {
                    // Steal was NOT successful - defender just bounced off
                    // Don't give possession to defender
                    return false;
                }
            }
        }

        // Normal possession pickup (no ball in air, no active defender)
        if (this.getCurrentDamager() == null) {
            this.setDamager(player);
            this.stealImmunityTicks = 20;
            return true;
        }

        // If we reach here, there's a ball handler but collision steal wasn't attempted
        // (shouldn't happen, but be safe)
        return false;
    }

    @Override
    public void setDamager(Player player) {
        this.game.setStepbacked(null);
        super.setDamager(player);
        this.setCurrentDamager(player);
        this.shouldPreventScore = false;
        this.justDunkedPlayer = null;
        this.catchDelay = 20;
        this.layupScored = false;
        this.isLayupAttempt = false;
        this.shotDistance = 0.0;

        // NEW: Reset pass mode to BULLET when picking up ball
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

        // NEW: If inbounder just picked up the ball during inbound, disable immunity immediately
        if (this.game.inboundingActive && player.equals(this.game.inbounder)) {
            this.game.setOutOfBoundsImmunity(false);
            System.out.println("Inbounder picked up ball - OUT OF BOUNDS IMMUNITY DISABLED");
        }
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

        // CRITICAL: Only cancel dunk meter if player has JUMPED and come back down
        // Check: meter is active AND player was airborne AND is now on ground
        if (damager != null && this.dunkMeterActive) {
            // Only cancel if they jumped (were airborne) and came back down
            // This prevents cancelling on the initial ground activation
            if (damager.getVelocity().getY() < -0.1 && damager.isOnGround()) {
                // They jumped and landed without releasing Q
                this.dunkMeterActive = false;
                this.dunkMeterAccuracy = 0;
                this.isDunkAttempt = false;
                damager.sendMessage(Component.text("Dunk meter cancelled").color(Colour.deny()));
                return;
            }
        }

        // REMOVED: Don't reset movement tracking here - it's reset in setDamager() on possession change
        // this.hasMovedWithBall = false;
        // this.lastMovementTime = 0L;

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
                this.forceThrow();
                if (this.game.inboundingActive && damager.equals(this.game.inbounder)) {
                    this.game.dropInboundBarrierButKeepClockFrozen();
                }
            }
        }

        // Check for travel after completing a dunk (landed after dunk animation)
        if (damager != null && this.justDunkedPlayer != null && damager.getUniqueId().equals(this.justDunkedPlayer)) {
            if (damager.isOnGround()) {
                damager.sendTitlePart(TitlePart.TITLE, Component.text(" "));
                damager.sendTitlePart(TitlePart.SUBTITLE, Component.text("Travel! (After dunk)").style(Style.style(Colour.deny(), TextDecoration.BOLD)));
                this.forceThrow();
                this.justDunkedPlayer = null; // Clear the flag
            }
        }
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
        // NEW: Decrement catch delay
        if (this.catchDelay > 0) {
            --this.catchDelay;
        }
    }

    private void modifyHand() {
        // Don't modify hand position during animations
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

            // CRITICAL: Handle dunk meter OR regular accuracy
            if (this.dunkMeterActive) {
                this.nextDunkMeterAccuracy();
                this.displayDunkMeter(); // Always call when meter is active
            } else {
                this.nextAccuracy();
                this.displayAccuracy();
            }

            Player poss = this.getCurrentDamager();
            if (poss == null) return;

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
            if (this.isLayupAttempt && this.perfectShot) {
                // Keep perfectShot active for layups
            } else if (this.perfectShot && this.lastOwnerUUID != null) {
                long elapsed = System.currentTimeMillis() - this.perfectShotStartTime;
                if (elapsed < 1900L) {
                    PlayerDb.get(this.lastOwnerUUID, PlayerDb.Stat.BALL_TRAIL).thenAccept(ballTrailKey -> {
                        CosmeticBallTrail ballTrail = Cosmetics.ballTrails.get(ballTrailKey);
                        if (ballTrail != null && !ballTrail.getKey().equals("balltrail.default")) {
                            ballTrail.applyEffect(this.getLocation());
                        }
                    });
                } else {
                    this.perfectShot = false;
                }
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
        if (!this.isLayupAttempt || this.getCurrentDamager() != null) {
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
                        // GREEN LAYUP - Use bezier curve from backboard to hoop (guaranteed make)
                        Location currentLoc = this.getLocation().clone();

                        // Target should be BELOW the rim (inside the net) so ball goes down through hoop
                        Location hoopTarget = targetHoop.clone();
                        hoopTarget.setY(targetHoop.getY() - 0.8); // Go through the hoop, not to rim level

                        // Create a small arc from backboard down through hoop
                        double midX = (currentLoc.getX() + hoopTarget.getX()) / 2;
                        double midY = Math.max(currentLoc.getY(), targetHoop.getY()) + 0.5; // Arc peaks at/above rim
                        double midZ = (currentLoc.getZ() + hoopTarget.getZ()) / 2;

                        final Location p1 = new Location(currentLoc.getWorld(), midX, midY, midZ);
                        // CHANGED: Reduced from 50 to 25 bezier points for faster animation
                        final List<Location> bezierPoints = bezierCurve(25, currentLoc, p1, hoopTarget);

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

                                // CHANGED: Slower layup animation (was Math.max(4, (index / 20) + 1))
                                int amountToIncrement = Math.max(2, (index / 30) + 1); // NOW: slower increment

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
        // Determine backboard orientation based on court setup
        double centerZ = this.game.getCenter().getZ();
        double backboardZ = backboardCenter.getZ();

        // Check if backboard is perpendicular to Z axis
        boolean isZBackboard = Math.abs(backboardZ - centerZ) > 0.5;

        if (!isZBackboard) {
            return false; // Shouldn't happen in standard courts
        }

        // Get the Z coordinates of the path segment
        double fromZ = from.getZ();
        double toZ = to.getZ();

        // Check if segment crosses the backboard plane
        boolean crosses = (fromZ < backboardZ && toZ > backboardZ) ||
                (fromZ > backboardZ && toZ < backboardZ);

        if (!crosses) return false;

        // Calculate intersection point with backboard plane
        double t = (backboardZ - fromZ) / (toZ - fromZ);
        double intersectX = from.getX() + t * (to.getX() - from.getX());
        double intersectY = from.getY() + t * (to.getY() - from.getY());

        // Check if intersection is within backboard bounds (1.2 block radius for safety)
        double distX = Math.abs(intersectX - backboardCenter.getX());
        double distY = Math.abs(intersectY - backboardCenter.getY());

        return distX <= 1.2 && distY <= 1.2;
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
        super.setCurrentDamager(player);
        if (game.getState().equals(GoalGame.State.INBOUND_WAIT)) {
            game.startCountdown(GoalGame.State.INBOUND, 10);
        } else if (game.getState().equals(GoalGame.State.INBOUND)) {
            game.forceEndCountdown();
        }
    }

    @Override
    public void removeCurrentDamager() {
        super.removeCurrentDamager();
        this.game.setStepbacked(null);
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
                    if (block.getType() != Material.QUARTZ_BLOCK) continue;
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
                    if (block.getType() != Material.QUARTZ_BLOCK) continue;
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
                    if (block.getType() != Material.QUARTZ_BLOCK) continue;
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

        // Calculate knockback direction
        Vector knockback = ballHandler.getLocation().getDirection().normalize().multiply(0.8);
        knockback.setY(0.2); // Slight upward push

        // Apply knockback
        defender.setVelocity(knockback);

        // Spawn invisible pig for sit animation
        Location defenderLoc = defender.getLocation().clone();
        defenderLoc.setY(defenderLoc.getBlockY()); // Ground level

        org.bukkit.entity.Pig sitPig = defenderLoc.getWorld().spawn(defenderLoc, org.bukkit.entity.Pig.class);
        sitPig.setAI(false);
        sitPig.setInvulnerable(true);
        sitPig.setInvisible(true);
        sitPig.setSilent(true);
        sitPig.setGravity(false);

        // Wait for pig to be fully spawned, then add passenger
        Bukkit.getScheduler().runTaskLater(Partix.getInstance(), () -> {
            sitPig.addPassenger(defender);

            // Apply slowness during freeze
            defender.addPotionEffect(
                    new PotionEffect(PotionEffectType.SLOWNESS, freezeDuration, 3, true, false)
            );

            // Send messages
            defender.sendMessage(Component.text("ANKLES BROKEN!").color(Colour.deny()).decorate(TextDecoration.BOLD));
            ballHandler.sendMessage(Component.text("Broke " + defender.getName() + " ankles!").color(Colour.allow()).decorate(TextDecoration.BOLD));

            // Sound effect
            for (Player nearby : defender.getWorld().getPlayers()) {
                if (nearby.getLocation().distance(defender.getLocation()) < 30) {
                    nearby.playSound(defender.getLocation(), Sound.ENTITY_ITEM_BREAK, SoundCategory.MASTER, 0.8f, 0.6f);
                }
            }
        }, 1L);

        // Remove pig and let defender stand up after freeze duration
        Bukkit.getScheduler().runTaskLater(Partix.getInstance(), () -> {
            sitPig.remove();
            defender.sendMessage(Component.text("You got back up!").color(Colour.allow()));
        }, freezeDuration);

        // Grant immunity for 3 seconds
        grantAnkleBreakImmunity(defender);
    }
    public void checkAnkleBreakerOnDribble(Player ballHandler) {
        // Check if there's an active steal attempt on this player
        if (stealAttemptDefender == null) {
            return; // No steal was attempted
        }

        Player defender = Bukkit.getPlayer(stealAttemptDefender);
        if (defender == null) {
            stealAttemptDefender = null;
            return;
        }

        long timeSinceSteals = System.currentTimeMillis() - stealAttemptTime;

        // Check if we're still in dribble counter window (0.3 seconds = 300ms)
        if (timeSinceSteals > 300L) {
            stealAttemptDefender = null; // Window closed
            return;
        }

        // Check if defender has immunity
        if (hasAnkleBreakImmunity(defender)) {
            return;
        }

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
    public void clearPerfectShot() {
    }

    public UUID getTrueLastPossessor() {
        // If shot was blocked, the SHOOTER gets OOB possession (not the blocker)
        if (this.lastShotBlockerUUID != null) {
            // The blocker UUID is stored, but we need to return the SHOOTER's UUID
            // The shooter is the lastPossessorUUID (person who shot before block)
            if (this.lastPossessorUUID != null) {
                Player shooter = Bukkit.getPlayer(this.lastPossessorUUID);
                if (shooter != null) {
                    System.out.println("Shot was blocked - shooter's team gets OOB possession: " + shooter.getName());
                    return this.lastPossessorUUID; // ← FIXED: Return shooter UUID
                }
            }
        }

        // If ball was poked, return who had it BEFORE the poke (the victim)
        if (this.wasPoked && this.lastPossessionBeforePoke != null) {
            System.out.println("Ball was poked - true last possessor (victim) is: " + Bukkit.getPlayer(this.lastPossessionBeforePoke).getName());
            return this.lastPossessionBeforePoke;
        }

        // Otherwise return the actual last possessor
        if (this.lastPossessorUUID != null) {
            System.out.println("Normal possession - last possessor is: " + Bukkit.getPlayer(this.lastPossessorUUID).getName());
            return this.lastPossessorUUID;
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

                        // Perfect timing - STRONG CONTEST, low posterizer risk
                        if (timeSinceClick < 300 && heightDiff > -0.3 && heightDiff < 1.0) {
                            contestValue = 0.90 - (distance / contestRadius) * 0.3; // BUFFED from 0.85
                            // Good timing = low posterizer risk
                        }
                        // Good timing - DECENT CONTEST, medium posterizer risk
                        else if (timeSinceClick < 600) {
                            contestValue = 0.65 - (distance / contestRadius) * 0.2; // BUFFED from 0.55
                            badTiming = true; // Medium risk
                        }
                        // Bad timing - WEAK CONTEST, HIGH posterizer risk
                        else {
                            contestValue = 0.25; // BUFFED from 0.15 (still gives some contest)
                            badTiming = true; // High risk
                        }
                    } else {
                        // Jumped without clicking - MODERATE CONTEST, HIGH posterizer risk
                        if (heightDiff > -0.5 && heightDiff < 1.2) {
                            contestValue = 0.55 - (distance / contestRadius) * 0.25; // BUFFED from 0.45
                        } else {
                            contestValue = 0.30; // BUFFED from 0.20
                        }
                        badTiming = true; // Always high risk when not clicking
                    }
                } else {
                    // GROUNDED DEFENDER - WEAK CONTEST, MODERATE posterizer risk
                    contestValue = 0.20 - (distance / contestRadius) * 0.15; // NERFED from 0.25
                }

                // POSTERIZER CONDITIONS - Airborne defenders at higher risk!

                // Condition 1: Jumped with bad timing (most common)
                if (isAirborne && badTiming && distance < 2.8 && this.dunkMeterAccuracy >= 4) {
                    shouldPosterize = true;
                }

                // Condition 2: Green release + close + defender jumped (high risk)
                if (isAirborne && distance < 2.5 && this.dunkMeterAccuracy >= 6 && this.dunkMeterAccuracy <= 7) {
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
                        }
                    }, 1L);

                    // Remove pig after 2 seconds (40 ticks)
                    Bukkit.getScheduler().runTaskLater(Partix.getInstance(), () -> {
                        sitPig.remove();
                        victim.sendMessage(Component.text("Get up!").color(Colour.allow()));
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

    @RequiredArgsConstructor
    @Getter
    enum ShotType {
        LAYUP(0.65, false),           // Not affected by standing/moving
        STANDING_MID(0.50, false),    // 50% green threshold
        MOVING_MID(0.45, true),       // 45% green threshold
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