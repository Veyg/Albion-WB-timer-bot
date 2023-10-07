package discord.worldbosses;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.JDA;

public class AlbionBot {
    public static void main(String[] args) throws Exception {
        String token = ConfigManager.getBotToken();
        JDABuilder builder = JDABuilder.createDefault(token);
        builder.enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT);
        builder.setActivity(Activity.playing("Infactor needs regears for ZvZ"));

        JDA jda = builder.build();
        jda.awaitReady();

        // Retrieve the server ID dynamically from the first guild (server) that the bot
        // is in.
        String serverId = jda.getGuilds().isEmpty() ? "defaultServerId" : jda.getGuilds().get(0).getId();

        String designatedChannelId = ConfigManager.getDesignatedChannelId(serverId);
        CommandListener commandListener = new CommandListener(jda, designatedChannelId, serverId);

        // Add the command listener using the commandListener variable
        jda.addEventListener(commandListener);

        /******** This is only needed when you want to register commands. ********/
        new SlashCommandRegistrar(jda).registerCommands();
    }
}
