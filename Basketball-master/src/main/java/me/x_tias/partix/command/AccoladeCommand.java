package me.x_tias.partix.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import me.x_tias.partix.database.PlayerDb;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

@CommandAlias("accolade|accolades")
@CommandPermission("basketball.admin")
public class AccoladeCommand extends BaseCommand {
    
    private static final Gson gson = new Gson();
    private static final Type LIST_TYPE = new TypeToken<List<String>>(){}.getType();
    
    @Subcommand("view")
    @CommandCompletion("@players")
    @Description("View a player's accolades")
    public void onView(Player sender, String targetName) {
        OfflinePlayer target = findPlayer(sender, targetName);
        if (target == null) return;
        
        PlayerDb.getString(target.getUniqueId(), PlayerDb.Stat.ACCOLADES).thenAccept(accoladesJson -> {
            List<String> accolades = parseAccolades(accoladesJson);
            
            Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("Partix"), () -> {
                sender.sendMessage(Component.empty());
                sender.sendMessage(Component.text("═══════════════════════════════════", NamedTextColor.AQUA, TextDecoration.BOLD));
                sender.sendMessage(Component.text("  ⭐ ACCOLADES", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD));
                sender.sendMessage(Component.text("═══════════════════════════════════", NamedTextColor.AQUA, TextDecoration.BOLD));
                sender.sendMessage(Component.empty());
                
                sender.sendMessage(Component.text("Player: ", NamedTextColor.GRAY)
                        .append(Component.text(target.getName(), NamedTextColor.WHITE, TextDecoration.BOLD))
                );
                
                sender.sendMessage(Component.text("Total Accolades: ", NamedTextColor.GRAY)
                        .append(Component.text(accolades.size(), NamedTextColor.AQUA, TextDecoration.BOLD))
                );
                
                if (!accolades.isEmpty()) {
                    sender.sendMessage(Component.empty());
                    for (String accolade : accolades) {
                        sender.sendMessage(Component.text("  • ", NamedTextColor.AQUA)
                                .append(Component.text(accolade, NamedTextColor.LIGHT_PURPLE))
                        );
                    }
                }
                
                sender.sendMessage(Component.empty());
                sender.sendMessage(Component.text("═══════════════════════════════════", NamedTextColor.AQUA, TextDecoration.BOLD));
                sender.sendMessage(Component.empty());
            });
        });
    }
    
    @Subcommand("add")
    @CommandCompletion("@players")
    @Description("Add an accolade to a player")
    public void onAdd(Player sender, String targetName, String accoladeName) {
        OfflinePlayer target = findPlayer(sender, targetName);
        if (target == null) return;
        
        PlayerDb.getString(target.getUniqueId(), PlayerDb.Stat.ACCOLADES).thenAccept(accoladesJson -> {
            List<String> accolades = parseAccolades(accoladesJson);
            accolades.add(accoladeName);
            
            String updatedJson = gson.toJson(accolades);
            PlayerDb.setString(target.getUniqueId(), PlayerDb.Stat.ACCOLADES, updatedJson);
            
            Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("Partix"), () -> {
                sender.sendMessage(Component.text("✅ ", NamedTextColor.GREEN)
                        .append(Component.text("Added accolade to ", NamedTextColor.WHITE))
                        .append(Component.text(target.getName(), NamedTextColor.AQUA))
                        .append(Component.text(": ", NamedTextColor.WHITE))
                        .append(Component.text(accoladeName, NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
                );
                sender.sendMessage(Component.text("   Total accolades: ", NamedTextColor.GRAY)
                        .append(Component.text(accolades.size(), NamedTextColor.AQUA))
                );
            });
        });
    }
    
    @Subcommand("remove")
    @CommandCompletion("@players")
    @Description("Remove the last accolade from a player")
    public void onRemove(Player sender, String targetName) {
        OfflinePlayer target = findPlayer(sender, targetName);
        if (target == null) return;
        
        PlayerDb.getString(target.getUniqueId(), PlayerDb.Stat.ACCOLADES).thenAccept(accoladesJson -> {
            List<String> accolades = parseAccolades(accoladesJson);
            
            if (accolades.isEmpty()) {
                Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("Partix"), () -> {
                    sender.sendMessage(Component.text("❌ ", NamedTextColor.RED)
                            .append(Component.text(target.getName() + " has no accolades to remove!", NamedTextColor.YELLOW))
                    );
                });
                return;
            }
            
            String removedAccolade = accolades.remove(accolades.size() - 1);
            
            String updatedJson = gson.toJson(accolades);
            PlayerDb.setString(target.getUniqueId(), PlayerDb.Stat.ACCOLADES, updatedJson);
            
            Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("Partix"), () -> {
                sender.sendMessage(Component.text("✅ ", NamedTextColor.GREEN)
                        .append(Component.text("Removed accolade from ", NamedTextColor.WHITE))
                        .append(Component.text(target.getName(), NamedTextColor.AQUA))
                        .append(Component.text(": ", NamedTextColor.WHITE))
                        .append(Component.text(removedAccolade, NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
                );
                sender.sendMessage(Component.text("   Remaining accolades: ", NamedTextColor.GRAY)
                        .append(Component.text(accolades.size(), NamedTextColor.AQUA))
                );
            });
        });
    }
    
    private OfflinePlayer findPlayer(Player sender, String targetName) {
        OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(targetName);
        
        if (target == null) {
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
            return null;
        }
        
        return target;
    }
    
    private List<String> parseAccolades(String json) {
        if (json == null || json.isEmpty() || json.equals("{}")) {
            return new ArrayList<>();
        }
        
        try {
            List<String> accolades = gson.fromJson(json, LIST_TYPE);
            return accolades != null ? accolades : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
