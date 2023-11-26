package discord.worldbosses;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.JDA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AlbionBot extends ListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(AlbionBot.class);
    private static Set<String> initializedGuilds = new HashSet<>();
    private static boolean commandsRegistered = true; // Flag to track if commands are registered
    private static Map<String, CommandListener> commandListeners = new HashMap<>();

    public static void main(String[] args) throws Exception {
        JDA jda = initializeBot();
        initializeServers(jda);
        registerSlashCommands(jda);
    }

    private static JDA initializeBot() throws Exception {
        String token = ConfigManager.getBotToken();
        JDABuilder builder = JDABuilder.createDefault(token);
        builder.enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT);
        builder.setActivity(Activity.watching("World Bosses"));
        builder.addEventListeners(new AlbionBot());
        JDA jda = builder.build();
        jda.awaitReady();
        return jda;
    }

    private static void initializeServers(JDA jda) {
        for (Guild guild : jda.getGuilds()) {
            if (!initializedGuilds.contains(guild.getId())) {
                initializeForGuild(jda, guild.getId());
                initializedGuilds.add(guild.getId()); // Mark this guild as initialized
            }
        }
    }

    private static void registerSlashCommands(JDA jda) {
        if (!commandsRegistered) { // Check if commands are already registered
            new SlashCommandRegistrar(jda).registerCommands();
            commandsRegistered = true; // Set the flag to true
        }
    }

    private static void initializeForGuild(JDA jda, String serverId) {
        logger.info("Initializing for server ID: {}", serverId);

        if (!commandListeners.containsKey(serverId)) {
            logger.info("Creating new CommandListener for server ID: {}", serverId);

            BossManager bossManager = new BossManager(serverId);
            String designatedChannelId = ConfigManager.getDesignatedChannelId(serverId);
            CommandListener commandListener = new CommandListener(jda, designatedChannelId, serverId, bossManager);
            jda.addEventListener(commandListener);
            commandListeners.put(serverId, commandListener);
            logger.info("Listener initialized for server ID: {}", serverId);
        } else {
            logger.info("Listener already registered for server ID: {}", serverId);
        }
    }

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        String serverId = event.getGuild().getId();

        // Check if the guild is already initialized
        if (!initializedGuilds.contains(serverId)) {
            createServerFiles(serverId);
            sendWelcomeMessageIfPossible(event, serverId);
            initializeForGuild(event.getJDA(), serverId);
            initializedGuilds.add(serverId); // Mark this guild as initialized
        }
    }

    private void createServerFiles(String serverId) {
        createFile("data/" + serverId + "/", "timers.json");
        createFile("data/" + serverId + "/", "config.json");
    }

    private void createFile(String directory, String fileName) {
        File file = new File(directory + fileName);
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                logger.error("Failed to create file: " + directory + fileName, e);
            }
        }
    }

    @Override
    public void onGuildLeave(GuildLeaveEvent event) {
        String serverId = event.getGuild().getId();
        initializedGuilds.remove(serverId);
        commandListeners.remove(serverId);
        logger.info("Left server ID: {}", serverId);
    }

    private void sendWelcomeMessageIfPossible(GuildJoinEvent event, String serverId) {
        if (event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
            event.getGuild().retrieveAuditLogs().type(ActionType.BOT_ADD).queue(auditLogEntries -> {
                if (!auditLogEntries.isEmpty()) {
                    String inviterId = auditLogEntries.get(0).getUser().getId();
                    event.getJDA().retrieveUserById(inviterId).queue(inviter -> {
                        inviter.openPrivateChannel().queue(privateChannel -> {
                            privateChannel.sendMessage(getWelcomeMessage()).queue(
                                    success -> logger.info("Message sent to inviter successfully."),
                                    error -> logger.error("Failed to send message to inviter.", error));
                        });
                    });
                }
            }, error -> logger.warn("Failed to retrieve audit logs.", error));
        } else {
            logger.warn("Bot does not have VIEW_AUDIT_LOGS permission in server: {}", serverId);
        }
    }

    private String getWelcomeMessage() {
        return "Thank you for inviting me to your server! Here's some important links:\n" +
                "Support the server: https://www.buymeacoffee.com/Veyg\n" +
                "Documentation: https://veyg.me/worldbossbot/\n" +
                "Discord for support: Soon \n" +
                "GitHub repo: https://github.com/Veyg/Albion-WB-timer-bot\n" +
                "Feel free to reach out if you have any questions or need assistance. Enjoy using the bot!";
    }
}
