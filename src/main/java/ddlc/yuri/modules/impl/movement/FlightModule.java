package ddlc.yuri.modules.impl.movement;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.player.MotionEvent;
import ddlc.yuri.api.events.impl.player.StrafeEvent;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.modules.impl.movement.flight.FlightMode;
import ddlc.yuri.modules.impl.movement.flight.impl.MotionFlight;
import ddlc.yuri.modules.impl.movement.flight.impl.NCPFlight;

import java.util.EnumMap;
import java.util.Map;

@ModuleInfo(label = "Flight", category = ModuleCategory.MOVEMENT, description = "Allows you to fly")
public final class FlightModule extends Module {

    public final ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.MOTION);
    public final NumberProperty motionSpeed = new NumberProperty("Motion Speed",
            0.9, 0.1, 5.0, 0.1, () -> mode.getValue() == Mode.MOTION);

    private enum Mode {
        MOTION("Motion"),
        NCP("NCP");

        public final String name;

        Mode(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    private final Map<Mode, FlightMode> flightModes;

    {
        flightModes = new EnumMap<>(Mode.class);

        flightModes.put(Mode.MOTION, new MotionFlight(this));
        flightModes.put(Mode.NCP, new NCPFlight(this));
    }

    @EventHook
    public void onPreUpdate(MotionEvent event) {
        setSuffix(mode.getValue().toString());

        FlightMode currentMode = flightModes.get(mode.getValue());
        if (currentMode != null) {
            currentMode.onMotion(event);
        }
    }

    @EventHook
    public void onStrafe(StrafeEvent event) {
        setSuffix(mode.getValue().toString());

        FlightMode currentMode = flightModes.get(mode.getValue());
        if (currentMode != null) {
            currentMode.onStrafe(event);
        }
    }
}
