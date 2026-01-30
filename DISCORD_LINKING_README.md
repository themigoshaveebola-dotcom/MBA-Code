# Discord-Minecraft Account Linking - Quick Setup

## ✅ Implementation Complete!

I've created a complete Discord-Minecraft account linking system for your Basketball plugin.

---

## 📋 What Was Created

### Minecraft Plugin Files:

1. **DiscordLinkDb.java** - Handles Supabase API communication
2. **LinkDiscordCommand.java** - `/linkdiscord` command (generates code)
3. **UnlinkDiscordCommand.java** - `/unlinkdiscord` command (removes link)  
4. **WhoisCommand.java** - `/whois <player>` command (shows Discord info)
5. **Partix.java** - Updated to register new commands

### Discord Bot Code:

- Complete Python code in `DISCORD_LINKING_SETUP.md`
- `/linkdiscord <code>` slash command
- `/unlinkdiscord` slash command
- Supabase integration

---

## 🚀 Setup Steps

### 1. Configure Supabase

Open [DiscordLinkDb.java](Basketball-master/src/main/java/me/x_tias/partix/database/DiscordLinkDb.java) and update lines 28-29:

```java
private static final String SUPABASE_URL = "https://aehtarohptmtrgksxhll.supabase.co";
private static final String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImFlaHRhcm9ocHRtdHJna3N4aGxsIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Njc1MDc5MDMsImV4cCI6MjA4MzA4MzkwM30.yamLCaDF4teavO7zTQhsUoVOJBhE9WPnqA6rKc1zd0M";
```

### 2. Create Supabase Tables

Run this SQL in your Supabase SQL Editor:

```sql
-- Discord-Minecraft links
CREATE TABLE discord_links (
    minecraft_uuid TEXT PRIMARY KEY,
    minecraft_username TEXT NOT NULL,
    discord_id TEXT UNIQUE NOT NULL,
    discord_username TEXT,
    discord_tag TEXT,
    linked_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_discord_id ON discord_links(discord_id);

-- Temporary verification codes
CREATE TABLE verification_codes (
    code TEXT PRIMARY KEY,
    minecraft_uuid TEXT NOT NULL,
    minecraft_username TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    expires_at TIMESTAMP WITH TIME ZONE DEFAULT (NOW() + INTERVAL '10 minutes')
);

CREATE INDEX idx_expires_at ON verification_codes(expires_at);
```

### 3. Update Your Discord Bot

See [DISCORD_LINKING_SETUP.md](DISCORD_LINKING_SETUP.md) for complete bot code.

**Quick steps:**
1. Install: `pip install supabase-py`
2. Add Supabase credentials to `.env`
3. Copy the `DiscordLinkCog` code to your bot
4. Load the cog

### 4. Create "Linked Account" Role

In your Discord server:
1. Server Settings → Roles → Create Role
2. Name it exactly: **Linked Account**
3. Set permissions (optional perks)
4. Make sure bot's role is **above** this role

### 5. Build & Test

```bash
cd Basketball-master
.\gradlew.bat build
```

Restart your Minecraft server, then test:
- Minecraft: `/linkdiscord` → get code
- Discord: `/linkdiscord ABC123` → link accounts
- Minecraft: `/whois PlayerName` → see Discord info

---

## 🎮 How It Works

### Linking Flow:

```
1. Player runs /linkdiscord in Minecraft
   ↓
2. Plugin generates 6-char code (e.g., "A7X9K2")
   ↓
3. Code stored in Supabase (expires in 10 min)
   ↓
4. Player runs /linkdiscord A7X9K2 in Discord
   ↓
5. Bot verifies code with Supabase
   ↓
6. Bot updates:
   - Discord nickname → Minecraft username
   - Assigns "Linked Account" role
   - Stores link in discord_links table
   ↓
7. Both accounts now linked! ✅
```

### Data Storage:

**Supabase Tables:**
- `discord_links` - Permanent links (minecraft_uuid ↔ discord_id)
- `verification_codes` - Temporary codes (auto-expire after 10 min)

**MySQL (Basketball Plugin):**
- No changes needed! Your existing player database stays separate

---

## 📝 Commands

### Minecraft:
- `/linkdiscord` - Generate verification code
- `/unlinkdiscord` - Remove Discord link
- `/whois <player>` - View player's Discord info
- `/whois` - View your own info

### Discord:
- `/linkdiscord <code>` - Link with 6-char code
- `/unlinkdiscord` - Remove Minecraft link

---

## 🔧 Configuration Options

### Change Code Length:
In [DiscordLinkDb.java](Basketball-master/src/main/java/me/x_tias/partix/database/DiscordLinkDb.java):
```java
private static final int CODE_LENGTH = 6;  // Change to 4, 8, etc.
```

### Change Role Name:
In your Discord bot code:
```python
LINKED_ROLE_NAME = "Linked Account"  # Change this
```

### Change Code Expiration:
In Supabase SQL:
```sql
-- Change '10 minutes' to '5 minutes', '30 minutes', etc.
expires_at TIMESTAMP WITH TIME ZONE DEFAULT (NOW() + INTERVAL '10 minutes')
```

---

## ⚠️ Important Notes

1. **Supabase Credentials**: Keep your Supabase key secure! It's in your code now.

2. **One-Time Codes**: Each code can only be used once and expires after 10 minutes.

3. **Re-linking**: If a player links multiple times, the old link is automatically replaced.

4. **Bot Permissions Required**:
   - Manage Nicknames
   - Manage Roles
   - Use Application Commands

5. **Role Hierarchy**: Bot's role must be above "Linked Account" role to assign it.

---

## 🐛 Troubleshooting

### "Failed to generate verification code"
- Check Supabase URL and key in DiscordLinkDb.java
- Verify tables exist in Supabase
- Check plugin console for errors

### "Invalid or expired verification code"
- Codes expire after 10 minutes
- Codes are case-sensitive (use uppercase)
- Each code can only be used once

### Role not assigning
- Check bot has "Manage Roles" permission
- Bot's role must be higher than "Linked Account"
- Role name must match exactly (case-sensitive)

### Nickname not changing
- Bot needs "Manage Nicknames" permission
- Cannot change server owner's nickname
- Bot's role must be higher in hierarchy

---

## 📚 Files to Review

1. [DiscordLinkDb.java](Basketball-master/src/main/java/me/x_tias/partix/database/DiscordLinkDb.java) - **UPDATE SUPABASE CREDENTIALS HERE**
2. [LinkDiscordCommand.java](Basketball-master/src/main/java/me/x_tias/partix/command/LinkDiscordCommand.java)
3. [UnlinkDiscordCommand.java](Basketball-master/src/main/java/me/x_tias/partix/command/UnlinkDiscordCommand.java)
4. [WhoisCommand.java](Basketball-master/src/main/java/me/x_tias/partix/command/WhoisCommand.java)
5. [DISCORD_LINKING_SETUP.md](DISCORD_LINKING_SETUP.md) - **DISCORD BOT CODE**

---

## ✨ Next Steps

1. ✅ Update Supabase credentials in DiscordLinkDb.java
2. ✅ Create Supabase tables (run SQL above)
3. ✅ Update Discord bot with new code
4. ✅ Create "Linked Account" role
5. ✅ Build and test!

Need help? Check the detailed guide in [DISCORD_LINKING_SETUP.md](DISCORD_LINKING_SETUP.md)!
