package me.x_tias.partix.plugin.cosmetics;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import me.x_tias.partix.Partix;
import me.x_tias.partix.database.PlayerDb;
import org.bukkit.Bukkit;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class CrateInventory {

    private static final Gson GSON = new Gson();

    public enum CrateType {
        TRAIL("trail"),
        EXPLOSION("explosion"),
        GREEN_SOUND("greensound"),
        BALL_TRAIL("balltrail");

        private final String key;

        CrateType(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }

        public String getDisplayName() {
            return switch(this) {
                case TRAIL -> "Trail";
                case EXPLOSION -> "Goal Explosion";
                case GREEN_SOUND -> "Green Sound";
                case BALL_TRAIL -> "Ball Trail";
            };
        }

        public static CrateType fromKey(String key) {
            for (CrateType type : values()) {
                if (type.key.equalsIgnoreCase(key)) {
                    return type;
                }
            }
            return null;
        }
    }

    /**
     * Add a crate to player's inventory
     */
    public static CompletableFuture<Void> addCrate(UUID playerId, CrateType type, CosmeticRarity rarity) {
        String crateKey = getCrateKey(type, rarity);
        return getCratesMap(playerId).thenCompose(crates -> {
            crates.put(crateKey, crates.getOrDefault(crateKey, 0) + 1);
            Bukkit.getLogger().info("[CrateInventory] Adding crate for " + playerId + ": " + crateKey + " (now has " + crates.get(crateKey) + ")");
            return saveCratesMap(playerId, crates);
        });
    }

    /**
     * Remove a crate from player's inventory
     */
    public static CompletableFuture<Boolean> removeCrate(UUID playerId, CrateType type, CosmeticRarity rarity) {
        String crateKey = getCrateKey(type, rarity);

        return getCratesMap(playerId).thenCompose(crates -> {
            if (crates.getOrDefault(crateKey, 0) > 0) {
                crates.put(crateKey, crates.get(crateKey) - 1);
                if (crates.get(crateKey) == 0) {
                    crates.remove(crateKey);
                }
                Bukkit.getLogger().info("[CrateInventory] Removing crate for " + playerId + ": " + crateKey);
                return saveCratesMap(playerId, crates).thenApply(v -> true);
            }
            return CompletableFuture.completedFuture(false);
        });
    }

    /**
     * Get all crates owned by player
     */
    public static CompletableFuture<Map<String, Integer>> getCrates(UUID playerId) {
        return getCratesMap(playerId);
    }

    /**
     * Check if player has a specific crate
     */
    public static CompletableFuture<Boolean> hasCrate(UUID playerId, CrateType type, CosmeticRarity rarity) {
        String crateKey = getCrateKey(type, rarity);
        return getCrates(playerId).thenApply(crates -> crates.getOrDefault(crateKey, 0) > 0);
    }

    /**
     * Get count of a specific crate
     */
    public static CompletableFuture<Integer> getCrateCount(UUID playerId, CrateType type, CosmeticRarity rarity) {
        String crateKey = getCrateKey(type, rarity);
        return getCrates(playerId).thenApply(crates -> crates.getOrDefault(crateKey, 0));
    }

    private static String getCrateKey(CrateType type, CosmeticRarity rarity) {
        return type.getKey() + "_" + rarity.name().toLowerCase();
    }

    private static CompletableFuture<Map<String, Integer>> getCratesMap(UUID playerId) {
        return PlayerDb.getString(playerId, PlayerDb.Stat.CRATE_DATA).thenApply(jsonData -> {
            if (jsonData == null || jsonData.isEmpty() || jsonData.equals("0")) {
                return new HashMap<>();
            }
            try {
                Map<String, Integer> crates = GSON.fromJson(jsonData, new TypeToken<Map<String, Integer>>(){}.getType());
                return crates != null ? crates : new HashMap<>();
            } catch (Exception e) {
                Bukkit.getLogger().warning("[CrateInventory] Failed to parse crate data for " + playerId + ": " + e.getMessage());
                return new HashMap<>();
            }
        });
    }

    private static CompletableFuture<Void> saveCratesMap(UUID playerId, Map<String, Integer> crates) {
        String json = GSON.toJson(crates);
        Bukkit.getLogger().info("[CrateInventory] Saving crate data for " + playerId + ": " + json);
        return PlayerDb.setString(playerId, PlayerDb.Stat.CRATE_DATA, json);
    }

    public static CrateType parseCrateType(String key) {
        if (key.contains("trail")) return CrateType.TRAIL;
        if (key.contains("explosion")) return CrateType.EXPLOSION;
        if (key.contains("greensound")) return CrateType.GREEN_SOUND;
        if (key.contains("balltrail")) return CrateType.BALL_TRAIL;
        return null;
    }

    public static CosmeticRarity parseCrateRarity(String key) {
        for (CosmeticRarity rarity : CosmeticRarity.values()) {
            if (key.contains(rarity.name().toLowerCase())) {
                return rarity;
            }
        }
        return null;
    }
}