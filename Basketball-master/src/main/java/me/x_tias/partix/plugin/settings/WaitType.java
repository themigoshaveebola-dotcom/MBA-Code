/*
 * Decompiled with CFR 0.152.
 */
package me.x_tias.partix.plugin.settings;

public enum WaitType {
    SHORT(7, 12, 20),
    MEDIUM(10, 20, 30),
    LONG(15, 30, 60);

    public final int low;
    public final int med;
    public final int high;

    WaitType(int low, int med, int high) {
        this.low = low;
        this.med = med;
        this.high = high;
    }
}

