package me.x_tias.partix.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.Default;
import me.x_tias.partix.database.DiscordLinkDb;
import me.x_tias.partix.database.PlayerDb;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

@CommandAlias("whois")
public class WhoisCommand extends BaseCommand {
    
    @Default
    @CommandCompletion("@players")
    public void onWhois(Player sender, String targetName) {
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
        
        final OfflinePlayer finalTarget = target;
        final UUID targetUuid = target.getUniqueId();
        final String targetUsername = target.getName();
        
        // Get Discord info
        DiscordLinkDb.getDiscordInfo(targetUuid).thenAccept(discordInfo -> {
            sender.sendMessage(Component.empty());
            sender.sendMessage(Component.text("═══════════════════════════════════", NamedTextColor.DARK_AQUA, TextDecoration.BOLD));
            sender.sendMessage(Component.text("        👤 PLAYER INFO", NamedTextColor.AQUA, TextDecoration.BOLD));
            sender.sendMessage(Component.text("═══════════════════════════════════", NamedTextColor.DARK_AQUA, TextDecoration.BOLD));
            sender.sendMessage(Component.empty());
            
            // Minecraft info
            sender.sendMessage(Component.text("Minecraft: ", NamedTextColor.GRAY)
                    .append(Component.text(targetUsername, NamedTextColor.WHITE, TextDecoration.BOLD))
            );
            sender.sendMessage(Component.text("UUID: ", NamedTextColor.GRAY)
                    .append(Component.text(targetUuid.toString(), NamedTextColor.DARK_GRAY))
            );
            
            boolean isOnline = finalTarget.isOnline();
            Component statusComponent = isOnline 
                    ? Component.text("● ONLINE", NamedTextColor.GREEN, TextDecoration.BOLD)
                    : Component.text("● OFFLINE", NamedTextColor.RED, TextDecoration.BOLD);
            sender.sendMessage(Component.text("Status: ", NamedTextColor.GRAY).append(statusComponent));
            
            sender.sendMessage(Component.empty());
            
            // Discord info
            if (discordInfo != null) {
                sender.sendMessage(Component.text("Discord: ", NamedTextColor.GRAY)
                        .append(Component.text("🔗 LINKED", NamedTextColor.GREEN, TextDecoration.BOLD))
                );
                
                String displayName = discordInfo.getDisplayName();
                sender.sendMessage(Component.text("  Tag: ", NamedTextColor.GRAY)
                        .append(Component.text(displayName, NamedTextColor.LIGHT_PURPLE))
                );
                sender.sendMessage(Component.text("  ID: ", NamedTextColor.GRAY)
                        .append(Component.text(discordInfo.discordId, NamedTextColor.DARK_GRAY))
                );
            } else {
                sender.sendMessage(Component.text("Discord: ", NamedTextColor.GRAY)
                        .append(Component.text("❌ NOT LINKED", NamedTextColor.RED, TextDecoration.BOLD))
                );
            }
            
            sender.sendMessage(Component.empty());
            sender.sendMessage(Component.text("═══════════════════════════════════", NamedTextColor.DARK_AQUA, TextDecoration.BOLD));
            sender.sendMessage(Component.empty());
        });
    }
    
    @Default
    public void onWhoisSelf(Player sender) {
        // If no argument provided, show info about the sender
        onWhois(sender, sender.getName());
    }
}
