package discord.worldbosses;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenuInteraction;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
        this.jda = event.getJDA();
        String message = event.getMessage().getContentRaw();
        String userId = event.getAuthor().getId();

        if (message.startsWith("!setDesignatedChannel")) {
            if (event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
                designatedChannelId = event.getChannel().getId();
                event.getChannel().sendMessage("This channel is now set as the designated channel for timers.").queue();
            } else {
                event.getChannel().sendMessage("You need administrator permissions to set the designated channel.").queue();
            }
        } else if (message.startsWith("!addtimer")) {
            SelectMenu menu = createMapSelectMenu();
            event.getChannel().sendMessage("Please select a map from the dropdown.")
                .setActionRow(menu)
                .queue();
            userStates.put(userId, "awaiting_map_selection");
        } else if (userStates.getOrDefault(userId, "").equals("awaiting_time_input")) {
            String time = message;
            bossManager.addTimer(userStates.get(userId + "_selected_map"), time);
            event.getChannel().sendMessage("Timer added for " + userStates.get(userId + "_selected_map") + " at " + time).queue();
            userStates.remove(userId);
            userStates.remove(userId + "_selected_map");
            sendTimersToChannel();
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
                selectMenu.getChannel().sendMessage("You selected " + selectedMap + ". Please provide the time.").queue();
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

        StringBuilder timersTable = new StringBuilder("Timers:\n");
        for (Map.Entry<String, String> entry : bossManager.getAllTimers().entrySet()) {
            timersTable.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }

        if (timerMessageId == null) {
            // If the timer message hasn't been sent yet, send it
            designatedChannel.sendMessage(timersTable.toString()).queue(message -> {
                timerMessageId = message.getId();
            });
        } else {
            // If the timer message has already been sent, edit it
            designatedChannel.editMessageById(timerMessageId, timersTable.toString()).queue();
        }
    }

    private void updateTimers() {
        if (timerMessageId != null) {
            TextChannel channel = jda.getTextChannelById(designatedChannelId);
            if (channel != null) {
                StringBuilder timersTable = new StringBuilder("Timers:\n");
                for (Map.Entry<String, String> entry : bossManager.getAllTimers().entrySet()) {
                    timersTable.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                }
                channel.editMessageById(timerMessageId, timersTable.toString()).queue();
            }
        }
    }
}
