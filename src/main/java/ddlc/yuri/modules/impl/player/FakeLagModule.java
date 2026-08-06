package ddlc.yuri.modules.impl.player;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.player.MotionEvent;
import ddlc.yuri.api.events.impl.render.Render3DEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.managers.impl.LagManager;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.utils.render.RenderUtils;

@ModuleInfo(label = "Fake Lag", description = "Causes you to lag by delaying packets sent to the server", category = ModuleCategory.PLAYER)
public class FakeLagModule extends Module {

    private final NumberProperty delay = new NumberProperty("Delay", 200, 50, 2000, 5);
    private final Property<Boolean> teleports = new Property<Boolean>("Delay Teleports", true);
    private final Property<Boolean> velocity = new Property<Boolean>("Delay Velocity", true);
    private final Property<Boolean> entities = new Property<Boolean>("Delay Entity Movements", true);
    private final Property<Boolean> renderLagPos = new Property<Boolean>("Render Lag Pos", true);

    @EventHook
    public void onMotion(MotionEvent event) {
        setSuffix(delay.getValue().intValue() + "ms");
        if (event.isPre()) return;
        LagManager.spoof(delay.getValue().intValue(), true, velocity.getValue(),
                teleports.getValue(), entities.getValue());
    }

    @EventHook
    public void onRender3D(Render3DEvent event) {
        if (!renderLagPos.getValue()) return;
        if (!LagManager.hasServerPosition) return;
        if (mc.gameSettings.thirdPersonView == 0) return;

        double x = LagManager.serverX - mc.getRenderManager().viewerPosX;
        double y = LagManager.serverY - mc.getRenderManager().viewerPosY;
        double z = LagManager.serverZ - mc.getRenderManager().viewerPosZ;

        RenderUtils.renderPlayerPosition(x, y, z);
    }
}