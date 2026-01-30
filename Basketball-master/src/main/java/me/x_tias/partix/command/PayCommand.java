package me.x_tias.partix.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.Syntax;
import me.x_tias.partix.database.PlayerDb;
import me.x_tias.partix.util.Colour;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

@CommandAlias("pay")
public class PayCommand extends BaseCommand {

    @CommandAlias("pay")
    @Syntax("<player> <amount> [type]")
    @CommandCompletion("@players @nothing coins|bucks")
    public void onPay(Player sender, String targetName, int amount, String... typeArgs) {
        // Validate amount
        if (amount <= 0) {
            sender.sendMessage(Component.text("You must pay at least 1 currency unit.").color(Colour.deny()));
            return;
        }

        // Determine currency type (default: COINS)
        String type = "COINS";
        if (typeArgs.length > 0) {
            String inputType = typeArgs[0].toUpperCase();
            if (inputType.equals("BUCKS")) {
                type = "MBA_BUCKS";
            } else if (inputType.equals("COINS")) {
                type = "COINS";
            } else {
                sender.sendMessage(Component.text("Invalid currency type. Use COINS or BUCKS.").color(Colour.deny()));
                return;
            }
        }

        // Get target player
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found: " + targetName).color(Colour.deny()));
            return;
        }

        // Don't allow paying yourself
        if (target.getUniqueId().equals(sender.getUniqueId())) {
            sender.sendMessage(Component.text("You cannot pay yourself.").color(Colour.deny()));
            return;
        }

        // Get sender's UUID
        UUID senderId = sender.getUniqueId();
        UUID targetId = target.getUniqueId();

        // Get the stat based on type
        PlayerDb.Stat stat = type.equals("MBA_BUCKS") ? PlayerDb.Stat.MBA_BUCKS : PlayerDb.Stat.COINS;
        String displayName = type.equals("MBA_BUCKS") ? "MBABucks" : "Coins";

        // Check sender's balance
        PlayerDb.get(senderId, stat).thenAccept(senderBalance -> {
            if (senderBalance < amount) {
                sender.sendMessage(Component.text("You only have " + senderBalance + " " + displayName + ".").color(Colour.deny()));
                return;
            }

            // Remove from sender
            PlayerDb.remove(senderId, stat, amount);

            // Add to recipient
            PlayerDb.add(targetId, stat, amount);

            // Send confirmation to sender
            sender.sendMessage(Component.text("✅ Sent " + amount + " " + displayName + " to " + target.getName()).color(Colour.allow()));

            // Send notification to recipient
            target.sendMessage(Component.text("💰 " + sender.getName() + " sent you " + amount + " " + displayName + "!").color(Colour.allow()));
        });
    }
}