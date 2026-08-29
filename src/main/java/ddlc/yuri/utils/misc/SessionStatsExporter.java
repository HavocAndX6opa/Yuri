package ddlc.yuri.utils.misc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

/**
 * Publishes the current session's stats so Yuri Launcher can show them.
 *
 * <p>The launcher is a separate process, so it cannot read the manager's fields
 * directly. Instead this writes one small JSON file into the game directory -
 * whatever folder the launcher passed as {@code --gameDir}, which is a setting
 * in the Tauri launcher (default {@code %APPDATA%\.minecraft}) and fixed at
 * {@code %APPDATA%\.yuriclient} in the older C++ one - and the launcher polls
 * it while its session popup is open:
 *
 * <pre>
 * {"schema":1,"alive":true,"username":"nvmop","server":"Singleplayer",
 *  "kills":0,"deaths":0,"wins":0,"sessionMs":0}
 * </pre>
 *
 * <p>Every number is counted by {@code SessionStatsManager} - a manager, not the
 * HUD module, so they keep counting with the overlay switched off - and handed
 * in. This class decides nothing; it used to guess deaths from the player's
 * health, which made it possible for the two to count the same death twice, so
 * that guesswork now lives in the manager alongside the other death signals.
 *
 * <p>Writes are throttled to once a second and go straight to the target file
 * rather than through a temp-file rename: on Windows a rename needs the target
 * deleted first, and that gap would make the launcher briefly see no file at
 * all. The launcher keeps its previous values whenever a read comes back
 * truncated, so writing in place is the safer trade.
 *
 * <p>Nothing here throws. If the file cannot be written the launcher simply
 * falls back to its own numbers (playtime, session count, last played).
 */
public final class SessionStatsExporter {

    static final String FILE_NAME = "yuri_session.json";
    private static final long WRITE_INTERVAL_MS = 1000L;

    private static long lastWriteAt;

    private static String lastUsername = "";
    private static String lastServer = "";
    private static int lastKills;
    private static int lastDeaths;
    private static int lastWins;
    private static long lastSessionMs;

    private SessionStatsExporter() {
    }

    /** Throttled: safe to call every tick. */
    public static void publish(File gameDir, String username, String server,
                               int kills, int deaths, int wins, long sessionStart) {
        // Before the throttle check, and with the numbers as they are right now:
        // the log is meant to prove whether this method runs and whether the stats
        // move, so it must not be filtered by the write interval.
        SessionStatsDebug.sample(gameDir, username, server,
                kills, deaths, wins, sessionStart);

        long now = System.currentTimeMillis();

        // Cached before the throttle, so these fields always hold the newest
        // numbers even on the calls that write nothing. publishStopped() writes
        // them without being handed anything, and a death in the last second
        // before the client exits used to be lost that way: the throttle skipped
        // the write, the fields kept the second-old values, and the "stats from
        // the last session" the launcher then showed were one death short.
        lastUsername = username == null ? "" : username;
        lastServer = server == null ? "" : server;
        lastKills = kills;
        lastDeaths = deaths;
        lastWins = wins;
        lastSessionMs = Math.max(0L, now - sessionStart);

        if (now - lastWriteAt < WRITE_INTERVAL_MS) {
            return;
        }
        lastWriteAt = now;

        write(gameDir, true);
    }

    /**
     * Marks the session as finished, keeping the last numbers so the launcher
     * can still show them as "stats from the last session".
     */
    public static void publishStopped(File gameDir) {
        lastWriteAt = 0L;
        SessionStatsDebug.note(gameDir, "session stopped - client shutting down,"
                + " final kills=" + lastKills + " deaths=" + lastDeaths
                + " wins=" + lastWins);
        write(gameDir, false);
    }

    private static void write(File gameDir, boolean alive) {
        if (gameDir == null) {
            return;
        }

        StringBuilder json = new StringBuilder(160);
        json.append("{\"schema\":1")
                .append(",\"alive\":").append(alive)
                .append(",\"username\":\"").append(escape(lastUsername)).append('"')
                .append(",\"server\":\"").append(escape(lastServer)).append('"')
                .append(",\"kills\":").append(lastKills)
                .append(",\"deaths\":").append(lastDeaths)
                .append(",\"wins\":").append(lastWins)
                .append(",\"sessionMs\":").append(lastSessionMs)
                .append('}');

        Writer out = null;
        try {
            if (!gameDir.isDirectory() && !gameDir.mkdirs()) {
                SessionStatsDebug.failed(gameDir, "create game dir", null);
                return;
            }
            out = new OutputStreamWriter(
                    new FileOutputStream(new File(gameDir, FILE_NAME), false), "UTF-8");
            out.write(json.toString());
            out.flush();
            SessionStatsDebug.wrote(gameDir, json.toString());
        } catch (Throwable error) {
            // Still swallowed - but no longer silent, which is the whole point
            // of the debug class: a permission or path problem here used to look
            // exactly like "the mod is not running".
            SessionStatsDebug.failed(gameDir, "write " + FILE_NAME, error);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    /** The launcher's reader is a flat-JSON scanner, so keep values plain. */
    private static String escape(String value) {
        StringBuilder sb = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '"' || c == '\\') {
                sb.append('\\').append(c);
            } else if (c >= ' ' && c != 0x7F) {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
