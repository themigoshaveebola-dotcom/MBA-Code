package me.x_tias.partix.plugin.ball.types;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
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
    private boolean perfectShot = false;
    @Getter
    private UUID lastOwnerUUID = null;
    private long perfectShotStartTime = 0L;
    private int ownerTicks;
    private int handYaw = 50;
    private int handModifier = 5;
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
        String swapHand = "Crossover";
        Component lc = Component.text("[", Colour.blackBorder()).append(Component.keybind("key.attack", Colour.border()).append(Component.text("]", Colour.blackBorder())).append(Component.text(" " + leftClick + ", ", Colour.darkBorder())));
        Component rc = Component.text("[", Colour.blackBorder()).append(Component.keybind("key.use", Colour.border()).append(Component.text("]", Colour.blackBorder())).append(Component.text(" " + rightClick + ", ", Colour.darkBorder())));
        Component di = Component.text("[", Colour.blackBorder()).append(Component.keybind("key.drop", Colour.border()).append(Component.text("]", Colour.blackBorder())).append(Component.text(" " + dropItem + ", ", Colour.darkBorder())));
        Component sh = Component.text("[", Colour.blackBorder()).append(Component.keybind("key.swapOffhand", Colour.border()).append(Component.text("]", Colour.blackBorder())).append(Component.text(" " + swapHand + ", ", Colour.darkBorder())));
        return lc.append(rc).append(di).append(sh);
    }

    public void throwBall(Player player) {
        if (this.getCurrentDamager() != null && this.getCurrentDamager() == player) {
            if (!player.isOnGround()) {
                this.executeThrow(player);
            } else {
                player.sendMessage("§cYou must be in the air to shoot!");
            }
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

    private void executeThrow(Player player) {
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
        updateGreenWindow(distanceToHoop); // Final calculation of green window size

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

            // is handled above. This is old code :)
//            this.threeEligible = material.equals(Material.OAK_PLANKS) || material.equals(Material.SPRUCE_PLANKS) || material.equals(Material.LIGHT_BLUE_TERRACOTTA);
            block.setType(this.threeEligible ? Material.RED_CONCRETE : Material.YELLOW_CONCRETE);
            Bukkit.getScheduler().runTaskLater(Partix.getInstance(), () -> block.setType(material), 22L);
            break;
        }

        // used to be 0 but I've never actually gotten a 0 accuracy
        float yaw = player.getLocation().getYaw();
        // Only apply inaccuracy if outside the green zone

        if (this.accuracy < distanceZone.getRequiredGreenAccuracy()) {
            float randomYawOffset = (float)(12 - this.accuracy) * 1.5f; // The more 'red', the wider the miss
            yaw += (new Random().nextBoolean() ? randomYawOffset : -randomYawOffset);
        }
        th.setYaw(yaw);

        // Determine which zone the player's accuracy landed in.
        boolean perfect = false;
        if (distanceToHoop <= 24 && this.shotContestPercentage < 0.45) {
            if (this.accuracy >= distanceZone.getRequiredGreenAccuracy()) {
                perfect = true; // Green
            } else if (this.accuracy >= distanceZone.getYellowAccuracyStart()) {
                perfect = Math.random() <= 0.35;
            } else {
                perfect = Math.random() <= 0.05;
            }
        }

        final String percentage = String.format("%.2f", this.shotContestPercentage * 100.0f);
        player.showTitle(Title.title(
                Component.empty(),
                MiniMessage.miniMessage().deserialize("<green>" + percentage + "% Contested"),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ofMillis(350L))
        ));

        if (perfect) { // max accuracy
            this.perfectShot = true;
            this.perfectShotStartTime = System.currentTimeMillis();
        } else {
            this.perfectShot = false;
        }

        this.game.onShotAttempt(player, this.threeEligible);
        if (this.perfectShot) {
            player.sendMessage("Perfect shot");
            Location loc1 = playerLoc.clone();
            Location loc2 = targetHoop.clone();

            double midX = (loc1.getX() + loc2.getX()) / 2;
            double midY = ((loc1.getY() + loc2.getY()) / 2) + 7.5;
            double midZ = (loc1.getZ() + loc2.getZ()) / 2;

            final Location p1 = new Location(loc1.getWorld(), midX, midY, midZ);
            final List<Location> bezierPoints = bezierCurve(100, loc1, p1, loc2);

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

                    int amountToIncrement = Math.max(4, (index / 25) + 1);

                    setVelocity(new Vector());
                    endPhysics(bezierPoints.get(index));
                    index += amountToIncrement;
                }
            }.runTaskTimer(Partix.getInstance(), 1L, 1L);
        }else{
            Vector vector = th.getDirection().normalize().multiply(0.3875 + (1.0 - (double) pitch / 45.0) / 2.25);
            double ySpeed = 0.366;
            if (this.game.getCourtLength() == 32.0) {
                vector.multiply(1.1);
            }
            this.setVelocity(player, vector, ySpeed);
        }

        this.lastOwnerUUID = player.getUniqueId();
        System.out.println("SET VELOCITY 10");
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

        double closestDefenderDistanceSq = Double.MAX_VALUE;

        final double contestRadius = 5.0; // The max distance a defender can be to contest

        for (Player onlinePlayer : shooter.getWorld().getPlayers()) {
            GoalGame.Team onlinePlayerTeam = game.getTeamOf(onlinePlayer);

            if (onlinePlayerTeam != GoalGame.Team.SPECTATOR) { // Make sure we don't check spectators
                GoalGame.Team playerTeam = game.getTeamOf(onlinePlayer);

                // Check if player is on the other team
                if (playerTeam != null && !playerTeam.equals(shooterTeam)) {
                    double distSq = onlinePlayer.getLocation().distanceSquared(shooter.getLocation());
                    if (onlinePlayer.getLocation().getY() > (game.getArenaBox().getMinY() + 2.65)) {
                        distSq /= 2;
                        System.out.println("Enhanced contest!");
                    }

                    if (distSq < closestDefenderDistanceSq) {
                        closestDefenderDistanceSq = distSq;
                    }
                }
            }
        }

        double closestDefenderDistance = Math.sqrt(closestDefenderDistanceSq);

        if (closestDefenderDistance > contestRadius) {
            return 0.0; // Defender is too far to contest
        }

        // The closer the defender, the higher the contest.
        // A defender at 0 distance is 1.0 (100%) contest.
        // A defender at contestRadius distance is 0.0 (0%) contest.
        return 1.0 - (closestDefenderDistance / contestRadius);
    }

    public void forceThrow() {
        if (this.getCurrentDamager() != null) {
            Player player = this.getCurrentDamager();
            this.setLocation(player.getEyeLocation().add(player.getLocation().getDirection().multiply(0.45)));
            this.setVelocity(player, player.getLocation().getDirection().multiply(0.6));
            System.out.println("SET VELOCITY 11");
            this.giveaway();
            this.threeEligible = false;
            this.delay = 10;
            this.giveaway();
        }
    }

    public boolean pass(Player player) {
        if (this.delay < 1 && this.getCurrentDamager() != null && this.getCurrentDamager().equals(player)) {
            Location location = player.getEyeLocation().clone();
            location.subtract(0.0, 0.5, 0.0);
            Vector direction = player.getLocation().getDirection().clone();
            if (player.isSneaking()) {
                direction.setX(-direction.getX());
                direction.setZ(-direction.getZ());
            }
            double passSpeed = 0.915;
            if (this.game.getCourtLength() == 32.0) {
                passSpeed *= 1.1;
            }
            this.setLocation(location.add(direction.clone().multiply(0.45)));
            this.setVelocity(player, direction.clone().multiply(passSpeed), 0.09);
            System.out.println("SET VELOCITY 12");
            this.game.startAssistTimer(player.getUniqueId());
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
        return player.getLocation().getY() > this.game.getCenter().getY() + 0.333 && dis > 0.525 && dis < 1.725;
    }

    public boolean dunk(Player player) {
        if (this.getCurrentDamager() != null && this.getCurrentDamager().equals(player)) {
            if (!this.isShotAttemptRegistered() && (this.game.getState().equals(GoalGame.State.REGULATION) || this.game.getState().equals(GoalGame.State.OVERTIME))) {
                this.game.onShotAttempt(player, false);
                this.setShotAttemptRegistered(true);
            }
            Location target = this.getTargetHoop(player);
            updateGreenWindow(0); // Pass distance 0 for a dunk.

            // Determine which zone the player's accuracy landed in.
            int zone; // 0 for Green, 1 for Yellow, 2 for Red
            if (this.accuracy >= distanceZone.getRequiredGreenAccuracy()) {
                zone = 0; // Green
            } else if (this.accuracy >= distanceZone.getYellowAccuracyStart()) {
                zone = 1; // Yellow
            } else {
                zone = 2; // Red
            }

            if (this.canDunk(player)) {
                double a;
                if (zone == 0) { // Green release, same as old 'acc == 0'
                    a = 0.05;
                } else if (zone == 1) { // Yellow release, similar to old 'acc <= 1' or 'acc == 2'
                    a = new Random().nextBoolean() ? 0.75 : 0.85;
                } else { // Red release, same as old 'acc == 3' or higher
                    a = 3.0;
                }

                Location slam = target.clone();
                Location location = player.getLocation().clone();
                location.setPitch(0.0f);
                slam.subtract(target.clone().subtract(player.getLocation()).multiply(0.33));
                Vector fly = player.getLocation().getDirection().multiply(-1.08);
                fly.setY(0.55);
                player.setVelocity(fly);
                this.setLocation(target.clone().add(0.0, 0.85, 0.0));
                this.setVelocity(player, player.getLocation().getDirection().normalize().multiply(player.getLocation().distance(this.getTargetHoop(player)) < 6.75 ? a : 0.0105), -0.2);
                System.out.println("SET VELOCITY 13");
            } else {
                // This is for a layup (when canDunk is false)

                double a;
                if (zone == 0) { // Green release
                    a = 0.25;
                } else if (zone == 1) { // Yellow release
                    a = new Random().nextBoolean() ? 0.265 : 0.27;
                } else { // Red release
                    a = new Random().nextBoolean() ? 0.833 : 0.866;
                }

                this.setLocation(player.getEyeLocation().add(player.getLocation().getDirection().multiply(1.425)));
                this.setVelocity(player, player.getLocation().getDirection().normalize().multiply(player.getLocation().distance(this.getTargetHoop(player)) < 6.75 ? a : 0.0105), 0.335);
                System.out.println("SET VELOCITY 14");
            }
            this.threeEligible = false;
            this.giveaway();
            this.delay = 15;
            return true;
        }
        return false;
    }

    public boolean crossover(Player player) {
        int nextHand;
        if (this.getCurrentDamager() != null && Math.abs(player.getVelocity().getY()) < 0.1 && player.isOnGround() && this.getCurrentDamager().equals(player) && ((nextHand = this.handYaw + this.handModifier) >= 49 || nextHand <= -49)) {
            if (this.handModifier == 0) {
                this.handModifier = 5;
                player.setVelocity(Position.stabilize(player, 70.0f, 0.0).getDirection().multiply(0.75));
                this.delay = 10;
            } else if (this.handModifier == 5) {
                this.handModifier = -5;
                this.delay = 10;
                player.setVelocity(Position.stabilize(player, -70.0f, 0.0).getDirection().multiply(0.75));
            } else if (this.handModifier == -5) {
                this.delay = 10;
                player.setVelocity(Position.stabilize(player, 70.0f, 0.0).getDirection().multiply(0.75));
                this.handModifier = 5;
            }
            return true;
        }
        return false;
    }

    public void giveaway() {
        this.removeCurrentDamager();
    }

    private void runStealImmunity() {
        if (this.stealImmunityTicks > 0) {
            --this.stealImmunityTicks;
        }
    }

    public void takeBall(Player player) {
        if (this.stealImmunityTicks > 0 && this.getCurrentDamager() != null && !this.getCurrentDamager().equals(player)) {
            this.error(player);
            return;
        }
        if (this.delay < 1 && this.getStealDelay() < 1) {
            if (this.getLocation().getY() > player.getEyeLocation().getY() - 0.1 && this.getVelocity().getY() < 0.0 && this.getCurrentDamager() == null) {
                this.error(player);
                this.setStealDelay(10);
                return;
            }
            this.steal(player);
            if (this.isReboundEligible()) {
                this.setReboundEligible(false);
                if (this.game.getState().equals(GoalGame.State.REGULATION) || this.game.getState().equals(GoalGame.State.OVERTIME)) {
                    PlayerStats stats = this.game.getStatsManager().getPlayerStats(player.getUniqueId());
                    stats.incrementRebounds();
                    player.sendMessage(Component.text("Rebound!").color(Colour.allow()));
                    System.out.println("Debug: " + player.getName() + " grabbed a rebound! Total rebounds: " + stats.getRebounds());
                }
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
            }
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 10, 1));
        } else {
            this.error(player);
        }
    }

    public void error(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_ARMOR_STAND_BREAK, SoundCategory.MASTER, 100.0f, 1.0f);
    }

    private void steal(Player player) {
        if (!BallFactory.hasBall(player)) {
            if (this.getCurrentDamager() == null) {
                this.setDamager(player);
                this.stealImmunityTicks = 20;
                this.setStealDelay(10);
                this.delay = 10;
                this.accuracy = 0;
                this.threeEligible = false;
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.MASTER, 100.0f, 1.2f);
                player.getInventory().setHeldItemSlot(0);
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 10, 0));
                return;
            }
            if (this.getCurrentDamager() != player) {
                Player oldOwner = this.getCurrentDamager();
                if ((this.game.getState().equals(GoalGame.State.REGULATION) || this.game.getState().equals(GoalGame.State.OVERTIME)) && this.game.getHomePlayers().contains(oldOwner) != this.game.getHomePlayers().contains(player)) {
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
                        this.removeCurrentDamager();
                        this.setVelocity(player.getEyeLocation().getDirection().multiply(0.2));
                        player.sendMessage("Ball poked!");
                        this.accuracy = 0;
                    } else {
                        this.setDamager(player);
                        this.stealImmunityTicks = 20;
                        this.accuracy = 0;
                        player.getInventory().setHeldItemSlot(0);
                        player.sendMessage("Snatched the ball!");
                        this.threeEligible = false;
                    }
                }
                this.setStealDelay(10);
                this.delay = 10;
            }
        }
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

        final double floorY = game.getArenaBox().getMinY() + 2.5; // arenas seem to be offset by 2.5
        if (player.getLocation().getY() > floorY && this.getLocation().getY() > floorY + 0.5) {
            Location main = this.getLocation().clone();
            main.setPitch(0.0f);
            main.setYaw(main.getYaw() + (float)(Math.random() * 7.0 - Math.random() * 7.0));
            this.setVelocity(main.getDirection().multiply(-1.0));

            System.out.println("Blocked!");
            return false;
        }

        if (this.getLastDamager() != null) {
            if (this.getStealDelay() > 0) {
                return true;
            }
            if (this.delay < 1 || this.getLastDamager() != null) {
                this.setStealDelay(0);
                if (this.getLastDamager() != player) {
                    this.setDamager(player);
                    this.stealImmunityTicks = 20;
                    this.setStealDelay(10);
                    this.delay = 10;
                    return true;
                }
                if (this.delay < 1) {
                    this.setDamager(player);
                    this.stealImmunityTicks = 20;
                    this.setStealDelay(10);
                    this.delay = 10;
                    return true;
                }
            } else if (this.getLastDamager() != player) {
                this.setStealDelay(0);
                this.setDamager(player);
                this.stealImmunityTicks = 20;
                this.setStealDelay(10);
                this.delay = 10;
                return true;
            }
        }

        this.setDamager(player);
        this.stealImmunityTicks = 20;
        return true;
    }

    @Override
    public void setDamager(Player player) {
        this.game.setStepbacked(null);
        super.setDamager(player);
        this.setCurrentDamager(player);
    }

    private void detectTravel() {
        Player damager;
        if (this.delay < 1 && this.stealImmunityTicks < 1 && (damager = this.getCurrentDamager()) != null && !damager.isOnGround() && damager.getVelocity().getY() < -0.19) {
            damager.sendTitlePart(TitlePart.TITLE, Component.text(" "));
            damager.sendTitlePart(TitlePart.SUBTITLE, Component.text("Travel!").style(Style.style(Colour.deny(), TextDecoration.BOLD)));
            this.forceThrow();
            if (this.game.inboundingActive && damager.equals(this.game.inbounder)) {
                this.game.dropInboundBarrierButKeepClockFrozen();
            }
        }
    }

    private void runDelay() {
        if (this.delay > 0) {
            --this.delay;
        }
    }

    private void modifyHand() {
        int nextHand = this.handYaw + this.handModifier;
        if (nextHand < 50 && nextHand > -50) {
            this.handYaw = nextHand;
        }
    }

    public void markReboundEligible() {
        this.reboundEligible = true;
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

            // Calculate the dynamic green window size based on player's current distance
            double distance = this.getCurrentDamager().getLocation().distance(targetHoop);
            updateGreenWindow(distance); // This method now just sets greenWindowSize

            String unicode = Partix.getInstance().getUnicodeConfig().getConfig().getString(distanceZone.name() + "." + this.accuracy);
            TextComponent a = Component.text(unicode, NamedTextColor.WHITE).shadowColor(ShadowColor.none());

            // Send title to player (your original code)
            this.getCurrentDamager().sendTitlePart(TitlePart.TITLE, a);
            this.getCurrentDamager().sendTitlePart(TitlePart.SUBTITLE, Component.text("   "));
            this.getCurrentDamager().sendTitlePart(TitlePart.TIMES, Title.Times.times(Duration.ofMillis(0L), Duration.ofMillis(100L), Duration.ofMillis(350L)));
        }
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

    @Override
    public void modify() {
        ScreenManager.INSTANCE.tickActiveScreens(this.game);

//        System.out.println("Perfect shot: " + this.perfectShot);
//        System.out.println("Last owner UUID: " + (this.lastOwnerUUID != null ? this.lastOwnerUUID.toString() : "null"));
//        System.out.println("Elapsed time: " + (System.currentTimeMillis() - this.perfectShotStartTime) + " ms");
//        System.out.println("Current damager: " + (this.getCurrentDamager() != null ? this.getCurrentDamager().getName() : "null"));

        if (this.getCurrentDamager() != null) {
            this.nextAccuracy();
            this.displayAccuracy();
            Player poss = this.getCurrentDamager();
            if (poss == null) return;
            Location l = Position.stabilize(poss, this.handYaw, 0.75);
            l.setY(poss.getEyeLocation().getY() - 0.75);
            double bounceSpeed = Math.max((0.234 + (Math.abs(poss.getVelocity().getX()) + Math.abs(poss.getVelocity().getZ())) * 2.0) * -1.0, -0.75);
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
            this.perfectShot = false;
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
        if (this.ownerTicks < 405) {
            this.ownerTicks = 405;
        }
        this.runStealImmunity();
        this.checkReboundEligibility();
        this.detectReboundPickup();
        this.detectMissedShotBySlab();
        this.checkSlabZone();
        this.runDelay();
    }

    public void clearPerfectShot() {
        this.perfectShot = false;
        this.lastOwnerUUID = null;
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

    @Setter
    @Getter
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
}