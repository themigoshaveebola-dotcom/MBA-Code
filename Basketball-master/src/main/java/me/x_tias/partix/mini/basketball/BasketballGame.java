/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.TextComponent
 *  net.kyori.adventure.text.event.ClickEvent
 *  net.kyori.adventure.text.event.HoverEventSource
 *  net.kyori.adventure.text.format.Style
 *  net.kyori.adventure.text.format.TextColor
 *  net.kyori.adventure.text.format.TextDecoration
 *  org.bukkit.Bukkit
 *  org.bukkit.Color
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.Particle
 *  org.bukkit.Particle$DustOptions
 *  org.bukkit.Sound
 *  org.bukkit.SoundCategory
 *  org.bukkit.World
 *  org.bukkit.block.Block
 *  org.bukkit.boss.BarColor
 *  org.bukkit.boss.BarFlag
 *  org.bukkit.boss.BarStyle
 *  org.bukkit.boss.BossBar
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitRunnable
 *  org.bukkit.scheduler.BukkitTask
 *  org.bukkit.util.Vector
 */
package me.x_tias.partix.mini.basketball;

import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.TitlePart;
import me.x_tias.partix.Partix;
import me.x_tias.partix.database.BasketballDb;
import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import me.x_tias.partix.database.PlayerDb;
import me.x_tias.partix.database.SeasonDb;
import me.x_tias.partix.mini.anteup.AnteUpManager;
import me.x_tias.partix.mini.game.GoalGame;
import me.x_tias.partix.mini.game.PlayerStats;
import me.x_tias.partix.mini.game.PlayerStatsManager;
import me.x_tias.partix.plugin.athlete.Athlete;
import me.x_tias.partix.plugin.athlete.AthleteManager;
import me.x_tias.partix.plugin.ball.Ball;
import me.x_tias.partix.plugin.ball.BallFactory;
import me.x_tias.partix.plugin.ball.BallType;
import me.x_tias.partix.plugin.ball.types.Basketball;
import me.x_tias.partix.plugin.cosmetics.CosmeticSound;
import me.x_tias.partix.plugin.gui.GUI;
import me.x_tias.partix.plugin.gui.ItemButton;
import me.x_tias.partix.plugin.settings.CompType;
import me.x_tias.partix.plugin.settings.GameType;
import me.x_tias.partix.plugin.settings.Settings;
import me.x_tias.partix.plugin.sidebar.Sidebar;
import me.x_tias.partix.plugin.team.BaseTeam;
import me.x_tias.partix.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class BasketballGame
        extends GoalGame {
    private final PlayerStatsManager statsManager = new PlayerStatsManager();
    private final Map<UUID, Boolean> shotInProgress = new HashMap<>();
    private final Map<UUID, Long> lastReboundTime = new HashMap<>();
    // Rebound machine shot tracking (per player)
    private final Map<UUID, Integer> reboundMachineShotsTaken = new HashMap<>();
    private final Map<UUID, Integer> reboundMachineShotsMade = new HashMap<>();
    private final Map<UUID, Integer> reboundMachineShotsMissed = new HashMap<>();
    private BukkitTask pregameTask = null;
    @Getter
    private final Location location;
    private final List<UUID> joinedHome = new ArrayList<>();
    private final List<UUID> joinedAway = new ArrayList<>();
    private final HashMap<UUID, Integer> points = new HashMap<>();
    private final HashMap<UUID, Integer> threes = new HashMap<>();
    private final Map<UUID, Boolean> activeShots = new HashMap<>();
    @Getter
    private final double courtLength;
    private final Map<String, Object> customProperties = new HashMap<>();
    public boolean inboundingActive = false;
    public Player inbounder;
    private int stoppageTicks = 100;
    public int getAwayTimeouts() {
        return this.awayTimeouts;
    }
    public int getHomeTimeouts() {
        return this.homeTimeouts;
    }
    private Location stoppageLastLocation;
    @Getter
    private int shotClockTicks = 480;
    private BukkitTask timeoutTask = null;
    private int timeoutSecs = 0;
    private final boolean shotClockFrozen = false;
    private boolean justScored = false; // NEW: Track if a goal just happened
    public boolean isSingleHoopMode;
    public boolean pressPrevention = false;
    public GoalGame.Team restrictedTeam = null;
    public boolean isHalfCourt1v1;
    public boolean isRecGame = false; // Flag to identify Rec games
    public boolean isPhysicalQueueGame = false; // Flag to identify physical queue games
    private boolean needsPositionReset = false;
    private long resetStartTime = 0L;
    private long pressPreventionStartTime = 0L;
    private static final long PRESS_PREVENTION_TIMEOUT = 8000L; // 8 seconds in milliseconds
    private static final long RESET_DELAY = 3000L;
    private UUID offensivePlayer = null;
    private UUID defensivePlayer = null;
    private UUID homeCoach = null;
    private UUID awayCoach = null;
    private boolean shotAttemptDetected = false;
    private boolean shotClockStopped = false;
    public boolean shotClockFrozenForInbound = false; // NBA rule: shot clock pauses on OOB
    public boolean gameClockFrozenForInbound = false; // NBA rule: game clock pauses on OOB
    public boolean inboundTouchedByInbounder = false;
    public boolean inbounderHasReleased = false;
    public GoalGame.Team inboundingTeam;
    public Location inboundSpot;
    private BukkitTask inboundBarrierTask;
    private BukkitTask inboundTimer;
    private Ball inboundBall;
    private GoalGame.Team shotAttemptTeam = null;
    private GoalGame.Team lastPossessionTeam = null;
    private boolean shotClockEnabled = true;
    private boolean quarterBuzzerPlayed = false;
    private boolean buzzerPlayed = false;
    private UUID currentPossessor = null;
    private long possessionStartTime = 0L;
    private long activeGameStartTime = 0L; // Track when REGULATION/OVERTIME started
    private Map<UUID, Long> playerCourtTime = new HashMap<>(); // Track last update time per player
    public long inboundPassTime = 0L; // Track when inbounder passed the ball
    public long inboundStartTime = 0L; // Track when inbound sequence started (for 5s timeout)
    private static final long INBOUND_OOB_DELAY = 1000L; // 1 second grace period in milliseconds
    private static final long INBOUND_TIMEOUT = 5000L; // 5 second safety timeout for inbound
    private static final long REBOUND_COOLDOWN_MS = 2000L; // 2 seconds in milliseconds
    private int homeTimeouts = 4;
    private int awayTimeouts = 4;
    private BossBar timeoutBar;
    private final boolean reboundEligible = false;
    private UUID assistEligiblePasser;
    private long assistTimerEndTime;
    @Getter @Setter private UUID stepbacked;
    private boolean assistTimerActive = false;
    @Getter
    @Setter
    private Team outOfBoundsLostTeam;
    @Setter
    @Getter
    private boolean outOfBoundsImmunity = false;
    @Getter
    private boolean outOfBoundsSide;
    @Getter
    private double outOfBoundsZ;
    @Getter
    private boolean outOfBoundsHome;

    @Getter
    private BoundingBox homeJump;

    @Getter
    private BoundingBox awayJump;

    public boolean isInMBAStadium() {
        Location gameLoc = this.getLocation();

        double[][] mbaCoords = {
                {448.5, -61, 963.5},    // Washington Withers
                {-42.5, -61, 963.5},    // Philadelphia 64s
                {-532.5, -61, 963.5},   // Chicago Bows
                {-532.5, -61, 452.5},   // Brooklyn Buckets
                {-576.5, -61, -510.5},  // Miami Magma Cubes
                {-576.5, -61, -19.5},   // Golden State Guardians
                {576.5, -61, -510.5},   // Atlanta Allays
                {576.5, -61, -19.5},    // LA Creepers
                {190.5, -61, -1000.5}    // Boston Breeze (NEW)
        };

        for (double[] coord : mbaCoords) {
            if (Math.abs(gameLoc.getX() - coord[0]) < 1.0 &&
                    Math.abs(gameLoc.getY() - coord[1]) < 1.0 &&
                    Math.abs(gameLoc.getZ() - coord[2]) < 1.0) {
                return true;
            }
        }
        return false;
    }
    public BasketballGame(Settings settings, Location location, double xDistance, double yDistance, double xLength, double zWidth, double yHeight) {
        this.courtLength = xDistance;
        this.location = location.clone();

        // ✅ Detect 1v1 half-court FIRST - BEFORE setup()
        this.isHalfCourt1v1 = (xDistance == 13.0 && settings.playersPerTeam == 1);
        this.isSingleHoopMode = this.isHalfCourt1v1;

        if (this.isHalfCourt1v1) {
            Bukkit.getLogger().info("✓ Half-court 1v1 detected - First to 21 with Win by 2");
        }

        // ✅ NOW call setup() - it will see the flags are already set
        this.setup(settings.copy(), location, xDistance, yDistance, xLength, zWidth, yHeight);

        this.homeJump = this.getJumpBox(this.getHomeSpawn());
        this.awayJump = this.getJumpBox(this.getAwaySpawn());
    }

    @Override
    public PlayerStatsManager getStatsManager() {
        return this.statsManager;
    }

    @Override
    public void resetStats() {
        this.statsManager.resetStats();
        this.lastReboundTime.clear();
        this.justScored = false; // NEW: Clear flag
        
        // Clear press prevention on reset
        this.pressPrevention = false;
        this.pressPreventionStartTime = 0L;
        this.restrictedTeam = null;
        System.out.println("Press prevention cleared - Stats/play reset");
        
        // Clear clock freeze flags on reset
        this.shotClockFrozenForInbound = false;
        this.gameClockFrozenForInbound = false;
        System.out.println("Clock freeze flags cleared - Stats/play reset");
        
        // Clear inbound state on reset
        this.inboundingActive = false;
        this.inbounder = null;
        this.inboundTouchedByInbounder = false;
        this.inbounderHasReleased = false;
        this.inboundStartTime = 0L;
        System.out.println("Inbound state cleared - Stats/play reset");

        for (Player p : this.getHomePlayers()) {
            p.sendMessage(Component.text("Your stats have been reset.").color(Colour.allow()));
        }
        for (Player p : this.getAwayPlayers()) {
            p.sendMessage(Component.text("Your stats have been reset.").color(Colour.allow()));
        }
    }

    public void setCoach(GoalGame.Team team, UUID playerUUID) {
        if (team == GoalGame.Team.HOME) {
            this.homeCoach = playerUUID;
        } else if (team == GoalGame.Team.AWAY) {
            this.awayCoach = playerUUID;
        }
    }

    public UUID getCoach(GoalGame.Team team) {
        if (team == GoalGame.Team.HOME) {
            return this.homeCoach;
        } else if (team == GoalGame.Team.AWAY) {
            return this.awayCoach;
        }
        return null;
    }
    public boolean isCoach(UUID playerUUID) {
        return playerUUID.equals(this.homeCoach) || playerUUID.equals(this.awayCoach);
    }
    public GoalGame.Team getCoachTeam(UUID playerUUID) {
        if (playerUUID.equals(this.homeCoach)) {
            return GoalGame.Team.HOME;
        } else if (playerUUID.equals(this.awayCoach)) {
            return GoalGame.Team.AWAY;
        }
        return null;
    }
    public void removeCoach(UUID playerUUID) {
        if (playerUUID.equals(this.homeCoach)) {
            this.homeCoach = null;
        } else if (playerUUID.equals(this.awayCoach)) {
            this.awayCoach = null;
        }
    }
    private void clearCoachIfPresent(UUID playerUUID) {
        if (this.isCoach(playerUUID)) {
            this.removeCoach(playerUUID);
            this.sendMessage(Component.text("🎯 Coach " + Bukkit.getPlayer(playerUUID).getName() + " has left the game.").color(Colour.partix()));
        }
    }

    private void checkPressPrevention() {
        // SKIP FOR 1V1 GAMES
        if (this.isHalfCourt1v1 || this.isSingleHoopMode) {
            this.pressPrevention = false;
            this.pressPreventionStartTime = 0L; // ✅ RESET TIMER
            this.restrictedTeam = null;
            return;
        }

        // Only enforce if press prevention is active
        if (!this.pressPrevention || this.restrictedTeam == null) {
            return;
        }

        // Get center line and hoop locations
        double centerZ = this.getCenter().getZ();
        Location homeHoopCenter = this.getHomeNet().getCenter().toLocation(this.getCenter().getWorld());
        Location awayHoopCenter = this.getAwayNet().getCenter().toLocation(this.getCenter().getWorld());

        // Get the restricted players (defensive team)
        List<Player> restrictedPlayers = this.restrictedTeam == Team.HOME ?
                this.getHomePlayers() : this.getAwayPlayers();

        // Get the inbounding team
        GoalGame.Team inboundingTeam = this.restrictedTeam == Team.HOME ? Team.AWAY : Team.HOME;

        // FIXED: Press prevention only protects the inbounding team's DEFENSIVE half (where their hoop is)
        // The inbounding team is on offense, so we protect their backcourt (their own hoop)
        Location protectedHoop = inboundingTeam == Team.HOME ? homeHoopCenter : awayHoopCenter;
        double protectedZ = protectedHoop.getZ();

        // Determine which direction is the protected zone (toward their own hoop)
        boolean homeHoopIsPositiveZ = homeHoopCenter.getZ() > centerZ;

        // For each restricted player, check if they're in the protected zone
        for (Player player : restrictedPlayers) {
            if (player == null || !player.isOnline()) continue;

            double playerZ = player.getLocation().getZ();

            // Check if player is in the protected half (inbounding team's defensive half)
            boolean isInProtectedZone;
            if (inboundingTeam == Team.HOME) {
                // Home is inbounding - protect home's defensive half (toward home hoop)
                if (homeHoopIsPositiveZ) {
                    isInProtectedZone = playerZ > centerZ; // Home hoop is positive Z
                } else {
                    isInProtectedZone = playerZ < centerZ; // Home hoop is negative Z
                }
            } else {
                // Away is inbounding - protect away's defensive half (toward away hoop)
                if (homeHoopIsPositiveZ) {
                    isInProtectedZone = playerZ < centerZ; // Away hoop is negative Z
                } else {
                    isInProtectedZone = playerZ > centerZ; // Away hoop is positive Z
                }
            }

            // If in protected zone, push them back to half court
            if (isInProtectedZone) {
                // Calculate push direction (toward half court)
                Vector pushDirection = new Vector();

                if (inboundingTeam == Team.HOME) {
                    // Push away team back toward half court
                    if (homeHoopIsPositiveZ) {
                        pushDirection.setZ(-0.5); // Push toward center (negative)
                    } else {
                        pushDirection.setZ(0.5); // Push toward center (positive)
                    }
                } else {
                    // Push home team back toward half court
                    if (homeHoopIsPositiveZ) {
                        pushDirection.setZ(0.5); // Push toward center (positive)
                    } else {
                        pushDirection.setZ(-0.5); // Push toward center (negative)
                    }
                }

                player.setVelocity(pushDirection);
            }
        }
    }

    private void checkPressPreventionWithLateGameException() {
        // SKIP FOR 1V1 GAMES
        if (this.isHalfCourt1v1 || this.isSingleHoopMode) {
            this.pressPrevention = false;
            this.pressPreventionStartTime = 0L; // ✅ RESET TIMER
            this.restrictedTeam = null;
            return;
        }

        // Only enforce if press prevention is active
        if (!this.pressPrevention || this.restrictedTeam == null) {
            return;
        }

        // ✅ SAFETY FALLBACK: Auto-disable after 8 seconds
        long currentTime = System.currentTimeMillis();
        if (this.pressPreventionStartTime > 0) {
            long elapsed = currentTime - this.pressPreventionStartTime;
            if (elapsed > PRESS_PREVENTION_TIMEOUT) {
                System.out.println("⚠️ Press prevention auto-disabled after 8 seconds (safety fallback)");
                this.pressPrevention = false;
                this.restrictedTeam = null;
                this.pressPreventionStartTime = 0L;
                return;
            }
        }

        // Check if under 1 minute (1200 ticks = 60 seconds)
        int gameTime = this.getTimeTicks();
        if (gameTime < 1200) {
            // Under 1 minute - disable press prevention if it's active
            if (this.pressPrevention) {
                this.pressPrevention = false;
                this.restrictedTeam = null;
                this.pressPreventionStartTime = 0L;
                System.out.println("✓ Press prevention lifted - Under 1 minute remaining (" + (gameTime / 20) + " seconds)");
            }
            return;
        }

        // Enforce press prevention
        this.checkPressPrevention();
    }

    public void resetReboundMachineStats() {
        this.reboundMachineShotsTaken.clear();
        this.reboundMachineShotsMade.clear();
        this.reboundMachineShotsMissed.clear(); // ADD THIS LINE
        this.sendMessage(Component.text("Rebound Machine stats reset for all players.").color(Colour.partix()));
    }

    public void resetAllStats() {
        System.out.println("Debug: Resetting all player stats.");
        this.statsManager.resetStats();
    }

    public void resetShotClock() {
        if (this.isHalfCourt1v1) {
            this.shotClockTicks = 240;  // 12 seconds
        } else {
            this.shotClockTicks = 480;  // 24 seconds
        }

        this.shotClockStopped = false;
        this.shotAttemptDetected = false;
        this.shotAttemptTeam = null;
        this.quarterBuzzerPlayed = false;

    }
    // NEW METHOD: Accept the scorer as a parameter (MAKE IT TAKE IT)
    public void resetHalfCourt1v1PositionsWithOffense(Player offensivePlayer) {
        if (!this.isHalfCourt1v1) {
            return;
        }

        if (offensivePlayer == null || !offensivePlayer.isOnline()) {
            Bukkit.getLogger().warning("❌ No valid offensive player for 1v1 reset");
            return;
        }

        // Find the defensive player (the OTHER player)
        List<Player> allPlayers = new ArrayList<>();
        allPlayers.addAll(this.getHomePlayers());
        allPlayers.addAll(this.getAwayPlayers());

        if (allPlayers.size() != 2) {
            Bukkit.getLogger().warning("❌ 1v1 game doesn't have exactly 2 players!");
            return;
        }

        Player defensivePlayer = null;
        for (Player p : allPlayers) {
            if (!p.equals(offensivePlayer)) {
                defensivePlayer = p;
                break;
            }
        }

        if (defensivePlayer == null) {
            Bukkit.getLogger().warning("❌ Could not find defensive player!");
            return;
        }

        Bukkit.getLogger().info("✓ MAKE IT TAKE IT: " + offensivePlayer.getName() + " scored, gets ball back");

        // Now call the position reset with the correct players
        this.resetHalfCourt1v1PositionsInternal(offensivePlayer, defensivePlayer);
    }

    // REFACTORED: Original method now just finds players randomly (for game start)
    private void resetHalfCourt1v1Positions() {
        if (!this.isHalfCourt1v1) {
            return;
        }

        List<Player> allPlayers = new ArrayList<>();
        allPlayers.addAll(this.getHomePlayers());
        allPlayers.addAll(this.getAwayPlayers());

        if (allPlayers.size() != 2) {
            return;
        }

        // For initial reset (not after scoring), just pick randomly
        Player offensivePlayer = allPlayers.get(0);
        Player defensivePlayer = allPlayers.get(1);

        Bukkit.getLogger().info("✓ Initial 1v1 position reset (random assignment)");
        this.resetHalfCourt1v1PositionsInternal(offensivePlayer, defensivePlayer);
    }

    // INTERNAL METHOD: Does the actual positioning work
    private void resetHalfCourt1v1PositionsInternal(Player offensivePlayer, Player defensivePlayer) {
        if (!this.isHalfCourt1v1) {
            return;
        }

        World world = offensivePlayer.getWorld();
        Location homeHoopCenter = this.getHomeNet().getCenter().toLocation(world);
        Location centerOfCourt = this.getCenter().clone();

        double direction = homeHoopCenter.getZ() > centerOfCourt.getZ() ? 1.0 : -1.0;

        // ✅ OFFENSIVE player at 3-point line (top of the key) - FURTHER from hoop
        Location offensiveSpawn = centerOfCourt.clone();
        offensiveSpawn.setZ(centerOfCourt.getZ() - (6.5 * direction));
        offensiveSpawn.setX(homeHoopCenter.getX());
        offensiveSpawn.setY(world.getHighestBlockYAt((int)offensiveSpawn.getX(), (int)offensiveSpawn.getZ()) + 1.0);
        offensiveSpawn.setPitch(0);
        offensiveSpawn.setYaw(0);

        // ✅ DEFENSIVE player closer to rim (paint area) - CLOSER to hoop
        Location defensiveSpawn = centerOfCourt.clone();
        defensiveSpawn.setZ(centerOfCourt.getZ() + (3.0 * direction));
        defensiveSpawn.setX(homeHoopCenter.getX());
        defensiveSpawn.setY(world.getHighestBlockYAt((int)defensiveSpawn.getX(), (int)defensiveSpawn.getZ()) + 1.0);
        defensiveSpawn.setPitch(0);
        defensiveSpawn.setYaw(0);

        // ===== REMOVE CURRENT BALL FIRST =====
        this.removeBalls();

        // ===== TELEPORT PLAYERS IMMEDIATELY =====
        offensivePlayer.teleport(offensiveSpawn);
        defensivePlayer.teleport(defensiveSpawn);

        // FREEZE PLAYERS for 2 seconds
        offensivePlayer.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 5, false, false));
        defensivePlayer.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 5, false, false));

        this.sendTitle(Component.text("RESET").color(Colour.partix()).decorate(TextDecoration.BOLD));

        // ===== MAKE FINAL COPY FOR LAMBDA =====
        final Player finalOffensivePlayer = offensivePlayer;

        // Spawn ball in offensive player's hand AFTER 2 seconds
        Bukkit.getScheduler().runTaskLater(Partix.getInstance(), () -> {
            if (!finalOffensivePlayer.isOnline()) {
                return;
            }
            Location ballSpawnLoc = finalOffensivePlayer.getEyeLocation().clone();
            Ball newBall = this.setBall(BallFactory.create(ballSpawnLoc, BallType.BASKETBALL, this));
            newBall.setStealDelay(0);
            newBall.setVelocity(0, 0, 0);
            
            // Initialize teammate hotkeys for new ball (even though 1v1, keeps code consistent)
            if (newBall instanceof Basketball basketball) {
                basketball.initializeAllTeammateHotkeys(this);
            }

            this.sendTitle(Component.text("PLAY!").color(Colour.allow()).decorate(TextDecoration.BOLD));
            Bukkit.getLogger().info("✓ 1V1 RESET: Ball spawned in " + finalOffensivePlayer.getName() + "'s hand");
        }, 40L);

        Bukkit.getLogger().info("✓ 1V1 RESET: Offense=" + offensivePlayer.getName() + ", Defense=" + defensivePlayer.getName());
    }

    private void handleHalfCourt1v1Reset() {
        if (!this.isHalfCourt1v1 || !this.needsPositionReset) {
            return;
        }

        long timeSinceReset = System.currentTimeMillis() - this.resetStartTime;

        if (timeSinceReset < RESET_DELAY) {
            long remaining = (RESET_DELAY - timeSinceReset) / 1000L;
            if (remaining >= 0) {
                Component countdown = Component.text("Ball spawning in " + remaining + "s")
                        .color(TextColor.color(0xFFFF00));
                for (Player p : this.getPlayers()) {
                    p.sendActionBar(countdown);
                }
            }
            return;
        }

        Player offPlayer = Bukkit.getPlayer(this.offensivePlayer);
        Player defPlayer = Bukkit.getPlayer(this.defensivePlayer);

        if (offPlayer == null || defPlayer == null) {
            this.needsPositionReset = false;
            return;
        }

        Location ballSpawnLoc = offPlayer.getEyeLocation().clone();

        Ball newBall = this.setBall(BallFactory.create(ballSpawnLoc, BallType.BASKETBALL, this));
        newBall.setStealDelay(0);
        newBall.setVelocity(0, 0, 0);
        
        // Initialize teammate hotkeys for new ball (even though 1v1, keeps code consistent)
        if (newBall instanceof Basketball basketball) {
            basketball.initializeAllTeammateHotkeys(this);
        }

        this.sendTitle(Component.text("PLAY!").color(Colour.allow()).decorate(TextDecoration.BOLD));


        this.needsPositionReset = false;
        this.offensivePlayer = null;
        this.defensivePlayer = null;
    }

    private Team determineNextPossession() {
        if (this.getBall() == null) {
            return Team.HOME;
        }

        Player lastPossessor = this.getBall().getCurrentDamager();
        if (lastPossessor == null) {
            return Team.HOME;
        }

        if (this.getHomePlayers().contains(lastPossessor)) {
            return Team.HOME;
        } else if (this.getAwayPlayers().contains(lastPossessor)) {
            return Team.AWAY;
        }

        return Team.HOME;
    }

    public void callTimeout(final GoalGame.Team callingTeam) {
        // Validation checks
        if (this.getState() != GoalGame.State.REGULATION) {
            return;
        }
        if (this.settings.compType == CompType.RANKED) {
            return;
        }

        // Check remaining timeouts
        int remaining = callingTeam == GoalGame.Team.HOME ? this.homeTimeouts : this.awayTimeouts;
        if (remaining <= 0) {
            return;
        }

        // Decrement timeout count
        if (callingTeam == GoalGame.Team.HOME) {
            --this.homeTimeouts;
        } else {
            --this.awayTimeouts;
        }

        // Set game to stoppage (music will resume via setState override)
        this.setState(GoalGame.State.STOPPAGE);

        // Remove balls
        this.removeBalls();

        // Broadcast timeout message
        this.sendMessage(Component.text(callingTeam.name() + " called a timeout!").color(Colour.partix()));

        // Update display
        this.updateDisplay();

        // Create timeout boss bar
        this.timeoutBar = Bukkit.createBossBar("Timeout: 60s", BarColor.WHITE, BarStyle.SOLID);
        this.getPlayers().forEach(p -> this.timeoutBar.addPlayer(p));

        // Set timeout duration
        this.timeoutSecs = 60;

        // Cancel any existing timeout task
        if (this.timeoutTask != null) {
            this.timeoutTask.cancel();
            this.timeoutTask = null;
        }

        // Start timeout countdown task
        this.timeoutTask = new BukkitRunnable() {
            @Override
            public void run() {
                --BasketballGame.this.timeoutSecs;
                BasketballGame.this.timeoutBar.setProgress((double) BasketballGame.this.timeoutSecs / 60.0);
                BasketballGame.this.timeoutBar.setTitle("Timeout: " + BasketballGame.this.timeoutSecs + "s");

                if (BasketballGame.this.timeoutSecs == 10) {
                    List<Player> teamPlayers = callingTeam == GoalGame.Team.HOME ?
                            BasketballGame.this.getHomePlayers() :
                            BasketballGame.this.getAwayPlayers();
                    teamPlayers.forEach(p -> p.sendMessage(
                            Component.text("10 seconds remaining: get ready to inbound!")
                                    .color(Colour.partix())
                    ));
                }

                if (BasketballGame.this.timeoutSecs <= 0) {
                    BasketballGame.this.timeoutBar.removeAll();
                    BasketballGame.this.timeoutTask = null;
                    this.cancel();

                    // Start inbound sequence for the team that called timeout
                    BasketballGame.this.endTimeoutInbound(callingTeam);
                }
            }
        }.runTaskTimer(Partix.getInstance(), 20L, 20L);

        // Remove timeout buttons from all players
        this.getPlayers().stream()
                .filter(p -> p.getInventory().contains(Material.POLISHED_BLACKSTONE_BUTTON))
                .forEach(p -> p.getInventory().remove(Material.POLISHED_BLACKSTONE_BUTTON));
    }

    private void endTimeoutInbound(GoalGame.Team inboundingTeam) {
        // Determine sideline inbound location (along the length of the court)
        BoundingBox arenaBox = this.getArenaBox();
        Location sidelineSpot = this.getCenter().clone();

        // Place ball at SIDELINE (side of court along X-axis)
        // Use the minX or maxX side depending on team
        if (inboundingTeam == Team.HOME) {
            sidelineSpot.setX(arenaBox.getMinX() - 0.5); // Home team inbounds from one sideline
        } else {
            sidelineSpot.setX(arenaBox.getMaxX() + 0.5); // Away team inbounds from other sideline
        }
        sidelineSpot.setZ(this.getCenter().getZ()); // Center of court length-wise
        sidelineSpot.setY(this.getCenter().getY() + 1.2); // Proper height

        // Set up out of bounds state for inbound
        this.setState(State.OUT_OF_BOUNDS_THROW_WAIT);
        this.outOfBoundsLostTeam = inboundingTeam;
        this.outOfBoundsSide = true; // Sideline inbound
        this.inboundSpot = sidelineSpot.clone();
        this.inboundingTeam = inboundingTeam;
        this.inboundingActive = true;
        this.inboundTouchedByInbounder = false;
        this.inbounderHasReleased = false;
        this.inbounder = null;
        this.inboundStartTime = System.currentTimeMillis(); // Track when inbound started

        // Activate press prevention as soon as inbound starts
        this.pressPrevention = true;
        this.restrictedTeam = (inboundingTeam == Team.HOME) ? Team.AWAY : Team.HOME;
        this.pressPreventionStartTime = System.currentTimeMillis();

        this.setOutOfBoundsImmunity(true);
        System.out.println("Timeout inbound - OOB immunity activated");

        // Spawn ball at sideline
        Bukkit.getScheduler().runTaskLater(Partix.getInstance(), () -> {
            Ball newBall = this.setBall(BallFactory.create(sidelineSpot, BallType.BASKETBALL, this));
            newBall.setStealDelay(0); // ✅ 5 SECOND PROTECTION (100 ticks = 5 seconds)
            
            // Initialize teammate hotkeys for new ball
            if (newBall instanceof Basketball basketball) {
                basketball.initializeAllTeammateHotkeys(this);
            }
        }, 10L);

        // Send inbound message
        this.sendTitle(Component.text("Inbound: " + inboundingTeam.name() + " Ball")
                .style(Style.style(Colour.partix(), TextDecoration.BOLD)));

        // Reset shot clock
        this.resetShotClock();
    }

    public void handleTravelTurnover(Team inboundingTeam) {
        this.removeBalls();
        this.sendMessage(Component.text("TRAVEL - Turnover!").color(Colour.deny()));

        // Set up sideline inbound
        BoundingBox arenaBox = this.getArenaBox();
        Location sidelineSpot = this.getCenter().clone();

        // Place ball at sideline based on which team is inbounding
        if (inboundingTeam == Team.HOME) {
            sidelineSpot.setX(arenaBox.getMinX() - 0.5);
        } else {
            sidelineSpot.setX(arenaBox.getMaxX() + 0.5);
        }
        sidelineSpot.setZ(this.getCenter().getZ());
        sidelineSpot.setY(this.getCenter().getY() + 1.2);

        this.setState(State.OUT_OF_BOUNDS_THROW_WAIT);
        this.outOfBoundsLostTeam = inboundingTeam;
        this.outOfBoundsSide = true;
        this.inboundSpot = sidelineSpot.clone();
        this.inboundingTeam = inboundingTeam;
        this.inboundingActive = true;
        this.inboundTouchedByInbounder = false;
        this.inbounderHasReleased = false;
        this.inbounder = null;

        // Activate press prevention as soon as inbound starts
        this.pressPrevention = true;
        this.restrictedTeam = (inboundingTeam == Team.HOME) ? Team.AWAY : Team.HOME;
        this.pressPreventionStartTime = System.currentTimeMillis();

        this.setOutOfBoundsImmunity(true);

        // Spawn ball at sideline
        Bukkit.getScheduler().runTaskLater(Partix.getInstance(), () -> {
            Ball newBall = this.setBall(BallFactory.create(sidelineSpot, BallType.BASKETBALL, this));
            newBall.setStealDelay(0);
            
            // Initialize teammate hotkeys for new ball
            if (newBall instanceof Basketball basketball) {
                basketball.initializeAllTeammateHotkeys(this);
            }

            // Remove immunity after 7 seconds
            Bukkit.getScheduler().runTaskLater(Partix.getInstance(), () -> {
                this.setOutOfBoundsImmunity(false);
                System.out.println("DEBUG: Travel inbound - OOB immunity removed after 7s");
            }, 140L);
        }, 10L);

        this.resetShotClock();
    }

    public void skipTimeoutToTen() {
        if (this.timeoutTask == null) {
            this.getPlayers().forEach(p -> p.sendMessage(Component.text("No timeout in progress to skip").color(Colour.deny())));
            return;
        }
        this.timeoutSecs = 10;
        this.timeoutBar.setTitle("Timeout: 10s");
        this.timeoutBar.setProgress(0.16666666666666666);
    }

    public void endTimeout(final GoalGame.Team callingTeam) {
        int remaining;
        if (this.getState() != GoalGame.State.REGULATION) {
            return;
        }
        if (this.settings.compType == CompType.RANKED) {
            return;
        }
        if (this.courtLength == 26.0) {
            return;
        }
        int n = remaining = callingTeam == GoalGame.Team.HOME ? this.homeTimeouts : this.awayTimeouts;
        if (remaining <= 0) {
            return;
        }
        if (callingTeam == GoalGame.Team.HOME) {
            --this.homeTimeouts;
        } else {
            --this.awayTimeouts;
        }
        this.setState(GoalGame.State.STOPPAGE);
        this.removeBalls();
        this.sendMessage(Component.text(callingTeam.name() + " called a timeout!").color(Colour.partix()));
        this.updateDisplay();
        this.timeoutBar = Bukkit.createBossBar("Timeout: 60s", BarColor.WHITE, BarStyle.SOLID);
        this.getPlayers().forEach(arg_0 -> this.timeoutBar.addPlayer(arg_0));
        this.timeoutSecs = 60;

        // CLEAR INBOUND PASS TIME
        this.inboundPassTime = 0L;

        if (this.timeoutTask != null) {
            this.timeoutTask.cancel();
            this.timeoutTask = null;
        }
        this.timeoutTask = new BukkitRunnable() {

            public void run() {
                --BasketballGame.this.timeoutSecs;
                BasketballGame.this.timeoutBar.setProgress((double) BasketballGame.this.timeoutSecs / 60.0);
                BasketballGame.this.timeoutBar.setTitle("Timeout: " + BasketballGame.this.timeoutSecs + "s");
                if (BasketballGame.this.timeoutSecs == 10) {
                    List<Player> teamPlayers = callingTeam == GoalGame.Team.HOME ? BasketballGame.this.getHomePlayers() : BasketballGame.this.getAwayPlayers();
                    teamPlayers.forEach(p -> p.sendMessage(Component.text("10 seconds remaining: get ready to inbound!").color(Colour.partix())));
                }
                if (BasketballGame.this.timeoutSecs <= 0) {
                    BasketballGame.this.timeoutBar.removeAll();
                    BasketballGame.this.timeoutTask = null;
                    this.cancel();
                    BasketballGame.this.endTimeout(callingTeam);
                }
            }
        }.runTaskTimer(Partix.getInstance(), 20L, 20L);
        this.getPlayers().stream().filter(p -> p.getInventory().contains(Material.POLISHED_BLACKSTONE_BUTTON)).forEach(p -> p.getInventory().remove(Material.POLISHED_BLACKSTONE_BUTTON));
    }

    private void enforceInboundBounds() {
        double dz;
        if (this.inbounder == null) {
            return;
        }
        Vector flatDir = this.inboundSpot.toVector().subtract(this.inbounder.getLocation().toVector()).setY(0).normalize();
        double dx = this.inbounder.getLocation().getX() - this.inboundSpot.getX();
        double dist = Math.hypot(dx, dz = this.inbounder.getLocation().getZ() - this.inboundSpot.getZ());
        if (dist > 0.5) {
            this.inbounder.setVelocity(flatDir.multiply(0.25));
        }
    }

    public void onInboundPass() {
        this.inboundPassTime = System.currentTimeMillis();

        System.out.println("Inbound pass detected - starting 1 second immunity countdown");

        // CRITICAL FIX: Clear press prevention AND inbound flags IMMEDIATELY on pass
        this.pressPrevention = false;
        this.restrictedTeam = null;
        this.pressPreventionStartTime = 0L;
        
        // CRITICAL: Also clear inboundingActive and inbounder IMMEDIATELY to prevent re-activation
        this.inboundingActive = false;
        this.inbounder = null;
        this.inboundTouchedByInbounder = false;
        this.inbounderHasReleased = false;
        this.inboundStartTime = 0L; // Clear inbound start time
        
        // CRITICAL: Unfreeze the clocks immediately when inbound pass happens
        this.shotClockFrozenForInbound = false;
        this.gameClockFrozenForInbound = false;
        
        System.out.println("✓ Press prevention AND inbound flags cleared IMMEDIATELY on inbound pass");
        System.out.println("✓ Shot clock and game clock unfrozen");

        // Schedule immunity removal after 1 second
        Bukkit.getScheduler().runTaskLater(Partix.getInstance(), () -> {
            this.setOutOfBoundsImmunity(false);
            System.out.println("OOB immunity removed - 1 second after inbound pass");
        }, 20L); // 1 second

        // Clean up remaining inbound state after 1 second
        Bukkit.getScheduler().runTaskLater(Partix.getInstance(), () -> {
            this.inboundPassTime = 0L;

            if (this.inboundBarrierTask != null) {
                this.inboundBarrierTask.cancel();
                this.inboundBarrierTask = null;
            }
            if (this.inboundTimer != null) {
                this.inboundTimer.cancel();
                this.inboundTimer = null;
            }

            this.setState(GoalGame.State.REGULATION);
            this.shotClockStopped = false;

            System.out.println("Inbound cleanup complete - 1 second after inbound pass");
        }, 20L); // 1 second
    }

    public void inboundViolation(GoalGame.Team callingTeam) {
        // Clear press prevention on inbound violation
        this.pressPrevention = false;
        this.pressPreventionStartTime = 0L; // ✅ RESET TIMER
        this.restrictedTeam = null;

        final GoalGame.Team next = callingTeam == GoalGame.Team.HOME ? GoalGame.Team.AWAY : GoalGame.Team.HOME;
        String title = "§c§lInbound Violation!";
        String subtitle = "§fNext: " + next.name();
        for (Player p : this.getPlayers()) {
            p.sendTitle(title, subtitle, 10, 40, 10);
        }
        this.removeBalls();
        this.resetShotClock();
        this.setState(GoalGame.State.STOPPAGE);
        this.cancelInboundSequence();

        // Set immunity BEFORE spawning the ball
        this.setOutOfBoundsImmunity(true);

        new BukkitRunnable() {
            public void run() {
                // Spawn the ball with immunity active
                Location inboundSpot = BasketballGame.this.getCenter().clone();
                inboundSpot.setY(inboundSpot.getY() + 1.2);

                Ball newBall = BasketballGame.this.setBall(
                        BallFactory.create(inboundSpot, BallType.BASKETBALL, BasketballGame.this)
                );
                newBall.setStealDelay(0);

                BasketballGame.this.inboundingActive = true;
                BasketballGame.this.inboundTouchedByInbounder = false;
                BasketballGame.this.inbounderHasReleased = false;
                BasketballGame.this.inbounder = null;

                // Activate press prevention as soon as inbound starts
                Team inboundingTeam = BasketballGame.this.inboundingTeam;
                if (inboundingTeam != null) {
                    BasketballGame.this.pressPrevention = true;
                    BasketballGame.this.restrictedTeam = (inboundingTeam == Team.HOME) ? Team.AWAY : Team.HOME;
                    BasketballGame.this.pressPreventionStartTime = System.currentTimeMillis();
                }

                // Call endTimeout which will set up the proper inbound sequence
                BasketballGame.this.endTimeout(next);
            }
        }.runTaskLater(Partix.getInstance(), 20L);
    }

    public void dropInboundBarrierButKeepClockFrozen() {
        if (this.inboundBarrierTask != null) {
            this.inboundBarrierTask.cancel();
            this.inboundBarrierTask = null;
        }
    }


    private void resumePlay() {
        this.inboundingActive = false;
        this.inbounder = null;
        this.inboundTouchedByInbounder = false;
        this.inbounderHasReleased = false;
        this.inboundPassTime = 0L;
        this.inboundStartTime = 0L;

        // Clear press prevention when play resumes
        this.pressPrevention = false;
        this.pressPreventionStartTime = 0L; // ✅ RESET TIMER
        this.restrictedTeam = null;
        System.out.println("Press prevention cleared - Resume play");

        if (this.inboundBarrierTask != null) {
            this.inboundBarrierTask.cancel();
            this.inboundBarrierTask = null;
        }

        if (this.inboundTimer != null) {
            this.inboundTimer.cancel();
            this.inboundTimer = null;
        }

        this.setState(GoalGame.State.REGULATION);
        this.shotClockStopped = false;
    }

    private void updateMinutesPlayed() {
        // Only track during active gameplay
        if (!this.getState().equals(State.REGULATION) && !this.getState().equals(State.OVERTIME)) {
            return;
        }

        long now = System.currentTimeMillis();

        // Update minutes played for all players currently on court
        List<Player> allPlayers = new ArrayList<>();
        allPlayers.addAll(this.getHomePlayers());
        allPlayers.addAll(this.getAwayPlayers());

        for (Player player : allPlayers) {
            UUID playerId = player.getUniqueId();
            
            // Initialize if first time seeing this player during active game
            if (!playerCourtTime.containsKey(playerId)) {
                playerCourtTime.put(playerId, now);
            } else {
                // Calculate time since last update
                long lastUpdate = playerCourtTime.get(playerId);
                long elapsed = now - lastUpdate;
                
                // Add to player's minutes played
                PlayerStats stats = this.statsManager.getPlayerStats(playerId);
                if (stats != null) {
                    stats.addMinutesPlayed(elapsed);
                }
                
                // Update last update time
                playerCourtTime.put(playerId, now);
            }
        }
    }

    private void updatePossessionTime() {
        Player currentHolder;
        if (!this.getState().equals(State.REGULATION) && !this.getState().equals(State.OVERTIME)) {
            this.currentPossessor = null;
            return;
        }
        long now = System.currentTimeMillis();
        Player player = currentHolder = this.getBall() != null ? this.getBall().getCurrentDamager() : null;
        if (currentHolder != null) {
            UUID holderId = currentHolder.getUniqueId();
            if (!holderId.equals(this.currentPossessor)) {
                if (this.currentPossessor != null) {
                    long duration = now - this.possessionStartTime;
                    PlayerStats stats = this.statsManager.getPlayerStats(this.currentPossessor);
                    stats.addPossessionTime(duration);
                }
                this.currentPossessor = holderId;
                this.possessionStartTime = now;
            }
        } else if (this.currentPossessor != null) {
            long duration = now - this.possessionStartTime;
            PlayerStats stats = this.statsManager.getPlayerStats(this.currentPossessor);
            stats.addPossessionTime(duration);
            this.currentPossessor = null;
        }
    }

    public void onShotAttempt(Player shooter, boolean isThree) {
        if (shooter == null) {
            return;
        }

        // Track rebound machine shot attempts (pregame only)
        if (this.settings.reboundMachineEnabled && this.getState().equals(State.PREGAME)) {
            UUID shooterId = shooter.getUniqueId();
            this.reboundMachineShotsTaken.put(shooterId, this.reboundMachineShotsTaken.getOrDefault(shooterId, 0) + 1);
        }

        GoalGame.State st = this.getState();
        if (!st.equals(State.REGULATION) && !st.equals(State.OVERTIME)) {
            return;
        }
        PlayerStats stats = this.statsManager.getPlayerStats(shooter.getUniqueId());
        stats.incrementFGAttempted();
        if (isThree) {
            stats.increment3FGAttempted();
        }
        this.shotInProgress.put(shooter.getUniqueId(), isThree);
    }

    @Override
    public void enableShotClock() {
        this.shotClockEnabled = true;
    }

    @Override
    public void disableShotClock() {
        this.shotClockEnabled = false;
    }

    private void shotClockViolation() {
        this.shotClockStopped = true;
        this.sendMessage(Component.text("SHOT CLOCK VIOLATION!").color(Colour.deny()));
        this.removeBalls();

        // Determine which team lost possession
        Team violatingTeam = this.lastPossessionTeam;
        Team inboundingTeam = (violatingTeam == Team.HOME) ? Team.AWAY : Team.HOME;

        // ===== SPECIAL HANDLING FOR 1V1 GAMES =====
        if (this.isHalfCourt1v1) {
            System.out.println("1v1 shot clock violation - resetting with turnover");

            // Find which player had the ball (violating player)
            Player violatingPlayer = null;
            Player otherPlayer = null;

            List<Player> allPlayers = new ArrayList<>();
            allPlayers.addAll(this.getHomePlayers());
            allPlayers.addAll(this.getAwayPlayers());

            if (allPlayers.size() == 2) {
                // Determine who had possession
                if (violatingTeam == Team.HOME) {
                    violatingPlayer = this.getHomePlayers().get(0);
                    otherPlayer = this.getAwayPlayers().get(0);
                } else {
                    violatingPlayer = this.getAwayPlayers().get(0);
                    otherPlayer = this.getHomePlayers().get(0);
                }

                System.out.println("Shot clock violation: " + violatingPlayer.getName() + " → Ball to " + otherPlayer.getName());

                // Reset positions with OTHER player getting the ball
                this.resetHalfCourt1v1PositionsWithOffense(otherPlayer);

                // Reset shot clock
                this.resetShotClockTo12();
                this.lastPossessionTeam = null;

                return; // EXIT - Don't run inbound logic for 1v1
            }
        }

        // ===== NORMAL GAME INBOUND LOGIC (2V2, 3V3, ETC) =====
        this.setState(State.OUT_OF_BOUNDS_THROW_WAIT);
        this.outOfBoundsLostTeam = inboundingTeam;

        // Inbound from sideline at center court
        Location sidelineSpot = this.getCenter().clone();

        // Determine which sideline based on court orientation
        BoundingBox arenaBox = this.getArenaBox();

        // Place ball on SIDELINE (X-axis side, not Z-axis baseline)
        sidelineSpot.setX(arenaBox.getMinX() - 0.5); // Place ball on sideline
        sidelineSpot.setZ(this.getCenter().getZ()); // Center along court length
        sidelineSpot.setY(this.getCenter().getY() + 1.2);

        this.outOfBoundsSide = true;
        this.inboundSpot = sidelineSpot.clone();
        this.inboundingTeam = inboundingTeam;
        this.inboundingActive = true;
        this.inboundTouchedByInbounder = false;
        this.inbounderHasReleased = false;
        this.inbounder = null;

        // Activate press prevention as soon as inbound starts
        this.pressPrevention = true;
        this.restrictedTeam = (inboundingTeam == Team.HOME) ? Team.AWAY : Team.HOME;
        this.pressPreventionStartTime = System.currentTimeMillis();

        // Set immunity BEFORE spawning ball
        this.setOutOfBoundsImmunity(true);
        System.out.println("DEBUG: Shot clock violation - OOB immunity activated");

        // Spawn ball at sideline after delay
        Bukkit.getScheduler().runTaskLater(Partix.getInstance(), () -> {
            Ball newBall = this.setBall(BallFactory.create(sidelineSpot, BallType.BASKETBALL, this));
            newBall.setStealDelay(0);
            
            // Initialize teammate hotkeys for new ball
            if (newBall instanceof Basketball basketball) {
                basketball.initializeAllTeammateHotkeys(this);
            }

            // Remove immunity after 7 seconds
            Bukkit.getScheduler().runTaskLater(Partix.getInstance(), () -> {
                this.setOutOfBoundsImmunity(false);
                System.out.println("DEBUG: Shot clock violation - OOB immunity removed after 7 seconds");
            }, 140L);
        }, 30L);

        // Only show OOB message if not immediately after scoring
        if (!this.justScored) {
            sendTitle(Component.text("Out of Bounds: " +
                    (this.inboundingTeam == Team.HOME ? "Home Ball" : "Away Ball")).style(
                    Style.style(Colour.deny(), TextDecoration.BOLD)
            ));
        } else {
            System.out.println("OOB after scoring - suppressing message");
        }

        this.lastPossessionTeam = null;
        this.resetShotClock();
    }

    private void stopPlayDueToShotClockViolation() {
        this.setState(GoalGame.State.STOPPAGE);
        this.removeBalls();
        this.startCountdown(GoalGame.State.FACEOFF, 10);
    }
    
    /**
     * Override setState to manage music based on game state changes.
     * Music pauses during REGULATION/OVERTIME and resumes during other states (STOPPAGE, PREGAME, etc.)
     */
    public void setState(GoalGame.State newState) {
        GoalGame.State oldState = this.getState();
        this.state = newState;
        
        // Update music when transitioning between states
        if (oldState != newState) {
            // Update music for all players in the game
            Partix.getInstance().getLocationMusicManager().updateGameMusic(this);
        }
    }

    private void updateActionBarShotClock() {
        String shotClockDisplay;
        if (!this.shotClockEnabled || this.getState().equals(State.STOPPAGE)) {
            for (Player p : this.getPlayers()) {
                p.sendActionBar(Component.empty());
            }
            return;
        }
        double secondsRemaining = (double) this.shotClockTicks / 20.0;
        String string = shotClockDisplay = secondsRemaining < 5.0 ? String.format("%.1f", secondsRemaining) : String.valueOf((int) Math.ceil(secondsRemaining));
        TextColor clockColor = secondsRemaining > 16.0 ? TextColor.color(65280) : (secondsRemaining > 8.0 ? TextColor.color(0xFFFF00) : TextColor.color(0xFF0000));
        TextComponent timeComponent = Component.text(shotClockDisplay, clockColor);
        if (secondsRemaining <= 3.0) {
            timeComponent = timeComponent.decorate(TextDecoration.BOLD);
        }
        Component display = Component.text("Shot Clock: ", TextColor.color(0xFFFFFF)).decorate(TextDecoration.BOLD).append(timeComponent);
        for (Player p : this.getPlayers()) {
            p.sendActionBar(display);
        }
    }

    public void resetShotClockTo12() {
        if (this.isHalfCourt1v1) {
            this.shotClockTicks = 240;  // 12 seconds for half-court
            this.buzzerPlayed = false;
            Bukkit.getLogger().info("Shot clock reset to 12s (half-court 1v1)");
        } else {
            this.shotClockTicks = 240;
            this.buzzerPlayed = false;
        }
    }


    @Override
    public void onTick() {
        super.onTick();
        this.tickArea();

        // Handle half-court 1v1 position resets
        if (this.isHalfCourt1v1 && this.needsPositionReset) {
            this.handleHalfCourt1v1Reset();
        }


        this.updateShotClock();
        this.updateActionBarShotClock();
        this.updatePossessionTime();
        this.updateMinutesPlayed();

        // ADDED: Check for quarter-end buzzer
        this.checkQuarterEndBuzzer();

        // ADDED: Check for press prevention
        this.checkPressPreventionWithLateGameException();
        
        // ADDED: Auto-clear stale inbound states after 5 seconds (safety timeout)
        if (this.inboundingActive && this.inboundStartTime > 0) {
            long elapsed = System.currentTimeMillis() - this.inboundStartTime;
            if (elapsed > INBOUND_TIMEOUT) {
                System.out.println("⚠️ SAFETY: Auto-clearing stale inbound state after 5 seconds");
                this.inboundingActive = false;
                this.inbounder = null;
                this.inboundTouchedByInbounder = false;
                this.inbounderHasReleased = false;
                this.inboundStartTime = 0L;
                this.inboundPassTime = 0L;
            }
        }
    }
    private void pregameGoalDetection() {
    }

    private void tickArea() {
        for (Player player : this.getPlayers()) {
            Vector vector = player.getLocation().toVector();
            final double floorY = this.getArenaBox().getMinY() + 2.5; // arenas seem to be offset by 2.5
            if (player.getLocation().getY() > floorY) {
                return;
            }

            if (this.homeJump.contains(vector) || this.awayJump.contains(vector)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 60, 1));
            }else{
                player.removePotionEffect(PotionEffectType.JUMP_BOOST);
            }
        }
    }
    private void reboundDetection() {
        // Get all balls near the court center (large radius to cover whole court)
        List<Ball> nearbyBalls = BallFactory.getNearby(this.getCenter(), 100.0);

        for (Ball ball : nearbyBalls) {
            if (!(ball instanceof Basketball basketball)) continue;

            Location ballLoc = basketball.getLocation();
            Vector ballVec = ballLoc.toVector();

            // Check if ball is in either hoop with LARGER expansion for better detection
            boolean inHomeNet = this.getHomeNet().clone().expand(0.3).contains(ballVec);
            boolean inAwayNet = this.getAwayNet().clone().expand(0.3).contains(ballVec);

            // MADE SHOT - Ball is in hoop and moving downward (or just scored)
            if ((inHomeNet || inAwayNet) && basketball.getVelocity().getY() < 0.15) {
                Player shooter = basketball.getLastDamager();

                if (shooter != null && shooter.isOnline()) {
                    // Check if we already counted this shot to prevent double-counting
                    UUID shooterId = shooter.getUniqueId();

                    // Prevent double-counting by checking if makes + misses already equals attempts
                    int currentAttempts = reboundMachineShotsTaken.getOrDefault(shooterId, 0);
                    int currentMakes = reboundMachineShotsMade.getOrDefault(shooterId, 0);
                    int currentMisses = reboundMachineShotsMissed.getOrDefault(shooterId, 0);

                    // Only count if this is a new shot result
                    if (currentMakes + currentMisses < currentAttempts) {
                        // Track the shot as made
                        reboundMachineShotsMade.put(shooterId, currentMakes + 1);

                        // Calculate shooting stats
                        int shotsTaken = currentAttempts;
                        int shotsMade = currentMakes + 1;
                        int shotsMissed = currentMisses;

                        double percentage = ((double) shotsMade / shotsTaken) * 100;
                        shooter.sendMessage(Component.text("✓ MAKE | Shots: " + shotsTaken + " | Makes: " + shotsMade + " | Misses: " + shotsMissed + " | FG%: " + String.format("%.1f", percentage) + "%").color(Colour.allow()));

                        // Determine which hoop the ball went through
                        Location hoopLocation = inHomeNet ? this.getHomeNet().getCenter().toLocation(ballLoc.getWorld()) : this.getAwayNet().getCenter().toLocation(ballLoc.getWorld());

                        // Position UNDER the rim (below the hoop)
                        Location underRim = hoopLocation.clone().subtract(0, 2.0, 0);

                        // Remove the ball temporarily
                        basketball.remove();

                        // Schedule ball return after 0.5 seconds (10 ticks)
                        Bukkit.getScheduler().runTaskLater(Partix.getInstance(), () -> {
                            if (shooter.isOnline()) {
                                // Create new ball under the rim
                                Ball newBall = BallFactory.create(underRim, BallType.BASKETBALL, this);

                                // Calculate direction from under rim to player
                                Location playerLoc = shooter.getLocation();
                                Vector direction = playerLoc.toVector().subtract(underRim.toVector()).normalize();

                                // Shoot ball back to player with chest pass velocity
                                newBall.setVelocity(direction.getX() * 0.75, 0.15, direction.getZ() * 0.75);

                                // Play sound for feedback
                                shooter.playSound(shooter.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 0.5f, 1.2f);
                            }
                        }, 10L);
                    }
                }
            }

            // MISSED SHOT - Ball hits ground after being shot
            else if (basketball.getVelocity().getY() < -0.3 && ballLoc.getY() < this.getCenter().getY() + 1.0) {
                Player shooter = basketball.getLastDamager();

                if (shooter != null && shooter.isOnline()) {
                    UUID shooterId = shooter.getUniqueId();

                    // Check if this miss has already been counted
                    int currentAttempts = reboundMachineShotsTaken.getOrDefault(shooterId, 0);
                    int currentMakes = reboundMachineShotsMade.getOrDefault(shooterId, 0);
                    int currentMisses = reboundMachineShotsMissed.getOrDefault(shooterId, 0);

                    // Only count if this is a new miss
                    if (currentMakes + currentMisses < currentAttempts) {
                        // Track the miss
                        reboundMachineShotsMissed.put(shooterId, currentMisses + 1);

                        // Calculate shooting stats
                        int shotsTaken = currentAttempts;
                        int shotsMade = currentMakes;
                        int shotsMissed = currentMisses + 1;

                        double percentage = ((double) shotsMade / shotsTaken) * 100;
                        shooter.sendMessage(Component.text("✗ MISS | Shots: " + shotsTaken + " | Makes: " + shotsMade + " | Misses: " + shotsMissed + " | FG%: " + String.format("%.1f", percentage) + "%").color(Colour.deny()));

                        // Remove the missed ball
                        basketball.remove();

                        // Return ball after 0.5 seconds
                        Location underRim;
                        if (ballLoc.getX() < this.getCenter().getX()) {
                            underRim = this.getHomeNet().getCenter().toLocation(ballLoc.getWorld()).subtract(0, 2.0, 0);
                        } else {
                            underRim = this.getAwayNet().getCenter().toLocation(ballLoc.getWorld()).subtract(0, 2.0, 0);
                        }

                        Bukkit.getScheduler().runTaskLater(Partix.getInstance(), () -> {
                            if (shooter.isOnline()) {
                                Ball newBall = BallFactory.create(underRim, BallType.BASKETBALL, this);
                                Location playerLoc = shooter.getLocation();
                                Vector direction = playerLoc.toVector().subtract(underRim.toVector()).normalize();
                                newBall.setVelocity(direction.getX() * 0.75, 0.15, direction.getZ() * 0.75);
                                shooter.playSound(shooter.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.3f, 0.8f);
                            }
                        }, 10L);
                    }
                }
            }
        }
    }
    private BoundingBox getJumpBox(Location area) {
        return new BoundingBox(
                area.x() - 8, area.y() - 20, area.z() + 5,
                area.x() + 8, area.y() + 20, area.z() - 5
        );
    }

    private void updateShotClock() {
        // CRITICAL FIX: Only run shot clock during REGULATION or OVERTIME
        if (!this.getState().equals(State.REGULATION) && !this.getState().equals(State.OVERTIME)) {
            return; // Exit early - don't run shot clock logic at all
        }

        // Check if shot clock is disabled, stopped, or frozen
        if (!this.shotClockEnabled) {
            System.out.println("DEBUG: Shot clock not running - shotClockEnabled = false");
            return;
        }
        
        if (this.shotClockStopped) {
            System.out.println("DEBUG: Shot clock not running - shotClockStopped = true");
            return;
        }
        
        if (this.shotClockFrozenForInbound) {
            System.out.println("DEBUG: Shot clock not running - shotClockFrozenForInbound = true");
            return;
        }

        // Don't run during stoppage states
        if (this.getState().equals(State.STOPPAGE) ||
                this.getState().equals(State.OUT_OF_BOUNDS_THROW) ||
                this.getState().equals(State.OUT_OF_BOUNDS_THROW_WAIT)) {
            return;
        }

        // FIXED: 1v1 games have independent shot clocks
        if (!this.isHalfCourt1v1 && !this.isSingleHoopMode) {
            int gameTimeLeft = this.getTimeTicks();
            if (this.shotClockTicks > gameTimeLeft) {
                return;  // Only for regular games
            }
        }

        Player possessor = this.getBall() != null ? this.getBall().getCurrentDamager() : null;
        
        // FIXED: Shot clock should run when ball is loose IF we know who had possession last
        // Only stop if both possessor is null AND we never established possession
        if (possessor == null && this.lastPossessionTeam == null) {
            System.out.println("DEBUG: Shot clock not running - no possessor and no lastPossessionTeam");
            return;
        }

        GoalGame.Team currentTeam = possessor != null ? this.getTeamOf(possessor) : this.lastPossessionTeam;

        if (this.lastPossessionTeam != null && currentTeam != this.lastPossessionTeam) {
            this.shotClockTicks = 480;
            this.shotAttemptDetected = false;
            this.shotAttemptTeam = null;
            this.buzzerPlayed = false;
        } else if (this.shotAttemptDetected && this.shotAttemptTeam == currentTeam && this.shotClockTicks < 240) {
            this.shotClockTicks = 240;
            this.shotAttemptDetected = false;
            this.shotAttemptTeam = null;
            this.buzzerPlayed = false;
        }

        this.lastPossessionTeam = currentTeam;

        if (this.shotClockTicks > 0) {
            --this.shotClockTicks;
            this.buzzerPlayed = false;
            double secondsRemaining = (double) this.shotClockTicks / 20.0;

            // Only play tick sounds during regulation/overtime
            if (secondsRemaining <= 5.0 && this.shotClockTicks % 20 == 0) {
                for (Player p : this.getPlayers()) {
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.MASTER, 1.0f, 1.0f);
                }
            }
        } else {
            this.shotClockTicks = 0;

            // CRITICAL FIX: Only play buzzer if in REGULATION or OVERTIME
//            if (!this.buzzerPlayed &&
//                    (this.getState().equals(State.REGULATION) || this.getState().equals(State.OVERTIME))) {
//                for (Player p : this.getPlayers()) {
//                    p.playSound(p.getLocation(), "shotclockbuzzer", SoundCategory.MASTER, 1.0f, 1.0f);
 //               }
 //               this.buzzerPlayed = true;
  //          }

            if (this.getBall() != null) {
                double threshold;
                double ballY = this.getBall().getLocation().getY();
                if (ballY <= (threshold = this.getCenter().getY() + 2.0)) {
                    this.shotClockViolation();
                }
            } else {
                this.shotClockViolation();
            }
        }
    }

    private void checkQuarterEndBuzzer() {
        // ADDED: Skip quarter buzzer for 1v1 games
        if (this.isHalfCourt1v1 || this.isSingleHoopMode) {
            return;
        }

        // Only check if not in stoppage and game clock is running
        if (this.getState().equals(State.STOPPAGE) || this.getState().equals(State.FINAL) ||
                this.getState().equals(State.PREGAME) || this.getState().equals(State.FACEOFF)) {
            return;
        }

        // Check if game time (timeTicks) has hit zero
        if (this.getTimeTicks() <= 0 && !this.quarterBuzzerPlayed) {
            this.quarterBuzzerPlayed = true;

            // Play buzzer sound to ALL players at their location
 //           for (Player p : this.getPlayers()) {
 //               p.playSound(p.getLocation(), "shotclockbuzzer", SoundCategory.MASTER, 1.0f, 1.0f);
  //          }

            // Optional: Send message to all players
  //          this.sendMessage(Component.text("QUARTER BUZZER!").color(Colour.partix()).decorate(TextDecoration.BOLD));

  //          System.out.println("Quarter-end buzzer played!");
        }
    }

    @Override
    public void setPregame() {
        World world = this.getCenter().getWorld();

        // ALWAYS create balls at home spawn
        Ball ball1 = BallFactory.create(this.getHomeSpawn().clone().add(0.0, 0.0, -3.0), this.getBallType(), this);
        Ball ball2 = BallFactory.create(this.getHomeSpawn().clone().add(0.0, 0.0, -1.5), this.getBallType(), this);
        Ball ball3 = BallFactory.create(this.getHomeSpawn().clone().add(0.0, 0.0, 1.5), this.getBallType(), this);
        Ball ball4 = BallFactory.create(this.getHomeSpawn().clone().add(0.0, 0.0, 3.0), this.getBallType(), this);
        
        // Initialize hotkeys for pregame balls
        if (ball1 instanceof Basketball) ((Basketball) ball1).initializeAllTeammateHotkeys(this);
        if (ball2 instanceof Basketball) ((Basketball) ball2).initializeAllTeammateHotkeys(this);
        if (ball3 instanceof Basketball) ((Basketball) ball3).initializeAllTeammateHotkeys(this);
        if (ball4 instanceof Basketball) ((Basketball) ball4).initializeAllTeammateHotkeys(this);

        // For 1v1: ONLY create balls at home spawn, not away spawn
        if (!this.isSingleHoopMode) {
            Ball ball5 = BallFactory.create(this.getAwaySpawn().clone().add(0.0, 0.0, -3.0), this.getBallType(), this);
            Ball ball6 = BallFactory.create(this.getAwaySpawn().clone().add(0.0, 0.0, -1.5), this.getBallType(), this);
            Ball ball7 = BallFactory.create(this.getAwaySpawn().clone().add(0.0, 0.0, 1.5), this.getBallType(), this);
            Ball ball8 = BallFactory.create(this.getAwaySpawn().clone().add(0.0, 0.0, 3.0), this.getBallType(), this);
            
            // Initialize hotkeys for away balls
            if (ball5 instanceof Basketball) ((Basketball) ball5).initializeAllTeammateHotkeys(this);
            if (ball6 instanceof Basketball) ((Basketball) ball6).initializeAllTeammateHotkeys(this);
            if (ball7 instanceof Basketball) ((Basketball) ball7).initializeAllTeammateHotkeys(this);
            if (ball8 instanceof Basketball) ((Basketball) ball8).initializeAllTeammateHotkeys(this);
        }

        // Always clear home net
        Block h = this.getHomeNet().clone().getCenter().toLocation(world).getBlock();
        h.getLocation().clone().getBlock().setType(Material.AIR);
        h.getLocation().clone().subtract(0.0, 1.0, 0.0).getBlock().setType(Material.AIR);

        // For 1v1: Don't touch away net (it never spawns)
        if (!this.isSingleHoopMode) {
            Block a = this.getAwayNet().clone().getCenter().toLocation(world).getBlock();
            a.getLocation().clone().getBlock().setType(Material.AIR);
            a.getLocation().clone().subtract(0.0, 1.0, 0.0).getBlock().setType(Material.AIR);
        }

        startPregameTask();
    }

    private void startPregameTask() {
        // Cancel any existing task
        if (pregameTask != null) {
            pregameTask.cancel();
        }

        // Start new repeating task for pregame rebounds
        pregameTask = Bukkit.getScheduler().runTaskTimer(Partix.getInstance(), () -> {
            if (getState().equals(State.PREGAME) && settings.reboundMachineEnabled) {
                reboundDetection();
            }
        }, 0L, 1L); // Run every tick
    }

    @Override
    public void setFaceoff() {
        World world = this.getCenter().getWorld();
        this.removeBalls();
        Location h = this.getHomeNet().clone().getCenter().toLocation(world);
        h.getBlock().setType(Material.AIR);

        // For 1v1: Don't spawn away net at all
        if (!this.isSingleHoopMode) {
            Location a = this.getAwayNet().clone().getCenter().toLocation(world);
            a.getBlock().setType(Material.AIR);
        }

        // Cancel pregame task when leaving pregame
        if (pregameTask != null) {
            pregameTask.cancel();
            pregameTask = null;
        }
    }

    @Override
    public void dropBall() {
        Location spawn = this.getCenter().add(0.0, 1.5 + Math.random() / 1.5, 0.0);
        Ball ball = this.setBall(BallFactory.create(spawn, BallType.BASKETBALL, this));
        ball.setVelocity(0.0, 0.1 + Math.random() / 3.0, new Random().nextBoolean() ? Math.max(0.05 + (0.05 + Math.random()) / 25.0, 0.05) / 3.0 : Math.min(-0.05 + (-0.5 - Math.random()) / 25.0, -0.05) / 3.0);
        
        // Initialize teammate hotkeys at game start (jump ball)
        if (ball instanceof Basketball basketball) {
            basketball.initializeAllTeammateHotkeys(this);
        }
    }

    @Override
    public boolean periodIsComplete(int ticksRemaining) {
        if (this.getBall() != null && this.getBall().getCurrentDamager() == null && this.getBall().getLocation().getY() > this.getCenter().getY() + 2.0) {
            this.addTime(1);
            return false;
        }

        // Only reset shot clock for timed games
        if (this.settings.winType.timed) {
            this.resetShotClock();
            // ADDED: Reset quarter buzzer flag when new period starts
            this.quarterBuzzerPlayed = false;
        }

        return true;
    }
    public void recordRebound(Player player) {
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        // Check if player is on cooldown
        if (lastReboundTime.containsKey(playerId)) {
            long timeSinceLastRebound = currentTime - lastReboundTime.get(playerId);
            if (timeSinceLastRebound < REBOUND_COOLDOWN_MS) {
                // Player is still on cooldown, don't record rebound
                return;
            }
        }

        // Record the rebound
        PlayerStats stats = this.statsManager.getPlayerStats(playerId);
        stats.incrementRebounds();

        // Update the last rebound time
        lastReboundTime.put(playerId, currentTime);
    }
    @Override
    public void gameOver(GoalGame.Team winner) {
        this.sendTitle(Component.text("The ").color(Colour.partix()).append(winner.equals(Team.HOME) ? this.home.name : this.away.name).append(Component.text(" Win!").color(Colour.bold())));

        if (this.settings.compType.equals(CompType.RANKED) && (this.getHomePlayers().size() >= 1 || this.getAwayPlayers().size() > 1)) {
                if (winner.equals(Team.HOME)) {
                // HOME TEAM WINS
                this.getHomePlayers().forEach(uuid -> {
                    BasketballDb.add(uuid.getUniqueId(), BasketballDb.Stat.WINS, 1);
                    SeasonDb.get(uuid.getUniqueId(), SeasonDb.Stat.WINS).thenAccept(wins -> SeasonDb.set(uuid.getUniqueId(), SeasonDb.Stat.WINS, wins + 1));
                    SeasonDb.get(uuid.getUniqueId(), SeasonDb.Stat.POINTS).thenAccept(points -> SeasonDb.set(uuid.getUniqueId(), SeasonDb.Stat.POINTS, points + 2));

                    // Career Record
                    PlayerDb.get(uuid.getUniqueId(), PlayerDb.Stat.CAREER_WINS).thenAccept(careerWins ->
                            PlayerDb.set(uuid.getUniqueId(), PlayerDb.Stat.CAREER_WINS, careerWins + 1)
                    );
                    PlayerDb.get(uuid.getUniqueId(), PlayerDb.Stat.CAREER_GAMES_PLAYED).thenAccept(games ->
                            PlayerDb.set(uuid.getUniqueId(), PlayerDb.Stat.CAREER_GAMES_PLAYED, games + 1)
                    );

                    // Season 1 Record (WINS)
                    PlayerDb.get(uuid.getUniqueId(), PlayerDb.Stat.SEASON_1_WINS).thenAccept(s1Wins ->
                            PlayerDb.set(uuid.getUniqueId(), PlayerDb.Stat.SEASON_1_WINS, s1Wins + 1)
                    );
                    PlayerDb.get(uuid.getUniqueId(), PlayerDb.Stat.SEASON_1_GAMES_PLAYED).thenAccept(s1Games ->
                            PlayerDb.set(uuid.getUniqueId(), PlayerDb.Stat.SEASON_1_GAMES_PLAYED, s1Games + 1)
                    );

                    // Rec Stats (WINS)
                    if (this.isRecGame) {
                        PlayerDb.get(uuid.getUniqueId(), PlayerDb.Stat.REC_WINS).thenAccept(recWins ->
                                PlayerDb.set(uuid.getUniqueId(), PlayerDb.Stat.REC_WINS, recWins + 1)
                        );
                        PlayerDb.get(uuid.getUniqueId(), PlayerDb.Stat.REC_GAMES).thenAccept(recGames ->
                                PlayerDb.set(uuid.getUniqueId(), PlayerDb.Stat.REC_GAMES, recGames + 1)
                        );
                    }

                    // Career Stats from this game
                    PlayerStats stats = this.statsManager.getPlayerStats(uuid.getUniqueId());
                    if (stats != null) {
                        // Career Stats
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.CAREER_POINTS, stats.getPoints());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.CAREER_ASSISTS, stats.getAssists());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.CAREER_REBOUNDS, stats.getRebounds());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.CAREER_STEALS, stats.getSteals());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.CAREER_BLOCKS, stats.getBlocks());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.CAREER_TURNOVERS, stats.getTurnovers());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.CAREER_FG_MADE, stats.getFGMade());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.CAREER_FG_ATTEMPTED, stats.getFGAttempted());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.CAREER_3FG_MADE, stats.get3FGMade());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.CAREER_3FG_ATTEMPTED, stats.get3FGAttempted());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.CAREER_THREES, stats.getThrees());

                        // Season 1 Stats
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.SEASON_1_POINTS, stats.getPoints());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.SEASON_1_ASSISTS, stats.getAssists());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.SEASON_1_REBOUNDS, stats.getRebounds());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.SEASON_1_STEALS, stats.getSteals());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.SEASON_1_BLOCKS, stats.getBlocks());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.SEASON_1_TURNOVERS, stats.getTurnovers());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.SEASON_1_FG_MADE, stats.getFGMade());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.SEASON_1_FG_ATTEMPTED, stats.getFGAttempted());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.SEASON_1_3FG_MADE, stats.get3FGMade());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.SEASON_1_3FG_ATTEMPTED, stats.get3FGAttempted());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.SEASON_1_THREES, stats.getThrees());
                    }

                    // Performance-based coin rewards
                    int coinReward = calculateCoinReward(stats, true, this.isRecGame);
                    AthleteManager.get(uuid.getUniqueId()).giveCoins(coinReward, true);
                    
                    // Award Season Pass EXP for ranked games
                    if (this.settings.compType == CompType.RANKED && stats != null) {
                        me.x_tias.partix.plugin.seasonpass.SeasonPassManager.awardGameExp(
                            uuid.getUniqueId(),
                            stats.getPoints(),
                            stats.getAssists(),
                            stats.getRebounds(),
                            stats.getSteals(),
                            stats.getBlocks(),
                            true // winner
                        );
                    }
                });

                // AWAY TEAM LOSES
                this.getAwayPlayers().forEach(uuid -> {
                    BasketballDb.add(uuid.getUniqueId(), BasketballDb.Stat.LOSSES, 1);
                    SeasonDb.get(uuid.getUniqueId(), SeasonDb.Stat.LOSSES).thenAccept(losses -> SeasonDb.set(uuid.getUniqueId(), SeasonDb.Stat.LOSSES, losses + 1));
                    SeasonDb.get(uuid.getUniqueId(), SeasonDb.Stat.POINTS).thenAccept(points -> {
                        int newPoints = Math.max(0, points - 1);
                        SeasonDb.set(uuid.getUniqueId(), SeasonDb.Stat.POINTS, newPoints);
                    });

                    // Career Record
                    PlayerDb.get(uuid.getUniqueId(), PlayerDb.Stat.CAREER_LOSSES).thenAccept(careerLosses ->
                            PlayerDb.set(uuid.getUniqueId(), PlayerDb.Stat.CAREER_LOSSES, careerLosses + 1)
                    );
                    PlayerDb.get(uuid.getUniqueId(), PlayerDb.Stat.CAREER_GAMES_PLAYED).thenAccept(games ->
                            PlayerDb.set(uuid.getUniqueId(), PlayerDb.Stat.CAREER_GAMES_PLAYED, games + 1)
                    );

                    // Season 1 Record (LOSSES)
                    PlayerDb.get(uuid.getUniqueId(), PlayerDb.Stat.SEASON_1_LOSSES).thenAccept(s1Losses ->
                            PlayerDb.set(uuid.getUniqueId(), PlayerDb.Stat.SEASON_1_LOSSES, s1Losses + 1)
                    );
                    PlayerDb.get(uuid.getUniqueId(), PlayerDb.Stat.SEASON_1_GAMES_PLAYED).thenAccept(s1Games ->
                            PlayerDb.set(uuid.getUniqueId(), PlayerDb.Stat.SEASON_1_GAMES_PLAYED, s1Games + 1)
                    );

                    // Rec Stats (LOSSES)
                    if (this.isRecGame) {
                        PlayerDb.get(uuid.getUniqueId(), PlayerDb.Stat.REC_LOSSES).thenAccept(recLosses ->
                                PlayerDb.set(uuid.getUniqueId(), PlayerDb.Stat.REC_LOSSES, recLosses + 1)
                        );
                        PlayerDb.get(uuid.getUniqueId(), PlayerDb.Stat.REC_GAMES).thenAccept(recGames ->
                                PlayerDb.set(uuid.getUniqueId(), PlayerDb.Stat.REC_GAMES, recGames + 1)
                        );
                    }

                    // Career Stats from this game
                    PlayerStats stats = this.statsManager.getPlayerStats(uuid.getUniqueId());
                    if (stats != null) {
                        // Career Stats
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.CAREER_POINTS, stats.getPoints());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.CAREER_ASSISTS, stats.getAssists());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.CAREER_REBOUNDS, stats.getRebounds());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.CAREER_STEALS, stats.getSteals());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.CAREER_BLOCKS, stats.getBlocks());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.CAREER_TURNOVERS, stats.getTurnovers());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.CAREER_FG_MADE, stats.getFGMade());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.CAREER_FG_ATTEMPTED, stats.getFGAttempted());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.CAREER_3FG_MADE, stats.get3FGMade());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.CAREER_3FG_ATTEMPTED, stats.get3FGAttempted());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.CAREER_THREES, stats.getThrees());

                        // Season 1 Stats
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.SEASON_1_POINTS, stats.getPoints());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.SEASON_1_ASSISTS, stats.getAssists());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.SEASON_1_REBOUNDS, stats.getRebounds());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.SEASON_1_STEALS, stats.getSteals());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.SEASON_1_BLOCKS, stats.getBlocks());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.SEASON_1_TURNOVERS, stats.getTurnovers());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.SEASON_1_FG_MADE, stats.getFGMade());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.SEASON_1_FG_ATTEMPTED, stats.getFGAttempted());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.SEASON_1_3FG_MADE, stats.get3FGMade());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.SEASON_1_3FG_ATTEMPTED, stats.get3FGAttempted());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.SEASON_1_THREES, stats.getThrees());
                    }

                    // Performance-based coin rewards
                    int coinReward = calculateCoinReward(stats, false, this.isRecGame);
                    AthleteManager.get(uuid.getUniqueId()).giveCoins(coinReward, true);
                    
                    // Award Season Pass EXP for ranked games (losers get exp too!)
                    if (this.settings.compType == CompType.RANKED && stats != null) {
                        me.x_tias.partix.plugin.seasonpass.SeasonPassManager.awardGameExp(
                            uuid.getUniqueId(),
                            stats.getPoints(),
                            stats.getAssists(),
                            stats.getRebounds(),
                            stats.getSteals(),
                            stats.getBlocks(),
                            false // loser
                        );
                    }
                });
            } else {
                // AWAY TEAM WINS
                this.getAwayPlayers().forEach(uuid -> {
                    BasketballDb.add(uuid.getUniqueId(), BasketballDb.Stat.WINS, 1);
                    SeasonDb.get(uuid.getUniqueId(), SeasonDb.Stat.WINS).thenAccept(wins -> SeasonDb.set(uuid.getUniqueId(), SeasonDb.Stat.WINS, wins + 1));
                    SeasonDb.get(uuid.getUniqueId(), SeasonDb.Stat.POINTS).thenAccept(points -> SeasonDb.set(uuid.getUniqueId(), SeasonDb.Stat.POINTS, points + 2));

                    // Career Record
                    PlayerDb.get(uuid.getUniqueId(), PlayerDb.Stat.CAREER_WINS).thenAccept(careerWins ->
                            PlayerDb.set(uuid.getUniqueId(), PlayerDb.Stat.CAREER_WINS, careerWins + 1)
                    );
                    PlayerDb.get(uuid.getUniqueId(), PlayerDb.Stat.CAREER_GAMES_PLAYED).thenAccept(games ->
                            PlayerDb.set(uuid.getUniqueId(), PlayerDb.Stat.CAREER_GAMES_PLAYED, games + 1)
                    );

                    // Season 1 Record (WINS)
                    PlayerDb.get(uuid.getUniqueId(), PlayerDb.Stat.SEASON_1_WINS).thenAccept(s1Wins ->
                            PlayerDb.set(uuid.getUniqueId(), PlayerDb.Stat.SEASON_1_WINS, s1Wins + 1)
                    );
                    PlayerDb.get(uuid.getUniqueId(), PlayerDb.Stat.SEASON_1_GAMES_PLAYED).thenAccept(s1Games ->
                            PlayerDb.set(uuid.getUniqueId(), PlayerDb.Stat.SEASON_1_GAMES_PLAYED, s1Games + 1)
                    );

                    // Rec Stats (WINS)
                    if (this.isRecGame) {
                        PlayerDb.get(uuid.getUniqueId(), PlayerDb.Stat.REC_WINS).thenAccept(recWins ->
                                PlayerDb.set(uuid.getUniqueId(), PlayerDb.Stat.REC_WINS, recWins + 1)
                        );
                        PlayerDb.get(uuid.getUniqueId(), PlayerDb.Stat.REC_GAMES).thenAccept(recGames ->
                                PlayerDb.set(uuid.getUniqueId(), PlayerDb.Stat.REC_GAMES, recGames + 1)
                        );
                    }

                    // Career Stats from this game
                    PlayerStats stats = this.statsManager.getPlayerStats(uuid.getUniqueId());
                    if (stats != null) {
                        // Career Stats
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.CAREER_POINTS, stats.getPoints());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.CAREER_ASSISTS, stats.getAssists());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.CAREER_REBOUNDS, stats.getRebounds());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.CAREER_STEALS, stats.getSteals());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.CAREER_BLOCKS, stats.getBlocks());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.CAREER_TURNOVERS, stats.getTurnovers());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.CAREER_FG_MADE, stats.getFGMade());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.CAREER_FG_ATTEMPTED, stats.getFGAttempted());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.CAREER_3FG_MADE, stats.get3FGMade());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.CAREER_3FG_ATTEMPTED, stats.get3FGAttempted());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.CAREER_THREES, stats.getThrees());

                        // Season 1 Stats
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.SEASON_1_POINTS, stats.getPoints());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.SEASON_1_ASSISTS, stats.getAssists());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.SEASON_1_REBOUNDS, stats.getRebounds());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.SEASON_1_STEALS, stats.getSteals());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.SEASON_1_BLOCKS, stats.getBlocks());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.SEASON_1_TURNOVERS, stats.getTurnovers());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.SEASON_1_FG_MADE, stats.getFGMade());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.SEASON_1_FG_ATTEMPTED, stats.getFGAttempted());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.SEASON_1_3FG_MADE, stats.get3FGMade());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.SEASON_1_3FG_ATTEMPTED, stats.get3FGAttempted());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.SEASON_1_THREES, stats.getThrees());
                    }

                    // Performance-based coin rewards
                    int coinReward = calculateCoinReward(stats, true, this.isRecGame);
                    AthleteManager.get(uuid.getUniqueId()).giveCoins(coinReward, true);
                    
                    // Award Season Pass EXP for ranked games
                    if (this.settings.compType == CompType.RANKED && stats != null) {
                        me.x_tias.partix.plugin.seasonpass.SeasonPassManager.awardGameExp(
                            uuid.getUniqueId(),
                            stats.getPoints(),
                            stats.getAssists(),
                            stats.getRebounds(),
                            stats.getSteals(),
                            stats.getBlocks(),
                            true // winner
                        );
                    }
                });

                // HOME TEAM LOSES
                this.getHomePlayers().forEach(uuid -> {
                    BasketballDb.add(uuid.getUniqueId(), BasketballDb.Stat.LOSSES, 1);
                    SeasonDb.get(uuid.getUniqueId(), SeasonDb.Stat.LOSSES).thenAccept(losses -> SeasonDb.set(uuid.getUniqueId(), SeasonDb.Stat.LOSSES, losses + 1));
                    SeasonDb.get(uuid.getUniqueId(), SeasonDb.Stat.POINTS).thenAccept(points -> {
                        int newPoints = Math.max(0, points - 1);
                        SeasonDb.set(uuid.getUniqueId(), SeasonDb.Stat.POINTS, newPoints);
                    });

                    // Career Record
                    PlayerDb.get(uuid.getUniqueId(), PlayerDb.Stat.CAREER_LOSSES).thenAccept(careerLosses ->
                            PlayerDb.set(uuid.getUniqueId(), PlayerDb.Stat.CAREER_LOSSES, careerLosses + 1)
                    );
                    PlayerDb.get(uuid.getUniqueId(), PlayerDb.Stat.CAREER_GAMES_PLAYED).thenAccept(games ->
                            PlayerDb.set(uuid.getUniqueId(), PlayerDb.Stat.CAREER_GAMES_PLAYED, games + 1)
                    );

                    // Season 1 Record (LOSSES)
                    PlayerDb.get(uuid.getUniqueId(), PlayerDb.Stat.SEASON_1_LOSSES).thenAccept(s1Losses ->
                            PlayerDb.set(uuid.getUniqueId(), PlayerDb.Stat.SEASON_1_LOSSES, s1Losses + 1)
                    );
                    PlayerDb.get(uuid.getUniqueId(), PlayerDb.Stat.SEASON_1_GAMES_PLAYED).thenAccept(s1Games ->
                            PlayerDb.set(uuid.getUniqueId(), PlayerDb.Stat.SEASON_1_GAMES_PLAYED, s1Games + 1)
                    );

                    // Rec Stats (LOSSES)
                    if (this.isRecGame) {
                        PlayerDb.get(uuid.getUniqueId(), PlayerDb.Stat.REC_LOSSES).thenAccept(recLosses ->
                                PlayerDb.set(uuid.getUniqueId(), PlayerDb.Stat.REC_LOSSES, recLosses + 1)
                        );
                        PlayerDb.get(uuid.getUniqueId(), PlayerDb.Stat.REC_GAMES).thenAccept(recGames ->
                                PlayerDb.set(uuid.getUniqueId(), PlayerDb.Stat.REC_GAMES, recGames + 1)
                        );
                    }

                    // Career Stats from this game
                    PlayerStats stats = this.statsManager.getPlayerStats(uuid.getUniqueId());
                    if (stats != null) {
                        // Career Stats
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.CAREER_POINTS, stats.getPoints());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.CAREER_ASSISTS, stats.getAssists());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.CAREER_REBOUNDS, stats.getRebounds());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.CAREER_STEALS, stats.getSteals());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.CAREER_BLOCKS, stats.getBlocks());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.CAREER_TURNOVERS, stats.getTurnovers());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.CAREER_FG_MADE, stats.getFGMade());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.CAREER_FG_ATTEMPTED, stats.getFGAttempted());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.CAREER_3FG_MADE, stats.get3FGMade());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.CAREER_3FG_ATTEMPTED, stats.get3FGAttempted());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.CAREER_THREES, stats.getThrees());

                        // Season 1 Stats
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.SEASON_1_POINTS, stats.getPoints());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.SEASON_1_ASSISTS, stats.getAssists());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.SEASON_1_REBOUNDS, stats.getRebounds());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.SEASON_1_STEALS, stats.getSteals());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.SEASON_1_BLOCKS, stats.getBlocks());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.SEASON_1_TURNOVERS, stats.getTurnovers());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.SEASON_1_FG_MADE, stats.getFGMade());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.SEASON_1_FG_ATTEMPTED, stats.getFGAttempted());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.SEASON_1_3FG_MADE, stats.get3FGMade());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.SEASON_1_3FG_ATTEMPTED, stats.get3FGAttempted());
                        PlayerDb.add(uuid.getUniqueId(), PlayerDb.Stat.SEASON_1_THREES, stats.getThrees());
                    }

                    // Performance-based coin rewards
                    int coinReward = calculateCoinReward(stats, false, this.isRecGame);
                    AthleteManager.get(uuid.getUniqueId()).giveCoins(coinReward, true);
                    
                    // Award Season Pass EXP for ranked games (losers get exp too!)
                    if (this.settings.compType == CompType.RANKED && stats != null) {
                        me.x_tias.partix.plugin.seasonpass.SeasonPassManager.awardGameExp(
                            uuid.getUniqueId(),
                            stats.getPoints(),
                            stats.getAssists(),
                            stats.getRebounds(),
                            stats.getSteals(),
                            stats.getBlocks(),
                            false // loser
                        );
                    }
                });
            }
            this.points.forEach((uuid, integer) -> BasketballDb.add(uuid, BasketballDb.Stat.POINTS, integer));
            this.threes.forEach((uuid, integer) -> BasketballDb.add(uuid, BasketballDb.Stat.THREES, integer));
        }

        if (Boolean.TRUE.equals(this.getCustomProperty("anteUp"))) {
            AnteUpManager.handleAnteUpPayout(this, winner);
        }

        this.setState(GoalGame.State.FINAL);
        this.sendMessage(Message.gameOver(this.homeScore, this.awayScore));
        this.displayTeamStatsWithCopyButton();
        this.displayNonZeroStats();
        this.generateStatsFile();
        this.startCountdown(GoalGame.State.FINAL, this.settings.waitType.med);

        if (Boolean.TRUE.equals(this.getCustomProperty("anteUp"))) {
            new BukkitRunnable() {
                public void run() {
                    for (Player p : BasketballGame.this.getPlayers()) {
                        AnteUpManager.resetPlayerState(p);
                    }
                    BasketballGame.this.reset();
                }
            }.runTaskLater(Partix.getInstance(), 400L);
        }

        this.resetStats();
        this.cancelInboundSequence();
    }

    @Override
    public void setCustomProperty(String key, Object value) {
        this.customProperties.put(key, value);
    }

    @Override
    public Object getCustomProperty(String key) {
        return this.customProperties.get(key);
    }

    @Override
    public Object getCustomPropertyOrDefault(String key, Object defaultValue) {
        return this.customProperties.getOrDefault(key, defaultValue);
    }

    @Override
    public void cancelInboundSequence() {
        this.inboundingActive = false;
        this.inbounder = null;
        this.inboundPassTime = 0L;
        this.inboundTouchedByInbounder = false;
        this.inbounderHasReleased = false;

        // Clear press prevention
        this.pressPrevention = false;
        this.pressPreventionStartTime = 0L; // ✅ RESET TIME
        this.restrictedTeam = null;
        System.out.println("Press prevention cleared - Inbound cancelled");
        
        // Clear clock freeze flags
        this.shotClockFrozenForInbound = false;
        this.gameClockFrozenForInbound = false;
        System.out.println("Clock freeze flags cleared - Inbound cancelled");

        if (this.inboundBarrierTask != null && !this.inboundBarrierTask.isCancelled()) {
            this.inboundBarrierTask.cancel();
            this.inboundBarrierTask = null;
        }
        if (this.inboundTimer != null && !this.inboundTimer.isCancelled()) {
            this.inboundTimer.cancel();
            this.inboundTimer = null;
        }
        this.inboundBall = null;
    }



    private void endGameSaveStats() {
        System.out.println("Debug: endGameSaveStats() has been triggered.");
    }

    /**
     * Calculate coin reward based on player performance
     * @param stats Player's game statistics
     * @param won Whether the player won the game
     * @param isRecGame Whether this is a REC Center game
     * @return Coin amount to award
     */
    private int calculateCoinReward(PlayerStats stats, boolean won, boolean isRecGame) {
        if (stats == null) {
            // Fallback to old minimums if no stats
            if (isRecGame) {
                return won ? 50 : 10;
            } else {
                return won ? 10 : 0;
            }
        }
        
        // Calculate performance score
        int performanceScore = 0;
        performanceScore += stats.getPoints() * 2;       // 2 coins per point
        performanceScore += stats.getAssists() * 3;      // 3 coins per assist
        performanceScore += stats.getRebounds() * 2;     // 2 coins per rebound
        performanceScore += stats.getSteals() * 4;       // 4 coins per steal
        performanceScore += stats.getBlocks() * 5;       // 5 coins per block
        performanceScore -= stats.getTurnovers() * 1;    // -1 coin per turnover
        
        // Apply win/loss modifiers and caps
        int coinReward;
        if (isRecGame) {
            // REC Center games
            if (won) {
                // Winners: 50-100 coins
                coinReward = 50 + (int)(performanceScore * 0.8);
                coinReward = Math.max(50, Math.min(100, coinReward));
            } else {
                // Losers: 10-50 coins
                coinReward = 10 + (int)(performanceScore * 0.6);
                coinReward = Math.max(10, Math.min(50, coinReward));
            }
        } else {
            // Regular ranked games
            if (won) {
                // Winners: 10-50 coins
                coinReward = 10 + (int)(performanceScore * 0.6);
                coinReward = Math.max(10, Math.min(50, coinReward));
            } else {
                // Losers: 0-25 coins
                coinReward = (int)(performanceScore * 0.4);
                coinReward = Math.max(0, Math.min(25, coinReward));
            }
        }
        
        return coinReward;
    }

    public void displayStats() {
        TextComponent statsHeader = Component.text("Game Stats", Colour.partix(), TextDecoration.BOLD);
        this.sendMessage(Component.text(" ").append(statsHeader).append(Component.text(" ")));
        for (UUID playerId : this.statsManager.getAllStats().keySet()) {
            Player player;
            PlayerStats stats = this.statsManager.getPlayerStats(playerId);
            if (stats.getPoints() <= 0 && stats.getThrees() <= 0 && stats.getAssists() <= 0 && stats.getRebounds() <= 0 && stats.getSteals() <= 0 || (player = Bukkit.getPlayer(playerId)) == null)
                continue;
            Component statsMessage = Component.text(player.getName() + ": ", Colour.title()).append(Component.text("Points: ", Colour.border()).append(Component.text(stats.getPoints(), Colour.allow()))).append(Component.text(", 3s: ", Colour.border()).append(Component.text(stats.getThrees(), Colour.allow()))).append(Component.text(", Assists: ", Colour.border()).append(Component.text(stats.getAssists(), Colour.allow()))).append(Component.text(", Rebounds: ", Colour.border()).append(Component.text(stats.getRebounds(), Colour.allow()))).append(Component.text(", Steals: ", Colour.border()).append(Component.text(stats.getSteals(), Colour.allow())));
            this.sendMessage(statsMessage);
        }
    }

    private UUID calculateMVP() {
        double rnd;
        double win;
        double pts;
        double pct;
        HashMap<UUID, Double> mvp = new HashMap<>();
        List<UUID> first = this.homeScore > this.awayScore ? new ArrayList<>(this.getHomePlayers()).stream().map(Entity::getUniqueId).toList() : new ArrayList<>(this.getAwayPlayers()).stream().map(Entity::getUniqueId).toList();
        List<UUID> second = this.homeScore > this.awayScore ? new ArrayList<>(this.getAwayPlayers()).stream().map(Entity::getUniqueId).toList() : new ArrayList<>(this.getHomePlayers()).stream().map(Entity::getUniqueId).toList();
        double firstScore = Math.max(this.homeScore, this.awayScore);
        double secondScore = Math.min(this.homeScore, this.awayScore);
        for (UUID uuid : first) {
            pct = (double) Math.max(this.points.getOrDefault(uuid, 0), 1) / firstScore * 50.0;
            pts = this.points.getOrDefault(uuid, 0) * 2 + this.threes.getOrDefault(uuid, 0);
            win = Math.abs(firstScore - secondScore);
            rnd = Math.random() / 10.0;
            mvp.putIfAbsent(uuid, pct + pts + win + rnd);
        }
        for (UUID uuid : second) {
            pct = (double) Math.max(this.points.getOrDefault(uuid, 0), 1) / secondScore * 50.0;
            pts = this.points.getOrDefault(uuid, 0) * 2 + this.threes.getOrDefault(uuid, 0);
            win = 0.0;
            rnd = Math.random() / 10.0;
            mvp.putIfAbsent(uuid, pct + pts + win + rnd);
        }
        return Util.getHighest(mvp);
    }

    @Override
    public void updateDisplay() {
        Object time;
        if (this.getState().equals(State.REGULATION) || this.getState().equals(State.OVERTIME) || this.getState().equals(State.FACEOFF)) {
            time = this.getState().equals(State.OVERTIME) ? (this.settings.suddenDeath ? "§fTime: §e0:00" : "§fTime: §e" + this.getGameTime()) : (this.settings.winType.timed ? "§fTime: §e" + this.getGameTime() : "§fFirst to: §e" + this.settings.winType.amount);
            if (this.getState().equals(State.FACEOFF)) {
                time = time + " §7(" + this.getCountSeconds() + "s)";
            }
        } else {
            time = this.getState().equals(State.PREGAME) ? "§bPregame" + (this.getCountSeconds() > 0 ? ": §f" + this.getCountSeconds() + "s" : "") : (this.getState().equals(State.FINAL) ? "§cGame Over" + (this.getCountSeconds() > 0 ? ": §f" + this.getCountSeconds() + "s" : "") : "§fStoppage");
        }
        int MAX_TO = 4;
        StringBuilder homeTO = new StringBuilder("§fTimeouts §7[");
        StringBuilder awayTO = new StringBuilder("§fTimeouts §7[");
        for (int i = 0; i < 4; ++i) {
            homeTO.append(i < this.homeTimeouts ? "§a§l⬤" : "§8◯");
            awayTO.append(i < this.awayTimeouts ? "§a§l⬤" : "§8◯");
        }
        homeTO.append("§7] §0");
        awayTO.append("§7] §1");
        
        // Update sidebar for each player individually to show their own teammate assignments
        Basketball bball = null;
        if (this.getBall() instanceof Basketball) {
            bball = (Basketball) this.getBall();
        }
        
        for (Player p : this.getPlayers()) {
            if (bball != null) {
                Player key2 = bball.getTeammateByHotkey(2, p);
                Player key3 = bball.getTeammateByHotkey(3, p);
                
                if (key2 != null || key3 != null) {
                    String teammateInfo = "§e§lTrack Pass";
                    // Use simple number keys instead of keybind translation
                    String key2Line = key2 != null ? "  §7[2] §f" + key2.getName() : "";
                    String key3Line = key3 != null ? "  §7[3] §f" + key3.getName() : "";
                    
                    Sidebar.set(p, Component.text("\uF808\uF808〩"), "                     ", " ", "§b§lMatch Info", "  " + time, "  §f" + (this.settings.winType.timed ? this.getShortPeriodString() : "---"), "", "§f§lScoreboard", "  §1Home: §a" + this.homeScore + " §7(" + Text.serialize(this.home.abrv) + ")", homeTO.toString(), "  §4Away: §a" + this.awayScore + " §7(" + Text.serialize(this.away.abrv) + ")", awayTO.toString(), "", teammateInfo, key2Line, key3Line);
                    continue;
                }
            }
            
            // No teammates or no ball - show normal sidebar
            Sidebar.set(p, Component.text("\uF808\uF808〩"), "                     ", " ", "§b§lMatch Info", "  " + time, "  §f" + (this.settings.winType.timed ? this.getShortPeriodString() : "---"), "", "§f§lScoreboard", "  §1Home: §a" + this.homeScore + " §7(" + Text.serialize(this.home.abrv) + ")", homeTO.toString(), "  §4Away: §a" + this.awayScore + " §7(" + Text.serialize(this.away.abrv) + ")", awayTO.toString());
        }
        
        this.updateBossBar("§r§f§l" + Text.serialize(this.away.name) + " §7§l" + this.awayScore + " §r§e@ §7§l" + this.homeScore + " §r§f§l" + Text.serialize(this.home.name), Math.min(1.0, Math.max(0.0, (double) this.getTimeTicks() / (double) (this.settings.winType.amount * 60 * 20))));
    }

// Replace the goal() method in BasketballGame.java with this fixed version:

// Replace the ENTIRE goal() method in BasketballGame.java starting around line 1650

    @Override
    public void goal(GoalGame.Team team) {
        Ball ball = this.getBall();
        if (ball == null) return;

        // ❌ REMOVED: Don't clear press prevention immediately!
        // OLD CODE: this.pressPrevention = false; this.restrictedTeam = null;

        if (ball instanceof Basketball ball2) {
            // Check if ball is in hoop
            boolean inHomeNet = this.getHomeNet().clone().expand(0.3).contains(ball2.getLocation().toVector());
            boolean inAwayNet = this.getAwayNet().clone().expand(0.3).contains(ball2.getLocation().toVector());

            if (!inHomeNet && !inAwayNet) {
                return;
            }

            if (ball2.layupScored) {
                return;
            }

            if (ball2.shouldPreventScore) {
                System.out.println("Shot prevented from scoring (bad release)");
                ball2.layupScored = true;
                ball2.shouldPreventScore = false;
                this.removeBalls();

                Bukkit.getScheduler().runTaskLater(Partix.getInstance(), () -> {
                    Player shooter = ball2.getLastDamager();
                    Team inboundTeam = Team.HOME;

                    if (shooter != null) {
                        if (this.getHomePlayers().contains(shooter)) {
                            inboundTeam = Team.AWAY;
                        } else if (this.getAwayPlayers().contains(shooter)) {
                            inboundTeam = Team.HOME;
                        }
                    }

                    Location inboundSpot;
                    if (inboundTeam == Team.HOME) {
                        inboundSpot = this.getHomeSpawn().clone().add(0, 1.2, 6);
                    } else {
                        inboundSpot = this.getAwaySpawn().clone().add(0, 1.2, -6);
                    }

                    Ball newBall = this.setBall(BallFactory.create(inboundSpot, BallType.BASKETBALL, this));
                    newBall.setStealDelay(0);
                    newBall.setVelocity(0, 0.05, 0.0);
                    
                    // Initialize teammate hotkeys for new ball
                    if (newBall instanceof Basketball basketball) {
                        basketball.initializeAllTeammateHotkeys(this);
                    }

                    this.resetShotClock();
                }, 20L);

                return;
            }

            ball2.layupScored = true;
            ball2.shouldPreventScore = false;

            // PREGAME REBOUND MACHINE - skip normal scoring
            if (this.settings.reboundMachineEnabled && this.getState().equals(State.PREGAME)) {
                Player scorer = ball2.getLastDamager();
                if (scorer != null && scorer.isOnline()) {
                    UUID scorerId = scorer.getUniqueId();
                    this.reboundMachineShotsMade.put(scorerId, this.reboundMachineShotsMade.getOrDefault(scorerId, 0) + 1);

                    int shotsMade = this.reboundMachineShotsMade.get(scorerId);
                    int shotsTaken = this.reboundMachineShotsTaken.getOrDefault(scorerId, 0);
                    double shootingPercentage = shotsTaken > 0 ? ((double) shotsMade / (double) shotsTaken) * 100.0 : 0.0;

                    Component statsMessage = Component.text("Shots Taken: ", Colour.border())
                            .append(Component.text(shotsTaken, Colour.title()))
                            .append(Component.text(" | Shots Made: ", Colour.border()))
                            .append(Component.text(shotsMade, Colour.allow()))
                            .append(Component.text(" | Shot %: ", Colour.border()))
                            .append(Component.text(String.format("%.1f%%", shootingPercentage), Colour.partix()))
                            .append(Component.text(" (" + shotsMade + "/" + shotsTaken + ")", Colour.text()));

                    scorer.sendMessage(statsMessage);

                    Location hoopLocation;
                    if (team.equals(Team.HOME)) {
                        hoopLocation = this.getHomeNet().getCenter().toLocation(scorer.getWorld());
                    } else {
                        hoopLocation = this.getAwayNet().getCenter().toLocation(scorer.getWorld());
                    }

                    Location underRim = hoopLocation.clone().subtract(0, 2.0, 0);
                    ball2.remove();

                    Bukkit.getScheduler().runTaskLater(Partix.getInstance(), () -> {
                        if (scorer.isOnline()) {
                            Ball newBall = BallFactory.create(underRim, BallType.BASKETBALL, this);
                            Location playerLoc = scorer.getLocation();
                            Vector direction = playerLoc.toVector().subtract(underRim.toVector()).normalize();
                            newBall.setVelocity(direction.getX() * 0.75, 0.15, direction.getZ() * 0.75);
                            scorer.playSound(scorer.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 0.5f, 1.2f);
                        }
                    }, 10L);
                }
                return;
            }

            // ===== 1V1 HALF-COURT LOGIC =====
            if (this.isSingleHoopMode) {
                if (ball2.getCurrentDamager() != null) {
                    return;
                }

                Player scorer = ball2.getLastDamager();
                if (scorer == null) {
                    return;
                }

                UUID scorerId = scorer.getUniqueId();
                boolean isThree = ball2.isThreeEligible();

                PlayerStats stats = this.statsManager.getPlayerStats(scorerId);
                this.checkAssistEligibility(scorer);
                stats.incrementFGMade();
                if (isThree) {
                    stats.increment3FGMade();
                    stats.incrementThrees();
                }

                stats.addPoints(isThree ? 3 : 2);

                GoalGame.Team scorerTeam;
                if (this.getHomePlayers().contains(scorer)) {
                    scorerTeam = Team.HOME;
                } else if (this.getAwayPlayers().contains(scorer)) {
                    scorerTeam = Team.AWAY;
                } else {
                    scorerTeam = Team.HOME;
                }

                int pointsScored = isThree ? 3 : 2;

                if (scorerTeam.equals(Team.HOME)) {
                    this.homeScore += pointsScored;
                } else {
                    this.awayScore += pointsScored;
                }

                Component scoringMessage = Component.text(scorer.getName() + (isThree ? " - 3 POINTS!" : " - 2 POINTS!"))
                        .color(scorerTeam.equals(Team.HOME) ? TextColor.color(0x00AAFF) : TextColor.color(0xFFAA00))
                        .decorate(TextDecoration.BOLD);

                this.sendTitle(scoringMessage);

                this.points.put(scorerId, this.points.getOrDefault(scorerId, 0) + pointsScored);
                if (isThree) {
                    this.threes.put(scorerId, this.threes.getOrDefault(scorerId, 0) + 1);
                    if (scorer.hasPermission("rank.vip") || scorer.hasPermission("rank.pro")) {
                        PlayerDb.add(scorerId, PlayerDb.Stat.COINS, 1);
                    }
                }

                AthleteManager.get(scorerId).getExplosion().mediumExplosion(ball2.getLocation().clone());
                Location hoopLocation = this.getHomeNet().getCenter().toLocation(scorer.getWorld());
                AthleteManager.get(scorerId).getExplosion().mediumExplosion(hoopLocation);

                // Check 1v1 win condition
                int currentHomeScore = this.homeScore;
                int currentAwayScore = this.awayScore;

                boolean gameOver = false;
                Team winner = null;

                if (currentHomeScore >= 21 && (currentHomeScore - currentAwayScore) >= 2) {
                    gameOver = true;
                    winner = Team.HOME;
                } else if (currentAwayScore >= 21 && (currentAwayScore - currentHomeScore) >= 2) {
                    gameOver = true;
                    winner = Team.AWAY;
                }

                if (gameOver) {
                    this.gameOver(winner);
                    this.sendTitle(Component.text("The ").color(Colour.partix()).append(
                            winner.equals(Team.HOME) ? this.home.name : this.away.name
                    ).append(Component.text(" Win!").color(Colour.partix())));
                    this.removeBalls();
                } else {
                    this.removeBalls();
                    final Player finalScorer = scorer;
                    Bukkit.getScheduler().runTaskLater(Partix.getInstance(), () -> {
                        this.resetHalfCourt1v1PositionsWithOffense(finalScorer);
                    }, 40L);
                }

                ball2.clearPerfectShot();
                this.lastPossessionTeam = null;
                this.resetShotClockTo12();
                return;
            }

            // ===== REGULAR GAME (2V2, 3V3, 4V4) SCORING LOGIC =====
            BaseTeam t = team.equals(Team.HOME) ? this.home : this.away;
            boolean isThree = ball2.isThreeEligible();
            Player scorer = ball2.getLastDamager();

            if (scorer != null) {
                UUID scorerId = scorer.getUniqueId();
                this.checkAssistEligibility(scorer);
                PlayerStats stats = this.statsManager.getPlayerStats(scorerId);
                stats.incrementFGMade();
                if (isThree) {
                    stats.increment3FGMade();
                    stats.incrementThrees();
                }

                stats.addPoints(isThree ? 3 : 2);
                this.sendMessage(Component.text(scorer.getName() + "'s current stats: ").color(Colour.title())
                        .append(Component.text("Points: ", Colour.border()))
                        .append(Component.text(stats.getPoints(), Colour.allow()))
                        .append(Component.text(", 3s: ", Colour.border()))
                        .append(Component.text(stats.getThrees(), Colour.allow()))
                        .append(Component.text(", Assists: ", Colour.border()))
                        .append(Component.text(stats.getAssists(), Colour.allow()))
                        .append(Component.text(", Rebounds: ", Colour.border()))
                        .append(Component.text(stats.getRebounds(), Colour.allow()))
                        .append(Component.text(", Steals: ", Colour.border()))
                        .append(Component.text(stats.getSteals(), Colour.allow())));
            }

            ball2.setReboundEligible(false);

            if (this.settings.gameType.equals(GameType.AUTOMATIC)) {
                List<Player> players = team.equals(Team.HOME) ? this.getHomePlayers() : this.getAwayPlayers();
                if (scorer != null && players.contains(scorer)) {
                    this.points.put(scorer.getUniqueId(), this.points.getOrDefault(scorer.getUniqueId(), 0) + (isThree ? 3 : 2));
                    if (isThree) {
                        this.threes.put(scorer.getUniqueId(), this.threes.getOrDefault(scorer.getUniqueId(), 0) + 1);
                        if (scorer.hasPermission("rank.vip") || scorer.hasPermission("rank.pro")) {
                            PlayerDb.add(scorer.getUniqueId(), PlayerDb.Stat.COINS, 1);
                        }
                    }
                }
            }

            // Generate scoring message
            Component scoringMessage;
            if (scorer != null) {
                String scorerName = scorer.getName();
                Location hoopLoc = team.equals(Team.HOME) ?
                        this.getAwayNet().getCenter().toLocation(scorer.getWorld()) :
                        this.getHomeNet().getCenter().toLocation(scorer.getWorld());

                double distance = ball2.getShotDistance();
                if (distance == 0.0) {
                    Location scorerLoc = scorer.getLocation();
                    distance = scorerLoc.distance(hoopLoc);
                }

                String shotType;
                if (isThree) {
                    if (distance > 8.0) {
                        shotType = "FROM DOWNTOWN!";
                    } else {
                        shotType = "FOR 3!";
                    }
                } else {
                    if (ball2.isDunkAttempt) {
                        shotType = "DUNKED THE BALL!";
                    } else if (distance < 3.5) {
                        shotType = "WITH THE LAYUP!";
                    } else if (distance < 5.0) {
                        shotType = "IN THE PAINT!";
                    } else if (distance < 7.0) {
                        shotType = "WITH THE MID-RANGE JUMPER!";
                    } else {
                        shotType = "WITH THE LONG TWO!";
                    }
                }

                scoringMessage = Component.text(scorerName + " " + shotType)
                        .color(team.equals(Team.HOME) ? TextColor.color(0x00AAFF) : TextColor.color(0xFFAA00))
                        .decorate(TextDecoration.BOLD);

                Location hoopLocation = team.equals(Team.HOME) ?
                        this.getAwayNet().getCenter().toLocation(scorer.getWorld()) :
                        this.getHomeNet().getCenter().toLocation(scorer.getWorld());

                AthleteManager.get(scorer.getUniqueId()).getExplosion().mediumExplosion(hoopLocation);
            } else {
                scoringMessage = t.name.append(Component.text(isThree ? " ‣ 3 Points!" : " ‣ 2 Points").color(Colour.partix()));
            }

            this.sendTitle(scoringMessage);

            this.justScored = true;
            Bukkit.getScheduler().runTaskLater(Partix.getInstance(), () -> {
                this.justScored = false;
            }, 40L);

            // ===== UPDATE SCORE AND CHECK WIN CONDITION =====
            if (team.equals(Team.HOME)) {
                this.homeScore += isThree ? 3 : 2;

                // Check win condition for ranked games
                if (this.settings.compType == CompType.RANKED &&
                        this.settings.winType.winByTwo &&
                        this.settings.winType.amount == 21) {

                    int currentHomeScore = this.homeScore;
                    int currentAwayScore = this.awayScore;

                    boolean homeWins = currentHomeScore >= 21 && (currentHomeScore - currentAwayScore) >= 2;

                    if (homeWins) {
                        Bukkit.getLogger().info("🎉 HOME WINS RANKED GAME: " + currentHomeScore + "-" + currentAwayScore);
                        this.gameOver(Team.HOME);
                        this.sendTitle(Component.text("The ").color(Colour.partix()).append(
                                this.home.name
                        ).append(Component.text(" Win!").color(Colour.partix())));
                        this.removeBalls();
                        return;
                    }
                }

                // ===== INBOUND SEQUENCE AFTER HOME SCORES =====
                this.removeBalls();

                for (Player player : this.getHomePlayers()) {
                    if (player != null && player.isOnline()) {
                        if (this.isInBench(player)) {
                            continue;
                        }
                        Location spawnLoc = this.getHomeSpawn().clone();
                        spawnLoc.setX(spawnLoc.getX() - 3.0);
                        player.teleport(spawnLoc);
                    }
                }

                this.clearInboundState();
                this.setState(State.OUT_OF_BOUNDS_THROW_WAIT);
                this.outOfBoundsLostTeam = Team.AWAY;
                this.outOfBoundsSide = false;

                Location awayInboundSpot = this.getAwaySpawn().clone().add(0, 1.2, -6);
                this.inboundSpot = awayInboundSpot.clone();
                this.inboundingTeam = Team.AWAY;

                this.inboundingActive = true;
                this.inboundTouchedByInbounder = false;
                this.inbounderHasReleased = false;
                this.inbounder = null;

                // Activate press prevention as soon as inbound starts
                this.pressPrevention = true;
                this.restrictedTeam = Team.HOME; // Restrict home team (opposite of inbounding away team)
                this.pressPreventionStartTime = System.currentTimeMillis();

                this.outOfBoundsZ = this.getAwayNet().getCenter().getBlockZ() - 2;
                this.outOfBoundsHome = false;

                this.setOutOfBoundsImmunity(true);

                Bukkit.getScheduler().runTaskLater(Partix.getInstance(), () -> {
                    Ball newBall = this.setBall(BallFactory.create(awayInboundSpot, BallType.BASKETBALL, this));
                    newBall.setStealDelay(0);
                    newBall.setVelocity(0, 0.05, 0.0);
                    
                    // Initialize teammate hotkeys for new ball
                    if (newBall instanceof Basketball basketball) {
                        basketball.initializeAllTeammateHotkeys(this);
                    }
                }, 20L);

                Bukkit.getScheduler().runTaskLater(Partix.getInstance(), () -> {
                    this.setOutOfBoundsImmunity(false);
                }, 140L);

            } else {
                // ===== AWAY TEAM SCORED =====
                this.awayScore += isThree ? 3 : 2;

                // Check win condition for ranked games
                if (this.settings.compType == CompType.RANKED &&
                        this.settings.winType.winByTwo &&
                        this.settings.winType.amount == 21) {

                    int currentHomeScore = this.homeScore;
                    int currentAwayScore = this.awayScore;

                    boolean awayWins = currentAwayScore >= 21 && (currentAwayScore - currentHomeScore) >= 2;

                    if (awayWins) {
                        Bukkit.getLogger().info("🎉 AWAY WINS RANKED GAME: " + currentHomeScore + "-" + currentAwayScore);
                        this.gameOver(Team.AWAY);
                        this.sendTitle(Component.text("The ").color(Colour.partix()).append(
                                this.away.name
                        ).append(Component.text(" Win!").color(Colour.partix())));
                        this.removeBalls();
                        return;
                    }
                }

                // ===== INBOUND SEQUENCE AFTER AWAY SCORES =====
                this.removeBalls();

                for (Player player : this.getAwayPlayers()) {
                    if (player != null && player.isOnline()) {
                        if (this.isInBench(player)) {
                            continue;
                        }
                        Location spawnLoc = this.getAwaySpawn().clone();
                        spawnLoc.setX(spawnLoc.getX() + 3.0);
                        player.teleport(spawnLoc);
                    }
                }

                this.clearInboundState();
                this.setState(State.OUT_OF_BOUNDS_THROW_WAIT);
                this.outOfBoundsLostTeam = Team.HOME;
                this.outOfBoundsSide = false;

                Location homeInboundSpot = this.getHomeSpawn().clone().add(0, 1.2, 6);
                this.inboundSpot = homeInboundSpot.clone();
                this.inboundingTeam = Team.HOME;


                this.inboundingActive = true;
                this.inboundTouchedByInbounder = false;
                this.inbounderHasReleased = false;
                this.inbounder = null;

                // Activate press prevention as soon as inbound starts
                this.pressPrevention = true;
                this.restrictedTeam = Team.AWAY; // Restrict away team (opposite of inbounding home team)
                this.pressPreventionStartTime = System.currentTimeMillis();

                this.outOfBoundsZ = this.getHomeNet().getCenter().getBlockZ() + 2;
                this.outOfBoundsHome = true;

                this.setOutOfBoundsImmunity(true);

                Bukkit.getScheduler().runTaskLater(Partix.getInstance(), () -> {
                    Ball newBall = this.setBall(BallFactory.create(homeInboundSpot, BallType.BASKETBALL, this));
                    newBall.setStealDelay(0);
                    newBall.setVelocity(0, 0.05, 0.0);
                    
                    // Initialize teammate hotkeys for new ball
                    if (newBall instanceof Basketball basketball) {
                        basketball.initializeAllTeammateHotkeys(this);
                    }
                }, 20L);

                Bukkit.getScheduler().runTaskLater(Partix.getInstance(), () -> {
                    this.setOutOfBoundsImmunity(false);
                }, 140L);
            }

            ball2.clearPerfectShot();
            this.lastPossessionTeam = null;
            this.resetShotClock();
        }
    }

    @Override
    public void joinTeam(Player player, GoalGame.Team team) {
        Athlete athlete = AthleteManager.get(player.getUniqueId());
        if (team.equals(Team.HOME)) {
            athlete.setSpectator(false);
            this.addHomePlayer(player);
            player.getActivePotionEffects().clear();
            if (this.settings.gameEffect.effect != null) {
                player.addPotionEffect(this.settings.gameEffect.effect);
            }
            player.getInventory().setChestplate(this.home.chest);

            // ACCESSORY SYSTEM - Load selected accessory
            PlayerDb.get(player.getUniqueId(), PlayerDb.Stat.ACCESSORY).thenAccept(accessory -> {
                Bukkit.getScheduler().runTask(Partix.getInstance(), () -> {
                    if (accessory == 1) {
                        // Headband selected - apply team color
                        ItemStack helmet = Items.armor(Material.LEATHER_HELMET,
                                this.home.pants.getItemMeta() instanceof LeatherArmorMeta homeMeta ?
                                        ((LeatherArmorMeta) this.home.pants.getItemMeta()).getColor().asRGB() : 0xFFFFFF,
                                "Headband", "Your team headband");
                        player.getInventory().setHelmet(helmet);
                    } else if (accessory == 2) {
                        // Remmy hat selected - equip carved pumpkin head
                        ItemStack pumpkinHead = new ItemStack(Material.CARVED_PUMPKIN);
                        player.getInventory().setHelmet(pumpkinHead);
                    } else if (accessory == 3) {
                        // Champion's Crown - golden helmet
                        ItemStack crown = new ItemStack(Material.GOLDEN_HELMET);
                        player.getInventory().setHelmet(crown);
                    } else {
                        // Nothing selected - no helmet
                        player.getInventory().setHelmet(null);
                    }
                });
            });

            player.getInventory().setLeggings(this.home.pants);
            player.getInventory().setBoots(this.home.boots);
            player.getInventory().setItem(7, Items.get(Component.text("Game Settings").color(Colour.partix()), Material.CHEST));
            player.getInventory().setItem(8, Items.get(Component.text("Change Team/Leave Game").color(Colour.partix()), Material.GRAY_DYE));

            // FIXED: Never auto-bench in ranked games - always spawn at team spawn
            if (this.settings.compType == CompType.RANKED || this.getHomePlayers().size() <= this.settings.playersPerTeam) {
                player.teleport(this.getHomeSpawn());
            } else {
                this.enterBench(player);
            }
            player.sendMessage(Message.joinTeam("home"));
            this.joinedHome.add(player.getUniqueId());
            this.removeAwayPlayer(player);
            
            // Re-initialize hotkeys when team composition changes
            if (this.getBall() instanceof Basketball basketball) {
                basketball.initializeAllTeammateHotkeys(this);
            }

        } else if (team.equals(Team.AWAY)) {
            athlete.setSpectator(false);
            this.addAwayPlayer(player);
            player.getActivePotionEffects().clear();
            if (this.settings.gameEffect.effect != null) {
                player.addPotionEffect(this.settings.gameEffect.effect);
            }
            player.getInventory().setChestplate(Items.armor(Material.LEATHER_CHESTPLATE, 0xFFFFFF, "Jersey", "Your teams away jersey"));

            // ACCESSORY SYSTEM - Load selected accessory
            PlayerDb.get(player.getUniqueId(), PlayerDb.Stat.ACCESSORY).thenAccept(accessory -> {
                Bukkit.getScheduler().runTask(Partix.getInstance(), () -> {
                    if (accessory == 1) {
                        // Headband selected - apply team color
                        ItemStack helmet = Items.armor(Material.LEATHER_HELMET,
                                this.away.away.getItemMeta() instanceof LeatherArmorMeta awayMeta ?
                                        ((LeatherArmorMeta) this.away.away.getItemMeta()).getColor().asRGB() : 0xFFFFFF,
                                "Headband", "Your team headband");
                        player.getInventory().setHelmet(helmet);
                    } else if (accessory == 2) {
                        // Remmy hat selected - equip carved pumpkin head
                        ItemStack pumpkinHead = new ItemStack(Material.CARVED_PUMPKIN);
                        player.getInventory().setHelmet(pumpkinHead);
                    } else {
                        // Nothing selected - no helmet
                        player.getInventory().setHelmet(null);
                    }
                });
            });

            player.getInventory().setLeggings(this.away.away);
            player.getInventory().setBoots(this.away.boots);
            player.getInventory().setItem(7, Items.get(Component.text("Game Settings").color(Colour.partix()), Material.CHEST));
            player.getInventory().setItem(8, Items.get(Component.text("Change Team").color(Colour.partix()), Material.GRAY_DYE));

            // FIXED: Never auto-bench in ranked games - always spawn at team spawn
            if (this.settings.compType == CompType.RANKED || this.getAwayPlayers().size() <= this.settings.playersPerTeam) {
                player.teleport(this.getAwaySpawn());
            } else {
                this.enterBench(player);
            }
            player.sendMessage(Message.joinTeam("away"));
            this.joinedAway.add(player.getUniqueId());
            this.removeHomePlayer(player);
            
            // Re-initialize hotkeys when team composition changes
            if (this.getBall() instanceof Basketball basketball) {
                basketball.initializeAllTeammateHotkeys(this);
            }

        } else {
            player.getActivePotionEffects().clear();
            athlete.setSpectator(true);
            player.getInventory().clear();
            player.getInventory().setItem(7, Items.get(Component.text("Game Settings").color(Colour.partix()), Material.CHEST));
            player.getInventory().setItem(8, Items.get(Component.text("Change Team").color(Colour.partix()), Material.GRAY_DYE));
            player.teleport(this.getCenter().add(0.0, 10.0, 0.0));
            player.sendMessage(Message.joinTeam("spectators"));
        }
        this.updateDisplay();
    }

    @Override
    public BallType getBallType() {
        return BallType.BASKETBALL;
    }

    @Override
    public boolean canEditGame(Player player) {
        if (player.hasPermission("rank.admin")) {
            return true;
        }
        if (this.owner != null) {
            return this.owner == player.getUniqueId();
        }
        return false;
    }

    @Override
    public void onJoin(Athlete... athletes) {
        if (this.settings.compType.equals(CompType.CASUAL)) {
            for (Athlete athlete : athletes) {
                this.joinTeam(athlete.getPlayer(), GoalGame.Team.SPECTATOR);
            }
        }
        
        // Start location-based music for all joining players
        for (Athlete athlete : athletes) {
            Player player = athlete.getPlayer();
            if (player != null) {
                Partix.getInstance().getLocationMusicManager().startLocationMusic(player);
            }
        }
    }
    public void activatePressPreventionForInbound(Player inbounder) {
        if (this.isHalfCourt1v1 || this.isSingleHoopMode) {
            return;
        }

        if (!this.inboundingActive || this.inboundingTeam == null) {
            return;
        }

        // Determine which team should be restricted (the defensive team)
        Team restrictTeam = (this.inboundingTeam == Team.HOME) ? Team.AWAY : Team.HOME;

        this.pressPrevention = true;
        this.restrictedTeam = restrictTeam;
        this.pressPreventionStartTime = System.currentTimeMillis();
    }
    @Override
    public void onQuit(Athlete... athletes) {
        for (Athlete athlete : athletes) {
            Player player = athlete.getPlayer();
            if (player == null) continue;
            this.clearCoachIfPresent(player.getUniqueId());
            boolean isProAm = Boolean.TRUE.equals(this.getCustomPropertyOrDefault("proam", false));
            if (this.settings.compType == CompType.RANKED && !this.getState().equals(State.FINAL) && !isProAm) {
                SeasonDb.remove(player.getUniqueId(), SeasonDb.Stat.POINTS, 3);
                player.sendMessage("§cYou disconnected from a ranked game. You lost 3 Season Points!");
            }
            
            // Stop music before removing athlete
            Partix.getInstance().getLocationMusicManager().stopLocationMusic(player);
            
            this.removeAthlete(athlete);
        }
        if (this.getPlayers().isEmpty()) {
            if (!this.getState().equals(State.FINAL)) {
                Bukkit.getLogger().info("No players remain. Ending with no changes..");
            }
            this.reset();
            return;
        }
        if (!(this.settings.playersPerTeam != 2 && this.settings.playersPerTeam != 3 || this.settings.compType != CompType.RANKED || this.getState().equals(State.FINAL))) {
            int homeCount = this.getHomePlayers().size();
            int awayCount = this.getAwayPlayers().size();
            if (homeCount == 0 && awayCount > 0) {
                Bukkit.broadcast(Component.text("§aThe away team is automatically declared the winner (other side left!)"));
                this.gameOver(GoalGame.Team.AWAY);
            } else if (awayCount == 0 && homeCount > 0) {
                Bukkit.broadcast(Component.text("§aThe home team is automatically declared the winner (other side left!)"));
                this.gameOver(GoalGame.Team.HOME);
            }
        }
    }

    public void removeAthlete(Athlete athlete) {
        Player player = athlete.getPlayer();
        this.getHomePlayers().remove(player);
        this.getAwayPlayers().remove(player);
    }

    @Override
    public void stoppageDetection() {
        Vector v = this.getBall().getVelocity();
        boolean dead = false;
        --this.stoppageTicks;
        if (this.stoppageTicks < 0) {
            Location ballLocation = this.getBall().getLocation();
            if (this.stoppageLastLocation != null) {
                this.stoppageTicks = 60;
                if (this.stoppageLastLocation.distance(ballLocation) < 0.2) {
                    // dead = true;
                }
            }
            this.stoppageLastLocation = ballLocation.clone();
        }
        double motion = Math.abs(v.getX()) + Math.abs(v.getY()) + Math.abs(v.getZ());
        if (motion < 0.01) {
            // dead = true;
        }

        final Location blockLoc = this.getBall().getLocation().clone();

        // Check for out of bounds - BUT NOT DURING STOPPAGE OR WHEN IMMUNITY IS ACTIVE
        if (!this.getArenaBox().contains(blockLoc.getX(), blockLoc.getY(), blockLoc.getZ())) {
            System.out.println("OUT OF BOUNDS - Ball detected outside arena");
            System.out.println("DEBUG: State=" + this.getState() + ", Immunity=" + this.outOfBoundsImmunity);

            // Skip OOB during stoppage, timeouts, or when immunity is active
            if (this.getState().equals(State.STOPPAGE) || this.outOfBoundsImmunity || this.justScored) {
                System.out.println("DEBUG: Skipping OOB detection (STOPPAGE or IMMUNITY)");
                return;
            }

            // If inbound is active and within 1 second grace period, DON'T trigger OOB
            if (this.inboundingActive && this.inboundPassTime > 0L) {
                long timeSincePass = System.currentTimeMillis() - this.inboundPassTime;

                if (timeSincePass < INBOUND_OOB_DELAY) {
                    System.out.println("Ball OOB during inbound grace period (" + timeSincePass + "ms) - ignoring");
                    return; // SKIP OOB detection
                } else {
                    System.out.println("Grace period expired - triggering OOB detection");
                    this.inboundPassTime = 0L; // Clear for next time
                }
            }

            if (state != State.OUT_OF_BOUNDS_THROW_WAIT && state != State.OUT_OF_BOUNDS_THROW) {
                outOfBounds();
            }
        }
        if (dead) {
            if (this.isHalfCourt1v1) {
                this.resetHalfCourt1v1Positions();
                return;
            }

            this.removeBalls();
            this.resetShotClock();
            this.sendTitle(Component.text("Dead Ball").style(Style.style(Colour.deny(), TextDecoration.BOLD)));
            this.startCountdown(GoalGame.State.FACEOFF, 10);
        }
    }

// Add this method to your BasketballGame class

    public void outOfBounds() {
        Ball ball = getBall();
        if (ball == null) return;
        if (outOfBoundsImmunity) {
            System.out.println("Out of bounds immunity is active, skipping out of bounds handling.");
            return;
        }
        
        // Safety check: If ball goes OOB again during inbound WITHOUT being touched, 
        // the clocks are already frozen - just re-trigger the inbound process
        if (this.inboundingActive && this.gameClockFrozenForInbound) {
            System.out.println("⚠️ Ball went OOB again during inbound WITHOUT being touched - clocks remain frozen");
            // Don't re-freeze (already frozen), just continue with normal OOB logic
        }
        
        this.pressPrevention = false;
        this.pressPreventionStartTime = 0L; // ✅ RESET TIMER
        this.restrictedTeam = null;

        // ===== CRITICAL: For 1v1 games, SKIP INBOUND SYSTEM ENTIRELY =====
        if (this.isHalfCourt1v1) {
            System.out.println("1v1 game detected - handling turnover");

            // Get the player who last had the ball (threw it out or shot it)
            Player lastPossessor = null;
            if (ball instanceof Basketball basketball) {
                UUID lastPossessorUUID = basketball.getLastDamager() != null ?
                        basketball.getLastDamager().getUniqueId() : null;
                if (lastPossessorUUID != null) {
                    lastPossessor = Bukkit.getPlayer(lastPossessorUUID);
                }
            }

            if (lastPossessor == null) {
                System.out.println("No last possessor found, resetting randomly");
                this.resetHalfCourt1v1Positions();
                return;
            }

            // Find the other player (the one who should get the ball)
            List<Player> allPlayers = new ArrayList<>();
            allPlayers.addAll(this.getHomePlayers());
            allPlayers.addAll(this.getAwayPlayers());

            if (allPlayers.size() != 2) {
                System.out.println("ERROR: 1v1 doesn't have 2 players!");
                this.resetHalfCourt1v1Positions();
                return;
            }

            Player otherPlayer = allPlayers.get(0).equals(lastPossessor) ?
                    allPlayers.get(1) : allPlayers.get(0);

            System.out.println("1v1 Turnover: " + lastPossessor.getName() + " turned it over");
            System.out.println("Ball goes to: " + otherPlayer.getName());
            
            // Add turnover stat
            if (this.getState().equals(State.REGULATION) || this.getState().equals(State.OVERTIME)) {
                this.statsManager.getPlayerStats(lastPossessor.getUniqueId()).incrementTurnovers();
            }

            // Reset with OTHER player as offensive (gets the ball back)
            this.resetHalfCourt1v1PositionsWithOffense(otherPlayer);
            return;  // EXIT - Don't run any inbound logic
        }

        Location outOfBoundsLocation = this.findClosestPointOnBoundary(ball.getLocation());
        removeBalls();

        Player lastPossessor = null;
        boolean wasBallPoked = false;
        boolean wasBallBlocked = false;
        if (ball instanceof Basketball basketball) {
            UUID lastPossessorUUID = basketball.getTrueLastPossessor();
            wasBallPoked = basketball.wasPoked;
            wasBallBlocked = (basketball.lastShotBlockerUUID != null);
            if (lastPossessorUUID != null) {
                lastPossessor = Bukkit.getPlayer(lastPossessorUUID);
            }
        }

// Determine possession
        Team inboundTeam;
        if (lastPossessor != null) {
            Team lastPossessorTeam = null;

            // ✅ SAFER TEAM DETECTION
            if (getHomePlayers().contains(lastPossessor)) {
                lastPossessorTeam = Team.HOME;
            } else if (getAwayPlayers().contains(lastPossessor)) {
                lastPossessorTeam = Team.AWAY;
            }

            // ✅ ONLY PROCEED IF WE KNOW THE TEAM
            if (lastPossessorTeam != null) {
                if (wasBallPoked || wasBallBlocked) {
                    // Victim's team keeps possession
                    inboundTeam = lastPossessorTeam;
                    System.out.println("DEBUG: " + (wasBallPoked ? "Poke" : "Block") +
                            " OOB - " + lastPossessorTeam + " keeps ball");
                } else {
                    // Normal OOB - opposite team gets ball
                    inboundTeam = (lastPossessorTeam == Team.HOME) ? Team.AWAY : Team.HOME;
                    System.out.println("DEBUG: Normal OOB - " + inboundTeam + " gets ball");
                }
            } else {
                // ⚠️ FALLBACK: Player not on either team (disconnected?)
                // Default to HOME team
                inboundTeam = Team.HOME;
                System.out.println("⚠️ WARNING: lastPossessor not found on either team! Defaulting to HOME");
            }
        } else {
            // No last possessor found - default to HOME
            inboundTeam = Team.HOME;
            System.out.println("⚠️ WARNING: No lastPossessor found! Defaulting to HOME");
        }


        state = State.OUT_OF_BOUNDS_THROW_WAIT;
        outOfBoundsLostTeam = inboundTeam;
        
        // Add turnover stat for player who threw it out (unless it was poked/blocked)
        if (lastPossessor != null && !wasBallPoked && !wasBallBlocked) {
            if (this.getState().equals(State.REGULATION) || this.getState().equals(State.OVERTIME)) {
                this.statsManager.getPlayerStats(lastPossessor.getUniqueId()).incrementTurnovers();
                System.out.println("Turnover stat added to " + lastPossessor.getName() + " for out of bounds");
            }
        }

        // SHOT CLOCK LOGIC - Only reset if NOT a poked ball
        if (wasBallPoked) {
            // Ball was poked - KEEP the shot clock as is (don't reset)
            System.out.println("Poked ball OOB - keeping current shot clock time: " + this.shotClockTicks + " ticks");
        } else {
            // Normal out of bounds - RESET shot clock to 24 seconds
            System.out.println("Normal OOB - resetting shot clock to 24 seconds");
            this.shotClockTicks = 480; // 24 seconds
            this.shotAttemptDetected = false;
            this.shotAttemptTeam = null;
        }

        // Determine inbound spot (sideline vs behind basket)
        Location finalInboundSpot;
        double ballZ = outOfBoundsLocation.getZ();
        double homeNetZ = getHomeNet().getCenter().getZ();
        double awayNetZ = getAwayNet().getCenter().getZ();

        if (ballZ > homeNetZ + 1) {
            finalInboundSpot = getHomeSpawn().clone().add(0, 1.2, 6);
            outOfBoundsSide = false;
            outOfBoundsZ = getHomeNet().getCenter().getBlockZ() + 2;
            outOfBoundsHome = true;
        } else if (ballZ < awayNetZ - 1) {
            finalInboundSpot = getAwaySpawn().clone().add(0, 1.2, -6);
            outOfBoundsSide = false;
            outOfBoundsZ = getAwayNet().getCenter().getBlockZ() - 2;
            outOfBoundsHome = false;
        } else {
            finalInboundSpot = outOfBoundsLocation.add(0, 1.2, 0);
            outOfBoundsSide = true;
        }

        this.inboundSpot = finalInboundSpot.clone();
        this.inboundingTeam = inboundTeam;

        this.inboundingActive = true;
        this.inboundTouchedByInbounder = false;
        this.inbounderHasReleased = false;
        this.inbounder = null;

        // Activate press prevention as soon as inbound starts
        this.pressPrevention = true;
        this.restrictedTeam = (inboundTeam == Team.HOME) ? Team.AWAY : Team.HOME;
        this.pressPreventionStartTime = System.currentTimeMillis();

        // NBA Rule: Freeze both clocks on out of bounds
        this.shotClockFrozenForInbound = true;
        this.gameClockFrozenForInbound = true;
        System.out.println("✓ Both clocks FROZEN on OOB (NBA rule)");

        // Activate immunity BEFORE spawning ball
        this.setOutOfBoundsImmunity(true);
        System.out.println("OOB immunity activated BEFORE ball spawn");

        // Spawn ball at inbound spot
        Bukkit.getScheduler().runTaskLater(Partix.getInstance(), () -> {
            Ball newBall = setBall(BallFactory.create(finalInboundSpot, BallType.BASKETBALL, this));
            newBall.setStealDelay(0);
            
            // Initialize teammate hotkeys for new ball
            if (newBall instanceof Basketball basketball) {
                basketball.initializeAllTeammateHotkeys(this);
            }
        }, 30L);

        sendTitle(Component.text("Out of Bounds: " +
                (inboundTeam == Team.HOME ? "Home Ball" : "Away Ball")).style(
                Style.style(Colour.deny(), TextDecoration.BOLD)
        ));

        // Clear poke flags AND block flags AFTER out-of-bounds is resolved
        if (ball instanceof Basketball basketball) {
            basketball.clearPokeFlags();
            basketball.clearBlockerFlag();
            System.out.println("Poke and block flags cleared after OOB resolution");
        }
    }

    private Location findClosestPointOnBoundary(Location ballLocation) {
        BoundingBox arenaBox = getArenaBox();
        double x = ballLocation.getX();
        double y = ballLocation.getY();
        double z = ballLocation.getZ();

        // Clamp X to arena boundaries
        double closestX = Math.max(arenaBox.getMin().getX(), Math.min(x, arenaBox.getMax().getX()));
        // Clamp Z to arena boundaries
        double closestZ = Math.max(arenaBox.getMin().getZ(), Math.min(z, arenaBox.getMax().getZ()));

        // Create a location with the closest point
        Location closestPoint = ballLocation.clone();
        closestPoint.setX(closestX);
        closestPoint.setZ(closestZ);

        // If the point is not on a boundary, find which boundary is closest
        if (closestPoint.distance(ballLocation) > 0.1) {
            double distToMinX = Math.abs(x - arenaBox.getMin().getX());
            double distToMaxX = Math.abs(x - arenaBox.getMax().getX());
            double distToMinZ = Math.abs(z - arenaBox.getMin().getZ());
            double distToMaxZ = Math.abs(z - arenaBox.getMax().getZ());

            // Find the minimum distance to determine which boundary is closest
            double minDist = Math.min(Math.min(distToMinX, distToMaxX), Math.min(distToMinZ, distToMaxZ));

            if (minDist == distToMinX) {
                closestPoint.setX(arenaBox.getMin().getX());
            } else if (minDist == distToMaxX) {
                closestPoint.setX(arenaBox.getMax().getX());
            } else if (minDist == distToMinZ) {
                closestPoint.setZ(arenaBox.getMin().getZ());
            } else {
                closestPoint.setZ(arenaBox.getMax().getZ());
            }
        }

        return closestPoint;
    }

    public void start() {
        this.reset();

        // For 1v1: Skip jump ball and go straight to game start
        if (this.isHalfCourt1v1) {
            this.start1v1Game();
        } else {
            // Normal game: Start with jump ball
            this.startCountdown(GoalGame.State.FACEOFF, 10);
        }
    }

    private void clearInboundState() {
        this.inboundingActive = false;
        this.inbounder = null;
        this.inboundTouchedByInbounder = false;
        this.inbounderHasReleased = false;
        this.inboundPassTime = 0L;

        if (this.inboundBarrierTask != null) {
            this.inboundBarrierTask.cancel();
            this.inboundBarrierTask = null;
        }
        if (this.inboundTimer != null) {
            this.inboundTimer.cancel();
            this.inboundTimer = null;
        }
    }

    /**
     * Clears the inbound state if the specified player is NOT the current inbounder.
     * This prevents the "can't shoot while inbounding" message from appearing for players
     * who are not actually inbounding.
     */
    public void clearInboundStateIfNotInbounder(Player newPossessor) {
        if (this.inboundingActive) {
            // If inbounder is null OR the new possessor is not the inbounder, clear the state
            if (this.inbounder == null || !this.inbounder.equals(newPossessor)) {
                System.out.println("Clearing inbound state - possession changed to " + newPossessor.getName() + 
                                   " (inbounder was: " + (this.inbounder != null ? this.inbounder.getName() : "NULL") + ")");
                this.inboundingActive = false;
                this.inbounder = null;
                this.inboundTouchedByInbounder = false;
                this.inbounderHasReleased = false;
                
                // CRITICAL FIX: Unfreeze the shot clock when possession changes during inbound
                this.shotClockFrozenForInbound = false;
                this.gameClockFrozenForInbound = false;
                System.out.println("✓ Shot clock and game clock unfrozen - possession changed during inbound");
                
                // Don't clear the timer tasks here - let onInboundPass handle that if needed
            }
        }
    }

    public void handleInboundTurnover() {
        // Clear press prevention on turnover
        this.pressPrevention = false;
        this.pressPreventionStartTime = 0L; // ✅ RESET TIMER
        this.restrictedTeam = null;
        System.out.println("Press prevention cleared - Inbound turnover detected");
    }

    public void start1v1Game() {
        if (!this.isHalfCourt1v1) {
            return;
        }

        Bukkit.getLogger().info("✓ Starting 1V1 game - skipping jump ball");

        // Remove any existing balls
        this.removeBalls();

        // ===== CLEAR BOTH NETS (don't spawn new ones) =====
        World world = this.getCenter().getWorld();
        Location h = this.getHomeNet().clone().getCenter().toLocation(world);
        h.getBlock().setType(Material.AIR);
        h.subtract(0.0, 1.0, 0.0).getBlock().setType(Material.AIR);

        Location a = this.getAwayNet().clone().getCenter().toLocation(world);
        a.getBlock().setType(Material.AIR);
        a.subtract(0.0, 1.0, 0.0).getBlock().setType(Material.AIR);

        // ===== TELEPORT PLAYERS TO STARTING POSITIONS =====
        List<Player> allPlayers = new ArrayList<>();
        allPlayers.addAll(this.getHomePlayers());
        allPlayers.addAll(this.getAwayPlayers());

        if (allPlayers.size() != 2) {
            Bukkit.getLogger().warning("ERROR: 1v1 game doesn't have exactly 2 players!");
            return;
        }

        Player homePlayer = this.getHomePlayers().get(0);
        Player awayPlayer = this.getAwayPlayers().get(0);

        // Get hoop locations
        Location homeHoopCenter = this.getHomeNet().getCenter().toLocation(world);
        Location awayHoopCenter = this.getAwayNet().getCenter().toLocation(world);
        Location centerOfCourt = this.getCenter().clone();

        // Determine direction toward each hoop
        double homeDirection = homeHoopCenter.getZ() > centerOfCourt.getZ() ? 1.0 : -1.0;
        double awayDirection = awayHoopCenter.getZ() > centerOfCourt.getZ() ? 1.0 : -1.0;

        // Home player starts at half-court, attacking toward away hoop
        Location homeStartSpawn = centerOfCourt.clone();
        homeStartSpawn.setX(homeHoopCenter.getX());
        homeStartSpawn.setZ(centerOfCourt.getZ() + (2.0 * (awayDirection > 0 ? -1.0 : 1.0)));
        homeStartSpawn.setY(world.getHighestBlockYAt((int)homeStartSpawn.getX(), (int)homeStartSpawn.getZ()) + 1.0);
        homeStartSpawn.setPitch(0);
        homeStartSpawn.setYaw(0);

        // Away player starts at 3-point line, defending
        Location awayDefenseSpawn = homeStartSpawn.clone();
        awayDefenseSpawn.setZ(homeStartSpawn.getZ() - (6.5 * (awayDirection > 0 ? -1.0 : 1.0)));
        awayDefenseSpawn.setY(world.getHighestBlockYAt((int)awayDefenseSpawn.getX(), (int)awayDefenseSpawn.getZ()) + 1.0);
        awayDefenseSpawn.setPitch(0);
        awayDefenseSpawn.setYaw(0);

        // TELEPORT IMMEDIATELY
        homePlayer.teleport(homeStartSpawn);
        awayPlayer.teleport(awayDefenseSpawn);

        Bukkit.getLogger().info("✓ Players teleported: Home at " + homeStartSpawn + ", Away at " + awayDefenseSpawn);

        // Freeze players for 2 seconds
        homePlayer.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 5, false, false));
        awayPlayer.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 5, false, false));

        // Set game state
        this.setState(GoalGame.State.REGULATION);
        this.resetShotClockTo12();

        // ===== MAKE FINAL COPY FOR LAMBDA =====
        final Player finalHomePlayer = homePlayer;

        // Spawn ball in home player's hand AFTER 2 seconds
        Bukkit.getScheduler().runTaskLater(Partix.getInstance(), () -> {
            if (!finalHomePlayer.isOnline()) {
                return;
            }
            Location ballSpawn = finalHomePlayer.getEyeLocation().clone();
            Ball newBall = this.setBall(BallFactory.create(ballSpawn, BallType.BASKETBALL, this));
            newBall.setStealDelay(0);
            newBall.setVelocity(0, 0, 0);

            // Initialize teammate hotkeys at game start
            if (newBall instanceof Basketball basketball) {
                basketball.initializeAllTeammateHotkeys(this);
            }

            this.sendTitle(Component.text("PLAY!").color(Colour.allow()).decorate(TextDecoration.BOLD));
            Bukkit.getLogger().info("✓ Ball spawned in hand");
        }, 40L);
    }


    public void generateStatsFile() {
        System.out.println("Debug: generateStatsFile() triggered.");
        String fileName = "BasketballGameStats.csv";
        String filePath = Paths.get("plugins", "GameStats", fileName).toString();
        try {
            File statsDirectory = new File("plugins/GameStats");
            if (!statsDirectory.exists()) {
                statsDirectory.mkdirs();
                System.out.println("Debug: Created stats directory.");
            }
            try (FileWriter writer = new FileWriter(filePath)) {
                writer.append("Player Name,Points,3-Pointers,Assists,Rebounds,Steals\n");
                for (UUID playerId : this.statsManager.getAllStats().keySet()) {
                    Player player = Bukkit.getPlayer(playerId);
                    if (player == null) continue;
                    PlayerStats stats = this.statsManager.getPlayerStats(playerId);
                    writer.append(player.getName()).append(",").append(String.valueOf(stats.getPoints())).append(",").append(String.valueOf(stats.getThrees())).append(",").append(String.valueOf(stats.getAssists())).append(",").append(String.valueOf(stats.getRebounds())).append(",").append(String.valueOf(stats.getSteals())).append("\n");
                }
                System.out.println("Debug: Stats saved to " + filePath);
            }
            Bukkit.getLogger().info("Game stats saved to: " + filePath);
        } catch (IOException e) {
            Bukkit.getLogger().severe("Failed to save game stats: " + e.getMessage());
        }
    }

    public boolean pass(Player player) {
        if (this.getBall() != null && this.getBall().getCurrentDamager() == player) {
            this.startAssistTimer(player.getUniqueId());
            this.sendMessage(Component.text(player.getName() + " passed the ball!").color(Colour.allow()));
            return true;
        }
        return false;
    }

    public void onOpponentTouch() {
        this.cancelAssistTimer();
    }

    public void startAssistTimer(UUID passerId) {
        this.assistEligiblePasser = passerId;
        this.assistTimerEndTime = System.currentTimeMillis() + 12000L;
        this.assistTimerActive = true;
    }

    private void checkAssistEligibility(Player scorer) {
        if (this.assistTimerActive && this.assistEligiblePasser != null && System.currentTimeMillis() <= this.assistTimerEndTime) {
            Player passer = Bukkit.getPlayer(this.assistEligiblePasser);

            // NEW: Check if this is a 1v1 game (1 player per team)
            boolean is1v1 = (this.getHomePlayers().size() == 1 && this.getAwayPlayers().size() == 1);

            // Don't record assists in 1v1 games
            if (is1v1) {
                System.out.println("❌ Assist denied: 1v1 game detected, assists disabled");
                this.assistTimerActive = false;
                this.assistEligiblePasser = null;
                return;
            }

            if (passer != null && !this.assistEligiblePasser.equals(scorer.getUniqueId()) && this.getHomePlayers().contains(scorer) == this.getHomePlayers().contains(passer)) {
                PlayerStats passerStats = this.statsManager.getPlayerStats(this.assistEligiblePasser);
                passerStats.incrementAssists();
                passer.sendMessage(Component.text("Assist!").color(Colour.allow()));
                System.out.println("✅ Assist recorded for " + passer.getName() + " (Scorer: " + scorer.getName() + ")");
            } else {
                System.out.println("❌ Assist denied: " + (passer != null ? passer.getName() : "Unknown") + " passed to themselves.");
            }
        }
        this.assistTimerActive = false;
        this.assistEligiblePasser = null;
    }

    public void cancelAssistTimer() {
        this.assistTimerActive = false;
        this.assistEligiblePasser = null;
    }

    public void handleBallInterception(Player opponent) {
        this.cancelAssistTimer();
        this.sendMessage(Component.text(opponent.getName() + " intercepted the ball!").color(Colour.deny()));
    }

    private void displayNonZeroStats() {
        this.sendMessage(Component.text("Game Stats", Colour.partix(), TextDecoration.BOLD));
        ArrayList<UUID> allPlayerIds = new ArrayList<>();
        allPlayerIds.addAll(this.getHomePlayers().stream().map(Entity::getUniqueId).toList());
        allPlayerIds.addAll(this.getAwayPlayers().stream().map(Entity::getUniqueId).toList());
        boolean hasStats = false;
        for (UUID playerId : allPlayerIds) {
            PlayerStats stats = this.statsManager.getPlayerStats(playerId);
            if (stats == null || stats.getPoints() <= 0 && stats.getThrees() <= 0 && stats.getAssists() <= 0 && stats.getRebounds() <= 0 && stats.getSteals() <= 0)
                continue;
            Player player = Bukkit.getPlayer(playerId);
            String playerName = player != null ? player.getName() : "Unknown";
            int minutesPlayedInt = (int)(stats.getMinutesPlayed() / 60000L);
            int secondsPlayedRemainder = (int)((stats.getMinutesPlayed() % 60000L) / 1000L);
            String timePlayedStr = String.format("%d:%02d", minutesPlayedInt, secondsPlayedRemainder);
            Component statsMessage = Component.text(playerName + ": ", Colour.title()).append(Component.text("Points: ", Colour.border()).append(Component.text(stats.getPoints(), Colour.allow()))).append(Component.text(", 3s: ", Colour.border()).append(Component.text(stats.getThrees(), Colour.allow()))).append(Component.text(", Assists: ", Colour.border()).append(Component.text(stats.getAssists(), Colour.allow()))).append(Component.text(", Rebounds: ", Colour.border()).append(Component.text(stats.getRebounds(), Colour.allow()))).append(Component.text(", Steals: ", Colour.border()).append(Component.text(stats.getSteals(), Colour.allow()))).append(Component.text(", Minutes: ", Colour.border()).append(Component.text(timePlayedStr, Colour.allow())));
            this.sendMessage(statsMessage);
            hasStats = true;
        }
        if (!hasStats) {
            this.sendMessage(Component.text("No player recorded stats in this game.").color(Colour.deny()));
        }
    }

    public boolean isShotActive(UUID shooterId) {
        return this.shotInProgress.containsKey(shooterId);
    }

    public void displayTeamStatsWithCopyButton() {
        HashMap<UUID, List<Double>> defenderDRValues = new HashMap<>();
        StringBuilder rawStatsBuilder = new StringBuilder();
        rawStatsBuilder.append("=== MBA Game Stats ===\n\n");
        int homePoints = 0;
        int homeAssists = 0;
        int homeRebounds = 0;
        int homeSteals = 0;
        int homeTurnovers = 0;
        int homeFGMade = 0;
        int homeFGAtt = 0;
        int home3FGMade = 0;
        int home3FGAtt = 0;
        long homePossTime = 0L;
        int homePassAttempts = 0;
        int homeFantasy = 0;
        rawStatsBuilder.append("=== ").append(Text.serialize(this.home.name)).append(" ===\n");
        this.sendMessage(Component.text("=== ").append(this.home.name).append(Component.text(" ===")).color(Colour.partix()));
        for (Player p : this.getHomePlayers()) {
            PlayerStats s;
            if (p == null || (s = this.statsManager.getPlayerStats(p.getUniqueId())) == null) continue;
            int points = s.getPoints();
            int assists = s.getAssists();
            int rebounds = s.getRebounds();
            int steals = s.getSteals();
            int turnovers = s.getTurnovers();
            long possTimeMillis = s.getPossessionTime();
            int possTimeSec = (int) (possTimeMillis / 1000L);
            long minutesPlayedMillis = s.getMinutesPlayed();
            int minutesPlayedMin = (int) (minutesPlayedMillis / 60000L);
            int minutesPlayedSec = (int) ((minutesPlayedMillis % 60000L) / 1000L);
            String minutesPlayedStr = String.format("%d:%02d", minutesPlayedMin, minutesPlayedSec);
            int passAttempts = s.getPassAttempts();
            int totalFGMadeLocal = s.getFGMade();
            int totalFGAttLocal = s.getFGAttempted();
            int threeFGMadeLocal = s.get3FGMade();
            int threeFGAttLocal = s.get3FGAttempted();
            double totalFGPct = totalFGAttLocal == 0 ? 0.0 : (double) totalFGMadeLocal / (double) totalFGAttLocal * 100.0;
            double threeFGPct = threeFGAttLocal == 0 ? 0.0 : (double) threeFGMadeLocal / (double) threeFGAttLocal * 100.0;
            double eFGPct = totalFGAttLocal == 0 ? 0.0 : ((double) totalFGMadeLocal + 0.5 * (double) threeFGMadeLocal) / (double) totalFGAttLocal * 100.0;
            double threePTRate = totalFGAttLocal == 0 ? 0.0 : (double) threeFGAttLocal / (double) totalFGAttLocal * 100.0;
            double assistsPerPass = passAttempts > 0 ? (double) assists / (double) passAttempts : 0.0;
            int fantasyPoints = points + totalFGMadeLocal * 2 - totalFGAttLocal + threeFGMadeLocal + rebounds + assists * 2 + steals * 4 - turnovers * 2;
            UUID defendedOpponent = s.getTopContestedOpponent();
            String defendedInfo = "None";
            if (defendedOpponent != null) {
                Player defendedPlayer = Bukkit.getPlayer(defendedOpponent);
                String defName = defendedPlayer != null ? defendedPlayer.getName() : defendedOpponent.toString();
                PlayerStats oppStats = this.statsManager.getPlayerStats(defendedOpponent);
                if (oppStats != null) {
                    int oppFGMade = oppStats.getFGMade();
                    int oppFGAtt = oppStats.getFGAttempted();
                    int opp3FGMade = oppStats.get3FGMade();
                    int oppPossTimeSec = (int) (oppStats.getPossessionTime() / 1000L);
                    if (oppStats.getPoints() + opp3FGMade > 0) {
                        int missedShots = oppFGAtt - oppFGMade;
                        double numerator = (double) missedShots + (double) oppPossTimeSec / 24.0;
                        double denominator = oppStats.getPoints() + opp3FGMade;
                        double defScore = 0.0;
                        if (denominator > 0.0) {
                            defScore = 100.0 * numerator / denominator;
                        }
                        defenderDRValues.computeIfAbsent(defendedOpponent, k -> new ArrayList<>()).add(defScore);
                        List<Double> scores = defenderDRValues.get(defendedOpponent);
                        double avgDR = scores.stream().mapToDouble(Double::doubleValue).average().orElse(defScore);
                        defendedInfo = String.format("%s | DR: %.0f ", defName, avgDR);
                    } else {
                        defendedInfo = defName;
                    }
                } else {
                    defendedInfo = defName;
                }
            }
            String rawLine = String.format("%s | Points: %d | Assists: %d | Rebounds: %d | Steals: %d | Turnovers: %d | Possession: %d sec | Minutes Played: %s | Defended By: %s | Pass Attempts: %d | FG: %d/%d | 3FG: %d/%d | FG%%: %.1f | 3FG%%: %.1f | eFG%%: %.1f | 3PT Rate: %.1f%% | Assists/Pass: %.1f | Fantasy Points: %d", p.getName(), points, assists, rebounds, steals, turnovers, possTimeSec, minutesPlayedStr, defendedInfo, passAttempts, totalFGMadeLocal, totalFGAttLocal, threeFGMadeLocal, threeFGAttLocal, totalFGPct, threeFGPct, eFGPct, threePTRate, assistsPerPass, fantasyPoints);
            rawStatsBuilder.append(rawLine).append("\n\n");
            Component lineComponent = Component.empty().append(Component.text(p.getName() + " | ").decorate(TextDecoration.BOLD).color(TextColor.color(43775))).append(Component.text("Points: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text("" + points).color(TextColor.color(65280))).append(Component.text(" | Assists: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text("" + assists).color(TextColor.color(65280))).append(Component.text(" | Rebounds: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text("" + rebounds).color(TextColor.color(65280))).append(Component.text(" | Steals: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text("" + steals).color(TextColor.color(65280))).append(Component.text(" | Turnovers: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text("" + turnovers).color(TextColor.color(65280))).append(Component.text(" | Possession: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text(possTimeSec + " sec").color(TextColor.color(65280))).append(Component.text(" | Minutes Played: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text(minutesPlayedStr).color(TextColor.color(65280))).append(Component.text(" | Defended By: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text(defendedInfo).color(TextColor.color(65280))).append(Component.text(" | Pass Attempts: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text("" + passAttempts).color(TextColor.color(65280))).append(Component.text(" | FG: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text(totalFGMadeLocal + "/" + totalFGAttLocal).color(TextColor.color(65280))).append(Component.text(" | 3FG: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text(threeFGMadeLocal + "/" + threeFGAttLocal).color(TextColor.color(65280))).append(Component.text(" | FG%%: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text(String.format("%.1f", totalFGPct)).color(TextColor.color(65280))).append(Component.text(" | 3FG%%: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text(String.format("%.1f", threeFGPct)).color(TextColor.color(65280))).append(Component.text(" | eFG%%: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text(String.format("%.1f", eFGPct)).color(TextColor.color(65280))).append(Component.text(" | 3PT Rate: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text(String.format("%.1f%%", threePTRate)).color(TextColor.color(65280))).append(Component.text(" | Assists/Pass: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text(String.format("%.1f", assistsPerPass)).color(TextColor.color(65280))).append(Component.text(" | Fantasy Points: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text("" + fantasyPoints).color(TextColor.color(65280)));
            this.sendMessage(lineComponent);
            homePoints += points;
            homeAssists += assists;
            homeRebounds += rebounds;
            homeSteals += steals;
            homeTurnovers += turnovers;
            homeFGMade += totalFGMadeLocal;
            homeFGAtt += totalFGAttLocal;
            home3FGMade += threeFGMadeLocal;
            home3FGAtt += threeFGAttLocal;
            homePossTime += possTimeMillis;
            homePassAttempts += passAttempts;
            homeFantasy += fantasyPoints;
        }
        int homePossSec = (int) (homePossTime / 1000L);
        double homeTotalFGPct = homeFGAtt == 0 ? 0.0 : (double) homeFGMade / (double) homeFGAtt * 100.0;
        double homeThreeFGPct = home3FGAtt == 0 ? 0.0 : (double) home3FGMade / (double) home3FGAtt * 100.0;
        double homeEFGPct = homeFGAtt == 0 ? 0.0 : ((double) homeFGMade + 0.5 * (double) home3FGMade) / (double) homeFGAtt * 100.0;
        double homeThreePTRate = homeFGAtt == 0 ? 0.0 : (double) home3FGAtt / (double) homeFGAtt * 100.0;
        double homeAssistsPerPass = homePassAttempts > 0 ? (double) homeAssists / (double) homePassAttempts : 0.0;
        String homeTotalLine = String.format("TEAM TOTALS | Points: %d | Assists: %d | Rebounds: %d | Steals: %d | Turnovers: %d | Possession: %d sec | Pass Attempts: %d | FG: %d/%d | 3FG: %d/%d | FG%%: %.1f | 3FG%%: %.1f | eFG%%: %.1f | 3PT Rate: %.1f%% | Assists/Pass: %.1f | Fantasy Points: %d", homePoints, homeAssists, homeRebounds, homeSteals, homeTurnovers, homePossSec, homePassAttempts, homeFGMade, homeFGAtt, home3FGMade, home3FGAtt, homeTotalFGPct, homeThreeFGPct, homeEFGPct, homeThreePTRate, homeAssistsPerPass, homeFantasy);
        rawStatsBuilder.append("\n").append(homeTotalLine).append("\n\n");
        this.sendMessage(Component.text(homeTotalLine).color(TextColor.color(43775)));
        int awayPoints = 0;
        int awayAssists = 0;
        int awayRebounds = 0;
        int awaySteals = 0;
        int awayTurnovers = 0;
        int awayFGMade = 0;
        int awayFGAtt = 0;
        int away3FGMade = 0;
        int away3FGAtt = 0;
        long awayPossTime = 0L;
        int awayPassAttempts = 0;
        int awayFantasy = 0;
        rawStatsBuilder.append("=== ").append(Text.serialize(this.away.name)).append(" ===\n");
        this.sendMessage(Component.text("=== ").append(this.away.name).append(Component.text(" ===")).color(Colour.partix()));
        for (Player p : this.getAwayPlayers()) {
            PlayerStats s;
            if (p == null || (s = this.statsManager.getPlayerStats(p.getUniqueId())) == null) continue;
            int points = s.getPoints();
            int assists = s.getAssists();
            int rebounds = s.getRebounds();
            int steals = s.getSteals();
            int turnovers = s.getTurnovers();
            long possTimeMillis = s.getPossessionTime();
            int possTimeSec = (int) (possTimeMillis / 1000L);
            long minutesPlayedMillis = s.getMinutesPlayed();
            int minutesPlayedMin = (int) (minutesPlayedMillis / 60000L);
            int minutesPlayedSec = (int) ((minutesPlayedMillis % 60000L) / 1000L);
            String minutesPlayedStr = String.format("%d:%02d", minutesPlayedMin, minutesPlayedSec);
            int passAttempts = s.getPassAttempts();
            int totalFGMadeLocal = s.getFGMade();
            int totalFGAttLocal = s.getFGAttempted();
            int threeFGMadeLocal = s.get3FGMade();
            int threeFGAttLocal = s.get3FGAttempted();
            double totalFGPct = totalFGAttLocal == 0 ? 0.0 : (double) totalFGMadeLocal / (double) totalFGAttLocal * 100.0;
            double threeFGPct = threeFGAttLocal == 0 ? 0.0 : (double) threeFGMadeLocal / (double) threeFGAttLocal * 100.0;
            double eFGPct = totalFGAttLocal == 0 ? 0.0 : ((double) totalFGMadeLocal + 0.5 * (double) threeFGMadeLocal) / (double) totalFGAttLocal * 100.0;
            double threePTRate = totalFGAttLocal == 0 ? 0.0 : (double) threeFGAttLocal / (double) totalFGAttLocal * 100.0;
            double assistsPerPass = passAttempts > 0 ? (double) assists / (double) passAttempts : 0.0;
            int fantasyPoints = points + totalFGMadeLocal * 2 - totalFGAttLocal + threeFGMadeLocal + rebounds + assists * 2 + steals * 4 - turnovers * 2;
            UUID defendedOpponent = s.getTopContestedOpponent();
            String defendedName = "None";
            if (defendedOpponent != null) {
                Player defendedPlayer = Bukkit.getPlayer(defendedOpponent);
                String defName = defendedPlayer != null ? defendedPlayer.getName() : defendedOpponent.toString();
                PlayerStats oppStats = this.statsManager.getPlayerStats(defendedOpponent);
                if (oppStats != null) {
                    int oppFGMade = oppStats.getFGMade();
                    int oppFGAtt = oppStats.getFGAttempted();
                    int opp3FGMade = oppStats.get3FGMade();
                    int oppPossTimeSec = (int) (oppStats.getPossessionTime() / 1000L);
                    int missedShots = oppFGAtt - oppFGMade;
                    double numerator = (double) missedShots + (double) oppPossTimeSec / 24.0;
                    double denominator = oppStats.getPoints() + opp3FGMade;
                    double defScore = 0.0;
                    if (denominator > 0.0) {
                        defScore = 100.0 * numerator / denominator;
                    }
                    defenderDRValues.computeIfAbsent(defendedOpponent, k -> new ArrayList<>()).add(defScore);
                    List<Double> scores = defenderDRValues.get(defendedOpponent);
                    double avgDR = scores.stream().mapToDouble(Double::doubleValue).average().orElse(defScore);
                    defendedName = String.format("%s | DR: %.0f ", defName, avgDR);
                } else {
                    defendedName = defName;
                }
            }
            String rawLine = String.format("%s | Points: %d | Assists: %d | Rebounds: %d | Steals: %d | Turnovers: %d | Possession: %d sec | Minutes Played: %s | Defended By: %s | Pass Attempts: %d | FG: %d/%d | 3FG: %d/%d | FG%%: %.1f | 3FG%%: %.1f | eFG%%: %.1f | 3PT Rate: %.1f%% | Assists/Pass: %.1f | Fantasy Points: %d", p.getName(), points, assists, rebounds, steals, turnovers, possTimeSec, minutesPlayedStr, defendedName, passAttempts, totalFGMadeLocal, totalFGAttLocal, threeFGMadeLocal, threeFGAttLocal, totalFGPct, threeFGPct, eFGPct, threePTRate, assistsPerPass, fantasyPoints);
            rawStatsBuilder.append(rawLine).append("\n\n");
            Component lineComponent = Component.empty().append(Component.text(p.getName() + " | ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFAA00))).append(Component.text("Points: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text("" + points).color(TextColor.color(65280))).append(Component.text(" | Assists: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text("" + assists).color(TextColor.color(65280))).append(Component.text(" | Rebounds: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text("" + rebounds).color(TextColor.color(65280))).append(Component.text(" | Steals: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text("" + steals).color(TextColor.color(65280))).append(Component.text(" | Turnovers: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text("" + turnovers).color(TextColor.color(65280))).append(Component.text(" | Possession: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text(possTimeSec + " sec").color(TextColor.color(65280))).append(Component.text(" | Minutes Played: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text(minutesPlayedStr).color(TextColor.color(65280))).append(Component.text(" | Defended By: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text(defendedName).color(TextColor.color(65280))).append(Component.text(" | Pass Attempts: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text("" + passAttempts).color(TextColor.color(65280))).append(Component.text(" | FG: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text(totalFGMadeLocal + "/" + totalFGAttLocal).color(TextColor.color(65280))).append(Component.text(" | 3FG: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text(threeFGMadeLocal + "/" + threeFGAttLocal).color(TextColor.color(65280))).append(Component.text(" | FG%%: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text(String.format("%.1f", totalFGPct)).color(TextColor.color(65280))).append(Component.text(" | 3FG%%: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text(String.format("%.1f", threeFGPct)).color(TextColor.color(65280))).append(Component.text(" | eFG%%: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text(String.format("%.1f", eFGPct)).color(TextColor.color(65280))).append(Component.text(" | 3PT Rate: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text(String.format("%.1f%%", threePTRate)).color(TextColor.color(65280))).append(Component.text(" | Assists/Pass: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text(String.format("%.1f", assistsPerPass)).color(TextColor.color(65280))).append(Component.text(" | Fantasy Points: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text("" + fantasyPoints).color(TextColor.color(65280)));
            this.sendMessage(lineComponent);
            awayPoints += points;
            awayAssists += assists;
            awayRebounds += rebounds;
            awaySteals += steals;
            awayTurnovers += turnovers;
            awayFGMade += totalFGMadeLocal;
            awayFGAtt += totalFGAttLocal;
            away3FGMade += threeFGMadeLocal;
            away3FGAtt += threeFGAttLocal;
            awayPossTime += possTimeMillis;
            awayPassAttempts += passAttempts;
            awayFantasy += fantasyPoints;
        }
        int awayPossSec = (int) (awayPossTime / 1000L);
        double awayTotalFGPct = awayFGAtt == 0 ? 0.0 : (double) awayFGMade / (double) awayFGAtt * 100.0;
        double awayThreeFGPct = away3FGAtt == 0 ? 0.0 : (double) away3FGMade / (double) away3FGAtt * 100.0;
        double awayEFGPct = awayFGAtt == 0 ? 0.0 : ((double) awayFGMade + 0.5 * (double) away3FGMade) / (double) awayFGAtt * 100.0;
        double awayThreePTRate = awayFGAtt == 0 ? 0.0 : (double) away3FGAtt / (double) awayFGAtt * 100.0;
        double awayAssistsPerPass = awayPassAttempts > 0 ? (double) awayAssists / (double) awayPassAttempts : 0.0;
        String awayTotalLine = String.format("TEAM TOTALS | Points: %d | Assists: %d | Rebounds: %d | Steals: %d | Turnovers: %d | Possession: %d sec | Pass Attempts: %d | FG: %d/%d | 3FG: %d/%d | FG%%: %.1f | 3FG%%: %.1f | eFG%%: %.1f | 3PT Rate: %.1f%% | Assists/Pass: %.1f | Fantasy Points: %d", awayPoints, awayAssists, awayRebounds, awaySteals, awayTurnovers, awayPossSec, awayPassAttempts, awayFGMade, awayFGAtt, away3FGMade, away3FGAtt, awayTotalFGPct, awayThreeFGPct, awayEFGPct, awayThreePTRate, awayAssistsPerPass, awayFantasy);
        rawStatsBuilder.append("\n").append(awayTotalLine).append("\n\n");
        this.sendMessage(Component.text(awayTotalLine).color(TextColor.color(0xFFAA00)));
        Component copyButton = Component.text("[Click to Copy Stats]").color(Colour.allow()).clickEvent(ClickEvent.copyToClipboard(rawStatsBuilder.toString())).hoverEvent(Component.text("Click to copy all final stats"));
        for (Player player : this.getPlayers()) {
            player.sendMessage(copyButton);
        }
    }

    public int getHomeScore() {
        return this.homeScore;
    }

    public int getAwayScore() {
        return this.awayScore;
    }

    private int parseColor(String colorName) {
        String c;
        return switch (c = colorName.toUpperCase()) {
            case "RED" -> 0xFF0000;
            case "BLUE" -> 255;
            case "GREEN" -> 65280;
            case "YELLOW" -> 0xFFFF00;
            case "BLACK" -> 0;
            default -> 0xFFFFFF;
        };
    }

    public void setTeamName(GoalGame.Team team, String name) {
        if (team == GoalGame.Team.HOME) {
            this.home.name = Component.text(name);
        } else if (team == GoalGame.Team.AWAY) {
            this.away.name = Component.text(name);
        }
    }

    public void setTeamJerseys(GoalGame.Team team, String chestplateColor, String leggingsColor, String bootsColor) {
        block3:
        {
            block2:
            {
                if (team != GoalGame.Team.HOME) break block2;
                this.home.chest = Items.armor(Material.LEATHER_CHESTPLATE, this.parseColor(chestplateColor), "Jersey", "Your team jersey");
                this.home.pants = Items.armor(Material.LEATHER_LEGGINGS, this.parseColor(leggingsColor), "Pants", "Your team pants");
                this.home.boots = Items.armor(Material.LEATHER_BOOTS, this.parseColor(bootsColor), "Boots", "Your team boots");
                for (Player p : this.getHomePlayers()) {
                    p.getInventory().setChestplate(this.home.chest);
                    p.getInventory().setLeggings(this.home.pants);
                    p.getInventory().setBoots(this.home.boots);
                }
                break block3;
            }
            if (team != GoalGame.Team.AWAY) break block3;
            this.away.chest = Items.armor(Material.LEATHER_CHESTPLATE, 0xFFFFFF, "Jersey", "Your team away jersey");
            this.away.pants = Items.armor(Material.LEATHER_LEGGINGS, this.parseColor(leggingsColor), "Pants", "Your team pants");
            this.away.boots = Items.armor(Material.LEATHER_BOOTS, this.parseColor(bootsColor), "Boots", "Your team boots");
            for (Player p : this.getAwayPlayers()) {
                p.getInventory().setChestplate(this.away.chest);
                p.getInventory().setLeggings(this.away.pants);
                p.getInventory().setBoots(this.away.boots);
            }
        }
    }

    public void updateHomeTeamDisplay(String teamName, String chestColor, String leggingsColor, String bootsColor) {
    }

    public void updateAwayTeamDisplay(String teamName, String chestColor, String leggingsColor, String bootsColor) {
    }

    public void openTeamManagementGUI(Player p) {
        if (this.isInMBAStadium()) {
            if (!p.hasPermission("rank.coach")) {
                p.sendMessage(Component.text("§c§l⚠ MBA Arena Restriction"));
                p.sendMessage(Component.text("§cYou need §e§lCoach §crank to access game settings!"));
                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, SoundCategory.MASTER, 1.0f, 1.0f);
                return;
            }
        }
        GUI gui = new GUI("Team Management", 3, false);
        gui.addButton(new ItemButton(10, Items.get(Component.text("Home – Timeout").color(Colour.deny()), Material.RED_DYE), b -> {
            this.homeTimeouts = Math.max(0, this.homeTimeouts - 1);
            b.sendMessage(Component.text("Home timeouts: " + this.homeTimeouts).color(Colour.deny()));
            this.openTeamManagementGUI(b);
        }));
        gui.addButton(new ItemButton(11, Items.get(Component.text("Home ＋ Timeout").color(Colour.allow()), Material.LIME_DYE), b -> {
            this.homeTimeouts = Math.min(4, this.homeTimeouts + 1);
            b.sendMessage(Component.text("Home timeouts: " + this.homeTimeouts).color(Colour.allow()));
            this.openTeamManagementGUI(b);
        }));
        gui.addButton(new ItemButton(12, Items.get(Component.text("Away – Timeout").color(Colour.deny()), Material.RED_DYE), b -> {
            this.awayTimeouts = Math.max(0, this.awayTimeouts - 1);
            b.sendMessage(Component.text("Away timeouts: " + this.awayTimeouts).color(Colour.deny()));
            this.openTeamManagementGUI(b);
        }));
        gui.addButton(new ItemButton(13, Items.get(Component.text("Away ＋ Timeout").color(Colour.allow()), Material.LIME_DYE), b -> {
            this.awayTimeouts = Math.min(4, this.awayTimeouts + 1);
            b.sendMessage(Component.text("Away timeouts: " + this.awayTimeouts).color(Colour.allow()));
            this.openTeamManagementGUI(b);
        }));
        gui.addButton(new ItemButton(21, Items.get(Component.text("Skip to 10s").color(Colour.partix()), Material.CLOCK), b -> {
            if (this.timeoutTask != null && this.getState().equals(State.STOPPAGE)) {
                this.timeoutSecs = 10;
                this.timeoutBar.setTitle("Timeout: 10s");
                this.timeoutBar.setProgress(0.16666666666666666);
                this.skipTimeoutToTen();
                b.sendMessage(Component.text("⚡ Skipped timeout to 10 seconds").color(Colour.allow()));
            } else {
                b.sendMessage(Component.text("No timeout in progress to skip").color(Colour.deny()));
            }
            this.openTeamManagementGUI(b);
        }));
        gui.addButton(new ItemButton(14, Items.get(Component.text("Inbound ▶ Home").color(Colour.partix()), Material.ARROW), b -> {
            this.endTimeout(GoalGame.Team.HOME);
            this.openTeamManagementGUI(b);
        }));
        gui.addButton(new ItemButton(15, Items.get(Component.text("Inbound ▶ Away").color(Colour.partix()), Material.ARROW), b -> {
            this.endTimeout(GoalGame.Team.AWAY);
            this.openTeamManagementGUI(b);
        }));
        gui.addButton(new ItemButton(16, Items.get(Component.text("Cancel Inbound").color(Colour.deny()), Material.BARRIER), b -> {
            this.inboundingActive = false;
            b.sendMessage(Component.text("Inbound sequence cancelled").color(Colour.deny()));
            this.openTeamManagementGUI(b);
        }));
        gui.addButton(new ItemButton(22, Items.get(Component.text("Reset Timeouts").color(Colour.partix()), Material.PAPER), b -> {
            this.awayTimeouts = 4;
            this.homeTimeouts = 4;
            b.sendMessage(Component.text("All timeouts reset to 4").color(Colour.allow()));
            this.openTeamManagementGUI(b);
        }));
        gui.openInventory(p);
    }
}

