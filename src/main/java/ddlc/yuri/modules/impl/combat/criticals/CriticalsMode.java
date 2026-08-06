package ddlc.yuri.modules.impl.combat.criticals;

import ddlc.yuri.api.events.impl.player.MotionEvent;
import ddlc.yuri.api.events.impl.player.PlayerAttackEvent;
import ddlc.yuri.api.events.impl.player.StrafeEvent;
import ddlc.yuri.utils.misc.IMinecraft;

public interface CriticalsMode extends IMinecraft {
    default void onAttack(PlayerAttackEvent event) {}
    default void onStrafe(StrafeEvent event) {}
    default void onEnable() {}
    default void onMotion(MotionEvent event) {}
    default void onDisable() {}
}
