package ddlc.yuri.modules.impl.ghost;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.player.MouseOverEvent;
import ddlc.yuri.api.events.impl.player.PlayerAttackEvent;
import ddlc.yuri.api.events.impl.player.RightClickEvent;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.managers.impl.RotationManager;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.utils.client.MathUtils;
import ddlc.yuri.utils.player.RayCastUtils;
import net.minecraft.util.MovingObjectPosition;

@ModuleInfo(label = "Reach", description = "Gives you more attack reach", category = ModuleCategory.GHOST)
public final class ReachModule extends Module {

    private final NumberProperty minRange = new NumberProperty("Min Range", 3.0, 3.0, 6.0, 0.01);
    private final NumberProperty maxRange = new NumberProperty("Max Range", 3.0, 3.0, 6.0, 0.01);

    @EventHook
    public void onMouseOver(MouseOverEvent event) {
        event.setRange(MathUtils.getRandom(minRange.getValue(), maxRange.getValue()));
    }

    @EventHook
    public void onRightClick(RightClickEvent event) {
        if (mc.thePlayer == null || RotationManager.rotations == null) return;

        MovingObjectPosition mop = RayCastUtils.rayCast(RotationManager.rotations, 4.5);
        if (mop != null) {
            mc.objectMouseOver = mop;
        }
    }

    @EventHook
    public void onAttack(PlayerAttackEvent event) {
        if (event.target == null) {
            event.setCancelled(true);
        }
    }
}