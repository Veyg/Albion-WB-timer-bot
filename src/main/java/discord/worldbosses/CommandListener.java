package discord.worldbosses;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenuInteraction;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonInteraction;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import discord.worldbosses.BossManager.TimerData;
import java.awt.Color;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

public class CommandListener extends ListenerAdapter {
    private BossManager bossManager;
    private Map<String, String> userStates = new HashMap<>();
    private String timerMessageId;
    private JDA jda;
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    // private Set<String> sentNotifications = new HashSet<>();
    private String serverId;
    private Map<String, List<String>> bossNotificationMessages = new HashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(CommandListener.class);

    public CommandListener(JDA jda, String designatedChannelId, String serverId) {
        this.jda = jda;
        this.serverId = serverId; // Store the serverId
        this.bossManager = new BossManager(serverId);

        startPeriodicCheck();
    }
    private void logAction(String action, String username, String details) {
        String logEntry = String.format("User: %s, Action: %s, Details: %s", username, action, details);
        logger.info(logEntry);
    }

    private void scheduleMessageDeletion(Message message, long delayMillis) {
        new java.util.Timer().schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                message.delete().queue();
            }
        }, delayMillis);
    }

    private void startPeriodicCheck() {
        scheduler.scheduleAtFixedRate(() -> {
            LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
            for (Map.Entry<String, TimerData> entry : bossManager.getAllTimers().entrySet()) {
                LocalDateTime bossSpawnTime = LocalDateTime.parse(entry.getValue().getBossSpawnTime(),
                        DateTimeFormatter.ofPattern("HH:mm:ss d/MM/yyyy"));
                long minutesUntilSpawn = ChronoUnit.MINUTES.between(now, bossSpawnTime);

                // Check if the boss has been marked as skipped or forgotten
                if (!bossManager.isSkippedOrForgotten(entry.getKey())) {
                    // Send a notification only once, 30 minutes before spawn
                    if (minutesUntilSpawn == 30) {
                        // Check if we have already sent a notification for this boss
                        if (!bossNotificationMessages.containsKey(entry.getKey())) {
                            sendBossNotification(serverId, entry.getKey(), entry.getValue().getBossSpawnTime(),
                                    minutesUntilSpawn);
                        }
                    }
                }
            }
        }, 0, 1, TimeUnit.MINUTES); // Check every minute
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getGuild().getId().equals(this.serverId)) {
            return; // Ignore events from other servers
        }
        String currentDesignatedChannelId = ConfigManager.getDesignatedChannelId(event.getGuild().getId());

        // Check if the command is setdesignatedchannel and if the user is an admin
        if (event.getName().equals("setdesignatedchannel")) {
            if (event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
                handleSetDesignatedChannel(event);
            } else {
                event.reply("You need administrator permissions to set the designated channel.")
                        .setEphemeral(true).queue();
            }
            return;
        }

        // Allow /aboutme and /help to be accessible even outside the designated channel
        if (event.getName().equals("aboutme")) {
            handleAboutMe(event);
            return;
        }

        if (event.getName().equals("help")) {
            handleHelp(event);
            return;
        }

        // For all other commands, check if they are used in the designated channel
        if (!event.getChannel().getId().equals(currentDesignatedChannelId)) {
            event.reply("Don't use it here 🤓").setEphemeral(true).queue();
            return; // Ignore interactions outside the designated channel
        }

        switch (event.getName()) {
            case "addtimer":
                handleAddTimer(event);
                break;
            case "deletetimer":
                handleDeleteTimer(event);
                break;
            case "edittimer":
                handleEditTimer(event);
                break;
            default:
                event.reply("Unknown command.").setEphemeral(true).queue();
        }
    }

    private void handleEditTimer(SlashCommandInteractionEvent event) {
        OptionMapping mapNameOption = event.getOption("map");
        OptionMapping newTimeOption = event.getOption("newtime");
        OptionMapping newDateOption = event.getOption("newdate");

        if (mapNameOption == null || newTimeOption == null || newDateOption == null) {
            event.reply("Required options were not provided. Please ensure you provide mapname, newtime, and newdate.")
                    .setEphemeral(true).queue();
            return;
        }
        
        String mapName = mapNameOption.getAsString();
        String newTimeInput = newTimeOption.getAsString();
        String newDateInput = newDateOption.getAsString();
        OptionMapping noteOption = event.getOption("note");
        String note = (noteOption != null) ? noteOption.getAsString() : "";

        // Validate the time format
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        LocalTime parsedNewTime;
        try {
            parsedNewTime = LocalTime.parse(newTimeInput, timeFormatter);
        } catch (DateTimeParseException e) {
            event.reply("Invalid time format for new time. Please use HH:mm:ss format.").setEphemeral(true).queue();
            return;
        }

        // Validate the date format
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("d/MM/yyyy");
        LocalDate parsedNewDate;
        try {
            parsedNewDate = LocalDate.parse(newDateInput, dateFormatter);
        } catch (DateTimeParseException e) {
            event.reply("Invalid date format for new date. Please use d/MM/yyyy format.").setEphemeral(true).queue();
            return;
        }

        // Combine the date and time to form the full new time
        LocalDateTime combinedDateTime = LocalDateTime.of(parsedNewDate, parsedNewTime);
        String fullNewTime = combinedDateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss d/MM/yyyy"));
        String username = event.getUser().getName(); // Get the username of the person who triggered the event
        String details = "Edited timer for map: " + mapName + " to " + newTimeInput + " " + newDateInput;
        logAction("Edit timer", username, details);
        // Update the timer using BossManager
        bossManager.editTimer(mapName, fullNewTime, note);
        sendTimersToChannel(serverId);
        // Provide feedback to the user
        String replyMessage = "Timer for " + mapName + " has been updated to " + fullNewTime;
        if (!note.isEmpty()) {
            replyMessage += " with note: " + note;
        }
        event.reply(replyMessage).queue(response -> {
            response.retrieveOriginal().queue(originalMessage -> {
                scheduleMessageDeletion(originalMessage, 7200000);
            });
        });
    }

    private void handleDeleteTimer(SlashCommandInteractionEvent event) {
        OptionMapping mapNameOption = event.getOption("map"); // Changed from "mapn" to "map"

        if (mapNameOption == null) {
            event.reply("Required option 'map' was not provided.").setEphemeral(true).queue();
            return;
        }

        String mapName = mapNameOption.getAsString();
        String username = event.getUser().getName(); // Get the username of the person who triggered the event
        String details = "Deleted timer for map: " + mapName;
        logAction("Delete Timer", username, details);
        // Check if the timer exists before deleting
        if (bossManager.getAllTimers().containsKey(mapName)) {
            bossManager.deleteTimer(mapName);
            sendTimersToChannel(serverId);
            event.reply("Timer for " + mapName + " has been deleted.").queue();
        } else {
            event.reply("Failed to delete timer for " + mapName + ". It might not exist.").setEphemeral(true).queue();
        }
    }

    private void handleAddTimer(SlashCommandInteractionEvent event) {
        String timeInput = event.getOption("time").getAsString();
        String mapName = event.getOption("map").getAsString();
        OptionMapping dateOption = event.getOption("date");
        OptionMapping noteOption = event.getOption("note");
        String note = noteOption != null ? noteOption.getAsString() : "";
        
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        LocalTime parsedTime;
        try {
            parsedTime = LocalTime.parse(timeInput, timeFormatter);
        } catch (DateTimeParseException e) {
            event.reply("Invalid time format. Please use HH:mm:ss format.").setEphemeral(true).queue();
            return;
        }

        // Ensure the time is always formatted as "HH:mm:ss"
        String formattedTime = String.format("%02d:%02d:%02d", parsedTime.getHour(), parsedTime.getMinute(),
                parsedTime.getSecond());

        LocalDate date;
        if (dateOption != null) {
            // Parse the provided date
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            try {
                date = LocalDate.parse(dateOption.getAsString(), dateFormatter);
            } catch (DateTimeParseException e) {
                event.reply("Invalid date format. Please use dd/MM/yyyy format.").setEphemeral(true).queue();
                return;
            }
        } else {
            // Default to the current date plus two days
            date = LocalDate.now(ZoneOffset.UTC).plusDays(2);
        }

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String formattedDate = date.format(dateFormatter);

        String fullTime = formattedTime + " " + formattedDate;
        String username = event.getUser().getName(); // Get the username of the person who triggered the event
        String details = "Added timer for map: " + mapName + " at " + fullTime;
        logAction("Add Timer", username, details);
        // Store the timer using BossManager
        bossManager.addTimer(mapName, fullTime, note);
        bossManager.saveTimers();
        sendTimersToChannel(serverId);
        // Provide feedback to the user
        String replyMessage = "Timer added for " + mapName + " at " + fullTime;
        if (!note.isEmpty()) {
            replyMessage += " with note: " + note;
        }
        event.reply(replyMessage)
                .queue(response -> {
                    response.retrieveOriginal().queue(originalMessage -> {
                        scheduleMessageDeletion(originalMessage, 7200000);
                    });
                });
    }

    @Override
    public void onGenericInteractionCreate(GenericInteractionCreateEvent event) {
        if (!event.getGuild().getId().equals(this.serverId)) {
            return; // Ignore events from other servers
        }
        String currentDesignatedChannelId = ConfigManager.getDesignatedChannelId(event.getGuild().getId());
        if (!event.getChannel().getId().equals(currentDesignatedChannelId)) {
            return; // Ignore interactions outside the designated channel
        }
        this.jda = event.getJDA();
        if (event.getInteraction() instanceof ButtonInteraction) {
            onButtonInteraction((ButtonInteraction) event.getInteraction());
        } else if (event.getInteraction() instanceof SelectMenuInteraction<?, ?>) {
            SelectMenuInteraction<?, ?> selectMenu = (SelectMenuInteraction<?, ?>) event.getInteraction();
            String userId = selectMenu.getUser().getId();

            String customId = selectMenu.getComponentId();

            if (customId.equals("map-selector") && "awaiting_map_selection_for_add".equals(userStates.get(userId))) {
                String selectedMap = (String) selectMenu.getValues().get(0);
                String fullTime = userStates.get(userId + "_input_time") + " " + userStates.get(userId + "_input_date");

                // Retrieve the note from userStates if you have stored it there previously
                String note = userStates.containsKey(userId + "_input_note") ? userStates.get(userId + "_input_note")
                        : "";

                // Now pass the note when adding the timer
                bossManager.addTimer(selectedMap, fullTime, note);
                selectMenu.getChannel()
                        .sendMessage("Timer added for " + selectedMap + " at " + fullTime)
                        .queue(response -> {
                            response.delete().queueAfter(10, TimeUnit.SECONDS, null, throwable -> {
                                if (throwable instanceof ErrorResponseException) {
                                    logger.error("Error deleting message with ID: " + response.getId()
                                            + ". Error: " + throwable.getMessage());
                                }
                            });
                        });
                userStates.remove(userId);
                userStates.remove(userId + "_input_time");
                userStates.remove(userId + "_input_date");
                selectMenu.getMessage().delete().queue();

                sendTimersToChannel(serverId);
            } else if (customId.equals("map-selector") && "awaiting_map_selection".equals(userStates.get(userId))) {
                // This is for other functionalities that might require map selection without
                // adding a timer
            }
        }
    }

    private void handleSetDesignatedChannel(SlashCommandInteractionEvent event) {
        if (event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            String currentDesignatedChannelId = event.getChannel().getId();

            // Save the designated channel ID to the config file
            ConfigManager.setDesignatedChannelId(event.getGuild().getId(), currentDesignatedChannelId);

            event.reply("This channel is now set as the designated channel for timers.")
                    .queue(response -> {
                        response.deleteOriginal().queueAfter(10, TimeUnit.SECONDS, null, throwable -> {
                            if (throwable instanceof ErrorResponseException) {
                                logger.error("Error deleting message with ID: " + event.getInteraction().getId()
                                        + ". Error: " + throwable.getMessage());
                            }
                        });
                    });
        } else {
            event.reply("You need administrator permissions to set the designated channel.")
                    .queue(response -> {
                        response.deleteOriginal().queueAfter(10, TimeUnit.SECONDS, null, throwable -> {
                            if (throwable instanceof ErrorResponseException) {
                                logger.error("Error deleting message with ID: " + event.getInteraction().getId()
                                        + ". Error: " + throwable.getMessage());
                            }
                        });
                    });
        }
        sendTimersToChannel(serverId);
    }

    public void sendBossNotification(String guildId, String mapName, String time, long minutesUntilSpawn) {
        String currentDesignatedChannelId = ConfigManager.getDesignatedChannelId(guildId);
        if (currentDesignatedChannelId == null)
            return;
        TextChannel designatedChannel = jda.getTextChannelById(currentDesignatedChannelId);
        if (designatedChannel == null)
            return;

        // Unique key for the notification
        String notificationKey = mapName + "_" + time;

        // Check if the notification for this boss at this time has already been sent
        if (bossNotificationMessages.containsKey(notificationKey)) {
            return; // If it has been sent, do nothing
        }

        Button killedButton = Button.primary("boss_killed", "Killed").withEmoji(Emoji.fromUnicode("🐘"));
        Button skippedButton = Button.secondary("boss_skipped", "Skipped").withEmoji(Emoji.fromUnicode("🕣"));
        Button forgotButton = Button.danger("boss_forgot", "Forgot").withEmoji(Emoji.fromUnicode("❓"));

        designatedChannel.sendMessage("@everyone\n**WORLD BOSS SPAWNING SOON**\nMap: " + mapName + "\nTime: " + time)
                .setActionRow(killedButton, skippedButton, forgotButton)
                .queue(message -> {
                    // Store the message ID for this boss
                    bossNotificationMessages.put(mapName, Arrays.asList(message.getId()));
                });
    }

    public void onButtonInteraction(ButtonInteraction event) {
        String username = event.getUser().getName();
        String mapName = extractMapNameFromMessage(event.getMessage().getContentRaw());    
        switch (event.getComponentId()) {
            case "boss_killed":
                handleBossKilled(event);
                logAction("Boss Killed", username, "Map: " + mapName);
                break;
            case "boss_skipped":
                event.getMessage().delete().queue();
                String mapNameSkipped = extractMapNameFromMessage(event.getMessage().getContentRaw());
                bossManager.markBossAsSkipped(mapNameSkipped);
                logAction("Boss Skipped", username, "Map: " + mapName);
                sendTimersToChannel(serverId);
                deleteAllNotificationsForBoss(mapNameSkipped);
                break;
            case "boss_forgot":
                event.getMessage().delete().queue();
                String mapNameForgot = extractMapNameFromMessage(event.getMessage().getContentRaw());
                bossManager.markBossAsForgotten(mapNameForgot);
                event.reply("Boss was forgotten!");
                logAction("Boss Forgot", username, "Map: " + mapName);
                sendTimersToChannel(serverId);
                deleteAllNotificationsForBoss(mapNameForgot);
                break;

            default:
                break;
        }
    }

    void sendTimersToChannel(String guildId) {
        // Fetch the designatedChannelId dynamically
        String currentDesignatedChannelId = ConfigManager.getDesignatedChannelId(guildId);
        if (currentDesignatedChannelId == null) {
            logger.info("Designated channel ID is null.");
            return; // No designated channel set
        }

        TextChannel designatedChannel = jda.getTextChannelById(currentDesignatedChannelId);
        if (designatedChannel == null) {
            logger.info("Designated channel not found.");
            return; // Designated channel not found
        }

        List<Map.Entry<String, TimerData>> sortedTimers = bossManager.getSortedTimers();
        if (sortedTimers.isEmpty()) {
            logger.info("No timers to send.");
            return; // No timers to send
        }

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("World Boss Timers");
        embed.setColor(0x00FFFF);

        // Create lists for upcoming, skipped/forgotten, and all timers
        List<Map.Entry<String, TimerData>> upcomingTimers = new ArrayList<>();
        List<Map.Entry<String, TimerData>> skippedForgottenTimers = new ArrayList<>();

        for (Map.Entry<String, TimerData> entry : sortedTimers) {
            TimerData timerData = entry.getValue();

            if ("Skipped".equals(timerData.getStatus()) || "Forgotten".equals(timerData.getStatus())) {
                // Add to skipped/forgotten section
                skippedForgottenTimers.add(entry);
            } else {
                LocalDateTime bossSpawnTime = LocalDateTime.parse(timerData.getBossSpawnTime(),
                        DateTimeFormatter.ofPattern("HH:mm:ss d/MM/yyyy"));

                // Calculate the time difference in hours
                long hoursUntilSpawn = Duration.between(LocalDateTime.now(ZoneOffset.UTC), bossSpawnTime).toHours();

                if (hoursUntilSpawn <= 12) {
                    // Add to upcoming section if within 12 hours
                    upcomingTimers.add(entry);
                }
            }
        }

        // Add "Coming up" section
        if (!upcomingTimers.isEmpty()) {
            StringBuilder comingUpBuilder = new StringBuilder();
            for (Map.Entry<String, TimerData> nextTimer : upcomingTimers) {
                LocalDateTime nextSpawnTime = LocalDateTime.parse(nextTimer.getValue().getBossSpawnTime(),
                        DateTimeFormatter.ofPattern("HH:mm:ss d/MM/yyyy"));
                comingUpBuilder.append("**")
                        .append(nextTimer.getKey())
                        .append(" - ")
                        .append(nextSpawnTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")))
                        .append(" UTC ")
                        .append(nextSpawnTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

                // Check if there is a note and append it
                String note = nextTimer.getValue().getNote();
                if (note != null && !note.isEmpty()) {
                    comingUpBuilder.append(" - ").append(note);
                }

                comingUpBuilder.append("**\n");
            }
            // Insert the "Coming up" section at the beginning of the embed
            embed.addField("🚨 Coming up:", comingUpBuilder.toString(), false);
        }

        // Add main timers section
        for (Map.Entry<String, TimerData> entry : sortedTimers) {
            String bossName = entry.getKey();
            TimerData timerData = entry.getValue();

            if (!"Skipped".equals(timerData.getStatus()) && !"Forgotten".equals(timerData.getStatus())) {
                LocalDateTime bossSpawnTime = LocalDateTime.parse(timerData.getBossSpawnTime(),
                        DateTimeFormatter.ofPattern("HH:mm:ss d/MM/yyyy"));

                // Calculate the time difference in hours
                long hoursUntilSpawn = Duration.between(LocalDateTime.now(ZoneOffset.UTC), bossSpawnTime).toHours();

                if (hoursUntilSpawn > 12) {
                    // Add to main timers section
                    String fieldText = bossSpawnTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")) + " UTC "
                            + bossSpawnTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                    // Check if there is a note and append it
                    if (timerData.getNote() != null && !timerData.getNote().isEmpty()) {
                        fieldText += " - " + timerData.getNote();
                    }
                    embed.addField(bossName, fieldText, false);
                }
            }
        }

        // Add "Skipped / Forgotten" section
        if (!skippedForgottenTimers.isEmpty()) {
            StringBuilder skippedForgottenBuilder = new StringBuilder();
            for (Map.Entry<String, TimerData> entry : skippedForgottenTimers) {
                String bossName = entry.getKey();
                TimerData timerData = entry.getValue();
                skippedForgottenBuilder.append(bossName)
                        .append(" - ")
                        .append(timerData.getBossSpawnTime())
                        .append("\n");
            }
            embed.addField("🕣 Skipped / Forgotten:", skippedForgottenBuilder.toString(), false);
        }

        // First, delete the previous message if it exists
        if (timerMessageId != null) {
            designatedChannel.deleteMessageById(timerMessageId).queue(success -> {
                // After deleting the old message, send a new one
                designatedChannel.sendMessageEmbeds(embed.build()).queue(message -> {
                    // Update the stored message ID with the new message's ID
                    timerMessageId = message.getId();
                });
            }, failure -> {
                // If something goes wrong with deletion, log the error and try to send a new
                // message anyway
                logger.error("Error deleting message: " + failure.getMessage());
                designatedChannel.sendMessageEmbeds(embed.build()).queue(message -> {
                    // Update the stored message ID with the new message's ID
                    timerMessageId = message.getId();
                });
            });
        } else {
            // If there is no previous message, just send a new one
            designatedChannel.sendMessageEmbeds(embed.build()).queue(message -> {
                // Update the stored message ID with the new message's ID
                timerMessageId = message.getId();
            });
        }
    }

    private String extractMapNameFromMessage(String messageContent) {
        String[] lines = messageContent.split("\n");
        for (String line : lines) {
            if (line.startsWith("Map: ")) {
                return line.split(": ")[1].trim();
            }
        }
        return null;
    }

    private void handleBossKilled(ButtonInteraction event) {
        // Extract map name from the message content
        String mapName = extractMapNameFromMessage(event.getMessage().getContentRaw());
        userStates.put(event.getUser().getId(), "awaiting_killed_time");
        userStates.put(event.getUser().getId() + "_mapName", mapName);
        event.reply("Enter the time the boss was killed in HH:mm:ss format.").setEphemeral(true).queue();
        deleteAllNotificationsForBoss(mapName);
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.isFromGuild()) {
            return; // Ignore messages from DMs
        }
        if (!event.getGuild().getId().equals(this.serverId)) {
            return; // Ignore events from other servers
        }
        String userId = event.getAuthor().getId();
        if ("awaiting_killed_time".equals(userStates.get(userId))) {
            String input = event.getMessage().getContentRaw().trim(); // Trim the input
            logger.info("Received killed time: " + input); // Debug log
            String mapName = userStates.get(userId + "_mapName");

            // Schedule deletion of user's message after 2 hours
            scheduleMessageDeletion(event.getMessage(), 7200000);

            // Split the input into time and note parts
            String[] parts = input.split(" ", 2);
            String timeInput = parts[0];
            String note = parts.length > 1 ? parts[1] : ""; // If there's a note, take it, otherwise it's an empty
                                                            // string

            // Validate the time format with explicit locale
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.US);
            try {
                LocalTime parsedTime = LocalTime.parse(timeInput, timeFormatter);

                // Add two days to the current date
                LocalDate twoDaysLater = LocalDate.now(ZoneOffset.UTC).plusDays(2);
                LocalDateTime combinedDateTime = LocalDateTime.of(twoDaysLater, parsedTime);
                String fullNewTime = combinedDateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss d/MM/yyyy"));

                // Pass the note along with the time to the bossManager
                bossManager.markBossAsKilled(mapName, fullNewTime, note); // Ensure bossManager can handle the note
                event.getChannel()
                        .sendMessage("Boss killed! Timer for " + mapName + " has been updated to " + fullNewTime
                                + (note.isEmpty() ? "" : " with note: " + note))
                        .queue(response -> scheduleMessageDeletion(response, 7200000));

                // Clear the user's state
                userStates.remove(userId);
                userStates.remove(userId + "_mapName");
            } catch (DateTimeParseException e) {
                logger.error("Error parsing time: " + e.getMessage()); // Debug log
                event.getChannel().sendMessage("Invalid time format. Please use HH:mm:ss format. Enter the time again.")
                        .queue(response -> scheduleMessageDeletion(response, 30000));

                // Keep the user in the "awaiting_killed_time" state so they can try again
            }
            sendTimersToChannel(serverId);
        }
    }

    private void deleteAllNotificationsForBoss(String mapName) {
        TextChannel designatedChannel = jda.getTextChannelById(ConfigManager.getDesignatedChannelId(serverId));
        if (designatedChannel == null)
            return;

        List<String> messageIds = bossNotificationMessages.get(mapName);
        if (messageIds != null) {
            for (String messageId : messageIds) {
                designatedChannel.deleteMessageById(messageId).queue(null, throwable -> {
                    if (throwable instanceof ErrorResponseException) {
                        logger.error(
                                "Error deleting message with ID: " + messageId + ". Error: " + throwable.getMessage());
                    }
                });
            }
            // Clear the message IDs for this boss
            bossNotificationMessages.remove(mapName);
        }
    }

    private void handleAboutMe(SlashCommandInteractionEvent event) {
        String version = readVersionFromFile();

        EmbedBuilder embed = new EmbedBuilder();

        embed.setTitle("About AlbionBot");
        embed.setDescription(
                "I'm AlbionBot, designed to assist you with world bosses in Albion Online! For more information visit my website.");
        embed.addField("Current Version", version, false);
        embed.addField("Website", "[Bot's Website](https://veyg.me/worldbossbot/)", false);
        embed.addField("Github", "[Github](https://github.com/Veyg/Albion-WB-timer-bot)", false);
        embed.addField("Support Me", "[Buy me a coffee](https://www.buymeacoffee.com/veyg)", false);
        embed.addField("Discord support link", "[Discord](https://discord.gg/QqRC8vnaeZ)", false);
        embed.setColor(Color.CYAN);
        embed.setThumbnail(event.getJDA().getSelfUser().getAvatarUrl());

        event.replyEmbeds(embed.build()).setEphemeral(true).queue();
    }

    private void handleHelp(SlashCommandInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder();

        embed.setTitle("World Boss Bot Help");
        embed.setDescription(
                "A Discord bot designed to help Albion Online players manage world boss spawn timers for different maps.");
        embed.setColor(Color.CYAN);
        embed.setThumbnail(event.getJDA().getSelfUser().getAvatarUrl());

        embed.addField("Commands",
                "1. `/setdesignatedchannel`: Set the designated channel for timers.\n" +
                        "2. `/addtimer`: Add a timer for a world boss.\n" +
                        "3. `/deletetimer`: Delete a timer for a world boss.\n" +
                        "4. `/edittimer`: Edit a timer for a world boss.\n" +
                        "5. `/aboutme`: Get information about the bot.",
                false);

        embed.addField("Usage",
                "To use the bot, invite it to your Discord server and interact with it through Discord commands. For detailed usage, check the [documentation](https://github.com/Veyg/Albion-WB-timer-bot).",
                false);

        embed.addField("Support the Project",
                "If you find this bot useful and would like to support its development, you can [Buy Me a Coffee](https://www.buymeacoffee.com/Veyg).",
                false);

        embed.addField("Getting Started",
                "1. **Invite the Bot**: [Invite the bot to your Discord server](https://discord.com/api/oauth2/authorize?client_id=1145671676902785084&permissions=566935907456&scope=bot).\n"
                        +
                        "2. **Set Up Designated Channels**: Use the `/setdesignatedchannel` command.\n" +
                        "3. **Add Timers**: Use the `/addtimer` command.\n" +
                        "4. **Manage Timers**: Use the `/deletetimer` and `/edittimer` commands.",
                false);

        embed.addField("Support",
                "For questions, issues, or support, you can contact the author, Veyg, or create a GitHub issue in this repository.\n"
                        +
                        "🔗 [GitHub Repo](https://github.com/Veyg/Albion-WB-timer-bot)\n" +
                        "🔗 [Support Server](https://discord.gg/rs7u8d5FRz)",
                false);

        embed.setFooter("Thank you for using the World Boss Bot!");

        event.replyEmbeds(embed.build()).setEphemeral(true).queue();
    }

    private String readVersionFromFile() {
        try {
            List<String> lines = Files.readAllLines(Paths.get("VERSION"));
            if (!lines.isEmpty()) {
                return lines.get(0).trim();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Unknown Version";
    }

}