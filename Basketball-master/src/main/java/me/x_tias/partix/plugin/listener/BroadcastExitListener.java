/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  org.bukkit.GameMode
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerToggleSneakEvent
 *  org.bukkit.scheduler.BukkitTask
 */
package me.x_tias.partix.plugin.listener;

import me.x_tias.partix.mini.game.GoalGame;
import me.x_tias.partix.plugin.athlete.Athlete;
import me.x_tias.partix.plugin.athlete.AthleteManager;
import me.x_tias.partix.plugin.gui.GUI;
import me.x_tias.partix.plugin.gui.ItemButton;
import me.x_tias.partix.plugin.gui.Items;
import me.x_tias.partix.server.Place;
import me.x_tias.partix.util.Colour;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

public class BroadcastExitListener
        implements Listener {
    @EventHandler
    public void onToggleSneak(PlayerToggleSneakEvent event) {
        if (event.isSneaking()) {
            return;
        }
        Player p = event.getPlayer();
        Athlete ath = AthleteManager.get(p.getUniqueId());
        if (ath == null) {
            return;
        }
        Place place = ath.getPlace();
        if (!(place instanceof GoalGame game)) {
            return;
        }
        UUID id = p.getUniqueId();
        if (!game.broadcasters.contains(id)) {
            return;
        }
        BukkitTask task = game.broadcastTasks.remove(id);
        if (task != null) {
            task.cancel();
        }
        game.broadcasters.remove(id);
        p.setGameMode(GameMode.ADVENTURE);
        event.setCancelled(true);
        Location origin = game.broadcastOrigins.remove(id);
        if (origin != null) {
            p.teleport(origin);
        }
        GUI gui = new GUI("Change Team", 3, false);
        gui.addButton(new ItemButton(10, Items.get(Component.text("Home Team").color(Colour.partix()), Material.BLACK_WOOL), player -> game.joinTeam(player, GoalGame.Team.HOME)));
        gui.addButton(new ItemButton(11, Items.get(Component.text("Away Team").color(Colour.partix()), Material.WHITE_WOOL), player -> game.joinTeam(player, GoalGame.Team.AWAY)));
        gui.addButton(new ItemButton(13, Items.get(Component.text("Broadcast View").color(Colour.partix()), Material.COMPASS), game::startBroadcastFor));
        gui.addButton(new ItemButton(14, Items.get(Component.text("Spectators").color(Colour.partix()), Material.ENDER_EYE), player -> game.joinTeam(player, GoalGame.Team.SPECTATOR)));
        gui.addButton(new ItemButton(16, Items.get(Component.text("Leave Game").color(Colour.deny()), Material.IRON_DOOR), player -> {
        }));
        gui.openInventory(p);
    }
}

