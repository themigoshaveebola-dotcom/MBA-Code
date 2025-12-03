/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Player
 *  org.bukkit.entity.Villager
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerInteractAtEntityEvent
 */
package me.x_tias.partix.proam;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;

public class ProAmListener
        implements Listener {
    private final ProAmManager proAmManager;

    public ProAmListener(ProAmManager proAmManager) {
        this.proAmManager = proAmManager;
    }

    @EventHandler
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        Villager villager;
        Entity entity = event.getRightClicked();
        if (entity instanceof Villager && "§6Pro Am Manager".equals((villager = (Villager) entity).getCustomName())) {
            Player player = event.getPlayer();
            this.proAmManager.showProAmGUI(player);
            event.setCancelled(true);
        }
    }
}

