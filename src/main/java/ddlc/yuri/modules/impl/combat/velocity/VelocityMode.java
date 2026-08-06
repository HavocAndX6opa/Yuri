package ddlc.yuri.modules.impl.combat.velocity;

import ddlc.yuri.api.events.impl.client.ClientTickEvent;
import ddlc.yuri.api.events.impl.client.PacketReceivedEvent;
import ddlc.yuri.api.events.impl.player.HitSlowDownEvent;
import ddlc.yuri.api.events.impl.player.PlayerAttackEvent;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.utils.misc.IMinecraft;

public interface VelocityMode extends IMinecraft {
    default void onPacket(PacketReceivedEvent event) {}
    default void onTick(ClientTickEvent event) {}
    default void onAttack(PlayerAttackEvent event) {}
    default void onPreUpdate(PreUpdateEvent event) {}
    default void onHitSlowdown(HitSlowDownEvent event) {}
}
