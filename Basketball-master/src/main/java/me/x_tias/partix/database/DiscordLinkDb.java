package me.x_tias.partix.database;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import org.bukkit.Bukkit;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Handles Discord account linking via Supabase API
 * 
 * Supabase Tables Required:
 * 1. discord_links: { minecraft_uuid: text (PK), minecraft_username: text, discord_id: text, linked_at: timestamp }
 * 2. verification_codes: { code: text (PK), minecraft_uuid: text, minecraft_username: text, created_at: timestamp, expires_at: timestamp }
 */
public class DiscordLinkDb {
    private static final String SUPABASE_URL = "https://aehtarohptmtrgksxhll.supabase.co"; // e.g., https://xxxxx.supabase.co
    private static final String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImFlaHRhcm9ocHRtdHJna3N4aGxsIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Njc1MDc5MDMsImV4cCI6MjA4MzA4MzkwM30.yamLCaDF4teavO7zTQhsUoVOJBhE9WPnqA6rKc1zd0M"; // Your anon/public key
    
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    
    private static final Gson gson = new Gson();
    private static final Logger logger = Bukkit.getLogger();
    private static final SecureRandom random = new SecureRandom();
    
    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 6;
    
    /**
     * Generate a random 6-character verification code
     */
    private static String generateCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return code.toString();
    }
    
    /**
     * Check if a Minecraft account is already linked to Discord
     * @return CompletableFuture<String> - Discord ID if linked, null if not linked
     */
    public static CompletableFuture<String> getLinkedDiscord(UUID minecraftUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = SUPABASE_URL + "/rest/v1/discord_links?minecraft_uuid=eq." + minecraftUuid.toString() + "&select=discord_id";
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("apikey", SUPABASE_KEY)
                        .header("Authorization", "Bearer " + SUPABASE_KEY)
                        .GET()
                        .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                logger.info("[Discord Link] getLinkedDiscord for " + minecraftUuid + " - Status: " + response.statusCode());
                
                if (response.statusCode() == 200) {
                    JsonArray results = gson.fromJson(response.body(), JsonArray.class);
                    if (results.size() > 0) {
                        return results.get(0).getAsJsonObject().get("discord_id").getAsString();
                    }
                    logger.info("[Discord Link] No link found in database");
                } else {
                    logger.warning("[Discord Link] Non-200 response: " + response.statusCode() + " - " + response.body());
                }
                return null;
            } catch (Exception e) {
                logger.severe("[Discord Link] Failed to check Discord link: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        });
    }
    
    /**
     * Create a verification code for linking Discord account
     * @return CompletableFuture<String> - The generated verification code
     */
    public static CompletableFuture<String> createVerificationCode(UUID minecraftUuid, String minecraftUsername) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Delete any existing codes for this player
                deleteVerificationCode(minecraftUuid).join();
                
                String code = generateCode();
                String url = SUPABASE_URL + "/rest/v1/verification_codes";
                
                JsonObject data = new JsonObject();
                data.addProperty("code", code);
                data.addProperty("minecraft_uuid", minecraftUuid.toString());
                data.addProperty("minecraft_username", minecraftUsername);
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("apikey", SUPABASE_KEY)
                        .header("Authorization", "Bearer " + SUPABASE_KEY)
                        .header("Content-Type", "application/json")
                        .header("Prefer", "return=minimal")
                        .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(data)))
                        .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                logger.info("[Discord Link] createVerificationCode - Status: " + response.statusCode());
                
                if (response.statusCode() == 201 || response.statusCode() == 200) {
                    logger.info("[Discord Link] Created verification code for " + minecraftUsername + ": " + code);
                    return code;
                } else {
                    logger.warning("[Discord Link] Failed to create verification code: " + response.statusCode() + " - " + response.body());
                    return null;
                }
            } catch (Exception e) {
                logger.severe("Error creating verification code: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        });
    }
    
    /**
     * Delete verification code for a Minecraft UUID
     */
    private static CompletableFuture<Void> deleteVerificationCode(UUID minecraftUuid) {
        return CompletableFuture.runAsync(() -> {
            try {
                String url = SUPABASE_URL + "/rest/v1/verification_codes?minecraft_uuid=eq." + minecraftUuid.toString();
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("apikey", SUPABASE_KEY)
                        .header("Authorization", "Bearer " + SUPABASE_KEY)
                        .DELETE()
                        .build();
                
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (Exception e) {
                logger.warning("Failed to delete verification code: " + e.getMessage());
            }
        });
    }
    
    /**
     * Unlink a Discord account from a Minecraft account
     * @return CompletableFuture<Boolean> - true if successfully unlinked
     */
    public static CompletableFuture<Boolean> unlinkDiscord(UUID minecraftUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = SUPABASE_URL + "/rest/v1/discord_links?minecraft_uuid=eq." + minecraftUuid.toString();
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("apikey", SUPABASE_KEY)
                        .header("Authorization", "Bearer " + SUPABASE_KEY)
                        .DELETE()
                        .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                logger.info("[Discord Link] unlinkDiscord - Status: " + response.statusCode());
                
                boolean success = response.statusCode() == 204 || response.statusCode() == 200;
                if (success) {
                    logger.info("[Discord Link] Unlinked Discord account for UUID: " + minecraftUuid);
                } else {
                    logger.warning("[Discord Link] Failed to unlink: " + response.statusCode() + " - " + response.body());
                }
                return success;
            } catch (Exception e) {
                logger.warning("Failed to unlink Discord: " + e.getMessage());
                return false;
            }
        });
    }
    
    /**
     * Get Discord info for a Minecraft player
     * @return CompletableFuture<DiscordInfo> - Discord info or null if not linked
     */
    public static CompletableFuture<DiscordInfo> getDiscordInfo(UUID minecraftUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = SUPABASE_URL + "/rest/v1/discord_links?minecraft_uuid=eq." + minecraftUuid.toString() + "&select=discord_id,discord_username,discord_tag";
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("apikey", SUPABASE_KEY)
                        .header("Authorization", "Bearer " + SUPABASE_KEY)
                        .GET()
                        .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 200) {
                    JsonArray results = gson.fromJson(response.body(), JsonArray.class);
                    if (results.size() > 0) {
                        JsonObject data = results.get(0).getAsJsonObject();
                        String discordId = data.get("discord_id").getAsString();
                        String discordUsername = data.has("discord_username") ? data.get("discord_username").getAsString() : null;
                        String discordTag = data.has("discord_tag") ? data.get("discord_tag").getAsString() : null;
                        return new DiscordInfo(discordId, discordUsername, discordTag);
                    }
                }
                return null;
            } catch (Exception e) {
                logger.warning("Failed to get Discord info: " + e.getMessage());
                return null;
            }
        });
    }
    
    /**
     * Discord info container
     */
    public static class DiscordInfo {
        public final String discordId;
        public final String discordUsername;
        public final String discordTag;
        
        public DiscordInfo(String discordId, String discordUsername, String discordTag) {
            this.discordId = discordId;
            this.discordUsername = discordUsername;
            this.discordTag = discordTag;
        }
        
        public String getDisplayName() {
            if (discordTag != null && !discordTag.isEmpty()) {
                return discordTag;
            } else if (discordUsername != null && !discordUsername.isEmpty()) {
                return discordUsername;
            } else {
                return discordId;
            }
        }
    }
}
