package ddlc.yuri.modules.impl.combat;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.client.ClientTickEvent;
import ddlc.yuri.api.events.impl.client.PacketReceivedEvent;
import ddlc.yuri.api.events.impl.player.PlayerAttackEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.modules.impl.combat.velocity.VelocityMode;
import ddlc.yuri.modules.impl.combat.velocity.impl.CancelVelocity;
import ddlc.yuri.modules.impl.combat.velocity.impl.LegitVelocity;
import ddlc.yuri.modules.impl.combat.velocity.impl.ReduceVelocity;

import java.util.EnumMap;
import java.util.Map;

@ModuleInfo(
        label = "Velocity",
        description = "Stops knockback or reduces it",
        category = ModuleCategory.COMBAT
)
public final class VelocityModule extends Module {
    public final ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.CANCEL);
    public final Property<Boolean> polar = new Property<>("Polar", false);
    public final Property<Boolean> ignoreOnFire = new Property<>("Ignore On Fire", true);

    public final NumberProperty reduceX = new NumberProperty("Reduce X", 100, 0, 100, 1, () -> mode.getValue() == Mode.REDUCE);
    public final NumberProperty reduceZ = new NumberProperty("Reduce Z", 100, 0, 100, 1, () -> mode.getValue() == Mode.REDUCE);

    private enum Mode {
        CANCEL("Cancel"),
        LEGIT("Legit"),
        REDUCE("Reduce")
        ;

        public final String name;

        Mode(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    private final Map<Mode, VelocityMode> velocityMode;

    {
        velocityMode = new EnumMap<>(VelocityModule.Mode.class);

        velocityMode.put(Mode.CANCEL, new CancelVelocity(this));
        velocityMode.put(Mode.LEGIT, new LegitVelocity(this));
        velocityMode.put(Mode.REDUCE, new ReduceVelocity(this));

    }

    @EventHook
    public void onTick(ClientTickEvent event) {
        setSuffix(mode.getValue().toString());

        VelocityMode currentMode = velocityMode.get(mode.getValue());
        if (currentMode != null) {
            currentMode.onTick(event);
        }
    }

    @EventHook
    public void onPacket(PacketReceivedEvent event) {

        VelocityMode currentMode = velocityMode.get(mode.getValue());
        if (currentMode != null) {
            currentMode.onPacket(event);
        }
    }

    @EventHook
    public void onAttack(PlayerAttackEvent event) {

        VelocityMode currentMode = velocityMode.get(mode.getValue());
        if (currentMode != null) {
            currentMode.onAttack(event);
        }
    }
}
