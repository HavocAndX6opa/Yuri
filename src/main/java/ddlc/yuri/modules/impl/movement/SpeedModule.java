package ddlc.yuri.modules.impl.movement;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.client.PacketReceivedEvent;
import ddlc.yuri.api.events.impl.player.MotionEvent;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.api.events.impl.player.StrafeEvent;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.modules.impl.movement.speed.SpeedMode;
import ddlc.yuri.modules.impl.movement.speed.impl.*;

import java.util.EnumMap;
import java.util.Map;

@ModuleInfo(label = "Speed", description = "Makes you go FAST", category = ModuleCategory.MOVEMENT)
public class SpeedModule extends Module {

    public final ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.VANILLA);
    public final NumberProperty speed = new NumberProperty("Speed",
            0.9, 0.1, 5.0, 0.1,
            () -> mode.getValue() == Mode.VANILLA);

    private enum Mode {
        VANILLA("Vanilla"),
        LEGIT("Legit"),
        POLAR("Polar"),
        INTAVE("Intave"),
        NCP("NCP"),
        LIBRECRAFT("Librecraft");

        public final String name;

        Mode(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    private final Map<Mode, SpeedMode> speedModes;

    {
        speedModes = new EnumMap<>(Mode.class);

        speedModes.put(Mode.VANILLA, new VanillaSpeed(this));
        speedModes.put(Mode.LEGIT, new LegitSpeed());
        speedModes.put(Mode.POLAR, new PolarSpeed());
        speedModes.put(Mode.INTAVE, new IntaveSpeed());
        speedModes.put(Mode.NCP, new NCPSpeed());
        speedModes.put(Mode.LIBRECRAFT, new LibrecraftSpeed());

    }

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        setSuffix(mode.getValue().toString());

        SpeedMode currentMode = speedModes.get(mode.getValue());
        if (currentMode != null) {
            currentMode.onPreUpdate(event);
        }
    }

    @EventHook
    public void onMotion(MotionEvent event) {

        SpeedMode currentMode = speedModes.get(mode.getValue());
        if (currentMode != null) {
            currentMode.onMotion(event);
        }
    }

    @EventHook
    public void onStrafe(StrafeEvent event) {
        SpeedMode currentMode = speedModes.get(mode.getValue());
        if (currentMode != null) {
            currentMode.onStrafe(event);
        }
    }

    @EventHook
    public void onPacketReceived(PacketReceivedEvent event) {

        SpeedMode currentMode = speedModes.get(mode.getValue());
        if (currentMode != null) {
            currentMode.onPacketReceived(event);
        }
    }

    @Override
    public void onDisable() {
        SpeedMode currentMode = speedModes.get(mode.getValue());
        if (currentMode != null) {
            currentMode.onDisable();
        }
    }

    @Override
    public void onEnable() {
        SpeedMode currentMode = speedModes.get(mode.getValue());
        if (currentMode != null) {
            currentMode.onEnable();
        }
    }
}
