package ddlc.yuri.modules.impl.movement;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.modules.impl.movement.speed.SpeedMode;
import ddlc.yuri.modules.impl.movement.speed.impl.IntaveSpeed;
import ddlc.yuri.modules.impl.movement.speed.impl.LegitSpeed;
import ddlc.yuri.modules.impl.movement.speed.impl.MineplexSpeed;
import ddlc.yuri.modules.impl.movement.speed.impl.VanillaSpeed;

import java.util.EnumMap;
import java.util.Map;

@ModuleInfo(
        label = "Speed",
        category = ModuleCategory.MOVEMENT,
        description = "Makes you go FAST"
)
public class SpeedModule extends Module {
    public final ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.VANILLA);
    public final NumberProperty speed = new NumberProperty("Speed",
            0.9, 0.1, 5.0, 0.1,
            () -> mode.getValue() == Mode.VANILLA);

    private enum Mode {
        VANILLA("Vanilla"),
        LEGIT("Legit"),
        INTAVE("Intave"),
        MINEPLEX("Mineplex")
        ;

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
        speedModes.put(Mode.INTAVE, new IntaveSpeed());
        speedModes.put(Mode.MINEPLEX, new MineplexSpeed());
    }

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        setSuffix(mode.getValue().toString());

        SpeedMode currentMode = speedModes.get(mode.getValue());
        if (currentMode != null) {
            currentMode.onPreUpdate(event);
        }
    }

    @Override
    public void onDisable() {
        if (mode.getValue() == Mode.INTAVE ||
                mode.getValue() == Mode.MINEPLEX) mc.timer.timerSpeed = 1.0f;
    }
}
