package ddlc.yuri.modules.impl.ghost;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.player.MouseOverEvent;
import ddlc.yuri.api.events.impl.player.RightClickEvent;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.managers.impl.RotationManager;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.utils.player.RayCastUtils;
import net.minecraft.util.MovingObjectPosition;

@ModuleInfo(label = "Hitbox Expand", description = "Expands player hitboxes", category = ModuleCategory.GHOST)
public final class HitboxExpandModule extends Module {

    private final NumberProperty amount = new NumberProperty("Amount", 0.0, 0.0, 6.0, 0.1);

    @EventHook
    public void onMouseOver(MouseOverEvent event) {
        event.setExpand(amount.getValue().floatValue());
    }

    @EventHook
    public void onRightClick(RightClickEvent event) {
        if (mc.thePlayer == null || RotationManager.rotations == null) return;

        MovingObjectPosition mop = RayCastUtils.rayCast(RotationManager.rotations, 4.5);
        if (mop != null) {
            mc.objectMouseOver = mop;
        }
    }
}