package ddlc.yuri.api.commands.impl;

import ddlc.yuri.api.commands.Command;
import ddlc.yuri.managers.impl.CommandManager;
import ddlc.yuri.utils.client.LoggingUtils;

public class HelpCommand extends Command {
    public HelpCommand() {
        super("help", "help meeee", "h");
    }

    @Override
    public void execute(String[] args) {
        for (Command command : CommandManager.INSTANCE.getCommands().values()) {
            LoggingUtils.sendChatMessage(command.getName() + " - " + command.getDescription() + " ["+command.getAlias()+"]");
        }
    }
}
