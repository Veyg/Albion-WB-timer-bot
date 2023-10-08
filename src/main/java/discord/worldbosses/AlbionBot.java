package discord.worldbosses;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.JDA;

import java.util.HashMap;
import java.util.Map;

public class AlbionBot {
    public static void main(String[] args) throws Exception {
        String token = ConfigManager.getBotToken();
        JDABuilder builder = JDABuilder.createDefault(token);
        builder.enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT);
        builder.setActivity(Activity.watching("World Bosses"));

        JDA jda = builder.build();
        jda.awaitReady();

        // Initialize BossManager for each server the bot is in
        Map<String, BossManager> bossManagers = new HashMap<>();
        for (Guild guild : jda.getGuilds()) {
            String serverId = guild.getId();
            bossManagers.put(serverId, new BossManager(serverId));

            // Set up the CommandListener for this server
            String designatedChannelId = ConfigManager.getDesignatedChannelId(serverId);
            CommandListener commandListener = new CommandListener(jda, designatedChannelId, serverId);
            jda.addEventListener(commandListener);
        }

        /******** This is only needed when you want to register commands. ********/
        // new SlashCommandRegistrar(jda).registerCommands();
    }
}
