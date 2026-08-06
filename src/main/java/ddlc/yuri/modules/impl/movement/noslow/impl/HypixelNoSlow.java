package ddlc.yuri.modules.impl.movement.noslow.impl;

import ddlc.yuri.modules.impl.movement.NoSlowModule;
import ddlc.yuri.modules.impl.movement.noslow.NoSlowMode;

public final class HypixelNoSlow implements NoSlowMode {

    private final NoSlowModule parentModule;

    public HypixelNoSlow(NoSlowModule parentModule) {
        this.parentModule = parentModule;
    }

    /*private int usedTicks = 0;
    private boolean wasUsingItem;

    @Override
    public void onMotion(MotionEvent event) {
        if (!event.isPre()) return;

        ItemStack held = mc.thePlayer.getHeldItem();
        if (held == null) return;
        Item item = held.getItem();

        if (!(item instanceof ItemSword || item instanceof ItemFood && mc.thePlayer.isEating() || item instanceof ItemPotion && !ItemPotion.isSplash(held.getMetadata()) && parentModule.potion.getValue() && mc.thePlayer.isEating())) return;

        if (mc.thePlayer.isUsingItem()) {
            usedTicks++;
            wasUsingItem = true;
        } else if (wasUsingItem) {
            wasUsingItem = false;
            usedTicks = 0;
        }

        if (usedTicks > parentModule.finishEating.getValue().intValue() && item instanceof ItemFood) {
            mc.gameSettings.keyBindUseItem.setPressed(false);
            usedTicks = 0;
        }
    }

    @Override
    public void onPreUpdate(PreUpdateEvent event) {
        if (Euphoria.client().getModuleManager().getModule(KillAuraModule.class).isEnabled() && KillAuraModule.target != null) return;
        if (mc.thePlayer.onGround) return;
        if (mc.gameSettings.keyBindForward.isKeyDown() || mc.gameSettings.keyBindBack.isKeyDown()) return;
        if (mc.thePlayer.ticksExisted <= 5) return;
        if (!mc.thePlayer.isUsingItem()) return;

        ItemStack held = mc.thePlayer.getHeldItem();
        if (held != null && held.getItem() instanceof ItemSword) return;

        RotationListener.setRotations(mc.thePlayer.rotationYaw + 45.0F, mc.thePlayer.rotationPitch, 10f, RotationListener.MovementFix.NORMAL);
    }

    @Override
    public void onSlowdown(ItemSlowdownEvent event) {
        ItemStack held = mc.thePlayer.getHeldItem();
        if (held == null) return;
        Item item = held.getItem();

        if (!(item instanceof ItemSword || item instanceof ItemFood || item instanceof ItemPotion)) return;

        if (!mc.thePlayer.isUsingItem()) return;
        if (usedTicks > parentModule.delay.getValue().intValue()) {
            event.setCancelled(true);
            mc.thePlayer.setSprinting(true);
        }
    }*/
}