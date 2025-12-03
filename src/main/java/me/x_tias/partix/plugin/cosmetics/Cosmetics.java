/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.Color
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.Particle
 *  org.bukkit.Particle$DustOptions
 *  org.bukkit.Sound
 *  org.bukkit.SoundCategory
 *  org.bukkit.World
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.ArmorStand
 *  org.bukkit.entity.EntityType
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.Action
 *  org.bukkit.event.player.PlayerInteractEvent
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitRunnable
 *  org.bukkit.util.Vector
 */
package me.x_tias.partix.plugin.cosmetics;

import ca.spottedleaf.concurrentutil.completable.Completable;
import lombok.Getter;
import me.x_tias.partix.Partix;
import me.x_tias.partix.database.PlayerDb;
import me.x_tias.partix.plugin.athlete.Athlete;
import me.x_tias.partix.plugin.athlete.AthleteManager;
import me.x_tias.partix.plugin.gui.GUI;
import me.x_tias.partix.plugin.gui.ItemButton;
import me.x_tias.partix.util.Items;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class Cosmetics {
    private static final int LEGEND_BALLS = 1;
    private static final int EPIC_BALLS = 2;
    private static final int RARE_BALLS = 3;
    private static final int COMMON_BALLS = 4;
    public static HashMap<Integer, CosmeticParticle> trails = new HashMap<>();
    public static HashMap<Integer, CosmeticSound> winSongs = new HashMap<>();
    public static HashMap<Integer, CosmeticParticle> explosions = new HashMap<>();
    public static HashMap<Integer, CosmeticSound> greenSounds = new HashMap<>();
    public static HashMap<Integer, CosmeticBallTrail> ballTrails = new HashMap<>();

    private static void setupTrails() {
        trails = new LinkedHashMap<>();
        trails.put(0, new CosmeticParticle("default", Material.BARRIER, "§r§cNo Trail", CosmeticRarity.COMMON, ParticleSet.empty()));
        trails.put(1, new CosmeticParticle("trail.red", Material.RED_WOOL, "§r§fRed Trail", CosmeticRarity.COMMON, ParticleSet.of(Particle.DUST, Color.RED)));
        trails.put(2, new CosmeticParticle("trail.blue", Material.BLUE_WOOL, "§r§fBlue Trail", CosmeticRarity.COMMON, ParticleSet.of(Particle.DUST, Color.BLUE)));
        trails.put(3, new CosmeticParticle("trail.yellow", Material.YELLOW_WOOL, "§r§fYellow Trail", CosmeticRarity.COMMON, ParticleSet.of(Particle.DUST, Color.YELLOW)));
        trails.put(4, new CosmeticParticle("trail.green", Material.GREEN_WOOL, "§r§fGreen Trail", CosmeticRarity.COMMON, ParticleSet.of(Particle.DUST, Color.GREEN)));
        trails.put(5, new CosmeticParticle("trail.purple", Material.PURPLE_WOOL, "§r§fPurple Trail", CosmeticRarity.COMMON, ParticleSet.of(Particle.DUST, Color.PURPLE)));
        trails.put(6, new CosmeticParticle("trail.lime", Material.LIME_WOOL, "§r§fLime Trail", CosmeticRarity.COMMON, ParticleSet.of(Particle.DUST, Color.LIME)));
        trails.put(7, new CosmeticParticle("trail.teal", Material.CYAN_WOOL, "§r§fTeal Trail", CosmeticRarity.COMMON, ParticleSet.of(Particle.DUST, Color.TEAL)));
        trails.put(8, new CosmeticParticle("trail.infection", Material.RED_GLAZED_TERRACOTTA, "§r§fInfection Trail", CosmeticRarity.EPIC, ParticleSet.of(Particle.DUST, Color.fromRGB(8000017)), ParticleSet.of(Particle.DUST, Color.fromRGB(11316647)), ParticleSet.of(Particle.DUST, Color.fromRGB(0x5A5B55)), ParticleSet.of(Particle.DUST, Color.fromRGB(2830377)), ParticleSet.of(Particle.DUST, Color.fromRGB(0x151611))));
        trails.put(9, new CosmeticParticle("trail.summer_sky", Material.ORANGE_GLAZED_TERRACOTTA, "§r§fSummer Sky Trail", CosmeticRarity.RARE, ParticleSet.of(Particle.DUST, Color.fromRGB(16756615)), ParticleSet.of(Particle.DUST, Color.fromRGB(16748146)), ParticleSet.of(Particle.DUST, Color.fromRGB(15559262)), ParticleSet.of(Particle.DUST, Color.fromRGB(5038259)), ParticleSet.of(Particle.DUST, Color.fromRGB(0x377771))));
        trails.put(10, new CosmeticParticle("trail.fallen_blue", Material.BLUE_CONCRETE_POWDER, "§r§fFallen Blue Trail", CosmeticRarity.RARE, ParticleSet.of(Particle.DUST, Color.fromRGB(0xF6F6F6)), ParticleSet.of(Particle.DUST, Color.fromRGB(14281200)), ParticleSet.of(Particle.DUST, Color.fromRGB(5618911)), ParticleSet.of(Particle.DUST, Color.fromRGB(2571129)), ParticleSet.of(Particle.DUST, Color.fromRGB(1319210))));
        trails.put(11, new CosmeticParticle("trail.blood_gold", Material.CRIMSON_STEM, "§r§fBlood Gold Trail", CosmeticRarity.EPIC, ParticleSet.of(Particle.DUST, Color.fromRGB(0x660000)), ParticleSet.of(Particle.DUST, Color.fromRGB(0x800000)), ParticleSet.of(Particle.DUST, Color.fromRGB(0x880303)), ParticleSet.of(Particle.DUST, Color.fromRGB(15583591)), ParticleSet.of(Particle.DUST, Color.fromRGB(13938487))));
        trails.put(12, new CosmeticParticle("trail.coffee", Material.STRIPPED_OAK_LOG, "§r§fCoffee Trail", CosmeticRarity.RARE, ParticleSet.of(Particle.DUST, Color.fromRGB(10712119)), ParticleSet.of(Particle.DUST, Color.fromRGB(12228432)), ParticleSet.of(Particle.DUST, Color.fromRGB(14005628)), ParticleSet.of(Particle.DUST, Color.fromRGB(16773850)), ParticleSet.of(Particle.DUST, Color.fromRGB(16644333))));
        trails.put(13, new CosmeticParticle("trail.twilight", Material.PURPLE_CONCRETE_POWDER, "§r§fTwilight Trail", CosmeticRarity.EPIC, ParticleSet.of(Particle.DUST, Color.fromRGB(1968444)), ParticleSet.of(Particle.DUST, Color.fromRGB(6696841)), ParticleSet.of(Particle.DUST, Color.fromRGB(9264548)), ParticleSet.of(Particle.DUST, Color.fromRGB(13415123)), ParticleSet.of(Particle.DUST, Color.fromRGB(0xFFFFFF))));
        trails.put(14, new CosmeticParticle("trail.steve", Material.WHITE_TERRACOTTA, "§r§fSteve Trail", CosmeticRarity.RARE, ParticleSet.of(Particle.DUST, Color.fromRGB(16703692)), ParticleSet.of(Particle.DUST, Color.fromRGB(6367488)), ParticleSet.of(Particle.DUST, Color.fromRGB(1150842)), ParticleSet.of(Particle.DUST, Color.fromRGB(543382)), ParticleSet.of(Particle.DUST, Color.fromRGB(197379))));
        trails.put(15, new CosmeticParticle("trail.warm_cyan", Material.LIGHT_BLUE_CONCRETE, "§r§fWarm Cyan Trail§6", CosmeticRarity.RARE, ParticleSet.of(Particle.DUST, Color.fromRGB(15465466)), ParticleSet.of(Particle.DUST, Color.fromRGB(12973552)), ParticleSet.of(Particle.DUST, Color.fromRGB(10481639)), ParticleSet.of(Particle.DUST, Color.fromRGB(7989726)), ParticleSet.of(Particle.DUST, Color.fromRGB(5497812))));
        trails.put(16, new CosmeticParticle("trail.eucalyptus", Material.OAK_LEAVES, "§r§fEucalyptus Trail", CosmeticRarity.RARE, ParticleSet.of(Particle.DUST, Color.fromRGB(15134951)), ParticleSet.of(Particle.DUST, Color.fromRGB(12637379)), ParticleSet.of(Particle.DUST, Color.fromRGB(11257262)), ParticleSet.of(Particle.DUST, Color.fromRGB(5536869)), ParticleSet.of(Particle.DUST, Color.fromRGB(804132))));
        trails.put(17, new CosmeticParticle("trail.midnight", Material.BLACK_CONCRETE_POWDER, "§r§fMidnight Trail", CosmeticRarity.EPIC, ParticleSet.of(Particle.DUST, Color.fromRGB(921159)), ParticleSet.of(Particle.DUST, Color.fromRGB(1579353)), ParticleSet.of(Particle.DUST, Color.fromRGB(2559289)), ParticleSet.of(Particle.DUST, Color.fromRGB(3347783)), ParticleSet.of(Particle.DUST, Color.fromRGB(1508392))));
        trails.put(18, new CosmeticParticle("trail.orange_soda", Material.ORANGE_CONCRETE_POWDER, "§r§fOrange Soda Trail", CosmeticRarity.RARE, ParticleSet.of(Particle.DUST, Color.fromRGB(15544577)), ParticleSet.of(Particle.DUST, Color.fromRGB(16744502)), ParticleSet.of(Particle.DUST, Color.fromRGB(16750949)), ParticleSet.of(Particle.DUST, Color.fromRGB(3735477)), ParticleSet.of(Particle.DUST, Color.fromRGB(4504424))));
        trails.put(19, new CosmeticParticle("trail.evening_flame", Material.PURPLE_GLAZED_TERRACOTTA, "§r§fEvening Flame Trail", CosmeticRarity.EPIC, ParticleSet.of(Particle.DUST, Color.fromRGB(2694986)), ParticleSet.of(Particle.DUST, Color.fromRGB(3482180)), ParticleSet.of(Particle.DUST, Color.fromRGB(2829135)), ParticleSet.of(Particle.DUST, Color.fromRGB(15703118)), ParticleSet.of(Particle.DUST, Color.fromRGB(16765793))));
        trails.put(20, new CosmeticParticle("trail.mango", Material.STRIPPED_ACACIA_WOOD, "§r§fMango Trail", CosmeticRarity.RARE, ParticleSet.of(Particle.DUST, Color.fromRGB(16585986)), ParticleSet.of(Particle.DUST, Color.fromRGB(16600834)), ParticleSet.of(Particle.DUST, Color.fromRGB(16618754)), ParticleSet.of(Particle.DUST, Color.fromRGB(16629250)), ParticleSet.of(Particle.DUST, Color.fromRGB(16634882))));
        trails.put(21, new CosmeticParticle("trail.kraken", Material.LAPIS_BLOCK, "§r§fDeep Sea Kraken Trail", CosmeticRarity.EPIC, ParticleSet.of(Particle.DUST, Color.fromRGB(51)), ParticleSet.of(Particle.DUST, Color.fromRGB(13107)), ParticleSet.of(Particle.DUST, Color.fromRGB(26163)), ParticleSet.of(Particle.DUST, Color.fromRGB(39219)), ParticleSet.of(Particle.DUST, Color.fromRGB(52275))));
        trails.put(22, new CosmeticParticle("trail.dragon_fruit", Material.PINK_GLAZED_TERRACOTTA, "§r§fDragon Fruit Trail", CosmeticRarity.RARE, ParticleSet.of(Particle.DUST, Color.fromRGB(0xCC3399)), ParticleSet.of(Particle.DUST, Color.fromRGB(0xCC9999)), ParticleSet.of(Particle.DUST, Color.fromRGB(65535)), ParticleSet.of(Particle.DUST, Color.fromRGB(65535))));
        trails.put(23, new CosmeticParticle("trail.orb", Material.EXPERIENCE_BOTTLE, "§r§fOrb Trail", CosmeticRarity.LEGENDARY, ParticleSet.of(Particle.TOTEM_OF_UNDYING)));
        trails.put(24, new CosmeticParticle("trail.blue_flame", Material.SOUL_CAMPFIRE, "§r§fBlue Flame Trail", CosmeticRarity.LEGENDARY, ParticleSet.of(Particle.SOUL_FIRE_FLAME)));
        trails.put(25, new CosmeticParticle("trail.flame", Material.CAMPFIRE, "§r§fFlame Trail", CosmeticRarity.RARE, ParticleSet.of(Particle.FLAME)));
        trails.put(26, new CosmeticParticle("trail.vice", Material.PINK_GLAZED_TERRACOTTA, "§r§fVice Trail", CosmeticRarity.EPIC, ParticleSet.of(Particle.DUST, Color.fromRGB(6801392)), ParticleSet.of(Particle.DUST, Color.fromRGB(6801392)), ParticleSet.of(Particle.DUST, Color.fromRGB(16290023)), ParticleSet.of(Particle.DUST, Color.fromRGB(16290023)), ParticleSet.of(Particle.DUST, Color.fromRGB(0xFFFFFF))));
        trails.put(27, new CosmeticParticle("trail.camouflage", Material.DARK_OAK_LEAVES, "§r§fCamouflage Trail", CosmeticRarity.EPIC, ParticleSet.of(Particle.DUST, Color.fromRGB(3875612)), ParticleSet.of(Particle.DUST, Color.fromRGB(8869680)), ParticleSet.of(Particle.DUST, Color.fromRGB(11445144)), ParticleSet.of(Particle.DUST, Color.fromRGB(3425052)), ParticleSet.of(Particle.DUST, Color.fromRGB(7177301))));
        trails.put(28, new CosmeticParticle("trail.halloween", Material.JACK_O_LANTERN, "§r§fHalloween Trail", CosmeticRarity.RARE, ParticleSet.of(Particle.DUST, Color.fromRGB(16751104)), ParticleSet.of(Particle.DUST, Color.fromRGB(16751104)), ParticleSet.of(Particle.DUST, Color.fromRGB(0)), ParticleSet.of(Particle.DUST, Color.fromRGB(655104)), ParticleSet.of(Particle.DUST, Color.fromRGB(13172991))));
        trails.put(29, new CosmeticParticle("trail.christmas", Material.CHEST, "§r§fChristmas Trail", CosmeticRarity.RARE, ParticleSet.of(Particle.DUST, Color.fromRGB(1209369)), ParticleSet.of(Particle.DUST, Color.fromRGB(1209369)), ParticleSet.of(Particle.DUST, Color.fromRGB(13055542)), ParticleSet.of(Particle.DUST, Color.fromRGB(13055542)), ParticleSet.of(Particle.DUST, Color.fromRGB(0xFFFFFF))));
        trails.put(30, new CosmeticParticle("trail.ender", Material.ENDER_CHEST, "§r§fEnder Trail", CosmeticRarity.RARE, ParticleSet.of(Particle.DUST, Color.fromRGB(0)), ParticleSet.of(Particle.DUST, Color.fromRGB(13326847)), ParticleSet.of(Particle.DUST, Color.fromRGB(0xAA1BAB)), ParticleSet.of(Particle.DUST, Color.fromRGB(15568127)), ParticleSet.of(Particle.DUST, Color.fromRGB(0))));
        trails.put(31, new CosmeticParticle("trail.nether", Material.WARPED_STEM, "§r§fNether Trail", CosmeticRarity.EPIC, ParticleSet.of(Particle.DUST, Color.fromRGB(0x511515)), ParticleSet.of(Particle.DUST, Color.fromRGB(0x723232)), ParticleSet.of(Particle.DUST, Color.fromRGB(11280416)), ParticleSet.of(Particle.DUST, Color.fromRGB(16680760)), ParticleSet.of(Particle.DUST, Color.fromRGB(1153924))));
        trails.put(32, new CosmeticParticle("trail.raspberry", Material.WARPED_STEM, "§r§fRaspberry Trail", CosmeticRarity.RARE, ParticleSet.of(Particle.DUST, Color.fromRGB(9056383)), ParticleSet.of(Particle.DUST, Color.fromRGB(7972819)), ParticleSet.of(Particle.DUST, Color.fromRGB(6849468))));
        trails.put(33, new CosmeticParticle("trail.pastel", Material.LIME_TERRACOTTA, "§r§fPastel Trail", CosmeticRarity.EPIC, ParticleSet.of(Particle.DUST, Color.fromRGB(11179738)), ParticleSet.of(Particle.DUST, Color.fromRGB(12974805)), ParticleSet.of(Particle.DUST, Color.fromRGB(0xFFFFD2))));
        trails.put(34, new CosmeticParticle("trail.nursery", Material.LIME_CONCRETE_POWDER, "§r§fNursery Trail§6", CosmeticRarity.RARE, ParticleSet.of(Particle.DUST, Color.fromRGB(16770938)), ParticleSet.of(Particle.DUST, Color.fromRGB(2907949))));
        trails.put(35, new CosmeticParticle("trail.royal", Material.BLUE_CONCRETE, "§r§fRoyal Trail", CosmeticRarity.RARE, ParticleSet.of(Particle.DUST, Color.fromRGB(2313840)), ParticleSet.of(Particle.DUST, Color.fromRGB(16513214))));
        trails.put(36, new CosmeticParticle("trail.maroon_blues", Material.PURPLE_TERRACOTTA, "§r§fMaroon Blues Trail§6", CosmeticRarity.EPIC, ParticleSet.of(Particle.DUST, Color.fromRGB(4230854)), ParticleSet.of(Particle.DUST, Color.fromRGB(8003656)), ParticleSet.of(Particle.DUST, Color.fromRGB(1976161))));
        trails.put(37, new CosmeticParticle("trail.maturity", Material.ORANGE_TERRACOTTA, "§r§fMaturity Trail", CosmeticRarity.EPIC, ParticleSet.of(Particle.DUST, Color.fromRGB(12079170)), ParticleSet.of(Particle.DUST, Color.fromRGB(15198417)), ParticleSet.of(Particle.DUST, Color.fromRGB(10993326))));
        trails.put(39, new CosmeticParticle("trail.soul", Material.SOUL_SAND, "§r§fSoul Trail", CosmeticRarity.LEGENDARY, ParticleSet.of(Particle.SOUL), ParticleSet.of(Particle.SCULK_SOUL)));
        trails.put(40, new CosmeticParticle("trail.spell", Material.SOUL_SAND, "§r§fSpell Trail", CosmeticRarity.LEGENDARY, ParticleSet.of(Particle.ENCHANT), ParticleSet.of(Particle.ENCHANT), ParticleSet.of(Particle.TOTEM_OF_UNDYING)));
        trails.put(41, new CosmeticParticle("trail.pro", Material.GOLD_BLOCK, "§6§lSupreme Trail (PRO)", CosmeticRarity.LEGENDARY, ParticleSet.of(Particle.DUST, Color.fromRGB(255, 215, 0)), ParticleSet.of(Particle.ENCHANT), ParticleSet.of(Particle.END_ROD)));
        trails.put(42, new CosmeticParticle("trail.vip", Material.LIME_CONCRETE, "§a§lEmerald Glow Trail (VIP)", CosmeticRarity.LEGENDARY, ParticleSet.of(Particle.DUST, Color.fromRGB(50, 205, 50)), ParticleSet.of(Particle.WITCH), ParticleSet.of(Particle.TOTEM_OF_UNDYING)));
    }

    private static void setupExplosions() {
        explosions = new LinkedHashMap<>();
        explosions.put(0, new CosmeticParticle("default", Material.BARRIER, "§r§cNo Explosion", CosmeticRarity.COMMON, ParticleSet.empty()));
        explosions.put(1, new CosmeticParticle("explosion.red", Material.RED_WOOL, "§r§eRed Explosion", CosmeticRarity.COMMON, ParticleSet.of(Particle.DUST, Color.RED)));
        explosions.put(2, new CosmeticParticle("explosion.blue", Material.BLUE_WOOL, "§r§eBlue Explosion", CosmeticRarity.COMMON, ParticleSet.of(Particle.DUST, Color.BLUE)));
        explosions.put(3, new CosmeticParticle("explosion.yellow", Material.YELLOW_WOOL, "§r§eYellow Explosion", CosmeticRarity.COMMON, ParticleSet.of(Particle.DUST, Color.YELLOW)));
        explosions.put(4, new CosmeticParticle("explosion.green", Material.GREEN_WOOL, "§r§eGreen Explosion", CosmeticRarity.COMMON, ParticleSet.of(Particle.DUST, Color.GREEN)));
        explosions.put(5, new CosmeticParticle("explosion.purple", Material.PURPLE_WOOL, "§r§ePurple Explosion", CosmeticRarity.COMMON, ParticleSet.of(Particle.DUST, Color.PURPLE)));
        explosions.put(6, new CosmeticParticle("explosion.lime", Material.LIME_WOOL, "§r§eLime Explosion", CosmeticRarity.COMMON, ParticleSet.of(Particle.DUST, Color.LIME)));
        explosions.put(7, new CosmeticParticle("explosion.teal", Material.CYAN_WOOL, "§r§eTeal Explosion§6", CosmeticRarity.COMMON, ParticleSet.of(Particle.DUST, Color.TEAL)));
        explosions.put(8, new CosmeticParticle("explosion.infection", Material.RED_GLAZED_TERRACOTTA, "§r§eInfection Explosion", CosmeticRarity.EPIC, ParticleSet.of(Particle.DUST, Color.fromRGB(8000017)), ParticleSet.of(Particle.DUST, Color.fromRGB(11316647)), ParticleSet.of(Particle.DUST, Color.fromRGB(0x5A5B55)), ParticleSet.of(Particle.DUST, Color.fromRGB(2830377)), ParticleSet.of(Particle.DUST, Color.fromRGB(0x151611))));
        explosions.put(9, new CosmeticParticle("explosion.summer_sky", Material.ORANGE_GLAZED_TERRACOTTA, "§r§eSummer Sky Explosion", CosmeticRarity.RARE, ParticleSet.of(Particle.DUST, Color.fromRGB(16756615)), ParticleSet.of(Particle.DUST, Color.fromRGB(16748146)), ParticleSet.of(Particle.DUST, Color.fromRGB(15559262)), ParticleSet.of(Particle.DUST, Color.fromRGB(5038259)), ParticleSet.of(Particle.DUST, Color.fromRGB(0x377771))));
        explosions.put(10, new CosmeticParticle("explosion.fallen_blue", Material.BLUE_CONCRETE_POWDER, "§r§eFallen Blue Explosion", CosmeticRarity.RARE, ParticleSet.of(Particle.DUST, Color.fromRGB(0xF6F6F6)), ParticleSet.of(Particle.DUST, Color.fromRGB(14281200)), ParticleSet.of(Particle.DUST, Color.fromRGB(5618911)), ParticleSet.of(Particle.DUST, Color.fromRGB(2571129)), ParticleSet.of(Particle.DUST, Color.fromRGB(1319210))));
        explosions.put(11, new CosmeticParticle("explosion.blood_gold", Material.CRIMSON_STEM, "§r§eBlood Gold Explosion", CosmeticRarity.EPIC, ParticleSet.of(Particle.DUST, Color.fromRGB(0x660000)), ParticleSet.of(Particle.DUST, Color.fromRGB(0x800000)), ParticleSet.of(Particle.DUST, Color.fromRGB(0x880303)), ParticleSet.of(Particle.DUST, Color.fromRGB(15583591)), ParticleSet.of(Particle.DUST, Color.fromRGB(13938487))));
        explosions.put(12, new CosmeticParticle("explosion.coffee", Material.STRIPPED_OAK_LOG, "§r§eCoffee Explosion", CosmeticRarity.RARE, ParticleSet.of(Particle.DUST, Color.fromRGB(10712119)), ParticleSet.of(Particle.DUST, Color.fromRGB(12228432)), ParticleSet.of(Particle.DUST, Color.fromRGB(14005628)), ParticleSet.of(Particle.DUST, Color.fromRGB(16773850)), ParticleSet.of(Particle.DUST, Color.fromRGB(16644333))));
        explosions.put(13, new CosmeticParticle("explosion.twilight", Material.PURPLE_CONCRETE_POWDER, "§r§eTwilight Explosion", CosmeticRarity.EPIC, ParticleSet.of(Particle.DUST, Color.fromRGB(1968444)), ParticleSet.of(Particle.DUST, Color.fromRGB(6696841)), ParticleSet.of(Particle.DUST, Color.fromRGB(9264548)), ParticleSet.of(Particle.DUST, Color.fromRGB(13415123)), ParticleSet.of(Particle.DUST, Color.fromRGB(0xFFFFFF))));
        explosions.put(14, new CosmeticParticle("explosion.steve", Material.WHITE_TERRACOTTA, "§r§eSteve Explosion", CosmeticRarity.RARE, ParticleSet.of(Particle.DUST, Color.fromRGB(16703692)), ParticleSet.of(Particle.DUST, Color.fromRGB(6367488)), ParticleSet.of(Particle.DUST, Color.fromRGB(1150842)), ParticleSet.of(Particle.DUST, Color.fromRGB(543382)), ParticleSet.of(Particle.DUST, Color.fromRGB(197379))));
        explosions.put(15, new CosmeticParticle("explosion.warm_cyan", Material.LIGHT_BLUE_CONCRETE, "§r§eWarm Cyan Explosion§6", CosmeticRarity.RARE, ParticleSet.of(Particle.DUST, Color.fromRGB(15465466)), ParticleSet.of(Particle.DUST, Color.fromRGB(12973552)), ParticleSet.of(Particle.DUST, Color.fromRGB(10481639)), ParticleSet.of(Particle.DUST, Color.fromRGB(7989726)), ParticleSet.of(Particle.DUST, Color.fromRGB(5497812))));
        explosions.put(16, new CosmeticParticle("explosion.eucalyptus", Material.OAK_LEAVES, "§r§eEucalyptus Explosion", CosmeticRarity.RARE, ParticleSet.of(Particle.DUST, Color.fromRGB(15134951)), ParticleSet.of(Particle.DUST, Color.fromRGB(12637379)), ParticleSet.of(Particle.DUST, Color.fromRGB(11257262)), ParticleSet.of(Particle.DUST, Color.fromRGB(5536869)), ParticleSet.of(Particle.DUST, Color.fromRGB(804132))));
        explosions.put(17, new CosmeticParticle("explosion.midnight", Material.BLACK_CONCRETE_POWDER, "§r§eMidnight Explosion", CosmeticRarity.EPIC, ParticleSet.of(Particle.DUST, Color.fromRGB(921159)), ParticleSet.of(Particle.DUST, Color.fromRGB(1579353)), ParticleSet.of(Particle.DUST, Color.fromRGB(2559289)), ParticleSet.of(Particle.DUST, Color.fromRGB(3347783)), ParticleSet.of(Particle.DUST, Color.fromRGB(1508392))));
        explosions.put(18, new CosmeticParticle("explosion.orange_soda", Material.ORANGE_CONCRETE_POWDER, "§r§eOrange Soda Explosion", CosmeticRarity.RARE, ParticleSet.of(Particle.DUST, Color.fromRGB(15544577)), ParticleSet.of(Particle.DUST, Color.fromRGB(16744502)), ParticleSet.of(Particle.DUST, Color.fromRGB(16750949)), ParticleSet.of(Particle.DUST, Color.fromRGB(3735477)), ParticleSet.of(Particle.DUST, Color.fromRGB(4504424))));
        explosions.put(19, new CosmeticParticle("explosion.flame", Material.PURPLE_GLAZED_TERRACOTTA, "§r§eEvening Flame Explosion§6", CosmeticRarity.EPIC, ParticleSet.of(Particle.DUST, Color.fromRGB(2694986)), ParticleSet.of(Particle.DUST, Color.fromRGB(3482180)), ParticleSet.of(Particle.DUST, Color.fromRGB(2829135)), ParticleSet.of(Particle.DUST, Color.fromRGB(15703118)), ParticleSet.of(Particle.DUST, Color.fromRGB(16765793)), ParticleSet.of(Particle.FLAME)));
        explosions.put(20, new CosmeticParticle("explosion.mango", Material.STRIPPED_ACACIA_WOOD, "§r§eMango Explosion", CosmeticRarity.RARE, ParticleSet.of(Particle.DUST, Color.fromRGB(16585986)), ParticleSet.of(Particle.DUST, Color.fromRGB(16600834)), ParticleSet.of(Particle.DUST, Color.fromRGB(16618754)), ParticleSet.of(Particle.DUST, Color.fromRGB(16629250)), ParticleSet.of(Particle.DUST, Color.fromRGB(16634882))));
        explosions.put(21, new CosmeticParticle("explosion.kraken", Material.LAPIS_BLOCK, "§r§eDeep Sea Kraken Explosion§6", CosmeticRarity.EPIC, ParticleSet.of(Particle.DUST, Color.fromRGB(51)), ParticleSet.of(Particle.DUST, Color.fromRGB(13107)), ParticleSet.of(Particle.DUST, Color.fromRGB(26163)), ParticleSet.of(Particle.DUST, Color.fromRGB(39219)), ParticleSet.of(Particle.DUST, Color.fromRGB(52275))));
        explosions.put(22, new CosmeticParticle("explosion.dragon_fruit", Material.PINK_GLAZED_TERRACOTTA, "§r§eDragon Fruit Explosion", CosmeticRarity.RARE, ParticleSet.of(Particle.DUST, Color.fromRGB(0xCC3399)), ParticleSet.of(Particle.DUST, Color.fromRGB(0xCC9999)), ParticleSet.of(Particle.DUST, Color.fromRGB(65535)), ParticleSet.of(Particle.DUST, Color.fromRGB(65535))));
        explosions.put(23, new CosmeticParticle("explosion.orb", Material.EXPERIENCE_BOTTLE, "§r§eOrb Explosion", CosmeticRarity.LEGENDARY, ParticleSet.of(Particle.TOTEM_OF_UNDYING)));
        explosions.put(24, new CosmeticParticle("explosion.blue_flame", Material.SOUL_CAMPFIRE, "§r§eBlue Flame Explosion", CosmeticRarity.LEGENDARY, ParticleSet.of(Particle.SOUL_FIRE_FLAME), ParticleSet.of(Particle.FLAME)));
        explosions.put(25, new CosmeticParticle("explosion.flame", Material.CAMPFIRE, "§r§eFlame Explosion", CosmeticRarity.RARE, ParticleSet.of(Particle.FLAME)));
        explosions.put(26, new CosmeticParticle("explosion.vice", Material.PINK_GLAZED_TERRACOTTA, "§r§eVice Explosion", CosmeticRarity.EPIC, ParticleSet.of(Particle.DUST, Color.fromRGB(6801392)), ParticleSet.of(Particle.DUST, Color.fromRGB(6801392)), ParticleSet.of(Particle.DUST, Color.fromRGB(16290023)), ParticleSet.of(Particle.DUST, Color.fromRGB(16290023)), ParticleSet.of(Particle.DUST, Color.fromRGB(0xFFFFFF))));
        explosions.put(27, new CosmeticParticle("explosion.camouflage", Material.DARK_OAK_LEAVES, "§r§eCamouflage Explosion", CosmeticRarity.EPIC, ParticleSet.of(Particle.DUST, Color.fromRGB(3875612)), ParticleSet.of(Particle.DUST, Color.fromRGB(8869680)), ParticleSet.of(Particle.DUST, Color.fromRGB(11445144)), ParticleSet.of(Particle.DUST, Color.fromRGB(3425052)), ParticleSet.of(Particle.DUST, Color.fromRGB(7177301))));
        explosions.put(28, new CosmeticParticle("explosion.halloween", Material.JACK_O_LANTERN, "§r§eHalloween Explosion", CosmeticRarity.RARE, ParticleSet.of(Particle.DUST, Color.fromRGB(16751104)), ParticleSet.of(Particle.DUST, Color.fromRGB(16751104)), ParticleSet.of(Particle.DUST, Color.fromRGB(0)), ParticleSet.of(Particle.DUST, Color.fromRGB(655104)), ParticleSet.of(Particle.DUST, Color.fromRGB(13172991))));
        explosions.put(29, new CosmeticParticle("explosion.christmas", Material.CHEST, "§r§eChristmas Explosion", CosmeticRarity.RARE, ParticleSet.of(Particle.DUST, Color.fromRGB(1209369)), ParticleSet.of(Particle.DUST, Color.fromRGB(1209369)), ParticleSet.of(Particle.DUST, Color.fromRGB(13055542)), ParticleSet.of(Particle.DUST, Color.fromRGB(13055542)), ParticleSet.of(Particle.DUST, Color.fromRGB(0xFFFFFF))));
        explosions.put(30, new CosmeticParticle("explosion.ender", Material.ENDER_CHEST, "§r§eEnder Explosion", CosmeticRarity.RARE, ParticleSet.of(Particle.DUST, Color.fromRGB(0)), ParticleSet.of(Particle.DUST, Color.fromRGB(13326847)), ParticleSet.of(Particle.DUST, Color.fromRGB(0xAA1BAB)), ParticleSet.of(Particle.DUST, Color.fromRGB(15568127)), ParticleSet.of(Particle.DUST, Color.fromRGB(0))));
        explosions.put(31, new CosmeticParticle("explosion.nether", Material.WARPED_STEM, "§r§eNether Explosion", CosmeticRarity.EPIC, ParticleSet.of(Particle.DUST, Color.fromRGB(0x511515)), ParticleSet.of(Particle.DUST, Color.fromRGB(0x723232)), ParticleSet.of(Particle.DUST, Color.fromRGB(11280416)), ParticleSet.of(Particle.DUST, Color.fromRGB(16680760)), ParticleSet.of(Particle.DUST, Color.fromRGB(1153924))));
        explosions.put(32, new CosmeticParticle("explosion.raspberry", Material.WARPED_STEM, "§r§eRaspberry Explosion", CosmeticRarity.RARE, ParticleSet.of(Particle.DUST, Color.fromRGB(9056383)), ParticleSet.of(Particle.DUST, Color.fromRGB(7972819)), ParticleSet.of(Particle.DUST, Color.fromRGB(6849468))));
        explosions.put(33, new CosmeticParticle("explosion.pastel", Material.LIME_TERRACOTTA, "§r§ePastel Explosion", CosmeticRarity.EPIC, ParticleSet.of(Particle.DUST, Color.fromRGB(11179738)), ParticleSet.of(Particle.DUST, Color.fromRGB(12974805)), ParticleSet.of(Particle.DUST, Color.fromRGB(0xFFFFD2))));
        explosions.put(34, new CosmeticParticle("explosion.nursery", Material.LIME_CONCRETE_POWDER, "§r§eNursery Explosion", CosmeticRarity.RARE, ParticleSet.of(Particle.DUST, Color.fromRGB(16770938)), ParticleSet.of(Particle.DUST, Color.fromRGB(2907949))));
        explosions.put(35, new CosmeticParticle("explosion.royal", Material.BLUE_CONCRETE, "§r§eRoyal Explosion", CosmeticRarity.RARE, ParticleSet.of(Particle.DUST, Color.fromRGB(2313840)), ParticleSet.of(Particle.DUST, Color.fromRGB(16513214))));
        explosions.put(36, new CosmeticParticle("explosion.maroon_blues", Material.PURPLE_TERRACOTTA, "§r§eMaroon Blues Explosion", CosmeticRarity.EPIC, ParticleSet.of(Particle.DUST, Color.fromRGB(4230854)), ParticleSet.of(Particle.DUST, Color.fromRGB(8003656)), ParticleSet.of(Particle.DUST, Color.fromRGB(1976161))));
        explosions.put(37, new CosmeticParticle("explosion.maturity", Material.ORANGE_TERRACOTTA, "§r§eMaturity Explosion", CosmeticRarity.EPIC, ParticleSet.of(Particle.DUST, Color.fromRGB(12079170)), ParticleSet.of(Particle.DUST, Color.fromRGB(15198417)), ParticleSet.of(Particle.DUST, Color.fromRGB(10993326))));
        explosions.put(38, new CosmeticParticle("explosion.heart", Material.APPLE, "§r§eHeart Explosion", CosmeticRarity.LEGENDARY, ParticleSet.of(Particle.HEART)));
        explosions.put(39, new CosmeticParticle("explosion.soul", Material.SOUL_SAND, "§r§eSoul Explosion", CosmeticRarity.LEGENDARY, ParticleSet.of(Particle.SOUL), ParticleSet.of(Particle.SCULK_SOUL)));
        explosions.put(40, new CosmeticParticle("explosion.spell", Material.SOUL_SAND, "§r§eSpell Explosion", CosmeticRarity.LEGENDARY, ParticleSet.of(Particle.ENCHANT), ParticleSet.of(Particle.ENCHANT), ParticleSet.of(Particle.TOTEM_OF_UNDYING)));
        explosions.put(41, new CosmeticParticle("explosion.pro", Material.FIREWORK_ROCKET, "§6§l Grand Finale Explosion (PRO)", CosmeticRarity.LEGENDARY, ParticleSet.of(Particle.EXPLOSION), ParticleSet.of(Particle.FIREWORK), ParticleSet.of(Particle.FLASH)));
        explosions.put(42, new CosmeticParticle("explosion.vip", Material.SLIME_BLOCK, "§a§lGreen Shockwave Explosion (VIP)", CosmeticRarity.LEGENDARY, ParticleSet.of(Particle.EXPLOSION), ParticleSet.of(Particle.HAPPY_VILLAGER), ParticleSet.of(Particle.DRAGON_BREATH)));
        explosions.put(47, new CosmeticParticle("explosion.quartz", Material.QUARTZ_BLOCK, "Quartz Gleam Explosion", CosmeticRarity.LEGENDARY, ParticleSet.of(Particle.DUST, Color.fromRGB(0xFFFFFF)), ParticleSet.of(Particle.FLASH)));
        explosions.put(52, new CosmeticParticle("explosion.prismarine", Material.PRISMARINE, "Prismarine Surge Explosion", CosmeticRarity.RARE, ParticleSet.of(Particle.DUST, Color.fromRGB(52945)), ParticleSet.of(Particle.FLAME)));
        explosions.put(53, new CosmeticParticle("explosion.blueorange", Material.BLUE_CONCRETE, "Blue & Orange Explosion", CosmeticRarity.LEGENDARY, ParticleSet.of(Particle.DUST, Color.BLUE), ParticleSet.of(Particle.DUST, Color.ORANGE)));
        for (Map.Entry<Integer, CosmeticParticle> entry : explosions.entrySet()) {
            CosmeticParticle explosion = entry.getValue();
            if (explosion.getKey() == null || explosion.getKey().isEmpty()) {
                Bukkit.getLogger().severe("[ERROR] Explosion with invalid key: " + explosion.getName());
                continue;
            }
            if (explosion.getMaterial() == null || explosion.getMaterial() == Material.AIR) {
                Bukkit.getLogger().severe("[ERROR] Explosion with invalid material: " + explosion.getName());
                explosion.setMaterial(Material.BARRIER);
            }
            Bukkit.getLogger().info("[DEBUG] Explosion loaded: " + explosion.getName() + ", Material: " + explosion.getMaterial().name());
        }
    }

    private static void setupWinSongs() {
        winSongs = new LinkedHashMap<>();
        winSongs.put(0, CosmeticSound.empty());
    }

    private static void setupGreenSounds() {
        greenSounds.put(0, new CosmeticSound("greensound.no_sound", Material.BARRIER, "§lNo Sound", "No Green Sound Effect", CosmeticRarity.COMMON, "greensound.none", "partix.sound.no_sound"));
        greenSounds.put(1, new CosmeticSound("greensound.whistle", Material.FEATHER, "§l§eWhistle", "Sharp and loud whistle", CosmeticRarity.EPIC, "greensound.whistle", "partix.sound.whistle"));
        greenSounds.put(2, new CosmeticSound("greensound.money", Material.GOLD_INGOT, "§l§6Money", "Cha-ching sound of wealth", CosmeticRarity.RARE, "greensound.money", "partix.sound.money"));
        greenSounds.put(3, new CosmeticSound("greensound.jazzhorn", Material.NOTE_BLOCK, "§l§dJazz Horn", "Classic jazzy horn", CosmeticRarity.RARE, "greensound.jazzhorn", "partix.sound.jazzhorn"));
        greenSounds.put(4, new CosmeticSound("greensound.horse", Material.HAY_BLOCK, "§l§7Horse", "Neigh of a horse", CosmeticRarity.RARE, "greensound.horse", "partix.sound.horse"));
        greenSounds.put(5, new CosmeticSound("greensound.horn", Material.GOAT_HORN, "§l§fHorn", "Loud horn blast", CosmeticRarity.LEGENDARY, "greensound.horn", "partix.sound.horn"));
        greenSounds.put(6, new CosmeticSound("greensound.goofy", Material.SLIME_BALL, "§l§aGoofy", "Goofy laugh sound", CosmeticRarity.RARE, "greensound.goofy", "partix.sound.goofy"));
        greenSounds.put(7, new CosmeticSound("greensound.glorious", Material.GOLD_BLOCK, "§l§eGlorious", "A triumphant sound", CosmeticRarity.COMMON, "greensound.glorious", "partix.sound.glorious"));
        greenSounds.put(8, new CosmeticSound("greensound.flame", Material.BLAZE_POWDER, "§l§6Flame", "Burning flame sound", CosmeticRarity.COMMON, "greensound.flame", "partix.sound.flame"));
        greenSounds.put(9, new CosmeticSound("greensound.fireworks", Material.FIREWORK_ROCKET, "§l§cFireworks", "Exploding fireworks sound", CosmeticRarity.COMMON, "greensound.fireworks", "partix.sound.fireworks"));
        greenSounds.put(10, new CosmeticSound("greensound.eagle", Material.FEATHER, "§l§fEagle", "Caw of a mighty eagle", CosmeticRarity.EPIC, "greensound.eagle", "partix.sound.eagle"));
        greenSounds.put(11, new CosmeticSound("greensound.cameraflashes", Material.SPYGLASS, "§l§bCamera Flashes", "Sounds of paparazzi", CosmeticRarity.COMMON, "greensound.cameraflashes", "partix.sound.cameraflashes"));
        greenSounds.put(12, new CosmeticSound("greensound.irish_spring_green", Material.LILY_PAD, "§l§aIrish Spring Green", "Flight Team Stand up", CosmeticRarity.LEGENDARY, "greensound.irish_spring_green", "partix.sound.irish_spring_green"));
        greenSounds.put(13, new CosmeticSound("greensound.ballgame", Material.SNOWBALL, "§l§fBall Game", "Paul George", CosmeticRarity.LEGENDARY, "greensound.ballgame", "partix.sound.ballgame"));
        greenSounds.put(14, new CosmeticSound("greensound.bangbang", Material.TNT, "§l§cBang Bang", "Chief Keef", CosmeticRarity.RARE, "greensound.bangbang", "partix.sound.bangbang"));
        greenSounds.put(15, new CosmeticSound("greensound.chicken", Material.COOKED_CHICKEN, "§l§eChicken", "CHICKEN", CosmeticRarity.RARE, "greensound.chicken", "partix.sound.chicken"));
        greenSounds.put(16, new CosmeticSound("greensound.chrissmoove", Material.SMOOTH_SANDSTONE, "§l§aChris Smoove", "Im Wide open Imma let this fly", CosmeticRarity.EPIC, "greensound.chrissmoove", "partix.sound.chrissmoove"));
        greenSounds.put(17, new CosmeticSound("greensound.cokebuttercheese", Material.BREAD, "§l§6Coke Butter Cheese", "Coke Butter and Cheese", CosmeticRarity.RARE, "greensound.cokebuttercheese", "partix.sound.cokebuttercheese"));
        greenSounds.put(18, new CosmeticSound("greensound.greengiant", Material.LIME_CANDLE, "§l§2Green Giant", "Powerful and loud", CosmeticRarity.RARE, "greensound.greengiant", "partix.sound.greengiant"));
        greenSounds.put(19, new CosmeticSound("greensound.gunshot", Material.CROSSBOW, "§l§4Gunshot", "BANG", CosmeticRarity.EPIC, "greensound.gunshot", "partix.sound.gunshot"));
        greenSounds.put(20, new CosmeticSound("greensound.kobe", Material.SLIME_BALL, "§l§eKobe", "RIP Mamba", CosmeticRarity.LEGENDARY, "greensound.kobe", "partix.sound.kobe"));
        greenSounds.put(21, new CosmeticSound("greensound.splash", Material.WATER_BUCKET, "§l§bSplaaasssshhhh", "Chris Smoove", CosmeticRarity.RARE, "greensound.splash", "partix.sound.splash"));
        greenSounds.put(22, new CosmeticSound("greensound.tooeasy", Material.PAPER, "§l§fThats to easy! - Lebron", "And 1", CosmeticRarity.LEGENDARY, "greensound.tooeasy", "partix.sound.tooeasy"));
        greenSounds.put(23, new CosmeticSound("greensound.wetlikewater", Material.CAULDRON, "§l§bWet Like Water", "Dripping sound", CosmeticRarity.COMMON, "greensound.wetlikewater", "partix.sound.wetlikewater"));
        greenSounds.put(24, new CosmeticSound("greensound.wheezy", Material.MINECART, "§l§cWheezy Outta Here", "Producer Tag", CosmeticRarity.RARE, "greensound.wheezy", "partix.sound.wheezy"));
        greenSounds.put(25, new CosmeticSound("greensound.yeatbell", Material.BELL, "§l§6Yeat Bell", "Ringing the hype", CosmeticRarity.RARE, "greensound.yeatbell", "partix.sound.yeatbell"));
        greenSounds.put(26, new CosmeticSound("greensound.heheheha", Material.GOLD_ORE, "§l§6HE HE HE HA", "Clash Royale", CosmeticRarity.EPIC, "greensound.heheheha", "partix.sound.heheheha"));
        greenSounds.put(27, new CosmeticSound("greensound.getout", Material.SPRUCE_DOOR, "§l§6GET OUT", "Self Explanatory", CosmeticRarity.RARE, "greensound.getout", "partix.sound.getout"));
        greenSounds.put(28, new CosmeticSound("greensound.lebronboom", Material.NETHER_STAR, "§6§lLEBRON BOOM!", "B- B- B- BOOM", CosmeticRarity.EPIC, "greensound.lebronboom", "partix.sound.lebronboom"));
        greenSounds.put(29, new CosmeticSound("greensound.pro", Material.GOLD_BLOCK, "§6§lBANGGG (PRO)", "Mike Breen", CosmeticRarity.LEGENDARY, "greensound.pro", "partix.sound.pro"));
        greenSounds.put(30, new CosmeticSound("greensound.vip", Material.LIME_CONCRETE, "§a§lAnkles (VIP)", "Bone Snap", CosmeticRarity.LEGENDARY, "greensound.vip", "partix.sound.vip"));
        greenSounds.put(31, new CosmeticSound("greensound.filthy", Material.DIRT, "§l§6Filthy", "Wake Up Filthy!", CosmeticRarity.RARE, "greensound.filthy", "partix.sound.filthy"));
        greenSounds.put(32, new CosmeticSound("greensound.cornertthree", Material.BRICK, "§l§6Corner Three", "Peter Griffin!", CosmeticRarity.LEGENDARY, "greensound.cornerthree", "partix.sound.cornerthree"));
        greenSounds.put(33, new CosmeticSound("greensound.flightlebron", Material.FEATHER, "§l§6Flight Lebron", "LEBRONNNN!", CosmeticRarity.LEGENDARY, "greensound.flightlebron", "partix.sound.flightlebron"));
        greenSounds.put(34, new CosmeticSound("greensound.foghorn", Material.HORN_CORAL, "§l§6Fog Horn", "Loud and deep", CosmeticRarity.RARE, "greensound.foghorn", "partix.sound.foghorn"));
        greenSounds.put(35, new CosmeticSound("greensound.wasted", Material.REDSTONE, "§l§6Wasted", "GTA V wasted sound", CosmeticRarity.EPIC, "greensound.wasted", "partix.sound.wasted"));
        greenSounds.put(36, new CosmeticSound("greensound.tacobell", Material.TOTEM_OF_UNDYING, "§l§6Taco Bell", "Tacos", CosmeticRarity.RARE, "greensound.tacobell", "partix.sound.tacobell"));
        greenSounds.put(37, new CosmeticSound("greensound.majestic", Material.END_CRYSTAL, "§d§lMajestic", "She know what she wanted", CosmeticRarity.EPIC, "greensound.majestic", "partix.sound.majestic"));
        greenSounds.put(38, new CosmeticSound("greensound.buhg", Material.BEDROCK, "§d§lBaby Keem", "Buhg", CosmeticRarity.EPIC, "greensound.babykeem", "partix.sound.buhg"));
        for (Map.Entry<Integer, CosmeticSound> entry : greenSounds.entrySet()) {
            CosmeticSound sound = entry.getValue();
            if (sound.getMaterial() == null || sound.getMaterial() == Material.AIR) {
                Bukkit.getLogger().severe("[ERROR] GreenSound with invalid material: " + sound.getName());
                sound.setMaterial(Material.BARRIER);
            }
            if (sound.getSoundIdentifier() == null || sound.getSoundIdentifier().isEmpty()) {
                Bukkit.getLogger().severe("[ERROR] GreenSound without a valid sound: " + sound.getName());
            }
            Bukkit.getLogger().info("[DEBUG] GreenSound loaded: " + sound.getName() + ", Material: " + sound.getMaterial().name() + ", Rarity: " + sound.getRarity());
        }
    }

    private static void setupBallTrails() {
        ballTrails = new LinkedHashMap<>();
        ballTrails.put(0, new CosmeticBallTrail("balltrail.default", Material.BARRIER, "No Ball Trail", CosmeticRarity.COMMON, "balltrail.none"));
        ballTrails.put(1, new CosmeticBallTrail("balltrail.stars", Material.NETHER_STAR, "Star Streak", CosmeticRarity.RARE, "balltrail.stars"));
        ballTrails.put(2, new CosmeticBallTrail("balltrail.fire", Material.BLAZE_POWDER, "Fire Trail", CosmeticRarity.EPIC, "balltrail.fire"));
        ballTrails.put(3, new CosmeticBallTrail("balltrail.water", Material.WATER_BUCKET, "Water Trail", CosmeticRarity.RARE, "balltrail.water"));
        ballTrails.put(4, new CosmeticBallTrail("balltrail.sparkle", Material.GLOWSTONE_DUST, "Sparkle Trail", CosmeticRarity.COMMON, "balltrail.sparkle"));
        ballTrails.put(5, new CosmeticBallTrail("balltrail.portal", Material.ENDER_PEARL, "Portal Trail", CosmeticRarity.EPIC, "balltrail.portal"));
        ballTrails.put(6, new CosmeticBallTrail("balltrail.bubble", Material.PUFFERFISH, "Bubble Trail", CosmeticRarity.COMMON, "balltrail.bubble"));
        ballTrails.put(7, new CosmeticBallTrail("balltrail.rainbow", Material.INK_SAC, "Rainbow Trail", CosmeticRarity.LEGENDARY, "balltrail.rainbow"));
        ballTrails.put(8, new CosmeticBallTrail("balltrail.meteor", Material.FIRE_CHARGE, "Meteor Trail", CosmeticRarity.EPIC, "balltrail.meteor"));
        ballTrails.put(9, new CosmeticBallTrail("balltrail.smoke", Material.FLINT, "Smoke Trail", CosmeticRarity.RARE, "balltrail.smoke"));
        ballTrails.put(10, new CosmeticBallTrail("balltrail.vip", Material.GOLD_INGOT, "Golden Trail (VIP)", CosmeticRarity.LEGENDARY, "balltrail.vip"));
        ballTrails.put(11, new CosmeticBallTrail("balltrail.pro", Material.DIAMOND, "Enchanted Trail (PRO)", CosmeticRarity.LEGENDARY, "balltrail.pro"));
        ballTrails.put(12, new CosmeticBallTrail("balltrail.dust", Material.SAND, "Dust Trail", CosmeticRarity.COMMON, "balltrail.dust"));
        ballTrails.put(13, new CosmeticBallTrail("balltrail.electric", Material.LAPIS_LAZULI, "Electric Trail", CosmeticRarity.RARE, "balltrail.electric"));
        ballTrails.put(14, new CosmeticBallTrail("balltrail.ice", Material.BLUE_ICE, "Ice Trail", CosmeticRarity.EPIC, "balltrail.ice"));
        ballTrails.put(15, new CosmeticBallTrail("balltrail.red", Material.REDSTONE, "Red Trail", CosmeticRarity.COMMON, "balltrail.red"));
        ballTrails.put(16, new CosmeticBallTrail("balltrail.orange", Material.ORANGE_DYE, "Orange Trail", CosmeticRarity.COMMON, "balltrail.orange"));
        ballTrails.put(17, new CosmeticBallTrail("balltrail.yellow", Material.YELLOW_DYE, "Yellow Trail", CosmeticRarity.COMMON, "balltrail.yellow"));
        ballTrails.put(18, new CosmeticBallTrail("balltrail.green", Material.GREEN_DYE, "Green Trail", CosmeticRarity.COMMON, "balltrail.green"));
        ballTrails.put(19, new CosmeticBallTrail("balltrail.blue", Material.BLUE_DYE, "Blue Trail", CosmeticRarity.COMMON, "balltrail.blue"));
        ballTrails.put(20, new CosmeticBallTrail("balltrail.america", Material.FEATHER, "America Trail", CosmeticRarity.RARE, "balltrail.america"));
        ballTrails.put(21, new CosmeticBallTrail("balltrail.canada", Material.BREAD, "Canada Trail", CosmeticRarity.RARE, "balltrail.canada"));
        ballTrails.put(22, new CosmeticBallTrail("balltrail.xp", Material.EXPERIENCE_BOTTLE, "Experience Orb Trail", CosmeticRarity.RARE, "balltrail.xp"));
        ballTrails.put(23, new CosmeticBallTrail("balltrail.vice", Material.MAGENTA_DYE, "Vice Trail", CosmeticRarity.EPIC, "balltrail.vice"));
        ballTrails.put(24, new CosmeticBallTrail("balltrail.black", Material.INK_SAC, "Black Trail", CosmeticRarity.RARE, "balltrail.black"));
        ballTrails.put(25, new CosmeticBallTrail("balltrail.mystic", Material.END_CRYSTAL, "Mystic Trail", CosmeticRarity.LEGENDARY, "balltrail.mystic"));
        ballTrails.put(26, new CosmeticBallTrail("balltrail.phantom", Material.OBSIDIAN, "Phantom Trail", CosmeticRarity.LEGENDARY, "balltrail.phantom"));
        ballTrails.put(27, new CosmeticBallTrail("balltrail.blueorange", Material.LAPIS_BLOCK, "Blue & Orange Ball Trail", CosmeticRarity.RARE, "balltrail.blueorange"));
        ballTrails.put(28, new CosmeticBallTrail("balltrail.lime", Material.LIME_DYE, "Lime Trail", CosmeticRarity.RARE, "balltrail.lime"));
        ballTrails.put(29, new CosmeticBallTrail("balltrail.forest", Material.GREEN_WOOL, "Forest Trail", CosmeticRarity.EPIC, "balltrail.forest"));
        ballTrails.put(30, new CosmeticBallTrail("balltrail.neon", Material.EMERALD, "Neon Green Trail", CosmeticRarity.LEGENDARY, "balltrail.neon"));
        ballTrails.put(31, new CosmeticBallTrail("balltrail.note", Material.NOTE_BLOCK, "Note Trail", CosmeticRarity.COMMON, "balltrail.note"));
        ballTrails.put(32, new CosmeticBallTrail("balltrail.witch", Material.BLAZE_ROD, "Witch Trail", CosmeticRarity.RARE, "balltrail.witch"));
        ballTrails.put(33, new CosmeticBallTrail("balltrail.dragon", Material.DRAGON_HEAD, "Dragon Breath Trail", CosmeticRarity.LEGENDARY, "balltrail.dragon"));
        ballTrails.put(34, new CosmeticBallTrail("balltrail.happy", Material.YELLOW_CONCRETE, "Happy Trail", CosmeticRarity.COMMON, "balltrail.happy"));
    }

    public static void setup() {
        Cosmetics.setupTrails();
        Cosmetics.setupGreenSounds();
        Cosmetics.setupExplosions();
        Cosmetics.setupBallTrails();
        Cosmetics.setupWinSongs();
        Bukkit.getLogger().info("[DEBUG] Trails loaded: " + trails.size());
        Bukkit.getLogger().info("[DEBUG] Explosions loaded: " + explosions.size());
        Bukkit.getLogger().info("[DEBUG] Green Sounds loaded: " + greenSounds.size());
        Cosmetics.validateCosmetics(greenSounds, "Green Sounds");
        Cosmetics.validateCosmetics(trails, "Trails");
        Cosmetics.validateCosmetics(explosions, "Explosions");
    }

    public static CosmeticParticle randomTrail() {
        ArrayList<CosmeticParticle> list = new ArrayList<>();
        for (CosmeticParticle c : new ArrayList<>(trails.values())) {
            int i;
            if (c.getRarity().equals(CosmeticRarity.COMMON)) {
                for (i = 0; i < 4; ++i) {
                    list.add(c);
                }
                continue;
            }
            if (c.getRarity().equals(CosmeticRarity.RARE)) {
                for (i = 0; i < 3; ++i) {
                    list.add(c);
                }
                continue;
            }
            if (c.getRarity().equals(CosmeticRarity.EPIC)) {
                for (i = 0; i < 2; ++i) {
                    list.add(c);
                }
                continue;
            }
            if (!c.getRarity().equals(CosmeticRarity.LEGENDARY)) continue;
            for (i = 0; i < 1; ++i) {
                list.add(c);
            }
        }
        if (list.isEmpty()) {
            Bukkit.getLogger().severe("[ERROR] No trails available for selection!");
            return null;
        }
        return list.get(new Random().nextInt(list.size()));
    }

    public static CosmeticParticle randomExplosion() {
        ArrayList<CosmeticParticle> list = new ArrayList<>();
        for (CosmeticParticle c : new ArrayList<>(explosions.values())) {
            int i;
            if (c.getRarity().equals(CosmeticRarity.COMMON)) {
                for (i = 0; i < 4; ++i) {
                    list.add(c);
                }
                continue;
            }
            if (c.getRarity().equals(CosmeticRarity.RARE)) {
                for (i = 0; i < 3; ++i) {
                    list.add(c);
                }
                continue;
            }
            if (c.getRarity().equals(CosmeticRarity.EPIC)) {
                for (i = 0; i < 2; ++i) {
                    list.add(c);
                }
                continue;
            }
            if (!c.getRarity().equals(CosmeticRarity.LEGENDARY)) continue;
            for (i = 0; i < 1; ++i) {
                list.add(c);
            }
        }
        if (list.isEmpty()) {
            Bukkit.getLogger().severe("[ERROR] No explosions available for selection!");
            return null;
        }
        return list.get(new Random().nextInt(list.size()));
    }

//    private static CosmeticSound randomGreenSound(Player player) {
//        ArrayList<CosmeticSound> list = new ArrayList<>();
//        for (CosmeticSound sound : greenSounds.values()) {
//            if (Cosmetics.playerHasCosmetic(player, sound)) continue;
//            switch (sound.getRarity()) {
//                case COMMON: {
//                    int i;
//                    for (i = 0; i < 4; ++i) {
//                        list.add(sound);
//                    }
//                    continue;
//                }
//                case RARE: {
//                    int i;
//                    for (i = 0; i < 3; ++i) {
//                        list.add(sound);
//                    }
//                    continue;
//                }
//                case EPIC: {
//                    int i;
//                    for (i = 0; i < 2; ++i) {
//                        list.add(sound);
//                    }
//                    continue;
//                }
//                case LEGENDARY: {
//                    int i;
//                    for (i = 0; i < 1; ++i) {
//                        list.add(sound);
//                    }
//                    break;
//                }
//            }
//        }
//        if (list.isEmpty()) {
//            Bukkit.getLogger().severe("[ERROR] No green sounds available for selection!");
//            return null;
//        }
//        return list.get(new Random().nextInt(list.size()));
//    }

    private static CompletableFuture<Boolean> playerHasCosmetic(Player player, CosmeticHolder cosmetic) {
        if (cosmetic == null || cosmetic.getKey() == null) {
            return CompletableFuture.completedFuture(false);
        }
        String key = cosmetic.getKey();
        if (!key.matches("\\d+")) {
            Bukkit.getLogger().warning("[WARN] Non-numeric key detected for cosmetic: " + key);
            return CompletableFuture.completedFuture(false);
        }
        try {
            final CompletableFuture<Boolean> future = new CompletableFuture<>();
            Bukkit.getScheduler().runTaskAsynchronously(Partix.getInstance(), () -> {
                int cosmeticKey = Integer.parseInt(key);
                int playerCosmeticKey;
                switch (cosmetic) {
                    case CosmeticParticle cosmeticParticle when trails.containsValue(cosmetic) ->
                            playerCosmeticKey = PlayerDb.get(player.getUniqueId(), PlayerDb.Stat.TRAIL).join();
                    case CosmeticSound cosmeticSound when greenSounds.containsValue(cosmetic) ->
                            playerCosmeticKey = PlayerDb.get(player.getUniqueId(), PlayerDb.Stat.GREEN_SOUND).join();
                    case CosmeticParticle cosmeticParticle when explosions.containsValue(cosmetic) ->
                            playerCosmeticKey = PlayerDb.get(player.getUniqueId(), PlayerDb.Stat.EXPLOSION).join();
                    default -> {
                        Bukkit.getLogger().severe("[ERROR] Unknown cosmetic type for: " + cosmetic.getName());
                        future.complete(false);
                        return;
                    }
                }

                boolean hasCosmetic = playerCosmeticKey == cosmeticKey;
                Bukkit.getLogger().info("[DEBUG] Checking ownership for " + player.getName() + ": " + key + " -> " + hasCosmetic);
                future.complete(hasCosmetic);
            });
            return future;
        } catch (Exception e) {
            Bukkit.getLogger().severe("[ERROR] Unexpected issue with cosmetic key: " + key);
            return CompletableFuture.completedFuture(false);
        }
    }

    private static <T extends CosmeticHolder> List<T> buildCosmeticsList(Map<Integer, T> source, int common, int rare, int epic, int legendary) {
        List<CosmeticHolder> list = new ArrayList<>();
        for (CosmeticHolder cosmetic : source.values()) {
            if (cosmetic.getPermission().equalsIgnoreCase("rank.pro") || cosmetic.getPermission().equalsIgnoreCase("default"))
                continue;
            int count = switch (cosmetic.getRarity()) {
                case CosmeticRarity.COMMON -> common;
                case CosmeticRarity.RARE -> rare;
                case CosmeticRarity.EPIC -> epic;
                case CosmeticRarity.LEGENDARY -> legendary;
            };
            for (int i = 0; i < count; ++i) {
                list.add(cosmetic);
            }
        }
        return (List<T>) list;
    }

    private static ItemStack createPreviewButton() {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("§ePreview Rewards"));
        meta.lore(List.of(Component.text("§7Preview the possible rewards"), Component.text("§7for this crate.")));
        item.setItemMeta(meta);
        return item;
    }

    private static void openPreviewGUIByRarity(Player player, CosmeticRarity rarity, int page) {
        List<CosmeticHolder> cosmetics = Cosmetics.getCosmeticsByRarity(rarity).stream().filter(cosmetic -> !CrateHandler.isExcludedCosmetic(cosmetic)).collect(Collectors.toList());
        if (cosmetics.isEmpty()) {
            player.sendMessage("§cNo cosmetics available for " + rarity.name().toLowerCase() + "!");
            return;
        }
        Cosmetics.openPaginatedGUI(player, cosmetics, page, "Preview " + rarity.name() + " Cosmetics", (p, cosmetic) -> p.sendMessage("§aPreviewed: §6" + cosmetic.getName()));
    }

    public static List<CosmeticHolder> getCosmeticsByRarity(CosmeticRarity rarity) {
        ArrayList<CosmeticHolder> available = new ArrayList<>();
        for (CosmeticHolder cosmeticHolder : trails.values()) {
            if (cosmeticHolder.getRarity() != rarity || CrateHandler.isExcludedCosmetic(cosmeticHolder)) continue;
            available.add(cosmeticHolder);
        }
        for (CosmeticHolder cosmeticHolder : explosions.values()) {
            if (cosmeticHolder.getRarity() != rarity || CrateHandler.isExcludedCosmetic(cosmeticHolder)) continue;
            available.add(cosmeticHolder);
        }
        for (CosmeticHolder cosmeticHolder : greenSounds.values()) {
            if (cosmeticHolder.getRarity() != rarity || CrateHandler.isExcludedCosmetic(cosmeticHolder)) continue;
            available.add(cosmeticHolder);
        }
        for (CosmeticHolder cosmeticHolder : ballTrails.values()) {
            if (cosmeticHolder.getRarity() != rarity || CrateHandler.isExcludedCosmetic(cosmeticHolder)) continue;
            available.add(cosmeticHolder);
        }
        return available;
    }

    private static void validateCosmetics(Map<Integer, ? extends CosmeticHolder> cosmetics, String type) {
        for (CosmeticHolder cosmeticHolder : cosmetics.values()) {
            if (cosmeticHolder.getMaterial() == null || cosmeticHolder.getMaterial() == Material.AIR) {
                Bukkit.getLogger().severe("[ERROR] Invalid material for " + type + ": " + cosmeticHolder.getName());
                cosmeticHolder.setMaterial(Material.BARRIER);
            }
            if (cosmeticHolder.getKey() != null && !cosmeticHolder.getKey().isEmpty()) continue;
            Bukkit.getLogger().severe("[ERROR] Invalid key for " + type + ": " + cosmeticHolder.getName());
        }
    }

    private static void validateGreenSounds() {
        for (Map.Entry<Integer, CosmeticSound> entry : greenSounds.entrySet()) {
            CosmeticSound sound = entry.getValue();
            if (sound.getGui() == null || sound.getGui() == Material.AIR) {
                Bukkit.getLogger().severe("[ERROR] GreenSound with invalid material: " + sound.getName());
                sound.setMaterial(Material.BARRIER);
            }
            if (sound.getSoundIdentifier() == null || sound.getSoundIdentifier().isEmpty()) {
                Bukkit.getLogger().severe("[ERROR] GreenSound without a valid sound: " + sound.getName());
            }
            Bukkit.getLogger().info("[DEBUG] GreenSound loaded: " + sound.getName() + ", Material: " + sound.getGui().name() + ", Rarity: " + sound.getRarity());
        }
    }

    private static void openPreviewGUIByType(Player player, String cosmeticType, int page) {
        List<CosmeticHolder> cosmetics = switch (cosmeticType.toLowerCase()) {
            case "trail" -> new ArrayList<>(trails.values());
            case "explosion" -> new ArrayList<>(explosions.values());
            case "greensound" -> new ArrayList<>(greenSounds.values());
            case "balltrail" -> new ArrayList<>(ballTrails.values());
            default -> new ArrayList<>();
        };
        cosmetics = cosmetics.stream().filter(cosmetic -> !CrateHandler.isExcludedCosmetic(cosmetic)).collect(Collectors.toList());
        if (cosmetics.isEmpty()) {
            player.sendMessage("§cNo cosmetics available for this type: " + cosmeticType + "!");
            return;
        }
        Cosmetics.openPaginatedGUI(player, cosmetics, page, "Preview " + cosmeticType + " Cosmetics", (p, cosmetic) -> p.sendMessage("§aPreviewed: §6" + cosmetic.getName()));
    }

    private static void openPaginatedGUI(Player player, List<CosmeticHolder> cosmetics, int page, String title, BiConsumer<Player, CosmeticHolder> onClick) {
        int currentPage;
        int totalItems = cosmetics.size();
        int itemsPerPage = 45;
        int maxPages = (int) Math.ceil((double) totalItems / (double) itemsPerPage);
        if (page < 1) {
            page = 1;
        }
        if (page > maxPages) {
            page = maxPages;
        }
        ItemButton[] buttons = new ItemButton[54];
        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, totalItems);
        int i = startIndex;
        int slot = 0;
        while (i < endIndex) {
            CosmeticHolder cosmetic = cosmetics.get(i);
            buttons[slot] = new ItemButton(slot, cosmetic.getGUIItem(), p -> onClick.accept(p, cosmetic));
            ++i;
            ++slot;
        }
        if (page > 1) {
            currentPage = page;
            int finalCurrentPage = currentPage;
            buttons[45] = new ItemButton(45, Cosmetics.createNavigationButton("Previous Page", Material.ARROW), p -> Cosmetics.openPaginatedGUI(p, cosmetics, finalCurrentPage - 1, title, onClick));
        }
        if (page < maxPages) {
            currentPage = page;
            int finalCurrentPage1 = currentPage;
            buttons[53] = new ItemButton(53, Cosmetics.createNavigationButton("Next Page", Material.ARROW), p -> Cosmetics.openPaginatedGUI(p, cosmetics, finalCurrentPage1 + 1, title, onClick));
        }
        new GUI(title + " (Page " + page + ")", 6, false, buttons).openInventory(player);
    }

    public static ItemStack createNavigationButton(String name, Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("§e" + name));
        item.setItemMeta(meta);
        return item;
    }

    private static SpinPrize pickWeightedPrize(List<SpinPrize> prizes, int totalWeight) {
        int rand = new Random().nextInt(totalWeight);
        int sum = 0;
        for (SpinPrize p : prizes) {
            if (rand >= (sum += p.weight)) continue;
            return p;
        }
        return prizes.getFirst();
    }

//    private CosmeticBallTrail randomBallTrail(Player player) {
//        ArrayList<CosmeticBallTrail> weightedList = new ArrayList<>();
//        for (CosmeticBallTrail ballTrail : ballTrails.values()) {
//            if (Cosmetics.playerHasCosmetic(player, ballTrail) || CrateHandler.isExcludedCosmetic(ballTrail)) continue;
//            int weight = switch (ballTrail.getRarity()) {
//                case CosmeticRarity.COMMON -> 4;
//                case CosmeticRarity.RARE -> 3;
//                case CosmeticRarity.EPIC -> 2;
//                case CosmeticRarity.LEGENDARY -> 1;
//            };
//            for (int i = 0; i < weight; ++i) {
//                weightedList.add(ballTrail);
//            }
//        }
//        if (weightedList.isEmpty()) {
//            return null;
//        }
//        return weightedList.get(new Random().nextInt(weightedList.size()));
//    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Athlete athlete = AthleteManager.get(player.getUniqueId());
        PlayerDb.get(player.getUniqueId(), PlayerDb.Stat.DEFAULT_COSMETICS).thenAccept(defaultCosmetics -> {
            if (defaultCosmetics == 0) {
                PlayerDb.set(player.getUniqueId(), PlayerDb.Stat.TRAIL, 0);
                PlayerDb.set(player.getUniqueId(), PlayerDb.Stat.EXPLOSION, 0);
                PlayerDb.set(player.getUniqueId(), PlayerDb.Stat.GREEN_SOUND, 0);
                PlayerDb.set(player.getUniqueId(), PlayerDb.Stat.DEFAULT_COSMETICS, 1);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aDefault cosmetics have been added to your account!"));
            }
        });
        PlayerDb.get(player.getUniqueId(), PlayerDb.Stat.GREEN_SOUND).thenAccept(greenSoundIndex -> {
            CosmeticSound equippedSound = greenSounds.getOrDefault(greenSoundIndex, greenSounds.get(0));
            athlete.setGreenSound(equippedSound);
            if (equippedSound != null) {
                player.sendMessage("§aEquipped Green Sound: " + equippedSound.getName());
            }
        });
        if (player.hasPermission("rank.vip")) {
            this.grantCosmetics(player, trails.values(), "(vip)");
            this.grantCosmetics(player, explosions.values(), "(vip)");
            this.grantCosmetics(player, greenSounds.values(), "(vip)");
        }
        if (player.hasPermission("rank.pro")) {
            this.grantCosmetics(player, trails.values(), "(pro)");
            this.grantCosmetics(player, explosions.values(), "(pro)");
            this.grantCosmetics(player, greenSounds.values(), "(pro)");
        }
    }

    private void grantCosmetics(Player player, Collection<? extends CosmeticHolder> cosmetics, String marker) {
        for (CosmeticHolder cosmeticHolder : cosmetics) {
            String permission;
            if (!cosmeticHolder.getName().toLowerCase().contains(marker.toLowerCase()) || (permission = cosmeticHolder.getPermission()) == null || permission.isEmpty())
                continue;
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + player.getName() + " permission set " + permission);
            Bukkit.getLogger().info("[DEBUG] Granted " + permission + " to " + player.getName());
        }
    }

    private boolean isValidCosmetic(CosmeticHolder cosmetic) {
        return cosmetic.getKey() != null && !cosmetic.getKey().isEmpty() && cosmetic.getMaterial() != null;
    }

    private ItemButton createButtonForCosmetic(Player player, CosmeticHolder cosmetic) {
        ItemStack item = new ItemStack(cosmetic.getMaterial());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(cosmetic.getName()));
        item.setItemMeta(meta);
        return new ItemButton(0, item, p -> player.sendMessage("Selected: " + cosmetic.getName()));
    }

    private ItemStack createRewardItem(CosmeticHolder reward) {
        Material material = reward.getMaterial();
        if (material == null || material == Material.AIR) {
            material = Material.BARRIER;
        }
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(this.getRarityColor(reward.getRarity()) + reward.getName()));
        meta.lore(List.of(Component.text("§7Rarity: " + this.getRarityColor(reward.getRarity()) + reward.getRarity().name())));
        item.setItemMeta(meta);
        return item;
    }

    private int calculateRows(int rewardCount) {
        return Math.min(6, (int) Math.ceil((double) rewardCount / 9.0));
    }

    private String getRarityColor(CosmeticRarity rarity) {
        return switch (rarity) {
            case CosmeticRarity.COMMON -> "§7";
            case CosmeticRarity.RARE -> "§9";
            case CosmeticRarity.EPIC -> "§5";
            case CosmeticRarity.LEGENDARY -> "§6";
        };
    }

    private void openSpinWheelGUI(final Player player) {
        final GUI[] spinGUI = new GUI[]{new GUI("Spin the Wheel", 3, false)};
        for (int slot = 0; slot < 27; ++slot) {
            if (slot == 13) continue;
            int row = slot / 9;
            int col = slot % 9;
            Material bgMaterial = (row + col) % 2 == 0 ? Material.RED_STAINED_GLASS_PANE : Material.BLACK_STAINED_GLASS_PANE;
            spinGUI[0].addButton(new ItemButton(slot, Items.get(Component.empty(), bgMaterial), p -> {
            }));
        }
        spinGUI[0].addButton(new ItemButton(13, Items.get(Component.text("Spinning..."), Material.NETHER_STAR), p -> {
        }));
        spinGUI[0].openInventory(player);
        final List<SpinPrize> prizes = Arrays.asList(new SpinPrize("No Coins", 0, Material.DIRT, 5), new SpinPrize("250 Coins", 250, Material.IRON_INGOT, 60), new SpinPrize("350 Coins", 350, Material.GOLD_INGOT, 25), new SpinPrize("600 Coins", 600, Material.EMERALD, 5), new SpinPrize("1000 Coins", 1000, Material.DIAMOND, 5));
        int totalWeight = 100;
        new BukkitRunnable() {
            int iterations = 20;

            public void run() {
                if (this.iterations <= 0) {
                    this.cancel();
                    return;
                }
                SpinPrize current = Cosmetics.pickWeightedPrize(prizes, 100);
                spinGUI[0].addButton(new ItemButton(13, current.toItem(), p -> {
                }));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.MASTER, 1.0f, 1.0f);
                --this.iterations;
                if (this.iterations == 0) {
                    if (current.coins > 0) {
                        PlayerDb.get(player.getUniqueId(), PlayerDb.Stat.COINS).thenAccept(existingCoins -> {
                            PlayerDb.set(player.getUniqueId(), PlayerDb.Stat.COINS, existingCoins + current.coins);
                            player.sendMessage("§aYou won: §6" + current.coins + " Coins!");
                        });
                    } else {
                        player.sendMessage("§cNo Coins won!");
                    }
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1.5f, 1.0f);
                    new BukkitRunnable() {

                        public void run() {
                            player.closeInventory();
                        }
                    }.runTaskLater(Partix.getInstance(), 60L);
                    this.cancel();
                }
            }
        }.runTaskTimer(Partix.getInstance(), 0L, 5L);
    }

    public static class CrateHandler
            implements Listener {
        private static final HashMap<Material, Crate> crates = new HashMap<>();
        private static final HashMap<Location, ArmorStand> holograms = new HashMap<>();
        private static final HashMap<Location, Boolean> crateInUse = new HashMap<>();

        static {
            crates.put(Material.LIGHT_GRAY_SHULKER_BOX, new Crate("Common Crate", 300, CosmeticRarity.COMMON, Material.LIGHT_GRAY_SHULKER_BOX));
            crates.put(Material.LIGHT_BLUE_SHULKER_BOX, new Crate("Rare Crate", 800, CosmeticRarity.RARE, Material.LIGHT_BLUE_SHULKER_BOX));
            crates.put(Material.PURPLE_SHULKER_BOX, new Crate("Epic Crate", 1900, CosmeticRarity.EPIC, Material.PURPLE_SHULKER_BOX));
            crates.put(Material.YELLOW_SHULKER_BOX, new Crate("Legendary Crate", 2600, CosmeticRarity.LEGENDARY, Material.YELLOW_SHULKER_BOX));
            crates.put(Material.ORANGE_SHULKER_BOX, new Crate("Trail Crate", 800, null, Material.ORANGE_SHULKER_BOX));
            crates.put(Material.RED_SHULKER_BOX, new Crate("Explosion Crate", 800, null, Material.RED_SHULKER_BOX));
            crates.put(Material.WHITE_SHULKER_BOX, new Crate("Green Sounds Crate", 850, null, Material.WHITE_SHULKER_BOX));
            crates.put(Material.BLUE_SHULKER_BOX, new Crate("Ball Trail Crate", 850, null, Material.BLUE_SHULKER_BOX));
            crates.put(Material.GREEN_SHULKER_BOX, new Crate("MBA Bucks Crate", 1000, null, Material.GREEN_SHULKER_BOX));
        }

        public static boolean isExcludedCosmetic(CosmeticHolder cosmetic) {
            boolean excluded;
            if (cosmetic == null || cosmetic.getName() == null) {
                return false;
            }
            String name = cosmetic.getName().toLowerCase();
            boolean bl = excluded = name.contains("no explosion") || name.contains("no trail") || name.contains("no sound") || name.contains("Baby Keem") || name.contains("(vip)") || name.contains("no ball trail") || name.contains("(pro)");
            if (excluded) {
                Bukkit.getLogger().info("[DEBUG] Excluded from crate rewards and preview: " + cosmetic.getName());
            }
            return excluded;
        }

        private static Prize getWeightedRandomPrize(List<Prize> prizes) {
            int totalWeight = prizes.stream().mapToInt(Prize::getChance).sum();
            int randomWeight = new Random().nextInt(totalWeight);
            int currentWeight = 0;
            for (Prize prize : prizes) {
                if (randomWeight >= (currentWeight += prize.getChance())) continue;
                return prize;
            }
            return prizes.getFirst();
        }

        private String getCrateType(Material block) {
            return switch (block) {
                case Material.ORANGE_SHULKER_BOX -> "trail";
                case Material.RED_SHULKER_BOX -> "explosion";
                case Material.WHITE_SHULKER_BOX -> "greensound";
                case Material.GREEN_SHULKER_BOX -> "mbabucks";
                case Material.BLUE_SHULKER_BOX -> "balltrail";
                default -> null;
            };
        }

        @EventHandler
        public void onCrateInteract(PlayerInteractEvent event) {
            if (!event.hasBlock() || event.getClickedBlock() == null) {
                return;
            }
            Action action = event.getAction();
            if (action != Action.LEFT_CLICK_BLOCK && action != Action.RIGHT_CLICK_BLOCK) {
                return;
            }
            Player player = event.getPlayer();
            Location blockLocation = event.getClickedBlock().getLocation();
            Material blockMaterial = event.getClickedBlock().getType();
            Crate crate = crates.get(blockMaterial);
            if (crate == null) {
                return;
            }
            event.setCancelled(true);
            String cosmeticType = this.getCrateType(blockMaterial);
            this.openConfirmationGUI(player, crate, blockLocation, cosmeticType);
        }

        private void openConfirmationGUI(Player player, Crate crate, Location blockLocation, String cosmeticType) {
            String finalCosmeticType = cosmeticType == null ? (crate.rarity != null ? crate.rarity.name().toLowerCase() : "invalid") : cosmeticType;
            if ("invalid".equalsIgnoreCase(finalCosmeticType)) {
                player.sendMessage("§cThis crate does not have a valid cosmetic type!");
                return;
            }
            ItemButton previewButton = cosmeticType == null ? new ItemButton(15, Cosmetics.createPreviewButton(), p -> Cosmetics.openPreviewGUIByRarity(p, crate.rarity, 1)) : new ItemButton(15, Cosmetics.createPreviewButton(), p -> Cosmetics.openPreviewGUIByType(p, finalCosmeticType, 1));
            GUI gui = new GUI(crate.name + " Crate", 3, false, new ItemButton(13, this.createPurchaseButton(crate), p -> this.processPurchase(p, crate, blockLocation, finalCosmeticType)), previewButton);
            gui.openInventory(player);
        }

        private ItemStack createPurchaseButton(Crate crate) {
            ItemStack item = new ItemStack(Material.GREEN_WOOL);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text("§aPurchase Crate"));
            String priceLabel = crate.material == Material.GREEN_SHULKER_BOX ? "MBA Bucks" : "Coins";
            meta.lore(List.of(Component.text("§7Rarity: " + crate.name), Component.text("§ePrice: §6" + crate.price + " " + priceLabel)));
            item.setItemMeta(meta);
            return item;
        }

        private void processPurchase(Player player, Crate crate, final Location blockLocation, String cosmeticType) {
            if ("mbabucks".equalsIgnoreCase(cosmeticType)) {
                PlayerDb.get(player.getUniqueId(), PlayerDb.Stat.MBA_BUCKS).thenAccept(playerMBA -> {
                    if (playerMBA < 1000) {
                        player.sendMessage("§cYou don't have enough MBA Bucks to purchase this crate!");
                        return;
                    }
                    PlayerDb.set(player.getUniqueId(), PlayerDb.Stat.MBA_BUCKS, playerMBA - 1000);
                    player.closeInventory();
                    crateInUse.put(blockLocation, true);
                    this.openSpinWheelGUI(player);
                    new BukkitRunnable() {

                        public void run() {
                            crateInUse.put(blockLocation, false);
                        }
                    }.runTaskLater(Partix.getInstance(), 100L);
                });
                return;
            }
            PlayerDb.get(player.getUniqueId(), PlayerDb.Stat.COINS).thenAccept(playerCoins -> {
                CosmeticHolder reward;
                if (playerCoins < crate.price) {
                    player.sendMessage("§cYou don't have enough coins to purchase this crate!");
                    return;
                }
                PlayerDb.set(player.getUniqueId(), PlayerDb.Stat.COINS, playerCoins - crate.price);
                player.closeInventory();
                crateInUse.put(blockLocation, true);
                switch (cosmeticType) {
                    case "trail": {
                        reward = this.randomTrail(player).join();
                        break;
                    }
                    case "explosion": {
                        reward = this.randomExplosion(player).join();
                        break;
                    }
                    case "greensound": {
                        reward = this.randomGreenSound(player).join();
                        break;
                    }
                    case "balltrail": {
                        reward = this.randomBallTrail(player).join();
                        break;
                    }
                    default: {
                        reward = this.getRandomReward(player, crate.rarity).join();
                    }
                }
                if (reward == null) {
                    player.sendMessage("§cYou already own all cosmetics of this rarity!");
                    crateInUse.put(blockLocation, false);
                    return;
                }
                if (CrateHandler.isExcludedCosmetic(reward)) {
                    player.sendMessage("§cYou cannot win " + reward.getName() + " from this crate!");
                    crateInUse.put(blockLocation, false);
                    return;
                }
                if (reward instanceof CosmeticSound greenSound) {
                    Athlete athlete = AthleteManager.get(player.getUniqueId());
                    athlete.setGreenSound(greenSound);
                    try {
                        int soundKey = Integer.parseInt(greenSound.getKey());
                        PlayerDb.set(player.getUniqueId(), PlayerDb.Stat.GREEN_SOUND, soundKey);
                    } catch (NumberFormatException e) {
                        Bukkit.getLogger().severe("[ERROR] Invalid key format for Green Sound: " + greenSound.getKey());
                    }
                }
                this.startCrateAnimation(player, crate, blockLocation, reward, reward.getRarity());
                new BukkitRunnable() {

                    public void run() {
                        crateInUse.put(blockLocation, false);
                    }
                }.runTaskLater(Partix.getInstance(), 100L);
            });
        }

        private void openSpinWheelGUI(final Player player) {
            final GUI[] spinGUI = new GUI[]{new GUI("Spin the Wheel", 3, false)};
            for (int slot = 0; slot < 27; ++slot) {
                if (slot == 13) continue;
                int row = slot / 9;
                int col = slot % 9;
                Material bgMaterial = (row + col) % 2 == 0 ? Material.RED_STAINED_GLASS_PANE : Material.BLACK_STAINED_GLASS_PANE;
                spinGUI[0].addButton(new ItemButton(slot, Items.get(Component.empty(), bgMaterial), p -> {
                }));
            }
            spinGUI[0].addButton(new ItemButton(13, Items.get(Component.text("Spinning..."), Material.NETHER_STAR), p -> {
            }));
            spinGUI[0].openInventory(player);
            final List<SpinPrize> prizes = Arrays.asList(new SpinPrize("No Coins", 0, Material.DIRT, 5), new SpinPrize("250 Coins", 250, Material.IRON_INGOT, 60), new SpinPrize("350 Coins", 350, Material.GOLD_INGOT, 25), new SpinPrize("600 Coins", 600, Material.EMERALD, 5), new SpinPrize("1000 Coins", 1000, Material.DIAMOND, 5));
            int totalWeight = 100;
            new BukkitRunnable() {
                int iterations = 20;

                public void run() {
                    if (this.iterations <= 0) {
                        this.cancel();
                        return;
                    }
                    SpinPrize current = Cosmetics.pickWeightedPrize(prizes, 100);
                    spinGUI[0].addButton(new ItemButton(13, current.toItem(), p -> {
                    }));
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.MASTER, 1.0f, 1.0f);
                    --this.iterations;
                    if (this.iterations == 0) {
                        if (current.coins > 0) {
                            PlayerDb.get(player.getUniqueId(), PlayerDb.Stat.COINS).thenAccept(existingCoins -> {
                                PlayerDb.set(player.getUniqueId(), PlayerDb.Stat.COINS, existingCoins + current.coins);
                                player.sendMessage("§aYou won: §6" + current.coins + " Coins!");
                            });
                        } else {
                            player.sendMessage("§cNo Coins won!");
                        }
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1.5f, 1.0f);
                        new BukkitRunnable() {

                            public void run() {
                                player.closeInventory();
                            }
                        }.runTaskLater(Partix.getInstance(), 60L);
                        this.cancel();
                    }
                }
            }.runTaskTimer(Partix.getInstance(), 0L, 5L);
        }

        private CosmeticRarity getRandomRarityForType(String cosmeticType) {
            Random random = new Random();
            int chance = random.nextInt(100);
            if (chance < 40) {
                return CosmeticRarity.COMMON;
            }
            if (chance < 70) {
                return CosmeticRarity.RARE;
            }
            if (chance < 90) {
                return CosmeticRarity.EPIC;
            }
            return CosmeticRarity.LEGENDARY;
        }

        private CompletableFuture<CosmeticParticle> randomTrail(Player player) {
            final CompletableFuture<CosmeticParticle> future = new CompletableFuture<>();
            Bukkit.getScheduler().runTaskAsynchronously(Partix.getInstance(), () -> {
                List<CosmeticParticle> eligibleTrails = trails.values().stream().filter(trail -> !Cosmetics.playerHasCosmetic(player, trail).join()).filter(trail -> !CrateHandler.isExcludedCosmetic(trail)).toList();
                if (eligibleTrails.isEmpty()) {
                    future.complete(null);
                    return;
                }
                future.complete(eligibleTrails.get(new Random().nextInt(eligibleTrails.size())));
            });
            return future;
        }

        private CompletableFuture<CosmeticParticle> randomExplosion(Player player) {
            final CompletableFuture<CosmeticParticle> future = new CompletableFuture<>();
            Bukkit.getScheduler().runTaskAsynchronously(Partix.getInstance(), () -> {
                List<CosmeticParticle> eligibleExplosions = explosions.values().stream().filter(explosion -> !Cosmetics.playerHasCosmetic(player, explosion).join()).filter(explosion -> !CrateHandler.isExcludedCosmetic(explosion)).toList();
                if (eligibleExplosions.isEmpty()) {
                    future.complete(null);
                    return;
                }
                future.complete(eligibleExplosions.get(new Random().nextInt(eligibleExplosions.size())));
            });
            return future;
        }

        private CompletableFuture<CosmeticSound> randomGreenSound(Player player) {
            final CompletableFuture<CosmeticSound> future = new CompletableFuture<>();
            Bukkit.getScheduler().runTaskAsynchronously(Partix.getInstance(), () -> {
                List<CosmeticSound> eligibleSounds = greenSounds.values().stream().filter(sound -> !Cosmetics.playerHasCosmetic(player, sound).join()).filter(sound -> !CrateHandler.isExcludedCosmetic(sound)).toList();
                if (eligibleSounds.isEmpty()) {
                    future.complete(null);
                    return;
                }
                future.complete(eligibleSounds.get(new Random().nextInt(eligibleSounds.size())));
            });
            return future;
        }

        private CompletableFuture<CosmeticBallTrail> randomBallTrail(Player player) {
            final CompletableFuture<CosmeticBallTrail> future = new CompletableFuture<>();
            Bukkit.getScheduler().runTaskAsynchronously(Partix.getInstance(), () -> {
                List<CosmeticBallTrail> eligibleBallTrails = ballTrails.values().stream().filter(ballTrail -> !Cosmetics.playerHasCosmetic(player, ballTrail).join()).filter(cosmetic -> !CrateHandler.isExcludedCosmetic(cosmetic)).toList();
                if (eligibleBallTrails.isEmpty()) {
                    future.complete(null);
                    return;
                }
                future.complete(eligibleBallTrails.get(new Random().nextInt(eligibleBallTrails.size())));
            });
            return future;
        }

        private void startCrateAnimation(final Player player, final Crate crate, final Location crateLocation, final CosmeticHolder reward, final CosmeticRarity rarity) {
            final World world = crateLocation.getWorld();
            new BukkitRunnable() {
                int tick = 0;

                public void run() {
                    Location particleLocation = crateLocation.clone().add(0.5, 1.0, 0.5);
                    if (rarity == CosmeticRarity.COMMON) {
                        world.spawnParticle(Particle.HAPPY_VILLAGER, particleLocation, 10, 0.3, 0.3, 0.3);
                        world.playSound(particleLocation, Sound.BLOCK_NOTE_BLOCK_BIT, 1.0f, 1.2f);
                    } else if (rarity == CosmeticRarity.RARE) {
                        world.spawnParticle(Particle.DUST, particleLocation, 20, 0.4, 0.4, 0.4, (Object) new Particle.DustOptions(Color.BLUE, 1.0f));
                        world.spawnParticle(Particle.END_ROD, particleLocation, 10, 0.3, 0.3, 0.3, 0.1);
                        world.playSound(particleLocation, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.2f, 1.0f);
                    } else if (rarity == CosmeticRarity.EPIC) {
                        world.spawnParticle(Particle.WITCH, particleLocation, 20, 0.5, 0.5, 0.5);
                        world.playSound(particleLocation, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.0f, 0.8f);
                    } else if (rarity == CosmeticRarity.LEGENDARY) {
                        world.spawnParticle(Particle.FLASH, particleLocation, 1);
                        world.spawnParticle(Particle.TOTEM_OF_UNDYING, particleLocation, 30, 0.6, 0.6, 0.6);
                        world.spawnParticle(Particle.DRAGON_BREATH, particleLocation, 50, 0.5, 0.5, 0.5);
                        world.playSound(particleLocation, Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 1.0f);
                        world.playSound(particleLocation, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.5f, 1.0f);
                        world.playSound(particleLocation, Sound.ENTITY_FIREWORK_ROCKET_BLAST_FAR, 1.5f, 0.8f);
                    }
                    if (this.tick >= 60) {
                        this.cancel();
                        announceCrateReward(player, crate, crateLocation, reward);
                    }
                    ++this.tick;
                }
            }.runTaskTimer(Partix.getInstance(), 0L, 1L);
        }

        private void announceCrateReward(Player player, Crate crate, Location crateLocation, CosmeticHolder reward) {
            if (reward != null) {
                String rewardName = reward.getName();
                boolean alreadyOwned = player.hasPermission(reward.getPermission());
                if (alreadyOwned) {
                    int refundAmount = crate.price / 2;
                    PlayerDb.get(player.getUniqueId(), PlayerDb.Stat.COINS).thenAccept(coins -> {
                        int newBalance = coins + refundAmount;
                        PlayerDb.set(player.getUniqueId(), PlayerDb.Stat.COINS, newBalance);
                        player.sendMessage("§cYou already own §6" + rewardName + "§c! Refunding §e" + refundAmount + " Coins.");
                    });
                } else {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + player.getName() + " permission set " + reward.getPermission());
                    player.sendMessage("§aYou won: §6" + rewardName + "!");
                }
                if (reward instanceof CosmeticSound greenSound) {
                    Athlete athlete = AthleteManager.get(player.getUniqueId());
                    athlete.setGreenSound(greenSound);
                    try {
                        int soundKey = Integer.parseInt(greenSound.getKey());
                        PlayerDb.set(player.getUniqueId(), PlayerDb.Stat.GREEN_SOUND, soundKey);
                    } catch (NumberFormatException e) {
                        Bukkit.getLogger().severe("[ERROR] Invalid key format for Green Sound: " + greenSound.getKey());
                    }
                    player.sendMessage("§aEquipped Green Sound: §6" + greenSound.getName());
                    this.playGreenSound(greenSound.getSoundIdentifier(), crateLocation);
                }
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                this.spawnRewardHologram(crateLocation, rewardName);
            } else {
                player.sendMessage("§cYou already own all cosmetics of this rarity!");
            }
        }

        private void playGreenSound(String soundIdentifier, Location soundLocation) {
            if (soundIdentifier == null || soundIdentifier.isEmpty() || soundLocation == null) {
                Bukkit.getLogger().severe("[ERROR] Invalid sound identifier or location for playing green sound.");
                return;
            }
            double maxDistance = 200.0;
            double maxDistanceSquared = maxDistance * maxDistance;
            float volume = 15.0f;
            float pitch = 1.0f;
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!(player.getLocation().distanceSquared(soundLocation) <= maxDistanceSquared)) continue;
                player.playSound(player.getLocation(), soundIdentifier, SoundCategory.PLAYERS, volume, pitch);
            }
        }

        private void spawnRewardHologram(Location location, String rewardName) {
            location = location.clone().add(0.5, 2.5, 0.5);
            final ArmorStand hologram = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
            hologram.setCustomNameVisible(true);
            hologram.setCustomName("§6§lYou Won: " + rewardName);
            hologram.setGravity(false);
            hologram.setInvisible(true);
            hologram.setMarker(true);
            new BukkitRunnable() {

                public void run() {
                    hologram.remove();
                }
            }.runTaskLater(Partix.getInstance(), 40L);
        }

        private CompletableFuture<CosmeticHolder> getRandomReward(Player player, CosmeticRarity rarity) {
            final CompletableFuture<CosmeticHolder> future = new CompletableFuture<>();
            Bukkit.getScheduler().runTaskAsynchronously(Partix.getInstance(), () -> {
                ArrayList<CosmeticHolder> available = new ArrayList<>();
                available.addAll(trails.values().stream().filter(c -> c.getRarity() == rarity && !Cosmetics.playerHasCosmetic(player, c).join()).filter(c -> !CrateHandler.isExcludedCosmetic(c)).toList());
                available.addAll(explosions.values().stream().filter(c -> c.getRarity() == rarity && !Cosmetics.playerHasCosmetic(player, c).join()).filter(c -> !CrateHandler.isExcludedCosmetic(c)).toList());
                available.addAll(greenSounds.values().stream().filter(c -> c.getRarity() == rarity && !Cosmetics.playerHasCosmetic(player, c).join()).filter(c -> !CrateHandler.isExcludedCosmetic(c)).toList());
                available.addAll(ballTrails.values().stream().filter(c -> c.getRarity() == rarity && !CrateHandler.isExcludedCosmetic(c)).filter(c -> !CrateHandler.isExcludedCosmetic(c)).toList());
                if (available.isEmpty()) {
                    future.complete(null);
                    return;
                }
                future.complete(available.get(new Random().nextInt(available.size())));
            });
            return future;
        }

        public void repulsePlayer(Player player) {
            Vector direction = player.getLocation().getDirection().multiply(-1).normalize();
            if (!this.isFiniteVector(direction)) {
                player.sendMessage("Failed to repulse due to invalid vector!");
                return;
            }
            player.setVelocity(direction.multiply(1.5));
        }

        private boolean isFiniteVector(Vector vector) {
            return Double.isFinite(vector.getX()) && Double.isFinite(vector.getY()) && Double.isFinite(vector.getZ());
        }

        private static class Crate {
            private final String name;
            private final int price;
            private final CosmeticRarity rarity;
            private final Material material;

            public Crate(String name, int price, CosmeticRarity rarity, Material material) {
                this.name = name;
                this.price = price;
                this.rarity = rarity;
                this.material = material;
            }
        }

        private static class Prize {
            @Getter
            private final String name;
            @Getter
            private final int coins;
            @Getter
            private final Material material;
            private final int weight;

            public Prize(String name, int coins, Material material, int weight) {
                this.name = name;
                this.coins = coins;
                this.material = material;
                this.weight = weight;
            }

            public int getChance() {
                return this.weight;
            }

            public ItemStack toItem() {
                return Items.get(Component.text(this.name + " (" + this.coins + " Coins)"), this.material);
            }
        }
    }

    private static class SpinPrize {
        final String name;
        final int coins;
        final Material material;
        final int weight;

        SpinPrize(String name, int coins, Material material, int weight) {
            this.name = name;
            this.coins = coins;
            this.material = material;
            this.weight = weight;
        }

        ItemStack toItem() {
            return Items.get(Component.text(this.name + " (" + this.coins + " Coins)"), this.material);
        }
    }
}

