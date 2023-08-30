package discord.worldbosses;

import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.Interaction;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenuInteraction;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class CommandListener extends ListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(CommandListener.class);

    private BossManager bossManager = new BossManager();
    private Map<String, String> userStates = new HashMap<>(); // To track user's current interaction state

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        String message = event.getMessage().getContentRaw();
        String userId = event.getAuthor().getId();

        if (message.startsWith("!addtimer")) {
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
        Interaction interaction = event.getInteraction();
        
        if (interaction instanceof SelectMenuInteraction<?, ?>) {
            SelectMenuInteraction<?, ?> selectMenu = (SelectMenuInteraction<?, ?>) interaction;
            
            // Handle the select menu interaction
            String userId = selectMenu.getUser().getId();
            String customId = selectMenu.getComponentId();
    
            logger.info("Received interaction with custom ID: " + customId);
    
            if (customId.equals("map-selector") && "awaiting_map_selection".equals(userStates.get(userId))) {
                String selectedMap = (String) selectMenu.getValues().get(0).toString(); // Explicitly convert to String
    
                logger.info("User " + userId + " selected map: " + selectedMap);
    
                // Now, ask the user to provide the time for the selected map
                selectMenu.reply("You selected " + selectedMap + ". Please provide the time.").queue();
    
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
}
