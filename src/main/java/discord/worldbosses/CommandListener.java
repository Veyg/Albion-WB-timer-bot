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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;


public class CommandListener extends ListenerAdapter {
    private BossManager bossManager = new BossManager();
    private Map<String, String> userStates = new HashMap<>();
    private String timerMessageId; // Store the ID of the timer message
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private JDA jda;
    private String designatedChannelId;

    public CommandListener() {
        // Schedule the task to run every 5 minutes
        scheduler.scheduleAtFixedRate(this::updateTimers, 0, 5, TimeUnit.MINUTES);
    }

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
            String time = message;

            // Get the date two days from now
            LocalDate twoDaysLater = LocalDate.now().plusDays(2);
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("d/MM/yyyy");
            String formattedDate = twoDaysLater.format(dateFormatter);

            // Append the date to the time
            String fullTime = time + " " + formattedDate;

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
        if (designatedChannelId == null) return; // No designated channel set

        TextChannel designatedChannel = jda.getTextChannelById(designatedChannelId);
        if (designatedChannel == null) return; // Designated channel not found

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("World Boss Timers");
        embed.setColor(0x00FFFF); // Cyan color

        // Convert the map to a list and sort it
        List<Map.Entry<String, String>> sortedTimers = new ArrayList<>(bossManager.getAllTimers().entrySet());
        sortedTimers.sort(Comparator.comparing(e -> {
            String[] parts = e.getValue().split(" ");
            return parts[0]; // This assumes the time is always the first part of the value
        }));

        StringBuilder mapNames = new StringBuilder();
        StringBuilder times = new StringBuilder();

        for (Map.Entry<String, String> entry : sortedTimers) {
            mapNames.append(entry.getKey()).append("\n");
            times.append(entry.getValue()).append("\n");
        }

        embed.addField("Map", mapNames.toString(), true);
        embed.addField("Time & Date", times.toString(), true);

        if (timerMessageId == null) {
            // If the timer message hasn't been sent yet, send it
            designatedChannel.sendMessageEmbeds(embed.build()).queue(message -> {
                timerMessageId = message.getId();
            });
        } else {
            // If the timer message has already been sent, edit it
            designatedChannel.editMessageEmbedsById(timerMessageId, embed.build()).queue();
        }
    }

    private void updateTimers() {
        if (timerMessageId != null) {
            TextChannel channel = jda.getTextChannelById(designatedChannelId);
            if (channel != null) {
                StringBuilder timersTable = new StringBuilder("Timers:\n");
                for (Map.Entry<String, String> entry : bossManager.getAllTimers().entrySet()) {
                    timersTable.append("**").append(entry.getKey()).append("**: `").append(entry.getValue())
                            .append("`\n");
                }
                channel.editMessageById(timerMessageId, timersTable.toString()).queue();
            }
        }
    }
}
