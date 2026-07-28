package ddlc.yuri.modules.impl.movement;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.player.ItemSlowdownEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import net.minecraft.item.*;

@ModuleInfo(
        label = "No Slow",
        description = "Stops slow-down on items",
        category = ModuleCategory.MOVEMENT
)
public final class NoSlowModule extends Module {

    private enum Mode {
        VANILLA("Vanilla");

        public final String name;

        Mode(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    private final ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.VANILLA);

    private final Property<Boolean> food = new Property<>("Food Items", true);
    private final Property<Boolean> potion = new Property<>("Potion Items", true);
    private final Property<Boolean> sword = new Property<>("Sword Items", true);
    private final Property<Boolean> bow = new Property<>("Bow Items", true);

    @EventHook
    public void onSlowdown(ItemSlowdownEvent event) {
        if (!this.isEnabled())
            return;
        if (mc.thePlayer == null)
            return;

        if (!mc.thePlayer.isUsingItem())
            return;

        Item item = mc.thePlayer.getHeldItem().getItem();

        if (food.getValue() && item instanceof ItemFood) {
            event.setCancelled(true);
        }

        if (potion.getValue() && item instanceof ItemPotion) {
            event.setCancelled(true);
        }

        if (sword.getValue() && item instanceof ItemSword) {
            event.setCancelled(true);
        }

        if (bow.getValue() && item instanceof ItemBow) {
            event.setCancelled(true);
        }
    }
}
