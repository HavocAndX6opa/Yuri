package ddlc.yuri.modules.impl.ghost;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.player.HitSlowDownEvent;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;

@ModuleInfo(label = "Keep Sprint", description = "Keeps you sprinting after attack", category = ModuleCategory.GHOST)
public final class KeepSprintModule extends Module {

    private final NumberProperty attackMotion = new NumberProperty("Attack Motion", 1, 0, 1, 0.05);
    private final NumberProperty chance = new NumberProperty("Chance", 100, 0, 100, 1);

    @EventHook
    public void onHitSlowDown(HitSlowDownEvent event) {
        setSuffix(attackMotion.getValue().toString());

        if (chance.getValue() != 100.0D && Math.random() >= chance.getValue() / 100.0D) {
            return;
        }

        event.setSlowDown(attackMotion.getValue());
        event.setSprint(true);
    }
}