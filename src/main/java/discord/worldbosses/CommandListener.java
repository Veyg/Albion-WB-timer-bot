package discord.worldbosses;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class CommandListener extends ListenerAdapter {
    private BossManager bossManager = new BossManager();
    private Map<String, String> userStates = new HashMap<>(); // To track user's current interaction state

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        String message = event.getMessage().getContentRaw();
        String userId = event.getAuthor().getId();

        if (message.startsWith("!addtimer")) {
            MessageEmbed embed = new EmbedBuilder()
                .setTitle("Add Timer")
                .setDescription("Please select a map from the dropdown.")
                .setColor(Color.BLUE)
                .build();

            // Create a select menu (dropdown) with map options
            SelectMenu menu = SelectMenu.create("map-selector")
                .addOption(SelectOption.of("Map 1", "map1"))
                .addOption(SelectOption.of("Map 2", "map2"))
                .addOption(SelectOption.of("Map 3", "map3"))
                .build();

            // Send the embed with the select menu
            event.getChannel().sendMessage(embed)
                .setActionRow(menu)
                .queue();

            userStates.put(userId, "awaiting_map_selection");
        } else if (userStates.getOrDefault(userId, "").equals("awaiting_time_input")) {
            // Handle time input here
            String time = message; // This is a simplification. You'd want to validate the format.
            bossManager.addTimer(userStates.get(userId + "_selected_map"), time);
            event.getChannel().sendMessage("Timer added for " + userStates.get(userId + "_selected_map") + " at " + time).queue();
            userStates.remove(userId);
            userStates.remove(userId + "_selected_map");
        }
    }

    @Override
    public void onSelectionMenu(SelectionMenuEvent event) {
        String userId = event.getUser().getId();
        if (event.getComponentId().equals("map-selector") && "awaiting_map_selection".equals(userStates.get(userId))) {
            String selectedMap = event.getSelectedValues().get(0); // Get the first selected value

            // Now, ask the user to provide the time for the selected map
            event.getChannel().sendMessage("You selected " + selectedMap + ". Please provide the time.").queue();

            userStates.put(userId, "awaiting_time_input");
            userStates.put(userId + "_selected_map", selectedMap);
        }
    }
}
