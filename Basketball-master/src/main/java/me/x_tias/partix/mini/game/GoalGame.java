/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.format.TextColor
 *  org.bukkit.Bukkit
 *  org.bukkit.GameMode
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.Sound
 *  org.bukkit.SoundCategory
 *  org.bukkit.entity.ArmorStand
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.EntityType
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.potion.PotionEffect
 *  org.bukkit.potion.PotionEffectType
 *  org.bukkit.scheduler.BukkitRunnable
 *  org.bukkit.scheduler.BukkitTask
 *  org.bukkit.util.BoundingBox
 *  org.bukkit.util.Vector
 */
package me.x_tias.partix.mini.game;

import lombok.Getter;
import lombok.Setter;
import me.x_tias.partix.Partix;
import me.x_tias.partix.mini.basketball.BasketballGame;
import me.x_tias.partix.mini.factories.Hub;
import me.x_tias.partix.plugin.athlete.Athlete;
import me.x_tias.partix.plugin.athlete.AthleteManager;
import me.x_tias.partix.plugin.ball.Ball;
import me.x_tias.partix.plugin.ball.BallFactory;
import me.x_tias.partix.plugin.ball.BallType;
import me.x_tias.partix.plugin.cooldown.Cooldown;
import me.x_tias.partix.plugin.cosmetics.CosmeticSound;
import me.x_tias.partix.plugin.gui.GUI;
import me.x_tias.partix.plugin.gui.ItemButton;
import me.x_tias.partix.plugin.settings.CompType;
import me.x_tias.partix.plugin.settings.GameType;
import me.x_tias.partix.plugin.settings.Settings;
import me.x_tias.partix.plugin.settings.WinType;
import me.x_tias.partix.plugin.team.BaseTeam;
import me.x_tias.partix.plugin.team.gui.CategoryTeamGUI;
import me.x_tias.partix.plugin.team.nba.TeamBulls;
import me.x_tias.partix.plugin.team.nba.TeamHawks;
import me.x_tias.partix.server.specific.Game;
import me.x_tias.partix.util.Colour;
import me.x_tias.partix.util.Items;
import me.x_tias.partix.util.Message;
import me.x_tias.partix.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.stream.Collectors;

public abstract class GoalGame
        extends Game {
    private static final int WALL_HEIGHT = 21;
    private static final int WALL_X_DISTANCE = 30;
    private static final int WALL_Z_DISTANCE = 26;
    public final Set<UUID> broadcasters = new HashSet<>();
    public final Map<UUID, BukkitTask> broadcastTasks = new HashMap<>();
    public final Map<UUID, Location> broadcastOrigins = new HashMap<>();
    private final Map<UUID, ArmorStand> cameraStands = new HashMap<>();
    private final PlayerStatsManager statsManager = new PlayerStatsManager();
    private final HashMap<UUID, Integer> points = new HashMap<>();
    private final HashMap<UUID, Integer> threes = new HashMap<>();
    private final Map<String, Object> customProperties = new HashMap<>();
    public BaseTeam home;
    public BaseTeam away;
    public int homeScore;
    public int awayScore;
    public Settings settings;
    public int period;
    public int totalPeriods;
    private Location center;
    @Getter
    private BoundingBox homeNet;
    @Getter
    private BoundingBox awayNet;
    @Getter
    private BoundingBox arenaBox;
    @Getter
    private Ball ball;
    @Setter
    protected State state;
    private int gameSeconds;
    private int countSeconds;
    private List<Player> homeTeam;
    private List<Player> awayTeam;
    private final Team faceoffTeam = null;
    private Location homeSpawn;
    private Location awaySpawn;
    private int c;

    public void startBroadcastFor(final Player p) {
        UUID id = p.getUniqueId();
        this.broadcastOrigins.put(id, p.getLocation().clone());
        final Location base = this.getCenter().clone().add(0.0, 8.0, 19.5);
        base.setYaw(180.0f);
        base.setPitch(0.0f);
        final ArmorStand cam = (ArmorStand) p.getWorld().spawnEntity(base, EntityType.ARMOR_STAND);
        cam.setInvisible(true);
        cam.setMarker(true);
        cam.setGravity(false);
        this.cameraStands.put(id, cam);
        p.setGameMode(GameMode.SPECTATOR);
        p.setSpectatorTarget(cam);
        p.sendMessage("§aBroadcast mode enabled. Sneak to exit.");
        this.broadcasters.add(id);
        BukkitTask task = new BukkitRunnable() {

            public void run() {
                if (!p.isOnline() || p.isSneaking()) {
                    this.cancel();
                    GoalGame.this.exitBroadcastFor(p);
                    return;
                }
                Optional<Ball> ob = BallFactory.getNearest(GoalGame.this.getCenter(), 100.0);
                if (ob.isEmpty()) {
                    return;
                }
                Ball ball = ob.get();
                Location target = Optional.ofNullable(ball.getCurrentDamager()).filter(OfflinePlayer::isOnline).map(LivingEntity::getEyeLocation).orElseGet(() -> ball.getLocation().clone().add(0.0, 2.0, 0.0));
                double dx = target.getX() - base.getX();
                double dy = target.getY() - (base.getY() + p.getEyeHeight());
                double dz = target.getZ() - base.getZ();
                double horiz = Math.hypot(dx, dz);
                float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
                float pitch = (float) Math.toDegrees(Math.atan2(-dy, horiz));
                Location moveTo = base.clone();
                moveTo.setYaw(yaw);
                moveTo.setPitch(pitch);
                cam.teleport(moveTo);
            }
        }.runTaskTimer(Partix.getInstance(), 1L, 1L);
        this.broadcastTasks.put(id, task);
    }

    public void exitBroadcastFor(Player p) {
        ArmorStand cam;
        UUID id = p.getUniqueId();
        BukkitTask task = this.broadcastTasks.remove(id);
        if (task != null) {
            task.cancel();
        }
        this.broadcasters.remove(id);
        p.setSpectatorTarget(null);
        p.setGameMode(GameMode.ADVENTURE);
        Location orig = this.broadcastOrigins.remove(id);
        if (orig != null) {
            p.teleport(orig);
        }
        if ((cam = this.cameraStands.remove(id)) != null) {
            cam.remove();
        }
        this.joinTeam(p, Team.SPECTATOR);
    }

    protected PlayerStatsManager getStatsManager() {
        return this.statsManager;
    }

    public void setup(Settings settings, Location location, double xDistance, double yDistance, double xLength, double zWidth, double yHeight) {
        this.center = location.clone().toCenterLocation();
        this.settings = settings;
        Location m = this.center.clone();
//        this.homeNet = new BoundingBox(m.getX() + xDistance - xLength, m.getY() + yDistance, m.getZ() + zWidth, m.getX() + xDistance + xLength, m.getY() + yHeight + yDistance, m.getZ() + zWidth * -1.0);
//        this.awayNet = new BoundingBox(m.getX() + xDistance * -1.0 - xLength * -1.0, m.getY() + yDistance, m.getZ() + zWidth, m.getX() + xDistance * -1.0 + xLength * -1.0, m.getY() + yHeight + yDistance, m.getZ() + zWidth * -1.0);
//        this.homeSpawn = this.homeNet.getCenter().toLocation(this.center.getWorld()).subtract(3.5, -1.0, 0.0);
//        this.awaySpawn = this.awayNet.getCenter().toLocation(this.center.getWorld()).add(3.5, -1.0, 0.0);

        // Swap X and Z for court alignment along Z-axis
        // Create the goal bounding boxes
        // For Z-axis alignment, the width is along X, length is along Z
        this.homeNet = new BoundingBox(
                m.getX() - xLength,
                m.getY() + yDistance,
                m.getZ() + xDistance,
                m.getX() + xLength,
                m.getY() + yDistance + yHeight,
                m.getZ() + xDistance + zWidth
        ).expand(0.25); // Slightly expand the box for better detection

        this.awayNet = new BoundingBox(
                m.getX() - xLength,
                m.getY() + yDistance,
                m.getZ() - xDistance - zWidth,
                m.getX() + xLength,
                m.getY() + yDistance + yHeight,
                m.getZ() - xDistance
        ).expand(0.25);

        // Set spawn points aligned to the court's Z orientation
        this.homeSpawn = new Location(
                this.center.getWorld(),
                this.center.getX(),
                this.center.getY() + 1.0,
                this.homeNet.getCenterZ() - 3.5,
                180,
                0
        );

        this.awaySpawn = new Location(
                this.center.getWorld(),
                this.center.getX(),
                this.center.getY() + 1.0,
                this.awayNet.getCenterZ() + 3.5
        );

        // Arena box with proper dimensions for Z-axis oriented court
        arenaBox = new BoundingBox(
                m.getX() - zWidth - 19.0,  // Left side boundary (X-axis)
                m.getY() - 3,                  // Bottom boundary
                m.getZ() - xDistance - xLength - 5.0 + 3.0,  // Back boundary (away side)
                m.getX() + zWidth + 19.0,  // Right side boundary (X-axis)
                m.getY() + yHeight + yDistance + 10.0,  // Top boundary with extra height
                m.getZ() + xDistance + xLength + 5.0 - 3.0   // Front boundary (home side)
        );

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                // Show center point
                center.getWorld().spawnParticle(Particle.FLAME, center, 3, 0.1, 0.1, 0.1, 0);

                // Show home net boundaries (green)
                showBoundingBox(homeNet, Particle.HAPPY_VILLAGER);

                // Show away net boundaries (red)
                showBoundingBox(awayNet, Particle.FLAME);

                // Show arena box boundaries (blue)
                showBoundingBox(arenaBox, Particle.SOUL_FIRE_FLAME);

                // Show spawn points
                center.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, homeSpawn, 5, 0.2, 0.2, 0.2, 0);
                center.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, awaySpawn, 5, 0.2, 0.2, 0.2, 0);
            }

            private void showBoundingBox(BoundingBox box, Particle particle) {
                World world = center.getWorld();
                double minX = box.getMinX();
                double minY = box.getMinY();
                double minZ = box.getMinZ();
                double maxX = box.getMaxX();
                double maxY = box.getMaxY();
                double maxZ = box.getMaxZ();

                // Bottom edges
                for (double x = minX; x <= maxX; x += 0.5) {
                    world.spawnParticle(particle, x, minY, minZ, 1, 0, 0, 0, 0);
                    world.spawnParticle(particle, x, minY, maxZ, 1, 0, 0, 0, 0);
                }
                for (double z = minZ; z <= maxZ; z += 0.5) {
                    world.spawnParticle(particle, minX, minY, z, 1, 0, 0, 0, 0);
                    world.spawnParticle(particle, maxX, minY, z, 1, 0, 0, 0, 0);
                }

                // Top edges
                for (double x = minX; x <= maxX; x += 0.5) {
                    world.spawnParticle(particle, x, maxY, minZ, 1, 0, 0, 0, 0);
                    world.spawnParticle(particle, x, maxY, maxZ, 1, 0, 0, 0, 0);
                }
                for (double z = minZ; z <= maxZ; z += 0.5) {
                    world.spawnParticle(particle, minX, maxY, z, 1, 0, 0, 0, 0);
                    world.spawnParticle(particle, maxX, maxY, z, 1, 0, 0, 0, 0);
                }

                // Vertical edges
                for (double y = minY; y <= maxY; y += 0.5) {
                    world.spawnParticle(particle, minX, y, minZ, 1, 0, 0, 0, 0);
                    world.spawnParticle(particle, maxX, y, minZ, 1, 0, 0, 0, 0);
                    world.spawnParticle(particle, minX, y, maxZ, 1, 0, 0, 0, 0);
                    world.spawnParticle(particle, maxX, y, maxZ, 1, 0, 0, 0, 0);
                }
            }
        }.runTaskTimer(Partix.getInstance(), 0, 10);

        this.home = new TeamHawks();
        this.away = new TeamBulls();
        this.homeTeam = new ArrayList<>();
        this.awayTeam = new ArrayList<>();
        this.state = State.PREGAME;
        this.reset();
        this.updateArmor();
    }

    public void reset() {
        if (this.settings.winType.timed) {
            this.setTime(this.settings.winType.amount * 60, 0);
        }
        this.period = 1;
        this.totalPeriods = this.settings.periods;
        this.countSeconds = 0;
        this.homeScore = 0;
        this.awayScore = 0;
        this.removeBalls();
        this.resetStats();
        this.resetShotClock();
        this.statsManager.resetStats();
        this.points.clear();
        this.threes.clear();
        this.cancelInboundSequence();
        this.getCenter().getWorld().getEntities().stream()
                .filter(entity -> entity.getPersistentDataContainer().has(Partix.getInstance().getBallKey()))
                .forEach(Entity::remove);
    }

    protected void cancelInboundSequence() {
    }

    public void resetShotClock() {
    }

    public void resetStats() {
        this.statsManager.resetStats();
        for (Player p : this.getHomePlayers()) {
            p.sendMessage(Component.text("Your stats have been reset.").color(Colour.allow()));
        }
        for (Player p : this.getAwayPlayers()) {
            p.sendMessage(Component.text("Your stats have been reset.").color(Colour.allow()));
        }
    }

    public void setTime(int seconds, int ticks) {
        this.gameSeconds = seconds * 20 + ticks;
    }

    public void startCountdown(State s, int seconds) {
        if (s.equals(State.FACEOFF) && !this.state.equals(State.FACEOFF)) {
            this.setFaceoff();
        } else if (s.equals(State.PREGAME)) {
            this.removeBalls();
            this.setPregame();
        } else if (s.equals(State.INBOUND)) {
            getBall().setLocation(getCenter().clone().add(0.0, 0.0, -20.0));
        }
        this.countSeconds = seconds * 20;
        this.state = s;
    }

    public int getTime() {
        return (int) Math.ceil((double) this.gameSeconds / 20.0);
    }

    public String getGameTime() {
        int t = (int) ((double) this.gameSeconds / 20.0);
        int minutes = t / 60;
        int seconds = t % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    public Location getHomeSpawn() {
        return this.homeSpawn.clone();
    }

    public Location getAwaySpawn() {
        return this.awaySpawn.clone();
    }

    public int getTimeTicks() {
        return this.gameSeconds;
    }

    public int getCountSeconds() {
        return (int) Math.ceil((double) this.countSeconds / 20.0);
    }

    public String getPeriodString() {
        boolean ot = this.state.equals(State.OVERTIME);
        if (this.totalPeriods == 1) {
            return ot ? "Overtime " + (this.period - 1) : "Regulation";
        }
        if (this.totalPeriods == 2) {
            return ot ? "Overtime " + (this.period - 1) : (this.period == 1 ? "1st Half" : "2nd Half");
        }
        if (this.totalPeriods == 3) {
            return ot ? "Overtime " + (this.period - 1) : (this.period == 1 ? "1st Period" : (this.period == 2 ? "2nd Period" : "3rd Period"));
        }
        if (this.totalPeriods == 4) {
            return ot ? "Overtime " + (this.period - 1) : (this.period == 1 ? "1st Quarter" : (this.period == 2 ? "2nd Quarter" : (this.period == 3 ? "3rd Quarter" : "4th Quarter")));
        }
        return ot ? "Overtime " + (this.period - 1) : "Regulation (" + this.period + "/" + this.totalPeriods + ")";
    }

    public String getNth(int i) {
        if (i % 10 == 1) {
            return i + "st";
        }
        if (i % 10 == 2) {
            return i + "nd";
        }
        if (i % 10 == 3) {
            return i + "rd";
        }
        return i + "th";
    }

    public String getShortPeriodString() {
        if (this.state.equals(State.PREGAME) || this.state.equals(State.FINAL)) {
            return "  §7- ";
        }
        if (this.state.equals(State.OVERTIME) || this.period > this.totalPeriods) {
            if (this.settings.suddenDeath) {
                return "§cSudden Death";
            }
            return "§cOT-" + (this.period - this.settings.periods);
        }
        if (this.totalPeriods == 1) {
            return this.getNth(this.period) + " Per §7(" + this.period + "/" + this.totalPeriods + ")";
        }
        if (this.totalPeriods == 2) {
            return this.getNth(this.period) + " Hlf §7(" + this.period + "/" + this.totalPeriods + ")";
        }
        if (this.totalPeriods == 3) {
            return this.getNth(this.period) + " Per §7(" + this.period + "/" + this.totalPeriods + ")";
        }
        if (this.totalPeriods == 4) {
            return this.getNth(this.period) + " Qtr §7(" + this.period + "/" + this.totalPeriods + ")";
        }
        return this.getNth(this.period) + " Per §7(" + this.period + "/" + this.totalPeriods + ")";
    }

    public State getState() {
        if (this.state == null) {
            this.state = State.PREGAME;
        }
        return this.state;
    }

    public Location getCenter() {
        return this.center.clone();
    }

    public Ball setBall(Ball b) {
        this.ball = b;
        return this.ball;
    }

    public List<Player> getHomePlayers() {
        ArrayList<Player> copy = new ArrayList<>(this.homeTeam);
        this.homeTeam = copy.stream().filter(p -> p.isOnline() && this.getPlayers().contains(p) && AthleteManager.get(p.getUniqueId()).getPlace().equals(this)).collect(Collectors.toList());
        return this.homeTeam;
    }

    public void addHomePlayer(Player player) {
        this.homeTeam.add(player);
    }

    public void removeHomePlayer(Player player) {
        this.homeTeam.remove(player);
    }

    public void removeAwayPlayer(Player player) {
        this.awayTeam.remove(player);
    }

    public List<Player> getAwayPlayers() {
        ArrayList<Player> copy = new ArrayList<>(this.awayTeam);
        this.awayTeam = copy.stream().filter(p -> p.isOnline() && this.getPlayers().contains(p) && AthleteManager.get(p.getUniqueId()).getPlace().equals(this)).collect(Collectors.toList());
        return this.awayTeam;
    }

    public void addAwayPlayer(Player player) {
        this.awayTeam.add(player);
    }

    public void addTime(int ticks) {
        this.gameSeconds += ticks;
    }

    @Override
    public void onTick() {
        if (this.ball != null) {
            if (state == State.OUT_OF_BOUNDS_THROW_WAIT || state == State.OUT_OF_BOUNDS_THROW) {
                // wait for the player to throw the ball, then change the state
                return;
            } else {
                this.goalDetection();
                this.stoppageDetection();
            }
        }
        if (this.state.equals(State.REGULATION) || this.state.equals(State.OVERTIME)) {
            this.runClock();
        } else {
            this.runCountdown();
        }
        ++this.c;
        if (this.c % 2 == 0) {
            this.spectatorDetection();
        }
        if (this.c > 9) {
            this.updateDisplay();
            this.c = 0;
        }
    }

    public void removeBalls() {
        System.out.println("REMOVE BALLS");
        if (this.ball != null) {
            this.ball.remove();
            this.ball = null;
        }
        BallFactory.removeBalls(this.center, 40.0);
        this.ball = null;
    }

    public abstract void stoppageDetection();

    public void spectatorDetection() {
        this.getAthletes().stream().filter(a -> a.isSpectating() && a.getPlayer().getLocation().getY() < this.center.getY() + 5.0).forEach(a -> {
            a.getPlayer().setVelocity(new Vector(0.0, 0.334, 0.0));
            Bukkit.getScheduler().runTaskLater(Partix.getInstance(), () -> {
                Player player = a.getPlayer();
                if (!player.isFlying() && player.getLocation().getY() >= this.center.getY() + 5.0) {
                    player.setFlying(true);
                }
            }, 10L);
        });
    }

    private void runClock() {
        if (this.settings.winType.timed) {
            if (this.gameSeconds % 20 == 0 && this.gameSeconds < 105 && this.gameSeconds > 15) {
                this.playSound(Sound.BLOCK_NOTE_BLOCK_BIT, SoundCategory.MASTER, 1.0f, 1.0f);
            }
            --this.gameSeconds;
            if (this.gameSeconds < 0) {
                this.endPeriod();
            }
        }
    }

    private void runCountdown() {
        if (this.countSeconds > 0) {
            --this.countSeconds;
            if (this.state.equals(State.FACEOFF) && this.countSeconds % 20 == 0 && this.countSeconds > 5 && this.countSeconds < 105) {
                this.removeBalls();
                this.sendTitle(Component.text("Commencing in ", Colour.bold()).append(Component.text((int) Math.ceil((double) this.countSeconds / 20.0), Colour.title())));
            }
            if (this.countSeconds < 1) {
                this.endCountdown();
            }
        }
    }

    public void forceEndCountdown() {
        endCountdown();
    }

    private void endCountdown() {
        this.removeBalls();
        if (this.state.equals(State.PREGAME)) {
            this.startCountdown(State.FACEOFF, this.settings.waitType.low);
            WinType winType = this.settings.winType;
            if (winType.timed) {
                this.setTime(winType.amount * 20, 0);
            }
        } else if (this.state.equals(State.FACEOFF)) {
            this.dropBall();
            this.state = this.period > this.totalPeriods ? State.OVERTIME : State.REGULATION;
        } else if (this.state.equals(State.FINAL)) {
            if (this.settings.gameType.equals(GameType.AUTOMATIC)) {
                this.kickAll();
                this.state = State.PREGAME;
                this.startCountdown(State.PREGAME, -1);
            } else {
                this.reset();
                this.startCountdown(State.PREGAME, -1);
            }
        }
    }

    public abstract void setPregame();

    public abstract void dropBall();

    private void endPeriod() {
        if (this.periodIsComplete(this.gameSeconds)) {
            this.removeBalls();
            boolean next = false;
            if (this.period >= this.totalPeriods) {
                if (this.homeScore > this.awayScore) {
                    this.gameOver(Team.HOME);
                } else if (this.awayScore > this.homeScore) {
                    this.gameOver(Team.AWAY);
                } else {
                    next = true;
                }
            } else {
                next = true;
            }
            if (next) {
                this.sendTitle(Component.text("End of " + this.getPeriodString(), Colour.deny()));
                ++this.period;
                if (this.period > this.totalPeriods) {
                    this.state = State.OVERTIME;
                    this.setTime(180, 0);
                } else {
                    this.state = State.REGULATION;
                    if (this.settings.winType.timed) {
                        this.setTime(this.settings.winType.amount * 60, 0);
                    }
                }
                this.startCountdown(State.FACEOFF, this.settings.waitType.med);
            }
        }
    }

    public void enableShotClock() {
    }

    public void disableShotClock() {
    }

    public abstract void setFaceoff();

    public abstract boolean periodIsComplete(int var1);

    public abstract void gameOver(Team var1);

    private void goalDetection() {
        if (this.ball != null && (this.state.equals(State.REGULATION) || this.state.equals(State.OVERTIME))) {
            Location ballLoc = this.ball.getLocation();
            Vector ballVec = ballLoc.toVector();

//            System.out.println("BALL LOCATION = " + ballVec);
//            System.out.println("HOME NET = " + this.homeNet);
//            System.out.println("AWAY NET = " + this.awayNet);
//            System.out.println("HOME CONTAINS BALL: " + this.homeNet.contains(ballVec));
//            System.out.println("AWAY CONTAINS BALL: " + this.awayNet.contains(ballVec));

            if (this.homeNet.clone().expand(0.1).contains(ballVec)) {
                this.goal(Team.AWAY);
            } else if (this.awayNet.clone().expand(0.1).contains(ballVec)) {
                this.goal(Team.HOME);
            }
        }
    }

    public abstract void updateDisplay();

    public abstract void goal(Team var1);

    public abstract void joinTeam(Player var1, Team var2);

    public abstract BallType getBallType();

    @Override
    public void clickItem(Player player, ItemStack itemStack) {
        if (itemStack.getType().equals(Material.GRAY_DYE)) {
            if (this.settings.compType.equals(CompType.RANKED) || Boolean.TRUE.equals(this.getCustomProperty("anteUp"))) {
                player.sendMessage(Component.text("Team joining is disabled in this game.").color(TextColor.color(0xFF0000)));
                return;
            }
            if (!BallFactory.getNearby(player.getLocation(), 5.0).isEmpty()) {
                player.sendMessage(Message.cantDoThisNow());
                return;
            }
            new GUI("Change Team", 3, false, new ItemButton(10, Items.get(Component.text("Home Team").color(Colour.partix()), Material.BLACK_WOOL), p -> {
                if (this.settings.teamLock) {
                    player.sendMessage(Message.disabled());
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_FALL, SoundCategory.MASTER, 1.0f, 1.0f);
                } else {
                    this.joinTeam(p, Team.HOME);
                }
            }), new ItemButton(11, Items.get(Component.text("Away Team").color(Colour.partix()), Material.WHITE_WOOL), p -> {
                if (this.settings.teamLock) {
                    player.sendMessage(Message.disabled());
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_FALL, SoundCategory.MASTER, 1.0f, 1.0f);
                } else {
                    this.joinTeam(p, Team.AWAY);
                }
            }), new ItemButton(13, Items.get(Component.text("Broadcast View").color(Colour.partix()), Material.COMPASS), p -> {
                p.closeInventory();
                this.startBroadcastFor(p);
            }), new ItemButton(14, Items.get(Component.text("Spectators").color(Colour.partix()), Material.ENDER_EYE), p -> {
                if (this.settings.teamLock) {
                    player.sendMessage(Message.disabled());
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_FALL, SoundCategory.MASTER, 1.0f, 1.0f);
                } else {
                    this.joinTeam(p, Team.SPECTATOR);
                }
            }), new ItemButton(16, Items.get(Component.text("Leave Game").color(Colour.deny()), Material.IRON_DOOR), p -> {
                if (this.settings.compType.equals(CompType.RANKED)) {
                    player.sendMessage(Component.text("You are unable to leave a ranked game!").color(TextColor.color(0xFF0000)));
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_FALL, SoundCategory.MASTER, 1.0f, 1.0f);
                } else {
                    Hub.hub.join(AthleteManager.get(player.getUniqueId()));
                }
            })).openInventory(player);
            return;
        }
        if (itemStack.getType().equals(Material.OAK_STAIRS)) {
            if (BallFactory.getNearby(player.getLocation(), 3.0).size() == 0 && !AthleteManager.get(player.getUniqueId()).isSpectating()) {
                if (this.isInBench(player)) {
                    this.leaveBench(player);
                } else {
                    this.enterBench(player);
                }
            } else {
                player.sendMessage(Message.cantDoThisNow());
            }
        } else if (itemStack.getType().equals(Material.CHEST)) {
            if (this.settings.gameType.equals(GameType.MANUAL)) {
                new GUI("Game Settings", 6, false, new ItemButton(11, Items.get(Component.text("Reset Game").color(Colour.partix()), Material.RED_CONCRETE_POWDER), p -> {
                    if (!p.hasPermission("rank.vip")) {
                        p.sendMessage("§cVIP rank required to reset the game.");
                        p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_FALL, SoundCategory.MASTER, 1.0f, 1.0f);
                        return;
                    }
                    if (this.canEditGame(player)) {
                        this.reset();
                        this.startCountdown(State.PREGAME, -1);
                    } else {
                        player.sendMessage(Message.onlyOwner());
                        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_FALL, SoundCategory.MASTER, 1.0f, 1.0f);
                    }
                }), new ItemButton(12, Items.get(Component.text("Pregame").color(Colour.partix()), Material.BLUE_CONCRETE_POWDER), p -> {
                    this.reset();
                    this.startCountdown(State.PREGAME, -1);
                }), new ItemButton(13, Items.get(Component.text("Ref Book").color(Colour.partix()), Material.BOOK), p -> {
                    if (!p.hasPermission("rank.vip")) {
                        p.sendMessage("§cVIP rank required to open the Ref Book.");
                        p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_FALL, SoundCategory.MASTER, 1.0f, 1.0f);
                        return;
                    }
                    if (this.canEditGame(p)) {
                        this.openRefBookGUI(p);
                    } else {
                        p.sendMessage(Message.onlyOwner());
                        p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_FALL, SoundCategory.MASTER, 1.0f, 1.0f);
                    }
                }), new ItemButton(14, Items.get(Component.text("Start Play").color(Colour.partix()), Material.YELLOW_CONCRETE), p -> {
                    if (!p.hasPermission("rank.vip")) {
                        p.sendMessage("§cVIP rank required to start play.");
                        p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_FALL, SoundCategory.MASTER, 1.0f, 1.0f);
                        return;
                    }
                    if (this.canEditGame(player)) {
                        this.startCountdown(State.FACEOFF, 5);
                    } else {
                        player.sendMessage(Message.onlyOwner());
                        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_FALL, SoundCategory.MASTER, 1.0f, 1.0f);
                    }
                }), new ItemButton(15, Items.get(Component.text("Stop Play").color(Colour.partix()), Material.ORANGE_CONCRETE), p -> {
                    if (!p.hasPermission("rank.vip")) {
                        p.sendMessage("§cVIP rank required to stop play.");
                        p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_FALL, SoundCategory.MASTER, 1.0f, 1.0f);
                        return;
                    }
                    if (this.canEditGame(player)) {
                        this.startCountdown(State.STOPPAGE, -5);
                        this.removeBalls();
                    } else {
                        player.sendMessage(Message.onlyOwner());
                        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_FALL, SoundCategory.MASTER, 1.0f, 1.0f);
                    }
                }), new ItemButton(18, Items.get(Component.text("Enable Shot Clock").color(Colour.partix()), Material.GREEN_DYE), p -> {
                    if (!p.hasPermission("rank.vip")) {
                        p.sendMessage("§cVIP rank required to enable shot clock.");
                        p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_FALL, SoundCategory.MASTER, 1.0f, 1.0f);
                        return;
                    }
                    if (this.canEditGame(p)) {
                        this.enableShotClock();
                        this.sendMessage(Component.text("Shot Clock Enabled").color(Colour.allow()));
                    } else {
                        p.sendMessage(Message.onlyOwner());
                        p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_FALL, SoundCategory.MASTER, 1.0f, 1.0f);
                    }
                }), new ItemButton(19, Items.get(Component.text("Disable Shot Clock").color(Colour.partix()), Material.RED_DYE), p -> {
                    if (!p.hasPermission("rank.vip")) {
                        p.sendMessage("§cVIP rank required to disable shot clock.");
                        p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_FALL, SoundCategory.MASTER, 1.0f, 1.0f);
                        return;
                    }
                    if (this.canEditGame(p)) {
                        this.disableShotClock();
                        this.sendMessage(Component.text("Shot Clock Disabled").color(Colour.deny()));
                    } else {
                        p.sendMessage(Message.onlyOwner());
                        p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_FALL, SoundCategory.MASTER, 1.0f, 1.0f);
                    }
                }), new ItemButton(20, Items.get(Component.text("Reset Shot Clock").color(Colour.partix()), Material.REDSTONE_TORCH), p -> {
                    if (!p.hasPermission("rank.vip")) {
                        p.sendMessage("§cVIP rank required to reset shot clock.");
                        p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_FALL, SoundCategory.MASTER, 1.0f, 1.0f);
                        return;
                    }
                    if (this.canEditGame(p)) {
                        this.resetShotClock();
                        this.sendMessage(Component.text("Shot Clock reset to 24 seconds").color(Colour.allow()));
                    } else {
                        p.sendMessage(Message.onlyOwner());
                        p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_FALL, SoundCategory.MASTER, 1.0f, 1.0f);
                    }
                }), new ItemButton(25, Items.get(Component.text("Change Home Team").color(Colour.partix()), Material.NETHERITE_CHESTPLATE), p -> {
                    if (!this.canEditGame(p)) {
                        p.sendMessage(Message.onlyOwner());
                        p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_FALL, SoundCategory.MASTER, 1.0f, 1.0f);
                        return;
                    }
                    CategoryTeamGUI.open(p, true, chosen -> {
                        this.home = chosen;
                        this.sendMessage(Message.settingChange("Home Team", Text.serialize(this.home.name)));
                        this.updateArmor();
                        this.updateDisplay();
                    });
                }), new ItemButton(26, Items.get(Component.text("Change Away Team").color(Colour.partix()), Material.IRON_CHESTPLATE), p -> {
                    if (!this.canEditGame(p)) {
                        p.sendMessage(Message.onlyOwner());
                        p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_FALL, SoundCategory.MASTER, 1.0f, 1.0f);
                        return;
                    }
                    CategoryTeamGUI.open(p, false, chosen -> {
                        this.away = chosen;
                        this.sendMessage(Message.settingChange("Away Team", Text.serialize(this.away.name)));
                        this.updateArmor();
                        this.updateDisplay();
                    });
                }), new ItemButton(27, Items.get(Component.text("Team Joining: Enabled").color(Colour.partix()), Material.OAK_DOOR), p -> {
                    if (this.canEditGame(player)) {
                        this.settings.teamLock = false;
                        this.sendMessage(Message.settingChange("Team joining", "unlocked"));
                    } else {
                        player.sendMessage(Message.onlyOwner());
                        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_FALL, SoundCategory.MASTER, 1.0f, 1.0f);
                    }
                }), new ItemButton(28, Items.get(Component.text("Team Joining: Disabled").color(Colour.partix()), Material.IRON_DOOR), p -> {
                    if (this.canEditGame(player)) {
                        this.settings.teamLock = true;
                        this.sendMessage(Message.settingChange("Team joining", "locked"));
                    } else {
                        player.sendMessage(Message.onlyOwner());
                        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_FALL, SoundCategory.MASTER, 1.0f, 1.0f);
                    }
                }), new ItemButton(30, Items.get(Component.text("OT: Sudden Death").color(Colour.partix()), Material.RED_DYE), p -> {
                    if (!p.hasPermission("rank.vip")) {
                        p.sendMessage("§cVIP rank required to change OT settings.");
                        p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_FALL, SoundCategory.MASTER, 1.0f, 1.0f);
                        return;
                    }
                    if (this.canEditGame(player)) {
                        this.settings.suddenDeath = true;
                        this.sendMessage(Message.settingChange("Overtime", "sudden death"));
                    } else {
                        player.sendMessage(Message.onlyOwner());
                        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_FALL, SoundCategory.MASTER, 1.0f, 1.0f);
                    }
                }), new ItemButton(31, Items.get(Component.text("OT: 3 Minutes Timed").color(Colour.partix()), Material.CLOCK), p -> {
                    if (!p.hasPermission("rank.vip")) {
                        p.sendMessage("§cVIP rank required to change OT settings.");
                        p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_FALL, SoundCategory.MASTER, 1.0f, 1.0f);
                        return;
                    }
                    if (this.canEditGame(player)) {
                        this.settings.suddenDeath = false;
                        this.sendMessage(Message.settingChange("Overtime", "timed"));
                    } else {
                        player.sendMessage(Message.onlyOwner());
                        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_FALL, SoundCategory.MASTER, 1.0f, 1.0f);
                    }
                }), new ItemButton(36, Items.get(Component.text("Team Management").color(Colour.partix()), Material.COMPASS), p -> {
                    if (!this.canEditGame(p)) {
                        p.sendMessage(Message.onlyOwner());
                        return;
                    }
                    if (this instanceof BasketballGame) {
                        ((BasketballGame) this).openTeamManagementGUI(p);
                    }
                }), new ItemButton(41, Items.get(Component.text("Sections: 1 Match").color(Colour.partix()), Material.WHITE_CANDLE), p -> {
                    if (!p.hasPermission("rank.vip")) {
                        p.sendMessage("§cVIP rank required to change sections.");
                        p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_FALL, SoundCategory.MASTER, 1.0f, 1.0f);
                        return;
                    }
                    if (this.canEditGame(player)) {
                        this.settings.periods = 1;
                        this.totalPeriods = 1;
                        this.sendMessage(Message.settingChange("Sections", "1 Match"));
                    } else {
                        player.sendMessage(Message.onlyOwner());
                        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_FALL, SoundCategory.MASTER, 1.0f, 1.0f);
                    }
                }), new ItemButton(42, Items.get(Component.text("Sections: 2 Halves").color(Colour.partix()), Material.WHITE_CANDLE), p -> {
                    if (!p.hasPermission("rank.vip")) {
                        p.sendMessage("§cVIP rank required to change sections.");
                        p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_FALL, SoundCategory.MASTER, 1.0f, 1.0f);
                        return;
                    }
                    if (this.canEditGame(player)) {
                        this.settings.periods = 2;
                        this.totalPeriods = 2;
                        this.sendMessage(Message.settingChange("Sections", "2 Halves"));
                    } else {
                        player.sendMessage(Message.onlyOwner());
                        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_FALL, SoundCategory.MASTER, 1.0f, 1.0f);
                    }
                }), new ItemButton(43, Items.get(Component.text("Sections: 3 Periods").color(Colour.partix()), Material.WHITE_CANDLE), p -> {
                    if (!p.hasPermission("rank.vip")) {
                        p.sendMessage("§cVIP rank required to change sections.");
                        p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_FALL, SoundCategory.MASTER, 1.0f, 1.0f);
                        return;
                    }
                    if (this.canEditGame(player)) {
                        this.settings.periods = 3;
                        this.totalPeriods = 3;
                        this.sendMessage(Message.settingChange("Sections", "3 Periods"));
                    } else {
                        player.sendMessage(Message.onlyOwner());
                        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_FALL, SoundCategory.MASTER, 1.0f, 1.0f);
                    }
                }), new ItemButton(44, Items.get(Component.text("Sections: 4 Quarters").color(Colour.partix()), Material.WHITE_CANDLE), p -> {
                    if (!p.hasPermission("rank.vip")) {
                        p.sendMessage("§cVIP rank required to change sections.");
                        p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_FALL, SoundCategory.MASTER, 1.0f, 1.0f);
                        return;
                    }
                    if (this.canEditGame(player)) {
                        this.settings.periods = 4;
                        this.totalPeriods = 4;
                        this.sendMessage(Message.settingChange("Sections", "4 Quarters"));
                    } else {
                        player.sendMessage(Message.onlyOwner());
                        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_FALL, SoundCategory.MASTER, 1.0f, 1.0f);
                    }
                }), new ItemButton(45, Items.get(Component.text("Scoring: 2 Minutes").color(Colour.partix()), Material.LIME_DYE), p -> {
                    if (!p.hasPermission("rank.vip")) {
                        p.sendMessage("§cVIP rank required to change scoring options.");
                        p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_FALL, SoundCategory.MASTER, 1.0f, 1.0f);
                        return;
                    }
                    if (this.canEditGame(player)) {
                        this.sendMessage(Message.settingChange("Scoring type", "2 Minute increments"));
                        this.settings.winType = WinType.TIME_2;
                        if (this.getTime() > 120) {
                            this.setTime(120, 0);
                        }
                    } else {
                        player.sendMessage(Message.onlyOwner());
                        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_FALL, SoundCategory.MASTER, 1.0f, 1.0f);
                    }
                }), new ItemButton(46, Items.get(Component.text("Scoring: 3 Minutes").color(Colour.partix()), Material.LIME_DYE), p -> {
                    if (!p.hasPermission("rank.vip")) {
                        p.sendMessage("§cVIP rank required to change scoring options.");
                        p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_FALL, SoundCategory.MASTER, 1.0f, 1.0f);
                        return;
                    }
                    if (this.canEditGame(player)) {
                        this.sendMessage(Message.settingChange("Scoring type", "3 Minute increments"));
                        this.settings.winType = WinType.TIME_3;
                        if (this.getTime() > 180) {
                            this.setTime(180, 0);
                        }
                    } else {
                        player.sendMessage(Message.onlyOwner());
                        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_FALL, SoundCategory.MASTER, 1.0f, 1.0f);
                    }
                }), new ItemButton(47, Items.get(Component.text("Scoring: 5 Minutes").color(Colour.partix()), Material.LIME_DYE), p -> {
                    if (!p.hasPermission("rank.vip")) {
                        p.sendMessage("§cVIP rank required to change scoring options.");
                        p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_FALL, SoundCategory.MASTER, 1.0f, 1.0f);
                        return;
                    }
                    if (this.canEditGame(player)) {
                        this.sendMessage(Message.settingChange("Scoring type", "5 Minute increments"));
                        this.settings.winType = WinType.TIME_5;
                        if (this.getTime() > 300) {
                            this.setTime(300, 0);
                        }
                    } else {
                        player.sendMessage(Message.onlyOwner());
                        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_FALL, SoundCategory.MASTER, 1.0f, 1.0f);
                    }
                }), new ItemButton(48, Items.get(Component.text("Scoring: 6 Minutes").color(Colour.partix()), Material.LIME_DYE), p -> {
                    if (!p.hasPermission("rank.vip")) {
                        p.sendMessage("§cVIP rank required to change scoring options.");
                        p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_FALL, SoundCategory.MASTER, 1.0f, 1.0f);
                        return;
                    }
                    if (this.canEditGame(player)) {
                        this.sendMessage(Message.settingChange("Scoring type", "6 Minute increments"));
                        this.settings.winType = WinType.TIME_6;
                        if (this.getTime() > 360) {
                            this.setTime(360, 0);
                        }
                    } else {
                        player.sendMessage(Message.onlyOwner());
                        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_FALL, SoundCategory.MASTER, 1.0f, 1.0f);
                    }
                }), new ItemButton(49, Items.get(Component.text("Scoring: First to 3").color(Colour.partix()), Material.PINK_DYE), p -> {
                    if (!p.hasPermission("rank.vip")) {
                        p.sendMessage("§cVIP rank required to change scoring options.");
                        p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_FALL, SoundCategory.MASTER, 1.0f, 1.0f);
                        return;
                    }
                    if (this.canEditGame(player)) {
                        this.sendMessage(Message.settingChange("Scoring type", "first to 3"));
                        this.settings.winType = WinType.GOALS_3;
                    } else {
                        player.sendMessage(Message.onlyOwner());
                        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_FALL, SoundCategory.MASTER, 1.0f, 1.0f);
                    }
                }), new ItemButton(50, Items.get(Component.text("Scoring: First to 5").color(Colour.partix()), Material.PINK_DYE), p -> {
                    if (!p.hasPermission("rank.vip")) {
                        p.sendMessage("§cVIP rank required to change scoring options.");
                        p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_FALL, SoundCategory.MASTER, 1.0f, 1.0f);
                        return;
                    }
                    if (this.canEditGame(player)) {
                        this.sendMessage(Message.settingChange("Scoring type", "first to 5"));
                        this.settings.winType = WinType.GOALS_5;
                    } else {
                        player.sendMessage(Message.onlyOwner());
                        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_FALL, SoundCategory.MASTER, 1.0f, 1.0f);
                    }
                }), new ItemButton(51, Items.get(Component.text("Scoring: First to 10").color(Colour.partix()), Material.PINK_DYE), p -> {
                    if (!p.hasPermission("rank.vip")) {
                        p.sendMessage("§cVIP rank required to change scoring options.");
                        p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_FALL, SoundCategory.MASTER, 1.0f, 1.0f);
                        return;
                    }
                    if (this.canEditGame(player)) {
                        this.sendMessage(Message.settingChange("Scoring type", "first to 10"));
                        this.settings.winType = WinType.GOALS_10;
                    } else {
                        player.sendMessage(Message.onlyOwner());
                        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_FALL, SoundCategory.MASTER, 1.0f, 1.0f);
                    }
                }), new ItemButton(52, Items.get(Component.text("Scoring: First to 15").color(Colour.partix()), Material.PINK_DYE), p -> {
                    if (!p.hasPermission("rank.vip")) {
                        p.sendMessage("§cVIP rank required to change scoring options.");
                        p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_FALL, SoundCategory.MASTER, 1.0f, 1.0f);
                        return;
                    }
                    if (this.canEditGame(player)) {
                        this.sendMessage(Message.settingChange("Scoring type", "first to 15"));
                        this.settings.winType = WinType.GOALS_15;
                    } else {
                        player.sendMessage(Message.onlyOwner());
                        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_FALL, SoundCategory.MASTER, 1.0f, 1.0f);
                    }
                }), new ItemButton(53, Items.get(Component.text("Scoring: First to 21").color(Colour.partix()), Material.PINK_DYE), p -> {
                    if (!p.hasPermission("rank.vip")) {
                        p.sendMessage("§cVIP rank required to change scoring options.");
                        p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_FALL, SoundCategory.MASTER, 1.0f, 1.0f);
                        return;
                    }
                    if (this.canEditGame(player)) {
                        this.sendMessage(Message.settingChange("Scoring type", "first to 21"));
                        this.settings.winType = WinType.GOALS_21;
                    } else {
                        player.sendMessage(Message.onlyOwner());
                        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_FALL, SoundCategory.MASTER, 1.0f, 1.0f);
                    }
                }), new ItemButton(37, Items.get(Component.text("Rebound Machine: ON").color(Colour.allow()), Material.SLIME_BALL), p -> {
                    if (!p.hasPermission("rank.vip")) {
                        p.sendMessage("§cVIP rank required to toggle rebound machine.");
                        p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_FALL, SoundCategory.MASTER, 1.0f, 1.0f);
                        return;
                    }
                    if (this.canEditGame(p)) {
                        this.settings.reboundMachineEnabled = true;

                        // Reset rebound machine stats when enabled
                        if (this instanceof BasketballGame) {
                            ((BasketballGame) this).resetReboundMachineStats();
                        }

                        this.sendMessage(Message.settingChange("Rebound Machine", "enabled"));
                        p.sendMessage(Component.text("Rebound Machine enabled - track your shooting stats!").color(Colour.allow()));
                    } else {
                        p.sendMessage(Message.onlyOwner());
                        p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_FALL, SoundCategory.MASTER, 1.0f, 1.0f);
                    }
                }), new ItemButton(38, Items.get(Component.text("Rebound Machine: OFF").color(Colour.deny()), Material.BARRIER), p -> {
                    if (!p.hasPermission("rank.vip")) {
                        p.sendMessage("§cVIP rank required to toggle rebound machine.");
                        p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_FALL, SoundCategory.MASTER, 1.0f, 1.0f);
                        return;
                    }
                    if (this.canEditGame(p)) {
                        this.settings.reboundMachineEnabled = false;
                        this.sendMessage(Message.settingChange("Rebound Machine", "disabled"));
                        p.sendMessage(Component.text("Rebound Machine disabled - normal gameplay.").color(Colour.deny()));
                    } else {
                        p.sendMessage(Message.onlyOwner());
                        p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_FALL, SoundCategory.MASTER, 1.0f, 1.0f);
                    }
                })).openInventory(player);
            } else {
                player.sendMessage(Message.disabled());
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_FALL, SoundCategory.MASTER, 1.0f, 1.0f);
            }
        }
    }

    public GoalGame.Team getTeamOf(Player p) {
        // from new method
//        if (homeTeam.contains(player)) {
//            return Team.HOME;
//        } else if (awayTeam.contains(player)) {
//            return Team.AWAY;
//        } else {
//            return Team.SPECTATOR;
//        }
        return this.getHomePlayers().contains(p) ? GoalGame.Team.HOME : GoalGame.Team.AWAY;
    }

    public void enterBench(Player player) {
        BasketballGame bg;
        Location bench = getTeamOf(player) == Team.HOME ? this.homeSpawn : this.awaySpawn;
        GoalGame goalGame = this;
        if (goalGame instanceof BasketballGame && (bg = (BasketballGame) goalGame).getCourtLength() == 32.0) {
            bench.add(0.0, 0.0, -4.0);
        }
        player.teleport(bench);
        Cooldown.setRestricted(player.getUniqueId(), 20);
    }

    public void leaveBench(Player player) {
        Location location = player.getLocation().clone();
        location.setZ(this.center.getZ() - 12.5);
        player.teleport(location);
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 10, 2, true, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 50, 1, true, false));
        Cooldown.setRestricted(player.getUniqueId(), 10);
    }

    public boolean isInBench(Player player) {
        return player.getLocation().getZ() < this.center.getZ() - 14.5;
    }

    public void updateArmor() {
        this.homeTeam.forEach(player -> {
            player.getInventory().setChestplate(this.home.chest);
            player.getInventory().setLeggings(this.home.pants);
            player.getInventory().setBoots(this.home.boots);
        });
        this.awayTeam.forEach(player -> {
            player.getInventory().setChestplate(Items.armor(Material.LEATHER_CHESTPLATE, 0xFFFFFF, "Jersey", "Your teams away jersey"));
            player.getInventory().setLeggings(this.away.away);
            player.getInventory().setBoots(this.away.boots);
        });
    }

    public abstract boolean canEditGame(Player var1);

    public void addScore(Player player, int points) {
        this.updateScoreboard(player, points);
        this.checkWinCondition();
        if (points == 3) {
            this.handleGreenSound(player, points);
        }
    }

    private void handleGreenSound(Player player, int pointsScored) {
        Athlete athlete = AthleteManager.get(player.getUniqueId());
        if (athlete == null) {
            Bukkit.getLogger().severe("[ERROR] No athlete data for player: " + player.getName());
            return;
        }
        if (pointsScored == 3) {
            CosmeticSound greenSound = athlete.getGreenSound();
            if (greenSound != null) {
                Bukkit.getLogger().info("[DEBUG] Retrieved Green Sound: " + greenSound.getName());
                if (greenSound.getSoundIdentifier() != null && !greenSound.getSoundIdentifier().isEmpty()) {
                    Bukkit.getLogger().info("[DEBUG] Playing Green Sound for player: " + player.getName() + ", Sound: " + greenSound.getSoundIdentifier());
                    player.getWorld().playSound(player.getLocation(), greenSound.getSoundIdentifier(), SoundCategory.PLAYERS, 6.0f, 1.0f);
                } else {
                    Bukkit.getLogger().severe("[ERROR] Green Sound is missing a sound: " + greenSound.getName());
                }
            } else {
                Bukkit.getLogger().severe("[ERROR] No Green Sound assigned to player: " + player.getName());
            }
        }
    }

    private void updateScoreboard(Player player, int points) {
    }

    private void checkWinCondition() {
    }

    public void openRefBookGUI(Player player) {
        GUI refGUI = new GUI("Ref Book", 4, false);
        ItemStack infoItem = Items.get(Component.text("Home: " + this.homeScore + " | Away: " + this.awayScore + "\nTime: " + this.getGameTime()).color(Colour.text()), Material.PAPER);
        refGUI.addButton(new ItemButton(4, infoItem, p -> {
        }));
        refGUI.addButton(new ItemButton(9, Items.get(Component.text("Home -3").color(Colour.deny()), Material.REDSTONE_BLOCK), p -> {
            if (this.canEditGame(p)) {
                this.homeScore = Math.max(0, this.homeScore - 3);
                p.sendMessage(Component.text("Home score decreased by 3, new score: " + this.homeScore).color(Colour.deny()));
                this.updateDisplay();
                this.openRefBookGUI(p);
            } else {
                p.sendMessage(Message.onlyOwner());
            }
        }));
        refGUI.addButton(new ItemButton(10, Items.get(Component.text("Home -2").color(Colour.deny()), Material.REDSTONE_BLOCK), p -> {
            if (this.canEditGame(p)) {
                this.homeScore = Math.max(0, this.homeScore - 2);
                p.sendMessage(Component.text("Home score decreased by 2, new score: " + this.homeScore).color(Colour.deny()));
                this.updateDisplay();
                this.openRefBookGUI(p);
            } else {
                p.sendMessage(Message.onlyOwner());
            }
        }));
        refGUI.addButton(new ItemButton(11, Items.get(Component.text("Home -1").color(Colour.deny()), Material.REDSTONE_BLOCK), p -> {
            if (this.canEditGame(p)) {
                this.homeScore = Math.max(0, this.homeScore - 1);
                p.sendMessage(Component.text("Home score decreased by 1, new score: " + this.homeScore).color(Colour.deny()));
                this.updateDisplay();
                this.openRefBookGUI(p);
            } else {
                p.sendMessage(Message.onlyOwner());
            }
        }));
        refGUI.addButton(new ItemButton(12, Items.get(Component.text("Home +1").color(Colour.allow()), Material.EMERALD_BLOCK), p -> {
            if (this.canEditGame(p)) {
                ++this.homeScore;
                p.sendMessage(Component.text("Home score increased by 1, new score: " + this.homeScore).color(Colour.allow()));
                this.updateDisplay();
                this.openRefBookGUI(p);
            } else {
                p.sendMessage(Message.onlyOwner());
            }
        }));
        refGUI.addButton(new ItemButton(13, Items.get(Component.text("Home +2").color(Colour.allow()), Material.EMERALD_BLOCK), p -> {
            if (this.canEditGame(p)) {
                this.homeScore += 2;
                p.sendMessage(Component.text("Home score increased by 2, new score: " + this.homeScore).color(Colour.allow()));
                this.updateDisplay();
                this.openRefBookGUI(p);
            } else {
                p.sendMessage(Message.onlyOwner());
            }
        }));
        refGUI.addButton(new ItemButton(14, Items.get(Component.text("Home +3").color(Colour.allow()), Material.EMERALD_BLOCK), p -> {
            if (this.canEditGame(p)) {
                this.homeScore += 3;
                p.sendMessage(Component.text("Home score increased by 3, new score: " + this.homeScore).color(Colour.allow()));
                this.updateDisplay();
                this.openRefBookGUI(p);
            } else {
                p.sendMessage(Message.onlyOwner());
            }
        }));
        refGUI.addButton(new ItemButton(8, Items.get(Component.text("HOME Ball Timeout").color(Colour.partix()), Material.SNOWBALL), p -> {
            if (this.canEditGame(p)) {
                this.forcedPosition(Team.HOME);
                p.sendMessage(Component.text("Ball spawned on HOME side, AWAY pushed away").color(Colour.allow()));
                this.openRefBookGUI(p);
            } else {
                p.sendMessage(Message.onlyOwner());
            }
        }));
        refGUI.addButton(new ItemButton(17, Items.get(Component.text("AWAY Ball Timeout").color(Colour.partix()), Material.SNOWBALL), p -> {
            if (this.canEditGame(p)) {
                this.forcedPosition(Team.AWAY);
                p.sendMessage(Component.text("Ball spawned on AWAY side, HOME pushed away").color(Colour.allow()));
                this.openRefBookGUI(p);
            } else {
                p.sendMessage(Message.onlyOwner());
            }
        }));
        refGUI.addButton(new ItemButton(19, Items.get(Component.text("Away -3").color(Colour.deny()), Material.REDSTONE_BLOCK), p -> {
            if (this.canEditGame(p)) {
                this.awayScore = Math.max(0, this.awayScore - 3);
                p.sendMessage(Component.text("Away score decreased by 3, new score: " + this.awayScore).color(Colour.deny()));
                this.updateDisplay();
                this.openRefBookGUI(p);
            } else {
                p.sendMessage(Message.onlyOwner());
            }
        }));
        refGUI.addButton(new ItemButton(20, Items.get(Component.text("Away -2").color(Colour.deny()), Material.REDSTONE_BLOCK), p -> {
            if (this.canEditGame(p)) {
                this.awayScore = Math.max(0, this.awayScore - 2);
                p.sendMessage(Component.text("Away score decreased by 2, new score: " + this.awayScore).color(Colour.deny()));
                this.updateDisplay();
                this.openRefBookGUI(p);
            } else {
                p.sendMessage(Message.onlyOwner());
            }
        }));
        refGUI.addButton(new ItemButton(21, Items.get(Component.text("Away -1").color(Colour.deny()), Material.REDSTONE_BLOCK), p -> {
            if (this.canEditGame(p)) {
                this.awayScore = Math.max(0, this.awayScore - 1);
                p.sendMessage(Component.text("Away score decreased by 1, new score: " + this.awayScore).color(Colour.deny()));
                this.updateDisplay();
                this.openRefBookGUI(p);
            } else {
                p.sendMessage(Message.onlyOwner());
            }
        }));
        refGUI.addButton(new ItemButton(22, Items.get(Component.text("Away +1").color(Colour.allow()), Material.EMERALD_BLOCK), p -> {
            if (this.canEditGame(p)) {
                ++this.awayScore;
                p.sendMessage(Component.text("Away score increased by 1, new score: " + this.awayScore).color(Colour.allow()));
                this.updateDisplay();
                this.openRefBookGUI(p);
            } else {
                p.sendMessage(Message.onlyOwner());
            }
        }));
        refGUI.addButton(new ItemButton(23, Items.get(Component.text("Away +2").color(Colour.allow()), Material.EMERALD_BLOCK), p -> {
            if (this.canEditGame(p)) {
                this.awayScore += 2;
                p.sendMessage(Component.text("Away score increased by 2, new score: " + this.awayScore).color(Colour.allow()));
                this.updateDisplay();
                this.openRefBookGUI(p);
            } else {
                p.sendMessage(Message.onlyOwner());
            }
        }));
        refGUI.addButton(new ItemButton(24, Items.get(Component.text("Away +3").color(Colour.allow()), Material.EMERALD_BLOCK), p -> {
            if (this.canEditGame(p)) {
                this.awayScore += 3;
                p.sendMessage(Component.text("Away score increased by 3, new score: " + this.awayScore).color(Colour.allow()));
                this.updateDisplay();
                this.openRefBookGUI(p);
            } else {
                p.sendMessage(Message.onlyOwner());
            }
        }));
        refGUI.addButton(new ItemButton(1, Items.get(Component.text("Time -60s").color(Colour.deny()), Material.RED_DYE), p -> {
            if (this.canEditGame(p)) {
                this.gameSeconds = Math.max(0, this.gameSeconds - 1200);
                p.sendMessage(Component.text("Game time decreased by 60 seconds, new time: " + this.getGameTime()).color(Colour.deny()));
                this.updateDisplay();
                this.openRefBookGUI(p);
            } else {
                p.sendMessage(Message.onlyOwner());
            }
        }));
        refGUI.addButton(new ItemButton(2, Items.get(Component.text("Time -30s").color(Colour.deny()), Material.RED_DYE), p -> {
            if (this.canEditGame(p)) {
                this.gameSeconds = Math.max(0, this.gameSeconds - 600);
                p.sendMessage(Component.text("Game time decreased by 30 seconds, new time: " + this.getGameTime()).color(Colour.deny()));
                this.updateDisplay();
                this.openRefBookGUI(p);
            } else {
                p.sendMessage(Message.onlyOwner());
            }
        }));
        refGUI.addButton(new ItemButton(3, Items.get(Component.text("Time -10s").color(Colour.deny()), Material.RED_DYE), p -> {
            if (this.canEditGame(p)) {
                this.gameSeconds = Math.max(0, this.gameSeconds - 200);
                p.sendMessage(Component.text("Game time decreased by 10 seconds, new time: " + this.getGameTime()).color(Colour.deny()));
                this.updateDisplay();
                this.openRefBookGUI(p);
            } else {
                p.sendMessage(Message.onlyOwner());
            }
        }));
        refGUI.addButton(new ItemButton(4, Items.get(Component.text("Time -5s").color(Colour.deny()), Material.RED_DYE), p -> {
            if (this.canEditGame(p)) {
                this.gameSeconds = Math.max(0, this.gameSeconds - 100);
                p.sendMessage(Component.text("Game time decreased by 5 seconds, new time: " + this.getGameTime()).color(Colour.deny()));
                this.updateDisplay();
                this.openRefBookGUI(p);
            } else {
                p.sendMessage(Message.onlyOwner());
            }
        }));
        refGUI.addButton(new ItemButton(5, Items.get(Component.text("Time -3s").color(Colour.deny()), Material.RED_DYE), p -> {
            if (this.canEditGame(p)) {
                this.gameSeconds = Math.max(0, this.gameSeconds - 60);
                p.sendMessage(Component.text("Game time decreased by 3 seconds, new time: " + this.getGameTime()).color(Colour.deny()));
                this.updateDisplay();
                this.openRefBookGUI(p);
            } else {
                p.sendMessage(Message.onlyOwner());
            }
        }));
        refGUI.addButton(new ItemButton(6, Items.get(Component.text("Time -1s").color(Colour.deny()), Material.RED_DYE), p -> {
            if (this.canEditGame(p)) {
                this.gameSeconds = Math.max(0, this.gameSeconds - 20);
                p.sendMessage(Component.text("Game time decreased by 1 second, new time: " + this.getGameTime()).color(Colour.deny()));
                this.updateDisplay();
                this.openRefBookGUI(p);
            } else {
                p.sendMessage(Message.onlyOwner());
            }
        }));
        refGUI.addButton(new ItemButton(27, Items.get(Component.text("Time +1s").color(Colour.allow()), Material.EMERALD), p -> {
            if (this.canEditGame(p)) {
                this.gameSeconds += 20;
                p.sendMessage(Component.text("Game time increased by 1 second, new time: " + this.getGameTime()).color(Colour.allow()));
                this.updateDisplay();
                this.openRefBookGUI(p);
            } else {
                p.sendMessage(Message.onlyOwner());
            }
        }));
        refGUI.addButton(new ItemButton(28, Items.get(Component.text("Time +3s").color(Colour.allow()), Material.EMERALD), p -> {
            if (this.canEditGame(p)) {
                this.gameSeconds += 60;
                p.sendMessage(Component.text("Game time increased by 3 seconds, new time: " + this.getGameTime()).color(Colour.allow()));
                this.updateDisplay();
                this.openRefBookGUI(p);
            } else {
                p.sendMessage(Message.onlyOwner());
            }
        }));
        refGUI.addButton(new ItemButton(29, Items.get(Component.text("Time +5s").color(Colour.allow()), Material.EMERALD), p -> {
            if (this.canEditGame(p)) {
                this.gameSeconds += 100;
                p.sendMessage(Component.text("Game time increased by 5 seconds, new time: " + this.getGameTime()).color(Colour.allow()));
                this.updateDisplay();
                this.openRefBookGUI(p);
            } else {
                p.sendMessage(Message.onlyOwner());
            }
        }));
        refGUI.addButton(new ItemButton(30, Items.get(Component.text("Time +10s").color(Colour.allow()), Material.EMERALD), p -> {
            if (this.canEditGame(p)) {
                this.gameSeconds += 200;
                p.sendMessage(Component.text("Game time increased by 10 seconds, new time: " + this.getGameTime()).color(Colour.allow()));
                this.updateDisplay();
                this.openRefBookGUI(p);
            } else {
                p.sendMessage(Message.onlyOwner());
            }
        }));
        refGUI.addButton(new ItemButton(31, Items.get(Component.text("Time +30s").color(Colour.allow()), Material.EMERALD), p -> {
            if (this.canEditGame(p)) {
                this.gameSeconds += 600;
                p.sendMessage(Component.text("Game time increased by 30 seconds, new time: " + this.getGameTime()).color(Colour.allow()));
                this.updateDisplay();
                this.openRefBookGUI(p);
            } else {
                p.sendMessage(Message.onlyOwner());
            }
        }));
        refGUI.addButton(new ItemButton(32, Items.get(Component.text("Time +60s").color(Colour.allow()), Material.EMERALD), p -> {
            if (this.canEditGame(p)) {
                this.gameSeconds += 1200;
                p.sendMessage(Component.text("Game time increased by 60 seconds, new time: " + this.getGameTime()).color(Colour.allow()));
                this.updateDisplay();
                this.openRefBookGUI(p);
            } else {
                p.sendMessage(Message.onlyOwner());
            }
        }));
        refGUI.addButton(new ItemButton(35, Items.get(Component.text("Back").color(Colour.partix()), Material.BOOK), p -> {
        }));
        refGUI.openInventory(player);
    }

    public void forcedPosition(Team ballSide) {
        this.removeBalls();
        if (ballSide == Team.HOME) {
            Vector pushVelocity = new Vector(-1.25, -1.5, 0.0);
            this.getAwayPlayers().stream().filter(p -> p.getLocation().getX() > this.getCenter().getX()).forEach(p -> {
                p.teleport(p.getLocation().clone().set(this.getCenter().getX(), p.getLocation().getY(), p.getLocation().getZ()));
                p.setVelocity(pushVelocity);
            });
            Ball newBall = this.setBall(BallFactory.create(this.getHomeSpawn(), this.getBallType(), this));
            newBall.setVelocity(0.05, 0.05, 0.0);
            System.out.println("SET VELOCITY 7");
        } else if (ballSide == Team.AWAY) {
            Vector pushVelocity = new Vector(1.25, -1.5, 0.0);
            this.getHomePlayers().stream().filter(p -> p.getLocation().getX() < this.getCenter().getX()).forEach(p -> {
                p.teleport(p.getLocation().clone().set(this.getCenter().getX(), p.getLocation().getY(), p.getLocation().getZ()));
                p.setVelocity(pushVelocity);
            });
            Ball newBall = this.setBall(BallFactory.create(this.getAwaySpawn(), this.getBallType(), this));
            newBall.setVelocity(-0.05, 0.05, 0.0);
            System.out.println("SET VELOCITY 8");
        }
        this.updateDisplay();
    }

    public void setCustomProperty(String key, Object value) {
        this.customProperties.put(key, value);
    }

    public Object getCustomProperty(String key) {
        return this.customProperties.get(key);
    }

    public Object getCustomPropertyOrDefault(String key, Object defaultValue) {
        return this.customProperties.getOrDefault(key, defaultValue);
    }

    public enum Team {
        HOME,
        AWAY,
        SPECTATOR;

        public Team getOppositeTeam() {
            return switch (this) {
                case HOME -> AWAY;
                case AWAY -> HOME;
                case SPECTATOR -> null;
            };
        }
    }

    public enum State {
        PREGAME,
        FACEOFF,
        STOPPAGE,
        REGULATION,
        OVERTIME,
        FINAL,
        OUT_OF_BOUNDS_THROW_WAIT,
        OUT_OF_BOUNDS_THROW,
        INBOUND_WAIT, INBOUND
    }
}

