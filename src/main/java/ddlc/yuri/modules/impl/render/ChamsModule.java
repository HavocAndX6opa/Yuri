package ddlc.yuri.modules.impl.render;

import ddlc.yuri.api.properties.Property;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

@ModuleInfo(label = "Chams", description = "Renders players through walls", category = ModuleCategory.RENDER)
public class ChamsModule extends Module {

    private static final Property<Boolean> tileEntities = new Property<>("Tile Entities", false);

    public boolean shouldRender(Entity entity) {
        return entity instanceof EntityPlayer && !mc.thePlayer.canEntityBeSeen(entity) && (!(entity instanceof EntityPlayerSP) || mc.gameSettings.thirdPersonView != 0);
    }

    public boolean doRenderTileEntities() {
        return tileEntities.getValue();
    }
}
