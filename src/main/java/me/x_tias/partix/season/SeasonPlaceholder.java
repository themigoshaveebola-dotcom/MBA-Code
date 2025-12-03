/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  me.clip.placeholderapi.expansion.PlaceholderExpansion
 *  org.bukkit.entity.Player
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package me.x_tias.partix.season;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.x_tias.partix.database.SeasonDb;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class SeasonPlaceholder
        extends PlaceholderExpansion {
    @NotNull
    public String getIdentifier() {
        return "season";
    }

    @NotNull
    public String getAuthor() {
        return "x_tias";
    }

    @NotNull
    public String getVersion() {
        return "1.0";
    }

    public boolean persist() {
        return true;
    }

    public boolean register() {
        return super.register();
    }

    @Nullable
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (params.startsWith("ranked")) {
            int rank = Integer.parseInt(params.replaceAll("ranked_", ""));
            return SeasonLeaderboard.getRankedLeaderboard().getOrDefault(rank, "Not Found..");
        }
        if (params.startsWith("goldseasons")) {
            int c = Integer.parseInt(params.replaceAll("goldseasons_", ""));
            return Objects.requireNonNullElse(AllTimeLeaderboard.getGoldSeasons().get(c), "Not Found..");
        }
        if (params.startsWith("trophies")) {
            int c = Integer.parseInt(params.replaceAll("trophies_", ""));
            return Objects.requireNonNullElse(AllTimeLeaderboard.getTrophies().get(c), "Not Found..");
        }
        if (params.startsWith("time")) {
            return Season.getTimeRemaining();
        }
        if (params.startsWith("points")) {
            int pts = SeasonDb.get(player.getUniqueId(), SeasonDb.Stat.POINTS).join();
            return String.valueOf(pts);
        }
        if (params.startsWith("division")) {
            int pts = SeasonDb.get(player.getUniqueId(), SeasonDb.Stat.POINTS).join();
            return pts >= 50000 ? "§eGold" : "§7Silver";
        }
        if (params.startsWith("record")) {
            return SeasonDb.get(player.getUniqueId(), SeasonDb.Stat.WINS) + "-" + SeasonDb.get(player.getUniqueId(), SeasonDb.Stat.LOSSES);
        }
        if (params.startsWith("wins")) {
            return String.valueOf(SeasonDb.get(player.getUniqueId(), SeasonDb.Stat.WINS));
        }
        if (params.startsWith("losses")) {
            return String.valueOf(SeasonDb.get(player.getUniqueId(), SeasonDb.Stat.LOSSES));
        }
        return "";
    }
}

