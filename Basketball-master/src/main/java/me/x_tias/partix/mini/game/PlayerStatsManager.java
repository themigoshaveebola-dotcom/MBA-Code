/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.jetbrains.annotations.NotNull
 */
package me.x_tias.partix.mini.game;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerStatsManager {
    private final Map<UUID, PlayerStats> statsMap;
    private final HashMap<UUID, PlayerStats> playerStats = new HashMap<>();

    public PlayerStatsManager() {
        this.statsMap = new HashMap<>();
    }

    public PlayerStats getStats(UUID playerId) {
        return this.statsMap.computeIfAbsent(playerId, k -> new PlayerStats());
    }

    public void resetStats() {
        this.playerStats.values().forEach(PlayerStats::reset);
    }

    public Map<UUID, PlayerStats> getAllStats() {
        return this.playerStats;
    }

    public PlayerStats getPlayerStats(UUID playerId) {
        return this.playerStats.computeIfAbsent(playerId, id -> new PlayerStats());
    }

    public void updatePlayerStats(@NotNull UUID uniqueId, PlayerStats stats) {
    }
}

