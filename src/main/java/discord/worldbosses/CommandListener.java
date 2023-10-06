package discord.worldbosses;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import discord.worldbosses.BossManager.TimerData;

import java.io.File;
import java.io.IOException;
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
    private String designatedChannelId;
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private Set<String> sentNotifications = new HashSet<>();
    
    public CommandListener(JDA jda, String designatedChannelId, String serverId) {
        this.jda = jda;
        this.designatedChannelId = designatedChannelId;
        this.bossManager = new BossManager(serverId);

        startPeriodicCheck();
        sendTimersToChannel();
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
                if (ChronoUnit.MINUTES.between(now, bossSpawnTime) <= 20) {
                    // Check if the boss has been marked as skipped or forgotten
                    if (!bossManager.isSkippedOrForgotten(entry.getKey())) {
                        sendBossNotification(entry.getKey(), entry.getValue().getBossSpawnTime());
                    }
                }
            }
        }, 0, 5, TimeUnit.MINUTES);
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getChannel().getId().equals(designatedChannelId)
                && !event.getName().equals("setdesignatedchannel")) {
            event.reply("Don't use it here retard 🤓").setEphemeral(true).queue();
            return; // Ignore interactions outside the designated channel
        }

        switch (event.getName()) {
            case "setdesignatedchannel":
                handleSetDesignatedChannel(event);
                break;
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

        // Update the timer using BossManager
        bossManager.editTimer(mapName, fullNewTime);
        sendTimersToChannel();
        // Provide feedback to the user
        event.reply("Timer for " + mapName + " has been updated to " + fullNewTime)
                .queue(response -> {
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

        // Check if the timer exists before deleting
        if (bossManager.getAllTimers().containsKey(mapName)) {
            bossManager.deleteTimer(mapName);
            sendTimersToChannel();
            event.reply("Timer for " + mapName + " has been deleted.").queue();
        } else {
            event.reply("Failed to delete timer for " + mapName + ". It might not exist.").setEphemeral(true).queue();
        }
    }

    private void handleAddTimer(SlashCommandInteractionEvent event) {
        String timeInput = event.getOption("time").getAsString();
        String mapName = event.getOption("map").getAsString();

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

        // Use the current date plus two days
        LocalDate twoDaysLater = LocalDate.now(ZoneOffset.UTC).plusDays(2);
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String formattedDate = twoDaysLater.format(dateFormatter);

        String fullTime = formattedTime + " " + formattedDate;

        // Store the timer using BossManager
        bossManager.addTimer(mapName, fullTime);
        bossManager.saveTimers(); // This line needs the saveTimers method to be public in BossManager
        sendTimersToChannel();
        // Provide feedback to the user
        event.reply("Timer added for " + mapName + " at " + fullTime)
                .queue(response -> {
                    response.retrieveOriginal().queue(originalMessage -> {
                        scheduleMessageDeletion(originalMessage, 7200000);
                    });
                });
        ;
    }

    @Override
    public void onGenericInteractionCreate(GenericInteractionCreateEvent event) {
        if (!event.getChannel().getId().equals(designatedChannelId)) {
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

                bossManager.addTimer(selectedMap, fullTime);
                selectMenu.getChannel()

                        // TODO: test this
                        .sendMessage("Timer added for " + selectedMap + " at " + fullTime)
                        .queue(response -> {
                            response.delete().queueAfter(10, TimeUnit.SECONDS, null, throwable -> {
                                if (throwable instanceof ErrorResponseException) {
                                    System.out.println("Error deleting message with ID: " + response.getId()
                                            + ". Error: " + throwable.getMessage());
                                }
                            });
                        });
                userStates.remove(userId);
                userStates.remove(userId + "_input_time");
                userStates.remove(userId + "_input_date");
                selectMenu.getMessage().delete().queue();

                sendTimersToChannel();
            } else if (customId.equals("map-selector") && "awaiting_map_selection".equals(userStates.get(userId))) {
                // This is for other functionalities that might require map selection without
                // adding a timer
            }
        }
    }

    private void handleSetDesignatedChannel(SlashCommandInteractionEvent event) {
        if (event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            designatedChannelId = event.getChannel().getId();
    
            // Save the designated channel ID to the config file
            ConfigManager.setDesignatedChannelId(event.getGuild().getId(), designatedChannelId);
    
            event.reply("This channel is now set as the designated channel for timers.")
                    .queue(response -> {
                        response.deleteOriginal().queueAfter(10, TimeUnit.SECONDS, null, throwable -> {
                            if (throwable instanceof ErrorResponseException) {
                                System.out.println("Error deleting message with ID: " + event.getInteraction().getId()
                                        + ". Error: " + throwable.getMessage());
                            }
                        });
                    });
        } else {
            event.reply("You need administrator permissions to set the designated channel.")
                    .queue(response -> {
                        response.deleteOriginal().queueAfter(10, TimeUnit.SECONDS, null, throwable -> {
                            if (throwable instanceof ErrorResponseException) {
                                System.out.println("Error deleting message with ID: " + event.getInteraction().getId()
                                        + ". Error: " + throwable.getMessage());
                            }
                        });
                    });
        }
        sendTimersToChannel();
    }    

    private void sendBossNotification(String mapName, String time) {
        if (designatedChannelId == null)
            return;
        TextChannel designatedChannel = jda.getTextChannelById(designatedChannelId);
        if (designatedChannel == null)
            return;

        Button killedButton = Button.primary("boss_killed", "Killed").withEmoji(Emoji.fromUnicode("🐘"));
        Button skippedButton = Button.secondary("boss_skipped", "Skipped").withEmoji(Emoji.fromUnicode("🕣"));
        Button forgotButton = Button.danger("boss_forgot", "Forgot").withEmoji(Emoji.fromUnicode("❓"));

        designatedChannel.sendMessage("@everyone\n**WORLD BOSS SPAWNING SOON**\nMap: " + mapName + "\nTime: " + time)
                .setActionRow(killedButton, skippedButton, forgotButton)
                .queue();

        // After sending the notification, add it to the set of sent notifications
        String currentDate = LocalDate.now(ZoneOffset.UTC).toString();
        sentNotifications.add(mapName + "_" + time + "_" + currentDate);
    }

    public void onButtonInteraction(ButtonInteraction event) {
        switch (event.getComponentId()) {
            case "boss_killed":
                handleBossKilled(event);
                break;
            case "boss_skipped":
                event.getMessage().delete().queue();
                String mapNameSkipped = extractMapNameFromMessage(event.getMessage().getContentRaw());
                bossManager.markBossAsSkipped(mapNameSkipped);
                sendTimersToChannel();
                break;
            case "boss_forgot":
                event.getMessage().delete().queue();
                String mapNameForgot = extractMapNameFromMessage(event.getMessage().getContentRaw());
                bossManager.markBossAsForgotten(mapNameForgot);
                event.reply("Boss was forgotten!")
                        .queue(response -> {
                            response.retrieveOriginal().queue(originalMessage -> {
                                scheduleMessageDeletion(originalMessage, 7200000);
                            });
                        });
                sendTimersToChannel();
                break;

            default:
                break;
        }
    }

    void sendTimersToChannel() {
        System.out.println("Sending timers to channel");
        if (designatedChannelId == null) {
            System.out.println("Designated channel ID is null.");
            return; // No designated channel set
        }

        TextChannel designatedChannel = jda.getTextChannelById(designatedChannelId);
        if (designatedChannel == null) {
            System.out.println("Designated channel not found.");
            return; // Designated channel not found
        }

        List<Map.Entry<String, TimerData>> sortedTimers = bossManager.getSortedTimers();
        if (sortedTimers.isEmpty()) {
            System.out.println("No timers to send.");
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
                        .append(nextSpawnTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                        .append("**\n");
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
                    embed.addField(bossName,
                            bossSpawnTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")) + " UTC "
                                    + bossSpawnTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                            false);
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

        // Send or edit the message
        if (timerMessageId == null) {
            designatedChannel.sendMessageEmbeds(embed.build()).queue(message -> timerMessageId = message.getId());
        } else {
            designatedChannel.editMessageEmbedsById(timerMessageId, embed.build()).queue(null, throwable -> {
                if (throwable instanceof ErrorResponseException
                        && ((ErrorResponseException) throwable).getErrorCode() == 10008) {
                    designatedChannel.sendMessageEmbeds(embed.build())
                            .queue(message -> timerMessageId = message.getId());
                } else {
                    System.out.println("Error editing message: " + throwable.getMessage());
                }
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

        // Delete the original boss notification
        event.getMessage().delete().queue();
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        String userId = event.getAuthor().getId();
        if ("awaiting_killed_time".equals(userStates.get(userId))) {
            String killedTimeInput = event.getMessage().getContentRaw().trim(); // Trim the input
            System.out.println("Received killed time: " + killedTimeInput); // Debug log
            String mapName = userStates.get(userId + "_mapName");

            // Schedule deletion of user's message after 2 hours
            scheduleMessageDeletion(event.getMessage(), 7200000);

            // Validate the time format with explicit locale
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.US);
            try {
                LocalTime parsedTime = LocalTime.parse(killedTimeInput, timeFormatter);

                // Add two days to the current date
                LocalDate twoDaysLater = LocalDate.now(ZoneOffset.UTC).plusDays(2);
                LocalDateTime combinedDateTime = LocalDateTime.of(twoDaysLater, parsedTime);
                String fullNewTime = combinedDateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss d/MM/yyyy"));

                bossManager.markBossAsKilled(mapName, fullNewTime);
                event.getChannel()
                        .sendMessage("Boss killed!. Timer for " + mapName + " has been updated to " + fullNewTime)
                        // TODO: test this also
                        .queue(response -> scheduleMessageDeletion(response, 7200000));

                // Clear the user's state
                userStates.remove(userId);
                userStates.remove(userId + "_mapName");
            } catch (DateTimeParseException e) {
                System.out.println("Error parsing time: " + e.getMessage()); // Debug log
                event.getChannel().sendMessage("Invalid time format. Please use HH:mm:ss format. Enter the time again.")
                        .queue(response -> scheduleMessageDeletion(response, 30000));

                // Keep the user in the "awaiting_killed_time" state so they can try again
            }
            sendTimersToChannel();
        }
    }

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        String serverId = event.getGuild().getId();

        // Create a new timers.json file for this server
        File serverTimersFile = new File("data/" + serverId + "/timers.json");
        if (!serverTimersFile.exists()) {
            serverTimersFile.getParentFile().mkdirs();
            try {
                serverTimersFile.createNewFile();
                // Optionally, initialize the file with default data
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

}