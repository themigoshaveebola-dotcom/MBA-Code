package me.x_tias.partix.plugin.seasonpass;

import lombok.Getter;
import org.bukkit.Material;

@Getter
public class SeasonPassReward {
    private final int tier;
    private final RewardType type;
    private final String name;
    private final String description;
    private final int value; // For coins/exp, or cosmetic ID
    private final Material displayMaterial;
    
    public SeasonPassReward(int tier, RewardType type, String name, String description, int value, Material displayMaterial) {
        this.tier = tier;
        this.type = type;
        this.name = name;
        this.description = description;
        this.value = value;
        this.displayMaterial = displayMaterial;
    }
    
    public String getId() {
        return "tier_" + tier;
    }
    
    public enum RewardType {
        COINS,
        BALL_TRAIL,
        GOAL_EXPLOSION,
        ACCESSORY
    }
}
