package me.x_tias.partix.plugin.athlete;

import lombok.Getter;
import lombok.Setter;
import me.x_tias.partix.Partix;
import me.x_tias.partix.database.PlayerDb;
import me.x_tias.partix.mini.factories.Hub;
import me.x_tias.partix.plugin.cosmetics.CosmeticParticle;
import me.x_tias.partix.plugin.cosmetics.CosmeticSound;
import me.x_tias.partix.plugin.cosmetics.Cosmetics;
import me.x_tias.partix.server.Place;
import me.x_tias.partix.server.rank.Ranks;
import me.x_tias.partix.util.Colour;
import me.x_tias.partix.util.Message;
import me.x_tias.partix.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Map;

@Getter
public class Athlete {
    private final Player player;
    @Setter
    private int party;
    @Setter
    private Place place;
    @Setter
    private RenderType renderType;
    private CosmeticParticle explosion;
    private CosmeticParticle trail;
    private CosmeticSound greenSound;
    private CosmeticSound winSong;

    public Athlete(Player p) {
        this.player = p;
        this.party = -1;
        this.updateCosmetics();
        this.updateRank();
        this.renderType = RenderType.PARTICLE;
        Hub.hub.join(this);
    }

    public void setSpectator(boolean b) {
        this.player.setGameMode(GameMode.ADVENTURE);
        if (this.player.isOnline()) {
            if (b) {
                this.player.setAllowFlight(true);
                this.player.setFlying(true);
                this.player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 255, true, false));
            } else {
                this.player.setFlying(false);
                this.player.setAllowFlight(false);
                this.player.removePotionEffect(PotionEffectType.INVISIBILITY);
            }
        }
    }

    public boolean isSpectating() {
        return this.player.getAllowFlight() && this.player.hasPotionEffect(PotionEffectType.INVISIBILITY);
    }

    public void setExplosion(int index) {
        this.explosion = Cosmetics.explosions.get(index);
        PlayerDb.set(this.player.getUniqueId(), PlayerDb.Stat.EXPLOSION, index);
    }

    public void setTrail(int index) {
        this.trail = Cosmetics.trails.get(index);
        PlayerDb.set(this.player.getUniqueId(), PlayerDb.Stat.TRAIL, index);
    }

    public void setGreenSound(int soundKey) {
        this.greenSound = Cosmetics.greenSounds.get(soundKey);
    }

    public void setGreenSound(CosmeticSound sound) {
        this.greenSound = sound;
    }

    public void equipGreenSound(CosmeticSound sound) {
        if (sound != null && !this.player.hasPermission("cosmetic.green." + sound.getName().toLowerCase().replace(" ", "_"))) {
            this.player.sendMessage(Component.text("You do not have permission to equip this Green Sound.").color(Colour.deny()));
            return;
        }
        this.greenSound = sound;
        int soundSlot = sound == null ? 0 : Cosmetics.greenSounds.entrySet().stream().filter(entry -> entry.getValue().equals(sound)).map(Map.Entry::getKey).findFirst().orElse(0);
        PlayerDb.set(this.player.getUniqueId(), PlayerDb.Stat.GREEN_SOUND, soundSlot);
        String message = sound == null ? "No Green Sound equipped." : "Green Sound equipped: " + sound.getName();
        this.player.sendMessage(Component.text(message).color(Colour.allow()));
    }

    public void updateCosmetics() {
        PlayerDb.get(this.player.getUniqueId(), PlayerDb.Stat.EXPLOSION).thenAccept(explosion -> this.explosion = Cosmetics.explosions.get(explosion));
        PlayerDb.get(this.player.getUniqueId(), PlayerDb.Stat.TRAIL).thenAccept(trail -> this.trail = Cosmetics.trails.get(trail));
        PlayerDb.get(this.player.getUniqueId(), PlayerDb.Stat.WINSONG).thenAccept(winsong -> this.winSong = Cosmetics.winSongs.get(winsong));
        PlayerDb.get(this.player.getUniqueId(), PlayerDb.Stat.GREEN_SOUND).thenAccept(greenSoundId -> this.greenSound = Cosmetics.greenSounds.getOrDefault(greenSoundId, Cosmetics.greenSounds.get(0)));
    }

    public void updateRank() {
        Scoreboard s = Bukkit.getScoreboardManager().getMainScoreboard();
        this.player.setScoreboard(s);

        // Debug: log what team they're currently in
        for (Team team : s.getTeams()) {
            if (team.hasEntry(this.player.getName())) {
            }
        }

        // Update player list name (tab list) with team prefix + rank
        this.player.playerListName(this.getName());

        // Remove from all existing teams
        for (Team team : s.getTeams()) {
            if (team.hasEntry(this.player.getName())) {
                team.removeEntry(this.player.getName());
            }
        }

        // Your existing team assignment code...
        if (this.player.hasPermission("rank.admin")) {
            Ranks.getAdmin().addEntry(this.player.getName());
        }
        else if (this.player.hasPermission("rank.mod")) {
            Ranks.getMod().addEntry(this.player.getName());
        }

        // Debug: Schedule a check in 2 seconds to see if it's still there
        Bukkit.getScheduler().runTaskLater(Partix.getInstance(), () -> {
            boolean found = false;
            for (Team team : s.getTeams()) {
                if (team.hasEntry(this.player.getName())) {
                    found = true;
                }
            }
            if (!found) {
            }
        }, 200L); // 40 ticks = 2 seconds
    }

    /**
     * Helper method to create multi-colored text where each character has its own color
     */
    private Component createMultiColorText(String text, TextColor[] colors, boolean bold) {
        Component result = Component.empty();
        for (int i = 0; i < text.length(); i++) {
            TextColor color = colors[i % colors.length];
            Component charComponent = Component.text(text.charAt(i), color);
            if (bold) {
                charComponent = charComponent.decorate(TextDecoration.BOLD);
            }
            result = result.append(charComponent);
        }
        return result;
    }
    private Component getTeamLogo() {
        // Return empty - we'll include the logo directly in the prefix instead
        return Component.empty();
    }
    /**
     * Get the player's team prefix component with per-letter colors (NBA jersey style)
     */
    private Component getTeamPrefix() {
        if (this.player.hasPermission("rank.washingtonwithers")) {
            return Component.text(" \uE001 [", TextColor.color(0xAAAAAA))
                    .append(createMultiColorText("WAS", new TextColor[]{
                            TextColor.color(0x5555FF), // W - Blue
                            TextColor.color(0xFF5555), // A - Red
                            TextColor.color(0x5555FF)  // S - Blue
                    }, true))
                    .append(Component.text("] ", TextColor.color(0xAAAAAA)));
        }
        if (this.player.hasPermission("rank.philadelphia64s")) {
            return Component.text(" \uE002 [", TextColor.color(0xAAAAAA))
                    .append(createMultiColorText("PHI", new TextColor[]{
                            TextColor.color(0xFF5555), // P - Red
                            TextColor.color(0x5555FF), // H - Blue
                            TextColor.color(0xFFFFFF)  // I - White
                    }, true))
                    .append(Component.text("] ", TextColor.color(0xAAAAAA)));
        }
        if (this.player.hasPermission("rank.chicagobows")) {
            return Component.text(" \uE003 [", TextColor.color(0xAAAAAA))
                    .append(createMultiColorText("CHI", new TextColor[]{
                            TextColor.color(0xFF5555), // C - Red
                            TextColor.color(0x000000), // H - Black
                            TextColor.color(0xFFFFFF)  // I - White
                    }, true))
                    .append(Component.text("] ", TextColor.color(0xAAAAAA)));
        }
        if (this.player.hasPermission("rank.bostonbreeze")) {
            return Component.text(" \uE004 [", TextColor.color(0xAAAAAA))
                    .append(createMultiColorText("BOS", new TextColor[]{
                            TextColor.color(0x00AA00), // B - Green
                            TextColor.color(0xFFFFFF), // O - White
                            TextColor.color(0x00AA00)  // S - Green
                    }, true))
                    .append(Component.text("] ", TextColor.color(0xAAAAAA)));
        }
        if (this.player.hasPermission("rank.brooklynbuckets")) {
            return Component.text(" \uE005 [", TextColor.color(0x555555))
                    .append(createMultiColorText("BKN", new TextColor[]{
                            TextColor.color(0x000000), // B - Black
                            TextColor.color(0xFFFFFF), // K - White
                            TextColor.color(0x555555)  // N - Gray
                    }, true))
                    .append(Component.text("] ", TextColor.color(0x555555)));
        }
        if (this.player.hasPermission("rank.miamimagmacubes")) {
            return Component.text(" \uE006 [", TextColor.color(0xAAAAAA))
                    .append(createMultiColorText("MIA", new TextColor[]{
                            TextColor.color(0xAA0000), // M - Dark Red
                            TextColor.color(0xFFAA00), // I - Orange
                            TextColor.color(0x000000)  // A - Black
                    }, true))
                    .append(Component.text("] ", TextColor.color(0xAAAAAA)));
        }
        if (this.player.hasPermission("rank.atlantaallays")) {
            return Component.text(" \uE008 [", TextColor.color(0xAAAAAA))
                    .append(createMultiColorText("ATL", new TextColor[]{
                            TextColor.color(0xFF5555), // A - Red
                            TextColor.color(0xFFFFFF), // T - White
                            TextColor.color(0xFF5555)  // L - Red
                    }, true))
                    .append(Component.text("] ", TextColor.color(0xAAAAAA)));
        }
        if (this.player.hasPermission("rank.goldenstateguardians")) {
            return Component.text(" \uE009 [", TextColor.color(0xAAAAAA))
                    .append(createMultiColorText("GSW", new TextColor[]{
                            TextColor.color(0x5555FF), // G - Blue
                            TextColor.color(0xFFAA00), // S - Gold
                            TextColor.color(0x5555FF)  // W - Blue
                    }, true))
                    .append(Component.text("] ", TextColor.color(0xAAAAAA)));
        }

        // Free Agent - Gray
        return Component.text(" [FA] ", TextColor.color(0xAAAAAA));
    }

    /**
     * Get the player's rank prefix component (e.g., "ADMIN ", "VIP ", etc.)
     * Returns empty component if player has no special rank
     */
    private Component getRankPrefix() {
        // Staff ranks with gradients
        if (this.player.hasPermission("rank.admin")) {
            return Text.gradient("ADMIN ",
                    TextColor.fromHexString("#c24528"),
                    TextColor.fromHexString("#c8682a"), false);
        }
        if (this.player.hasPermission("rank.mod")) {
            return Text.gradient("MOD ",
                    TextColor.fromHexString("#1478cc"),
                    TextColor.fromHexString("#47b0b4"), false);
        }
        if (this.player.hasPermission("rank.media")) {
            return Text.gradient("MEDIA ",
                    TextColor.fromHexString("#FF1493"),
                    TextColor.fromHexString("#FF69B4"), false);
        }

        // Special roles
        if (this.player.hasPermission("rank.coach")) {
            return Component.text("COACH ", TextColor.color(0xFF0000));
        }
        if (this.player.hasPermission("rank.referee")) {
            return Component.text("REFEREE ", TextColor.color(0x000000));
        }

        // Subscription ranks with gradients
        if (this.player.hasPermission("rank.pro")) {
            return Text.gradient("PRO ",
                    TextColor.fromHexString("#FFD700"),
                    TextColor.fromHexString("#FF8C00"), false);
        }
        if (this.player.hasPermission("rank.vip")) {
            return Text.gradient("VIP ",
                    TextColor.fromHexString("#00FF00"),
                    TextColor.fromHexString("#008000"), false);
        }

        // No special rank
        return Component.empty();
    }

    /**
     * Get the player's name with team colors applied per-character
     * (For leaderboards and special displays)
     */
    private Component getColoredPlayerName() {
        String name = this.player.getName();

        // Staff ranks - use single color
        if (this.player.hasPermission("rank.admin")) {
            return Component.text(name, Colour.adminText());
        }
        if (this.player.hasPermission("rank.mod")) {
            return Component.text(name, Colour.adminText());
        }
        if (this.player.hasPermission("rank.media")) {
            return Component.text(name, TextColor.color(0xFF1493));
        }

        // Special roles - use single color
        if (this.player.hasPermission("rank.coach")) {
            return Component.text(name, TextColor.color(0xFF0000));
        }
        if (this.player.hasPermission("rank.referee")) {
            return Component.text(name, TextColor.color(0x555555));
        }

        // Subscription ranks - use single color
        if (this.player.hasPermission("rank.pro")) {
            return Component.text(name, Colour.premiumText());
        }
        if (this.player.hasPermission("rank.vip")) {
            return Component.text(name, Colour.vipText());
        }

        // MBA Teams - apply alternating team colors to each character
        if (this.player.hasPermission("rank.washingtonwithers")) {
            return createMultiColorText(name, new TextColor[]{
                    TextColor.color(0x5555FF), // Blue
                    TextColor.color(0xFF5555)  // Red
            }, false);
        }
        if (this.player.hasPermission("rank.philadelphia64s")) {
            return createMultiColorText(name, new TextColor[]{
                    TextColor.color(0xFF5555), // Red
                    TextColor.color(0x5555FF), // Blue
                    TextColor.color(0xFFFFFF)  // White
            }, false);
        }
        if (this.player.hasPermission("rank.chicagobows")) {
            return createMultiColorText(name, new TextColor[]{
                    TextColor.color(0xFF5555), // Red
                    TextColor.color(0x000000), // Black
                    TextColor.color(0xFFFFFF)  // White
            }, false);
        }
        if (this.player.hasPermission("rank.bostonbreeze")) {
            return createMultiColorText(name, new TextColor[]{
                    TextColor.color(0x00AA00), // Green
                    TextColor.color(0xFFFFFF)  // White
            }, false);
        }
        if (this.player.hasPermission("rank.brooklynbuckets")) {
            return createMultiColorText(name, new TextColor[]{
                    TextColor.color(0x000000), // Black
                    TextColor.color(0xFFFFFF)  // White
            }, false);
        }
        if (this.player.hasPermission("rank.miamimagmacubes")) {
            return createMultiColorText(name, new TextColor[]{
                    TextColor.color(0xAA0000), // Dark Red
                    TextColor.color(0xFFAA00), // Orange/Gold
                    TextColor.color(0x000000)  // Black
            }, false);
        }
        if (this.player.hasPermission("rank.atlantaallays")) {
            return createMultiColorText(name, new TextColor[]{
                    TextColor.color(0xFF5555), // Red
                    TextColor.color(0x000000)  // Blue
            }, false);
        }
        if (this.player.hasPermission("rank.goldenstateguardians")) {
            return createMultiColorText(name, new TextColor[]{
                    TextColor.color(0x5555FF), // Blue
                    TextColor.color(0xFFAA00)  // Gold
            }, false);
        }

        // Default - Free Agent yellow
        return Component.text(name, TextColor.color(0xFFFF55));
    }

    /**
     * Get the player's name in WHITE for chat display
     * (Used in chat messages so the name stands out clearly)
     */
    private Component getWhitePlayerName() {
        String name = this.player.getName();
        return Component.text(name, TextColor.color(0xFFFFFF)); // Pure white
    }

    public Component getName() {
        // Build the full display name: [LOGO] [TEAM] RANK PlayerName
        Component teamLogo = getTeamLogo();
        Component teamPrefix = getTeamPrefix();
        Component rankPrefix = getRankPrefix();
        Component playerName = getWhitePlayerName(); // Use WHITE for chat display

        return teamLogo.append(teamPrefix).append(rankPrefix).append(playerName);
    }

    /**
     * Get display name for leaderboards and scoreboards
     * Returns formatted team prefix + rank + player name (with team colors)
     */
    public Component getLeaderboardName() {
        Component teamLogo = getTeamLogo();
        Component teamPrefix = getTeamPrefix();
        Component rankPrefix = getRankPrefix();
        Component playerName = getColoredPlayerName(); // Use team colors for leaderboards

        return teamLogo.append(teamPrefix).append(rankPrefix).append(playerName);
    }

    /**
     * Get simple team abbreviation for compact displays
     */
    public String getTeamAbbreviation() {
        if (this.player.hasPermission("rank.washingtonwithers")) return "WAS";
        if (this.player.hasPermission("rank.philadelphia64s")) return "PHI";
        if (this.player.hasPermission("rank.chicagobows")) return "CHI";
        if (this.player.hasPermission("rank.brooklynbuckets")) return "BKN";
        if (this.player.hasPermission("rank.miamimagmacubes")) return "MIA";
        if (this.player.hasPermission("rank.atlantaallays")) return "ATL";
        if (this.player.hasPermission("rank.goldenstateguardians")) return "GSW";
        if (this.player.hasPermission("rank.bostonbreeze")) return "BOS";

        return "FA"; // Free Agent
    }

    /**
     * Get rank abbreviation (for special ranks/subscriptions)
     */
    public String getRankAbbreviation() {
        if (this.player.hasPermission("rank.admin")) return "ADMIN";
        if (this.player.hasPermission("rank.mod")) return "MOD";
        if (this.player.hasPermission("rank.media")) return "MEDIA";
        if (this.player.hasPermission("rank.coach")) return "COACH";
        if (this.player.hasPermission("rank.referee")) return "REF";
        if (this.player.hasPermission("rank.pro")) return "PRO";
        if (this.player.hasPermission("rank.vip")) return "VIP";

        return ""; // No special rank
    }

    /**
     * Get team color for UI displays
     */
    public TextColor getTeamColor() {
        // MBA Teams - primary color
        if (this.player.hasPermission("rank.washingtonwithers")) return TextColor.color(0x5555FF);
        if (this.player.hasPermission("rank.philadelphia64s")) return TextColor.color(0xFF5555);
        if (this.player.hasPermission("rank.chicagobows")) return TextColor.color(0xFF5555);
        if (this.player.hasPermission("rank.brooklynbuckets")) return TextColor.color(0x000000);
        if (this.player.hasPermission("rank.miamimagmacubes")) return TextColor.color(0xAA0000);
        if (this.player.hasPermission("rank.atlantaallays")) return TextColor.color(0xFF5555);
        if (this.player.hasPermission("rank.goldenstateguardians")) return TextColor.color(0x5555FF);
        if (this.player.hasPermission("rank.bostonbreeze")) return TextColor.color(0x00AA00);

        return TextColor.color(0xFFFF55); // Free Agent yellow
    }

    public void giveCoins(int amount, boolean multiply) {
        int amt = amount;
        if (multiply) {
            if (this.player.hasPermission("rank.pro")) {
                amt = amount * 7;
            } else if (this.player.hasPermission("rank.vip")) {
                amt = amount * 4;
            }
        }
        PlayerDb.add(this.player.getUniqueId(), PlayerDb.Stat.COINS, amt);
        this.player.sendMessage(Message.receiveCoins(amt));
    }
}