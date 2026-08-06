package ddlc.yuri.modules.impl.player;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.player.MotionEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.utils.player.packet.PacketUtils;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemPotion;
import net.minecraft.network.play.client.C03PacketPlayer;

@ModuleInfo(label = "Fast Use", description = "Allows you to use items faster", category = ModuleCategory.PLAYER)
public class FastUseModule extends Module {

    public static final NumberProperty speed = new NumberProperty("Speed", 15, 1, 100, 1);
    public static final Property<Boolean> food = new Property<Boolean>("Food Items", true);
    public static final Property<Boolean> potion = new Property<Boolean>("Potion Items", true);
    public static final Property<Boolean> bow = new Property<Boolean>("Bow Items", true);

    @EventHook
    public void onMotion(MotionEvent event) {
        setSuffix(String.valueOf(speed.getValue().intValue()));

        if (!event.isPre()) return;
        if (mc.thePlayer.getHeldItem() == null) return;

        Item item = mc.thePlayer.getHeldItem().getItem();
        boolean allowed = (food.getValue() && item instanceof ItemFood) ||
                (potion.getValue() && item instanceof ItemPotion) ||
                (bow.getValue() && item instanceof ItemBow);
        if (!allowed) return;

        if (mc.thePlayer.isUsingItem() && mc.thePlayer.getItemInUseCount() == 31) {
            for (int i = 0; i <= speed.getValue().intValue(); i++) {
                PacketUtils.sendPacket(new C03PacketPlayer.C06PacketPlayerPosLook(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, mc.thePlayer.onGround));
            }
        }
    }
}
