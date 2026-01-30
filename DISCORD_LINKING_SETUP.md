# Discord Bot Setup for Minecraft Account Linking

This guide will help you add Discord-Minecraft account linking to your existing Discord bot.

## Prerequisites
- Python 3.8+
- discord.py library
- supabase-py library
- Your existing Discord bot

## Installation

```bash
pip install discord.py supabase-py python-dotenv
```

## Supabase Setup

### 1. Create Tables in Supabase

Go to your Supabase SQL Editor and run:

```sql
-- Table for verified Discord-Minecraft links
CREATE TABLE discord_links (
    minecraft_uuid TEXT PRIMARY KEY,
    minecraft_username TEXT NOT NULL,
    discord_id TEXT UNIQUE NOT NULL,
    discord_username TEXT,
    discord_tag TEXT,
    linked_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Index for faster Discord ID lookups
CREATE INDEX idx_discord_id ON discord_links(discord_id);

-- Table for verification codes (temporary storage)
CREATE TABLE verification_codes (
    code TEXT PRIMARY KEY,
    minecraft_uuid TEXT NOT NULL,
    minecraft_username TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    expires_at TIMESTAMP WITH TIME ZONE DEFAULT (NOW() + INTERVAL '10 minutes')
);

-- Index for cleanup
CREATE INDEX idx_expires_at ON verification_codes(expires_at);

-- Function to auto-delete expired codes (runs every hour)
CREATE OR REPLACE FUNCTION delete_expired_codes()
RETURNS void AS $$
BEGIN
    DELETE FROM verification_codes WHERE expires_at < NOW();
END;
$$ LANGUAGE plpgsql;

-- Schedule cleanup (if using pg_cron extension)
-- SELECT cron.schedule('delete-expired-codes', '0 * * * *', 'SELECT delete_expired_codes()');
```

### 2. Get Supabase Credentials

1. Go to your Supabase project settings
2. Find **API** section
3. Copy:
   - **Project URL** (e.g., `https://aehtarohptmtrgksxhll.supabase.co`)
   - **anon/public key** (starts with `eyJ...`)

## Bot Code Implementation

### Add to your existing bot file:

```python
import discord
from discord import app_commands
from discord.ext import commands
import os
from supabase import create_client, Client
from datetime import datetime
import asyncio

# Initialize Supabase client
SUPABASE_URL = os.getenv("SUPABASE_URL")  # Add to your .env file
SUPABASE_KEY = os.getenv("SUPABASE_KEY")  # Add to your .env file
supabase: Client = create_client(SUPABASE_URL, SUPABASE_KEY)

# Role and settings
LINKED_ROLE_NAME = "Linked Account"  # Change this to match your role name
GUILD_ID = 1450671860520976559  # Your Discord server ID

class DiscordLinkCog(commands.Cog):
    def __init__(self, bot):
        self.bot = bot
    
    @app_commands.command(name="linkdiscord", description="Link your Minecraft account to Discord")
    @app_commands.describe(code="The 6-character code from Minecraft")
    async def link_discord(self, interaction: discord.Interaction, code: str):
        """Link Minecraft account using verification code"""
        await interaction.response.defer(ephemeral=True)
        
        code = code.upper().strip()
        discord_id = str(interaction.user.id)
        
        try:
            # Check if Discord account is already linked
            existing_link = supabase.table("discord_links").select("*").eq("discord_id", discord_id).execute()
            
            if existing_link.data and len(existing_link.data) > 0:
                await interaction.followup.send(
                    "❌ **Your Discord account is already linked!**\n"
                    f"Linked to: `{existing_link.data[0]['minecraft_username']}`\n\n"
                    "Use `/unlinkdiscord` to unlink first.",
                    ephemeral=True
                )
                return
            
            # Verify the code
            verification = supabase.table("verification_codes").select("*").eq("code", code).execute()
            
            if not verification.data or len(verification.data) == 0:
                await interaction.followup.send(
                    "❌ **Invalid or expired verification code!**\n\n"
                    "Make sure you:\n"
                    "1. Generated the code in Minecraft with `/linkdiscord`\n"
                    "2. Used the code within 10 minutes\n"
                    "3. Copied the code correctly (case-sensitive)",
                    ephemeral=True
                )
                return
            
            code_data = verification.data[0]
            minecraft_uuid = code_data["minecraft_uuid"]
            minecraft_username = code_data["minecraft_username"]
            
            # Check if Minecraft account is already linked to another Discord
            existing_minecraft = supabase.table("discord_links").select("*").eq("minecraft_uuid", minecraft_uuid).execute()
            
            if existing_minecraft.data and len(existing_minecraft.data) > 0:
                # Delete the old link (user is re-linking)
                supabase.table("discord_links").delete().eq("minecraft_uuid", minecraft_uuid).execute()
            
            # Create the link
            link_data = {
                "minecraft_uuid": minecraft_uuid,
                "minecraft_username": minecraft_username,
                "discord_id": discord_id,
                "discord_username": interaction.user.name,
                "discord_tag": f"{interaction.user.name}#{interaction.user.discriminator}" if interaction.user.discriminator != "0" else interaction.user.name
            }
            
            supabase.table("discord_links").insert(link_data).execute()
            
            # Delete the used verification code
            supabase.table("verification_codes").delete().eq("code", code).execute()
            
            # Update nickname
            try:
                await interaction.user.edit(nick=minecraft_username)
            except discord.Forbidden:
                pass  # Bot doesn't have permission or user is server owner
            
            # Assign role
            guild = interaction.guild
            linked_role = discord.utils.get(guild.roles, name=LINKED_ROLE_NAME)
            
            if linked_role:
                try:
                    await interaction.user.add_roles(linked_role)
                except discord.Forbidden:
                    pass
            
            # Success message
            embed = discord.Embed(
                title="🎉 Account Linked Successfully!",
                description=f"Your Discord account has been linked to **{minecraft_username}**",
                color=discord.Color.green()
            )
            embed.add_field(name="Minecraft Username", value=f"`{minecraft_username}`", inline=True)
            embed.add_field(name="Discord", value=f"{interaction.user.mention}", inline=True)
            embed.add_field(
                name="✅ Completed",
                value="• Nickname updated\n• Linked Account role assigned\n• Database updated",
                inline=False
            )
            embed.set_footer(text="Use /unlinkdiscord to unlink your accounts")
            
            await interaction.followup.send(embed=embed, ephemeral=True)
            
        except Exception as e:
            print(f"Error linking Discord: {e}")
            await interaction.followup.send(
                "❌ **An error occurred while linking your account.**\n"
                "Please try again or contact an administrator.",
                ephemeral=True
            )
    
    @app_commands.command(name="unlinkdiscord", description="Unlink your Minecraft account from Discord")
    async def unlink_discord(self, interaction: discord.Interaction):
        """Unlink Minecraft account"""
        await interaction.response.defer(ephemeral=True)
        
        discord_id = str(interaction.user.id)
        
        try:
            # Check if account is linked
            existing_link = supabase.table("discord_links").select("*").eq("discord_id", discord_id).execute()
            
            if not existing_link.data or len(existing_link.data) == 0:
                await interaction.followup.send(
                    "❌ **Your Discord account is not linked!**\n\n"
                    "Use `/linkdiscord <code>` to link your Minecraft account.",
                    ephemeral=True
                )
                return
            
            minecraft_username = existing_link.data[0]["minecraft_username"]
            
            # Delete the link
            supabase.table("discord_links").delete().eq("discord_id", discord_id).execute()
            
            # Remove role
            guild = interaction.guild
            linked_role = discord.utils.get(guild.roles, name=LINKED_ROLE_NAME)
            
            if linked_role and linked_role in interaction.user.roles:
                try:
                    await interaction.user.remove_roles(linked_role)
                except discord.Forbidden:
                    pass
            
            # Success message
            embed = discord.Embed(
                title="🔓 Account Unlinked",
                description=f"Your Discord account has been unlinked from **{minecraft_username}**",
                color=discord.Color.orange()
            )
            embed.add_field(
                name="✅ Completed",
                value="• Link removed from database\n• Linked Account role removed",
                inline=False
            )
            embed.set_footer(text="You can link again anytime with /linkdiscord")
            
            await interaction.followup.send(embed=embed, ephemeral=True)
            
        except Exception as e:
            print(f"Error unlinking Discord: {e}")
            await interaction.followup.send(
                "❌ **An error occurred while unlinking your account.**\n"
                "Please try again or contact an administrator.",
                ephemeral=True
            )

# Add this to your bot setup
async def setup(bot):
    await bot.add_cog(DiscordLinkCog(bot))

# If you're not using cogs, add these directly to your bot:
# bot.tree.add_command(link_discord)
# bot.tree.add_command(unlink_discord)
```

## Configuration

### 1. Update `.env` file:

```env
SUPABASE_URL=https://aehtarohptmtrgksxhll.supabase.co
SUPABASE_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImFlaHRhcm9ocHRtdHJna3N4aGxsIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Njc1MDc5MDMsImV4cCI6MjA4MzA4MzkwM30.yamLCaDF4teavO7zTQhsUoVOJBhE9WPnqA6rKc1zd0M
DISCORD_TOKEN=your-bot-token
```

### 2. Update Minecraft plugin:

Edit `DiscordLinkDb.java` and replace:
```java
private static final String SUPABASE_URL = "YOUR_SUPABASE_PROJECT_URL";
private static final String SUPABASE_KEY = "YOUR_SUPABASE_ANON_KEY";
```

With your actual Supabase URL and key.

## Testing

1. **In Minecraft:**
   - Run `/linkdiscord`
   - Copy the 6-character code

2. **In Discord:**
   - Run `/linkdiscord ABC123` (replace with your code)
   - Your nickname should change to your Minecraft username
   - You should get the "Linked Account" role

3. **Test whois:**
   - In Minecraft: `/whois YourUsername`
   - Should show Discord information

4. **Test unlinking:**
   - In Discord: `/unlinkdiscord`
   - In Minecraft: `/unlinkdiscord`
   - Both should work

## Troubleshooting

### Code not working?
- Codes expire after 10 minutes
- Codes are case-sensitive
- Check Supabase tables for data

### Role not assigning?
- Check bot has "Manage Roles" permission
- Bot's role must be higher than "Linked Account" role
- Role name must match exactly (case-sensitive)

### Nickname not changing?
- Bot needs "Manage Nicknames" permission
- Cannot change server owner's nickname
- Bot's role must be higher in hierarchy

### Commands not showing?
- Wait a few minutes for Discord to sync
- Try `/` and start typing the command
- Make sure bot was invited with `applications.commands` scope

## Need Help?

Check the logs:
- **Minecraft:** `logs/latest.log`
- **Discord Bot:** Your bot's console output
- **Supabase:** Dashboard > Database > Logs
