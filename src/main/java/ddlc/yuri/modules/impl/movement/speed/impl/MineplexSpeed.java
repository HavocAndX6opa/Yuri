package ddlc.yuri.modules.impl.movement.speed.impl;

import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.modules.impl.movement.speed.SpeedMode;
import ddlc.yuri.utils.player.MoveUtils;

public class MineplexSpeed implements SpeedMode {
    @Override
    public void onPreUpdate(PreUpdateEvent event) {
        mc.gameSettings.keyBindJump.pressed =
                MoveUtils.isMoving()
                        && MoveUtils.isOnGround();

        if (mc.thePlayer.offGroundTicks > 0) {
            mc.timer.timerSpeed = 1.8f;
        } else {
            mc.timer.timerSpeed = 1.0f;
        }
    }
}
