package ddlc.yuri.managers.impl;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.player.KillEvent;
import ddlc.yuri.api.events.impl.player.PlayerAttackEvent;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import net.minecraft.entity.EntityLivingBase;

import static ddlc.yuri.utils.misc.IMinecraft.mc;

public class KillEventManager {

    private static EntityLivingBase target;

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        if (target != null) {
            boolean isRemoved = !mc.theWorld.loadedEntityList.contains(target);
            boolean isDead = target.isDead || target.getHealth() <= 0.0F;

            if (isRemoved || isDead) {
                Yuri.INSTANCE.getEventBus().post(new KillEvent(target));
                target = null;
            }
        }
    }

    @EventHook
    public void onPlayerAttack(PlayerAttackEvent event) {
        event.target = target;
    }
}
