package ddlc.yuri.modules.impl.movement.speed.impl;

import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.modules.impl.movement.speed.SpeedMode;
import ddlc.yuri.utils.player.MoveUtils;

public class IntaveSpeed implements SpeedMode {
    @Override
    public void onPreUpdate(PreUpdateEvent event) {
        mc.gameSettings.keyBindJump.pressed = MoveUtils.isMoving()
                && MoveUtils.isOnGround();

        switch (mc.thePlayer.offGroundTicks) {
            case 1:
                mc.thePlayer.motionX *= 1.005;
                mc.thePlayer.motionZ *= 1.005;
                break;
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
                mc.thePlayer.motionX *= 1.011;
                mc.thePlayer.motionZ *= 1.001;
                break;
        }

        if (mc.thePlayer.offGroundTicks == 1) {
            mc.thePlayer.motionX *= 1.0045;
            mc.thePlayer.motionZ *= 1.0045;
        }

        mc.timer.timerSpeed = 1.0075f;
    }
}
