package ddlc.yuri.modules.impl.movement;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.client.ClientTickEvent;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import net.minecraft.block.BlockAir;
import net.minecraft.util.BlockPos;

@ModuleInfo(label = "Safe Walk", category = ModuleCategory.MOVEMENT, description = "Prevents you from falling off edges")
public final class SafeWalkModule extends Module {

    @EventHook
    public void onTick(ClientTickEvent event) {
        if(mc.thePlayer == null) return;

        if (mc.gameSettings.keyBindJump.isKeyDown()) {
            mc.thePlayer.safeWalk = false;
            return;
        }

        if(!mc.thePlayer.onGround) {
            mc.thePlayer.safeWalk = true;
            return;
        }

        BlockPos below = new BlockPos(
                mc.thePlayer.posX,
                mc.thePlayer.posY - 1.0,
                mc.thePlayer.posZ
        );

        mc.thePlayer.safeWalk =
                mc.theWorld.getBlockState(below).getBlock()
                        instanceof BlockAir
        ;
    }

    @Override
    public void onDisable() {
        mc.thePlayer.safeWalk = false;
    }
}

