package ddlc.yuri.modules.impl.misc;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.player.MotionEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemEgg;
import net.minecraft.item.ItemSnowball;

@ModuleInfo(label = "Item Delays", description = "Uses items faster", category = ModuleCategory.MISC)
public final class ItemDelaysModule extends Module {
    
    private final NumberProperty delay = new NumberProperty("Delay", 1, 0, 3, 1);
    private final Property<Boolean> allowBlocks = new Property<Boolean>("Allow Blocks", true);
    private final Property<Boolean> allowProjectiles = new Property<Boolean>("Allow Projectiles", true);

    @EventHook
    public void onMotion(MotionEvent event) {
        if (mc.thePlayer == null || mc.thePlayer.inventory == null
                || mc.thePlayer.inventory.getCurrentItem() ==
                null || !event.isPre()) {
            return;
        }

        if (allowBlocks.getValue() &&
                mc.thePlayer.inventory.getCurrentItem().getItem() instanceof ItemBlock) {
            mc.rightClickDelayTimer =
                    Math.min(mc.rightClickDelayTimer, this.delay.getValue().intValue());
        }

        if (allowProjectiles.getValue()
            && mc.thePlayer.inventory.getCurrentItem().getItem() instanceof ItemSnowball ||
                mc.thePlayer.inventory.getCurrentItem().getItem() instanceof ItemEgg) {
            mc.rightClickDelayTimer =
                    Math.min(mc.rightClickDelayTimer, this.delay.getValue().intValue());
        }
    }
}
