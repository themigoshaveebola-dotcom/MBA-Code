/*
 * Decompiled with CFR 0.152.
 */
package me.x_tias.partix.database;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BettingDb {
    private static final Map<UUID, Integer> playerBucks = new HashMap<>();

    public static List<UUID> getTopBucksPlayers() {
        return playerBucks.entrySet().stream().sorted((a, b) -> Integer.compare(b.getValue(), a.getValue())).limit(3L).map(Map.Entry::getKey).toList();
    }

    public static int getPlayerBucks(UUID playerId) {
        return playerBucks.getOrDefault(playerId, 0);
    }

    public static void addBucks(UUID playerId, int amount) {
        playerBucks.put(playerId, BettingDb.getPlayerBucks(playerId) + amount);
    }
}

