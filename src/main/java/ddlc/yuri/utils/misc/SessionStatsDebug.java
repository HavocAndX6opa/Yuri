package ddlc.yuri.utils.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Diagnostic for the launcher's session panel. Answers, in order, the three
 * questions that a panel showing nothing cannot answer by itself:
 *
 * <ol>
 *   <li><b>Is the exporter running at all?</b> If
 *       {@code yuri_session_debug.log} does not appear in the game directory
 *       (the folder the launcher passed as {@code --gameDir}) shortly after a
 *       world loads, then
 *       {@link SessionStatsExporter#publish} is never being called - which now
 *       means the patch is not in the build, since the caller is
 *       {@code SessionStatsManager} and a manager is subscribed for the whole
 *       client run.</li>
 *   <li><b>Do kills and deaths actually change?</b> Every change is logged and
 *       announced in chat the moment it happens, with the health reading that
 *       triggered it.</li>
 *   <li><b>Is the JSON file being written?</b> The exporter deliberately
 *       swallows every exception, so a failed write is otherwise completely
 *       silent. Failures land here with the exception attached.</li>
 * </ol>
 *
 * <p>Everything is opt-out at runtime: set {@link #chat} or {@link #file} to
 * false to silence either channel. Nothing here throws, and nothing here is
 * required - {@link SessionStatsExporter} works with this class deleted, as
 * long as its four calls go with it.
 */
public final class SessionStatsDebug {

    /** In-game chat notices for each kill, death and win. */
    public static boolean chat = true;

    /** The log file next to yuri_session.json in the game directory. */
    public static boolean file = true;

    public static final String FILE_NAME = "yuri_session_debug.log";

    /** A stat change is rare; this is the "still alive, still publishing" tick. */
    private static final long HEARTBEAT_MS = 5000L;

    /** Truncated rather than rotated: only the current session is interesting. */
    private static final long MAX_BYTES = 256L * 1024L;

    /**
     * Minecraft's colour-code prefix, built from its code point instead of
     * typed as a literal: Gradle compiles with the platform encoding by
     * default, so a UTF-8 source file read as cp1252 would turn the character
     * into mojibake and print the code instead of colouring the text. Keeping
     * this file pure ASCII sidesteps the whole question.
     */
    private static final char COLOUR = (char) 0x00A7;

    private static long trackedSessionStart = Long.MIN_VALUE;
    private static int lastKills;
    private static int lastDeaths;
    private static int lastWins;
    private static long lastHeartbeatAt;
    private static long publishCalls;
    private static String lastJson;
    private static String lastFailure;

    private SessionStatsDebug() {
    }

    /**
     * Called from {@link SessionStatsExporter#publish} on every tick, before its
     * once-a-second throttle, so a change is seen the moment it happens rather
     * than up to a second late.
     */
    public static void sample(File gameDir, String username, String server,
                              int kills, int deaths, int wins, long sessionStart) {
        try {
            publishCalls++;

            // Same signal the exporter uses to reset its death counter: a new
            // sessionStart means a new client run - SessionStatsManager sets it
            // once, in its constructor.
            if (sessionStart != trackedSessionStart) {
                trackedSessionStart = sessionStart;
                lastKills = kills;
                lastDeaths = deaths;
                lastWins = wins;
                publishCalls = 1L;
                lastHeartbeatAt = 0L;
                lastJson = null;
                lastFailure = null;
                truncate(gameDir);
                write(gameDir, "session start"
                        + " | user=" + username
                        + " | server=" + server
                        + " | kills=" + kills + " deaths=" + deaths + " wins=" + wins);
                write(gameDir, "writing stats to "
                        + path(gameDir, SessionStatsExporter.FILE_NAME));
                write(gameDir, "logging to " + path(gameDir, FILE_NAME));
                say("Session stats logging started - " + FILE_NAME);
            }

            if (kills != lastKills) {
                change(gameDir, kills > lastKills ? "KILL" : "kills reset",
                        "kills", lastKills, kills);
                lastKills = kills;
            }
            if (deaths != lastDeaths) {
                change(gameDir, deaths > lastDeaths ? "DEATH" : "deaths reset",
                        "deaths", lastDeaths, deaths);
                lastDeaths = deaths;
            }
            if (wins != lastWins) {
                change(gameDir, wins > lastWins ? "WIN" : "wins reset",
                        "wins", lastWins, wins);
                lastWins = wins;
            }

            long now = System.currentTimeMillis();
            if (now - lastHeartbeatAt >= HEARTBEAT_MS) {
                lastHeartbeatAt = now;
                write(gameDir, "alive | publish calls=" + publishCalls
                        + " | kills=" + kills + " deaths=" + deaths + " wins=" + wins
                        + " | server=" + server + " | " + health());
            }
        } catch (Throwable ignored) {
            // A diagnostic that can crash the game is worse than no diagnostic.
        }
    }

    /**
     * Logged from the exporter's successful write, but only when the JSON has
     * actually changed. The exporter writes once a second whether or not
     * anything moved, and a log line per second buries the interesting ones.
     */
    public static void wrote(File gameDir, String json) {
        if (json == null || json.equals(lastJson)) {
            return;
        }
        lastJson = json;
        write(gameDir, "wrote " + json);
    }

    /** The exporter swallows its exceptions - this is where they surface. */
    public static void failed(File gameDir, String what, Throwable error) {
        String detail = error == null ? "no exception"
                : error.getClass().getName() + ": " + error.getMessage();
        write(gameDir, "FAILED " + what + " | " + detail);
        // A failing write fails every second; say it once so chat stays usable.
        String key = what + "|" + detail;
        if (!key.equals(lastFailure)) {
            lastFailure = key;
            say(COLOUR + "cSession stats: " + what + " failed - " + detail);
        }
    }

    /** Free-form note, for wiring the logger into anything else. */
    public static void note(File gameDir, String message) {
        write(gameDir, message);
    }

    private static void change(File gameDir, String label, String field,
                               int from, int to) {
        write(gameDir, label + " | " + field + " " + from + " -> " + to
                + " | " + health());
        say(label + " - " + field + " now " + to);
    }

    /**
     * The health reading is included on every line because deaths are inferred
     * from it: if a death is missed, this is the evidence for why.
     */
    private static String health() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.thePlayer == null) {
                return "no player";
            }
            return "health=" + mc.thePlayer.getHealth()
                    + " isDead=" + mc.thePlayer.isDead;
        } catch (Throwable ignored) {
            return "health unavailable";
        }
    }

    private static void say(String message) {
        if (!chat) {
            return;
        }
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.thePlayer == null) {
                return;
            }
            mc.thePlayer.addChatMessage(new ChatComponentText(
                    COLOUR + "d[Yuri] " + COLOUR + "f" + message));
        } catch (Throwable ignored) {
            // Chat is unavailable outside a world; the log file still has it.
        }
    }

    private static String path(File gameDir, String name) {
        try {
            return new File(gameDir, name).getAbsolutePath();
        } catch (Throwable ignored) {
            return name;
        }
    }

    private static void truncate(File gameDir) {
        try {
            File target = new File(gameDir, FILE_NAME);
            if (target.isFile() && !target.delete()) {
                // Deletion can fail if something has it open; the size cap in
                // write() will deal with it on the next line instead.
                return;
            }
        } catch (Throwable ignored) {
        }
    }

    /** Appends one timestamped line. Opens and closes per line on purpose: a
     *  crash must not cost the last thing that happened before it. */
    private static void write(File gameDir, String message) {
        if (!file || gameDir == null) {
            return;
        }
        Writer out = null;
        try {
            if (!gameDir.isDirectory() && !gameDir.mkdirs()) {
                return;
            }
            File target = new File(gameDir, FILE_NAME);
            boolean append = target.isFile() && target.length() < MAX_BYTES;
            out = new OutputStreamWriter(new FileOutputStream(target, append), "UTF-8");
            out.write(new SimpleDateFormat("HH:mm:ss.SSS").format(new Date()));
            out.write("  ");
            out.write(message == null ? "null" : message);
            out.write("\r\n");
            out.flush();
        } catch (Throwable ignored) {
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (Throwable ignored) {
                }
            }
        }
    }
}
