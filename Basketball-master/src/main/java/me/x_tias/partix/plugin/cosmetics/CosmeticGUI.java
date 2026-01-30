/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  org.bukkit.Material
 *  org.bukkit.enchantments.Enchantment
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemFlag
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 */
package me.x_tias.partix.plugin.cosmetics;

import me.x_tias.partix.Partix;
import me.x_tias.partix.database.PlayerDb;
import me.x_tias.partix.plugin.athlete.Athlete;
import me.x_tias.partix.plugin.athlete.AthleteManager;
import me.x_tias.partix.plugin.gui.GUI;
import me.x_tias.partix.plugin.gui.ItemButton;
import me.x_tias.partix.util.Colour;
import me.x_tias.partix.util.Items;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;


public class CosmeticGUI {
    public CosmeticGUI(Player player) {
        ItemButton[] buttons = new ItemButton[27];
        for (int i = 0; i < buttons.length; ++i) {
            buttons[i] = new ItemButton(i, Items.get(Component.text(" "), Material.BLACK_STAINED_GLASS_PANE, 1, " "), p -> {
            });
        }
        buttons[10] = new ItemButton(10, Items.get(Component.text("Trails").color(Colour.partix()), Material.MELON_SEEDS), p -> this.trails(p, 1));
        buttons[12] = new ItemButton(12, Items.get(Component.text("Explosions").color(Colour.partix()), Material.GUNPOWDER), p -> this.explosions(p, 1));
        buttons[14] = new ItemButton(14, Items.get(Component.text("Green Sounds").color(Colour.partix()), Material.LIME_CONCRETE), p -> this.openGreenSoundMenu(p, 1));
        buttons[16] = new ItemButton(16, Items.get(Component.text("Ball Trails").color(Colour.partix()), Material.SLIME_BALL), p -> this.openBallTrailsMenu(p, 1));
        buttons[20] = new ItemButton(20, Items.get(Component.text("Crate Inventory").color(Colour.partix()), Material.CHEST), p -> this.openCrateInventory(p));
        buttons[22] = new ItemButton(22, Items.get(Component.text("Accessories").color(Colour.partix()), Material.LEATHER_HELMET), p -> this.openAccessoriesGUI(p));
        new GUI("Cosmetics Menu", 3, false, buttons).openInventory(player);
    }

    private void trails(Player player, int page) {
        final int[] index = new int[1];
        List<Map.Entry<Integer, CosmeticParticle>> availableTrails = Cosmetics.trails.entrySet().stream().filter(entry -> player.hasPermission(entry.getValue().getPermission())).toList();
        Athlete athlete = AthleteManager.get(player.getUniqueId());
        PlayerDb.get(player.getUniqueId(), PlayerDb.Stat.TRAIL).thenAccept(equippedTrailKey -> Bukkit.getScheduler().runTask(Partix.getInstance(), () -> {
            int maxItemsPerPage = 45;
            int totalPages = Math.max(1, (int) Math.ceil((double) availableTrails.size() / (double) maxItemsPerPage));
            ItemButton[] buttons = new ItemButton[54];
            for (int i = 0; i < maxItemsPerPage && (index[0] = (page - 1) * maxItemsPerPage + i) < availableTrails.size(); ++i) {
                Map.Entry<Integer, CosmeticParticle> entry2 = availableTrails.get(index[0]);
                int trailKey = entry2.getKey();
                CosmeticParticle trail = entry2.getValue();
                boolean isSelected = equippedTrailKey == trailKey;
                ItemStack item = trail.getGUIItem().clone();
                if (isSelected) {
                    ItemMeta meta = item.getItemMeta();
                    meta.setEnchantmentGlintOverride(true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    item.setItemMeta(meta);
                }
                buttons[i] = new ItemButton(i, item, p -> {
                    athlete.setTrail(trailKey);
                    PlayerDb.set(player.getUniqueId(), PlayerDb.Stat.TRAIL, trailKey);
                    p.sendMessage("§aTrail equipped: §6" + trail.getName());
                    this.trails(p, page);
                });
            }
            if (page > 1) {
                buttons[45] = new ItemButton(45, this.createArrow("§aPrevious Page"), p -> this.trails(p, page - 1));
            }
            if (page < totalPages) {
                buttons[53] = new ItemButton(53, this.createArrow("§aNext Page"), p -> this.trails(p, page + 1));
            }
            new GUI("Select Trail - Page " + page, 6, false, buttons).openInventory(player);
        }));
    }
    private void openCrateInventory(Player player) {
        CrateInventory.getCrates(player.getUniqueId()).thenAccept(crates ->
                Bukkit.getScheduler().runTask(Partix.getInstance(), () -> {
                    if (crates.isEmpty()) {
                        player.sendMessage("§cYou don't have any crates!");
                        player.sendMessage("§7Visit the crate NPCs to purchase crates!");
                        return;
                    }

                    ItemButton[] buttons = new ItemButton[54];
                    int slot = 0;

                    for (Map.Entry<String, Integer> entry : crates.entrySet()) {
                        String crateKey = entry.getKey();
                        int count = entry.getValue();

                        CrateInventory.CrateType type = CrateInventory.parseCrateType(crateKey);
                        CosmeticRarity rarity = CrateInventory.parseCrateRarity(crateKey);

                        if (type == null || rarity == null) continue;

                        Material material = getCrateMaterial(rarity);
                        String displayName = rarity.getColor() + rarity.name() + " " + type.getDisplayName() + " Crate";

                        ItemStack item = Items.get(
                                Component.text(displayName),
                                material,
                                count,
                                "§7Click to open or preview",
                                "§eQuantity: §6" + count
                        );

                        buttons[slot] = new ItemButton(slot, item, p -> {
                            openCrateOptionsGUI(p, type, rarity);
                        });

                        slot++;
                        if (slot >= 45) break;
                    }

                    // Back button
                    buttons[49] = new ItemButton(49,
                            Items.get(Component.text("§cBack"), Material.ARROW),
                            p -> new CosmeticGUI(p)
                    );

                    new GUI("Your Crates", 6, false, buttons).openInventory(player);
                })
        );
    }

    private void openCrateOptionsGUI(Player player, CrateInventory.CrateType type, CosmeticRarity rarity) {
        ItemButton[] buttons = new ItemButton[27];

        // Fill with glass panes
        for (int i = 0; i < 27; ++i) {
            buttons[i] = new ItemButton(i, Items.get(Component.text(" "), Material.BLACK_STAINED_GLASS_PANE, 1, " "), p -> {});
        }

        // Open button
        buttons[11] = new ItemButton(11,
                Items.get(Component.text("§aOpen Crate"), Material.TRIPWIRE_HOOK, 1, "§7Click to open this crate"),
                p -> {
                    CrateInventory.removeCrate(p.getUniqueId(), type, rarity).thenAccept(success -> {
                        if (success) {
                            Bukkit.getScheduler().runTask(Partix.getInstance(), () -> {
                                openCrateAnimation(p, type, rarity);
                            });
                        } else {
                            p.sendMessage("§cYou don't have this crate!");
                        }
                    });
                }
        );

        // Preview button
        buttons[13] = new ItemButton(13,
                Items.get(Component.text("§ePreview Rewards"), Material.COMPASS, 1, "§7See what you can win"),
                p -> {
                    Cosmetics.openPreviewGUIByRarity(p, rarity, 1);
                }
        );

        // Back button
        buttons[15] = new ItemButton(15,
                Items.get(Component.text("§cBack"), Material.ARROW),
                p -> openCrateInventory(p)
        );

        String title = rarity.getColor() + rarity.name() + " " + type.getDisplayName() + " Crate";
        new GUI(title, 3, false, buttons).openInventory(player);
    }

    private Material getCrateMaterial(CosmeticRarity rarity) {
        return switch(rarity) {
            case COMMON -> Material.LIGHT_GRAY_SHULKER_BOX;
            case RARE -> Material.LIGHT_BLUE_SHULKER_BOX;
            case EPIC -> Material.PURPLE_SHULKER_BOX;
            case LEGENDARY -> Material.YELLOW_SHULKER_BOX;
        };
    }

    private void openCrateAnimation(Player player, CrateInventory.CrateType type, CosmeticRarity rarity) {
        player.closeInventory();

        // Get random reward based on type and rarity
        CosmeticHolder reward = getRewardForCrate(player, type, rarity);

        if (reward == null) {
            player.sendMessage("§cNo available rewards! Refunding crate...");
            CrateInventory.addCrate(player.getUniqueId(), type, rarity);
            return;
        }

        // Use existing animation system
        Location playerLoc = player.getLocation();
        startCrateAnimationEffect(player, playerLoc, reward, rarity);
    }
    private CosmeticHolder getRewardForCrate(Player player, CrateInventory.CrateType type, CosmeticRarity rarity) {
        List<CosmeticHolder> available = new ArrayList<>();

        switch(type) {
            case TRAIL -> {
                available.addAll(Cosmetics.trails.values().stream()
                        .filter(c -> c.getRarity() == rarity)
                        .filter(c -> !player.hasPermission(c.getPermission()))
                        .filter(c -> !Cosmetics.CrateHandler.isExcludedCosmetic(c))
                        .toList());
            }
            case EXPLOSION -> {
                available.addAll(Cosmetics.explosions.values().stream()
                        .filter(c -> c.getRarity() == rarity)
                        .filter(c -> !player.hasPermission(c.getPermission()))
                        .filter(c -> !Cosmetics.CrateHandler.isExcludedCosmetic(c))
                        .toList());
            }
            case GREEN_SOUND -> {
                available.addAll(Cosmetics.greenSounds.values().stream()
                        .filter(c -> c.getRarity() == rarity)
                        .filter(c -> !player.hasPermission(c.getPermission()))
                        .filter(c -> !Cosmetics.CrateHandler.isExcludedCosmetic(c))
                        .toList());
            }
            case BALL_TRAIL -> {
                available.addAll(Cosmetics.ballTrails.values().stream()
                        .filter(c -> c.getRarity() == rarity)
                        .filter(c -> !player.hasPermission(c.getPermission()))
                        .filter(c -> !Cosmetics.CrateHandler.isExcludedCosmetic(c))
                        .toList());
            }
        }

        if (available.isEmpty()) return null;
        return available.get(new Random().nextInt(available.size()));
    }

    private void startCrateAnimationEffect(Player player, Location location, CosmeticHolder reward, CosmeticRarity rarity) {
        World world = location.getWorld();

        new BukkitRunnable() {
            int tick = 0;

            public void run() {
                Location particleLocation = location.clone().add(0, 2.0, 0);

                if (rarity == CosmeticRarity.COMMON) {
                    world.spawnParticle(Particle.HAPPY_VILLAGER, particleLocation, 10, 0.3, 0.3, 0.3);
                    world.playSound(particleLocation, Sound.BLOCK_NOTE_BLOCK_BIT, 1.0f, 1.2f);
                } else if (rarity == CosmeticRarity.RARE) {
                    world.spawnParticle(Particle.DUST, particleLocation, 20, 0.4, 0.4, 0.4, new Particle.DustOptions(Color.BLUE, 1.0f));
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
                }

                if (tick >= 60) {
                    cancel();
                    announceReward(player, reward);
                }
                tick++;
            }
        }.runTaskTimer(Partix.getInstance(), 0L, 1L);
    }

    private void announceReward(Player player, CosmeticHolder reward) {
        if (reward != null) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + player.getName() + " permission set " + reward.getPermission());
            player.sendMessage("§aYou won: §6" + reward.getName() + "§a!");
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

            // Auto-equip green sounds
            if (reward instanceof CosmeticSound greenSound) {
                Athlete athlete = AthleteManager.get(player.getUniqueId());
                athlete.setGreenSound(greenSound);
                try {
                    int soundKey = Integer.parseInt(greenSound.getKey());
                    PlayerDb.set(player.getUniqueId(), PlayerDb.Stat.GREEN_SOUND, soundKey);
                    player.sendMessage("§aEquipped Green Sound: §6" + greenSound.getName());
                } catch (NumberFormatException e) {
                    Bukkit.getLogger().severe("[ERROR] Invalid key format for Green Sound: " + greenSound.getKey());
                }
            }
        }
    }


    private void explosions(Player player, int page) {
        final int[] index = new int[1];
        List<Map.Entry<Integer, CosmeticParticle>> availableExplosions = Cosmetics.explosions.entrySet().stream().filter(entry -> player.hasPermission(entry.getValue().getPermission())).toList();
        Athlete athlete = AthleteManager.get(player.getUniqueId());
        PlayerDb.get(player.getUniqueId(), PlayerDb.Stat.EXPLOSION).thenAccept(equippedExplosionKey -> Bukkit.getScheduler().runTask(Partix.getInstance(), () -> {
            int maxItemsPerPage = 45;
            int totalPages = Math.max(1, (int) Math.ceil((double) availableExplosions.size() / (double) maxItemsPerPage));
            ItemButton[] buttons = new ItemButton[54];
            for (int i = 0; i < maxItemsPerPage && (index[0] = (page - 1) * maxItemsPerPage + i) < availableExplosions.size(); ++i) {
                Map.Entry<Integer, CosmeticParticle> entry2 = availableExplosions.get(index[0]);
                int explosionKey = entry2.getKey();
                CosmeticParticle explosion = entry2.getValue();
                boolean isSelected = equippedExplosionKey == explosionKey;
                ItemStack item = explosion.getGUIItem().clone();
                if (isSelected) {
                    ItemMeta meta = item.getItemMeta();
                    meta.setEnchantmentGlintOverride(true);
                    meta.setEnchantmentGlintOverride(true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    item.setItemMeta(meta);
                }
                buttons[i] = new ItemButton(i, item, p -> {
                    athlete.setExplosion(explosionKey);
                    PlayerDb.set(player.getUniqueId(), PlayerDb.Stat.EXPLOSION, explosionKey);
                    p.sendMessage("§aExplosion equipped: §6" + explosion.getName());
                    this.explosions(p, page);
                });
            }
            if (page > 1) {
                buttons[45] = new ItemButton(45, this.createArrow("§aPrevious Page"), p -> this.explosions(p, page - 1));
            }
            if (page < totalPages) {
                buttons[53] = new ItemButton(53, this.createArrow("§aNext Page"), p -> this.explosions(p, page + 1));
            }
            new GUI("Select Explosion - Page " + page, 6, false, buttons).openInventory(player);
        }));
    }
    private void openAccessoriesGUI(Player player) {
        // Get current selection
        PlayerDb.get(player.getUniqueId(), PlayerDb.Stat.ACCESSORY).thenAccept(currentAccessory -> {
            Bukkit.getScheduler().runTask(Partix.getInstance(), () -> {
                List<AccessoryOption> accessoryOptions = new ArrayList<>();

                // Nothing (always available)
                boolean isNothingSelected = (currentAccessory == 0);
                ItemStack nothingItem = Items.get(
                        Component.text("Nothing").color(isNothingSelected ? Colour.allow() : Colour.partix()),
                        Material.BARRIER, 1,
                        isNothingSelected ? "§a✓ Currently Selected" : "§7Click to select"
                );
                if (isNothingSelected) {
                    ItemMeta meta = nothingItem.getItemMeta();
                    meta.setEnchantmentGlintOverride(true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    nothingItem.setItemMeta(meta);
                }
                accessoryOptions.add(new AccessoryOption(nothingItem, p -> {
                    PlayerDb.set(p.getUniqueId(), PlayerDb.Stat.ACCESSORY, 0);
                    p.sendMessage("§aNo accessory equipped!");
                    this.openAccessoriesGUI(p);
                }));

                // Headband (available to all players - no permission required)
                boolean isHeadbandSelected = (currentAccessory == 1);
                ItemStack headbandItem = Items.get(
                        Component.text("Headband").color(isHeadbandSelected ? Colour.allow() : Colour.partix()),
                        Material.LEATHER_HELMET, 1,
                        isHeadbandSelected ? "§a✓ Currently Selected" : "§7Click to select"
                );
                if (isHeadbandSelected) {
                    ItemMeta meta = headbandItem.getItemMeta();
                    meta.setEnchantmentGlintOverride(true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    headbandItem.setItemMeta(meta);
                }
                accessoryOptions.add(new AccessoryOption(headbandItem, p -> {
                    PlayerDb.set(p.getUniqueId(), PlayerDb.Stat.ACCESSORY, 1);
                    p.sendMessage("§aHeadband equipped!");
                    this.openAccessoriesGUI(p);
                }));

                // Remmy Hat - now shows as carved pumpkin (only show if player has permission)
                if (player.hasPermission("cosmetic.accessory.remmy")) {
                    boolean isRemmySelected = (currentAccessory == 2);
                    ItemStack remmyItem = Items.get(
                            Component.text("Remmy").color(isRemmySelected ? Colour.allow() : Colour.partix()),
                            Material.CARVED_PUMPKIN, 1,
                            "§7Exclusive hat accessory",
                            isRemmySelected ? "§a✓ Currently Selected" : "§7Click to select"
                    );
                    if (isRemmySelected) {
                        ItemMeta meta = remmyItem.getItemMeta();
                        meta.setEnchantmentGlintOverride(true);
                        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                        remmyItem.setItemMeta(meta);
                    }
                    accessoryOptions.add(new AccessoryOption(remmyItem, p -> {
                        PlayerDb.set(p.getUniqueId(), PlayerDb.Stat.ACCESSORY, 2);
                        p.sendMessage("§aRemmy hat equipped!");
                        this.openAccessoriesGUI(p);
                    }));
                }
                
                // Champion's Crown - Season Pass reward (check if player has unlocked it)
                PlayerDb.getString(player.getUniqueId(), PlayerDb.Stat.SEASON_PASS_CLAIMED_REWARDS).thenAccept(claimedJson -> {
                    Bukkit.getScheduler().runTask(Partix.getInstance(), () -> {
                        boolean hasCrown = false;
                        if (claimedJson != null && !claimedJson.isEmpty() && !claimedJson.equals("{}")) {
                            try {
                                com.google.gson.Gson gson = new com.google.gson.Gson();
                                java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<java.util.Set<Integer>>(){}.getType();
                                java.util.Set<Integer> claimed = gson.fromJson(claimedJson, type);
                                hasCrown = claimed != null && claimed.contains(35);
                            } catch (Exception ignored) {}
                        }
                        
                        if (hasCrown) {
                            boolean isCrownSelected = (currentAccessory == 3);
                            ItemStack crownItem = Items.get(
                                    Component.text("Champion's Crown").color(isCrownSelected ? Colour.allow() : Colour.partix()),
                                    Material.GOLDEN_HELMET, 1,
                                    "§7Season Pass exclusive",
                                    "§7Wear the crown of victory",
                                    isCrownSelected ? "§a✓ Currently Selected" : "§7Click to select"
                            );
                            if (isCrownSelected) {
                                ItemMeta meta = crownItem.getItemMeta();
                                meta.setEnchantmentGlintOverride(true);
                                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                                crownItem.setItemMeta(meta);
                            }
                            accessoryOptions.add(new AccessoryOption(crownItem, p -> {
                                PlayerDb.set(p.getUniqueId(), PlayerDb.Stat.ACCESSORY, 3);
                                p.sendMessage("§5Champion's Crown equipped!");
                                this.openAccessoriesGUI(p);
                            }));
                        }
                        
                        // Build and open GUI after checking for crown
                        buildAccessoryGUI(player, accessoryOptions);
                    });
                });
            });
        });
    }
    
    private void buildAccessoryGUI(Player player, List<AccessoryOption> accessoryOptions) {
        int guiSize = 3;
        ItemButton[] buttons = new ItemButton[guiSize * 9];

        // Fill with glass panes
        for (int i = 0; i < buttons.length; ++i) {
            buttons[i] = new ItemButton(i, Items.get(Component.text(" "), Material.BLACK_STAINED_GLASS_PANE, 1, " "), p -> {});
        }

        // Place accessories in center slots (10, 12, 14, 16, etc.)
        for (int i = 0; i < accessoryOptions.size(); i++) {
            AccessoryOption option = accessoryOptions.get(i);
            int slot = 10 + (i * 2);
            if (slot < buttons.length) {
                buttons[slot] = new ItemButton(slot, option.item, option.onClick);
            }
        }

        new GUI("Select Accessory", guiSize, false, buttons).openInventory(player);
    }

    // Helper class to store accessory options
    private static class AccessoryOption {
        ItemStack item;
        Consumer<Player> onClick;

        AccessoryOption(ItemStack item, Consumer<Player> onClick) {
            this.item = item;
            this.onClick = onClick;
        }
    }
    public void openGreenSoundMenu(Player player, int page) {
        final int[] index = new int[1];
        List<Map.Entry<Integer, CosmeticSound>> availableSounds = Cosmetics.greenSounds.entrySet().stream().filter(entry -> player.hasPermission(entry.getValue().getPermission())).toList();
        Athlete athlete = AthleteManager.get(player.getUniqueId());
        PlayerDb.get(player.getUniqueId(), PlayerDb.Stat.GREEN_SOUND).thenAccept(equippedSoundKey -> Bukkit.getScheduler().runTask(Partix.getInstance(), () -> {
            int maxItemsPerPage = 45;
            int totalPages = Math.max(1, (int) Math.ceil((double) availableSounds.size() / (double) maxItemsPerPage));
            ItemButton[] buttons = new ItemButton[54];
            for (int i = 0; i < maxItemsPerPage && (index[0] = (page - 1) * maxItemsPerPage + i) < availableSounds.size(); ++i) {
                Map.Entry<Integer, CosmeticSound> entry2 = availableSounds.get(index[0]);
                int soundKey = entry2.getKey();
                CosmeticSound sound = entry2.getValue();
                buttons[i] = new ItemButton(i, sound.getGUIItem(), p -> {
                    athlete.setGreenSound(sound);
                    PlayerDb.set(player.getUniqueId(), PlayerDb.Stat.GREEN_SOUND, soundKey);
                    p.sendMessage("§aGreen Sound equipped: §6" + sound.getName());
                    this.openGreenSoundMenu(p, page);
                });
            }
            if (page > 1) {
                buttons[45] = new ItemButton(45, this.createArrow("§aPrevious Page"), p -> this.openGreenSoundMenu(p, page - 1));
            }
            if (page < totalPages) {
                buttons[53] = new ItemButton(53, this.createArrow("§aNext Page"), p -> this.openGreenSoundMenu(p, page + 1));
            }
            new GUI("Green Sound Selector - Page " + page, 6, false, buttons).openInventory(player);
        }));
    }

    private ItemStack createPlaceholder() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        item.editMeta(meta -> meta.displayName(Component.text(" ")));
        return item;
    }

    public void openGreenSoundSelectionGUI(Player player, Consumer<CosmeticSound> callback) {
        List<Map.Entry<Integer, CosmeticSound>> availableSounds = Cosmetics.greenSounds.entrySet().stream().filter(entry -> player.hasPermission(entry.getValue().getPermission())).toList();
        Athlete athlete = AthleteManager.get(player.getUniqueId());
        PlayerDb.get(player.getUniqueId(), PlayerDb.Stat.GREEN_SOUND).thenAccept(equippedSoundKey -> Bukkit.getScheduler().runTask(Partix.getInstance(), () -> {
            CosmeticSound equippedSound = Cosmetics.greenSounds.get(equippedSoundKey);
            ItemButton[] buttons = new ItemButton[availableSounds.size()];
            for (int i = 0; i < availableSounds.size(); ++i) {
                Map.Entry<Integer, CosmeticSound> entry2 = availableSounds.get(i);
                int soundKey = entry2.getKey();
                CosmeticSound sound = entry2.getValue();
                boolean isSelected = equippedSound != null && equippedSound.equals(sound);
                ItemStack item = Items.get(Component.text(sound.getName()).color(isSelected ? Colour.allow() : Colour.partix()), sound.getMaterial(), 1, "§7" + sound.getDescription(), isSelected ? "§a✓ Equipped" : "§6Click to equip");
                if (isSelected) {
                    ItemMeta meta = item.getItemMeta();
                    meta.setEnchantmentGlintOverride(true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    item.setItemMeta(meta);
                }
                buttons[i] = new ItemButton(i, item, p -> {
                    athlete.setGreenSound(sound);
                    PlayerDb.set(player.getUniqueId(), PlayerDb.Stat.GREEN_SOUND, soundKey);
                    p.sendMessage("§aGreen Sound set to: §6" + sound.getName());
                    callback.accept(sound);
                });
            }
            new GUI("Select Green Sound", 3, false, buttons).openInventory(player);
        }));
    }

    private void openBallTrailsMenu(Player player, int page) {
        PlayerDb.get(player.getUniqueId(), PlayerDb.Stat.BALL_TRAIL).thenAccept(equippedTrailKey -> Bukkit.getScheduler().runTask(Partix.getInstance(), () -> {
            int index;
            List<Map.Entry<Integer, CosmeticBallTrail>> availableTrails = Cosmetics.ballTrails.entrySet().stream().filter(entry -> player.hasPermission(entry.getValue().getPermission())).toList();
            int maxItemsPerPage = 45;
            int totalPages = Math.max(1, (int) Math.ceil((double) availableTrails.size() / (double) maxItemsPerPage));
            ItemButton[] buttons = new ItemButton[54];
            for (int i = 0; i < maxItemsPerPage && (index = (page - 1) * maxItemsPerPage + i) < availableTrails.size(); ++i) {
                Map.Entry<Integer, CosmeticBallTrail> entry2 = availableTrails.get(index);
                int ballTrailKey = entry2.getKey();
                CosmeticBallTrail ballTrail = entry2.getValue();
                boolean isSelected = equippedTrailKey == ballTrailKey;
                ItemStack item = ballTrail.getGUIItem().clone();
                if (isSelected) {
                    ItemMeta meta = item.getItemMeta();
                    meta.setEnchantmentGlintOverride(true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    item.setItemMeta(meta);
                }
                buttons[i] = new ItemButton(i, item, p -> {
                    PlayerDb.set(player.getUniqueId(), PlayerDb.Stat.BALL_TRAIL, ballTrailKey);
                    p.sendMessage("§aBall Trail equipped: §6" + ballTrail.getName());
                    this.openBallTrailsMenu(p, page);
                });
            }
            if (page > 1) {
                buttons[45] = new ItemButton(45, this.createArrow("§aPrevious Page"), p -> this.openBallTrailsMenu(p, page - 1));
            }
            if (page < totalPages) {
                buttons[53] = new ItemButton(53, this.createArrow("§aNext Page"), p -> this.openBallTrailsMenu(p, page + 1));
            }
            new GUI("Select Ball Trail - Page " + page, 6, false, buttons).openInventory(player);
        }));
    }

    private <T extends CosmeticHolder> void openPaginatedOwnedCosmeticGUI(Player player, List<Map.Entry<Integer, T>> ownedCosmetics, int page, String title, BiConsumer<Player, Integer> equipFunction) {
        int totalItems = ownedCosmetics.size();
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
        int currentPage = page;
        int i = startIndex;
        int slot = 0;
        while (i < endIndex) {
            Map.Entry<Integer, T> entry = ownedCosmetics.get(i);
            int cosmeticKey = entry.getKey();
            CosmeticHolder cosmetic = entry.getValue();
            buttons[slot] = new ItemButton(slot, cosmetic.getGUIItem(), p -> {
                equipFunction.accept(p, cosmeticKey);
                this.openPaginatedOwnedCosmeticGUI(p, ownedCosmetics, currentPage, title, equipFunction);
            });
            ++i;
            ++slot;
        }
        if (currentPage > 1) {
            buttons[45] = new ItemButton(45, this.createArrow("§aPrevious Page"), p -> this.openPaginatedOwnedCosmeticGUI(p, ownedCosmetics, currentPage - 1, title, equipFunction));
        }
        if (currentPage < maxPages) {
            buttons[53] = new ItemButton(53, Cosmetics.createNavigationButton("Next Page", Material.ARROW), p -> this.openPaginatedOwnedCosmeticGUI(p, ownedCosmetics, currentPage + 1, title, equipFunction));
        }
        new GUI(title + " (Page " + currentPage + ")", 6, false, buttons).openInventory(player);
    }

    private ItemStack createArrow(String name) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name));
        item.setItemMeta(meta);
        return item;
    }
}

