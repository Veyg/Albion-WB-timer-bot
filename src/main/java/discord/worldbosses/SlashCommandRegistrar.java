package discord.worldbosses;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SlashCommandRegistrar {

    private static final Logger logger = LoggerFactory.getLogger(SlashCommandRegistrar.class);
    private JDA jda;

    public SlashCommandRegistrar(JDA jda) {
        this.jda = jda;
        registerCommands();
    }

    private OptionData getMapOptionData() {
        OptionData mapOption = new OptionData(OptionType.STRING, "map", "Name of the map", true);
        String[] maps = {
            "Darkground Swamp", "Deadvein Gully", "Deathwisp Sink", "Drownfield Wetland", "Dryvein Steppe",
            "Farshore Heath", "Flimmerair Steppe", "Hightree Levee", "Longfen Arms", "Longfen Veins",
            "Longtimber Glen", "Nightbloom Forest", "Rivercopse Fount", "Runnelvein Bog", "Skysand Ridge",
            "Stumprot Swamp", "Sunfang Cliffs", "Sunfang Dawn", "Sunstrand Dunes", "Timberslope Grove",
            "Timbertop Dale", "Watchwood Bluffs", "Westweald Shore"
        };
        for (String map : maps) {
            mapOption.addChoice(map, map);
        }
        return mapOption;
    }

    public void registerCommands() {
        OptionData mapOption = getMapOptionData();

        jda.updateCommands().addCommands(
            Commands.slash("aboutme", "Get information about the bot"),
            Commands.slash("setdesignatedchannel", "Set the designated channel for timers"),
            Commands.slash("help", "Get a list of available commands and their descriptions"),
            Commands.slash("addtimer", "Add a timer for a world boss")
                .addOption(OptionType.STRING, "time", "Time for the boss spawn in HH:mm:ss format", true)
                .addOptions(mapOption)
                .addOption(OptionType.STRING, "date", "Date for the boss spawn in dd/MM/yyyy format", false)
                .addOption(OptionType.STRING, "note", "A note about the timer", false),
            Commands.slash("deletetimer", "Delete a timer for a world boss")
                .addOptions(mapOption),
            Commands.slash("edittimer", "Edit a timer for a world boss")
                .addOptions(mapOption)
                .addOption(OptionType.STRING, "newtime", "New time for the boss spawn in HH:mm:ss format", true)
                .addOption(OptionType.STRING, "newdate", "New date for the boss spawn in d/MM/yyyy format", true)
                .addOption(OptionType.STRING, "note", "An optional note about the timer", false)
        ).queue(
            success -> logger.info("Commands registered successfully"), 
            failure -> logger.error("Command registration failed: ", failure)
        );
    }
}
