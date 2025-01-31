# Phantasma_Iran
Telegram Group Management Bot
# Configuration
Configure
Edit the following parameters in /src/resources/application.properties
- bot.token -> Add your bot token from https://t.me/BotFather
- bot.whitelist -> Add the Telegram user IDs of the administrators who are allowed to interact with the bot separated by commas. TG user IDs are numerical and can be obtained from https://t.me/userinfobot
- bot.chat -> Add the Telegram chat IDs of the groups the bot should monitor including the verification group separated by commas. To get Telegram group IDs do the following:
- bot.verification -> This is the group where you choose whether to blacklist posted content and ban offending users, ignore a deleted post or whitelist the user who posted the deleted content, allowing him/her to post images, links etc.

1) Add the Telegram BOT to the group.
2) Get the list of updates for your BOT by visiting the following URL (replace with the token you got from BotFather):
https://api.telegram.org/bot<YourBOTToken>/getUpdates
Example: https://api.telegram.org/bot123456789:jbd78sadvbdy63d37gda37bd8/getUpdates
3) Look for the "chat" object in the following:
{"update_id":8393,"message":{"message_id":3,"from":{"id":7474,"first_name":"AAA"},"chat":{"id":,"title":""},"date":25497,"new_chat_participant":{"id":71,"first_name":"NAME","username":"YOUR_BOT_NAME"}}}
4) Add chat id for bot.chat parameter

# Installation
Clone this Repository and change dir to inside the cloned repository
# Run
mvn spring-boot:run
# Java version
Open JDK 11
# Commands
The following are forwarded from new user posts to the Bot Chat for review to ban or whitelist the user posting:
- Links
- Images
- Files

When a user is whitelisted, he/she can post the above (links/images/files) unless another bot og group wide settings prohibit this.

Bulk whitelisting - To avoid unnecessary post deletion and quickly whitelist regular users in the chat after initializing the bot, do the following:
- Forward a post from the user you want to whitelist to the Bot Chat. The bot will then instantly whitelist the user.
- This can also be done in bulk (ie forward 50 posts from different users to Bot Chat to whitelist all those users).
- If a user has previously been added to the bot database (for example due to being !warned, see below) this method will not work - use the !trust command (see below) in these instances.

If links are manually confirmed in the Bot Chat as scam links, the Bot will autodelete and ban any other users posting the same link

The following commands can be used as a reply to a any user post:
- !warn - warns user (count of 3, bans user on third warn)
- !forgive - removes warns
- !mute - mutes user permanently
- !mute1 - mutes user for 1 hour
- !mute24 - mutes user for 24 hours
- !mute48 - mutes user for 48 hours
- !unmute - unmutes muted user
- !unban - unbans banned user
- !trust - allows monitored new member to post links and images before the 10 day period is up

The commands are flexible, can be part of a sentence and can also be contained within a word. Examples:
- "Sorry mate, we've been over this - consider yourself !warned"
- "That's enough, you've been !muted mate"
- "It's ok, you've been !trusted - try posting that again"
