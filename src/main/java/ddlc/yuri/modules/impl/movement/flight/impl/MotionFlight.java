package ddlc.yuri.modules.impl.movement.flight.impl;

import ddlc.yuri.api.events.impl.player.StrafeEvent;
import ddlc.yuri.modules.impl.movement.FlightModule;
import ddlc.yuri.modules.impl.movement.flight.FlightMode;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class MotionFlight implements FlightMode {
    private final FlightModule parent;

    @Override
    public void onStrafe(StrafeEvent event) {
        event.setSpeed(parent.motionSpeed.getValue());
        mc.thePlayer.motionY = mc.gameSettings.keyBindJump.isKeyDown() ? 0.5
                : mc.gameSettings.keyBindSneak.isKeyDown() ? -0.5 : 0;
    }
}
