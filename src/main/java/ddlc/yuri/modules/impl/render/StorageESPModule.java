package ddlc.yuri.modules.impl.render;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.render.Render3DEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.managers.impl.ColorManager;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.utils.render.RenderUtils;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@ModuleInfo(label = "Storage ESP", description = "Highlights storage blocks like chests and ender chests", category = ModuleCategory.RENDER)
public final class StorageESPModule extends Module {

    private final Property<Boolean> chests = new Property<>("Chests", true);
    private final Property<Boolean> enderChests = new Property<>("Ender Chests", true);
    private final Property<Boolean> throughWalls = new Property<>("Through Walls", true);
    private final Property<Boolean> filled = new Property<>("Filled", false);
    private final Property<Boolean> outline = new Property<>("Outline", true);
    private final NumberProperty lineWidth = new NumberProperty("Line Width", 2.0, 1.0, 5.0, 0.5, outline::getValue);
    private final NumberProperty alpha = new NumberProperty("Alpha", 0.3, 0.1, 1.0, 0.05);

    @EventHook
    public void onRender3D(Render3DEvent event) {
        if (mc.theWorld == null || mc.thePlayer == null) return;

        List<TileEntity> storageBlocks = getStorageBlocks();
        if (storageBlocks.isEmpty()) return;

        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableCull();

        if (throughWalls.getValue()) GL11.glDisable(GL11.GL_DEPTH_TEST);

        for (TileEntity te : storageBlocks) renderStorageBlock(te);

        if (throughWalls.getValue()) GL11.glEnable(GL11.GL_DEPTH_TEST);

        GlStateManager.enableCull();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GlStateManager.color(1f, 1f, 1f, 1f);
        GL11.glPopMatrix();
    }

    private List<TileEntity> getStorageBlocks() {
        List<TileEntity> storageBlocks = new ArrayList<>();

        for (TileEntity tileEntity : mc.theWorld.loadedTileEntityList) {
            if (isValidStorageBlock(tileEntity)) {
                storageBlocks.add(tileEntity);
            }
        }

        return storageBlocks;
    }

    private boolean isValidStorageBlock(TileEntity tileEntity) {
        BlockPos pos = tileEntity.getPos();
        if (pos == null) return false;

        Block block = mc.theWorld.getBlockState(pos).getBlock();
        if (block == null) return false;

        if (tileEntity instanceof TileEntityChest && chests.getValue()) {
            return true;
        }
        return tileEntity instanceof TileEntityEnderChest && enderChests.getValue();
    }

    private void renderStorageBlock(TileEntity tileEntity) {
        BlockPos pos = tileEntity.getPos();
        if (pos == null) return;

        Block block = mc.theWorld.getBlockState(pos).getBlock();
        if (block == null) return;

        AxisAlignedBB boundingBox = block.getSelectedBoundingBox(mc.theWorld, pos);
        if (boundingBox == null) return;

        boundingBox = boundingBox.offset(
                -mc.getRenderManager().renderPosX,
                -mc.getRenderManager().renderPosY,
                -mc.getRenderManager().renderPosZ
        ).expand(-0.002, -0.002, -0.002);

        Color c = ColorManager.getColor();
        float r = c.getRed() / 255f;
        float g = c.getGreen() / 255f;
        float b = c.getBlue() / 255f;
        float a = alpha.getValue().floatValue();

        if (filled.getValue()) {
            GL11.glDepthMask(false);
            GlStateManager.color(r, g, b, a);
            RenderUtils.drawBoundingBox(boundingBox);
            GL11.glDepthMask(true);
        }

        if (outline.getValue()) {
            GL11.glLineWidth(lineWidth.getValue().floatValue());
            GlStateManager.color(r, g, b, a);
            RenderUtils.drawOutlinedBoundingBox(boundingBox);
        }
    }
}
