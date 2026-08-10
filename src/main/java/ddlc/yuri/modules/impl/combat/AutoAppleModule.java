package ddlc.yuri.modules.impl.combat;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.player.PlayerAttackEvent;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.api.events.impl.world.WorldJoinEvent;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.modules.impl.player.ScaffoldModule;
import ddlc.yuri.utils.client.TimerUtils;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAppleGold;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;

@ModuleInfo(label = "Auto Apple", category = ModuleCategory.COMBAT, description = "Automatically eats a golden apple when your health is low")
public final class AutoAppleModule extends Module {

    private final NumberProperty health = new NumberProperty("Health", 15, 1, 20, 1);
    private final NumberProperty delay = new NumberProperty("Delay", 50, 0, 100, 5);

    private final TimerUtils stopWatch = new TimerUtils();
    private int attackTicks;
    private long nextEat;
    private boolean eating;

    @EventHook
    public void onUpdate(PreUpdateEvent event) {
        this.attackTicks++;

        if (mc.currentScreen != null) {
            this.attackTicks = 0;
        }

        if (mc.thePlayer.isPotionActive(Potion.regeneration) && eating) {
            mc.gameSettings.keyBindUseItem.setPressed(false);
            eating = false;
            if (Yuri.INSTANCE.getModuleManager().getModule(AuraModule.class).isEnabled() && AuraModule.target != null && !AuraModule.canAttack) {
                AuraModule.canAttack = true;
            }
        }

        if (mc.thePlayer.onGroundTicks <= 1 || !stopWatch.hasTimeElapsed(nextEat) || attackTicks < 10 || Yuri.INSTANCE.getModuleManager().getModule(ScaffoldModule.class).isEnabled() || mc.thePlayer.isPotionActive(Potion.regeneration)) {
            return;
        }

        for (int i = 0; i < 9; i++) {
            final ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);

            if (stack == null) {
                continue;
            }

            final Item item = stack.getItem();

            if (item instanceof ItemAppleGold && mc.thePlayer.getHealth() <= this.health.getValue().floatValue()) {
                mc.thePlayer.inventory.currentItem = i;

                mc.playerController.syncCurrentPlayItem();
                mc.gameSettings.keyBindUseItem.setPressed(true);
                eating = true;
                if (Yuri.INSTANCE.getModuleManager().getModule(AuraModule.class).isEnabled() && AuraModule.target != null)
                    AuraModule.canAttack = false;
                this.nextEat = delay.getValue().longValue() * 10;
                stopWatch.reset();
                break;
            }
        }
    }


    @EventHook
    public void onWorldJoin(WorldJoinEvent event) {
        if (eating) {
            mc.gameSettings.keyBindUseItem.setPressed(false);
            eating = false;
            if (Yuri.INSTANCE.getModuleManager().getModule(AuraModule.class).isEnabled() && AuraModule.target != null && !AuraModule.canAttack)
                AuraModule.canAttack = true;
        }
    }


    @EventHook
    public void onAttack(PlayerAttackEvent event) {
        this.attackTicks = 0;
    }

    @Override
    public void onDisable() {
        if (eating) {
            mc.gameSettings.keyBindUseItem.setPressed(false);
            eating = false;
            if (Yuri.INSTANCE.getModuleManager().getModule(AuraModule.class).isEnabled() && AuraModule.target != null && !AuraModule.canAttack)
                AuraModule.canAttack = true;
        }
        super.onDisable();
    }
}
