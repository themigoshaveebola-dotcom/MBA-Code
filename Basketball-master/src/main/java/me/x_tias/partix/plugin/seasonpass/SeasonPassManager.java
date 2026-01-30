package me.x_tias.partix.plugin.seasonpass;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import me.x_tias.partix.Partix;
import me.x_tias.partix.database.PlayerDb;
import me.x_tias.partix.mini.lobby.MainLobby;
import me.x_tias.partix.plugin.athlete.Athlete;
import me.x_tias.partix.plugin.athlete.AthleteManager;
import me.x_tias.partix.plugin.cosmetics.Cosmetics;
import me.x_tias.partix.util.Colour;
import me.x_tias.partix.plugin.gui.GUI;
import me.x_tias.partix.plugin.gui.ItemButton;
import me.x_tias.partix.util.Items;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Type;
import java.util.*;

public class SeasonPassManager {
    private static final Gson GSON = new Gson();
    private static final int MAX_TIER = 50;
    private static final int EXP_PER_TIER = 1000;
    private static final int MAX_EXP_PER_GAME = 500; // Maximum exp from a single game
    
    private static final Map<Integer, SeasonPassReward> REWARDS = new HashMap<>();
    
    static {
        initializeRewards();
    }
    
    private static void initializeRewards() {
        // Tier 5 - Ball Trail
        REWARDS.put(5, new SeasonPassReward(5, SeasonPassReward.RewardType.BALL_TRAIL, 
            "§6Phoenix Trail", "§7Blazing wings follow your shots", 30, Material.BLAZE_POWDER));
        
        // Tier 10 - Goal Explosion
        REWARDS.put(10, new SeasonPassReward(10, SeasonPassReward.RewardType.GOAL_EXPLOSION,
            "§bFrostbite Explosion", "§7Icy burst on every make", 48, Material.ICE));
        
        // Tier 15 - Ball Trail
        REWARDS.put(15, new SeasonPassReward(15, SeasonPassReward.RewardType.BALL_TRAIL,
            "§dNeon Pulse Trail", "§7Electric energy trail", 31, Material.REDSTONE));
        
        // Tier 20 - Goal Explosion
        REWARDS.put(20, new SeasonPassReward(20, SeasonPassReward.RewardType.GOAL_EXPLOSION,
            "§5Galaxy Explosion", "§7Cosmic stars burst outward", 54, Material.END_ROD));
        
        // Tier 25 - Goal Explosion
        REWARDS.put(25, new SeasonPassReward(25, SeasonPassReward.RewardType.GOAL_EXPLOSION,
            "§eGolden Burst", "§7Shower of gold particles", 49, Material.GOLD_BLOCK));
        
        // Tier 30 - Ball Trail
        REWARDS.put(30, new SeasonPassReward(30, SeasonPassReward.RewardType.BALL_TRAIL,
            "§cInferno Trail", "§7Leaves a trail of flames", 32, Material.FIRE_CHARGE));
        
        // Tier 35 - Accessory
        REWARDS.put(35, new SeasonPassReward(35, SeasonPassReward.RewardType.ACCESSORY,
            "§5Champion's Crown", "§7Wear the crown of victory", 3, Material.GOLDEN_HELMET));
        
        // Tier 40 - Goal Explosion
        REWARDS.put(40, new SeasonPassReward(40, SeasonPassReward.RewardType.GOAL_EXPLOSION,
            "§9Cosmic Explosion", "§7Stars burst from the hoop", 50, Material.END_CRYSTAL));
        
        // Tier 45 - Ball Trail
        REWARDS.put(45, new SeasonPassReward(45, SeasonPassReward.RewardType.BALL_TRAIL,
            "§aEmerald Comet", "§7Green energy trail", 33, Material.EMERALD));
        
        // Tier 50 - Ball Trail
        REWARDS.put(50, new SeasonPassReward(50, SeasonPassReward.RewardType.BALL_TRAIL,
            "§6§lSupreme Ball Trail", "§7The ultimate ball trail", 29, Material.DIAMOND));
        
        // Coins for non-special tiers
        for (int tier = 1; tier <= MAX_TIER; tier++) {
            if (!REWARDS.containsKey(tier)) {
                int coinAmount = 100 + (tier * 20); // Increases as tiers progress
                REWARDS.put(tier, new SeasonPassReward(tier, SeasonPassReward.RewardType.COINS,
                    "§e" + coinAmount + " Coins", "§7In-game currency", coinAmount, Material.GOLD_NUGGET));
            }
        }
    }
    
    /**
     * Award experience to a player from ranked game performance
     */
    public static void awardGameExp(UUID playerUUID, int points, int assists, int rebounds, int steals, int blocks, boolean won) {
        Bukkit.getScheduler().runTaskAsynchronously(Partix.getInstance(), () -> {
            // Calculate exp based on performance
            int exp = 0;
            exp += points * 10;     // 10 exp per point (increased from 5)
            exp += assists * 20;    // 20 exp per assist (increased from 10)
            exp += rebounds * 15;   // 15 exp per rebound (increased from 8)
            exp += steals * 25;     // 25 exp per steal (increased from 12)
            exp += blocks * 30;     // 30 exp per block (increased from 15)
            
            if (won) {
                exp += 200; // Bonus for winning (increased from 100)
            } else {
                exp += 100; // Consolation for losing (increased from 50)
            }
            
            // Cap at max exp per game
            exp = Math.min(exp, MAX_EXP_PER_GAME);
            
            final int finalExp = exp; // Make final for lambda
            
            // Add exp
            PlayerDb.add(playerUUID, PlayerDb.Stat.SEASON_PASS_EXP, finalExp);
            
            // Check for tier ups
            PlayerDb.get(playerUUID, PlayerDb.Stat.SEASON_PASS_EXP).thenAccept(totalExp -> {
                PlayerDb.get(playerUUID, PlayerDb.Stat.SEASON_PASS_TIER).thenAccept(currentTier -> {
                    int newTier = Math.min((totalExp / EXP_PER_TIER) + 1, MAX_TIER);
                    
                    if (newTier > currentTier) {
                        // Tier up!
                        PlayerDb.set(playerUUID, PlayerDb.Stat.SEASON_PASS_TIER, newTier);
                        
                        Player player = Bukkit.getPlayer(playerUUID);
                        if (player != null && player.isOnline()) {
                            player.sendMessage(Component.text("═══════════════════════════════════").color(Colour.partix()));
                            player.sendMessage(Component.text("     SEASON PASS TIER UP!").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
                            player.sendMessage(Component.text(""));
                            player.sendMessage(Component.text("     You reached ").color(NamedTextColor.YELLOW)
                                .append(Component.text("Tier " + newTier).color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD)));
                            player.sendMessage(Component.text("     +").color(NamedTextColor.GREEN)
                                .append(Component.text(finalExp + " EXP").color(NamedTextColor.YELLOW))
                                .append(Component.text(" earned this game").color(NamedTextColor.GRAY)));
                            player.sendMessage(Component.text("═══════════════════════════════════").color(Colour.partix()));
                            
                            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER, 1.0f, 1.0f);
                            
                            // Update sidebar
                            Athlete athlete = AthleteManager.get(playerUUID);
                            if (athlete != null && athlete.getPlace() instanceof MainLobby lobby) {
                                lobby.updateSidebar(athlete);
                            }
                        }
                    } else {
                        // Just exp gain
                        Player player = Bukkit.getPlayer(playerUUID);
                        if (player != null && player.isOnline()) {
                            player.sendMessage(Component.text("+" + finalExp + " Season Pass EXP").color(NamedTextColor.GREEN));
                        }
                    }
                });
            });
        });
    }
    
    /**
     * Open the Season Pass GUI for a player
     */
    public static void openSeasonPassGUI(Player player) {
        openSeasonPassGUI(player, 1);
    }
    
    /**
     * Open the Season Pass GUI for a player at a specific page
     */
    public static void openSeasonPassGUI(Player player, int page) {
        UUID uuid = player.getUniqueId();
        
        PlayerDb.get(uuid, PlayerDb.Stat.SEASON_PASS_TIER).thenAccept(currentTier -> {
            PlayerDb.get(uuid, PlayerDb.Stat.SEASON_PASS_EXP).thenAccept(totalExp -> {
                PlayerDb.getString(uuid, PlayerDb.Stat.SEASON_PASS_CLAIMED_REWARDS).thenAccept(claimedJson -> {
                    
                    Bukkit.getScheduler().runTask(Partix.getInstance(), () -> {
                        Set<String> claimedRewards = parseClaimedRewards(claimedJson);
                        int currentTierExp = totalExp % EXP_PER_TIER;
                        
                        // 3 rows (27 slots)
                        ItemButton[] buttons = new ItemButton[27];
                        
                        // Fill with filler
                        ItemStack FILLER = Items.get(Component.text(" "), Material.BLACK_STAINED_GLASS_PANE, 1, " ");
                        for (int i = 0; i < 27; i++) {
                            buttons[i] = new ItemButton(i, FILLER, p -> {});
                        }
                        
                        // Calculate tiers per page (7 items in middle row)
                        final int ITEMS_PER_PAGE = 7;
                        int totalPages = (int) Math.ceil((double) MAX_TIER / ITEMS_PER_PAGE);
                        int currentPage = Math.max(1, Math.min(page, totalPages));
                        
                        int startTier = ((currentPage - 1) * ITEMS_PER_PAGE) + 1;
                        int endTier = Math.min(MAX_TIER, startTier + ITEMS_PER_PAGE - 1);
                        
                        // Info item (top middle)
                        buttons[4] = new ItemButton(4, Items.get(
                            Component.text("Season Pass").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD),
                            Material.ENCHANTED_BOOK, 1,
                            "§r",
                            "§7Current Tier: §6" + currentTier + "§7/§6" + MAX_TIER,
                            "§7Progress: §e" + currentTierExp + "§7/§e" + EXP_PER_TIER + " EXP",
                            "§r",
                            "§7Page: §e" + currentPage + "§7/§e" + totalPages,
                            "§r",
                            "§7Earn EXP by playing ranked games!",
                            "§7Performance determines EXP gained."
                        ), p -> {});
                        
                        // Show tiers in middle row (slots 10-16)
                        int slot = 10;
                        for (int tier = startTier; tier <= endTier; tier++) {
                            SeasonPassReward reward = REWARDS.get(tier);
                            if (reward == null) continue;
                            
                            boolean unlocked = currentTier >= tier;
                            boolean claimed = claimedRewards.contains(reward.getId());
                            
                            List<String> lore = new ArrayList<>();
                            lore.add("§r");
                            lore.add("§7Tier: §6" + tier);
                            lore.add("§7" + reward.getDescription());
                            lore.add("§r");
                            
                            if (!unlocked) {
                                lore.add("§c§l✘ LOCKED");
                                lore.add("§7Reach tier " + tier + " to unlock");
                            } else if (claimed) {
                                lore.add("§a§l✓ CLAIMED");
                            } else {
                                lore.add("§e§l➤ CLICK TO CLAIM");
                            }
                            
                            ItemStack item = Items.get(
                                Component.text(reward.getName()).decoration(TextDecoration.ITALIC, false),
                                reward.getDisplayMaterial(),
                                1,
                                lore.toArray(new String[0])
                            );
                            
                            // Add enchantment glow to claimed items
                            if (claimed) {
                                item.editMeta(meta -> {
                                    meta.addEnchant(org.bukkit.enchantments.Enchantment.LUCK_OF_THE_SEA, 1, true);
                                    meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                                });
                            }
                            
                            buttons[slot] = new ItemButton(slot, item, p -> {
                                if (unlocked && !claimed) {
                                    claimReward(p, reward, claimedRewards);
                                }
                            });
                            
                            slot++;
                        }
                        
                        // Previous page button (bottom left)
                        if (currentPage > 1) {
                            buttons[19] = new ItemButton(19, Items.get(
                                Component.text("« Previous Page").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD),
                                Material.ARROW, 1,
                                "§r",
                                "§7Click to view page " + (currentPage - 1),
                                "§7Tiers " + ((currentPage - 2) * ITEMS_PER_PAGE + 1) + "-" + ((currentPage - 1) * ITEMS_PER_PAGE)
                            ), p -> {
                                openSeasonPassGUI(p, currentPage - 1);
                            });
                        }
                        
                        // Next page button (bottom right)
                        if (currentPage < totalPages) {
                            buttons[25] = new ItemButton(25, Items.get(
                                Component.text("Next Page »").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD),
                                Material.ARROW, 1,
                                "§r",
                                "§7Click to view page " + (currentPage + 1),
                                "§7Tiers " + (currentPage * ITEMS_PER_PAGE + 1) + "-" + Math.min((currentPage + 1) * ITEMS_PER_PAGE, MAX_TIER)
                            ), p -> {
                                openSeasonPassGUI(p, currentPage + 1);
                            });
                        }
                        
                        // Close button (bottom middle)
                        buttons[22] = new ItemButton(22, Items.get(
                            Component.text("Close").color(NamedTextColor.RED).decorate(TextDecoration.BOLD),
                            Material.BARRIER, 1
                        ), p -> {
                            p.closeInventory();
                        });
                        
                        new GUI("§6§lSeason Pass §8(Page " + currentPage + "/" + totalPages + ")", 3, false, buttons).openInventory(player);
                    });
                });
            });
        });
    }
    
    private static void claimReward(Player player, SeasonPassReward reward, Set<String> claimedRewards) {
        UUID uuid = player.getUniqueId();
        
        // Add to claimed rewards
        claimedRewards.add(reward.getId());
        String claimedJson = GSON.toJson(claimedRewards);
        PlayerDb.setString(uuid, PlayerDb.Stat.SEASON_PASS_CLAIMED_REWARDS, claimedJson);
        
        // Grant the reward
        switch (reward.getType()) {
            case COINS:
                PlayerDb.add(uuid, PlayerDb.Stat.COINS, reward.getValue());
                player.sendMessage(Component.text("✓ Claimed ").color(NamedTextColor.GREEN)
                    .append(Component.text(reward.getValue() + " Coins").color(NamedTextColor.GOLD)));
                break;
                
            case BALL_TRAIL:
                PlayerDb.set(uuid, PlayerDb.Stat.BALL_TRAIL, reward.getValue());
                player.sendMessage(Component.text("✓ Unlocked ").color(NamedTextColor.GREEN)
                    .append(Component.text(reward.getName()).color(NamedTextColor.GOLD)));
                break;
                
            case GOAL_EXPLOSION:
                PlayerDb.set(uuid, PlayerDb.Stat.EXPLOSION, reward.getValue());
                player.sendMessage(Component.text("✓ Unlocked ").color(NamedTextColor.GREEN)
                    .append(Component.text(reward.getName()).color(NamedTextColor.GOLD)));
                break;
                
            case ACCESSORY:
                PlayerDb.set(uuid, PlayerDb.Stat.ACCESSORY, reward.getValue());
                player.sendMessage(Component.text("✓ Unlocked ").color(NamedTextColor.GREEN)
                    .append(Component.text(reward.getName()).color(NamedTextColor.GOLD)));
                break;
        }
        
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1.0f, 1.0f);
        
        // Reopen GUI
        player.closeInventory();
        Bukkit.getScheduler().runTaskLater(Partix.getInstance(), () -> openSeasonPassGUI(player), 2L);
    }
    
    private static Set<String> parseClaimedRewards(String json) {
        if (json == null || json.isEmpty() || json.equals("{}")) {
            return new HashSet<>();
        }
        
        try {
            Type setType = new TypeToken<HashSet<String>>(){}.getType();
            Set<String> set = GSON.fromJson(json, setType);
            return set != null ? set : new HashSet<>();
        } catch (Exception e) {
            return new HashSet<>();
        }
    }
    
    /**
     * Get current tier and exp for sidebar display
     */
    public static String getTierProgress(UUID playerUUID) {
        try {
            int tier = PlayerDb.get(playerUUID, PlayerDb.Stat.SEASON_PASS_TIER).join();
            int totalExp = PlayerDb.get(playerUUID, PlayerDb.Stat.SEASON_PASS_EXP).join();
            int currentTierExp = totalExp % EXP_PER_TIER;
            
            return "§6Tier " + tier + " §7(" + currentTierExp + "/" + EXP_PER_TIER + ")";
        } catch (Exception e) {
            return "§6Tier 1 §7(0/1000)";
        }
    }
}
