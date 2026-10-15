package ddlc.yuri.modules.impl.combat.velocity;

import ddlc.yuri.api.events.impl.client.ClientTickEvent;
import ddlc.yuri.api.events.impl.client.PacketReceivedEvent;
import ddlc.yuri.api.events.impl.client.PacketSendEvent;
import ddlc.yuri.api.events.impl.player.HitSlowDownEvent;
import ddlc.yuri.api.events.impl.player.PlayerAttackEvent;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.api.events.impl.player.StrafeEvent;
import ddlc.yuri.utils.misc.IMinecraft;

public interface VelocityMode extends IMinecraft {
    default void onPacket(PacketReceivedEvent event) {}
    default void onPacketSend(PacketSendEvent event) {}
    default void onTick(ClientTickEvent event) {}
    default void onStrafe(StrafeEvent event) {}
    default void onAttack(PlayerAttackEvent event) {}
    default void onPreUpdate(PreUpdateEvent event) {}
    default void onHitSlowdown(HitSlowDownEvent event) {}

    /**
     * Called when the module is toggled off so modes can drop per-session state instead of
     * leaking it into the next time they are selected.
     */
    default void reset() {}
}
