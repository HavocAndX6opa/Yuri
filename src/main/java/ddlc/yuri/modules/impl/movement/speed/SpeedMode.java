package ddlc.yuri.modules.impl.movement.speed;

import ddlc.yuri.api.events.impl.client.PacketReceivedEvent;
import ddlc.yuri.api.events.impl.player.MotionEvent;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.api.events.impl.player.StrafeEvent;
import ddlc.yuri.utils.misc.IMinecraft;

public interface SpeedMode extends IMinecraft {
    default void onPreUpdate(PreUpdateEvent event) {}
    default void onMotion(MotionEvent event) {}
    default void onStrafe(StrafeEvent event) {}
    default void onPacketReceived(PacketReceivedEvent event) {}
    default void onDisable() {}
    default void onEnable() {}

}