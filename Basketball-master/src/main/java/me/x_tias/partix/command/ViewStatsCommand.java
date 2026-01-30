package me.x_tias.partix.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.Default;
import me.x_tias.partix.mini.factories.Hub;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

@CommandAlias("viewstats")
public class ViewStatsCommand extends BaseCommand {
    
    @Default
    @CommandCompletion("@players")
    public void onViewStats(Player sender, String targetName) {
        // Try to find the player by name
        OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(targetName);
        
        if (target == null) {
            // Try online players
            Player onlineTarget = Bukkit.getPlayer(targetName);
            if (onlineTarget != null) {
                target = onlineTarget;
            }
        }
        
        if (target == null) {
            sender.sendMessage(Component.text("❌ ", NamedTextColor.RED)
                    .append(Component.text("Player not found: ", NamedTextColor.YELLOW))
                    .append(Component.text(targetName, NamedTextColor.WHITE))
            );
            return;
        }
        
        // Open the player profile GUI for the target player
        Hub.hub.openPlayerProfileGUIForTarget(sender, target.getUniqueId());
    }
    
    @Default
    public void onViewStatsSelf(Player sender) {
        // If no argument provided, show sender's own stats
        Hub.hub.openPlayerProfileGUI(sender);
    }
}
