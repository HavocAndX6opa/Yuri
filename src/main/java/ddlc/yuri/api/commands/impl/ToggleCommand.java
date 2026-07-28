package ddlc.yuri.api.commands.impl;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.commands.Command;
import ddlc.yuri.modules.Module;

public class ToggleCommand extends Command {
    public ToggleCommand() {
        super("toggle",
                "Toggle modules by commands.", "t");
    }

    @Override
    public void execute(String[] args) {

        if (args.length == 0) {
            return;
        }

        final String moduleName = args[0];
        final Module module = Yuri.INSTANCE.getModuleManager().getModule(moduleName);

        if (module != null) {
            module.toggle();
        }
    }
}
