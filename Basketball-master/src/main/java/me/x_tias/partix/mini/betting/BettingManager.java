/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.TextComponent
 *  net.kyori.adventure.text.format.TextColor
 *  net.kyori.adventure.text.format.TextDecoration
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.Material
 *  org.bukkit.command.CommandSender
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.enchantments.Enchantment
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.InventoryView
 *  org.bukkit.inventory.ItemFlag
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitRunnable
 */
package me.x_tias.partix.mini.betting;

import lombok.Getter;
import me.x_tias.partix.Partix;
import me.x_tias.partix.database.PlayerDb;
import me.x_tias.partix.plugin.gui.GUI;
import me.x_tias.partix.plugin.gui.ItemButton;
import me.x_tias.partix.util.Colour;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class BettingManager {
    private static final Map<String, GameConfig> gamesMap = new LinkedHashMap<>();
    private static final Map<String, BetPick> allPicks = new LinkedHashMap<>();
    @Getter
    private static final Map<UUID, Map<String, String>> playerSelections = new HashMap<>();
    @Getter
    private static final Map<String, Map<String, Integer>> pickStats = new HashMap<>();
    @Getter
    private static final Map<UUID, List<PlayerBet>> playerBets = new HashMap<>();
    private static final Map<UUID, Boolean> lockedPlayers = new HashMap<>();
    private static final Set<UUID> lockedFromParlay = new HashSet<>();
    private static final Map<UUID, Map<Integer, Integer>> pendingBetAmounts = new HashMap<>();
    private static final Map<UUID, Map<String, Integer>> straightWagers = new HashMap<>();
    private static final int[] parlayMultipliers = new int[]{2, 3, 7, 15, 31};
    @Getter
    private static boolean globalLocked = false;

    public static void loadGamesFromConfig(FileConfiguration config) {
        gamesMap.clear();
        List<Map<?, ?>> gameList = config.getMapList("games");
        for (Map<?, ?> gameMap : gameList) {
            ZonedDateTime lockTime;
            String gameId = (String) gameMap.get("id");
            String displayName = (String) gameMap.get("displayName");
            String spread = (String) gameMap.get("spread");
            String total = (String) gameMap.get("total");
            String lockString = (String) gameMap.get("lockTimeEastern");
            try {
                LocalDateTime localDT = LocalDateTime.parse(lockString);
                lockTime = localDT.atZone(ZoneId.of("America/New_York"));
            } catch (Exception e) {
                lockTime = ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneId.of("America/New_York"));
            }
            ArrayList<CategoryConfig> categoryConfigs = new ArrayList<>();
            List<Map<?, ?>> yamlCategories = (List<Map<?, ?>>) gameMap.get("categories");
            for (Map<?, ?> catMap : yamlCategories) {
                String catName = (String) catMap.get("name");
                String matName = (String) catMap.get("itemMaterial");
                Material itemMat = Material.matchMaterial(matName);
                ArrayList<PickData> pickDataList = new ArrayList<>();
                List<Map<?, ?>> yamlPicks = (List<Map<?, ?>>) catMap.get("picks");
                for (Map<?, ?> pickMap : yamlPicks) {
                    String question = (String) pickMap.get("question");
                    String leftOption = (String) pickMap.get("leftOption");
                    String leftBlock = (String) pickMap.get("leftBlock");
                    String rightOption = (String) pickMap.get("rightOption");
                    String rightBlock = (String) pickMap.get("rightBlock");
                    int bucks = (Integer) pickMap.get("bucks");
                    String playerName = (String) pickMap.get("playerName");
                    List statList = (List) pickMap.get("statCategories");
                    String correctOption = (String) pickMap.get("correctOption");
                    Object rawLeftObj = pickMap.get("leftOdds");
                    int leftOdds = rawLeftObj instanceof Number ? ((Number) rawLeftObj).intValue() : -110;
                    Object rawRightObj = pickMap.get("rightOdds");
                    int rightOdds = rawRightObj instanceof Number ? ((Number) rawRightObj).intValue() : -110;
                    HashSet<String> statCats = new HashSet<>();
                    if (statList != null) {
                        for (Object o : statList) {
                            statCats.add(o.toString().toLowerCase());
                        }
                    }
                    pickDataList.add(new PickData(question, leftOption, leftBlock, rightOption, rightBlock, bucks, playerName, statCats, correctOption, leftOdds, rightOdds));
                }
                categoryConfigs.add(new CategoryConfig(catName, itemMat, pickDataList));
            }
            gamesMap.put(gameId, new GameConfig(gameId, displayName, spread, total, lockTime, categoryConfigs));
        }
        allPicks.clear();
        pickStats.clear();
        for (GameConfig game : gamesMap.values()) {
            for (CategoryConfig cat : game.categories) {
                for (int i = 0; i < cat.picks.size(); ++i) {
                    PickData pd = cat.picks.get(i);
                    Material leftMat = Material.matchMaterial(pd.leftBlock.toUpperCase());
                    Material rightMat = Material.matchMaterial(pd.rightBlock.toUpperCase());
                    String pickKey = game.gameId + ":" + cat.name + ":" + i;
                    BetPick bp = new BetPick(game.gameId, cat.name, i, pd.question, pd.leftOption, leftMat, pd.rightOption, rightMat, pd.bucks, pd.playerName, pd.statCategories, pd.correctOption, pd.leftOdds, pd.rightOdds);
                    allPicks.put(pickKey, bp);
                    pickStats.putIfAbsent(pickKey, new HashMap<>());
                }
            }
            String spreadKey = game.gameId + ":Spread:0";
            String[] teams = game.displayName.split(" vs ");
            double s = Double.parseDouble(game.spread);
            String fav = s < 0.0 ? teams[0] + " " + game.spread : teams[1] + " +" + game.spread;
            String dog = s < 0.0 ? teams[1] + " +" + game.spread.substring(1) : teams[0] + " -" + game.spread;
            BetPick spreadPick = new BetPick(game.gameId, "Spread", 0, "Spread", fav, Material.PAPER, dog, Material.PAPER, 0, "", Collections.emptySet(), null, -110, -110);
            allPicks.put(spreadKey, spreadPick);
            pickStats.putIfAbsent(spreadKey, new HashMap<>());
            String totalKey = game.gameId + ":Total:0";
            BetPick totalPick = new BetPick(game.gameId, "Total", 0, "Total (" + game.total + ")", "Under", Material.PAPER, "Over", Material.PAPER, 0, "", Collections.emptySet(), null, -110, -110);
            allPicks.put(totalKey, totalPick);
            pickStats.putIfAbsent(totalKey, new HashMap<>());
        }
    }

    public static void openGameSelectionGUI(Player player) {
        GUI gui = new GUI("Select a Game to Bet On", 6, false);
        for (int i = 0; i < 54; ++i) {
            gui.addButton(new ItemButton(i, BettingManager.createFiller(), p -> {
            }));
        }
        ArrayList<GameConfig> games = new ArrayList<>(gamesMap.values());
        int count = games.size();
        if (count == 0) {
            player.sendMessage("§cNo games available right now.");
            gui.openInventory(player);
            return;
        }
        int rowStart = 18;
        int available = 9;
        int offset = (available - count) / 2;
        int slot = rowStart + Math.max(0, offset);
        ZonedDateTime nowEastern = ZonedDateTime.now(ZoneId.of("America/New_York"));
        for (GameConfig game : games) {
            String dog;
            String fav;
            if (slot > rowStart + available - 1) break;
            boolean locked = !nowEastern.isBefore(game.lockTimeEastern);
            String[] teams = game.displayName.split(" vs ");
            double s = Double.parseDouble(game.spread);
            if (s < 0.0) {
                fav = teams[0] + " " + game.spread;
                dog = teams[1] + " +" + game.spread.substring(1);
            } else {
                fav = teams[1] + " +" + game.spread;
                dog = teams[0] + " -" + game.spread;
            }
            String title = locked ? Material.BOOK.name() : Material.WRITABLE_BOOK.name();
            ItemStack icon = new ItemStack(locked ? Material.BOOK : Material.WRITABLE_BOOK);
            icon.editMeta(meta -> {
                String display = game.displayName + (locked ? " §c(Locked)" : "");
                meta.displayName(Component.text(display).color(Colour.partix()).decorate(TextDecoration.BOLD));
                ArrayList<TextComponent> lore = new ArrayList<>();
                lore.add(Component.text("§7Spread: §e" + fav + " / " + dog));
                lore.add(Component.text("§7Total:  §e" + game.total));
                if (locked) {
                    lore.add(Component.text("§cBets closed at “" + game.lockTimeEastern.format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a")) + " EST”"));
                }
                meta.lore(lore);
            });
            int thisSlot = slot;
            if (locked) {
                ItemStack grey = new ItemStack(Material.BOOK);
                grey.editMeta(meta -> meta.displayName(Component.text(game.displayName + " §c(Locked)").decorate(TextDecoration.STRIKETHROUGH)));
                gui.addButton(new ItemButton(thisSlot, grey, p -> p.sendMessage(Component.text("§cBetting for this game is closed."))));
            } else {
                gui.addButton(new ItemButton(thisSlot, icon, p -> BettingManager.openCategorySelectionGUI(p, game.gameId)));
            }
            ++slot;
        }
        ItemStack clearItem = new ItemStack(Material.BARRIER);
        clearItem.editMeta(meta -> meta.displayName(Component.text("§c❎ Clear All Selections").decorate(TextDecoration.BOLD)));
        gui.addButton(new ItemButton(47, clearItem, p -> {
            playerSelections.getOrDefault(player.getUniqueId(), new HashMap<>()).clear();
            player.sendMessage(Component.text("§cAll selections have been cleared."));
            BettingManager.openGameSelectionGUI(player);
        }));
        ItemStack submitButton = new ItemStack(Material.EMERALD);
        submitButton.editMeta(meta -> {
            meta.displayName(Component.text("✅ Submit All Picks").decorate(TextDecoration.BOLD));
            meta.lore(Collections.singletonList(Component.text("§7Click to finalize all straight bets")));
            meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        });
        gui.addButton(new ItemButton(49, submitButton, BettingManager::openFinalizeStraightBetsGUI));
        ItemStack parlayButton = new ItemStack(Material.DIAMOND);
        parlayButton.editMeta(meta -> {
            meta.displayName(Component.text("\ud83d\udcb0 Parlay Picks").decorate(TextDecoration.BOLD));
            meta.lore(Arrays.asList(Component.text("§7Click to combine selections into a Parlay"), Component.text("§7Bigger risk → bigger reward")));
        });
        gui.addButton(new ItemButton(51, parlayButton, BettingManager::openParlayBetGUI));
        gui.openInventory(player);
    }

    public static void openCategorySelectionGUI(Player player, String gameId) {
        ItemStack icon;
        CategoryConfig cat;
        int slotIndex;
        String dogStr;
        String favStr;
        GameConfig game = gamesMap.get(gameId);
        if (game == null) {
            player.sendMessage(Component.text("§cGame not found!"));
            return;
        }
        ZonedDateTime nowEastern = ZonedDateTime.now(ZoneId.of("America/New_York"));
        if (!nowEastern.isBefore(game.lockTimeEastern)) {
            player.sendMessage(Component.text("§cBetting for “" + game.displayName + "” is now closed."));
            return;
        }
        if (BettingManager.isGlobalLocked()) {
            player.sendMessage(Component.text("§cAll picks are locked globally!"));
            return;
        }
        GUI gui = new GUI("Bet: " + game.displayName, 6, false);
        for (int i = 0; i < 54; ++i) {
            gui.addButton(new ItemButton(i, BettingManager.createFiller(), p -> {
            }));
        }
        Map<String, String> selections = playerSelections.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
        String[] teams = game.displayName.split(" vs ");
        double s = Double.parseDouble(game.spread);
        String rawSpread = game.spread.replaceFirst("^[+-]", "");
        if (s < 0.0) {
            favStr = teams[0] + " -" + rawSpread;
            dogStr = teams[1] + " +" + rawSpread;
        } else {
            favStr = teams[1] + " -" + rawSpread;
            dogStr = teams[0] + " +" + rawSpread;
        }
        String pickKeySpread = gameId + ":Spread:0";
        String currentSpreadSelection = selections.get(pickKeySpread);
        ItemStack spreadLeft = new ItemStack(Material.RED_CONCRETE);
        spreadLeft.editMeta(meta -> {
            boolean isFavSelected = favStr.equalsIgnoreCase(currentSpreadSelection);
            String displayName = isFavSelected ? "§a✔ " + favStr + " (-110)" : "§7" + favStr + " (-110)";
            meta.displayName(Component.text(displayName).decorate(TextDecoration.BOLD));
            meta.lore(Arrays.asList(Component.text("§7Spread"), Component.text(isFavSelected ? "§aClick to unselect" : "§7Click to select")));
        });
        gui.addButton(new ItemButton(1, spreadLeft, p -> {
            if (favStr.equalsIgnoreCase(currentSpreadSelection)) {
                selections.remove(pickKeySpread);
                p.sendMessage(Component.text("§cUnselected Spread → " + favStr));
            } else {
                selections.put(pickKeySpread, favStr);
                p.sendMessage(Component.text("§aSelected Spread → " + favStr));
            }
            BettingManager.openCategorySelectionGUI(p, gameId);
        }));
        ItemStack spreadPaper = new ItemStack(Material.PAPER);
        spreadPaper.editMeta(meta -> {
            meta.displayName(Component.text("§6Spread").decorate(TextDecoration.BOLD));
            meta.lore(Collections.singletonList(Component.text("§7Pick who covers")));
        });
        gui.addButton(new ItemButton(2, spreadPaper, p -> {
        }));
        ItemStack spreadRight = new ItemStack(Material.GREEN_CONCRETE);
        spreadRight.editMeta(meta -> {
            boolean isDogSelected = dogStr.equalsIgnoreCase(currentSpreadSelection);
            String displayName = isDogSelected ? "§a✔ " + dogStr + " (-110)" : "§7" + dogStr + " (-110)";
            meta.displayName(Component.text(displayName).decorate(TextDecoration.BOLD));
            meta.lore(Arrays.asList(Component.text("§7Spread"), Component.text(isDogSelected ? "§aClick to unselect" : "§7Click to select")));
        });
        gui.addButton(new ItemButton(3, spreadRight, p -> {
            if (dogStr.equalsIgnoreCase(currentSpreadSelection)) {
                selections.remove(pickKeySpread);
                p.sendMessage(Component.text("§cUnselected Spread → " + dogStr));
            } else {
                selections.put(pickKeySpread, dogStr);
                p.sendMessage(Component.text("§aSelected Spread → " + dogStr));
            }
            BettingManager.openCategorySelectionGUI(p, gameId);
        }));
        gui.addButton(new ItemButton(0, BettingManager.createFiller(), p -> {
        }));
        gui.addButton(new ItemButton(4, BettingManager.createFiller(), p -> {
        }));
        String pickKeyTotal = gameId + ":Total:0";
        String currentTotalSelection = selections.get(pickKeyTotal);
        ItemStack totalLeft = new ItemStack(Material.RED_CONCRETE);
        totalLeft.editMeta(meta -> {
            boolean isUnderSelected = "Under".equalsIgnoreCase(currentTotalSelection);
            String displayName = isUnderSelected ? "§a✔ Under (-110)" : "§7Under (-110)";
            meta.displayName(Component.text(displayName).decorate(TextDecoration.BOLD));
            meta.lore(Arrays.asList(Component.text("§7Total (" + game.total + ")"), Component.text(isUnderSelected ? "§aClick to unselect" : "§7Click to select")));
        });
        gui.addButton(new ItemButton(5, totalLeft, p -> {
            if ("Under".equalsIgnoreCase(currentTotalSelection)) {
                selections.remove(pickKeyTotal);
                p.sendMessage(Component.text("§cUnselected Total → Under"));
            } else {
                selections.put(pickKeyTotal, "Under");
                p.sendMessage(Component.text("§aSelected Total → Under"));
            }
            BettingManager.openCategorySelectionGUI(p, gameId);
        }));
        ItemStack totalPaper = new ItemStack(Material.PAPER);
        totalPaper.editMeta(meta -> {
            meta.displayName(Component.text("§6Total").decorate(TextDecoration.BOLD));
            meta.lore(Collections.singletonList(Component.text("§7Pick Under/Over")));
        });
        gui.addButton(new ItemButton(6, totalPaper, p -> {
        }));
        ItemStack totalRight = new ItemStack(Material.GREEN_CONCRETE);
        totalRight.editMeta(meta -> {
            boolean isOverSelected = "Over".equalsIgnoreCase(currentTotalSelection);
            String displayName = isOverSelected ? "§a✔ Over (-110)" : "§7Over (-110)";
            meta.displayName(Component.text(displayName).decorate(TextDecoration.BOLD));
            meta.lore(Arrays.asList(Component.text("§7Total (" + game.total + ")"), Component.text(isOverSelected ? "§aClick to unselect" : "§7Click to select")));
        });
        gui.addButton(new ItemButton(7, totalRight, p -> {
            if ("Over".equalsIgnoreCase(currentTotalSelection)) {
                selections.remove(pickKeyTotal);
                p.sendMessage(Component.text("§cUnselected Total → Over"));
            } else {
                selections.put(pickKeyTotal, "Over");
                p.sendMessage(Component.text("§aSelected Total → Over"));
            }
            BettingManager.openCategorySelectionGUI(p, gameId);
        }));
        gui.addButton(new ItemButton(8, BettingManager.createFiller(), p -> {
        }));
        for (int sId = 9; sId <= 17; ++sId) {
            gui.addButton(new ItemButton(sId, BettingManager.createFiller(), p -> {
            }));
        }
        List<CategoryConfig> cats = game.categories;
        int totalCats = cats.size();
        gui.addButton(new ItemButton(18, BettingManager.createFiller(), p -> {
        }));
        for (int i = 0; i < 7; ++i) {
            slotIndex = 19 + i;
            if (i < totalCats) {
                cat = cats.get(i);
                icon = new ItemStack(cat.itemMaterial != null ? cat.itemMaterial : Material.GRAY_WOOL);
                CategoryConfig finalCat = cat;
                icon.editMeta(meta -> {
                    meta.displayName(Component.text(finalCat.name).color(Colour.partix()).decorate(TextDecoration.BOLD));
                    meta.lore(Collections.singletonList(Component.text("§7Click to view picks")));
                });
                gui.addButton(new ItemButton(slotIndex, icon, p -> BettingManager.openPickSelectionGUI(p, gameId, finalCat.name)));
                continue;
            }
            gui.addButton(new ItemButton(slotIndex, BettingManager.createFiller(), p -> {
            }));
        }
        gui.addButton(new ItemButton(26, BettingManager.createFiller(), p -> {
        }));
        gui.addButton(new ItemButton(27, BettingManager.createFiller(), p -> {
        }));
        for (int j = 7; j < 14; ++j) {
            slotIndex = 28 + (j - 7);
            if (j < totalCats) {
                cat = cats.get(j);
                icon = new ItemStack(cat.itemMaterial != null ? cat.itemMaterial : Material.GRAY_WOOL);
                CategoryConfig finalCat1 = cat;
                icon.editMeta(meta -> {
                    meta.displayName(Component.text(finalCat1.name).color(Colour.partix()).decorate(TextDecoration.BOLD));
                    meta.lore(Collections.singletonList(Component.text("§7Click to view picks")));
                });
                gui.addButton(new ItemButton(slotIndex, icon, p -> BettingManager.openPickSelectionGUI(p, gameId, finalCat1.name)));
                continue;
            }
            gui.addButton(new ItemButton(slotIndex, BettingManager.createFiller(), p -> {
            }));
        }
        gui.addButton(new ItemButton(35, BettingManager.createFiller(), p -> {
        }));
        ItemStack backArrow = new ItemStack(Material.ARROW);
        backArrow.editMeta(meta -> meta.displayName(Component.text("§c◀ Back to Games").decorate(TextDecoration.BOLD)));
        gui.addButton(new ItemButton(45, backArrow, BettingManager::openGameSelectionGUI));
        ItemStack clearItem = new ItemStack(Material.BARRIER);
        clearItem.editMeta(meta -> meta.displayName(Component.text("§c❎ Clear All Selections").decorate(TextDecoration.BOLD)));
        gui.addButton(new ItemButton(47, clearItem, p -> {
            playerSelections.getOrDefault(player.getUniqueId(), new HashMap<>()).clear();
            player.sendMessage(Component.text("§cAll selections have been cleared."));
            BettingManager.openCategorySelectionGUI(player, gameId);
        }));
        ItemStack submitButton = new ItemStack(Material.EMERALD);
        submitButton.editMeta(meta -> {
            meta.displayName(Component.text("✅ Submit All Picks").decorate(TextDecoration.BOLD));
            meta.lore(Collections.singletonList(Component.text("§7Click to finalize all straight bets")));
            meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        });
        gui.addButton(new ItemButton(49, submitButton, BettingManager::openFinalizeStraightBetsGUI));
        ItemStack parlayButton = new ItemStack(Material.DIAMOND);
        parlayButton.editMeta(meta -> {
            meta.displayName(Component.text("\ud83d\udcb0 Parlay Picks").decorate(TextDecoration.BOLD));
            meta.lore(Arrays.asList(Component.text("§7Click to combine selections into a Parlay"), Component.text("§7Bigger risk → bigger reward")));
        });
        gui.addButton(new ItemButton(51, parlayButton, BettingManager::openParlayBetGUI));
        gui.openInventory(player);
    }

    public static void openPickSelectionGUI(Player player, String gameId, String categoryName) {
        if (BettingManager.isGlobalLocked()) {
            player.sendMessage(Component.text("§cPicks are locked globally!"));
            return;
        }
        GameConfig game = gamesMap.get(gameId);
        if (game == null) {
            player.sendMessage(Component.text("§cGame not found!"));
            return;
        }
        CategoryConfig chosenCategory = null;
        for (CategoryConfig cat : game.categories) {
            if (!cat.name.equalsIgnoreCase(categoryName)) continue;
            chosenCategory = cat;
            break;
        }
        if (chosenCategory == null) {
            player.sendMessage(Component.text("§cCategory not found!"));
            return;
        }
        GUI gui = new GUI("Picks: " + game.displayName + " → " + chosenCategory.name, 6, false);
        for (int i = 0; i < 54; ++i) {
            gui.addButton(new ItemButton(i, BettingManager.createFiller(), p -> {
            }));
        }
        Map<String, String> selections = playerSelections.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
        ArrayList<BetPick> pickList = new ArrayList<>();
        for (int i = 0; i < chosenCategory.picks.size(); ++i) {
            String key = gameId + ":" + chosenCategory.name + ":" + i;
            BetPick bp = allPicks.get(key);
            if (bp == null) continue;
            pickList.add(bp);
        }
        int totalPicks = pickList.size();
        int rowsNeeded = (totalPicks + 1) / 2;
        for (int row = 0; row < rowsNeeded; ++row) {
            int idx;
            int base = row * 9;
            gui.addButton(new ItemButton(base, BettingManager.createFiller(), p -> {
            }));
            gui.addButton(new ItemButton(base + 4, BettingManager.createFiller(), p -> {
            }));
            gui.addButton(new ItemButton(base + 8, BettingManager.createFiller(), p -> {
            }));
            for (int side = 0; side < 2 && (idx = row * 2 + side) < totalPicks; ++side) {
                BetPick bp = pickList.get(idx);
                String pickKey = bp.getPickKey();
                boolean hasSel = selections.containsKey(pickKey);
                String chosen = selections.get(pickKey);
                int colOffset = side == 0 ? 1 : 5;
                String leftOddsStr = bp.leftOdds > 0 ? "+" + bp.leftOdds : String.valueOf(bp.leftOdds);
                ItemStack leftIcon = new ItemStack(bp.leftBlock);
                leftIcon.editMeta(arg_0 -> BettingManager.openPickSelectionGUI52(hasSel, chosen, bp, leftOddsStr, arg_0));
                int leftSlot = base + colOffset;
                gui.addButton(new ItemButton(leftSlot, leftIcon, p -> {
                    if (hasSel && chosen.equalsIgnoreCase(bp.leftOption)) {
                        selections.remove(pickKey);
                        p.sendMessage(Component.text("§cUnselected “" + bp.question + " → " + bp.leftOption + "”"));
                        pickStats.get(pickKey).merge(bp.leftOption, -1, Integer::sum);
                    } else {
                        selections.put(pickKey, bp.leftOption);
                        p.sendMessage(Component.text("§aSelected “" + bp.question + " → " + bp.leftOption + "”"));
                        pickStats.computeIfAbsent(pickKey, k -> new HashMap<>()).merge(bp.leftOption, 1, Integer::sum);
                    }
                    BettingManager.openPickSelectionGUI(player, gameId, categoryName);
                }));
                ItemStack q = new ItemStack(Material.PAPER);
                q.editMeta(meta -> {
                    meta.displayName(Component.text("✎ " + bp.question).decorate(TextDecoration.BOLD));
                    meta.lore(Arrays.asList(Component.text("§7Bet Cost: " + bp.bucks + " MBABucks"), Component.text(hasSel ? "§aSelected: “" + chosen + "”" : "§7Click either side to pick")));
                });
                gui.addButton(new ItemButton(base + colOffset + 1, q, p -> {
                }));
                String rightOddsStr = bp.rightOdds > 0 ? "+" + bp.rightOdds : String.valueOf(bp.rightOdds);
                ItemStack rightIcon = new ItemStack(bp.rightBlock);
                rightIcon.editMeta(arg_0 -> BettingManager.openPickSelectionGUI57(hasSel, chosen, bp, rightOddsStr, arg_0));
                gui.addButton(new ItemButton(base + colOffset + 2, rightIcon, p -> {
                    if (hasSel && chosen.equalsIgnoreCase(bp.rightOption)) {
                        selections.remove(pickKey);
                        p.sendMessage(Component.text("§cUnselected “" + bp.question + " → " + bp.rightOption + "”"));
                        pickStats.get(pickKey).merge(bp.rightOption, -1, Integer::sum);
                    } else {
                        selections.put(pickKey, bp.rightOption);
                        p.sendMessage(Component.text("§aSelected “" + bp.question + " → " + bp.rightOption + "”"));
                        pickStats.computeIfAbsent(pickKey, k -> new HashMap<>()).merge(bp.rightOption, 1, Integer::sum);
                    }
                    BettingManager.openPickSelectionGUI(player, gameId, categoryName);
                }));
            }
        }
        ItemStack back = new ItemStack(Material.ARROW);
        back.editMeta(meta -> meta.displayName(Component.text("§c◀ Back to Categories").decorate(TextDecoration.BOLD)));
        gui.addButton(new ItemButton(45, back, p -> BettingManager.openCategorySelectionGUI(p, gameId)));
        gui.openInventory(player);
    }

    public static void openFinalizeStraightBetsGUI(Player player) {
        UUID uuid = player.getUniqueId();
        Map<String, String> selections = playerSelections.getOrDefault(uuid, new HashMap<>());
        if (selections.isEmpty()) {
            player.sendMessage(Component.text("§cNo picks selected to finalize."));
            return;
        }
        ArrayList<BetPick> chosenPicks = new ArrayList<>();
        for (Object pickKey : selections.keySet()) {
            BetPick bp = allPicks.get(pickKey);
            if (bp == null) continue;
            chosenPicks.add(bp);
        }
        if (chosenPicks.isEmpty()) {
            player.sendMessage(Component.text("§cNo valid straight-bet picks found."));
            return;
        }
        Map<String, Integer> wagers = straightWagers.computeIfAbsent(uuid, k -> new HashMap<>());
        for (BetPick bp : chosenPicks) {
            wagers.putIfAbsent(bp.getPickKey(), bp.bucks);
        }
        int total = chosenPicks.size();
        int rowsNeeded = (total + 1) / 2;
        int height = rowsNeeded + 2;
        int totalSlots = height * 9;
        GUI gui = new GUI("Finalize Straight Bets", height, false);
        for (int i = 0; i < totalSlots; ++i) {
            gui.addButton(new ItemButton(i, BettingManager.createFiller(), p -> {
            }));
        }
        for (int idx = 0; idx < total; ++idx) {
            BetPick bp = chosenPicks.get(idx);
            String pickKey = bp.getPickKey();
            String chosenSide = selections.get(pickKey);
            int odds = chosenSide.equalsIgnoreCase(bp.leftOption) ? bp.leftOdds : bp.rightOdds;
            int currentWager = wagers.get(pickKey);
            GameConfig game = gamesMap.get(bp.gameId);
            String displayGame = game != null ? game.displayName : bp.gameId;
            String displayQuestion = displayGame + ": " + bp.question + " → " + chosenSide;
            int rowIndex = idx / 2 + 1;
            int sideInRow = idx % 2;
            int baseColumn = sideInRow == 0 ? 1 : 5;
            int baseSlot = rowIndex * 9 + baseColumn;
            ItemStack minus = new ItemStack(Material.RED_CONCRETE);
            minus.editMeta(meta -> meta.displayName(Component.text("➖").decorate(TextDecoration.BOLD)));
            gui.addButton(new ItemButton(baseSlot, minus, p -> {
                int amt = wagers.get(pickKey);
                int newAmt = Math.max(bp.bucks, amt - 50);
                wagers.put(pickKey, newAmt);
                BettingManager.openFinalizeStraightBetsGUI(player);
            }));
            ItemStack info = new ItemStack(Material.PAPER);
            info.editMeta(meta -> {
                meta.displayName(Component.text(displayQuestion).decorate(TextDecoration.BOLD));
                meta.lore(Arrays.asList(Component.text("§7Odds: " + (odds > 0 ? "+" + odds : "" + odds)), Component.text("§7Wager: " + currentWager + " MBABucks"), Component.text("§7Click ➖/➕ to adjust by 50")));
            });
            gui.addButton(new ItemButton(baseSlot + 1, info, p -> {
            }));
            ItemStack plus = new ItemStack(Material.GREEN_CONCRETE);
            plus.editMeta(meta -> meta.displayName(Component.text("➕").decorate(TextDecoration.BOLD)));
            gui.addButton(new ItemButton(baseSlot + 2, plus, p -> {
                PlayerDb.get(uuid, PlayerDb.Stat.MBA_BUCKS).thenAccept(currentBucks -> {
                    int amt = wagers.get(pickKey);
                    int newAmt = amt + 50;
                    if (newAmt > currentBucks) {
                        p.sendMessage(Component.text("§cInsufficient MBABucks to increase further."));
                    } else {
                        wagers.put(pickKey, newAmt);
                    }
                    BettingManager.openFinalizeStraightBetsGUI(player);
                });
            }));
        }
        int confirmSlot = (height - 1) * 9 + 4;
        ItemStack confirm = new ItemStack(Material.EMERALD);
        confirm.editMeta(meta -> {
            meta.displayName(Component.text("✅ Place Straight Bets").decorate(TextDecoration.BOLD));
            meta.lore(Collections.singletonList(Component.text("§7Click to finalize your straight wagers")));
            meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        });
        gui.addButton(new ItemButton(confirmSlot, confirm, p -> {
            PlayerDb.get(uuid, PlayerDb.Stat.MBA_BUCKS).thenAccept(currentBucks -> {
                int totalWager = 0;
                for (BetPick bp : chosenPicks) {
                    totalWager += wagers.get(bp.getPickKey());
                }

                if (currentBucks < totalWager) {
                    p.sendMessage(Component.text("§cInsufficient MBABucks! Total wager: " + totalWager + ", you have: " + currentBucks));
                    return;
                }
                PlayerDb.remove(uuid, PlayerDb.Stat.MBA_BUCKS, totalWager);
                List<PlayerBet> bets = playerBets.computeIfAbsent(uuid, k -> new ArrayList<>());
                for (BetPick bp : chosenPicks) {
                    String key = bp.getPickKey();
                    String chosenSide2 = selections.get(key);
                    GameConfig game = gamesMap.get(bp.gameId);
                    String displayGame = game != null ? game.displayName : bp.gameId;
                    int odds2 = chosenSide2.equalsIgnoreCase(bp.leftOption) ? bp.leftOdds : bp.rightOdds;
                    int wager2 = wagers.get(key);
                    ParlayPick singleLeg = new ParlayPick(bp.pickIndex, displayGame + ": " + bp.question, chosenSide2, bp.gameId, bp.categoryName, bp.statCategories, odds2, wager2);
                    bets.add(new PlayerBet(uuid, wager2, false, Collections.singletonList(singleLeg)));
                }
                playerSelections.remove(uuid);
                straightWagers.remove(uuid);
                p.sendMessage(Component.text("§aStraight bets placed! Good luck!"));
                p.closeInventory();
            });
        }));
        gui.openInventory(player);
    }

    public static void openParlayBetGUI(Player player) {
        UUID uuid = player.getUniqueId();
        Map<String, String> selections = playerSelections.getOrDefault(uuid, new HashMap<>());
        if (selections.size() < 2) {
            player.sendMessage(Component.text("§cYou need at least 2 picks to start a Parlay."));
            return;
        }
        pendingBetAmounts.computeIfAbsent(uuid, k -> new HashMap<>()).put(-1, 100);
        BettingManager.updateParlayGUI(player);
    }

    public static void updateParlayGUI(Player player) {
        UUID uuid = player.getUniqueId();
        int betAmount = pendingBetAmounts.getOrDefault(uuid, new HashMap<>()).getOrDefault(-1, 100);
        Map<String, String> selections = playerSelections.getOrDefault(uuid, new HashMap<>());
        ArrayList<BetPick> chosenPicks = new ArrayList<>();
        for (String pickKey : selections.keySet()) {
            BetPick bp = allPicks.get(pickKey);
            if (bp == null) continue;
            chosenPicks.add(bp);
        }
        int combinedOdds = BettingManager.calculateParlayOdds(chosenPicks, selections);
        int potentialPayout = BettingManager.americanToPayout(betAmount, combinedOdds);
        TextComponent title = Component.text("\ud83d\udcb0 Parlay Bet - Risk: " + betAmount + " Win: " + potentialPayout + " (" + (combinedOdds >= 0 ? "+" : "") + combinedOdds + ")");
        GUI parlayGui = new GUI(title, 3, false);
        parlayGui.addButton(new ItemButton(11, BettingManager.createAmountButton("➖ Decrease Bet (-50)", Material.RED_CONCRETE), p -> BettingManager.changeParlayBetAmount(player, -50)));
        parlayGui.addButton(new ItemButton(15, BettingManager.createAmountButton("➕ Increase Bet (+50)", Material.GREEN_CONCRETE), p -> BettingManager.changeParlayBetAmount(player, 50)));
        parlayGui.addButton(new ItemButton(13, BettingManager.createInfoButton(player, betAmount, potentialPayout), p -> {
        }));
        parlayGui.addButton(new ItemButton(22, BettingManager.createConfirmButton(), p -> BettingManager.confirmParlayBet(p, betAmount, combinedOdds)));
        parlayGui.openInventory(player);
    }

    private static ItemStack createAmountButton(String name, Material mat) {
        ItemStack item = new ItemStack(mat);
        item.editMeta(meta -> meta.displayName(Component.text(name)));
        return item;
    }

    private static ItemStack createInfoButton(Player player, int amount, int payout) {
        UUID uuid = player.getUniqueId();
        Map<String, String> selections = playerSelections.getOrDefault(uuid, new HashMap<>());
        ItemStack item = new ItemStack(Material.PAPER);
        item.editMeta(meta -> {
            meta.displayName(Component.text("\ud83d\udcb0 Parlay Info").color(TextColor.color(16766720)));
            ArrayList<Component> lore = new ArrayList<>();
            lore.add(Component.text("Current Bet: " + amount + " MBABucks").color(TextColor.color(0xFFFFFF)));
            lore.add(Component.text("Potential Payout: " + payout + " MBABucks").color(TextColor.color(0xFFFFFF)));
            lore.add(Component.text(" "));
            lore.add(Component.text("\ud83d\udccb Current Parlay Picks:").color(TextColor.color(65535)));
            if (selections.isEmpty()) {
                lore.add(Component.text(" - None selected").color(TextColor.color(0xAAAAAA)));
            } else {
                for (Map.Entry entry : selections.entrySet()) {
                    String displayGame;
                    GameConfig g;
                    String gId;
                    String[] parts;
                    String pickKey = (String) entry.getKey();
                    String chosenSide = (String) entry.getValue();
                    if (allPicks.containsKey(pickKey)) {
                        BetPick bp = allPicks.get(pickKey);
                        GameConfig game = gamesMap.get(bp.gameId);
                        String displayGame2 = game != null ? game.displayName : bp.gameId;
                        lore.add(Component.text(" - " + displayGame2 + ": " + bp.question + " → " + chosenSide).color(TextColor.color(0xFFFFFF)));
                        continue;
                    }
                    if (pickKey.endsWith(":Spread:0")) {
                        parts = pickKey.split(":");
                        gId = parts[0];
                        g = gamesMap.get(gId);
                        displayGame = g != null ? g.displayName : gId;
                        lore.add(Component.text(" - " + displayGame + ": Spread → " + chosenSide).color(TextColor.color(0xFFFFFF)));
                        continue;
                    }
                    if (pickKey.endsWith(":Total:0")) {
                        parts = pickKey.split(":");
                        gId = parts[0];
                        g = gamesMap.get(gId);
                        displayGame = g != null ? g.displayName : gId;
                        lore.add(Component.text(" - " + displayGame + ": Total → " + chosenSide).color(TextColor.color(0xFFFFFF)));
                        continue;
                    }
                    lore.add(Component.text(" - " + pickKey + ": " + chosenSide).color(TextColor.color(0xFFFFFF)));
                }
            }
            meta.lore(lore);
        });
        return item;
    }

    public static void startLockMonitor(Partix plugin) {
        new BukkitRunnable() {

            public void run() {
                boolean globallyLocked = BettingManager.isGlobalLocked();
                ZonedDateTime nowEastern = ZonedDateTime.now(ZoneId.of("America/New_York"));
                for (Player p : Bukkit.getOnlinePlayers()) {
                    String title;
                    InventoryView invView = p.getOpenInventory();
                    if (invView == null || (title = ChatColor.stripColor(invView.getTitle())) == null || title.isEmpty())
                        continue;
                    if (title.equalsIgnoreCase("Select a Game to Bet On")) {
                        if (!globallyLocked) continue;
                        p.closeInventory();
                        p.sendMessage(Component.text("§cPicks have just been locked. Closing menu."));
                        continue;
                    }
                    if (title.startsWith("Bet: ")) {
                        if (globallyLocked) {
                            p.closeInventory();
                            p.sendMessage(Component.text("§cAll picks were locked. Closing menu."));
                            continue;
                        }
                        String dispName = title.substring(5);
                        GameConfig game = null;
                        for (GameConfig g : gamesMap.values()) {
                            if (!g.displayName.equalsIgnoreCase(dispName)) continue;
                            game = g;
                            break;
                        }
                        if (game != null && nowEastern.isBefore(game.lockTimeEastern)) continue;
                        p.closeInventory();
                        p.sendMessage(Component.text("§cBetting for \"" + dispName + "\" has closed. Closing menu."));
                        continue;
                    }
                    if (!title.startsWith("Picks: ")) continue;
                    if (globallyLocked) {
                        p.closeInventory();
                        p.sendMessage(Component.text("§cAll picks have just been locked. Closing menu."));
                        continue;
                    }
                    String remainder = title.substring(7);
                    String[] parts = remainder.split("→");
                    if (parts.length < 1) continue;
                    String dispName = parts[0].trim();
                    GameConfig game = null;
                    for (GameConfig g : gamesMap.values()) {
                        if (!g.displayName.equalsIgnoreCase(dispName)) continue;
                        game = g;
                        break;
                    }
                    if (game != null && nowEastern.isBefore(game.lockTimeEastern)) continue;
                    p.closeInventory();
                    p.sendMessage(Component.text("§cBetting for \"" + dispName + "\" has closed. Closing menu."));
                }
            }
        }.runTaskTimer(plugin, 60L, 60L);
    }

    private static ItemStack createConfirmButton() {
        ItemStack item = new ItemStack(Material.EMERALD);
        item.editMeta(meta -> meta.displayName(Component.text("✅ Confirm Parlay")));
        return item;
    }

    private static void changeParlayBetAmount(Player player, int delta) {
        UUID uuid = player.getUniqueId();
        Map<Integer, Integer> bets = pendingBetAmounts.computeIfAbsent(uuid, k -> new HashMap<>());
        int current = bets.getOrDefault(-1, 100);
        int newAmount = Math.max(10, current + delta);
        bets.put(-1, newAmount);
        BettingManager.updateParlayGUI(player);
    }

    private static void confirmParlayBet(Player player, int betAmount, int combinedOdds) {
        UUID uuid = player.getUniqueId();
        Map<String, String> selections = playerSelections.get(uuid);
        if (selections == null || selections.size() < 2) {
            player.sendMessage(Component.text("§cYou must have at least 2 picks to parlay."));
            return;
        }
        PlayerDb.get(uuid, PlayerDb.Stat.MBA_BUCKS).thenAccept(currentBucks -> {

            if (currentBucks < betAmount) {
                player.sendMessage(Component.text("§cYou do not have enough MBABucks."));
                player.closeInventory();
                return;
            }
            PlayerDb.remove(uuid, PlayerDb.Stat.MBA_BUCKS, betAmount);
            ArrayList<ParlayPick> parlayPicks = new ArrayList<>();
            for (String pickKey : selections.keySet()) {
                BetPick bp = allPicks.get(pickKey);
                if (bp == null) continue;
                GameConfig game = gamesMap.get(bp.gameId);
                String displayGame = game != null ? game.displayName : bp.gameId;
                String side = selections.get(pickKey);
                int odds = side.equalsIgnoreCase(bp.leftOption) ? bp.leftOdds : bp.rightOdds;
                String prefixedQuestion = displayGame + ": " + bp.question;
                parlayPicks.add(new ParlayPick(bp.pickIndex, prefixedQuestion, side, bp.gameId, bp.categoryName, bp.statCategories, odds));
            }
            if (parlayPicks.size() < 2) {
                player.sendMessage(Component.text("§cYou need at least 2 picks for a parlay."));
                return;
            }
            if (BettingManager.hasOverlappingStats(parlayPicks)) {
                player.sendMessage(Component.text("§cYou cannot bet on overlapping stats for the same player!"));
                return;
            }
            List<PlayerBet> bets = playerBets.computeIfAbsent(uuid, k -> new ArrayList<>());
            bets.add(new PlayerBet(uuid, betAmount, true, parlayPicks, combinedOdds));
            selections.clear();
            player.sendMessage(Component.text("§aParlay placed for " + betAmount + " MBABucks! Potential Win: " + BettingManager.americanToPayout(betAmount, combinedOdds) + " MBABucks."));
            player.closeInventory();
        });
    }

    public static boolean hasOverlappingStats(List<ParlayPick> picksList) {
        HashMap<String, Set> playerStats = new HashMap<>();
        for (ParlayPick pp : picksList) {
            String pName = pp.playerName;
            Set<String> stats = pp.statCategories;
            if (pName == null || stats == null) continue;
            Set existing = playerStats.computeIfAbsent(pName, k -> new HashSet());
            for (String stat : stats) {
                if (!existing.contains(stat)) continue;
                return true;
            }
            existing.addAll(stats);
        }
        return false;
    }

    private static int calculateParlayOdds(List<BetPick> picks, Map<String, String> selections) {
        ArrayList<List<BetPick>> grouped = new ArrayList<>();
        HashMap<String, List> byPlayer = new HashMap<>();
        for (BetPick bp2 : picks) {
            String name = bp2.playerName;
            if (name == null || name.isEmpty()) {
                grouped.add(Collections.singletonList(bp2));
                continue;
            }
            byPlayer.computeIfAbsent(name, k -> new ArrayList()).add(bp2);
        }
        for (Object lst : byPlayer.values()) {
            grouped.add((List) lst);
        }
        ArrayList<BetPick> effective = new ArrayList<>();
        for (List<BetPick> grp : grouped) {
            if (grp.size() <= 1) {
                effective.addAll(grp);
                continue;
            }
            grp.sort(Comparator.comparingDouble(bp -> Math.abs(BettingManager.getDecimalOdds(bp, selections))));
            effective.add(grp.getFirst());
        }
        double product = 1.0;
        for (BetPick bp3 : effective) {
            double dec = BettingManager.getDecimalOdds(bp3, selections);
            product *= dec;
        }
        return BettingManager.decimalToAmerican(product);
    }

    private static double getDecimalOdds(BetPick bp, Map<String, String> selections) {
        int ao;
        String chosen = selections.get(bp.getPickKey());
        int n = ao = chosen.equalsIgnoreCase(bp.leftOption) ? bp.leftOdds : bp.rightOdds;
        if (ao > 0) {
            return 1.0 + (double) ao / 100.0;
        }
        return 1.0 + 100.0 / (double) (-ao);
    }

    private static int decimalToAmerican(double dec) {
        if (dec >= 2.0) {
            return (int) Math.round((dec - 1.0) * 100.0);
        }
        return (int) Math.round(-100.0 / (dec - 1.0));
    }

    private static int americanToPayout(int wager, int ao) {
        if (ao == 0) {
            return wager;
        }
        if (ao > 0) {
            return wager + wager * ao / 100;
        }
        return wager + wager * 100 / -ao;
    }

    private static ItemStack createFiller() {
        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        pane.editMeta(meta -> meta.displayName(Component.text(" ")));
        return pane;
    }

    public static List<String> getTop5OptionsForPick(String pickKey) {
        Map<String, Integer> counts = pickStats.getOrDefault(pickKey, new HashMap<>());
        return counts.entrySet().stream().sorted((a, b) -> Integer.compare(b.getValue(), a.getValue())).limit(5L).map(Map.Entry::getKey).toList();
    }

    private static void selectOption(Player player, String pickKey, String chosenSide) {
        UUID uuid = player.getUniqueId();
        if (lockedPlayers.getOrDefault(uuid, false)) {
            player.sendMessage(Component.text("❌ You already submitted picks!"));
            return;
        }
        Map<String, String> selections = playerSelections.computeIfAbsent(uuid, k -> new HashMap<>());
        if (selections.containsKey(pickKey)) {
            String oldSide = (String) selections.get(pickKey);
            Map<String, Integer> sideMap = pickStats.getOrDefault(pickKey, new HashMap<>());
            sideMap.put(oldSide, sideMap.getOrDefault(oldSide, 1) - 1);
        }
        selections.put(pickKey, chosenSide);
        pickStats.computeIfAbsent(pickKey, k -> new HashMap<>()).put(chosenSide, pickStats.get(pickKey).getOrDefault(chosenSide, 0) + 1);
        BetPick bp = allPicks.get(pickKey);
        if (bp != null) {
            player.sendMessage(Component.text("✅ You selected: " + chosenSide + " for " + bp.question));
        }
        String[] parts = pickKey.split(":");
        String gId = parts[0];
        String categoryName = parts[1];
        BettingManager.openPickSelectionGUI(player, gId, categoryName);
    }

    public static void lockPlayerFromBets(UUID uuid) {
        lockedPlayers.put(uuid, true);
        lockedFromParlay.add(uuid);
    }

    public static void lockAllPicksGlobally() {
        globalLocked = true;
    }

    public static void unlockAllPicksGlobally() {
        globalLocked = false;
    }

    public static void setPicksLockedForUser(UUID uuid, boolean locked) {
        lockedPlayers.put(uuid, locked);
    }

    public static BetPick getPickByKey(String pickKey) {
        return allPicks.get(pickKey);
    }

    public static void setPickCorrectSide(String pickKey, String side) {
        BetPick bp = allPicks.get(pickKey);
        if (bp != null) {
            bp.correctSide = side.toLowerCase();
        }
    }

    public static void endCurrentPicks(CommandSender sender, String gameId) {
        GameConfig game = gamesMap.get(gameId);
        if (game == null) {
            sender.sendMessage(Component.text("§cGame \"" + gameId + "\" not found."));
            return;
        }
        ArrayList<String> missing = new ArrayList<>();
        for (CategoryConfig categoryConfig : game.categories) {
            for (int i = 0; i < categoryConfig.picks.size(); ++i) {
                String pickKey = gameId + ":" + categoryConfig.name + ":" + i;
                BetPick bp = allPicks.get(pickKey);
                if (bp == null || bp.correctOption != null || bp.correctSide != null) continue;
                missing.add(pickKey + " (“" + bp.question + "”)");
            }
        }
        if (!missing.isEmpty()) {
            sender.sendMessage(Component.text("§cCannot finalize \"" + gameId + "\" because these picks are missing a correct answer:"));
            for (String string : missing) {
                sender.sendMessage(Component.text("  §e• " + string));
            }
            sender.sendMessage(Component.text("§cPlease set a correctOption in config.yml or use /betting correctpick."));
            return;
        }
        HashMap<UUID, Integer> roundChange = new HashMap<>();
        for (Map.Entry<UUID, Map<String, String>> entry : playerSelections.entrySet()) {
            UUID uuid2 = entry.getKey();
            Map<String, String> picksMap = entry.getValue();
            if (playerBets.containsKey(uuid2)) continue;
            for (Map.Entry<String, String> pickEntry : picksMap.entrySet()) {
                String winner;
                BetPick bp;
                String pickKey = pickEntry.getKey();
                if (!pickKey.startsWith(gameId + ":") || (bp = allPicks.get(pickKey)) == null) continue;
                String string = winner = bp.correctSide != null ? bp.correctSide : bp.correctOption;
                if ("void".equalsIgnoreCase(winner)) {
                    PlayerDb.add(uuid2, PlayerDb.Stat.MBA_BUCKS, bp.bucks);
                    continue;
                }
                String playerChoice = pickEntry.getValue();
                if (!playerChoice.equalsIgnoreCase(winner)) continue;
                PlayerDb.add(uuid2, PlayerDb.Stat.MBA_BUCKS, bp.bucks * 2);
                roundChange.put(uuid2, roundChange.getOrDefault(uuid2, 0) + bp.bucks);
            }
        }
        for (Map.Entry<UUID, List<PlayerBet>> pEntry : playerBets.entrySet()) {
            UUID uuid2 = pEntry.getKey();
            for (PlayerBet bet : pEntry.getValue()) {
                if (!bet.parlayed) continue;
                ArrayList<ParlayPick> voidLegs = new ArrayList<>();
                ArrayList<ParlayPick> liveLegs = new ArrayList<>();
                for (ParlayPick leg : bet.picks) {
                    String win;
                    String pk = leg.gameId + ":" + leg.categoryName + ":" + leg.pickIndex;
                    BetPick orig = allPicks.get(pk);
                    String string = win = orig.correctSide != null ? orig.correctSide : orig.correctOption;
                    if ("void".equalsIgnoreCase(win)) {
                        voidLegs.add(leg);
                        continue;
                    }
                    liveLegs.add(leg);
                }
                if (liveLegs.isEmpty()) {
                    PlayerDb.add(uuid2, PlayerDb.Stat.MBA_BUCKS, bet.totalBet);
                    continue;
                }
                boolean allCorrect = true;
                for (ParlayPick leg : liveLegs) {
                    String pk = leg.gameId + ":" + leg.categoryName + ":" + leg.pickIndex;
                    BetPick orig = allPicks.get(pk);
                    String win = orig.correctSide != null ? orig.correctSide : orig.correctOption;
                    if (leg.chosenSide.equalsIgnoreCase(win)) continue;
                    allCorrect = false;
                    break;
                }
                if (!allCorrect) continue;
                double product = 1.0;
                for (ParlayPick leg : liveLegs) {
                    int ao = leg.odds;
                    double dec = ao > 0 ? 1.0 + (double) ao / 100.0 : 1.0 + 100.0 / (double) (-ao);
                    product *= dec;
                }
                int newAmerican = product >= 2.0 ? (int) Math.round((product - 1.0) * 100.0) : (int) Math.round(-100.0 / (product - 1.0));
                int payout = BettingManager.americanToPayout(bet.totalBet, newAmerican);
                PlayerDb.add(uuid2, PlayerDb.Stat.MBA_BUCKS, payout);
                roundChange.put(uuid2, roundChange.getOrDefault(uuid2, 0) + (payout - bet.totalBet));
            }
        }
        ArrayList<UUID> arrayList = new ArrayList<>(roundChange.keySet());
        arrayList.sort((a, b) -> Integer.compare(roundChange.get(b), roundChange.get(a)));
        if (!arrayList.isEmpty()) {
            Bukkit.broadcast(Component.text("§6§l— Winners for “" + game.displayName + "” —"));
            int rank = 0;
            for (UUID pId : arrayList) {
                int gain = roundChange.get(pId);
                if (gain <= 0) continue;
                if (++rank <= 10) {
                    String name = Bukkit.getOfflinePlayer(pId).getName();
                    Bukkit.broadcast(Component.text("§e#" + rank + " " + name + " → + " + gain + " MBABucks"));
                    continue;
                }
                break;
            }
        } else {
            Bukkit.broadcast(Component.text("§cNo winners for “" + game.displayName + "”"));
        }
        gamesMap.remove(gameId);
        allPicks.keySet().removeIf(k -> k.startsWith(gameId + ":"));
        pickStats.keySet().removeIf(k -> k.startsWith(gameId + ":"));
        playerSelections.values().forEach(m -> m.keySet().removeIf(k -> k.startsWith(gameId + ":")));
        playerBets.values().forEach(list -> list.removeIf(pb -> pb.picks.stream().anyMatch(pp -> pp.gameId.equals(gameId))));
        lockedPlayers.keySet().removeIf(uuid -> playerBets.getOrDefault(uuid, Collections.emptyList()).isEmpty());
        Partix plugin = Partix.getInstance();
        FileConfiguration cfg = plugin.getConfig();
        List<Map<?, ?>> gamesSection = cfg.getMapList("games");
        gamesSection.removeIf(gm -> gameId.equals(String.valueOf(gm.get("id"))));
        cfg.set("games", gamesSection);
        plugin.saveConfig();
        sender.sendMessage(Component.text("§aGame \"" + game.displayName + "\" finalized, payouts issued, and removed from config."));
    }

    private static /* synthetic */ void openPickSelectionGUI57(boolean hasSel, String chosen, BetPick bp, String rightOddsStr, ItemMeta meta) {
        String name = hasSel && chosen.equalsIgnoreCase(bp.rightOption) ? "§a✔ " + bp.rightOption + " (" + rightOddsStr + ")" : "§7" + bp.rightOption + " (" + rightOddsStr + ")";
        meta.displayName(Component.text(name).decorate(TextDecoration.BOLD));
        meta.lore(Arrays.asList(Component.text("§7" + bp.question), Component.text(hasSel && chosen.equalsIgnoreCase(bp.rightOption) ? "§aClick to unselect" : "§7Click to select")));
    }

    private static /* synthetic */ void openPickSelectionGUI52(boolean hasSel, String chosen, BetPick bp, String leftOddsStr, ItemMeta meta) {
        String name = hasSel && chosen.equalsIgnoreCase(bp.leftOption) ? "§a✔ " + bp.leftOption + " (" + leftOddsStr + ")" : "§7" + bp.leftOption + " (" + leftOddsStr + ")";
        meta.displayName(Component.text(name).decorate(TextDecoration.BOLD));
        meta.lore(Arrays.asList(Component.text("§7" + bp.question), Component.text(hasSel && chosen.equalsIgnoreCase(bp.leftOption) ? "§aClick to unselect" : "§7Click to select")));
    }

    public static class PickData {
        public String question;
        public String leftOption;
        public String leftBlock;
        public String rightOption;
        public String rightBlock;
        public int bucks;
        public String playerName;
        public Set<String> statCategories;
        public String correctOption;
        public int leftOdds;
        public int rightOdds;

        public PickData(String question, String leftOption, String leftBlock, String rightOption, String rightBlock, int bucks, String playerName, Set<String> statCategories, String correctOption, int leftOdds, int rightOdds) {
            this.question = question;
            this.leftOption = leftOption;
            this.leftBlock = leftBlock;
            this.rightOption = rightOption;
            this.rightBlock = rightBlock;
            this.bucks = bucks;
            this.playerName = playerName;
            this.statCategories = statCategories;
            this.correctOption = correctOption;
            this.leftOdds = leftOdds;
            this.rightOdds = rightOdds;
        }
    }

    public static class CategoryConfig {
        public String name;
        public Material itemMaterial;
        public List<PickData> picks;

        public CategoryConfig(String name, Material itemMaterial, List<PickData> picks) {
            this.name = name;
            this.itemMaterial = itemMaterial;
            this.picks = picks;
        }
    }

    public static class GameConfig {
        public String gameId;
        public String displayName;
        public String spread;
        public String total;
        public ZonedDateTime lockTimeEastern;
        public List<CategoryConfig> categories;

        public GameConfig(String gameId, String displayName, String spread, String total, ZonedDateTime lockTimeEastern, List<CategoryConfig> categories) {
            this.gameId = gameId;
            this.displayName = displayName;
            this.spread = spread;
            this.total = total;
            this.lockTimeEastern = lockTimeEastern;
            this.categories = categories;
        }
    }

    public static class BetPick {
        public final String gameId;
        public final String categoryName;
        public final int pickIndex;
        public final String question;
        public final String leftOption;
        public final Material leftBlock;
        public final String rightOption;
        public final Material rightBlock;
        public final int bucks;
        public final String playerName;
        public final Set<String> statCategories;
        public final String correctOption;
        public final int leftOdds;
        public final int rightOdds;
        public String correctSide = null;

        public BetPick(String gameId, String categoryName, int pickIndex, String question, String leftOption, Material leftBlock, String rightOption, Material rightBlock, int bucks, String playerName, Set<String> statCategories, String correctOption, int leftOdds, int rightOdds) {
            this.gameId = gameId;
            this.categoryName = categoryName;
            this.pickIndex = pickIndex;
            this.question = question;
            this.leftOption = leftOption;
            this.leftBlock = leftBlock;
            this.rightOption = rightOption;
            this.rightBlock = rightBlock;
            this.bucks = bucks;
            this.playerName = playerName;
            this.statCategories = statCategories;
            this.correctOption = correctOption;
            this.leftOdds = leftOdds;
            this.rightOdds = rightOdds;
        }

        public String getPickKey() {
            return this.gameId + ":" + this.categoryName + ":" + this.pickIndex;
        }
    }

    public static class ParlayPick {
        public int pickIndex;
        public String question;
        public String chosenSide;
        public String gameId;
        public String categoryName;
        public String playerName;
        public Set<String> statCategories;
        public int odds;
        public int wager;

        public ParlayPick(int pickIndex, String question, String chosenSide, String gameId, String categoryName, Set<String> statCategories, int odds) {
            this.pickIndex = pickIndex;
            this.question = question;
            this.chosenSide = chosenSide;
            this.gameId = gameId;
            this.categoryName = categoryName;
            this.playerName = BettingManager.allPicks.get(gameId + ":" + categoryName + ":" + pickIndex).playerName;
            this.statCategories = statCategories;
            this.odds = odds;
            this.wager = 0;
        }

        public ParlayPick(int pickIndex, String question, String chosenSide, String gameId, String categoryName, Set<String> statCategories, int odds, int wager) {
            this(pickIndex, question, chosenSide, gameId, categoryName, statCategories, odds);
            this.wager = wager;
        }
    }

    public static class PlayerBet {
        public UUID player;
        public int totalBet;
        public boolean parlayed;
        public List<ParlayPick> picks;
        public int combinedOdds;

        public PlayerBet(UUID player, int totalBet, boolean parlayed, List<ParlayPick> picks) {
            this.player = player;
            this.totalBet = totalBet;
            this.parlayed = parlayed;
            this.picks = picks;
            this.combinedOdds = 0;
        }

        public PlayerBet(UUID player, int totalBet, boolean parlayed, List<ParlayPick> picks, int combinedOdds) {
            this.player = player;
            this.totalBet = totalBet;
            this.parlayed = parlayed;
            this.picks = picks;
            this.combinedOdds = combinedOdds;
        }

        public int getPotentialPayout() {
            if (this.parlayed) {
                return BettingManager.americanToPayout(this.totalBet, this.combinedOdds);
            }
            int sum = 0;
            for (ParlayPick pp : this.picks) {
                sum += BettingManager.americanToPayout(pp.wager, pp.odds);
            }
            return sum;
        }
    }

    private static class ParlayResult {
        private final UUID playerId;
        private final int payout;
        private final List<ParlayPick> picks;

        public ParlayResult(UUID playerId, int payout, List<ParlayPick> picks) {
            this.playerId = playerId;
            this.payout = payout;
            this.picks = picks;
        }
    }
}

