package discord.worldbosses;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.JDA;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AlbionBot extends ListenerAdapter {
    public static void main(String[] args) throws Exception {
        String token = ConfigManager.getBotToken();
        JDABuilder builder = JDABuilder.createDefault(token);
        builder.enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT);
        builder.setActivity(Activity.watching("World Bosses"));

        // Add the AlbionBot instance as an event listener
        builder.addEventListeners(new AlbionBot());

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

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        String serverId = event.getGuild().getId();
        String serverDataDir = "data/" + serverId + "/";
    
        // Create a new timers.json file for this server if it doesn't exist
        File serverTimersFile = new File(serverDataDir + "timers.json");
        if (!serverTimersFile.exists()) {
            serverTimersFile.getParentFile().mkdirs();
            try {
                if (serverTimersFile.createNewFile()) {
                    // Optionally, initialize the timers.json file with default data here
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    
        // Create a new config.json file for this server if it doesn't exist
        File serverConfigFile = new File(serverDataDir + "config.json");
        if (!serverConfigFile.exists()) {
            try {
                if (serverConfigFile.createNewFile()) {
                    // Optionally, initialize the config.json file with default data here
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    
        // When the bot joins a new server, initialize BossManager and CommandListener    
        // Set up the CommandListener for this server
        String designatedChannelId = ConfigManager.getDesignatedChannelId(serverId);
        CommandListener commandListener = new CommandListener(event.getJDA(), designatedChannelId, serverId);
        event.getJDA().addEventListener(commandListener);
    }
    
}
