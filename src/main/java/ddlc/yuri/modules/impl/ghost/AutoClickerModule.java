package ddlc.yuri.modules.impl.ghost;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.client.ClientTickEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.input.Mouse;

import java.util.concurrent.ThreadLocalRandom;

@ModuleInfo(label = "Auto Clicker", description = "Simple, Gaussian auto clicker", category = ModuleCategory.GHOST)
public final class AutoClickerModule extends Module {

    private final NumberProperty minCPS = new NumberProperty("Min CPS", 8.0, 1.0, 25.0, 0.5);
    private final NumberProperty maxCPS = new NumberProperty("Max CPS", 14.0, 1.0, 25.0, 0.5);
    private final Property<Boolean> breakBlocks = new Property<>("Break Blocks", false);
    private final Property<Boolean> weaponsOnly = new Property<>("Weapons Only", false);

    private long lastClick = 0L;

    @EventHook
    public void onTick(ClientTickEvent event) {
        setSuffix(String.valueOf(maxCPS.getValue().intValue()));

        if (mc.currentScreen != null || !mc.inGameHasFocus) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!Mouse.isButtonDown(0)) return;

        if (weaponsOnly.getValue() && !isHoldingWeapon()) return;

        if (breakBlocks.getValue()) {
            MovingObjectPosition mop = mc.objectMouseOver;
            if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) return;
        }

        long now = System.currentTimeMillis();
        int cps = getHumanizedCPS();

        if (now - lastClick >= 1000L / cps) {
            KeyBinding.onTick(mc.gameSettings.keyBindAttack.getKeyCode());
            lastClick = now;
        }
    }

    private boolean isHoldingWeapon() {
        return mc.thePlayer.getHeldItem() != null
                && (mc.thePlayer.getHeldItem().getItem() instanceof ItemSword
                || mc.thePlayer.getHeldItem().getItem() instanceof ItemTool);
    }

    private int getHumanizedCPS() {
        int min = minCPS.getValue().intValue();
        int max = maxCPS.getValue().intValue();

        if (min >= max) return max;

        double mean = (min + max) / 2.0;
        double deviation = (max - min) / 4.0;

        double val = ThreadLocalRandom.current().nextGaussian() * deviation + mean;
        val = Math.max(min, Math.min(max, val));
        val += ThreadLocalRandom.current().nextDouble(-0.4, 0.4);

        return (int) Math.round(val);
    }
}