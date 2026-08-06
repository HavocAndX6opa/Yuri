package ddlc.yuri.modules.impl.render;

import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;

@ModuleInfo(label = "Motion Blur", description = "Applies a motion blur effect to the screen", category = ModuleCategory.RENDER)
public final class MotionBlurModule extends Module {
    public NumberProperty blurAmount = new NumberProperty("Blur Amount", 7.0, 0.0, 10.0, 0.1);
}
