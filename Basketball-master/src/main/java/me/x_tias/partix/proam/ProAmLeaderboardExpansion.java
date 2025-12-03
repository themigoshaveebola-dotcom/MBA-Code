/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  me.clip.placeholderapi.expansion.PlaceholderExpansion
 *  org.bukkit.OfflinePlayer
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package me.x_tias.partix.proam;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class ProAmLeaderboardExpansion
        extends PlaceholderExpansion {
    @NotNull
    public String getIdentifier() {
        return "proam";
    }

    @NotNull
    public String getAuthor() {
        return "X_Tias";
    }

    @NotNull
    public String getVersion() {
        return "1.0";
    }

    public boolean persist() {
        return true;
    }

    @Nullable
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (params.startsWith("ranked_")) {
            try {
                int rank = Integer.parseInt(params.replace("ranked_", ""));
                if (rank < 1 || rank > 15) {
                    return "§7Invalid Rank";
                }
                Map<Integer, String> leaderboard = ProAmLeaderboard.getTeamLeaderboard();
                return leaderboard.getOrDefault(rank, "§7None");
            } catch (NumberFormatException e) {
                return "§cInvalid Rank Number";
            }
        }
        return null;
    }
}

