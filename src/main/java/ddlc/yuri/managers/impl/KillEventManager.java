package ddlc.yuri.managers.impl;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.player.KillEvent;
import ddlc.yuri.api.events.impl.player.PlayerAttackEvent;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.api.events.impl.world.WorldJoinEvent;
import net.minecraft.entity.EntityLivingBase;

import static ddlc.yuri.utils.misc.IMinecraft.mc;

public class KillEventManager {

    private EntityLivingBase target;

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        if (target != null && !mc.theWorld.loadedEntityList.contains(target)) {
            Yuri.INSTANCE.getEventBus().post(new KillEvent(target));
            target = null;
        }
    }

    @EventHook
    public void onPlayerAttack(PlayerAttackEvent event) {
        target = event.target;
    }

    @EventHook
    public void onWorldJoin(WorldJoinEvent event) {
        target = null;
    }
}
