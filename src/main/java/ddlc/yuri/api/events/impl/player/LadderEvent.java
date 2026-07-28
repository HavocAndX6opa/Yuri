package ddlc.yuri.api.events.impl.player;

import ddlc.yuri.api.events.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public final class LadderEvent implements Event {
    private double motionY;
}
