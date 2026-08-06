package ddlc.yuri.modules.impl.player.antivoid;

import ddlc.yuri.api.events.impl.player.MotionEvent;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.utils.misc.IMinecraft;

public interface AntiVoidMode extends IMinecraft {
    default void onMotion(MotionEvent event) {}
    default void onPreUpdate(PreUpdateEvent event) {}
    default void onDisable() {}
    default void onEnable() {}
}
