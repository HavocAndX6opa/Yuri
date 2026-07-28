package ddlc.yuri.api.commands.impl;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.commands.Command;
import ddlc.yuri.api.config.Config;
import ddlc.yuri.api.config.ConfigManager;
import ddlc.yuri.utils.client.LoggingUtils;

import java.io.File;

public class ConfigCommand extends Command {

    public ConfigCommand() {
        super("config", "Manage your client configs.", "c");
    }

    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            LoggingUtils.sendChatMessage("Usage: .config save/load/list/delete");
            return;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "save": {
                if (args.length < 2) {
                    LoggingUtils.sendChatMessage("Usage: .config save <config name>");
                    return;
                }

                String name = args[1];
                Yuri.INSTANCE.getConfigManager().saveConfig(name);
                LoggingUtils.sendChatMessage("Successfully saved config " + name + "!");
                break;
            }

            case "load": {
                if (args.length < 2) {
                    LoggingUtils.sendChatMessage("Usage: .config load <config name>");
                    return;
                }

                String name = args[1];
                Yuri.INSTANCE.getConfigManager().loadConfig(name);
                LoggingUtils.sendChatMessage("Successfully loaded config " + name + "!");
                break;
            }

            case "list": {
                listConfigs();
                break;
            }

            case "delete": {
                if (args.length < 2) {
                    LoggingUtils.sendChatMessage("Usage: .config delete <config name>");
                    return;
                }

                String name = args[1];
                if (deleteConfig(name)) {
                    LoggingUtils.sendChatMessage("Successfully deleted config profile " + name + ".");
                } else {
                    LoggingUtils.sendChatMessage("The config " + name + " does not exist.");
                }
                break;
            }

            default:
                LoggingUtils.sendChatMessage("Usage: .config delete <config name>");
        }
    }

    private void listConfigs() {
        if (Yuri.INSTANCE.getConfigManager().getElements().isEmpty()) {
            LoggingUtils.sendChatMessage("No configs found.");
            return;
        }
        for (Config config : Yuri.INSTANCE.getConfigManager().getElements()) {
            LoggingUtils.sendChatMessage(config.getName());
        }
    }

    private boolean deleteConfig(String name) {
        Config config = Yuri.INSTANCE.getConfigManager().findConfig(name);
        if (config == null) {
            File file = new File(ConfigManager.CONFIGS_DIR, name + ".json");
            return file.exists() && file.delete();
        }
        return false;
    }
}