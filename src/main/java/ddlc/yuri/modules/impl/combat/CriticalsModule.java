package ddlc.yuri.modules.impl.combat;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.player.MotionEvent;
import ddlc.yuri.api.events.impl.player.PlayerAttackEvent;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.api.events.impl.player.StrafeEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.modules.impl.combat.criticals.CriticalsMode;
import ddlc.yuri.modules.impl.combat.criticals.impl.MospixelCriticals;
import ddlc.yuri.modules.impl.combat.criticals.impl.NCPCriticals;
import ddlc.yuri.modules.impl.combat.criticals.impl.PacketCriticals;

import java.util.EnumMap;
import java.util.Map;

@ModuleInfo(label = "Criticals", category = ModuleCategory.COMBAT, description = "Makes your attacks critical hits")
public final class CriticalsModule extends Module {

    private enum Mode {
        PACKET("Packet"),
        MOSPIXEL("Mospixel"),
        NCP("NCP");

        public final String name;

        Mode(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    private final Map<Mode, CriticalsMode> criticalsMode;

    {
        criticalsMode = new EnumMap<>(Mode.class);

        criticalsMode.put(Mode.PACKET, new PacketCriticals());
        criticalsMode.put(Mode.MOSPIXEL, new MospixelCriticals());
        criticalsMode.put(Mode.NCP, new NCPCriticals());
    }

    private final ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.PACKET);
    private final Property<Boolean> onGroundCheck = new Property<>("On Ground Check", true);

    @EventHook
    public void onAttack(PlayerAttackEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        if (onGroundCheck.getValue() && !mc.thePlayer.onGround) {
            return;
        }

        CriticalsMode currentMode = criticalsMode.get(mode.getValue());
        if (currentMode != null) {
            currentMode.onAttack(event);
        }
    }

    @EventHook
    public void onMotion(MotionEvent event) {
        CriticalsMode currentMode = criticalsMode.get(mode.getValue());
        if (currentMode != null) {
            currentMode.onMotion(event);
        }
    }

    @Override
    public void onDisable() {
        CriticalsMode currentMode = criticalsMode.get(mode.getValue());
        if (currentMode != null) {
            currentMode.onDisable();
        }
    }

    @Override
    public void onEnable() {
        CriticalsMode currentMode = criticalsMode.get(mode.getValue());
        if (currentMode != null) {
            currentMode.onEnable();
        }
    }

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        setSuffix(mode.getValue().toString());
    }

    @EventHook
    public void onStrafe(StrafeEvent event) {
        CriticalsMode currentMode = criticalsMode.get(mode.getValue());
        if (currentMode != null) {
            currentMode.onStrafe(event);
        }
    }
}
