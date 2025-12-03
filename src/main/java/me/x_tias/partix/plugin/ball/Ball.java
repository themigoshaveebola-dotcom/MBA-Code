package me.x_tias.partix.plugin.ball;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import lombok.Getter;
import lombok.Setter;
import me.x_tias.partix.Partix;
import me.x_tias.partix.mini.basketball.BasketballGame;
import me.x_tias.partix.mini.game.GoalGame;
import me.x_tias.partix.plugin.athlete.Athlete;
import me.x_tias.partix.plugin.athlete.AthleteManager;
import me.x_tias.partix.plugin.athlete.RenderType;
import me.x_tias.partix.plugin.ball.event.BallHitBlockEvent;
import me.x_tias.partix.plugin.ball.event.BallHitEntityEvent;
import me.x_tias.partix.plugin.ball.event.BallRemoveDamagerEvent;
import me.x_tias.partix.plugin.ball.event.BallSetDamagerEvent;
import me.x_tias.partix.plugin.ball.types.Basketball;
import me.x_tias.partix.server.Place;
import me.x_tias.partix.util.Colour;
import me.x_tias.partix.util.Items;
import me.x_tias.partix.util.Packeter;
import me.x_tias.partix.util.UUIDDataType;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;

import java.util.Collection;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

public abstract class Ball {
    public final boolean debug = true;
    private final UUID uuid;
    private final World world;
    @Getter
    private final double stealBallDistance;
    @Getter
    private final double friction;
    private final double weight;
    @Getter
    private final double gravity;
    private final double repulsion;
    @Getter
    private final double balance;
    private final boolean maintainBounceY;
    private final boolean maintainBounceX;
    @Getter
    private final Vector dimensions;
    private final double hitbox;
    @Getter
    private final BallType ballType;
    private final Place place;
    public int packetId;
    private ItemDisplay entity;

    private Color primary;
    private Color secondary;
    @Setter
    @Getter
    private Location location;
    @Getter
    private int repulseDelay;
    @Setter
    @Getter
    private int stealDelay;
    @Getter
    private Vector velocity = new Vector(0.0, 0.1, 0.0);
    @Getter
    private Player lastDamager;
    @Getter
    private Player currentDamager;
    private double currentYaw = 180;
    @Setter
    private boolean locked;

    public Ball(Location location, Place place, BallType ballType, double hitbox, double width, double height, double friction, double gravity, double repulsion, double balance, double weight, boolean maintainBounceY, boolean maintainBounceX, double stealBallDistance, Color primary, Color secondary) {
        this.location = location;
        this.hitbox = hitbox;
        this.place = place;
        this.world = location.getWorld();
        this.friction = friction;
        this.weight = weight;
        this.repulsion = repulsion;
        this.stealBallDistance = stealBallDistance;
        this.gravity = gravity;
        this.primary = primary;
        this.uuid = UUID.randomUUID();
        this.secondary = secondary;
        this.balance = balance;
        this.packetId = -1;
        this.ballType = ballType;
        this.repulseDelay = 0;
        this.stealDelay = 5;
        this.maintainBounceY = maintainBounceY;
        this.maintainBounceX = maintainBounceX;
        this.lastDamager = null;
        this.currentDamager = null;
        this.dimensions = new Vector(width / 2, height, width / 2);

        summonEntity();
    }

    public boolean isValid() {
        return entity != null;
    }

    private void summonEntity() {
        //entity = (Slime) world.spawnEntity(location, EntityType.SLIME);
        entity = (ItemDisplay) world.spawnEntity(location, EntityType.ITEM_DISPLAY);
        entity.setGravity(false);
        entity.setPersistent(false);
        entity.getPersistentDataContainer().set(Partix.getInstance().getBallKey(), UUIDDataType.TYPE, place.getUniqueId());
        //entity.setAI(false);
        //entity.setCollidable(false);
        //entity.setSize(1);
        entity.setInvulnerable(true);
        entity.setSilent(true);
        //entity.playEffect(EntityEffect.HURT);

        //entity.setVisible(false);
        ItemStack ball = new ItemStack(Material.TROPICAL_FISH);
        entity.setItemStack(ball);
        Transformation transformation = entity.getTransformation();
        // taken from previous config
        //double[] ballOffset = {-0.55, -0.25, -0.55};
        //transformation.getTranslation().set(new Vector3f((float) ballOffset[0], (float) ballOffset[1], (float) ballOffset[2]));
        entity.setTransformation(transformation);
        //transformationLeft(entity, new Vector(0, 1, 0), 0.785f);
    }

    public void transformationLeft(Display entity, Vector axis, float angle) {
        entity.setInterpolationDuration(40);
        entity.setInterpolationDelay(-1);
        Transformation transformation = entity.getTransformation();
        transformation.getLeftRotation()
                .set(new AxisAngle4f(angle, (float) axis.getX(), (float) axis.getY(), (float) axis.getZ()));
        entity.setTransformation(transformation);
    }

    private void removeEntity() {
        System.out.println("REMOVE ENTITY");
        this.entity.remove();
        this.entity = null;
    }

    public void changeColors(Color p, Color s) {
        this.primary = p;
        this.secondary = s;
    }

    public void setVelocity(double x, double y, double z) {
        this.velocity.setX(x);
        this.velocity.setY(y);
        this.velocity.setZ(z);
    }

    public void setVelocity(Player player, double x, double y, double z) {
        this.lastDamager = player;
        this.setCurrentDamager(player);
        this.velocity.setX(x);
        this.velocity.setY(y);
        this.velocity.setZ(z);
    }

    protected void setCurrentDamager(Player player) {
        if (stealDelay < 1) {
            // event cancelled
            if (!new BallSetDamagerEvent(player, this).callEvent()) return;

            stealDelay = 10;
            if (currentDamager != null) {
                currentDamager.getInventory().setItem(1, null);
            }
            currentDamager = player;

            player.getInventory().setHeldItemSlot(0);
            player.getInventory().setItem(0, Items.get(Component.text("play.partix.net").color(Colour.partix()), Material.POLISHED_BLACKSTONE_BUTTON));
        }
    }

    public double getHitboxSize() {
        return this.hitbox;
    }

    public void setVelocity(Vector vector) {
        this.velocity = vector;
    }

    public void setVelocity(Player player, Vector vector) {
        this.lastDamager = player;
        this.setCurrentDamager(player);
        this.velocity = vector;
    }

    public void setHorizontal(Vector vector) {
        this.velocity.setX(vector.getX());
        this.velocity.setZ(vector.getZ());
    }

    public void setHorizontal(Location l) {
        this.location.setX(l.getX());
        this.location.setZ(l.getZ());
    }

    public void setHorizontal(Player player, Vector vector) {
        this.lastDamager = player;
        this.setCurrentDamager(player);
        this.velocity.setX(vector.getX());
        this.velocity.setZ(vector.getZ());
    }

    public void setVertical(Vector vector) {
        this.velocity.setY(vector.getY());
    }

    public void setVertical(double v) {
        this.velocity.setY(v);
    }

    public void setVertical(Player player, Vector vector) {
        this.lastDamager = player;
        this.setCurrentDamager(player);
        this.velocity.setY(vector.getY());
    }

    public void setVelocity(Vector horizontal, Vector vertical) {
        this.setHorizontal(horizontal);
        this.setVertical(vertical);
    }

    public void setVelocity(Player player, Vector horizontal, Vector vertical) {
        this.lastDamager = player;
        this.setCurrentDamager(player);
        this.setHorizontal(horizontal);
        this.setVertical(vertical);
    }

    public void setVelocity(Vector horizontal, double vertical) {
        this.setHorizontal(horizontal);
        this.velocity.setY(vertical);
    }

    public void setVelocity(Player player, Vector horizontal, double vertical) {
        this.lastDamager = player;
        this.setCurrentDamager(player);
        this.setHorizontal(horizontal);
        this.velocity.setY(vertical);
    }

    public double getSpeed() {
        return Math.abs(this.velocity.getX()) + Math.abs(this.velocity.getY()) + Math.abs(this.velocity.getZ());
    }

    public UUID getUniqueId() {
        return this.uuid;
    }

    public void setDamager(Player player) {
        if (this.lastDamager != null) {
            this.lastDamager.getInventory().setItem(0, null);
        }
        this.lastDamager = player;
        this.setCurrentDamager(player);
    }

    public void removeCurrentDamager() {
        Player nowDamager = currentDamager;
        if (currentDamager != null) {
            currentDamager.getInventory().setItem(0, null);
        }
        currentDamager = null;

        new BallRemoveDamagerEvent(nowDamager, this).callEvent();
    }

    private void onTick() {
        if (this.repulseDelay > 0) {
            --this.repulseDelay;
        }
        if (this.stealDelay > 0) {
            --this.stealDelay;
        }
    }


    public void move() {
        this.onTick();
        if (this.locked) return;
        Location previous = this.location.clone();
        this.physics();
        Location next = this.location.clone().add(this.velocity);

        // Original raytrace logic for when ball is moving
        if (previous.distance(next) > 0.0) {
            Player player;
            Entity entity;
            RayTraceResult result = this.world.rayTrace(
                    this.location,
                    this.velocity,
                    previous.distance(next) + this.dimensions.getX(),
                    FluidCollisionMode.ALWAYS,
                    false,
                    0.05 + this.dimensions.getX(),
                    Objects::nonNull
            );

            if (result == null) {
                this.endPhysics(next);
                return;
            }

            // Check for entity hits
            if (!(this.repulseDelay >= 1 || result.getHitEntity() == null ||
                    (entity = result.getHitEntity()) instanceof Player &&
                            AthleteManager.get((player = (Player) entity).getUniqueId()).isSpectating())) {

                boolean cont = this.hitEntity(result.getHitPosition().toLocation(this.world), result.getHitEntity());

                if (cont) {
                    this.endPhysics(next);
                } else {
                    this.spawn();
                }
                return;
            }

            // Check for block hits
            if (result.getHitBlock() != null && result.getHitBlockFace() != null) {
                if (!result.getHitBlock().getType().equals(Material.LIGHT)) {
                    this.hitBlock(
                            result.getHitPosition().toLocation(this.world),
                            result.getHitBlock(),
                            result.getHitBlockFace()
                    );
                    this.spawn();
                    return;
                }
                this.endPhysics(next);
                return;
            }
        } else {
            boolean playerTouchingBall = false;
            Player touchingPlayer = null;

            // Get all players within hitbox distance
            double checkRadius = this.getHitboxSize() + 1.0; // Add small buffer for detection
            Collection<Entity> nearbyEntities = this.world.getNearbyEntities(this.location, checkRadius, checkRadius, checkRadius);

            for (Entity entity : nearbyEntities) {
                if (entity instanceof Player) {
                    Player player = (Player) entity;

                    // Skip if player is spectating
                    Athlete athlete = AthleteManager.get(player.getUniqueId());
                    if (athlete != null && athlete.isSpectating()) {
                        continue;
                    }

                    // Skip if player has invisibility
                    if (player.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                        continue;
                    }

                    // Create bounding box for the ball
                    BoundingBox ballBox = BoundingBox.of(
                            this.location.clone().subtract(dimensions.getX(), 0, dimensions.getZ()),
                            this.location.clone().add(dimensions.getX(), dimensions.getY(), dimensions.getZ())
                    );

                    // Get player's bounding box
                    BoundingBox playerBox = player.getBoundingBox();

                    // Check if bounding boxes overlap
                    if (ballBox.overlaps(playerBox)) {
                        // Additional distance check for precision
                        double distance = player.getLocation().distance(this.location);

                        if (debug) {
                        }

                        playerTouchingBall = true;
                        touchingPlayer = player;
                        break;
                    }
                }
            }

            // If a player is touching the ball and the distance check is not valid
            if (playerTouchingBall && touchingPlayer != null) {
                if (place instanceof BasketballGame game) {
                    if (game.getState() == GoalGame.State.OUT_OF_BOUNDS_THROW_WAIT)  return;
                }
                if (debug) {
                }

                // Set the ball as held by the touching player
                this.setCurrentDamager(touchingPlayer);

                // Set ball velocity based on player's direction
                Vector playerDirection = touchingPlayer.getLocation().getDirection();

                // Update ball location to be in front of player
                Location playerLoc = touchingPlayer.getLocation();
                Location ballLoc = playerLoc.clone().add(playerDirection.multiply(1.5));
                ballLoc.setY(ballLoc.getY() + 0.5); // Slightly above ground
                this.location = ballLoc;

                this.spawn();
                return;
            } else {
                // Ball is not moving and no player is touching it
                if (debug) {
                }

                // Check one more time for very close players who might pick up the stationary ball
                for (Entity entity : nearbyEntities) {
                    if (entity instanceof Player) {
                        Player player = (Player) entity;

                        // Skip checks as before
                        Athlete athlete = AthleteManager.get(player.getUniqueId());
                        if (athlete != null && athlete.isSpectating()) {
                            continue;
                        }
                        if (player.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                            continue;
                        }

                        // Very close distance check for pickup
                        double distance = player.getLocation().distance(this.location);
                        if (distance <= this.stealBallDistance) {
                            if (debug) {
                            }
                            this.setCurrentDamager(player);
                            break;
                        }
                    }
                }
            }
        }

        this.endPhysics(next);

        // Remove ball if no players nearby
        if (this.location.getNearbyPlayers(250.0).isEmpty()) {
            this.remove();
        }
    }

    public void endPhysics(Location next) {
        this.location = next;
        this.spawn();
    }

    private boolean hitEntity(Location hitLocation, Entity entity) {
        if (this.currentDamager == null || entity != this.currentDamager) {
            Player player;
            if (entity instanceof Player && (player = (Player) entity).hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                return true;
            }
            BallHitEntityEvent event = new BallHitEntityEvent(this, this.ballType, entity, hitLocation);
            Partix.getInstance().getServer().getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                Location main = entity.getLocation().clone();
                main.setPitch(0.0f);
                main.setYaw(main.getYaw() + (float) (Math.random() * 7.0 - Math.random() * 7.0));
                this.velocity = main.getDirection().multiply(1.0);
                this.repulseDelay = 5;
                return false;
            }
            return true;
        }
        return true;
    }

    private void hitBlock(Location hitLocation, Block block, BlockFace blockFace) {
        BallHitBlockEvent event = new BallHitBlockEvent(this, this.ballType, block, blockFace, hitLocation);
        Partix.getInstance().getServer().getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            Vector normal = new Vector(blockFace.getModX(), blockFace.getModY(), blockFace.getModZ());
            this.location = hitLocation.add(0.0, this.dimensions.getY(), 0.0);
            this.velocity = this.velocity.subtract(normal.multiply(2.0 * this.velocity.dot(normal)));
            BoundingBox box = block.getBoundingBox();
            if (box.contains(this.location.toVector())) {
                if (normal.getX() != 0.0) {
                    this.location.setX(normal.getX() > 0.0 ? box.getMinX() - this.dimensions.getX() / 2.0 : box.getMaxX() + this.dimensions.getX() / 2.0);
                }
                if (normal.getY() != 0.0) {
                    this.location.setY(normal.getY() > 0.0 ? box.getMinY() - this.dimensions.getY() / 2.0 : box.getMaxY() + this.dimensions.getY() / 2.0);
                }
                if (normal.getZ() != 0.0) {
                    this.location.setZ(normal.getZ() > 0.0 ? box.getMinZ() - this.dimensions.getZ() / 2.0 : box.getMaxZ() + this.dimensions.getZ() / 2.0);
                }
            }
            if (blockFace.equals(BlockFace.UP) && !block.getType().equals(Material.BARRIER)) {
                this.bounce();
            } else {
                this.repulse(blockFace, block.getType());
            }
        }
    }

    private void bounce() {
        double y = this.velocity.getY();
        if ((y -= y * this.weight) < this.balance) {
            this.velocity.setY(0.0);
        } else {
            this.velocity.setY(y);
        }
    }

    private void repulse(BlockFace blockFace, Material material) {
        double rep;
        double y;
        if (material.equals(Material.BARRIER)) {
            this.velocity.multiply(0.45);
            y = -0.075;
            rep = Math.abs(repulsion + Math.random());
            repulseDelay = 12;
        } else {
            y = -0.075;
            rep = Math.abs(repulsion);
            repulseDelay = 5;
        }
        switch (blockFace) {
            case NORTH, NORTH_EAST, NORTH_NORTH_EAST, NORTH_NORTH_WEST, NORTH_WEST, SOUTH, SOUTH_EAST, SOUTH_WEST,
                 SOUTH_SOUTH_EAST, SOUTH_SOUTH_WEST -> {
                double z = velocity.getZ();
                z -= (z * (1 - rep));
                velocity.setZ(z);
                if (maintainBounceX) {
                    double x = velocity.getX();
                    x -= (x * (1 - rep));
                    velocity.setX(x);
                }
                if (!maintainBounceY) {
                    velocity.setY(y);
                }
                return;
            }
            case WEST, WEST_NORTH_WEST, WEST_SOUTH_WEST, EAST, EAST_NORTH_EAST, EAST_SOUTH_EAST -> {
                double x = velocity.getX();
                x -= (x * (1 - repulsion));
                velocity.setX(x);
                if (maintainBounceX) {
                    double z = velocity.getZ();
                    z -= (z * (1 - repulsion));
                    velocity.setZ(z);
                }
                if (!maintainBounceY) {
                    velocity.setY(y);
                }
                return;
            }
            default -> {
                velocity.setY(-0.15);
                velocity.setX(velocity.getX() * -1);
                velocity.setZ(velocity.getZ() * -1);
                return;
            }
        }


    }

    private void physics() {
        if (this.isRolling()) {
            this.rolling();
            this.fall();
        } else {
            this.moving();
            this.gravity();
        }
        this.fixVelocity();
    }

    private void rolling() {
        double x = this.velocity.getX();
        double z = this.velocity.getZ();
        x -= x * this.friction;
        z -= z * this.friction;
        this.velocity.setX(x);
        this.velocity.setY(0);
        this.velocity.setZ(z);
    }

    private void moving() {
        double x = this.velocity.getX();
        double z = this.velocity.getZ();
        x -= x * (this.friction - 0.0047);
        z -= z * (this.friction - 0.0047);
        this.velocity.setX(x);
        this.velocity.setZ(z);
    }

    public void sendDebug(String message) {
        this.location.getNearbyPlayers(15.0).forEach(player -> player.sendMessage("[DEBUG] " + message));
    }

    private void gravity() {
        double y = this.velocity.getY();
        if (Math.abs(y) < 0.05) {
            this.fall();
        }
        if (Math.abs(y) > 0.01) {
            if (y > 0.0) {
                y = y < 0.018 ? -0.018 : (y -= y * this.gravity + 0.014);
            } else {
                y += y * this.gravity - 0.014;
                y = Math.max(y, -0.75);
            }
            this.velocity.setY(y);
        }
    }

    private void fall() {
        if (this.getBlockBelow().getType().equals(Material.AIR) || this.getBlockBelow().getType().equals(Material.LIGHT)) {
            this.velocity.setY(Math.min(Math.abs(this.velocity.getY()) * -1.0, -0.025));
        }
    }

    private void fixVelocity() {
        if (Math.abs(this.velocity.getX()) < 0.0075) {
            this.velocity.setX(0);
        }
        if (Math.abs(this.velocity.getY()) < 0.0075) {
            this.velocity.setY(0);
        }
        if (Math.abs(this.velocity.getZ()) < 0.0075) {
            this.velocity.setZ(0);
        }
    }

    public Block getBlockBelow() {
        return this.location.clone().subtract(0.0, this.dimensions.getY(), 0.0).getBlock();
    }

    public boolean isRolling() {
        return Math.abs(this.velocity.getX()) + Math.abs(this.velocity.getY()) > 0.0 && Math.abs(this.velocity.getY()) < 0.05 && !this.getBlockBelow().getType().equals(Material.AIR) && !this.getBlockBelow().getType().equals(Material.LIGHT);
    }

    public void remove() {
        System.out.println("REMOVE BALL 2");
        BallFactory.remove(this);
        removeEntity();
    }


    private void spawn() {
        this.modify();
        if (this.getCurrentDamager() != null) {
            // existing code...
        }

        Location newLocation = location.clone().add(0, 0.2, 0);

        // Calculate the magnitude of velocity
        double speed = Math.sqrt(
                this.velocity.getX() * this.velocity.getX() +
                        this.velocity.getY() * this.velocity.getY() +
                        this.velocity.getZ() * this.velocity.getZ()
        );

        // Scale rotation based on speed, with a minimum threshold
        if (speed > 0.0075) {
            // The faster the ball, the more it rotates
            double rotationAmount = Math.min(speed * 20, 15); // Cap at 15 degrees per tick
            currentYaw -= rotationAmount;

            if (currentYaw < 0f) {
                currentYaw = 180f;
            }
        }

        // Set the rotation
        newLocation.setPitch((float) currentYaw - 90);
        entity.teleport(newLocation);

        PacketContainer remove = null;

        if (packetId > 0) {
            remove = Packeter.removeEntity(packetId);
        }

        packetId = 100000 + new Random().nextInt(899999);
        PacketContainer spawn = Packeter.spawnEntity(packetId, location, EntityType.SLIME);

        for (Player player : location.getNearbyPlayers(100)) {
            Athlete athlete = AthleteManager.get(player.getUniqueId());
            float size = 0.4f;
            if (athlete.getRenderType().equals(RenderType.SLIME)) {
                if (remove != null) {
                    sendPacket(player, remove);
                }
                sendPacket(player, spawn);
                player.spawnParticle(Particle.DUST, location.getX(), location.getY() + (dimensions.getY() / 2), location.getZ(), 1 + (int) (1 + (((dimensions.getX() * 10) * (dimensions.getY() * 10)) * 20)) / 30, dimensions.getX(), dimensions.getY() / 2, dimensions.getZ(), 0, new Particle.DustTransition(primary, Color.GRAY, size));
                player.spawnParticle(Particle.DUST, location.getX(), location.getY() + (dimensions.getY() / 2), location.getZ(), 1 + (int) (1 + (((dimensions.getX() * 10) * (dimensions.getY() * 10))) * 10) / 30, dimensions.getX(), dimensions.getY() / 2, dimensions.getZ(), 0, new Particle.DustTransition(secondary, Color.GRAY, size));
                player.spawnParticle(Particle.DUST, location.getX(), location.getY() + (dimensions.getY() / 2), location.getZ(), 1 + (int) (1 + (((dimensions.getX() * 10) * (dimensions.getY() * 10))) * 20) / 30, dimensions.getX(), dimensions.getY() / 2, dimensions.getZ(), 0, new Particle.DustTransition(primary, Color.GRAY, size));
            } else if (athlete.getRenderType().equals(RenderType.REMOVE_SLIME)) {
                if (remove != null) {
                    sendPacket(player, remove);
                }
                player.spawnParticle(Particle.DUST, location.getX(), location.getY() + (dimensions.getY() / 2), location.getZ(), 1 + (int) (1 + (((dimensions.getX() * 10) * (dimensions.getY() * 10)) * 20)) / 10, dimensions.getX(), dimensions.getY() / 2, dimensions.getZ(), 0, new Particle.DustTransition(primary, Color.GRAY, size));
                player.spawnParticle(Particle.DUST, location.getX(), location.getY() + (dimensions.getY() / 2), location.getZ(), 1 + (int) (1 + (((dimensions.getX() * 10) * (dimensions.getY() * 10))) * 10) / 10, dimensions.getX(), dimensions.getY() / 2, dimensions.getZ(), 0, new Particle.DustTransition(secondary, Color.GRAY, size));
                player.spawnParticle(Particle.DUST, location.getX(), location.getY() + (dimensions.getY() / 2), location.getZ(), 1 + (int) (1 + (((dimensions.getX() * 10) * (dimensions.getY() * 10))) * 20) / 10, dimensions.getX(), dimensions.getY() / 2, dimensions.getZ(), 0, new Particle.DustTransition(primary, Color.GRAY, size));
            } else {
                player.spawnParticle(Particle.DUST, location.getX(), location.getY() + (dimensions.getY() / 2), location.getZ(), (int) (1 + (((dimensions.getX() * 10) * (dimensions.getY() * 10)) * 20)), dimensions.getX(), dimensions.getY() / 2, dimensions.getZ(), 0, new Particle.DustTransition(primary, Color.GRAY, size));
                player.spawnParticle(Particle.DUST, location.getX(), location.getY() + (dimensions.getY() / 2), location.getZ(), (int) (1 + (((dimensions.getX() * 10) * (dimensions.getY() * 10))) * 10), dimensions.getX(), dimensions.getY() / 2, dimensions.getZ(), 0, new Particle.DustTransition(secondary, Color.GRAY, size));
                player.spawnParticle(Particle.DUST, location.getX(), location.getY() + (dimensions.getY() / 2), location.getZ(), (int) (1 + (((dimensions.getX() * 10) * (dimensions.getY() * 10))) * 20), dimensions.getX(), dimensions.getY() / 2, dimensions.getZ(), 0, new Particle.DustTransition(primary, Color.GRAY, size));
            }
        }

    }


    private void sendPacket(Player player, PacketContainer packet) {
        ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
    }

    public abstract void modify();

    public abstract Component getControls(Player player);
    }

