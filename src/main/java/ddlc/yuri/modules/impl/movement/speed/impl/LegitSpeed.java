package ddlc.yuri.modules.impl.movement.speed.impl;

import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.modules.impl.movement.speed.SpeedMode;
import ddlc.yuri.utils.player.MoveUtils;

public class LegitSpeed implements SpeedMode {
    @Override
    public void onPreUpdate(PreUpdateEvent event) {
        mc.gameSettings.keyBindJump.pressed =
                MoveUtils.isMoving()
                        && MoveUtils.isOnGround();
    }
}
