package ddlc.yuri.modules.impl.movement.speed;

import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.utils.misc.IMinecraft;

public interface SpeedMode extends IMinecraft {
    default void onPreUpdate(PreUpdateEvent event) {}
}
