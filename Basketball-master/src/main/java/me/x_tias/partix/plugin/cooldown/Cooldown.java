/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitRunnable
 */
package me.x_tias.partix.plugin.cooldown;

import me.x_tias.partix.Partix;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.UUID;

public class Cooldown {
    private static HashMap<UUID, Integer> map;

    public static void setup() {
        map = new HashMap<>();
        Partix plugin = Partix.getInstance();
        new BukkitRunnable() {

            public void run() {
                if (map.isEmpty()) {
                    return;
                }
                HashMap<UUID, Integer> clone = new HashMap<>(map);
                for (UUID uuid : clone.keySet()) {
                    int next = clone.get(uuid) - 1;
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null && player.isOnline()) {
                        float amount = Math.max(Math.min((float) next / ((float) next + 1.0f) - 0.475f, 1.0f), 0.0f);
                        player.setExp(amount);
                    }
                    if (next < 1) {
                        map.remove(uuid);
                        continue;
                    }
                    map.put(uuid, next);
                }
            }
        }.runTaskTimerAsynchronously(plugin, 1L, 1L);
    }

    public static boolean isRestricted(Player player) {
        UUID uuid = player.getUniqueId();
        return isRestricted(uuid);
    }

    public static boolean isRestricted(UUID uuid) {
        if (map.containsKey(uuid)) {
            return true;
        }
        map.put(uuid, 2);
        return false;
    }

    public static int getRestriction(UUID uuid) {
        return map.getOrDefault(uuid, -1);
    }

    public static void setRestricted(UUID uuid, int ticks) {
        map.put(uuid, ticks);
    }
}

