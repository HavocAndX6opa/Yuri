package ddlc.yuri.modules.impl.misc;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;

@ModuleInfo(
        label = "Timer",
        description = "Changes the game speed",
        category = ModuleCategory.MISC
)
public final class TimerModule extends Module {
    private final NumberProperty amount = new NumberProperty("Amount", 1f, 0f, 5f, 0.1f);

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        mc.timer.timerSpeed = amount.getValue().floatValue();
    }

    @Override
    public void onDisable() {
        mc.timer.resetTimerSpeed();
    }
}
