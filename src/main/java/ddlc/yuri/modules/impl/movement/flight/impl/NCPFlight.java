package ddlc.yuri.modules.impl.movement.flight.impl;

import ddlc.yuri.api.events.impl.player.MotionEvent;
import ddlc.yuri.api.events.impl.player.StrafeEvent;
import ddlc.yuri.modules.impl.movement.FlightModule;
import ddlc.yuri.modules.impl.movement.flight.FlightMode;
import ddlc.yuri.utils.player.MoveUtils;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.RandomUtils;

@AllArgsConstructor
public class NCPFlight implements FlightMode {
    private final FlightModule parent;

    @Override
    public void onStrafe(StrafeEvent event) {
        event.setSpeed(MoveUtils.getBaseMoveSpeed(), Math.random() / 2000);
    }

    @Override
    public void onMotion(MotionEvent event) {
        if (event.isPre()) {
            event.setPosY(event.getPosY() + 1E-5 + (mc.thePlayer.ticksExisted % 2 == 0 ? RandomUtils.nextDouble(1E-10, 1E-5) : -RandomUtils.nextDouble(1E-10, 1E-5)));
            mc.thePlayer.motionY = 0;
        }
    }
}