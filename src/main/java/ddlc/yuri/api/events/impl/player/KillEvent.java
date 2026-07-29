package ddlc.yuri.api.events.impl.player;

import ddlc.yuri.api.events.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.entity.Entity;

@Getter
@AllArgsConstructor
public final class KillEvent implements Event {
    Entity entity;
}