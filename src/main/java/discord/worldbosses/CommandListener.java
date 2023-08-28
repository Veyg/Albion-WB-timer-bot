package discord.worldbosses;

import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.Interaction;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenuInteraction;

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
            // TODO: Adjust the select menu creation based on JDA 5's methods
            // For now, we'll comment it out
            /*
            SelectMenu menu = ...; // Create the select menu using JDA 5's methods

            event.getChannel().sendMessage("Please select a map from the dropdown.")
                .setActionRow(menu)
                .queue();
            */

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
    public void onGenericInteractionCreate(GenericInteractionCreateEvent event) {
        Interaction interaction = event.getInteraction();
        
        if (interaction instanceof SelectMenuInteraction<?, ?>) {
            SelectMenuInteraction<?, ?> selectMenu = (SelectMenuInteraction<?, ?>) interaction;
            
            // Handle the select menu interaction
            String userId = selectMenu.getUser().getId();
            if (selectMenu.getId().equals("map-selector") && "awaiting_map_selection".equals(userStates.get(userId))) {
                String selectedMap = (String) selectMenu.getValues().get(0); // Get the first selected value

                // Now, ask the user to provide the time for the selected map
                selectMenu.getChannel().sendMessage("You selected " + selectedMap + ". Please provide the time.").queue();

                userStates.put(userId, "awaiting_time_input");
                userStates.put(userId + "_selected_map", selectedMap);
            }
        }
    }
}
