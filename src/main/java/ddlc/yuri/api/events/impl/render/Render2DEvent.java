package ddlc.yuri.api.events.impl.render;

import ddlc.yuri.api.events.Event;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public final class Render2DEvent implements Event {
    public final float partialTicks;
}
