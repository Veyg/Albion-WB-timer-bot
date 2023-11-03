package discord.worldbosses;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.OptionType;

public class SlashCommandRegistrar {

        private JDA jda;

        public SlashCommandRegistrar(JDA jda) {
                this.jda = jda;
                registerCommands();
        }

        public void registerCommands() {
                // Registering the setDesignatedChannel command
                jda.updateCommands().addCommands(
                                Commands.slash("aboutme", "Get information about the bot"),
                                Commands.slash("setdesignatedchannel", "Set the designated channel for timers"),
                                Commands.slash("help", "Get a list of available commands and their descriptions"),
                                Commands.slash("addtimer", "Add a timer for a world boss")
                                                .addOption(OptionType.STRING, "time",
                                                                "Time for the boss spawn in HH:mm:ss format", true)
                                                .addOptions(
                                                                new OptionData(OptionType.STRING, "map",
                                                                                "Name of the map", true)
                                                                                .addChoice("Darkground Swamp", "Darkground Swamp")
                                                                                .addChoice("Deadvein Gully", "Deadvein Gully")
                                                                                .addChoice("Deathwisp Sink", "Deathwisp Sink")
                                                                                .addChoice("Drownfield Wetland", "Drownfield Wetland")
                                                                                .addChoice("Dryvein Steppe", "Dryvein Steppe")
                                                                                .addChoice("Farshore Heath", "Farshore Heath")
                                                                                .addChoice("Flimmerair Steppe", "Flimmerair Steppe")
                                                                                .addChoice("Hightree Levee", "Hightree Levee")
                                                                                .addChoice("Longfen Arms", "Longfen Arms")
                                                                                .addChoice("Longfen Veins", "Longfen Veins")
                                                                                .addChoice("Longtimber Glen", "Longtimber Glen")
                                                                                .addChoice("Nightbloom Forest", "Nightbloom Forest")
                                                                                .addChoice("Rivercopse Fount", "Rivercopse Fount")
                                                                                .addChoice("Runnelvein Bog", "Runnelvein Bog")
                                                                                .addChoice("Skysand Ridge", "Skysand Ridge")
                                                                                .addChoice("Stumprot Swamp", "Stumprot Swamp")
                                                                                .addChoice("Sunfang Cliffs", "Sunfang Cliffs")
                                                                                .addChoice("Sunfang Dawn", "Sunfang Dawn")
                                                                                .addChoice("Sunstrand Dunes", "Sunstrand Dunes")
                                                                                .addChoice("Timberslope Grove", "Timberslope Grove")
                                                                                .addChoice("Timbertop Dale", "Timbertop Dale")
                                                                                .addChoice("Watchwood Bluffs", "Watchwood Bluffs")
                                                                                .addChoice("Westweald Shore", "Westweald Shore"))
                                                .addOption(OptionType.STRING, "date", "Date for the boss spawn in dd/MM/yyyy format", false), // Correct this line

                                Commands.slash("deletetimer", "Delete a timer for a world boss")
                                                .addOptions(
                                                                new OptionData(OptionType.STRING, "map",
                                                                                "Name of the map", true)
                                                                                .addChoice("Darkground Swamp", "Darkground Swamp")
                                                                                .addChoice("Deadvein Gully", "Deadvein Gully")
                                                                                .addChoice("Deathwisp Sink", "Deathwisp Sink")
                                                                                .addChoice("Drownfield Wetland", "Drownfield Wetland")
                                                                                .addChoice("Dryvein Steppe", "Dryvein Steppe")
                                                                                .addChoice("Farshore Heath", "Farshore Heath")
                                                                                .addChoice("Flimmerair Steppe", "Flimmerair Steppe")
                                                                                .addChoice("Hightree Levee", "Hightree Levee")
                                                                                .addChoice("Longfen Arms", "Longfen Arms")
                                                                                .addChoice("Longfen Veins", "Longfen Veins")
                                                                                .addChoice("Longtimber Glen", "Longtimber Glen")
                                                                                .addChoice("Nightbloom Forest", "Nightbloom Forest")
                                                                                .addChoice("Rivercopse Fount", "Rivercopse Fount")
                                                                                .addChoice("Runnelvein Bog", "Runnelvein Bog")
                                                                                .addChoice("Skysand Ridge", "Skysand Ridge")
                                                                                .addChoice("Stumprot Swamp", "Stumprot Swamp")
                                                                                .addChoice("Sunfang Cliffs", "Sunfang Cliffs")
                                                                                .addChoice("Sunfang Dawn", "Sunfang Dawn")
                                                                                .addChoice("Sunstrand Dunes", "Sunstrand Dunes")
                                                                                .addChoice("Timberslope Grove", "Timberslope Grove")
                                                                                .addChoice("Timbertop Dale", "Timbertop Dale")
                                                                                .addChoice("Watchwood Bluffs", "Watchwood Bluffs")
                                                                                .addChoice("Westweald Shore", "Westweald Shore")),
                                Commands.slash("edittimer", "Edit a timer for a world boss")
                                                .addOptions(
                                                                new OptionData(OptionType.STRING, "map",
                                                                                "Name of the map", true)
                                                                                .addChoice("Darkground Swamp", "Darkground Swamp")
                                                                                .addChoice("Deadvein Gully", "Deadvein Gully")
                                                                                .addChoice("Deathwisp Sink", "Deathwisp Sink")
                                                                                .addChoice("Drownfield Wetland", "Drownfield Wetland")
                                                                                .addChoice("Dryvein Steppe", "Dryvein Steppe")
                                                                                .addChoice("Farshore Heath", "Farshore Heath")
                                                                                .addChoice("Flimmerair Steppe", "Flimmerair Steppe")
                                                                                .addChoice("Hightree Levee", "Hightree Levee")
                                                                                .addChoice("Longfen Arms", "Longfen Arms")
                                                                                .addChoice("Longfen Veins", "Longfen Veins")
                                                                                .addChoice("Longtimber Glen", "Longtimber Glen")
                                                                                .addChoice("Nightbloom Forest", "Nightbloom Forest")
                                                                                .addChoice("Rivercopse Fount", "Rivercopse Fount")
                                                                                .addChoice("Runnelvein Bog", "Runnelvein Bog")
                                                                                .addChoice("Skysand Ridge", "Skysand Ridge")
                                                                                .addChoice("Stumprot Swamp", "Stumprot Swamp")
                                                                                .addChoice("Sunfang Cliffs", "Sunfang Cliffs")
                                                                                .addChoice("Sunfang Dawn", "Sunfang Dawn")
                                                                                .addChoice("Sunstrand Dunes", "Sunstrand Dunes")
                                                                                .addChoice("Timberslope Grove", "Timberslope Grove")
                                                                                .addChoice("Timbertop Dale", "Timbertop Dale")
                                                                                .addChoice("Watchwood Bluffs", "Watchwood Bluffs")
                                                                                .addChoice("Westweald Shore", "Westweald Shore"))
                                                .addOption(OptionType.STRING, "newtime",
                                                                "New time for the boss spawn in HH:mm:ss format", true)
                                                .addOption(OptionType.STRING, "newdate",
                                                                "New date for the boss spawn in d/MM/yyyy format",
                                                                true))
                                .queue();

        }
}
