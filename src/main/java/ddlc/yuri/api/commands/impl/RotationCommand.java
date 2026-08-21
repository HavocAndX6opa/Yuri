package ddlc.yuri.api.commands.impl;

import ddlc.yuri.api.commands.Command;
import ddlc.yuri.managers.impl.RotationLearnerManager;
import ddlc.yuri.utils.misc.IMinecraft;
import net.minecraft.util.ChatComponentText;

import java.util.Arrays;
import java.util.List;

public class RotationCommand extends Command implements IMinecraft {

    public RotationCommand() {
        super("rotation", "Manage rotation learning presets.", "rot");
    }

    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            sendMessage("Usage: .rotation <record|stop|load|unload|list|delete|status> [name]");
            return;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "record": {
                if (args.length < 2) {
                    sendMessage("Usage: .rotation record <name>");
                    return;
                }
                String name = joinName(args);
                RotationLearnerManager.startRecording(name);
                sendMessage("Recording rotation data to preset '" + name + "'.");
                break;
            }

            case "stop":
                RotationLearnerManager.stopRecording();
                sendMessage("Stopped recording.");
                break;

            case "load": {
                if (args.length < 2) {
                    sendMessage("Usage: .rotation load <name>");
                    return;
                }
                String name = joinName(args);
                boolean loaded = RotationLearnerManager.loadPreset(name);
                sendMessage(loaded ? "Loaded preset '" + name + "'." : "Preset '" + name + "' not found or empty.");
                break;
            }

            case "unload":
                RotationLearnerManager.unloadPreset();
                sendMessage("Unloaded active preset.");
                break;

            case "list":
                sendPresetList();
                break;

            case "delete": {
                if (args.length < 2) {
                    sendMessage("Usage: .rotation delete <name>");
                    return;
                }
                String name = joinName(args);
                if (RotationLearnerManager.isBuiltIn(name)) {
                    sendMessage("Preset '" + name + "' is built-in and cannot be deleted.");
                    return;
                }
                boolean deleted = RotationLearnerManager.deletePreset(name);
                sendMessage(deleted ? "Deleted preset '" + name + "'." : "Preset '" + name + "' not found.");
                break;
            }

            case "status": {
                String status = RotationLearnerManager.isRecording()
                        ? "Recording to '" + RotationLearnerManager.getActivePresetName() + "'."
                        : "Not recording.";
                status += RotationLearnerManager.hasModelLoaded() ? " Model loaded." : " No model loaded.";
                sendMessage(status);
                break;
            }

            default:
                sendMessage("Unknown subcommand. Usage: .rotation <record|stop|load|unload|list|delete|status> [name]");
                break;
        }
    }

    private void sendPresetList() {
        List<String> presets = RotationLearnerManager.listPresets();
        if (presets.isEmpty()) {
            sendMessage("No rotation presets saved. Use '.rotation record <name>' to create one.");
            return;
        }

        presets.sort(String.CASE_INSENSITIVE_ORDER);

        sendMessage("--- Rotation Presets (" + presets.size() + ") ---");
        for (int i = 0; i < presets.size(); i++) {
            String name = presets.get(i);
            int samples = RotationLearnerManager.getSampleCount(name);
            boolean builtIn = RotationLearnerManager.isBuiltIn(name);
            boolean active = RotationLearnerManager.hasModelLoaded()
                    && name.equals(RotationLearnerManager.getActivePresetName());
            sendMessage((i + 1) + ". " + name + " [" + samples + " samples]"
                    + (builtIn ? " (built-in)" : "") + (active ? " <loaded>" : ""));
        }
        sendMessage("-------------------------");
    }

    private static String joinName(String[] args) {
        return String.join(" ", Arrays.copyOfRange(args, 1, args.length));
    }

    private void sendMessage(String message) {
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText(message));
        }
    }
}
