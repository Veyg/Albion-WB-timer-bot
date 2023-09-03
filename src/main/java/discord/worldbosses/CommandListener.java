package discord.worldbosses;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenuInteraction;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

public class CommandListener extends ListenerAdapter {
    private BossManager bossManager = new BossManager();
    private Map<String, String> userStates = new HashMap<>();
    private String timerMessageId; // Store the ID of the timer message
    private JDA jda;
    private String designatedChannelId;

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // Check if the message is sent by a bot. If yes, return immediately.
        if (event.getAuthor().isBot()) {
            return;
        }

        this.jda = event.getJDA();
        String message = event.getMessage().getContentRaw();
        String userId = event.getAuthor().getId();

        if (message.startsWith("!setDesignatedChannel")) {
            if (event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
                designatedChannelId = event.getChannel().getId();
                event.getChannel().sendMessage("This channel is now set as the designated channel for timers.")
                        .queue(response -> {
                            response.delete().queueAfter(10, TimeUnit.SECONDS); // Delete bot's message after 5 seconds
                        });
                sendTimersToChannel();
            } else {
                event.getChannel().sendMessage("You need administrator permissions to set the designated channel.")
                        .queue(response -> {
                            response.delete().queueAfter(10, TimeUnit.SECONDS); // Delete bot's message after 5 seconds
                        });
            }
            event.getMessage().delete().queue(); // Delete user's command message
        } else if (message.startsWith("!addtimer")) {
            if (event.getChannel().getId().equals(designatedChannelId)) { // Check if the command is in the designated
                                                                          // channel
                SelectMenu menu = createMapSelectMenu();
                event.getChannel().sendMessage("Please select a map from the dropdown.")
                        .setActionRow(menu)
                        .queue(response -> {
                            response.delete().queueAfter(10, TimeUnit.SECONDS); // Delete bot's message after 5 seconds
                        });
                userStates.put(userId, "awaiting_map_selection");
            } else {
                event.getChannel().sendMessage("This command can only be used in the designated channel.")
                        .queue(response -> {
                            response.delete().queueAfter(10, TimeUnit.SECONDS); // Delete bot's message after 5 seconds
                        });
            }
            event.getMessage().delete().queue(); // Delete user's command message
        } else if (userStates.getOrDefault(userId, "").equals("awaiting_time_input")) {
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
            LocalDate twoDaysLater = LocalDate.now().plusDays(2);
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("d/MM/yyyy");
            String formattedDate = twoDaysLater.format(dateFormatter);

            // Append the date to the time
            String fullTime = parsedTime.toString() + " " + formattedDate;

            bossManager.addTimer(userStates.get(userId + "_selected_map"), fullTime);
            event.getChannel()
                    .sendMessage("Timer added for " + userStates.get(userId + "_selected_map") + " at " + fullTime)
                    .queue(response -> {
                        response.delete().queueAfter(10, TimeUnit.SECONDS); // Delete bot's message after 10 seconds
                    });
            userStates.remove(userId);
            userStates.remove(userId + "_selected_map");
            sendTimersToChannel();
            event.getMessage().delete().queue(); // Delete user's command message

        } else if (message.startsWith("!editTimer")) {
            String[] parts = message.split(" ", 4);
            if (parts.length == 4) {
                String mapName = parts[1];
                String newTime = parts[2];
                String newDate = parts[3];
                bossManager.editTimer(mapName, newTime + " " + newDate);
                event.getChannel().sendMessage("Timer for " + mapName + " updated to " + newTime + " on " + newDate)
                        .queue(response -> {
                            response.delete().queueAfter(5, TimeUnit.SECONDS);
                        });
                sendTimersToChannel();
            } else {
                event.getChannel()
                        .sendMessage("Invalid command format. Use '!editTimer [mapName] [HH:MM:SS] [d/MM/yyyy]'")
                        .queue();
            }

        } else if (message.startsWith("!deleteTimer")) {
            String[] parts = message.split(" ", 2);
            if (parts.length == 2) {
                String mapName = parts[1];
                bossManager.deleteTimer(mapName);
                event.getChannel().sendMessage("Timer for " + mapName + " deleted.").queue(response -> {
                    response.delete().queueAfter(10, TimeUnit.SECONDS);
                });
                sendTimersToChannel();
            } else {
                event.getChannel().sendMessage("Invalid command format. Use `!deleteTimer [MapName]`").queue();
            }
        }

    }

    @Override
    public void onGenericInteractionCreate(GenericInteractionCreateEvent event) {
        this.jda = event.getJDA();
        if (event.getInteraction() instanceof SelectMenuInteraction<?, ?>) {
            SelectMenuInteraction<?, ?> selectMenu = (SelectMenuInteraction<?, ?>) event.getInteraction();
            String userId = selectMenu.getUser().getId();

            String customId = selectMenu.getComponentId();

            if (customId.equals("map-selector") && "awaiting_map_selection".equals(userStates.get(userId))) {
                String selectedMap = (String) selectMenu.getValues().get(0);
                selectMenu.getChannel().sendMessage("You selected " + selectedMap + ". Please provide the time.")
                        .queue(response -> {
                            response.delete().queueAfter(5, TimeUnit.SECONDS); // Delete bot's message after 5 seconds
                        });
                userStates.put(userId, "awaiting_time_input");
                userStates.put(userId + "_selected_map", selectedMap);
            }
        }
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

    private void sendTimersToChannel() {
        if (designatedChannelId == null) {
            System.out.println("Designated channel ID is null.");
            return; // No designated channel set
        }
    
        TextChannel designatedChannel = jda.getTextChannelById(designatedChannelId);
        if (designatedChannel == null) {
            System.out.println("Designated channel not found.");
            return; // Designated channel not found
        }
    
        Map<String, String> allTimers = bossManager.getAllTimers();
        if (allTimers.isEmpty()) {
            System.out.println("No timers to send.");
            return; // No timers to send
        }
    
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("World Boss Timers");
        embed.setColor(0x00FFFF);
    
        // Sort the timers by date and time
        List<Map.Entry<String, String>> sortedTimers = new ArrayList<>(allTimers.entrySet());
        sortedTimers.sort(Comparator.comparing(e -> {
            String[] parts = e.getValue().split(" ");
            return LocalDate.parse(parts[1], DateTimeFormatter.ofPattern("d/MM/yyyy"))
                    .atTime(LocalTime.parse(parts[0], DateTimeFormatter.ofPattern("HH:mm:ss")));
        }));
    
        // Check if the first timer is within the next 12 hours
        String[] nextTimerParts = sortedTimers.get(0).getValue().split(" ");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        System.out.println("Trying to parse the time: " + nextTimerParts[0] + " " + nextTimerParts[1]);
        LocalTime nextTimerLocalTime = LocalTime.parse(nextTimerParts[0], timeFormatter);
        LocalDate nextTimerLocalDate = LocalDate.parse(nextTimerParts[1], DateTimeFormatter.ofPattern("d/MM/yyyy"));    
        long hoursDifference = ChronoUnit.HOURS.between(LocalTime.now(), nextTimerLocalTime);
        long daysDifference = ChronoUnit.DAYS.between(LocalDate.now(), nextTimerLocalDate);
    
        if (daysDifference == 0 && hoursDifference <= 12) {
            embed.addField("🚨 Coming up:", "**" + sortedTimers.get(0).getKey() + " - " + nextTimerParts[0] + " UTC "
                    + nextTimerParts[1] + "**", false);
            sortedTimers.remove(0); // Remove the first timer as it's already displayed
        }
    
        for (Map.Entry<String, String> entry : sortedTimers) {
            String[] timerParts = entry.getValue().split(" ");
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
            // If the timer message has already been sent, edit it
            designatedChannel.editMessageEmbedsById(timerMessageId, embed.build()).queue(null, throwable -> {
                System.out.println("Error editing message: " + throwable.getMessage());
            });
        }
    }
}
