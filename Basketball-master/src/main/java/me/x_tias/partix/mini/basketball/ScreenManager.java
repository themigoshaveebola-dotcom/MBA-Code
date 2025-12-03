/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.Sound
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerMoveEvent
 *  org.bukkit.event.player.PlayerToggleSneakEvent
 *  org.bukkit.metadata.FixedMetadataValue
 *  org.bukkit.metadata.MetadataValue
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 *  org.bukkit.potion.PotionEffect
 *  org.bukkit.potion.PotionEffectType
 *  org.bukkit.scheduler.BukkitTask
 *  org.bukkit.util.Vector
 */
package me.x_tias.partix.mini.basketball;

import me.x_tias.partix.Partix;
import me.x_tias.partix.plugin.ball.BallFactory;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ScreenManager
        implements Listener {
    public static final ScreenManager INSTANCE = new ScreenManager(Partix.getInstance());
    private static final String META_SCREEN = "partix.screening";
    private static final long CHARGE_TICKS = 11L;
    private static final double TOLERANCE_XZ_SQ = 0.0625;
    private static final double RADIUS_SQ = 4.0;
    private final JavaPlugin plugin;
    private final Map<UUID, Location> anchor = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> chargeJob = new ConcurrentHashMap<>();

    private ScreenManager(JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        Player p = e.getPlayer();
        UUID id = p.getUniqueId();
        if (e.isSneaking()) {
            if (BallFactory.hasBall(p)) {
                return;
            }
            this.anchor.put(id, p.getLocation().clone());
            this.scheduleCharge(id, p);
        } else {
            this.cancelCharge(id);
            this.anchor.remove(id);
            p.removeMetadata(META_SCREEN, this.plugin);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        double dz;
        Player p = e.getPlayer();
        if (!p.isSneaking()) {
            return;
        }
        if (BallFactory.hasBall(p)) {
            this.cancelCharge(p.getUniqueId());
            this.anchor.remove(p.getUniqueId());
            p.removeMetadata(META_SCREEN, this.plugin);
            return;
        }
        UUID id = p.getUniqueId();
        Location a = this.anchor.get(id);
        if (a == null) {
            return;
        }
        double dx = e.getTo().getX() - a.getX();
        double distSq = dx * dx + (dz = e.getTo().getZ() - a.getZ()) * dz;
        if (distSq > 0.0625) {
            this.anchor.put(id, e.getTo().clone());
            p.removeMetadata(META_SCREEN, this.plugin);
            this.scheduleCharge(id, p);
        }
    }

    private void scheduleCharge(UUID id, Player p) {
        this.cancelCharge(id);
        BukkitTask task = Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.finishCharge(p), 11L);
        this.chargeJob.put(id, task);
    }

    private void cancelCharge(UUID id) {
        BukkitTask old = this.chargeJob.remove(id);
        if (old != null && !old.isCancelled()) {
            old.cancel();
        }
    }

    private void finishCharge(Player p) {
        double dz;
        if (!p.isOnline() || !p.isSneaking()) {
            return;
        }
        if (BallFactory.hasBall(p)) {
            return;
        }
        Location a = this.anchor.get(p.getUniqueId());
        if (a == null) {
            return;
        }
        double dx = p.getLocation().getX() - a.getX();
        if (dx * dx + (dz = p.getLocation().getZ() - a.getZ()) * dz > 0.0625) {
            return;
        }
        if (!p.hasMetadata(META_SCREEN)) {
            p.setMetadata(META_SCREEN, new FixedMetadataValue(this.plugin, true));
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.8f, 1.6f);
        }
        this.chargeJob.remove(p.getUniqueId());
    }


    public void tickActiveScreens(BasketballGame game) {
        ArrayList<Player> all = new ArrayList<>();
        all.addAll(game.getHomePlayers());
        all.addAll(game.getAwayPlayers());
        for (Player screener : all) {
            this.applyBumps(screener, all, game);
        }
    }

    private void applyBumps(Player screener, List<Player> targets, BasketballGame game) {
        if (BallFactory.hasBall(screener)) {
            screener.removeMetadata(META_SCREEN, this.plugin);
            this.cancelCharge(screener.getUniqueId());
            return;
        }
        if (!screener.hasMetadata(META_SCREEN)) {
            return;
        }

        // Get screener's team
        Object screenerTeam = game.getTeamOf(screener);

        for (Player o : targets) {
            if (o == screener || !(o.getLocation().distanceSquared(screener.getLocation()) <= 4.0)) continue;

            // Get target's team
            Object targetTeam = game.getTeamOf(o);

            // Skip if both players are on the same team
            if (screenerTeam != null && targetTeam != null && screenerTeam.equals(targetTeam)) {
                continue;
            }

            Vector push = o.getLocation().toVector().subtract(screener.getLocation().toVector()).setY(0).normalize().multiply(0.03);
            o.setVelocity(o.getVelocity().add(push));
            o.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 0, true, false));
        }
    }}

