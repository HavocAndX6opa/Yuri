package ddlc.yuri.utils.client;

import java.awt.Desktop;
import java.io.File;

/**
 * Opens a directory in the OS file manager, falling back to the platform CLI when AWT's
 * Desktop integration is unavailable (common on minimal Linux installs).
 */
public final class FolderUtils {

    private FolderUtils() {
    }

    public static boolean openFolder(File directory) {
        if (directory == null) return false;

        if (!directory.exists() && !directory.mkdirs()) {
            return false;
        }

        if (Desktop.isDesktopSupported()) {
            try {
                Desktop desktop = Desktop.getDesktop();

                if (desktop.isSupported(Desktop.Action.OPEN)) {
                    desktop.open(directory);
                    return true;
                }
            } catch (Throwable ignored) {
            }
        }

        return openWithOsCommand(directory);
    }

    private static boolean openWithOsCommand(File directory) {
        String os = System.getProperty("os.name", "").toLowerCase();
        String path = directory.getAbsolutePath();
        String[] command;

        if (os.contains("win")) {
            command = new String[]{"explorer", path};
        } else if (os.contains("mac") || os.contains("darwin")) {
            command = new String[]{"open", path};
        } else {
            command = new String[]{"xdg-open", path};
        }

        try {
            new ProcessBuilder(command).start();
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}