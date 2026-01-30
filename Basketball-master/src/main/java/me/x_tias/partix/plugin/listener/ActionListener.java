/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.bukkit.GameMode
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.HumanEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.entity.EntityDamageByEntityEvent
 *  org.bukkit.event.inventory.InventoryInteractEvent
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitRunnable
 */
package me.x_tias.partix.plugin.listener;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import me.x_tias.partix.Partix;
import me.x_tias.partix.mini.basketball.BasketballGame;
import me.x_tias.partix.mini.game.GoalGame;
import me.x_tias.partix.plugin.athlete.Athlete;
import me.x_tias.partix.plugin.athlete.AthleteManager;
import me.x_tias.partix.plugin.ball.Ball;
import me.x_tias.partix.plugin.ball.BallFactory;
import me.x_tias.partix.plugin.ball.BallType;
import me.x_tias.partix.plugin.ball.event.*;
import me.x_tias.partix.plugin.ball.types.Basketball;
import me.x_tias.partix.plugin.mechanic.Mechanic;
import me.x_tias.partix.plugin.rightclick.events.PlayerRightClickStartHoldEvent;
import me.x_tias.partix.plugin.rightclick.events.PlayerRightClickStopHoldEvent;
import me.x_tias.partix.server.Place;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ActionListener
        implements Listener {
    private static final Map<UUID, Long> recentDashes = new HashMap<>();
    private static final long DASH_JUMP_LOCK_MS = 500; // 0.5 seconds
    
    @EventHandler
    public void onHitBall(PlayerHitBallEvent e) {
        Ball ball = e.getBall();
        Player player = e.getPlayer();
        if (AthleteManager.get(player.getUniqueId()).isSpectating()) {
            return;
        }
        Mechanic.punch(player, ball);
    }


    @EventHandler
    public void onRightClick(PressRightClickEvent e) {
        Player player = e.getPlayer();
        Athlete athlete = e.getAthlete();
        if (athlete.getPlace() != null) {
            athlete.getPlace().clickItem(player, e.getItemStack());
        }
    }

    private final Map<UUID, Basketball> activeDunkMeters = new HashMap<>();
    
    @EventHandler
    public void onRightClickStart(PlayerRightClickStartHoldEvent event) {
        final Player player = event.getPlayer();
        final Athlete athlete = AthleteManager.get(player.getUniqueId());
        if (athlete.isSpectating()) {
            return;
        }

        // When player STARTS holding right-click (meter starts), calculate contest for jump shots
        for (Ball ball : BallFactory.getNearby(player.getLocation(), 3.5)) {
            if (!(ball instanceof Basketball basketball)) continue;
            if (basketball.getCurrentDamager() == null || !basketball.getCurrentDamager().equals(player)) continue;
            
            // Only calculate contest at meter start if player will be shooting (in air)
            // This captures the contest at the START of the shot meter
            basketball.calculateAndStoreContestAtMeterStart(player);
            break;
        }
    }
    
    @EventHandler
    public void onRightClickRelease(PlayerRightClickStopHoldEvent event) {
        final Player player = event.getPlayer();
        final Athlete athlete = AthleteManager.get(player.getUniqueId());
        if (athlete.isSpectating()) {
            return;
        }

        boolean isInBlock;
        try {
            RayTraceResult result = player.getWorld().rayTraceBlocks(player.getEyeLocation(), player.getLocation().getDirection(), 0.5, FluidCollisionMode.NEVER, false);
            isInBlock = result != null && result.getHitBlock() != null;
        } catch (Exception ex) {
            isInBlock = false;
        }

        for (Ball ball : BallFactory.getNearby(player.getLocation(), 3.5)) {
            if (ball.getCurrentDamager() == null || !ball.getCurrentDamager().equals(player)) continue;
            Mechanic.rightClick(player, ball, isInBlock);
            break;
        }
    }

    @EventHandler
    public void onLeftClick(PressLeftClickEvent e) {
        Player player = e.getPlayer();
        if (!e.getAthlete().isSpectating()) {
            // ===== LAYUP BLOCK DETECTION (check first, before other actions) =====
            for (Ball ball : BallFactory.getNearby(player.getLocation(), 5.0)) {
                if (ball instanceof Basketball basketball) {
                    // Check if this is a layup in flight
                    if (basketball.isLayupAttempt && basketball.getCurrentDamager() == null) {
                        // Player clicked near a layup in flight - attempt block
                        basketball.registerLayupBlockAttempt(player);
                        // Don't return here - let other block attempts register too
                    }

                    // Register block attempt for dunks (existing code)
                    basketball.registerDefenderBlockAttempt(player);

                    // FIXED: Use correct variable names
                    if (basketball.getCurrentDamager() != null) {
                        basketball.onDefenderClickedBallHandler(player, basketball.getCurrentDamager());
                    }
                }
            }

            // ===== BUFFED STEAL RANGE: Now 4.5 blocks instead of 2.3 =====
            BallFactory.getNearest(player.getLocation(), 2.67).ifPresent(ball ->
                    Mechanic.leftClick(player, ball, e.isThrownInBlock()));
        }
    }

    @EventHandler
    public void onBallHitPlayer(BallHitEntityEvent e) {
        Entity entity = e.getEntity();
        if (entity instanceof Player player) {
            e.setCancelled(Mechanic.collides(player, e.getBall()));
            return;
        }
        e.setCancelled(true);
    }

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();

        for (Ball ball : BallFactory.getNearby(player.getLocation(), 8.0)) {
            if (!(ball instanceof Basketball basketball)) {
                continue;
            }

            // Verify player is the current damager (has possession)
            if (basketball.getCurrentDamager() == null || !basketball.getCurrentDamager().equals(player)) {
                continue;
            }

            // Cancel the swap hands event and toggle pass mode instead
            event.setCancelled(true);
            basketball.togglePassMode(player);
            break;
        }
    }

    @EventHandler
    public void onPlayerDropItemEvent(PressDropKeyEvent e) {
        Player player = e.getPlayer();

        for (Ball ball : BallFactory.getNearby(player.getLocation(), 4.0)) {
            Basketball bball;
            if (!(ball instanceof Basketball) || (bball = (Basketball) ball).getCurrentDamager() == null) continue;

            // REMOVED the canDunk() check here - let dunk() handle validation
            if (bball.dunk(player)) {
                // Successfully handled (either started meter or executed dunk)
                e.setKeepItem(false);
                return;
            } else {
                // NOT dunking - play door sound
                player.playSound(player.getLocation(), Sound.BLOCK_WOODEN_DOOR_OPEN, 1.0f, 1.0f);
            }

            e.setKeepItem(false);
            return;
        }
    }


    @EventHandler
    public void onSwitchItem(PlayerItemHeldEvent event) {
        int slot = event.getNewSlot();
        Player player = event.getPlayer();

        if (slot == 3) {
            // BTB (Behind-the-Back) on slot 3 (hotkey 4)
            for (Ball ball : BallFactory.getNearby(player.getLocation(), 4.0)) {
                if (!(ball instanceof Basketball basketball)) continue;

                if (basketball.getCurrentDamager() == null || !basketball.getCurrentDamager().equals(player)) continue;

                event.setCancelled(true);
                basketball.behindTheBack(player);
                break;
            }
        } else if (slot == 4) {
            // Hesi on slot 4 (hotkey 5)
            for (Ball ball : BallFactory.getNearby(player.getLocation(), 4.0)) {
                if (!(ball instanceof Basketball basketball)) continue;

                if (basketball.getCurrentDamager() == null || !basketball.getCurrentDamager().equals(player)) continue;

                event.setCancelled(true);
                basketball.hesi(player);
                break;
            }
        } else if (slot == 5) {
            // Crossover on slot 5 (hotkey 6)
            for (Ball ball : BallFactory.getNearby(player.getLocation(), 4.0)) {
                if (!(ball instanceof Basketball basketball)) continue;

                if (basketball.getCurrentDamager() == null || !basketball.getCurrentDamager().equals(player)) continue;

                event.setCancelled(true);
                basketball.crossover(player);
                break;
            }
        } else if (slot == 6) {
            // ===== HOTKEY 7: Stepback (with ball) OR Dash (defense without ball) =====
            boolean actionTaken = false;
            
            // First check: Stepback if player has the ball
            for (Ball ball : BallFactory.getNearby(player.getLocation(), 4.0)) {
                if (!(ball instanceof Basketball basketball)) continue;

                if (basketball.getCurrentDamager() != null && basketball.getCurrentDamager().equals(player)) {
                    event.setCancelled(true);
                    basketball.stepback(player);
                    actionTaken = true;
                    break;
                }
            }
            
            // Second check: Dash if no ball (defense only, handled in dash() method)
            if (!actionTaken) {
                for (Ball ball : BallFactory.getNearby(player.getLocation(), 100.0)) {
                    if (!(ball instanceof Basketball basketball)) continue;

                    // Always cancel event and switch back, regardless of success
                    event.setCancelled(true);
                    if (basketball.dash(player)) {
                        // Track dash time to prevent jump exploit
                        recentDashes.put(player.getUniqueId(), System.currentTimeMillis());
                    }
                    
                    // Auto-switch back to slot 1
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (player.isOnline()) {
                                player.getInventory().setHeldItemSlot(0);
                            }
                        }
                    }.runTaskLater(Partix.getInstance(), 1L);
                    
                    return;
                }
            }
        } else if (slot == 1 || slot == 2) {
            // ===== TRACK PASS HOTKEYS: Slots 1 & 2 (Hotkeys 2 & 3) =====
            int hotkey = (slot == 1) ? 2 : 3;

            // Track Pass hotkey pressed (2 or 3)
            for (Ball ball : BallFactory.getNearby(player.getLocation(), 4.0)) {
                if (!(ball instanceof Basketball basketball)) continue;

                if (basketball.getCurrentDamager() == null || !basketball.getCurrentDamager().equals(player)) continue;

                // Get teammate assigned to this hotkey
                Player teammate = basketball.getTeammateByHotkey(hotkey, player);

                if (teammate == null) {
                    // Determine error message based on situation
                    GoalGame.Team playerTeam = basketball.getGame().getTeamOf(player);
                    if (playerTeam == null) {
                        player.sendActionBar(Component.text("Error getting team!").color(NamedTextColor.RED));
                        event.setCancelled(true);
                        return;
                    }

                    java.util.List<Player> teamPlayers = playerTeam == GoalGame.Team.HOME
                            ? basketball.getGame().getHomePlayers()
                            : basketball.getGame().getAwayPlayers();

                    if (teamPlayers.size() <= 1) {
                        player.sendActionBar(Component.text("No teammates to pass to!").color(NamedTextColor.RED));
                    } else if (teamPlayers.size() == 2) {
                        // Only 1 teammate, only slot 1 is valid
                        if (hotkey == 3) {
                            player.sendActionBar(Component.text("You only have 1 teammate! Use Key 2").color(NamedTextColor.RED));
                        } else {
                            player.sendActionBar(Component.text("Teammate not found!").color(NamedTextColor.RED));
                        }
                    } else {
                        player.sendActionBar(Component.text("Teammate not assigned to this key!").color(NamedTextColor.RED));
                    }
                    event.setCancelled(true);
                    return;
                }

                // Execute track pass
                event.setCancelled(true);
                basketball.trackPass(player, teammate);
                break;
            }
        }
    }

    @EventHandler
    public void onAttackWithBall(EntityDamageByEntityEvent e) {
        e.setCancelled(true);
        Object object = e.getDamager();
        if (object instanceof Player attacker) {
            object = e.getEntity();
            if (object instanceof Player damaged) {
                if (attacker.getLocation().distance(damaged.getLocation()) < 2.5) {
                    for (Ball ball : BallFactory.getNearby(attacker.getLocation(), 4.0)) {
                        if (ball.getCurrentDamager() == null) continue;
                        Mechanic.attack(attacker, damaged, ball);
                    }
                }
            }
        }
    }

    @EventHandler
    public void changeItems(InventoryInteractEvent e) {
        Player player;
        HumanEntity humanEntity = e.getWhoClicked();
        if (humanEntity instanceof Player && (player = (Player) humanEntity).getGameMode().equals(GameMode.ADVENTURE)) {
            e.setCancelled(true);
        }
    }

    public static final Map<Player, BasketballGame> WAITING_FOR_THROW_PLAYERS = new HashMap<>();


    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        WAITING_FOR_THROW_PLAYERS.remove(event.getPlayer());
        activeDunkMeters.remove(event.getPlayer().getUniqueId());
        recentDashes.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        Player player = e.getPlayer();
        Location from = e.getFrom();
        Location to = e.getTo();
        if (from.getBlockX() == to.getBlockX() && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        for (Ball ball : BallFactory.getNearby(player.getLocation(), 4.0)) {
            if (!(ball instanceof Basketball basketball)) continue;

            UUID stepbacked = basketball.getGame().getStepbacked();
            if (ball.getCurrentDamager() != null && ball.getCurrentDamager().equals(player) && stepbacked != null && stepbacked.equals(player.getUniqueId())) {
                e.setCancelled(true);
            }
        }

        BasketballGame game = WAITING_FOR_THROW_PLAYERS.get(player);
        if (game != null) {
            if (game.isOutOfBoundsSide()) {
                e.setCancelled(true);
                return;
            }

            if (game.isOutOfBoundsHome()) {
                final double diff = Math.abs(game.getHomeSpawn().getX() - e.getTo().getX());
                System.out.println(e.getTo().getBlockZ() + " < " + game.getOutOfBoundsZ());
                if (e.getTo().getBlockZ() < game.getOutOfBoundsZ() || diff > 5) {
                    e.setCancelled(true);
                }
            }else{
                final double diff = Math.abs(game.getAwaySpawn().getX() - e.getTo().getX());
                System.out.println(e.getTo().getBlockZ() + " > " + game.getOutOfBoundsZ());
                if (e.getTo().getBlockZ() > game.getOutOfBoundsZ() || diff > 5) {
                    e.setCancelled(true);
                }
            }
        }
    }


    @EventHandler
    public void onBallThrow(BallRemoveDamagerEvent event) {
        WAITING_FOR_THROW_PLAYERS.remove(event.getPlayer());
        if (event.getBall() instanceof Basketball basketballBall) {
            BasketballGame game = basketballBall.getGame();
            if (game.getState() == GoalGame.State.OUT_OF_BOUNDS_THROW) {
                game.setState(GoalGame.State.REGULATION);

                // Set out of bounds immunity for the player who threw the ball
                // this is done so that if the player is a little beyond the out-of-bounds line
                // the throw is not immediately counted as out of bounds
                game.setOutOfBoundsImmunity(true);
                Bukkit.getScheduler().runTaskLater(Partix.getInstance(), () ->
                                game.setOutOfBoundsImmunity(false),
                        10L // 500 ms delay
                );
            }
        }
    }

    @EventHandler
    public void onJump(PlayerJumpEvent event) {
        Player player = event.getPlayer();
        Athlete athlete = AthleteManager.get(player.getUniqueId());
        if (athlete == null) {
            return;
        }

        Place place = athlete.getPlace();
        if (!(place instanceof BasketballGame game)) {
            return;
        }

        Vector vector = player.getLocation().toVector();
        if (game.getHomeJump().contains(vector) || game.getAwayJump().contains(vector)) {
            player.removePotionEffect(PotionEffectType.JUMP_BOOST);
        }

        // Check for dash jump lock - prevent jumping immediately after dashing
        Long lastDashTime = recentDashes.get(player.getUniqueId());
        if (lastDashTime != null) {
            long timeSinceDash = System.currentTimeMillis() - lastDashTime;
            if (timeSinceDash < DASH_JUMP_LOCK_MS) {
                event.setCancelled(true);
                return;
            } else {
                recentDashes.remove(player.getUniqueId());
            }
        }

        // Register block attempts for dunks
        for (Ball ball : BallFactory.getNearby(player.getLocation(), 4.0)) {
            if (ball instanceof Basketball basketball) {
                basketball.registerDefenderBlockAttempt(player);
            }
        }
    }

    @EventHandler
    public void onBallSetDamager(BallSetDamagerEvent event) {
        Player player = event.getPlayer();
        if (!(event.getBall() instanceof Basketball basketballBall)) return;

        BasketballGame game = basketballBall.getGame();

        if (game.getState() == GoalGame.State.OUT_OF_BOUNDS_THROW_WAIT) {
            GoalGame.Team outOfBoundsLostTeam = game.getOutOfBoundsLostTeam();
            // don't allow players to throw the ball if they are on the same team as the player who lost the ball outside the field
            // check if there are more 2 teams with at least 1 player each
            boolean hasMultipleTeams = game.getPlayers().stream()
                    .map(game::getTeamOf)
                    .distinct()
                    .count() > 1;

            if (outOfBoundsLostTeam != game.getTeamOf(player) && hasMultipleTeams) {
                player.sendMessage(Component.text("You cannot throw the ball out of bounds if you are on the same team as the player who lost it.", NamedTextColor.RED));
                event.setCancelled(true);
                return;
            }

            game.setState(GoalGame.State.OUT_OF_BOUNDS_THROW);

            WAITING_FOR_THROW_PLAYERS.put(player, game);
            new BukkitRunnable() {
                int timeRemaining = 6;
                @Override
                public void run() {
                    if (!player.isOnline() || !WAITING_FOR_THROW_PLAYERS.containsKey(player)) {
                        this.cancel();
                        return;
                    }

                    this.timeRemaining--;
                    if (timeRemaining == 0) {
                        this.cancel();
                        basketballBall.removeCurrentDamager();
                        game.setOutOfBoundsLostTeam(game.getTeamOf(player).getOppositeTeam());
                        game.setState(GoalGame.State.OUT_OF_BOUNDS_THROW_WAIT);
                    }
                }
            }.runTaskTimer(Partix.getInstance(), 0L, 20L);
        }
    }
}

