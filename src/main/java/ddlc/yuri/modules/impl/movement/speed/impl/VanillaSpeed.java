package ddlc.yuri.modules.impl.movement.speed.impl;

import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.modules.impl.movement.SpeedModule;
import ddlc.yuri.modules.impl.movement.speed.SpeedMode;
import ddlc.yuri.utils.player.MoveUtils;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class VanillaSpeed implements SpeedMode {
    private final SpeedModule parent;

    @Override
    public void onPreUpdate(PreUpdateEvent event) {
        if (MoveUtils.isMoving()) {
            if (MoveUtils.isOnGround()) mc.thePlayer.jump();
            MoveUtils.setSpeed(parent.speed.getValue());
        }
    }
}
