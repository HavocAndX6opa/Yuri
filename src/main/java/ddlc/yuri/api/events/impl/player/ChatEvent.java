package ddlc.yuri.api.events.impl.player;

import ddlc.yuri.api.events.CancellableEvent;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public final class ChatEvent extends CancellableEvent {
    public String message;
}
