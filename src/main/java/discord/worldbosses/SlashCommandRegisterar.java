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
        jda.upsertCommand(addTimer).queue();

        // Registering the deleteTimer command
        CommandData deleteTimer = new CommandData("deletetimer", "Delete a timer for a world boss")
            .addOptions(new OptionData(OptionType.STRING, "mapname", "Name of the map where the boss spawns", true));
        jda.upsertCommand(deleteTimer).queue();

        // Registering the editTimer command
        CommandData editTimer = new CommandData("edittimer", "Edit a timer for a world boss")
            .addOptions(
                new OptionData(OptionType.STRING, "mapname", "Name of the map where the boss spawns", true),
                new OptionData(OptionType.STRING, "newtime", "New time for the boss spawn in HH:mm:ss format", true),
                new OptionData(OptionType.STRING, "newdate", "New date for the boss spawn in d/MM/yyyy format", true)
            );
        jda.upsertCommand(editTimer).queue();
    }
}
