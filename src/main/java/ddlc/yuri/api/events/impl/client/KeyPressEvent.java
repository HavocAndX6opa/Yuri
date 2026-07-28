package ddlc.yuri.api.events.impl.client;

import ddlc.yuri.api.events.Event;

public final class KeyPressEvent implements Event {

    private final int key;

    public KeyPressEvent(int key) {
        this.key = key;
    }

    public int getKey() {
        return key;
    }

}
