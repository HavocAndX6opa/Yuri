package ddlc.yuri.modules.impl.player.nofall;

import ddlc.yuri.api.events.impl.client.ClientTickEvent;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.utils.misc.IMinecraft;

public interface NoFallMode extends IMinecraft {
    default void onTick(ClientTickEvent event) {}
    default void onPreUpdate(PreUpdateEvent event) {}
    default void onDisable() {}
}
