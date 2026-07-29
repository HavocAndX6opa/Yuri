package ddlc.yuri.modules.impl.player;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.client.ClientTickEvent;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.modules.impl.player.nofall.NoFallMode;
import ddlc.yuri.modules.impl.player.nofall.impl.MLGNoFall;
import ddlc.yuri.modules.impl.player.nofall.impl.VanillaNoFall;

import java.util.EnumMap;
import java.util.Map;

@ModuleInfo(
        label = "No Fall",
        description = "Makes fall damage impossible!",
        category = ModuleCategory.PLAYER
)
public final class NoFallModule extends Module {
    public final ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.VANILLA);

    private enum Mode {
        VANILLA("Vanilla"),
        MLG("MLG"),;

        public final String name;

        Mode(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    private final Map<Mode, NoFallMode> nofallModes;

    {
        nofallModes = new EnumMap<>(Mode.class);

        nofallModes.put(Mode.VANILLA, new VanillaNoFall());
        nofallModes.put(Mode.MLG, new MLGNoFall());
    }

    @EventHook
    public void onTick(ClientTickEvent event) {
        NoFallMode currentMode = nofallModes.get(mode.getValue());
        if (currentMode != null) {
            currentMode.onTick(event);
        }
    }

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        setSuffix(mode.getValue().toString());

        NoFallMode currentMode = nofallModes.get(mode.getValue());
        if (currentMode != null) {
            currentMode.onPreUpdate(event);
        }
    }

    @Override
    public void onDisable() {
        NoFallMode currentMode = nofallModes.get(mode.getValue());
        if (currentMode != null) {
            currentMode.onDisable();
        }
    }
}
