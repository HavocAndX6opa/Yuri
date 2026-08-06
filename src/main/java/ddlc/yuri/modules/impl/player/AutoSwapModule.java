package ddlc.yuri.modules.impl.player;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.player.MotionEvent;
import ddlc.yuri.api.events.impl.player.PlayerAttackEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.managers.impl.SlotManager;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.BlockPos;

@ModuleInfo(label = "Auto Swap", description = "Automatically swaps to the best tool for the block you're mining.", category = ModuleCategory.PLAYER)
public final class AutoSwapModule extends Module {

    public enum SwapMode {
        CLIENT("Client"), SERVER("Server");
        public final String name;

        SwapMode(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    private final Property<Boolean> sneakOnly = new Property<>("Sneak Only", false);
    private final ModeProperty<SwapMode> swapMode = new ModeProperty<>("Swap Mode", SwapMode.CLIENT);

    public static boolean shouldSwap = true;

    @EventHook
    public void onMotion(MotionEvent event) {
        if (!event.isPre()) return;

        if (sneakOnly.getValue() && !mc.thePlayer.isSneaking()) {
            SlotManager.swapBack();
            return;
        }

        if (!mc.gameSettings.keyBindAttack.isKeyDown() || mc.objectMouseOver == null) {
            SlotManager.swapBack();
            return;
        }

        BlockPos pos = mc.objectMouseOver.getBlockPos();
        if (pos == null) {
            SlotManager.swapBack();
            return;
        }

        int itemToUse = getBestToolSlot(pos);
        if (itemToUse == -1) {
            SlotManager.swapBack();
            return;
        }

        if (mc.thePlayer.inventory.currentItem == itemToUse) {
            return;
        }

        if (!shouldSwap) {
            return;
        }

        SlotManager.swap(itemToUse, swapMode.getValue() == SwapMode.SERVER);
    }

    @EventHook
    public void onAttack(PlayerAttackEvent event) {
        float bestStr = 0.0F;
        int itemToUse = -1;

        for (int i = 0; i < 9; i++) {
            ItemStack itemStack = mc.thePlayer.inventory.mainInventory[i];
            if (itemStack == null) continue;

            if (!(itemStack.getItem() instanceof ItemSword)) continue;

            ItemSword item = (ItemSword) itemStack.getItem();

            if (item.attackDamage > bestStr) {
                bestStr = item.attackDamage;
                itemToUse = i;
            }
        }
        if (itemToUse != -1) {
            SlotManager.swap(itemToUse, swapMode.getValue() == SwapMode.SERVER);
        }
    }

    @Override
    public void onDisable() {
        SlotManager.swapBack();
        super.onDisable();
    }

    private int getBestToolSlot(BlockPos pos) {
        Block block = mc.theWorld.getBlockState(pos).getBlock();

        float bestStr = 1.0F;
        int itemTouse = -1;

        for (int i = 0; i < 9; i++) {
            ItemStack itemStack = mc.thePlayer.inventory.mainInventory[i];
            if (itemStack == null) continue;

            if (itemStack.getStrVsBlock(block) > bestStr) {
                bestStr = itemStack.getStrVsBlock(block);
                itemTouse = i;
            }
        }

        return itemTouse;
    }
}
