package ddlc.yuri.modules.impl.ghost;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.annotations.EventPriority;
import ddlc.yuri.api.events.impl.player.MotionEvent;
import ddlc.yuri.api.events.impl.player.PlayerAttackEvent;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.managers.impl.BadPacketsManager;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.utils.client.TimerUtils;
import ddlc.yuri.utils.player.InventoryUtils;
import net.minecraft.entity.EntityLivingBase;
import org.lwjgl.input.Keyboard;

import java.util.concurrent.ThreadLocalRandom;

@ModuleInfo(label = "Block Hit", description = "Blocks your sword while hitting", category = ModuleCategory.GHOST)
public final class BlockHitModule extends Module {

    private final NumberProperty range = new NumberProperty("Range", 3, 1, 6, 0.05);
    private final NumberProperty chance = new NumberProperty("Block Chance", 100, 0, 100, 1);
    private final NumberProperty delay = new NumberProperty("Block Delay", 30, 1, 300, 1);
    private final NumberProperty combo = new NumberProperty("Combo Delay", 30, 1, 300, 1);

    private final TimerUtils actionTimer = new TimerUtils();
    private final TimerUtils waitTimer = new TimerUtils();

    private boolean block;
    private boolean tryStartCombo;

    @EventHook
    public void onMotion(MotionEvent event) {
        setSuffix(String.format("%s%%", chance.getValue().intValue()));

        if (!event.isPre() || !mc.inGameHasFocus) return;

        if (tryStartCombo
                && waitTimer.hasTimeElapsed(ThreadLocalRandom.current().nextDouble(combo.getValue(), combo.getValue() + 0.01))
                && !BadPacketsManager.bad()) {
            tryStartCombo = false;
            startCombo();
        }

        if (block
                && actionTimer.hasTimeElapsed(ThreadLocalRandom.current().nextDouble(delay.getValue(), delay.getValue() + 0.01))
                && !BadPacketsManager.bad()) {
            finishCombo();
        }
    }

    @EventHook(EventPriority.HIGH)
    public void onAttack(PlayerAttackEvent event) {
        if (block) return;

        EntityLivingBase target = event.target;

        if (mc.thePlayer == null || target == null
                || !(Math.random() <= chance.getValue() / 100)
                || !InventoryUtils.isHoldingSword()
                || mc.thePlayer.getDistanceToEntity(target) > range.getValue()) {
            return;
        }

        tryStartCombo();
    }

    @Override
    public void onDisable() {
        finishCombo();
        tryStartCombo = false;
        super.onDisable();
    }

    private void finishCombo() {
        block = false;
        mc.gameSettings.keyBindUseItem.setPressed(false);
    }

    private void startCombo() {
        if (mc.thePlayer == null) return;
        if (!Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode())) return;

        block = true;
        mc.gameSettings.keyBindUseItem.setPressed(true);
        actionTimer.reset();
    }

    private void tryStartCombo() {
        tryStartCombo = true;
        waitTimer.reset();
    }
}