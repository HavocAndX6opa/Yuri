package ddlc.yuri.modules.impl.movement.noslow;

import ddlc.yuri.api.events.impl.player.ItemSlowdownEvent;
import ddlc.yuri.api.events.impl.player.MotionEvent;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.api.events.impl.player.StrafeEvent;
import ddlc.yuri.utils.misc.IMinecraft;

public interface NoSlowMode extends IMinecraft {
    default void onSlowdown(ItemSlowdownEvent event) {}
    default void onPreUpdate(PreUpdateEvent event) {}
    default void onMotion(MotionEvent event) {}
    default void onStrafe(StrafeEvent event) {}
}