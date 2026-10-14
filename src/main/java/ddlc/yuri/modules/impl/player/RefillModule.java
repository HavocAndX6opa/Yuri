package ddlc.yuri.modules.impl.player;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;

@ModuleInfo(label = "Refill", description = "Refills your splash pots when out", category = ModuleCategory.PLAYER)
public class RefillModule extends Module {

    private static final long CLICK_DELAY = 50;
    private static final int SHIFT_CLICK = 1;

    private final Property<Boolean> autoClose = new Property<>("Auto Close", true);

    private long lastClicked = 0;

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        if (!(mc.currentScreen instanceof GuiInventory)) {
            return;
        }

        boolean hotbarFull = true;
        for (int i = 0; i < 9; i++) {
            if (mc.thePlayer.inventory.getStackInSlot(i) == null) {
                hotbarFull = false;
                break;
            }
        }

        if (hotbarFull) {
            if (autoClose.getValue()) {
                mc.thePlayer.closeScreen();
            }
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastClicked <= CLICK_DELAY) {
            return;
        }

        for (int i = 9; i < 36; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);

            if (stack == null || !(stack.getItem() instanceof ItemPotion) || !ItemPotion.isSplash(stack.getMetadata())) {
                continue;
            }

            mc.playerController.windowClick(
                    mc.thePlayer.inventoryContainer.windowId,
                    i,
                    0,
                    SHIFT_CLICK,
                    mc.thePlayer
            );
            lastClicked = now;
            return;
        }
    }
}