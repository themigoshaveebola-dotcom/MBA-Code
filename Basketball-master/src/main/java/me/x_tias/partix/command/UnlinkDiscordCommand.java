package me.x_tias.partix.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import me.x_tias.partix.database.DiscordLinkDb;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

@CommandAlias("unlinkdiscord")
public class UnlinkDiscordCommand extends BaseCommand {
    
    @Default
    public void onUnlinkDiscord(Player player) {
        // Check if linked
        DiscordLinkDb.getLinkedDiscord(player.getUniqueId()).thenAccept(discordId -> {
            if (discordId == null) {
                player.sendMessage(Component.text("❌ ", NamedTextColor.RED)
                        .append(Component.text("You don't have a Discord account linked!", NamedTextColor.YELLOW))
                );
                player.sendMessage(Component.text("   Use ", NamedTextColor.GRAY)
                        .append(Component.text("/linkdiscord", NamedTextColor.AQUA, TextDecoration.BOLD))
                        .append(Component.text(" to link your account.", NamedTextColor.GRAY))
                );
                return;
            }
            
            // Unlink the account
            DiscordLinkDb.unlinkDiscord(player.getUniqueId()).thenAccept(success -> {
                if (success) {
                    player.sendMessage(Component.empty());
                    player.sendMessage(Component.text("═══════════════════════════════════", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD));
                    player.sendMessage(Component.text("        🔓 DISCORD UNLINKED", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD));
                    player.sendMessage(Component.text("═══════════════════════════════════", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD));
                    player.sendMessage(Component.empty());
                    
                    player.sendMessage(Component.text("✅ ", NamedTextColor.GREEN)
                            .append(Component.text("Successfully unlinked your Discord account!", NamedTextColor.WHITE))
                    );
                    player.sendMessage(Component.text("   Your Minecraft and Discord accounts are no longer connected.", NamedTextColor.GRAY));
                    player.sendMessage(Component.empty());
                    player.sendMessage(Component.text("   You can link again anytime with ", NamedTextColor.GRAY)
                            .append(Component.text("/linkdiscord", NamedTextColor.AQUA, TextDecoration.BOLD))
                    );
                    
                    player.sendMessage(Component.text("═══════════════════════════════════", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD));
                    player.sendMessage(Component.empty());
                } else {
                    player.sendMessage(Component.text("❌ ", NamedTextColor.RED)
                            .append(Component.text("Failed to unlink Discord account. Please try again.", NamedTextColor.YELLOW))
                    );
                }
            });
        });
    }
}
