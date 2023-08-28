package discord.worldbosses;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;

public class AlbionBot {
    public static void main(String[] args) throws Exception {
        String token = "MTE0NTY3MTY3NjkwMjc4NTA4NA.G1PTSn.lOpPgx6POdwXi96Degh0aWtRlvP96UNlsWT0k8"; // Replace with your bot token

        JDABuilder builder = JDABuilder.createDefault(token);
        builder.setActivity(Activity.playing("Tracking Albion Bosses"));
        builder.addEventListeners(new CommandListener());

        builder.build();
    }
}
