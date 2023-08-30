package discord.worldbosses;

import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.Interaction;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenuInteraction;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;

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
            System.out.println("Received !addtimer command.");
            SelectMenu menu = createMapSelectMenu();

            event.getChannel().sendMessage("Please select a map from the dropdown.")
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
    public void onGenericInteractionCreate(GenericInteractionCreateEvent event) {
        System.out.println("Received an interaction event.");

        Interaction interaction = event.getInteraction();
        
        if (interaction instanceof SelectMenuInteraction<?, ?>) {
            System.out.println("Interaction is of type SelectMenuInteraction.");

            SelectMenuInteraction<?, ?> selectMenu = (SelectMenuInteraction<?, ?>) interaction;
            
            String userId = selectMenu.getUser().getId();
            System.out.println("User ID: " + userId);
            System.out.println("SelectMenu ID: " + selectMenu.getId());
            System.out.println("User state: " + userStates.get(userId));

            if (selectMenu.getId().equals("map-selector") && "awaiting_map_selection".equals(userStates.get(userId))) {
                System.out.println("Inside the map selection logic.");

                String selectedMap = (String) selectMenu.getValues().get(0); // Get the first selected value

                // Now, ask the user to provide the time for the selected map
                selectMenu.getChannel().sendMessage("You selected " + selectedMap + ". Please provide the time.").queue();

                userStates.put(userId, "awaiting_time_input");
                userStates.put(userId + "_selected_map", selectedMap);
            }
        }
    }

    private SelectMenu createMapSelectMenu() {
        return StringSelectMenu.create("map-selector")
                .addOption("Map 1", "map1_value")
                .addOption("Map 2", "map2_value")
                // ... add more maps as needed
                .setPlaceholder("Choose a map...")
                .setRequiredRange(1, 1)
                .build();
    }
}
