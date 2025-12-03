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
import me.x_tias.partix.Partix;
import me.x_tias.partix.database.BasketballDb;
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

public class BasketballGame
        extends GoalGame {
    private final PlayerStatsManager statsManager = new PlayerStatsManager();
    private final Map<UUID, Boolean> shotInProgress = new HashMap<>();
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
    private Location stoppageLastLocation;
    @Getter
    private int shotClockTicks = 480;
    private BukkitTask timeoutTask = null;
    private int timeoutSecs = 0;
    private final boolean shotClockFrozen = false;
    private boolean shotAttemptDetected = false;
    private boolean shotClockStopped = false;
    private boolean inboundTouchedByInbounder = false;
    private boolean inbounderHasReleased = false;
    private GoalGame.Team inboundingTeam;
    private Location inboundSpot;
    private BukkitTask inboundBarrierTask;
    private BukkitTask inboundTimer;
    private Ball inboundBall;
    private GoalGame.Team shotAttemptTeam = null;
    private GoalGame.Team lastPossessionTeam = null;
    private boolean shotClockEnabled = true;
    private boolean buzzerPlayed = false;
    private UUID currentPossessor = null;
    private long possessionStartTime = 0L;
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

    public BasketballGame(Settings settings, Location location, double xDistance, double yDistance, double xLength, double zWidth, double yHeight) {
        this.setup(settings.copy(), location, xDistance, yDistance, xLength, zWidth, yHeight);
        this.courtLength = xDistance;
        this.location = location.clone();

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
        for (Player p : this.getHomePlayers()) {
            p.sendMessage(Component.text("Your stats have been reset.").color(Colour.allow()));
        }
        for (Player p : this.getAwayPlayers()) {
            p.sendMessage(Component.text("Your stats have been reset.").color(Colour.allow()));
        }
    }

    public void resetAllStats() {
        System.out.println("Debug: Resetting all player stats.");
        this.statsManager.resetStats();
    }

    @Override
    public void resetShotClock() {
        this.shotClockTicks = 480;
        this.shotClockStopped = false;
        this.shotAttemptDetected = false;
        this.shotAttemptTeam = null;
        this.lastPossessionTeam = null;
    }

    public void callTimeout(final GoalGame.Team callingTeam) {
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

    public void skipTimeoutToTen() {
        if (this.timeoutTask == null) {
            this.getPlayers().forEach(p -> p.sendMessage(Component.text("No timeout in progress to skip").color(Colour.deny())));
            return;
        }
        this.timeoutSecs = 10;
        this.timeoutBar.setTitle("Timeout: 10s");
        this.timeoutBar.setProgress(0.16666666666666666);
    }

    private void enforceHalfCourtBarrier() {
        double centerX = this.getCenter().getX();
        List<Player> opponents = this.inboundingTeam == GoalGame.Team.HOME ? this.getAwayPlayers() : this.getHomePlayers();
        for (Player p : opponents) {
            double x = p.getLocation().getX();
            if (!(this.inboundingTeam == Team.HOME ? x < centerX : x > centerX)) continue;
            double pushX = this.inboundingTeam == GoalGame.Team.HOME ? 0.5 : -0.5;
            p.setVelocity(new Vector(pushX, 0.0, 0.0));
        }
    }

    public void endTimeout(final GoalGame.Team callingTeam) {
        this.inboundingActive = true;
        this.inboundingTeam = callingTeam;
        this.inboundTouchedByInbounder = false;
        this.inbounderHasReleased = false;
        final Location center = this.getCenter();
        final double centerX = center.getX();
        final double centerZ = center.getZ();
        double xOffset = callingTeam == GoalGame.Team.HOME ? -2.0 : 2.0;
        double zOffset = callingTeam == GoalGame.Team.HOME ? -20.0 : 20.0;
        this.inboundSpot = center.clone().add(xOffset, 0.0, zOffset);
        List<Player> candidates = callingTeam == GoalGame.Team.HOME ? this.getHomePlayers() : this.getAwayPlayers();
        this.inbounder = candidates.stream().min(Comparator.comparingDouble(p -> p.getLocation().distance(this.inboundSpot))).orElse(null);
        if (this.inbounder == null) {
            this.inboundingActive = false;
            return;
        }
        Location spawnLoc = this.inboundSpot.clone();
        if (callingTeam == GoalGame.Team.AWAY) {
            spawnLoc.setYaw(180.0f);
        } else {
            spawnLoc.setYaw(0.0f);
        }
        this.inbounder.teleport(spawnLoc);
        for (int slot = 0; slot < this.inbounder.getInventory().getSize(); ++slot) {
            ItemStack it = this.inbounder.getInventory().getItem(slot);
            if (it == null || it.getType() != Material.POLISHED_BLACKSTONE_BUTTON) continue;
            this.inbounder.getInventory().setItem(slot, null);
            break;
        }
        this.inbounder.updateInventory();
        this.inboundBall = this.setBall(BallFactory.create(this.inboundSpot.clone().add(0.0, 1.0, 0.0), this.getBallType(), this));
        this.inboundBall.setVelocity(0.0, 0.0, 0.0);
        System.out.println("SET VELOCITY 1");
        this.setState(GoalGame.State.STOPPAGE);
        this.shotClockStopped = true;
        this.inboundBarrierTask = new BukkitRunnable() {

            public void run() {
                if (!BasketballGame.this.inboundingActive || BasketballGame.this.getState() != GoalGame.State.STOPPAGE || BasketballGame.this.inbounder == null || !BasketballGame.this.inbounder.isOnline()) {
                    this.cancel();
                    BasketballGame.this.inboundBarrierTask = null;
                    return;
                }
                World w = center.getWorld();
                for (double z = centerZ - 30.0; z <= centerZ + 30.0; z += 1.0) {
                    w.spawnParticle(Particle.DUST, centerX, center.getY() + 0.1, z, 1, (Object) new Particle.DustOptions(Color.RED, 1.0f));
                }
                List<Player> opponents = callingTeam == GoalGame.Team.HOME ? BasketballGame.this.getAwayPlayers() : BasketballGame.this.getHomePlayers();
                for (Player opp : opponents) {
                    Vector push;
                    boolean crossed;
                    double x = opp.getLocation().getX();
                    if (callingTeam == GoalGame.Team.HOME) {
                        crossed = x > centerX;
                        push = new Vector(-0.5, 0.0, 0.0);
                    } else {
                        crossed = x < centerX;
                        push = new Vector(0.5, 0.0, 0.0);
                    }
                    if (!crossed) continue;
                    opp.setVelocity(push);
                }
            }
        }.runTaskTimer(Partix.getInstance(), 1L, 1L);
        this.inboundTimer = new BukkitRunnable() {
            int secs = 7;

            public void run() {
                Player holder;
                if (!BasketballGame.this.inboundingActive) {
                    this.cancel();
                    BasketballGame.this.inboundTimer = null;
                    return;
                }
                Player player = holder = BasketballGame.this.getBall() != null ? BasketballGame.this.getBall().getCurrentDamager() : null;
                if (!BasketballGame.this.inboundTouchedByInbounder) {
                    if (holder != null && holder.equals(BasketballGame.this.inbounder)) {
                        BasketballGame.this.inboundTouchedByInbounder = true;
                    }
                    return;
                }
                if (!BasketballGame.this.inbounderHasReleased) {
                    if (holder != null && holder.equals(BasketballGame.this.inbounder)) {
                        if (--this.secs > 0 && this.secs <= 5) {
                            for (Player p : BasketballGame.this.getPlayers()) {
                                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.MASTER, 1.0f, 1.0f);
                            }
                        }
                        if (this.secs <= 0) {
                            BasketballGame.this.cancelInboundSequence();
                            BasketballGame.this.inboundViolation(callingTeam);
                            this.cancel();
                            BasketballGame.this.inboundTimer = null;
                        }
                        return;
                    }
                    if (holder == null) {
                        BasketballGame.this.inbounderHasReleased = true;
                    }
                }
                if (BasketballGame.this.inbounderHasReleased) {
                    if (holder != null && holder.equals(BasketballGame.this.inbounder)) {
                        BasketballGame.this.cancelInboundSequence();
                        BasketballGame.this.inboundViolation(callingTeam);
                        this.cancel();
                        BasketballGame.this.inboundTimer = null;
                        return;
                    }
                    if (holder != null && !holder.equals(BasketballGame.this.inbounder)) {
                        BasketballGame.this.resumePlay();
                        this.cancel();
                        BasketballGame.this.inboundTimer = null;
                        return;
                    }
                }
            }
        }.runTaskTimer(Partix.getInstance(), 20L, 20L);
    }

    public void dropInboundBarrierButKeepClockFrozen() {
        if (this.inboundBarrierTask != null) {
            this.inboundBarrierTask.cancel();
            this.inboundBarrierTask = null;
        }
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

    private void enforceNoProximity() {
        if (this.inbounder == null) {
            return;
        }
        World w = this.inboundSpot.getWorld();
        for (int deg = 0; deg < 360; deg += 10) {
            double rad = Math.toRadians(deg);
            double x = this.inboundSpot.getX() + Math.cos(rad) * 3.0;
            double z = this.inboundSpot.getZ() + Math.sin(rad) * 3.0;
            w.spawnParticle(Particle.DUST, x, this.inboundSpot.getY() + 0.1, z, 1, (Object) new Particle.DustOptions(Color.RED, 1.0f));
        }
        for (Player p : this.getPlayers()) {
            Location loc;
            if (p.equals(this.inbounder) || !((loc = p.getLocation()).distance(this.inboundSpot) < 3.0)) continue;
            Vector dir = loc.toVector().subtract(this.inboundSpot.toVector()).setY(0).normalize();
            p.setVelocity(dir.multiply(0.5));
        }
    }

    public void inboundViolation(GoalGame.Team callingTeam) {
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
        new BukkitRunnable() {

            public void run() {
                BasketballGame.this.endTimeout(next);
            }
        }.runTaskLater(Partix.getInstance(), 100L);
    }

    private void resumePlay() {
        this.inboundingActive = false;
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
        if (this.lastPossessionTeam == GoalGame.Team.HOME) {
            Vector pushVelocity = new Vector(1.25, -1.5, 0.0);
            this.getHomePlayers().stream().filter(p -> p.getLocation().getX() < this.getCenter().getX()).forEach(p -> {
                p.teleport(p.getLocation().clone().set(this.getCenter().getX(), p.getLocation().getY(), p.getLocation().getZ()));
                p.setVelocity(pushVelocity);
            });
            Ball newBall = this.setBall(BallFactory.create(this.getAwaySpawn(), this.getBallType(), this));
            newBall.setVelocity(0.05, 0.05, 0.0);
            System.out.println("SET VELOCITY 2");
        } else if (this.lastPossessionTeam == GoalGame.Team.AWAY) {
            Vector pushVelocity = new Vector(-1.25, -1.5, 0.0);
            this.getAwayPlayers().stream().filter(p -> p.getLocation().getX() > this.getCenter().getX()).forEach(p -> {
                p.teleport(p.getLocation().clone().set(this.getCenter().getX(), p.getLocation().getY(), p.getLocation().getZ()));
                p.setVelocity(pushVelocity);
            });
            Ball newBall = this.setBall(BallFactory.create(this.getHomeSpawn(), this.getBallType(), this));
            newBall.setVelocity(-0.05, 0.05, 0.0);
            System.out.println("SET VELOCITY 3");
        }
        this.lastPossessionTeam = null;
        this.resetShotClock();
    }

    private void stopPlayDueToShotClockViolation() {
        this.setState(GoalGame.State.STOPPAGE);
        this.removeBalls();
        this.startCountdown(GoalGame.State.FACEOFF, 10);
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
        this.shotClockTicks = 240;
        this.buzzerPlayed = false;
    }

    @Override
    public void onTick() {
        super.onTick();
        this.tickArea();
        if (this.inboundingActive) {
            Player holder;
            Player player = holder = this.getBall() != null ? this.getBall().getCurrentDamager() : null;
            if (!this.inboundTouchedByInbounder) {
                if (holder != null && holder.equals(this.inbounder)) {
                    this.inboundTouchedByInbounder = true;
                }
                this.enforceInboundBounds();
                this.enforceNoProximity();
                return;
            }
            if (this.inboundTouchedByInbounder && holder == null) {
                return;
            }
            if (holder != null && !holder.equals(this.inbounder)) {
                this.resumePlay();
                if (this.inboundTimer != null) {
                    this.inboundTimer.cancel();
                    this.inboundTimer = null;
                }
                return;
            }
            this.enforceInboundBounds();
            this.enforceNoProximity();
            return;
        }
        this.updateShotClock();
        this.updateActionBarShotClock();
        this.updatePossessionTime();
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

    private BoundingBox getJumpBox(Location area) {
        return new BoundingBox(
                area.x() - 5, area.y() - 20, area.z() + 5,
                area.x() + 5, area.y() + 20, area.z() - 5
        );
    }

    private void updateShotClock() {
        if (!this.shotClockEnabled || this.getState().equals(State.STOPPAGE) || this.getState().equals(State.OUT_OF_BOUNDS_THROW) || this.getState().equals(State.OUT_OF_BOUNDS_THROW_WAIT)) {
            return;
        }
        int gameTimeLeft = this.getTimeTicks();
        if (this.shotClockTicks > gameTimeLeft) {
            return;
        }
        Player possessor = this.getBall() != null ? this.getBall().getCurrentDamager() : null;
        if (possessor == null && this.lastPossessionTeam == null) {
            return;
        }

        GoalGame.Team currentTeam = possessor != null ? this.getTeamOf(possessor) : this.lastPossessionTeam;

        if (this.lastPossessionTeam != null && currentTeam != this.lastPossessionTeam) {
            this.shotClockTicks = 480;
            System.out.println("Resetting shot clock");
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
            if (secondsRemaining <= 5.0 && this.shotClockTicks % 20 == 0) {
                for (Player p : this.getPlayers()) {
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.MASTER, 1.0f, 1.0f);
                }
            }
        } else {
            this.shotClockTicks = 0;
            if (!this.buzzerPlayed) {
                for (Player p : this.getPlayers()) {
                    p.playSound(p.getLocation(), "shotclockbuzzer", SoundCategory.MASTER, 1.0f, 1.0f);
                }
                this.buzzerPlayed = true;
            }
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

    @Override
    public void setPregame() {
        World world = this.getCenter().getWorld();
        BallFactory.create(this.getHomeSpawn().clone().add(0.0, 0.0, -3.0), this.getBallType(), this);
        BallFactory.create(this.getHomeSpawn().clone().add(0.0, 0.0, -1.5), this.getBallType(), this);
        BallFactory.create(this.getHomeSpawn().clone().add(0.0, 0.0, 3.0), this.getBallType(), this);
        BallFactory.create(this.getAwaySpawn().clone().add(0.0, 0.0, -3.0), this.getBallType(), this);
        BallFactory.create(this.getAwaySpawn().clone().add(0.0, 0.0, -1.5), this.getBallType(), this);
        BallFactory.create(this.getAwaySpawn().clone().add(0.0, 0.0, 3.0), this.getBallType(), this);
        Block h = this.getHomeNet().clone().getCenter().toLocation(world).getBlock();
        h.getLocation().clone().getBlock().setType(Material.AIR);
        h.getLocation().clone().subtract(0.0, 1.0, 0.0).getBlock().setType(Material.AIR);
        Block a = this.getAwayNet().clone().getCenter().toLocation(world).getBlock();
        a.getLocation().clone().getBlock().setType(Material.AIR);
        a.getLocation().clone().subtract(0.0, 1.0, 0.0).getBlock().setType(Material.AIR);
        this.resetShotClock();
    }

    @Override
    public void setFaceoff() {
        World world = this.getCenter().getWorld();
        this.removeBalls();
        Location h = this.getHomeNet().clone().getCenter().toLocation(world);
        h.getBlock().setType(Material.AIR);
        h.subtract(0.0, 1.0, 0.0).getBlock().setType(Material.BARRIER);
        Location a = this.getAwayNet().clone().getCenter().toLocation(world);
        a.getBlock().setType(Material.AIR);
        a.subtract(0.0, 1.0, 0.0).getBlock().setType(Material.BARRIER);
    }

    @Override
    public void dropBall() {
        Location spawn = this.getCenter().add(0.0, 1.5 + Math.random() / 1.5, 0.0);
        Ball ball = this.setBall(BallFactory.create(spawn, BallType.BASKETBALL, this));
        ball.setVelocity(0.0, 0.1 + Math.random() / 3.0, new Random().nextBoolean() ? Math.max(0.05 + (0.05 + Math.random()) / 25.0, 0.05) / 3.0 : Math.min(-0.05 + (-0.5 - Math.random()) / 25.0, -0.05) / 3.0);
        System.out.println("SET VELOCITY 4");
    }

    @Override
    public boolean periodIsComplete(int ticksRemaining) {
        if (this.getBall() != null && this.getBall().getCurrentDamager() == null && this.getBall().getLocation().getY() > this.getCenter().getY() + 2.0) {
            this.addTime(1);
            return false;
        }
        this.resetShotClock();
        return true;
    }

    @Override
    public void gameOver(GoalGame.Team winner) {
        this.sendTitle(Component.text("The ").color(Colour.partix()).append(winner.equals(Team.HOME) ? this.home.name : this.away.name).append(Component.text(" Win!").color(Colour.bold())));
        if (this.settings.compType.equals(CompType.RANKED) && (this.getHomePlayers().size() > 1 || this.getAwayPlayers().size() > 1)) {
            if (winner.equals(Team.HOME)) {
                this.getHomePlayers().forEach(uuid -> {
                    BasketballDb.add(uuid.getUniqueId(), BasketballDb.Stat.WINS, 1);
                    SeasonDb.get(uuid.getUniqueId(), SeasonDb.Stat.WINS).thenAccept(wins -> SeasonDb.set(uuid.getUniqueId(), SeasonDb.Stat.WINS, wins + 1));
                    SeasonDb.get(uuid.getUniqueId(), SeasonDb.Stat.POINTS).thenAccept(points -> SeasonDb.set(uuid.getUniqueId(), SeasonDb.Stat.POINTS, points + 2));
                    AthleteManager.get(uuid.getUniqueId()).giveCoins(10, true);
                });
                this.getAwayPlayers().forEach(uuid -> {
                    BasketballDb.add(uuid.getUniqueId(), BasketballDb.Stat.LOSSES, 1);
                    SeasonDb.get(uuid.getUniqueId(), SeasonDb.Stat.LOSSES).thenAccept(losses -> SeasonDb.set(uuid.getUniqueId(), SeasonDb.Stat.LOSSES, losses + 1));
                    SeasonDb.get(uuid.getUniqueId(), SeasonDb.Stat.POINTS).thenAccept(points -> {
                        int newPoints = Math.max(0, points - 1);
                        SeasonDb.set(uuid.getUniqueId(), SeasonDb.Stat.POINTS, newPoints);
                    });
                    AthleteManager.get(uuid.getUniqueId()).giveCoins(5, true);
                });
            } else {
                this.getAwayPlayers().forEach(uuid -> {
                    BasketballDb.add(uuid.getUniqueId(), BasketballDb.Stat.WINS, 1);
                    SeasonDb.get(uuid.getUniqueId(), SeasonDb.Stat.WINS).thenAccept(wins -> SeasonDb.set(uuid.getUniqueId(), SeasonDb.Stat.WINS, wins + 1));
                    SeasonDb.get(uuid.getUniqueId(), SeasonDb.Stat.POINTS).thenAccept(points -> SeasonDb.set(uuid.getUniqueId(), SeasonDb.Stat.POINTS, points + 2));
                    AthleteManager.get(uuid.getUniqueId()).giveCoins(10, true);
                });
                this.getHomePlayers().forEach(uuid -> {
                    BasketballDb.add(uuid.getUniqueId(), BasketballDb.Stat.LOSSES, 1);
                    SeasonDb.get(uuid.getUniqueId(), SeasonDb.Stat.LOSSES).thenAccept(losses -> SeasonDb.set(uuid.getUniqueId(), SeasonDb.Stat.LOSSES, losses + 1));
                    SeasonDb.get(uuid.getUniqueId(), SeasonDb.Stat.POINTS).thenAccept(points -> {
                        int newPoints = Math.max(0, points - 1);
                        SeasonDb.set(uuid.getUniqueId(), SeasonDb.Stat.POINTS, newPoints);
                    });
                    AthleteManager.get(uuid.getUniqueId()).giveCoins(5, true);
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
            homeTO.append(i < this.homeTimeouts ? "§6§l⬤" : "§8◯");
            awayTO.append(i < this.awayTimeouts ? "§6§l⬤" : "§8◯");
        }
        homeTO.append("§7] §0");
        awayTO.append("§7] §1");
        Sidebar.set(this.getPlayers(), Component.text("⛹ MBA Basketball", Colour.partix(), TextDecoration.BOLD), "", "", "§6§lMatch Info", "  " + time, "  §f" + (this.settings.winType.timed ? this.getShortPeriodString() : "---"), "", "§6§lScoreboard", "  §bHome: §e" + this.homeScore + " §7(" + Text.serialize(this.home.abrv) + ")", homeTO.toString(), "  §dAway: §e" + this.awayScore + " §7(" + Text.serialize(this.away.abrv) + ")", awayTO.toString());
        this.updateBossBar("§r§f§l" + Text.serialize(this.away.name) + " §7§l" + this.awayScore + " §r§e@ §7§l" + this.homeScore + " §r§f§l" + Text.serialize(this.home.name), Math.min(1.0, Math.max(0.0, (double) this.getTimeTicks() / (double) (this.settings.winType.amount * 60 * 20))));
    }

    @Override
    public void goal(GoalGame.Team team) {
        Ball ball = this.getBall();
        if (ball instanceof Basketball ball2) {
            if (ball2.getVelocity().getY() < 0.01) {
                int score;
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
                    if (isThree) {
                        CosmeticSound greenSound;
                        Athlete athlete = AthleteManager.get(scorerId);
                        CosmeticSound cosmeticSound = greenSound = athlete != null ? athlete.getGreenSound() : CosmeticSound.NO_SOUND;
                        if (greenSound != CosmeticSound.NO_SOUND && greenSound.getSoundIdentifier() != null && !greenSound.getSoundIdentifier().isEmpty()) {
                            Bukkit.getLogger().info("[DEBUG] Playing Green Sound for player: " + scorer.getName() + ", Sound: " + greenSound.getSoundIdentifier());
                            scorer.getWorld().playSound(scorer.getLocation(), greenSound.getSoundIdentifier(), SoundCategory.PLAYERS, 3.5f, 1.0f);
                        } else {
                            Bukkit.getLogger().warning("No valid Green Sound for player: " + scorer.getName());
                        }
                    }
                    stats.addPoints(isThree ? 3 : 2);
                    this.sendMessage(Component.text(scorer.getName() + "'s current stats: ").color(Colour.title()).append(Component.text("Points: ", Colour.border())).append(Component.text(stats.getPoints(), Colour.allow())).append(Component.text(", 3s: ", Colour.border())).append(Component.text(stats.getThrees(), Colour.allow())).append(Component.text(", Assists: ", Colour.border())).append(Component.text(stats.getAssists(), Colour.allow())).append(Component.text(", Rebounds: ", Colour.border())).append(Component.text(stats.getRebounds(), Colour.allow())).append(Component.text(", Steals: ", Colour.border())).append(Component.text(stats.getSteals(), Colour.allow())));
                }
                ball2.setReboundEligible(false);
                if (this.settings.gameType.equals(GameType.AUTOMATIC)) {
                    List<Player> players;
                    List<Player> list = players = team.equals(Team.HOME) ? this.getHomePlayers() : this.getAwayPlayers();
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
                int n = score = team.equals(Team.HOME) ? this.homeScore : this.awayScore;
                if (this.getState().equals(State.REGULATION) || !this.settings.suddenDeath || !this.settings.winType.timed && score + (isThree ? 3 : 2) >= this.settings.winType.amount) {
                    this.sendTitle(t.name.append(Component.text(isThree ? " ‣ 3 Points!" : " ‣ 2 Points").color(Colour.partix())));
                    if (scorer != null) {
                        AthleteManager.get(scorer.getUniqueId()).getExplosion().mediumExplosion(ball2.getLocation());
                    }
                    if (team.equals(Team.HOME)) {
                        this.homeScore += isThree ? 3 : 2;
                        Vector v = new Vector(1.25, -1.5, 0.0);
                        this.getHomePlayers().stream().filter(player -> player.getLocation().getX() < this.getCenter().getX()).forEach(player -> {
                            player.teleport(player.getLocation().clone().set(this.getCenter().clone().getX(), player.getLocation().getY(), player.getLocation().getZ()));
                            player.setVelocity(v);
                        });
                        ball2.setLocation(this.getAwaySpawn().add(0, 1.2, -6));
                        ball2.setVelocity(0, 0.05, 0.0);
                    } else {
                        this.awayScore += isThree ? 3 : 2;
                        Vector v = new Vector(-1.25, -1.5, 0.0);
                        this.getAwayPlayers().stream().filter(player -> player.getLocation().getX() > this.getCenter().getX()).forEach(player -> {
                            player.teleport(player.getLocation().clone().set(this.getCenter().clone().getX(), player.getLocation().getY(), player.getLocation().getZ()));
                            player.setVelocity(v);
                        });
                        ball2.setLocation(this.getHomeSpawn().clone().add(0, 1.2, 6));
                        ball2.setVelocity(0, 0.05, 0.0);
                    }
                } else {
                    if (team.equals(Team.HOME)) {
                        this.homeScore += isThree ? 3 : 2;
                    } else {
                        this.awayScore += isThree ? 3 : 2;
                    }
                    this.gameOver(team);
                    this.sendTitle(Component.text("The ").color(Colour.partix()).append(t.name).append(Component.text(" Win!").color(Colour.partix())));
                    this.removeBalls();
                }
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
            player.getInventory().setLeggings(this.home.pants);
            player.getInventory().setBoots(this.home.boots);
            player.getInventory().setItem(6, Items.get(Component.text("Your Bench").color(Colour.partix()), Material.OAK_STAIRS));
            player.getInventory().setItem(7, Items.get(Component.text("Game Settings").color(Colour.partix()), Material.CHEST));
            player.getInventory().setItem(8, Items.get(Component.text("Change Team/Leave Game").color(Colour.partix()), Material.GRAY_DYE));
            if (this.getHomePlayers().size() < this.settings.playersPerTeam) {
                player.teleport(this.getHomeSpawn());
            } else {
                this.enterBench(player);
            }
            player.sendMessage(Message.joinTeam("home"));
            this.joinedHome.add(player.getUniqueId());
            this.removeAwayPlayer(player);
        } else if (team.equals(Team.AWAY)) {
            athlete.setSpectator(false);
            this.addAwayPlayer(player);
            player.getActivePotionEffects().clear();
            if (this.settings.gameEffect.effect != null) {
                player.addPotionEffect(this.settings.gameEffect.effect);
            }
            player.getInventory().setChestplate(Items.armor(Material.LEATHER_CHESTPLATE, 0xFFFFFF, "Jersey", "Your teams away jersey"));
            player.getInventory().setLeggings(this.away.away);
            player.getInventory().setBoots(this.away.boots);
            player.getInventory().setItem(6, Items.get(Component.text("Your Bench").color(Colour.partix()), Material.OAK_STAIRS));
            player.getInventory().setItem(7, Items.get(Component.text("Game Settings").color(Colour.partix()), Material.CHEST));
            player.getInventory().setItem(8, Items.get(Component.text("Change Team").color(Colour.partix()), Material.GRAY_DYE));
            if (this.getAwayPlayers().size() < this.settings.playersPerTeam) {
                player.teleport(this.getAwaySpawn());
            } else {
                this.enterBench(player);
            }
            player.sendMessage(Message.joinTeam("away"));
            this.joinedAway.add(player.getUniqueId());
            this.removeHomePlayer(player);
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
    }

    @Override
    public void onQuit(Athlete... athletes) {
        for (Athlete athlete : athletes) {
            Player player = athlete.getPlayer();
            if (player == null) continue;
            boolean isProAm = Boolean.TRUE.equals(this.getCustomPropertyOrDefault("proam", false));
            if (this.settings.compType == CompType.RANKED && !this.getState().equals(State.FINAL) && !isProAm) {
                SeasonDb.remove(player.getUniqueId(), SeasonDb.Stat.POINTS, 3);
                player.sendMessage("§cYou disconnected from a ranked game. You lost 3 Season Points!");
            }
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
        boolean outOfBounds;
        if (this.inboundingActive) {
            return;
        }
        Vector v = this.getBall().getVelocity();
        boolean dead = false;
        --this.stoppageTicks;
        if (this.stoppageTicks < 0) {
            Location ballLocation = this.getBall().getLocation();
            if (this.stoppageLastLocation != null) {
                this.stoppageTicks = 60;
                if (this.stoppageLastLocation.distance(ballLocation) < 0.2) {
                    // client wanted the dead ball mechanic removed for now
                    // dead = true;
                }
            }
            this.stoppageLastLocation = ballLocation.clone();
        }
        double motion = Math.abs(v.getX()) + Math.abs(v.getY()) + Math.abs(v.getZ());
        if (motion < 0.01) {
            // client wanted the dead ball mechanic removed for now
//            dead = true;
        }
        final Location blockLoc = this.getBall().getLocation().clone();
        if (!this.getArenaBox().contains(blockLoc.getX(), blockLoc.getY(), blockLoc.getZ())) {
            System.out.println("OUT OF BOUNDS");
            if (state != State.OUT_OF_BOUNDS_THROW_WAIT && state != State.OUT_OF_BOUNDS_THROW)
                outOfBounds();
        }
        if (dead) {
            this.removeBalls();
            this.resetShotClock();
            this.sendTitle(Component.text("Dead Ball").style(Style.style(Colour.deny(), TextDecoration.BOLD)));
            this.startCountdown(GoalGame.State.FACEOFF, 10);
        }
    }

    public void outOfBounds() {
        Ball ball = getBall();
        if (ball == null) return;
        if (outOfBoundsImmunity) {
            System.out.println("Out of bounds immunity is active, skipping out of bounds handling.");
            return;
        }

        Location outOfBoundsLocation = this.findClosestPointOnBoundary(ball.getLocation());
        removeBalls();

        Player lastTouched = (ball.getLastDamager() != null) ? ball.getLastDamager() : null;

        // Determine possession: Opposite team gets ball
        Team inboundTeam;
        if (lastTouched != null) {
            if (getHomePlayers().contains(lastTouched)) {
                inboundTeam = Team.AWAY;
            } else if (getAwayPlayers().contains(lastTouched)) {
                inboundTeam = Team.HOME;
            } else {
                inboundTeam = Team.HOME; // Default
            }
        } else {
            inboundTeam = Team.HOME; // Default
        }

        state = State.OUT_OF_BOUNDS_THROW_WAIT;
        outOfBoundsLostTeam = inboundTeam;
        System.out.println("OUT OF BOUNDS TEAM : " + inboundTeam);

        // put the ball below the hoop if it goes out behind the hoops (not on the side of the court)
        Location finalInboundSpot;
        double ballZ = outOfBoundsLocation.getZ();
        double homeNetZ = getHomeNet().getCenter().getZ();
        double awayNetZ = getAwayNet().getCenter().getZ();

        // Check if the ball went out behind the home team's basket
        if (ballZ > homeNetZ + 1) { // Assuming home basket is on the positive Z side
            finalInboundSpot = getHomeSpawn().clone().add(0, 1.2, 6);
            outOfBoundsSide = false;
            outOfBoundsZ = getHomeNet().getCenter().getBlockZ() + 2;
            outOfBoundsHome = true;
        }
        // Check if the ball went out behind the away team's basket
        else if (ballZ < awayNetZ - 1) { // Assuming away basket is on the negative Z side
            finalInboundSpot = getAwaySpawn().clone().add(0, 1.2, -6);
            outOfBoundsSide = false;
            outOfBoundsZ = getAwayNet().getCenter().getBlockZ() - 2;
            outOfBoundsHome = false;
        }
        // Otherwise, it went out on a sideline
        else {
            finalInboundSpot = outOfBoundsLocation.add(0, 1.2, 0);
            outOfBoundsSide = true;
        }


        // Spawn inbound ball at the determined location
        Bukkit.getScheduler().runTaskLater(Partix.getInstance(), () -> {
            Ball newBall = setBall(BallFactory.create(finalInboundSpot, BallType.BASKETBALL, this));
            newBall.setStealDelay(0);
        }, 30L);

        sendTitle(Component.text("Out of Bounds: " +
                (inboundTeam == Team.HOME ? "Home Ball" : "Away Ball")).style(
                Style.style(Colour.deny(), TextDecoration.BOLD)
        ));
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
        this.startCountdown(GoalGame.State.FACEOFF, 10);
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
            Component statsMessage = Component.text(playerName + ": ", Colour.title()).append(Component.text("Points: ", Colour.border()).append(Component.text(stats.getPoints(), Colour.allow()))).append(Component.text(", 3s: ", Colour.border()).append(Component.text(stats.getThrees(), Colour.allow()))).append(Component.text(", Assists: ", Colour.border()).append(Component.text(stats.getAssists(), Colour.allow()))).append(Component.text(", Rebounds: ", Colour.border()).append(Component.text(stats.getRebounds(), Colour.allow()))).append(Component.text(", Steals: ", Colour.border()).append(Component.text(stats.getSteals(), Colour.allow())));
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
            String rawLine = String.format("%s | Points: %d | Assists: %d | Rebounds: %d | Steals: %d | Turnovers: %d | Possession: %d sec | Defended By: %s | Pass Attempts: %d | FG: %d/%d | 3FG: %d/%d | FG%%: %.1f | 3FG%%: %.1f | eFG%%: %.1f | 3PT Rate: %.1f%% | Assists/Pass: %.1f | Fantasy Points: %d", p.getName(), points, assists, rebounds, steals, turnovers, possTimeSec, defendedInfo, passAttempts, totalFGMadeLocal, totalFGAttLocal, threeFGMadeLocal, threeFGAttLocal, totalFGPct, threeFGPct, eFGPct, threePTRate, assistsPerPass, fantasyPoints);
            rawStatsBuilder.append(rawLine).append("\n\n");
            Component lineComponent = Component.empty().append(Component.text(p.getName() + " | ").decorate(TextDecoration.BOLD).color(TextColor.color(43775))).append(Component.text("Points: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text("" + points).color(TextColor.color(65280))).append(Component.text(" | Assists: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text("" + assists).color(TextColor.color(65280))).append(Component.text(" | Rebounds: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text("" + rebounds).color(TextColor.color(65280))).append(Component.text(" | Steals: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text("" + steals).color(TextColor.color(65280))).append(Component.text(" | Turnovers: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text("" + turnovers).color(TextColor.color(65280))).append(Component.text(" | Possession: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text(possTimeSec + " sec").color(TextColor.color(65280))).append(Component.text(" | Defended By: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text(defendedInfo).color(TextColor.color(65280))).append(Component.text(" | Pass Attempts: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text("" + passAttempts).color(TextColor.color(65280))).append(Component.text(" | FG: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text(totalFGMadeLocal + "/" + totalFGAttLocal).color(TextColor.color(65280))).append(Component.text(" | 3FG: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text(threeFGMadeLocal + "/" + threeFGAttLocal).color(TextColor.color(65280))).append(Component.text(" | FG%%: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text(String.format("%.1f", totalFGPct)).color(TextColor.color(65280))).append(Component.text(" | 3FG%%: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text(String.format("%.1f", threeFGPct)).color(TextColor.color(65280))).append(Component.text(" | eFG%%: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text(String.format("%.1f", eFGPct)).color(TextColor.color(65280))).append(Component.text(" | 3PT Rate: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text(String.format("%.1f%%", threePTRate)).color(TextColor.color(65280))).append(Component.text(" | Assists/Pass: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text(String.format("%.1f", assistsPerPass)).color(TextColor.color(65280))).append(Component.text(" | Fantasy Points: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text("" + fantasyPoints).color(TextColor.color(65280)));
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
            String rawLine = String.format("%s | Points: %d | Assists: %d | Rebounds: %d | Steals: %d | Turnovers: %d | Possession: %d sec | Defended By: %s | Pass Attempts: %d | FG: %d/%d | 3FG: %d/%d | FG%%: %.1f | 3FG%%: %.1f | eFG%%: %.1f | 3PT Rate: %.1f%% | Assists/Pass: %.1f | Fantasy Points: %d", p.getName(), points, assists, rebounds, steals, turnovers, possTimeSec, defendedName, passAttempts, totalFGMadeLocal, totalFGAttLocal, threeFGMadeLocal, threeFGAttLocal, totalFGPct, threeFGPct, eFGPct, threePTRate, assistsPerPass, fantasyPoints);
            rawStatsBuilder.append(rawLine).append("\n\n");
            Component lineComponent = Component.empty().append(Component.text(p.getName() + " | ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFAA00))).append(Component.text("Points: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text("" + points).color(TextColor.color(65280))).append(Component.text(" | Assists: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text("" + assists).color(TextColor.color(65280))).append(Component.text(" | Rebounds: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text("" + rebounds).color(TextColor.color(65280))).append(Component.text(" | Steals: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text("" + steals).color(TextColor.color(65280))).append(Component.text(" | Turnovers: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text("" + turnovers).color(TextColor.color(65280))).append(Component.text(" | Possession: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text(possTimeSec + " sec").color(TextColor.color(65280))).append(Component.text(" | Defended By: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text(defendedName).color(TextColor.color(65280))).append(Component.text(" | Pass Attempts: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text("" + passAttempts).color(TextColor.color(65280))).append(Component.text(" | FG: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text(totalFGMadeLocal + "/" + totalFGAttLocal).color(TextColor.color(65280))).append(Component.text(" | 3FG: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text(threeFGMadeLocal + "/" + threeFGAttLocal).color(TextColor.color(65280))).append(Component.text(" | FG%%: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text(String.format("%.1f", totalFGPct)).color(TextColor.color(65280))).append(Component.text(" | 3FG%%: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text(String.format("%.1f", threeFGPct)).color(TextColor.color(65280))).append(Component.text(" | eFG%%: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text(String.format("%.1f", eFGPct)).color(TextColor.color(65280))).append(Component.text(" | 3PT Rate: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text(String.format("%.1f%%", threePTRate)).color(TextColor.color(65280))).append(Component.text(" | Assists/Pass: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text(String.format("%.1f", assistsPerPass)).color(TextColor.color(65280))).append(Component.text(" | Fantasy Points: ").decorate(TextDecoration.BOLD).color(TextColor.color(0xFFFFFF))).append(Component.text("" + fantasyPoints).color(TextColor.color(65280)));
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

