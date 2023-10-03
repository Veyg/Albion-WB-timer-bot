package discord.worldbosses;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.OptionType;

public class SlashCommandRegistrar {

    private JDA jda;
    private String designatedChannelId;

    public SlashCommandRegistrar(JDA jda, String designatedChannelId) {
        this.jda = jda;
        this.designatedChannelId = designatedChannelId;
        registerCommands();
    }

    private void registerCommands() {
        // Registering the setDesignatedChannel command
        CommandData setDesignatedChannel = new CommandData("setdesignatedchannel", "Set the designated channel for timers");
        jda.upsertCommand(setDesignatedChannel).queue();

        // Registering the addtimer command
        CommandData addTimer = new CommandData("addtimer", "Add a timer for a world boss")
            .addOptions(new OptionData(OptionType.STRING, "time", "Time for the boss spawn in HH:mm:ss format", true));

        // Add map choices based on the list from CommandListener
        addTimer.addOption(new OptionData(OptionType.STRING, "map", "Name of the map", true)
            .addChoice("Deathwisp Sink", "Deathwisp Sink")
            .addChoice("Drownfield Wetland", "Drownfield Wetland")
            .addChoice("Dryvein Steppe", "Dryvein Steppe")
            .addChoice("Farshore Heath", "Farshore Heath")
            .addChoice("Hightree Levee", "Hightree Levee")
            .addChoice("Longfen Arms", "Longfen Arms")
            .addChoice("Longfen Veins", "Longfen Veins")
            .addChoice("Longtimber Glen", "Longtimber Glen")
            .addChoice("Rivercopse Fount", "Rivercopse Fount")
            .addChoice("Runnelvein Bog", "Runnelvein Bog")
            .addChoice("Skysand Ridge", "Skysand Ridge")
            .addChoice("Sunfang Cliffs", "Sunfang Cliffs")
            .addChoice("Sunfang Dawn", "Sunfang Dawn")
            .addChoice("Sunstrand Dunes", "Sunstrand Dunes")
            .addChoice("Timberslope Grove", "Timberslope Grove")
            .addChoice("Timbertop Dale", "Timbertop Dale")
            .addChoice("Watchwood Bluffs", "Watchwood Bluffs")
            .addChoice("Westweald Shore", "Westweald Shore"));

        jda.upsertCommand(addTimer).queue();

        // Registering the deleteTimer command
        CommandData deleteTimer = new CommandData("deletetimer", "Delete a timer for a world boss")
            .addOptions(new OptionData(OptionType.STRING, "mapname", "Name of the map where the boss spawns", true));
        jda.upsertCommand(deleteTimer).queue();

        // Registering the editTimer command
        CommandData editTimer = new CommandData("edittimer", "Edit a timer for a world boss")
            .addOptions(
                new OptionData(OptionType.STRING, "mapname", "Name of the map where the boss spawns", true)
                    .addChoice("Deathwisp Sink", "Deathwisp Sink")
                    .addChoice("Drownfield Wetland", "Drownfield Wetland")
                    .addChoice("Dryvein Steppe", "Dryvein Steppe")
                    .addChoice("Farshore Heath", "Farshore Heath")
                    .addChoice("Hightree Levee", "Hightree Levee")
                    .addChoice("Longfen Arms", "Longfen Arms")
                    .addChoice("Longfen Veins", "Longfen Veins")
                    .addChoice("Longtimber Glen", "Longtimber Glen")
                    .addChoice("Rivercopse Fount", "Rivercopse Fount")
                    .addChoice("Runnelvein Bog", "Runnelvein Bog")
                    .addChoice("Skysand Ridge", "Skysand Ridge")
                    .addChoice("Sunfang Cliffs", "Sunfang Cliffs")
                    .addChoice("Sunfang Dawn", "Sunfang Dawn")
                    .addChoice("Sunstrand Dunes", "Sunstrand Dunes")
                    .addChoice("Timberslope Grove", "Timberslope Grove")
                    .addChoice("Timbertop Dale", "Timbertop Dale")
                    .addChoice("Watchwood Bluffs", "Watchwood Bluffs")
                    .addChoice("Westweald Shore", "Westweald Shore"),
                new OptionData(OptionType.STRING, "newtime", "New time for the boss spawn in HH:mm:ss format", true),
                new OptionData(OptionType.STRING, "newdate", "New date for the boss spawn in d/MM/yyyy format", true)
            );
        jda.upsertCommand(editTimer).queue();

    }
}
