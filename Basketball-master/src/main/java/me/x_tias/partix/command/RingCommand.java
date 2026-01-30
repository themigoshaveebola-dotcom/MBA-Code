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

@CommandAlias("ring|rings")
@CommandPermission("basketball.admin")
public class RingCommand extends BaseCommand {
    
    private static final Gson gson = new Gson();
    private static final Type LIST_TYPE = new TypeToken<List<String>>(){}.getType();
    
    @Subcommand("view")
    @CommandCompletion("@players")
    @Description("View a player's championship rings")
    public void onView(Player sender, String targetName) {
        OfflinePlayer target = findPlayer(sender, targetName);
        if (target == null) return;
        
        PlayerDb.getString(target.getUniqueId(), PlayerDb.Stat.CHAMPIONSHIP_RINGS).thenAccept(ringsJson -> {
            List<String> rings = parseRings(ringsJson);
            
            Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("Partix"), () -> {
                sender.sendMessage(Component.empty());
                sender.sendMessage(Component.text("═══════════════════════════════════", NamedTextColor.GOLD, TextDecoration.BOLD));
                sender.sendMessage(Component.text("  🏆 CHAMPIONSHIP RINGS", NamedTextColor.YELLOW, TextDecoration.BOLD));
                sender.sendMessage(Component.text("═══════════════════════════════════", NamedTextColor.GOLD, TextDecoration.BOLD));
                sender.sendMessage(Component.empty());
                
                sender.sendMessage(Component.text("Player: ", NamedTextColor.GRAY)
                        .append(Component.text(target.getName(), NamedTextColor.WHITE, TextDecoration.BOLD))
                );
                
                sender.sendMessage(Component.text("Championship Rings: ", NamedTextColor.GRAY)
                        .append(Component.text(rings.size(), NamedTextColor.GOLD, TextDecoration.BOLD))
                );
                
                if (!rings.isEmpty()) {
                    sender.sendMessage(Component.empty());
                    for (String ring : rings) {
                        sender.sendMessage(Component.text("  • ", NamedTextColor.GOLD)
                                .append(Component.text(ring, NamedTextColor.YELLOW))
                        );
                    }
                }
                
                sender.sendMessage(Component.empty());
                sender.sendMessage(Component.text("═══════════════════════════════════", NamedTextColor.GOLD, TextDecoration.BOLD));
                sender.sendMessage(Component.empty());
            });
        });
    }
    
    @Subcommand("set")
    @CommandCompletion("@players")
    @Description("Set a player's championship ring count")
    public void onSet(Player sender, String targetName, int amount) {
        OfflinePlayer target = findPlayer(sender, targetName);
        if (target == null) return;
        
        if (amount < 0) {
            sender.sendMessage(Component.text("❌ Amount must be 0 or greater!", NamedTextColor.RED));
            return;
        }
        
        PlayerDb.set(target.getUniqueId(), PlayerDb.Stat.CHAMPIONSHIPS, amount);
        
        sender.sendMessage(Component.text("✅ ", NamedTextColor.GREEN)
                .append(Component.text("Set ", NamedTextColor.WHITE))
                .append(Component.text(target.getName(), NamedTextColor.GOLD))
                .append(Component.text("'s championship count to ", NamedTextColor.WHITE))
                .append(Component.text(amount, NamedTextColor.GOLD, TextDecoration.BOLD))
        );
    }
    
    @Subcommand("add")
    @CommandCompletion("@players")
    @Description("Add a championship ring to a player")
    public void onAdd(Player sender, String targetName, String ringName) {
        OfflinePlayer target = findPlayer(sender, targetName);
        if (target == null) return;
        
        PlayerDb.getString(target.getUniqueId(), PlayerDb.Stat.CHAMPIONSHIP_RINGS).thenAccept(ringsJson -> {
            List<String> rings = parseRings(ringsJson);
            rings.add(ringName);
            
            String updatedJson = gson.toJson(rings);
            PlayerDb.setString(target.getUniqueId(), PlayerDb.Stat.CHAMPIONSHIP_RINGS, updatedJson);
            PlayerDb.set(target.getUniqueId(), PlayerDb.Stat.CHAMPIONSHIPS, rings.size());
            
            Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("Partix"), () -> {
                sender.sendMessage(Component.text("✅ ", NamedTextColor.GREEN)
                        .append(Component.text("Added ring to ", NamedTextColor.WHITE))
                        .append(Component.text(target.getName(), NamedTextColor.GOLD))
                        .append(Component.text(": ", NamedTextColor.WHITE))
                        .append(Component.text(ringName, NamedTextColor.YELLOW, TextDecoration.BOLD))
                );
                sender.sendMessage(Component.text("   Total rings: ", NamedTextColor.GRAY)
                        .append(Component.text(rings.size(), NamedTextColor.GOLD))
                );
            });
        });
    }
    
    @Subcommand("remove")
    @CommandCompletion("@players")
    @Description("Remove the last championship ring from a player")
    public void onRemove(Player sender, String targetName) {
        OfflinePlayer target = findPlayer(sender, targetName);
        if (target == null) return;
        
        PlayerDb.getString(target.getUniqueId(), PlayerDb.Stat.CHAMPIONSHIP_RINGS).thenAccept(ringsJson -> {
            List<String> rings = parseRings(ringsJson);
            
            if (rings.isEmpty()) {
                Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("Partix"), () -> {
                    sender.sendMessage(Component.text("❌ ", NamedTextColor.RED)
                            .append(Component.text(target.getName() + " has no rings to remove!", NamedTextColor.YELLOW))
                    );
                });
                return;
            }
            
            String removedRing = rings.remove(rings.size() - 1);
            
            String updatedJson = gson.toJson(rings);
            PlayerDb.setString(target.getUniqueId(), PlayerDb.Stat.CHAMPIONSHIP_RINGS, updatedJson);
            PlayerDb.set(target.getUniqueId(), PlayerDb.Stat.CHAMPIONSHIPS, rings.size());
            
            Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("Partix"), () -> {
                sender.sendMessage(Component.text("✅ ", NamedTextColor.GREEN)
                        .append(Component.text("Removed ring from ", NamedTextColor.WHITE))
                        .append(Component.text(target.getName(), NamedTextColor.GOLD))
                        .append(Component.text(": ", NamedTextColor.WHITE))
                        .append(Component.text(removedRing, NamedTextColor.YELLOW, TextDecoration.BOLD))
                );
                sender.sendMessage(Component.text("   Remaining rings: ", NamedTextColor.GRAY)
                        .append(Component.text(rings.size(), NamedTextColor.GOLD))
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
    
    private List<String> parseRings(String json) {
        if (json == null || json.isEmpty() || json.equals("{}")) {
            return new ArrayList<>();
        }
        
        try {
            List<String> rings = gson.fromJson(json, LIST_TYPE);
            return rings != null ? rings : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
