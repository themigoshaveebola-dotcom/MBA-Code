/*
 * Decompiled with CFR 0.152.
 */
package me.x_tias.partix.plugin.cosmetics;

import lombok.Getter;

@Getter
public enum CosmeticRarity {
    COMMON("§7§lCOMMON", 300),
    RARE("§f§lRARE", 800),
    EPIC("§e§lEPIC", 1900),
    LEGENDARY("§6§lLEGENDARY", 2600);

    private final String title;
    private final int cost;

    CosmeticRarity(String title, int cost) {
        this.title = title;
        this.cost = cost;
    }

    // Add this method
    public String getColor() {
        return switch(this) {
            case COMMON -> "§7";
            case RARE -> "§9";
            case EPIC -> "§5";
            case LEGENDARY -> "§6";
        };
    }

}