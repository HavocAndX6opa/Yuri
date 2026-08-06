package ddlc.yuri.modules.impl.movement;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.player.BlockCollideEvent;
import ddlc.yuri.api.events.impl.player.MotionEvent;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.utils.player.PlayerUtils;
import net.minecraft.block.BlockLiquid;
import net.minecraft.util.AxisAlignedBB;

@ModuleInfo(label = "Water Walk", category = ModuleCategory.MOVEMENT, description = "Allows you to walk on water")
public class WaterWalkModule extends Module {

    public final ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.VANILLA);

    public enum Mode {
        VANILLA("Vanilla"),
        NCP("NCP");

        public final String name;

        Mode(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    @EventHook
    public void onMotion(MotionEvent event) {
        if (!event.isPre()) return;

        setSuffix(mode.getValue().toString());

        if (mode.getValue() == Mode.NCP) {
            if (mc.thePlayer.ticksExisted % 2 == 0 && PlayerUtils.onLiquid()) {
                event.setPosY(event.getPosY() - 0.015625);
            }
        }
    }

    @EventHook
    public void onBlockCollide(BlockCollideEvent event) {
        if (event.getBlock() instanceof BlockLiquid && !mc.gameSettings.keyBindSneak.isKeyDown()) {
            final int x = event.getBlockPos().getX();
            final int y = event.getBlockPos().getY();
            final int z = event.getBlockPos().getZ();

            event.setBoundingBox(AxisAlignedBB.fromBounds(x, y, z, x + 1, y + 1, z + 1));
        }
    }
}
