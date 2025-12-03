/*
 * Decompiled with CFR 0.152.
 */
package me.x_tias.partix.mini.game;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerStats {
    private final Map<UUID, Integer> contestTime = new HashMap<>();
    @Setter
    @Getter
    private int points;
    @Setter
    @Getter
    private int threes;
    @Setter
    @Getter
    private int assists;
    @Setter
    @Getter
    private int rebounds;
    @Setter
    @Getter
    private int steals;
    @Getter
    private int turnovers;
    @Getter
    private long possessionTime;
    private int fgMade;
    private int fgAttempted;
    private int fg3Made;
    private int fg3Attempted;
    private int fg2Made;
    private int fg2Attempted;
    @Getter
    private int passAttempts;

    public PlayerStats() {
        this.reset();
    }

    public int getFGMade() {
        return this.fgMade;
    }

    public void setFGMade(int fgMade) {
        this.fgMade = fgMade;
    }

    public int getFGAttempted() {
        return this.fgAttempted;
    }

    public void setFGAttempted(int fgAttempted) {
        this.fgAttempted = fgAttempted;
    }

    public int get3FGMade() {
        return this.fg3Made;
    }

    public void set3FGMade(int fg3Made) {
        this.fg3Made = fg3Made;
    }

    public int get3FGAttempted() {
        return this.fg3Attempted;
    }

    public void set3FGAttempted(int fg3Attempted) {
        this.fg3Attempted = fg3Attempted;
    }

    public int get2FGMade() {
        return this.fg2Made;
    }

    public int get2FGAttempted() {
        return this.fg2Attempted;
    }

    public double getFGPercentage() {
        return this.fgAttempted == 0 ? 0.0 : (double) this.fgMade / (double) this.fgAttempted * 100.0;
    }

    public double get3FGPercentage() {
        return this.fg3Attempted == 0 ? 0.0 : (double) this.fg3Made / (double) this.fg3Attempted * 100.0;
    }

    public void addPoints(int pts) {
        this.points += pts;
    }

    public void incrementThrees() {
        ++this.threes;
    }

    public void incrementAssists() {
        ++this.assists;
    }

    public void incrementRebounds() {
        ++this.rebounds;
    }

    public void incrementSteals() {
        ++this.steals;
    }

    public void incrementTurnovers() {
        ++this.turnovers;
    }

    public void addPossessionTime(long millis) {
        this.possessionTime += millis;
    }

    public void incrementFGMade() {
        ++this.fgMade;
    }

    public void incrementFGAttempted() {
        ++this.fgAttempted;
    }

    public void increment3FGMade() {
        ++this.fg3Made;
    }

    public void increment3FGAttempted() {
        ++this.fg3Attempted;
    }

    public void increment2FGMade() {
        ++this.fg2Made;
    }

    public void increment2FGAttempted() {
        ++this.fg2Attempted;
    }

    public void incrementPassAttempts() {
        ++this.passAttempts;
    }

    public void addContestTime(UUID opponent, int time) {
        this.contestTime.merge(opponent, time, Integer::sum);
    }

    public UUID getTopContestedOpponent() {
        return this.contestTime.entrySet().stream().max((e1, e2) -> Integer.compare(e1.getValue(), e2.getValue())).map(Map.Entry::getKey).orElse(null);
    }

    public void resetContestTime() {
        this.contestTime.clear();
    }

    public void reset() {
        this.points = 0;
        this.threes = 0;
        this.assists = 0;
        this.rebounds = 0;
        this.steals = 0;
        this.turnovers = 0;
        this.possessionTime = 0L;
        this.fgMade = 0;
        this.fgAttempted = 0;
        this.fg3Made = 0;
        this.fg3Attempted = 0;
        this.fg2Made = 0;
        this.fg2Attempted = 0;
        this.passAttempts = 0;
        this.resetContestTime();
    }
}

