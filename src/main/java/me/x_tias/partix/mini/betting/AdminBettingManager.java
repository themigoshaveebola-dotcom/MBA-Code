/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.TextComponent
 *  net.kyori.adventure.text.event.HoverEvent
 *  net.kyori.adventure.text.event.HoverEventSource
 *  net.kyori.adventure.text.format.TextDecoration
 *  org.bukkit.Material
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.event.inventory.ClickType
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.scoreboard.Team
 */
package me.x_tias.partix.mini.betting;

import me.x_tias.partix.Partix;
import me.x_tias.partix.plugin.gui.GUI;
import me.x_tias.partix.plugin.gui.ItemButton;
import me.x_tias.partix.server.rank.Ranks;
import me.x_tias.partix.util.Colour;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Team;

import java.util.HashSet;
import java.util.Map;

public class AdminBettingManager {
    public static void openAdminView(Player admin) {
        GUI adminGUI = new GUI("Admin Betting View", 6, false);
        Map<String, Map<String, Integer>> stats = BettingManager.getPickStats();
        int netherStarSlot = 53;
        for (Map.Entry<String, Map<String, Integer>> entry : stats.entrySet()) {
            int pickIndex;
            String pickKey = entry.getKey();
            String[] parts = pickKey.split(":");
            if (parts.length != 3) continue;
            String gameId = parts[0];
            String categoryName = parts[1];
            try {
                pickIndex = Integer.parseInt(parts[2]);
            } catch (NumberFormatException e) {
                continue;
            }
            Map<String, Integer> options = entry.getValue();
            adminGUI.addButton(new ItemButton(pickIndex - 1, AdminBettingManager.createStatItem(Material.RED_CONCRETE, gameId, categoryName, pickIndex, options), p -> {
            }));
            adminGUI.addButton(new ItemButton(pickIndex + 1, AdminBettingManager.createStatItem(Material.GREEN_CONCRETE, gameId, categoryName, pickIndex, options), p -> {
            }));
        }
        adminGUI.addButton(new ItemButton(netherStarSlot, AdminBettingManager.createNetherStar(), AdminBettingManager::finalizePicks));
        adminGUI.addButton(new ItemButton(49, AdminBettingManager.createToggleLockButton(), AdminBettingManager::togglePicksLock));
        adminGUI.openInventory(admin);
    }

    private static ItemStack createStatItem(Material material, String gameId, String categoryName, int pickIndex, Map<String, Integer> options) {
        ItemStack item = new ItemStack(material);
        item.editMeta(meta -> {
            int totalVotes = options.values().stream().mapToInt(i -> i).sum();
            Component hoverText = Component.text("\ud83d\udcca Voting Stats (").append(Component.text(gameId + " | " + categoryName + " #" + pickIndex)).append(Component.text("):\n")).color(Colour.border());
            for (Map.Entry<String, Integer> opt : options.entrySet()) {
                int percentage = totalVotes == 0 ? 0 : opt.getValue() * 100 / totalVotes;
                hoverText = hoverText.append(Component.text(" - " + opt.getKey() + ": " + percentage + "% (" + opt.getValue() + " votes)\n").color(Colour.text()));
            }
            meta.displayName(Component.text("\ud83d\udcca Pick ( " + gameId + " | " + categoryName + " #" + pickIndex + " )").hoverEvent(HoverEvent.showText(hoverText)));
        });
        return item;
    }

    private static ItemStack createNetherStar() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        item.editMeta(meta -> meta.displayName(Component.text("✅ Finalize Correct Picks").color(Colour.allow()).decorate(TextDecoration.BOLD)));
        return item;
    }

    private static void finalizePicks(Player player, ClickType click) {
        HashSet<String> allGameIds = new HashSet<>();
        for (String pickKey : BettingManager.getPickStats().keySet()) {
            String[] parts = pickKey.split(":");
            if (parts.length < 1) continue;
            allGameIds.add(parts[0]);
        }
        if (allGameIds.isEmpty()) {
            player.sendMessage(Component.text("§cNo games to finalize."));
            return;
        }
        for (String gameId : allGameIds) {
            BettingManager.endCurrentPicks(player, gameId);
        }
        player.sendMessage(Component.text("✅ All games have been finalized and reset."));
    }

    private static ItemStack createToggleLockButton() {
        ItemStack item = new ItemStack(Material.EMERALD);
        item.editMeta(meta -> {
            boolean isLocked = BettingManager.isGlobalLocked();
            meta.displayName(Component.text(isLocked ? "\ud83d\udd13 Unlock All Picks" : "\ud83d\udd12 Lock All Picks"));
        });
        return item;
    }

    private static void togglePicksLock(Player player, ClickType click) {
        boolean newLockState;
        boolean bl = newLockState = !BettingManager.isGlobalLocked();
        if (newLockState) {
            BettingManager.lockAllPicksGlobally();
        } else {
            BettingManager.unlockAllPicksGlobally();
        }
        player.sendMessage(Component.text(newLockState ? "\ud83d\udd12 All picks have been locked!" : "\ud83d\udd13 Picks are now unlocked!"));
        AdminBettingManager.openAdminView(player);
    }

    private static boolean isAdmin(Player player) {
        Team adminTeam = Ranks.getAdmin();
        return adminTeam != null && adminTeam.hasEntry(player.getName());
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        Player admin;
        if (!(sender instanceof Player) || !AdminBettingManager.isAdmin(admin = (Player) sender)) {
            sender.sendMessage("❌ You must be an ADMIN to use this command!");
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            Partix.getInstance().reloadConfig();
            BettingManager.loadGamesFromConfig(Partix.getInstance().getConfig());
            admin.sendMessage("§a[Betting] Config reloaded successfully.");
            return true;
        }
        AdminBettingManager.openAdminView(admin);
        return true;
    }
}

