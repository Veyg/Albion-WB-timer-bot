package discord.worldbosses;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class AlbionBot {
    public static void main(String[] args) throws Exception {
        String token = "MTE0NTY3MTY3NjkwMjc4NTA4NA.GqwMqw.R8ZKeByKpZX4UIp0oqch9msZ8G74lSXFdK0MEA"; // Replace with your bot token
        JDABuilder builder = JDABuilder.createDefault(token);
        builder.enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT);
        builder.setActivity(Activity.playing("Tracking Albion Bosses"));
        builder.addEventListeners(new CommandListener());

        builder.build();
    }
}
