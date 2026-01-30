/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  com.comphenix.protocol.ProtocolLibrary
 *  com.comphenix.protocol.ProtocolManager
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Listener
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 */
package me.x_tias.partix;

import co.aikar.commands.PaperCommandManager;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import lombok.Getter;
import me.x_tias.partix.bucks.DailyRewardDb;
import me.x_tias.partix.bucks.DailyRewardListener;
import me.x_tias.partix.command.RefereeCommand;
import me.x_tias.partix.command.CoachCommand;
import me.x_tias.partix.bucks.MbaBucksLeaderboard;
import me.x_tias.partix.bucks.MbaBucksPlaceholder;
import me.x_tias.partix.command.*;
import me.x_tias.partix.database.Databases;
import me.x_tias.partix.mini.anteup.AnteUpManager;
import me.x_tias.partix.mini.betting.BettingManager;
import me.x_tias.partix.plugin.ball.BallFactory;
import me.x_tias.partix.plugin.cooldown.Cooldown;
import me.x_tias.partix.plugin.cosmetics.Cosmetics;
import me.x_tias.partix.command.CrateShopCommand;
import me.x_tias.partix.plugin.cosmetics.ItemShop;
import me.x_tias.partix.plugin.listener.*;
import me.x_tias.partix.plugin.rightclick.RightClickListener;
import me.x_tias.partix.plugin.rightclick.RightClickManager;
import me.x_tias.partix.proam.*;
import me.x_tias.partix.season.AllTimeLeaderboard;
import me.x_tias.partix.season.Season;
import me.x_tias.partix.season.SeasonLeaderboard;
import me.x_tias.partix.season.SeasonPlaceholder;
import me.x_tias.partix.season.LeaderboardManager;
import me.x_tias.partix.season.LeaderboardPlaceholder;
import me.x_tias.partix.server.PlaceLoader;
import me.x_tias.partix.server.rank.Ranks;
import me.x_tias.partix.util.Config;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public final class Partix extends JavaPlugin implements Listener {
    @Getter
    public static ProtocolManager protocolManager;
    private static Partix plugin;
    @Getter
    private ProAmManager proAmManager;
    @Getter
    private ProAmGameManager proAmGameManager;
    @Getter
    private NamespacedKey ballKey;
    @Getter
    private RightClickManager rightClickManager;
    @Getter private Config unicodeConfig;
    @Getter
    private LocationMusicManager locationMusicManager;

    public static Partix getInstance() {
        return plugin;
    }

    public static Player getPlayer(UUID uuid) {
        return plugin.getServer().getPlayer(uuid);
    }

    public void onEnable() {
        this.saveDefaultConfig();
        plugin = this;
        protocolManager = ProtocolLibrary.getProtocolManager();
        this.ballKey = new NamespacedKey(this, "partix-ball");
        this.unicodeConfig = new Config("unicode.yml", this);
        this.rightClickManager = new RightClickManager();
        this.locationMusicManager = new LocationMusicManager();
        this.proAmManager = new ProAmManager(this);
        this.proAmGameManager = new ProAmGameManager(this, this.proAmManager);
        this.database();
        this.factories();
        this.listeners();
        this.placeholders();
        this.cosmetics();
        this.commands();
        this.ranks();
        ProAmLeaderboard.setup();
        new AnteUpManager(this);
        BettingManager.loadGamesFromConfig(this.getConfig());
        BettingManager.startLockMonitor(this);


        // FIXME do the new accuracy title bar system
        // FIXME do the new accuracy auto-aim system

        // FIXME ball not always being killed on game stop (server stop or player quit maybe?)
        // FIXME ball going away when being touched a lot by opposite team then the out of bounds should throw
    }

    private void database() {
        Databases.setup();
        DailyRewardDb.setup();
        Season.setup();
    }

    private void factories() {
        Cooldown.setup();
        BallFactory.setup();
        PlaceLoader.setup();
    }

    private void listeners() {
        this.getServer().getPluginManager().registerEvents(new EventListener(), this);
        this.getServer().getPluginManager().registerEvents(new ActionListener(), this);
        this.getServer().getPluginManager().registerEvents(new WelcomeListener(), this);
        this.getServer().getPluginManager().registerEvents(new QualityListener(), this);
        this.getServer().getPluginManager().registerEvents(new ChatListener(), this);
        this.getServer().getPluginManager().registerEvents(new CosmeticListener(), this);
        this.getServer().getPluginManager().registerEvents(new Cosmetics.CrateHandler(), this);
        this.getServer().getPluginManager().registerEvents(new BettingListener(), this);
        this.getServer().getPluginManager().registerEvents(new DailyRewardListener(), this);
        this.getServer().getPluginManager().registerEvents(new ProAmListener(this.proAmManager), this);
        this.getServer().getPluginManager().registerEvents(new BroadcastExitListener(), this);
        this.getServer().getPluginManager().registerEvents(new BroadcastMovementBlocker(), this);
        this.getServer().getPluginManager().registerEvents(new RightClickListener(), this);
        this.getServer().getPluginManager().registerEvents(this.locationMusicManager, this);
        this.getServer().getPluginManager().registerEvents(me.x_tias.partix.mini.factories.Hub.hub, this);
        
        // Register ProtocolLib packet listener for key press detection
        PlayerInputTracker.register(this);
    }

    private void placeholders() {
        SeasonLeaderboard.setup();
        AllTimeLeaderboard.setup();
        new SeasonPlaceholder().register();
        MbaBucksLeaderboard.setup();
        new MbaBucksPlaceholder().register();
        new ProAmLeaderboardExpansion().register();
        
        // Initialize stat leaderboards for holograms
        LeaderboardManager.setup();
        new LeaderboardPlaceholder().register();
    }

    private void cosmetics() {
        Cosmetics.setup();
        ItemShop.setup();
    }

    private void commands() {
        PaperCommandManager cm = new PaperCommandManager(this);
        cm.registerCommand(new PartixCommand());
        cm.registerCommand(new PartyCommand());
        cm.registerCommand(new ParticleCommand());
        cm.registerCommand(new BettingCommand(this));
        cm.registerCommand(new ProAmCommand(this));
        cm.registerCommand(new StatsCommand());
        cm.registerCommand(new CrateShopCommand());


        // ADD THESE TWO LINES:
        cm.registerCommand(new RefereeCommand());
        cm.registerCommand(new CoachCommand());
        cm.registerCommand(new PayCommand());
        
        // Discord linking commands
        cm.registerCommand(new LinkDiscordCommand());
        cm.registerCommand(new UnlinkDiscordCommand());
        cm.registerCommand(new WhoisCommand());
        cm.registerCommand(new ViewStatsCommand());
        
        // Admin commands
        cm.registerCommand(new RingCommand());
        cm.registerCommand(new AccoladeCommand());
        
        // Test commands
        cm.registerCommand(new HatTestCommand());

    }

    private void ranks() {
        Ranks.setup(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    public void onDisable() {
        plugin = null;
    }

}

