package ddlc.yuri.modules.impl.movement.speed.impl;

import ddlc.yuri.api.events.impl.player.StrafeEvent;
import ddlc.yuri.modules.impl.movement.SpeedModule;
import ddlc.yuri.modules.impl.movement.speed.SpeedMode;
import ddlc.yuri.utils.player.MoveUtils;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class VanillaSpeed implements SpeedMode {
    private final SpeedModule parent;

    @Override
    public void onStrafe(StrafeEvent event) {
        if (MoveUtils.isMoving() && mc.thePlayer.onGround) {
            mc.thePlayer.jump();
        }

        event.setSpeed(parent.speed.getValue().floatValue());
    }
}
