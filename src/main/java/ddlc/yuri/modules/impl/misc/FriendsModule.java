package ddlc.yuri.modules.impl.misc;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.player.MiddleClickEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.utils.player.FriendUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;

@ModuleInfo(
        label = "Friends",
        description = "Adds friends to a list to exclude to other modules",
        category = ModuleCategory.MISC)
public final class FriendsModule extends Module {

    public final Property<Boolean> midClickAdd = new Property<>("Mid-Click Add", true);

    @EventHook
    public void onMidClick(MiddleClickEvent event) {
        if(!midClickAdd.getValue()) return;

        if (mc.objectMouseOver != null &&
                mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY &&
                mc.objectMouseOver.entityHit instanceof EntityPlayer) {

            EntityPlayer clickedPlayer = (EntityPlayer) mc.objectMouseOver.entityHit;

            if (clickedPlayer != mc.thePlayer) {
                if (FriendUtils.isFriend(clickedPlayer)) {
                    FriendUtils.remove(clickedPlayer.getName());
                } else {
                    FriendUtils.add(clickedPlayer.getName());
                }
                event.setCancelled(true);
            }
        }
    }

    @Override
    public void onEnable() {
        FriendUtils.loadFriends();
    }
}
