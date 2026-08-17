package ddlc.yuri.modules.impl.movement;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.client.PacketSendEvent;
import ddlc.yuri.api.events.impl.player.*;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.modules.impl.movement.noslow.NoSlowMode;
import ddlc.yuri.modules.impl.movement.noslow.impl.GrimNoSlow;
import ddlc.yuri.modules.impl.movement.noslow.impl.NCPNoSlow;
import ddlc.yuri.utils.player.InvUtils;
import net.minecraft.item.*;

import java.util.EnumMap;
import java.util.Map;

@ModuleInfo(label = "No Slow", category = ModuleCategory.MOVEMENT, description = "Prevents you from slowing down while using items")
public final class NoSlowModule extends Module {

    public enum Mode {
        VANILLA("Vanilla"),
        NCP("NCP"),
        GRIM("Grim");

        public final String name;

        Mode(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    public final ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.VANILLA);
    public final Property<Boolean> food = new Property<Boolean>("Food Items", true, () -> mode.getValue() != Mode.GRIM);
    public final Property<Boolean> potion = new Property<Boolean>("Potion Items", true, () -> mode.getValue() != Mode.GRIM);
    public final Property<Boolean> sword = new Property<Boolean>("Sword Items", true);
    public final Property<Boolean> bow = new Property<Boolean>("Bow Items", true, () -> mode.getValue() != Mode.GRIM);

    public boolean usingItem;

    private final Map<Mode, NoSlowMode> noSlowModes;

    {
        noSlowModes = new EnumMap<>(Mode.class);
        noSlowModes.put(Mode.NCP, new NCPNoSlow());
        noSlowModes.put(Mode.GRIM, new GrimNoSlow(this));
    }

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        if (mc.thePlayer == null) return;

        setSuffix(mode.getValue().toString());

        NoSlowMode currentMode = noSlowModes.get(mode.getValue());
        if (currentMode != null) {
            currentMode.onPreUpdate(event);
        }
    }

    @EventHook
    public void onPacketSend(PacketSendEvent event) {
        if (mc.thePlayer == null) return;

        NoSlowMode currentMode = noSlowModes.get(mode.getValue());
        if (currentMode != null) {
            currentMode.onPacketSend(event);
        }
    }

    @EventHook
    public void onRightClick(RightClickEvent event) {
        if (mc.thePlayer == null) return;

        NoSlowMode currentMode = noSlowModes.get(mode.getValue());
        if (currentMode != null) {
            currentMode.onRightClick(event);
        }
    }

    @EventHook
    public void onStrafe(StrafeEvent event) {
        if (mc.thePlayer == null) return;

        NoSlowMode currentMode = noSlowModes.get(mode.getValue());
        if (currentMode != null) {
            currentMode.onStrafe(event);
        }
    }

    @EventHook
    public void onPreMotion(MotionEvent event) {
        if (mc.thePlayer == null) return;
        if (!event.isPre()) return;

        NoSlowMode currentMode = noSlowModes.get(mode.getValue());
        if (currentMode != null) {
            currentMode.onMotion(event);
        }
    }

    @EventHook
    public void onSlowdown(ItemSlowdownEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null || !mc.thePlayer.isUsingItem()) return;

        NoSlowMode currentMode = noSlowModes.get(mode.getValue());
        if (currentMode instanceof GrimNoSlow) {
            (currentMode).onSlowdown(event);
            return;
        }

        Item item = mc.thePlayer.getHeldItem().getItem();

        if (food.getValue() && item instanceof ItemFood) event.setCancelled(true);
        if (potion.getValue() && item instanceof ItemPotion) event.setCancelled(true);
        if (sword.getValue() && item instanceof ItemSword) event.setCancelled(true);
        if (bow.getValue() && item instanceof ItemBow) event.setCancelled(true);
    }

    public boolean isSwordActive() {
        return this.sword.getValue() && InvUtils.isHoldingSword();
    }

    @Override
    public void onDisable() {
        if (mc.thePlayer == null) return;

        NoSlowMode currentMode = noSlowModes.get(mode.getValue());
        if (currentMode instanceof GrimNoSlow) {
            GrimNoSlow.release();
            GrimNoSlow.resetCycle();
        }
    }
}
