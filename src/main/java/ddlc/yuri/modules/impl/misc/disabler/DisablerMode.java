package ddlc.yuri.modules.impl.misc.disabler;

import ddlc.yuri.api.events.impl.client.PacketReceivedEvent;
import ddlc.yuri.api.events.impl.client.PacketSendEvent;
import ddlc.yuri.api.events.impl.player.MotionEvent;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.api.events.impl.world.WorldJoinEvent;
import ddlc.yuri.utils.misc.IMinecraft;

public interface DisablerMode extends IMinecraft {
    default void onPreUpdate(PreUpdateEvent event) {}
    default void onMotion(MotionEvent event) {}
    default void onPacketReceived(PacketReceivedEvent event) {}
    default void onPacketSend(PacketSendEvent event) {}
    default void onWorldJoin(WorldJoinEvent event) {}
}
