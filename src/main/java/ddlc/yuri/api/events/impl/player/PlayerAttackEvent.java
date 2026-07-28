package ddlc.yuri.api.events.impl.player;

import ddlc.yuri.api.events.CancellableEvent;
import lombok.AllArgsConstructor;
import net.minecraft.entity.EntityLivingBase;

@AllArgsConstructor
public class PlayerAttackEvent extends CancellableEvent {
    public EntityLivingBase target;
}
