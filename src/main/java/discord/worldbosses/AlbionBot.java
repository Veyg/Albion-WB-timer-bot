package discord.worldbosses;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.JDA;

public class AlbionBot {
    public static void main(String[] args) throws Exception {
        String token = "MTE0NTY3MTY3NjkwMjc4NTA4NA.GqwMqw.R8ZKeByKpZX4UIp0oqch9msZ8G74lSXFdK0MEA"; // Replace with your bot token
        JDABuilder builder = JDABuilder.createDefault(token);
        builder.enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT);
        builder.setActivity(Activity.playing("Infactor needs regears for ZvZ"));

        JDA jda = builder.build();
        jda.awaitReady();

        String designatedChannelId = ConfigManager.getDesignatedChannelId();
        CommandListener commandListener = new CommandListener(jda, designatedChannelId);

        // Add the command listener using the commandListener variable
        jda.addEventListener(commandListener);
            
        /******** This is only needed when you want to register commands. ********/
        new SlashCommandRegistrar(jda).registerCommands(); 
    }
}
