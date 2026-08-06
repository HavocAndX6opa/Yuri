package ddlc.yuri.modules.impl.movement.flight;

import ddlc.yuri.api.events.impl.player.MotionEvent;
import ddlc.yuri.api.events.impl.player.StrafeEvent;
import ddlc.yuri.utils.misc.IMinecraft;

public interface FlightMode extends IMinecraft {
    default void onMotion(MotionEvent event) {}
    default void onStrafe(StrafeEvent event) {}
}
