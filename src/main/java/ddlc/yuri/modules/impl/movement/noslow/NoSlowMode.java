package ddlc.yuri.modules.impl.movement.noslow;

import ddlc.yuri.api.events.impl.client.PacketSendEvent;
import ddlc.yuri.api.events.impl.player.*;
import ddlc.yuri.utils.misc.IMinecraft;

public interface NoSlowMode extends IMinecraft {
    default void onSlowdown(ItemSlowdownEvent event) {}
    default void onRightClick(RightClickEvent event) {}
    default void onPreUpdate(PreUpdateEvent event) {}
    default void onPacketSend(PacketSendEvent event) {}
    default void onMotion(MotionEvent event) {}
    default void onStrafe(StrafeEvent event) {}
}