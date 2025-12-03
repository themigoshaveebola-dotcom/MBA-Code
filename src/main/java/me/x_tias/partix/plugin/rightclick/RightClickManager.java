package me.x_tias.partix.plugin.rightclick;

import me.x_tias.partix.Partix;
import me.x_tias.partix.plugin.rightclick.events.PlayerRightClickStartHoldEvent;
import me.x_tias.partix.plugin.rightclick.events.PlayerRightClickStopHoldEvent;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class RightClickManager {

    private static final long DELAY = 200L;
    private static final int SUCCESSES_REQUIRED = 1;

    private final Map<Player, RightClickData> lastClickData = new HashMap<>();
    public RightClickManager() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<Player, RightClickData> entry : new HashSet<>(lastClickData.entrySet())) {
                    final Player player = entry.getKey();
                    if (System.currentTimeMillis() - entry.getValue().getLastClickTimestamp() > DELAY) {
                        lastClickData.remove(entry.getKey());
                        if (!player.isOnline()) return;

                        new PlayerRightClickStopHoldEvent(player).callEvent();
                    }
                }
            }
        }.runTaskTimer(Partix.getInstance(), 1L, 1L);
    }

    public void addClick(@NotNull Player player) {
        final RightClickData data = lastClickData.getOrDefault(player, new RightClickData());
        data.setSuccesses(data.getSuccesses() + 1);
        if (data.getSuccesses() == SUCCESSES_REQUIRED) {
            new PlayerRightClickStartHoldEvent(player).callEvent();
        }

        data.setLastClickTimestamp(System.currentTimeMillis());
        this.lastClickData.put(player, data);
    }

    public boolean isHolding(@NotNull Player player) {
        final RightClickData data = this.lastClickData.get(player);
        return data != null;
    }

    public void removeHolding(@NotNull Player player) {
        this.lastClickData.remove(player);
    }
}
