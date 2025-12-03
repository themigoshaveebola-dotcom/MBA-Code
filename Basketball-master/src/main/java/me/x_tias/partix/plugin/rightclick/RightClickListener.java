package me.x_tias.partix.plugin.rightclick;

import me.x_tias.partix.Partix;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

public class RightClickListener implements Listener {

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (!event.getAction().isRightClick())
            return;

        Partix.getInstance().getRightClickManager().addClick(event.getPlayer());
    }
}
