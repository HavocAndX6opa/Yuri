package ddlc.yuri.api.events.impl.player;

import ddlc.yuri.api.events.CancellableEvent;
import ddlc.yuri.utils.player.MoveUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import static ddlc.yuri.utils.misc.IMinecraft.mc;

@Getter
@Setter
@AllArgsConstructor
public final class StrafeEvent extends CancellableEvent {

    private float forward;
    private float strafe;
    private float friction;
    private float yaw;

    public void setSpeed(final double speed, final double motionMultiplier) {
        setFriction((float) (getForward() != 0 && getStrafe() != 0 ? speed * 0.98F : speed));
        mc.thePlayer.motionX *= motionMultiplier;
        mc.thePlayer.motionZ *= motionMultiplier;
    }

    public void setSpeed(final double speed) {
        setFriction((float) (getForward() != 0 && getStrafe() != 0 ? speed * 0.98F : speed));
        MoveUtils.stop();
    }
}
