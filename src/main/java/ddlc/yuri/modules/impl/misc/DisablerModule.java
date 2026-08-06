package ddlc.yuri.modules.impl.misc;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.client.PacketReceivedEvent;
import ddlc.yuri.api.events.impl.client.PacketSendEvent;
import ddlc.yuri.api.events.impl.player.MotionEvent;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.api.events.impl.world.WorldJoinEvent;
import ddlc.yuri.api.properties.impl.MultiModeProperty;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.modules.impl.misc.disabler.DisablerMode;
import ddlc.yuri.modules.impl.misc.disabler.impl.HypixelInvDisabler;

import java.util.EnumMap;
import java.util.Map;

@ModuleInfo(
        label = "Disabler",
        description = "Disables parts of or entire anticheats",
        category = ModuleCategory.MISC)
public final class DisablerModule extends Module {

    public enum Mode {
        HYPIXEL_INV("Hypixel Inv Move");

        public final String name;

        Mode(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    public final MultiModeProperty<Mode> mode = new MultiModeProperty<>("Mode", Mode.HYPIXEL_INV);
    public boolean usingItem;

    private final Map<Mode, DisablerMode> disablerModes;

    {
        disablerModes = new EnumMap<>(Mode.class);
        disablerModes.put(Mode.HYPIXEL_INV, new HypixelInvDisabler(this));
    }

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        setSuffix(mode.getValue().toString());

        DisablerMode currentMode = disablerModes.get(mode.getValue());
        if (currentMode != null) {
            currentMode.onPreUpdate(event);
        }
    }

    @EventHook
    public void onPreMotion(MotionEvent event) {
        if (!event.isPre()) return;

        DisablerMode currentMode = disablerModes.get(mode.getValue());
        if (currentMode != null) {
            currentMode.onMotion(event);
        }
    }

    @EventHook
    public void onPacketReceived(PacketReceivedEvent event) {
        DisablerMode currentMode = disablerModes.get(mode.getValue());
        if (currentMode != null) {
            currentMode.onPacketReceived(event);
        }
    }

    @EventHook
    public void onPacketSend(PacketSendEvent event) {
        DisablerMode currentMode = disablerModes.get(mode.getValue());
        if (currentMode != null) {
            currentMode.onPacketSend(event);
        }
    }

    @EventHook
    public void onWorldJoin(WorldJoinEvent event) {
        DisablerMode currentMode = disablerModes.get(mode.getValue());
        if (currentMode != null) {
            currentMode.onWorldJoin(event);
        }
    }
}
