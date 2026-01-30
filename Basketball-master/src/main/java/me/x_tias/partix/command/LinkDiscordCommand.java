package me.x_tias.partix.command;

import org.bukkit.entity.Player;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import me.x_tias.partix.database.DiscordLinkDb;
import me.x_tias.partix.plugin.cooldown.Cooldown;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

@CommandAlias("linkdiscord")
public class LinkDiscordCommand extends BaseCommand {
    
    private static final int COOLDOWN_TICKS = 1200; // 60 seconds (1 minute)
    
    @Default
    public void onLinkDiscord(Player player) {
        // Check cooldown
        int remainingTicks = Cooldown.getRestriction(player.getUniqueId());
        if (remainingTicks > 0) {
            int remainingSeconds = (int) Math.ceil(remainingTicks / 20.0);
            player.sendMessage(Component.text("❌ ", NamedTextColor.RED)
                    .append(Component.text("Please wait " + remainingSeconds + " second" + (remainingSeconds != 1 ? "s" : "") + " before using this command again.", NamedTextColor.YELLOW))
            );
            return;
        }
        
        // Check if already linked
        DiscordLinkDb.getLinkedDiscord(player.getUniqueId()).thenAccept(discordId -> {
            if (discordId != null) {
                player.sendMessage(Component.text("❌ ", NamedTextColor.RED)
                        .append(Component.text("You already have a Discord account linked!", NamedTextColor.YELLOW))
                );
                player.sendMessage(Component.text("   Use ", NamedTextColor.GRAY)
                        .append(Component.text("/unlinkdiscord", NamedTextColor.AQUA, TextDecoration.BOLD))
                        .append(Component.text(" to unlink first.", NamedTextColor.GRAY))
                );
                return;
            }
            
            // Generate verification code
            DiscordLinkDb.createVerificationCode(player.getUniqueId(), player.getName()).thenAccept(code -> {
                if (code == null) {
                    player.sendMessage(Component.text("❌ ", NamedTextColor.RED)
                            .append(Component.text("Failed to generate verification code. Please try again.", NamedTextColor.YELLOW))
                    );
                    return;
                }
                
                // Set cooldown after successful code generation
                Cooldown.setRestricted(player.getUniqueId(), COOLDOWN_TICKS);
                
                player.sendMessage(Component.empty());
                player.sendMessage(Component.text("═══════════════════════════════════", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD));
                player.sendMessage(Component.text("        🔗 DISCORD LINK", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD));
                player.sendMessage(Component.text("═══════════════════════════════════", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD));
                player.sendMessage(Component.empty());
                
                player.sendMessage(Component.text("Your verification code:", NamedTextColor.GRAY));
                
                // Clickable code
                Component codeComponent = Component.text("  » " + code + " «", NamedTextColor.GOLD, TextDecoration.BOLD)
                        .hoverEvent(HoverEvent.showText(Component.text("Click to copy!", NamedTextColor.GREEN)))
                        .clickEvent(ClickEvent.copyToClipboard(code));
                
                player.sendMessage(codeComponent);
                player.sendMessage(Component.empty());
                
                player.sendMessage(Component.text("Instructions:", NamedTextColor.YELLOW, TextDecoration.BOLD));
                player.sendMessage(Component.text("1. ", NamedTextColor.GRAY)
                        .append(Component.text("Open Discord and go to the server", NamedTextColor.WHITE))
                );
                player.sendMessage(Component.text("2. ", NamedTextColor.GRAY)
                        .append(Component.text("Type ", NamedTextColor.WHITE))
                        .append(Component.text("/linkdiscord " + code, NamedTextColor.AQUA))
                );
                player.sendMessage(Component.text("3. ", NamedTextColor.GRAY)
                        .append(Component.text("Your accounts will be linked!", NamedTextColor.WHITE))
                );
                player.sendMessage(Component.empty());
                
                player.sendMessage(Component.text("⚠ ", NamedTextColor.YELLOW)
                        .append(Component.text("This code expires in 10 minutes", NamedTextColor.GRAY, TextDecoration.ITALIC))
                );
                
                player.sendMessage(Component.text("═══════════════════════════════════", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD));
                player.sendMessage(Component.empty());
            });
        });
    }
}
