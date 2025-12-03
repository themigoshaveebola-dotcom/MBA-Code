/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 */
package me.x_tias.partix.plugin.athlete;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

public class AthleteManager {
    private static final HashMap<UUID, Athlete> map = new HashMap<>();

    public static Athlete get(UUID uuid) {
        Athlete athlete = map.get(uuid);
        if (athlete == null) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                athlete = AthleteManager.create(player);
                Bukkit.getLogger().warning("WARNING: Athlete for player " + player.getName() + " (UUID: " + uuid + ") was missing. Created new Athlete.");
            } else {
                Bukkit.getLogger().severe("ERROR: Attempted to retrieve Athlete for UUID " + uuid + " but player is offline!");
            }
        }
        return athlete;
    }

    public static Athlete create(Player player) {
        Athlete athlete = new Athlete(player);
        map.put(player.getUniqueId(), athlete);
        return athlete;
    }

    public static Athlete remove(Player player) {
        Athlete athlete = map.get(player.getUniqueId());
        map.remove(player.getUniqueId());
        return athlete;
    }
}

