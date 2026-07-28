package ddlc.yuri.modules.impl.movement;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.player.MoveEvent;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;

@ModuleInfo(
        label = "Step",
        description = "Allows you to step up blocks",
        category = ModuleCategory.MOVEMENT
)
public final class StepModule extends Module {

    private final NumberProperty stepHeight = new NumberProperty("Step Height", 1f, 1f, 10f, 0.5f);

    @EventHook
    public void onMove(MoveEvent event) {
        mc.thePlayer.stepHeight = stepHeight.getValue().floatValue();
    }

    @Override
    public void onDisable() {
        mc.thePlayer.stepHeight = 0.5F;
    }
}
