package ddlc.yuri.modules.impl.misc;

import ddlc.yuri.api.properties.Property;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import net.arikia.dev.drpc.DiscordEventHandlers;
import net.arikia.dev.drpc.DiscordRPC;
import net.arikia.dev.drpc.DiscordRichPresence;
import net.minecraft.client.multiplayer.ServerData;

import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@ModuleInfo(label = "Discord RPC", category = ModuleCategory.MISC, description = "discord rpcc", enabledByDefault = true)
public final class DiscordRPCModule extends Module {

    private static final String APPLICATION_ID = "1541943239861342218";
    private static final String LARGE_IMAGE = "yuri";
    private static final String LARGE_TEXT = "Yuri Client";

    public final Property<Boolean> showServer = new Property<>("Show Server", true);
    public final Property<Boolean> showTimestamp = new Property<>("Show Timestamp", true);

    private DiscordRichPresence presence;
    private ExecutorService executor;
    private volatile boolean running;
    private long startTimestamp;

    private String lastState = "";
    private String lastDetails = "";

    @Override
    public void onEnable() {
        if (running) return;

        running = true;
        presence = new DiscordRichPresence();
        startTimestamp = Instant.now().getEpochSecond();
        executor = Executors.newSingleThreadExecutor();

        DiscordEventHandlers handlers = new DiscordEventHandlers.Builder()
                .setReadyEventHandler(user -> {})
                .build();

        DiscordRPC.discordInitialize(APPLICATION_ID, handlers, true, null);

       // no point update("Starting up...", "");

        executor.execute(() -> {
            while (running) {
                try {
                    DiscordRPC.discordRunCallbacks();
                    update(buildState(), buildDetails());
                    Thread.sleep(2000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (Exception ignored) {
                }
            }
        });
    }

    @Override
    public void onDisable() {
        running = false;

        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }

        try {
            DiscordRPC.discordShutdown();
        } catch (Exception ignored) {
        }

        lastState = "";
        lastDetails = "";
    }

    private String buildState() {
        if (mc.thePlayer == null || mc.theWorld == null) return "In the menus";
        return "Playing " + (mc.thePlayer.getName() == null ? "" : mc.thePlayer.getName());
    }

    private String buildDetails() {
        if (mc.thePlayer == null || mc.theWorld == null) return "";

        if (mc.isSingleplayer()) return "Singleplayer";

        if (!showServer.getValue()) return "Multiplayer";

        ServerData data = mc.getCurrentServerData();
        return data != null && data.serverIP != null ? data.serverIP : "Multiplayer";
    }

    private void update(String state, String details) {
        if (!running || presence == null) return;
        if (state.equals(lastState) && details.equals(lastDetails)) return;

        lastState = state;
        lastDetails = details;

        presence.state = state;
        presence.details = details;
        presence.largeImageKey = LARGE_IMAGE;
        presence.largeImageText = LARGE_TEXT;
        presence.startTimestamp = showTimestamp.getValue() ? startTimestamp : 0;

        DiscordRPC.discordUpdatePresence(presence);
    }
}
