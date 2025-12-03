/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.bukkit.GameMode
 *  org.bukkit.entity.HumanEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.entity.EntityDamageByEntityEvent
 *  org.bukkit.event.entity.EntityDamageEvent
 *  org.bukkit.event.entity.FoodLevelChangeEvent
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.player.PlayerInteractEvent
 */
package me.x_tias.partix.plugin.listener;

import org.bukkit.GameMode;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class QualityListener
        implements Listener {
    @EventHandler
    public void onHungerDeplete(FoodLevelChangeEvent e) {
        e.setCancelled(true);
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        e.setCancelled(true);
    }

    @EventHandler
    public void onAnyDamage(EntityDamageEvent e) {
        e.setCancelled(true);
    }

    @EventHandler
    public void onInteractBlock(PlayerInteractEvent e) {
        if (e.getPlayer().getGameMode().equals(GameMode.ADVENTURE)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onEditInventory(InventoryClickEvent e) {
        Player player;
        HumanEntity humanEntity = e.getWhoClicked();
        if (humanEntity instanceof Player && (player = (Player) humanEntity).getGameMode().equals(GameMode.ADVENTURE)) {
            e.setCancelled(true);
        }
    }
}

