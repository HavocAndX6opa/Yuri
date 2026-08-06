package ddlc.yuri.modules.impl.misc;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.utils.client.TimerUtils;

@ModuleInfo(label = "Timer", description = "Changes the game speed", category = ModuleCategory.MISC)
public final class TimerModule extends Module {

    private enum Mode {
        CONSTANT("Constant"),
        PULSE("Pulse");

        public final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public final ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.CONSTANT);
    public final NumberProperty time1 = new NumberProperty("Time Amount", 1f, 0f, 5f, 0.1f);
    public final NumberProperty time2 = new NumberProperty("Time Amount 2", 1f, 0f, 5f, 0.1f, () -> mode.getValue() == Mode.PULSE);

    private final TimerUtils theTimer = new TimerUtils();

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        setSuffix(String.valueOf(mc.timer.timerSpeed));
        switch (mode.getValue()) {
            case CONSTANT: {
                mc.timer.timerSpeed = time1.getValue().floatValue();
                break;
            }

            case PULSE: {
                if (theTimer.getTime() < 100) {
                    mc.timer.timerSpeed = time2.getValue().floatValue();
                } else {
                    if (theTimer.getTime() > 200) {
                        theTimer.reset();
                    } else {
                        mc.timer.timerSpeed = time1.getValue().floatValue();
                    }
                }
                break;
            }
        }
    }

    @Override
    public void onDisable() {
        mc.timer.timerSpeed = 1.0f;
    }
}
