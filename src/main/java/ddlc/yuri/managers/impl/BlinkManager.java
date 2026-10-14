package ddlc.yuri.managers.impl;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.client.PacketReceivedEvent;
import ddlc.yuri.api.events.impl.client.PacketSendEvent;
import ddlc.yuri.api.events.impl.player.MotionEvent;
import ddlc.yuri.api.events.impl.world.WorldJoinEvent;
import ddlc.yuri.utils.player.packet.PacketUtils;
import net.minecraft.network.Packet;

import java.util.ArrayList;
import java.util.List;

public class BlinkManager {
    public static boolean enabled;
    private static boolean disable = false;
    private static boolean cancelReceived = false;
    public static final List<Packet<?>> blinkedSendPackets = new ArrayList<>();
    public static final List<Packet<?>> blinkedReceivePackets = new ArrayList<>();

    /**
     * A blink that is never released (lag, disconnect, a macro stuck on) would otherwise keep
     * every packet alive for the rest of the session. Drop the oldest once we hit the cap so a
     * stuck blink cannot eat the heap.
     */
    private static final int MAX_BUFFERED_PACKETS = 8192;

    private static void buffer(List<Packet<?>> buffer, Packet<?> packet) {
        if (buffer.size() >= MAX_BUFFERED_PACKETS) {
            buffer.remove(0);
        }

        buffer.add(packet);
    }

    @EventHook
    public void onPacketSend(PacketSendEvent event) {
        if (enabled) {
            buffer(blinkedSendPackets, event.getPacket());
            event.setCancelled(true);
        }
    }

    @EventHook
    public void onPacketReceived(PacketReceivedEvent event) {
        if (enabled && cancelReceived) {
            buffer(blinkedReceivePackets, event.getPacket());
            event.setCancelled(true);
        }
    }

    @EventHook
    public void onWorldJoin(WorldJoinEvent event) {
        if (enabled) disable();
    }

    @EventHook
    public void onMotion(MotionEvent event) {
        if (event.isPre()) return;
        if (!disable) return;
        enabled = false;
        disable = false;

        List<Packet<?>> sendCopy = new ArrayList<>(blinkedSendPackets);
        List<Packet<?>> receiveCopy = new ArrayList<>(blinkedReceivePackets);
        blinkedSendPackets.clear();
        blinkedReceivePackets.clear();

        for (Packet<?> packet : sendCopy) PacketUtils.queue(packet);
        for (Packet<?> packet : receiveCopy) PacketUtils.queue(packet);
    }

    public static void enable(boolean cancelReceivedPackets) {
        blinkedSendPackets.clear();
        blinkedReceivePackets.clear();
        cancelReceived = cancelReceivedPackets;
        enabled = true;
    }

    public static void disable() {
        disable = true;
    }
}
