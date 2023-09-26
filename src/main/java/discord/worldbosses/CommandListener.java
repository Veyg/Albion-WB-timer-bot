package discord.worldbosses;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenuInteraction;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonInteraction;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import discord.worldbosses.BossManager.TimerData;

import java.util.ArrayList;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

public class CommandListener extends ListenerAdapter {
    private BossManager bossManager = new BossManager();
    private Map<String, String> userStates = new HashMap<>();
    private String timerMessageId;
    private JDA jda;
    private String designatedChannelId;
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    public CommandListener() {
        scheduleLoadedTimers();
        startPeriodicCheck();
    }
    
    private void startPeriodicCheck() {
        scheduler.scheduleAtFixedRate(() -> {
            LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
            for (Map.Entry<String, TimerData> entry : bossManager.getAllTimers().entrySet()) {
                LocalDateTime notificationTime = LocalDateTime.parse(entry.getValue().getNotificationTime(), DateTimeFormatter.ofPattern("HH:mm:ss d/MM/yyyy"));
                if (ChronoUnit.MINUTES.between(now, notificationTime) <= 5) {
                    sendBossNotification(entry.getKey(), entry.getValue().getBossSpawnTime());
                    // bossManager.deleteTimer(entry.getKey());  // Remove the timer after sending the notification
                }
            }
        }, 0, 5, TimeUnit.MINUTES);
    }    

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot())
            return;
        this.jda = event.getJDA();
        String message = event.getMessage().getContentRaw();

        if (message.startsWith("!setDesignatedChannel")) {
            handleSetDesignatedChannel(event);
        } else if (message.startsWith("!addtimer")) {
            handleAddTimer(event, message);
        } else if (userStates.getOrDefault(event.getAuthor().getId(), "").equals("awaiting_time_input")) {
            handleAwaitingTimeInput(event, message);
        } else if (message.startsWith("!deleteTimer")) {
            handleDeleteTimer(event, message);
        } else if (message.startsWith("!testNotification")) {
            handleTestNotification(event);
        } else if (message.startsWith("!editTimer")) {
            handleEditTimer(event, message);
        }
        else if (userStates.getOrDefault(event.getAuthor().getId(), "").equals("awaiting_killed_time")) {
            handleAwaitingKilledTime(event, message);
        }
        
    }

    @Override
    public void onGenericInteractionCreate(GenericInteractionCreateEvent event) {
        this.jda = event.getJDA();
        if (event.getInteraction() instanceof ButtonInteraction) {
            onButtonInteraction((ButtonInteraction) event.getInteraction());
        }
        else if (event.getInteraction() instanceof SelectMenuInteraction<?, ?>) {
            SelectMenuInteraction<?, ?> selectMenu = (SelectMenuInteraction<?, ?>) event.getInteraction();
            String userId = selectMenu.getUser().getId();

            String customId = selectMenu.getComponentId();

            if (customId.equals("map-selector") && "awaiting_map_selection_for_add".equals(userStates.get(userId))) {
                String selectedMap = (String) selectMenu.getValues().get(0);
                String fullTime = userStates.get(userId + "_input_time") + " " + userStates.get(userId + "_input_date");

                bossManager.addTimer(selectedMap, fullTime);
                selectMenu.getChannel()
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

                scheduleBossNotification(selectedMap, fullTime);
                sendTimersToChannel();
            } else if (customId.equals("map-selector") && "awaiting_map_selection".equals(userStates.get(userId))) {
                // This is for other functionalities that might require map selection without
                // adding a timer
            }
        }
    }

    private void handleSetDesignatedChannel(MessageReceivedEvent event) {
        if (event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            designatedChannelId = event.getChannel().getId();
            event.getChannel().sendMessage("This channel is now set as the designated channel for timers.")
                    .queue(response -> {
                        response.delete().queueAfter(10, TimeUnit.SECONDS, null, throwable -> {
                            if (throwable instanceof ErrorResponseException) {
                                System.out.println("Error deleting message with ID: " + response.getId()
                                        + ". Error: " + throwable.getMessage());
                            }
                        });
                    });
        } else {
            event.getChannel().sendMessage("You need administrator permissions to set the designated channel.")
                    .queue(response -> {
                        response.delete().queueAfter(10, TimeUnit.SECONDS, null, throwable -> {
                            if (throwable instanceof ErrorResponseException) {
                                System.out.println("Error deleting message with ID: " + response.getId()
                                        + ". Error: " + throwable.getMessage());
                            }
                        });
                    });
        }
        event.getMessage().delete().queue(); // Delete user's command message
        sendTimersToChannel();
    }

    private void handleAddTimer(MessageReceivedEvent event, String message) {
        if (!event.getChannel().getId().equals(designatedChannelId)) {
            event.getChannel().sendMessage("This command can only be used in the designated channel.")
                    .queue(response -> {
                        response.delete().queueAfter(10, TimeUnit.SECONDS, null, throwable -> {
                            if (throwable instanceof ErrorResponseException) {
                                System.out.println("Error deleting message with ID: " + response.getId()
                                        + ". Error: " + throwable.getMessage());
                            }
                        });
                    });
            return; // Exit the method early
        }
        String[] parts = message.split(" ", 2);
        if (parts.length == 2) {
            String timeInput = parts[1];
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
            LocalTime parsedTime = null;
    
            try {
                parsedTime = LocalTime.parse(timeInput, timeFormatter);
                System.out.println("Successfully parsed time: " + parsedTime); // Logging successful parsing
            } catch (DateTimeParseException e) {
                System.out.println("Error parsing time: " + e.getMessage()); // Logging the error
                event.getChannel().sendMessage("Invalid time format. Please use HH:mm:ss format.").queue();
                return; // Exit the method if parsing fails
            }
    
            // Ensure the time is always formatted as "HH:mm:ss"
            String formattedTime = String.format("%02d:%02d:%02d", parsedTime.getHour(), parsedTime.getMinute(), parsedTime.getSecond());
    
            LocalDate twoDaysLater = LocalDate.now(ZoneOffset.UTC).plusDays(2);
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("d/MM/yyyy");
            String formattedDate = twoDaysLater.format(dateFormatter);
    
            // Store the time and date in userStates for later use
            userStates.put(event.getAuthor().getId() + "_input_time", formattedTime);
            userStates.put(event.getAuthor().getId() + "_input_date", formattedDate);
    
            // Prompt the user to select a map
            SelectMenu menu = createMapSelectMenu();
            event.getChannel().sendMessage("Please select a map from the dropdown.")
                    .setActionRow(menu)
                    .queue();
            userStates.put(event.getAuthor().getId(), "awaiting_map_selection_for_add");
        } else {
            event.getChannel().sendMessage("Invalid command format. Use `!addtimer [HH:MM:SS]`").queue();
        }
        event.getMessage().delete().queue(); // Delete the command message
    }
    

    private void handleAwaitingTimeInput(MessageReceivedEvent event, String message) {
        String userId = event.getAuthor().getId();
        System.out.println("Received time input: " + message); // Logging the input

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        LocalTime parsedTime = null;

        try {
            parsedTime = LocalTime.parse(message, timeFormatter);
            System.out.println("Successfully parsed time: " + parsedTime); // Logging successful parsing
        } catch (DateTimeParseException e) {
            System.out.println("Error parsing time: " + e.getMessage()); // Logging the error
            event.getChannel().sendMessage("Invalid time format. Please use HH:mm:ss format.").queue();
            return; // Exit the method if parsing fails
        }

        // If we reach here, it means parsing was successful
        LocalDate twoDaysLater = LocalDate.now(ZoneOffset.UTC).plusDays(2);
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("d/MM/yyyy");
        String formattedDate = twoDaysLater.format(dateFormatter);

        // Append the date to the time
        String fullTime = parsedTime.toString() + " " + formattedDate;

        bossManager.addTimer(userStates.get(userId + "_selected_map"), fullTime);
        event.getChannel()
                .sendMessage("Timer added for " + userStates.get(userId + "_selected_map") + " at " + fullTime)
                .queue(response -> {
                    response.delete().queueAfter(10, TimeUnit.SECONDS, null, throwable -> {
                        if (throwable instanceof ErrorResponseException) {
                            System.out.println("Error deleting message with ID: " + response.getId() + ". Error: "
                                    + throwable.getMessage());
                        }
                    });
                });
        userStates.remove(userId);
        userStates.remove(userId + "_selected_map");

        sendTimersToChannel();

        event.getMessage().delete().queue(); // Delete user's command message
    }

    private void handleDeleteTimer(MessageReceivedEvent event, String message) {
        String[] parts = message.split(" ", 2);
        if (parts.length == 2) {
            String mapName = parts[1];
            bossManager.deleteTimer(mapName);
            event.getChannel().sendMessage("Timer for " + mapName + " deleted.").queue(response -> {
                response.delete().queueAfter(10, TimeUnit.SECONDS, null, throwable -> {
                    if (throwable instanceof ErrorResponseException) {
                        System.out.println("Error deleting message with ID: " + response.getId() + ". Error: "
                                + throwable.getMessage());
                    }
                });
            });
            sendTimersToChannel();
        } else {
            event.getChannel().sendMessage("Invalid command format. Use `!deleteTimer [MapName]`").queue();
        }
    }

    private void handleTestNotification(MessageReceivedEvent event) {
        sendBossNotification("TestMap", "20:00:00 1/01/2023");
        event.getMessage().delete().queue(); // Delete user's command message
    }

    private SelectMenu createMapSelectMenu() {
        return StringSelectMenu.create("map-selector")
                .addOption("Deathwisp Sink", "Deathwisp Sink")
                .addOption("Drownfield Wetland", "Drownfield Wetland")
                .addOption("Dryvein Steppe", "Dryvein Steppe")
                .addOption("Farshore Heath", "Farshore Heath")
                .addOption("Hightree Levee", "Hightree Levee")
                .addOption("Longfen Arms", "Longfen Arms")
                .addOption("Longfen Veins", "Longfen Veins")
                .addOption("Longtimber Glen", "Longtimber Glen")
                .addOption("Rivercopse Fount", "Rivercopse Fount")
                .addOption("Runnelvein Bog", "Runnelvein Bog")
                .addOption("Skysand Ridge", "Skysand Ridge")
                .addOption("Sunfang Cliffs", "Sunfang Cliffs")
                .addOption("Sunfang Dawn", "Sunfang Dawn")
                .addOption("Sunstrand Dunes", "Sunstrand Dunes")
                .addOption("Timberslope Grove", "Timberslope Grove")
                .addOption("Timbertop Dale", "Timbertop Dale")
                .addOption("Watchwood Bluffs", "Watchwood Bluffs")
                .addOption("Westweald Shore", "Westweald Shore")
                .setPlaceholder("Choose a map...")
                .setRequiredRange(1, 1)
                .build();
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
    }

    public void onButtonInteraction(ButtonInteraction event) {
        try {
            String componentId = event.getComponentId();
            switch (componentId) {
                case "boss_killed":
                    // Handle boss killed action
                    handleBossKilled(event);
                    break;
                case "boss_skipped":
                    // Extract map name from the message content
                    String mapNameSkipped = extractMapNameFromMessage(event.getMessage().getContentRaw());
                    bossManager.markBossAsSkipped(mapNameSkipped);
                    event.reply("Boss was skipped!").queue();
                    sendTimersToChannel();
                    break;
                case "boss_forgot":
                    // Extract map name from the message content
                    String mapNameForgot = extractMapNameFromMessage(event.getMessage().getContentRaw());
                    bossManager.markBossAsForgotten(mapNameForgot);
                    event.reply("Boss was forgotten!").queue();
                    sendTimersToChannel();
                    break;
                default:
                    event.reply("Unknown action.").queue();
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            event.reply("An error occurred while processing the interaction.").setEphemeral(true).queue();
        }
    }
        
    private void scheduleLoadedTimers() {
        for (Map.Entry<String, TimerData> entry : bossManager.getAllTimers().entrySet()) {
            String mapName = entry.getKey();
            String notificationTime = entry.getValue().getNotificationTime();
            scheduleBossNotification(mapName, notificationTime);
        }
    }


    private void scheduleBossNotification(String mapName, String time) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss d/MM/yyyy");
        LocalDateTime bossSpawnTime = LocalDateTime.parse(time, formatter);
        LocalDateTime notificationTime = bossSpawnTime.minusMinutes(20);
        long delay = LocalDateTime.now(ZoneOffset.UTC).until(notificationTime, ChronoUnit.SECONDS);

        System.out.println("Boss spawn time: " + bossSpawnTime);
        System.out.println("Notification time: " + notificationTime);
        System.out.println("Scheduling boss notification for: " + time);
        System.out.println("Current time: " + LocalDateTime.now(ZoneOffset.UTC));
        System.out.println("Calculated delay in seconds: " + delay);
        scheduler.schedule(() -> {
            try {
                sendBossNotification(mapName, time);
            } catch (Exception e) {
                System.out.println("Error sending boss notification: " + e.getMessage());
                e.printStackTrace();
            }
        }, delay, TimeUnit.SECONDS);
    }

    private void sendTimersToChannel() {
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

        Map<String, BossManager.TimerData> allTimers = bossManager.getAllTimers();
        if (allTimers.isEmpty()) {
            System.out.println("No timers to send.");
            return; // No timers to send
        }

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("World Boss Timers");
        embed.setColor(0x00FFFF);

        // Sort the timers by date and time
        List<Map.Entry<String, TimerData>> sortedTimers = new ArrayList<>(allTimers.entrySet());
        sortedTimers.sort(Comparator.comparing(e -> {
            String[] parts = e.getValue().getBossSpawnTime().split(" ");
            return LocalDate.parse(parts[1], DateTimeFormatter.ofPattern("d/MM/yyyy"))
                    .atTime(LocalTime.parse(parts[0], DateTimeFormatter.ofPattern("HH:mm:ss")));
        }));

        // Check if the first timer is within the next 12 hours
        String[] nextTimerParts = sortedTimers.get(0).getValue().getBossSpawnTime().split(" ");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        System.out.println("Trying to parse the time: " + nextTimerParts[0] + " " + nextTimerParts[1]);
        LocalTime nextTimerLocalTime = LocalTime.parse(nextTimerParts[0], timeFormatter);
        LocalDate nextTimerLocalDate = LocalDate.parse(nextTimerParts[1], DateTimeFormatter.ofPattern("d/MM/yyyy"));
        long hoursDifference = ChronoUnit.HOURS.between(LocalTime.now(ZoneOffset.UTC), nextTimerLocalTime);
        LocalDate utcDate = LocalDate.now(ZoneOffset.UTC);
        long daysDifference = ChronoUnit.DAYS.between(utcDate, nextTimerLocalDate);

        if (daysDifference == 0 && hoursDifference <= 12) {
            embed.addField("🚨 Coming up:", "**" + sortedTimers.get(0).getKey() + " - " + nextTimerParts[0] + " UTC "
                    + nextTimerParts[1] + "**", false);
            sortedTimers.remove(0); // Remove the first timer as it's already displayed
        }

        for (Map.Entry<String, BossManager.TimerData> entry : sortedTimers) {
            String[] timerParts = entry.getValue().getBossSpawnTime().split(" ");
            embed.addField(entry.getKey(), timerParts[0] + " UTC " + timerParts[1], false);
        }

        if (timerMessageId == null) {
            // If the timer message hasn't been sent yet, send it
            designatedChannel.sendMessageEmbeds(embed.build()).queue(message -> {
                timerMessageId = message.getId();
            }, throwable -> {
                System.out.println("Error sending message: " + throwable.getMessage());
            });
        } else {
            // Try to edit the message
            designatedChannel.editMessageEmbedsById(timerMessageId, embed.build()).queue(null, throwable -> {
                // If there's an error editing the message, check if it's the "Unknown Message"
                // error
                if (throwable instanceof ErrorResponseException
                        && ((ErrorResponseException) throwable).getErrorCode() == 10008) {
                    // If the message doesn't exist, send a new one
                    designatedChannel.sendMessageEmbeds(embed.build()).queue(message -> {
                        timerMessageId = message.getId();
                    });
                } else {
                    System.out.println("Error editing message: " + throwable.getMessage());
                }
            });
        }
    }
    private void handleEditTimer(MessageReceivedEvent event, String message) {
        String[] parts = message.split(" ", 4); // Split into 4 parts
        if (parts.length == 4) {
            String mapName = parts[1] + " " + parts[2]; // Combine the two parts of the map name
            String newTime = parts[3].split(" ")[0];
            String newDate = parts[3].split(" ")[1];
            bossManager.editTimer(mapName, newTime + " " + newDate);
            event.getChannel().sendMessage("Timer for " + mapName + " updated to " + newTime + " on " + newDate)
                    .queue(response -> {
                        response.delete().queueAfter(10, TimeUnit.SECONDS, null, throwable -> {
                            if (throwable instanceof ErrorResponseException) {
                                System.out.println("Error deleting message with ID: " + response.getId()
                                        + ". Error: " + throwable.getMessage());
                            }
                        });
                    });
            sendTimersToChannel();
        } else {
            event.getChannel()
                    .sendMessage(
                            "Invalid command format. Use '!editTimer [mapNamePart1] [mapNamePart2] [HH:MM:SS] [d/MM/yyyy]'")
                    .queue(response -> {
                        response.delete().queueAfter(10, TimeUnit.SECONDS, null, throwable -> {
                            if (throwable instanceof ErrorResponseException) {
                                System.out.println("Error deleting message with ID: " + response.getId()
                                        + ". Error: " + throwable.getMessage());
                            }
                        });
                    });
        }
        event.getMessage().delete().queue(); // Delete user's command message
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
    }
    private void handleAwaitingKilledTime(MessageReceivedEvent event, String message) {
        String userId = event.getAuthor().getId();
        String mapName = userStates.get(userId + "_mapName");
        bossManager.markBossAsKilled(mapName, message);
        event.getChannel().sendMessage("Boss at " + mapName + " was killed at " + message).queue();
        sendTimersToChannel();
        userStates.remove(userId);
        userStates.remove(userId + "_mapName");
    }
    
    
    
}
