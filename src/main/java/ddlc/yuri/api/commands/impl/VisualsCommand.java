package ddlc.yuri.api.commands.impl;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.commands.Command;
import ddlc.yuri.api.config.ConfigManager;
import ddlc.yuri.api.config.VisualsConfig;
import ddlc.yuri.utils.client.FolderUtils;
import ddlc.yuri.utils.client.LoggingUtils;

import java.io.File;

public class VisualsCommand extends Command {

    public VisualsCommand() {
        super("visuals", "Save, load or open the folder of visual module and HUD profiles.", "visualconfig");
    }

    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            LoggingUtils.sendChatMessage("Usage: .visuals save/load/delete/list/folder <name>");
            return;
        }

        String action = args[0].toLowerCase();
        String name = args.length > 1 ? join(args, 1) : ConfigManager.DEFAULT_PROFILE;

        switch (action) {
            case "save":
                if (Yuri.INSTANCE.getConfigManager().saveVisuals(name)) {
                    LoggingUtils.sendChatMessage("Successfully saved visuals profile " + name + "!");
                } else {
                    LoggingUtils.sendChatMessage("Failed to save visuals profile " + name + ".");
                }
                break;

            case "load":
                if (Yuri.INSTANCE.getConfigManager().loadVisuals(name)) {
                    LoggingUtils.sendChatMessage("Successfully loaded visuals profile " + name + "!");
                } else {
                    LoggingUtils.sendChatMessage("Failed to load visuals profile " + name + ".");
                }
                break;

            case "delete": {
                if (name.equalsIgnoreCase(ConfigManager.DEFAULT_PROFILE)) {
                    LoggingUtils.sendChatMessage("You cannot delete the autosaved " + ConfigManager.DEFAULT_PROFILE + " profile.");
                    break;
                }

                if (Yuri.INSTANCE.getConfigManager().deleteVisuals(name)) {
                    LoggingUtils.sendChatMessage("Successfully deleted visuals profile " + name + ".");
                } else {
                    LoggingUtils.sendChatMessage("The visuals profile " + name + " does not exist.");
                }
                break;
            }

            case "list":
                listProfiles();
                break;

            case "folder":
                openFolder();
                break;

            default:
                LoggingUtils.sendChatMessage("Usage: .visuals save/load/delete/list/folder <name>");
        }
    }

    private void listProfiles() {
        if (Yuri.INSTANCE.getConfigManager().getVisualsConfigs().isEmpty()) {
            LoggingUtils.sendChatMessage("No visuals profiles found.");
            return;
        }

        for (VisualsConfig visualsConfig : Yuri.INSTANCE.getConfigManager().getVisualsConfigs()) {
            LoggingUtils.sendChatMessage(visualsConfig.getName());
        }
    }

    private void openFolder() {
        File directory = ConfigManager.VISUALS_DIR;
        LoggingUtils.sendChatMessage("Visuals folder: " + directory.getAbsolutePath());

        if (!FolderUtils.openFolder(directory)) {
            LoggingUtils.sendChatMessage("Could not open the folder automatically.");
        }
    }

    /** Joins the remaining args so profile names with spaces still work. */
    static String join(String[] args, int from) {
        StringBuilder builder = new StringBuilder();

        for (int i = from; i < args.length; i++) {
            if (builder.length() > 0) builder.append(' ');
            builder.append(args[i]);
        }

        return builder.toString();
    }
}