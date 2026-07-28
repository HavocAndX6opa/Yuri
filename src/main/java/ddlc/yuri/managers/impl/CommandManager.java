package ddlc.yuri.managers.impl;

import ddlc.yuri.api.commands.Command;
import ddlc.yuri.api.commands.impl.BindCommand;
import ddlc.yuri.api.commands.impl.ConfigCommand;
import ddlc.yuri.api.commands.impl.HelpCommand;
import ddlc.yuri.api.commands.impl.ToggleCommand;
import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.player.ChatEvent;
import ddlc.yuri.utils.client.LoggingUtils;
import lombok.Getter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class CommandManager {
    public static final CommandManager INSTANCE = new CommandManager();

    @Getter
    private final Map<String, Command> commands = new HashMap<>();
    private static final String prefix = ".";

    public CommandManager() {
        addCommand(new HelpCommand());
        addCommand(new ToggleCommand());
        addCommand(new ConfigCommand());
        addCommand(new BindCommand());

    }

    public void addCommand(Command command) {
        commands.put(command.getName(), command);
    }

    public boolean processor(String input) {
        if(input == null || input.isEmpty()) {
            return false;
        }

        if(!input.startsWith(prefix)) {
            return false;
        }
        input = input.substring(prefix.length()).trim();
        String[] parts = input.split("\\s+");
        String commandName = parts[0];
        Command command = null;

        for (Command cmd : commands.values()) {
            if(cmd.matches(commandName)) {
                command = cmd;
                break;
            }
        }

        if(command == null) {
            LoggingUtils.sendChatMessage("Command does not exist.");
            return true;
        }

        String[] args = Arrays.copyOfRange(parts, 1, parts.length);
        command.execute(args);
        return true;
    }

    public boolean handleMessages(String text) {
        return text.startsWith(prefix) && processor(text);
    }


    @EventHook
    public void onChatMessage(ChatEvent event) {
        if (handleMessages(event.message)) {
            event.setCancelled(true);
        }
    }
}
