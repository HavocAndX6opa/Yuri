package ddlc.yuri.modules.impl.movement.speed.impl;

import ddlc.yuri.api.events.impl.player.StrafeEvent;
import ddlc.yuri.modules.impl.movement.speed.SpeedMode;
import ddlc.yuri.utils.player.MoveUtils;

public class LegitSpeed implements SpeedMode {
    @Override
    public void onStrafe(StrafeEvent event) {
        if (MoveUtils.isMoving() && mc.thePlayer.onGround && !mc.gameSettings.keyBindJump.pressed && !(mc.thePlayer.isInLava() || mc.thePlayer.isInWater() || mc.thePlayer.isInWeb)) {
            mc.thePlayer.jump();
        }
    }
}
