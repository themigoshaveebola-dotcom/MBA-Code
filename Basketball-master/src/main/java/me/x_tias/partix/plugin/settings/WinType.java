/*
 * Decompiled with CFR 0.152.
 */
package me.x_tias.partix.plugin.settings;

public enum WinType {
    TIME_2(true, 2, false),
    TIME_3(true, 3, false),
    TIME_5(true, 5, false),
    TIME_6(true, 6, false),
    GOALS_3(false, 3, false),
    GOALS_5(false, 5, false),
    GOALS_10(false, 10, false),
    GOALS_15(false, 15, false),
    FIRST_TO(false, 21, true),
    GOALS_21(false, 21, false);

    public boolean timed;      // ← No 'final'
    public int amount;         // ← No 'final'
    public boolean winByTwo;   // ← No 'final'

    WinType(boolean timed, int amount, boolean winByTwo) {
        this.timed = timed;
        this.amount = amount;
        this.winByTwo = winByTwo;
    }
}

