package ddlc.yuri.modules.impl.combat.velocity.impl;

import ddlc.yuri.api.events.impl.player.PlayerAttackEvent;
import ddlc.yuri.modules.impl.combat.VelocityModule;
import ddlc.yuri.modules.impl.combat.velocity.VelocityMode;
import lombok.AllArgsConstructor;
import net.minecraft.entity.EntityLivingBase;

@AllArgsConstructor
public class ReduceVelocity implements VelocityMode {
    private final VelocityModule parent;

    @Override
    public void onAttack(PlayerAttackEvent event) {
        if (parent.ignoreOnFire.getValue() && mc.thePlayer.isBurning()) {
            return;
        }

        // FUCK THIS
        if (event.target instanceof EntityLivingBase && mc.thePlayer.hurtTime > 0) {
            mc.thePlayer.motionX *= parent.reduceX.getValue() / 100.0;
            mc.thePlayer.motionZ *= parent.reduceZ.getValue() / 100.0;
        }
    }
}
