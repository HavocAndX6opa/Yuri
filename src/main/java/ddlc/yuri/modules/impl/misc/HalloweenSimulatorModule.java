package ddlc.yuri.modules.impl.misc;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.player.MotionEvent;
import ddlc.yuri.api.events.impl.render.Render3DEvent;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.managers.impl.ColorManager;
import ddlc.yuri.managers.impl.RotationManager;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.util.BlockPos;
import net.minecraft.util.StringUtils;
import org.lwjgl.opengl.GL11;
import java.awt.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@ModuleInfo(label = "Halloween Simulator", description = "Auto clicks candy skulls in Hypixel's Halloween Simulator", category = ModuleCategory.MISC)
public class HalloweenSimulatorModule extends Module {

    private final NumberProperty range = new NumberProperty("Range", 4.5f, 1, 6, 0.5);
    private final NumberProperty rotationSpeed = new NumberProperty("Rotation Speed", 10, 0.5, 10, 0.5);

    private final List<BlockPos> skullList = new CopyOnWriteArrayList<>();
    private boolean looking;

    @Override
    public void onDisable() {
        skullList.clear();
        looking = false;
    }

    @EventHook
    public void onMotion(MotionEvent event) {
        if (!event.isPre() || !checkStatus() || mc.thePlayer == null || mc.theWorld == null) return;

        EntityPlayer player = mc.thePlayer;
        double rangeValue = range.getValue();
        double threshold = rangeValue * 1.5;
        double reachSq = rangeValue * rangeValue;

        double eyeX = player.posX;
        double eyeY = player.posY + player.getEyeHeight();
        double eyeZ = player.posZ;

        boolean sentPacket = false;
        skullList.clear();

        for (TileEntity tileEntity : mc.theWorld.loadedTileEntityList) {
            if (!(tileEntity instanceof TileEntitySkull)) continue;

            TileEntitySkull skull = (TileEntitySkull) tileEntity;
            if (skull.getPlayerProfile() == null) continue;

            BlockPos skullPos = skull.getPos();
            double dx = eyeX - skullPos.getX();
            double dy = eyeY - skullPos.getY();
            double dz = eyeZ - skullPos.getZ();
            double distanceSq = dx * dx + dy * dy + dz * dz;

            if (!sentPacket && distanceSq < reachSq
                    && Math.abs(dx) < threshold && Math.abs(dy) < threshold && Math.abs(dz) < threshold) {

                float[] targetRotations = getRotations(skullPos, eyeX, eyeY, eyeZ);
                RotationManager.setRotations(targetRotations, rotationSpeed.getValue(), RotationManager.MovementFix.NORMAL);

                if (!looking) {
                    mc.thePlayer.sendQueue.addToSendQueue(new C08PacketPlayerBlockPlacement(
                            skullPos, 1, player.getHeldItem(), 0.5f, 0.5f, 0.5f));
                    looking = true;
                } else {
                    looking = false;
                }

                sentPacket = true;
            }

            skullList.add(skullPos);
        }
    }

    @EventHook
    public void onRender(Render3DEvent event) {
        if (!checkStatus() || mc.thePlayer == null) return;

        EntityPlayer player = mc.thePlayer;

        double camX = player.lastTickPosX + (player.posX - player.lastTickPosX) * mc.timer.renderPartialTicks;
        double camY = player.lastTickPosY + (player.posY - player.lastTickPosY) * mc.timer.renderPartialTicks;
        double camZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * mc.timer.renderPartialTicks;

        for (BlockPos skullPos : skullList) {
            drawBox(skullPos, camX, camY, camZ, ColorManager.getColor());
        }
    }

    private void drawBox(BlockPos pos, double camX, double camY, double camZ, Color color) {
        double x = pos.getX() - camX, y = pos.getY() - camY, z = pos.getZ() - camZ;

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableDepth();
        GL11.glLineWidth(2f);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        worldRenderer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);

        int r = color.getRed(), g = color.getGreen(), b = color.getBlue();
        double[][] corners = {
                {x, y, z}, {x + 1, y, z}, {x + 1, y, z + 1}, {x, y, z + 1}, {x, y, z},
                {x, y + 1, z}, {x + 1, y + 1, z}, {x + 1, y + 1, z + 1}, {x, y + 1, z + 1}, {x, y + 1, z}
        };
        for (double[] corner : corners) {
            worldRenderer.pos(corner[0], corner[1], corner[2]).color(r, g, b, 255).endVertex();
        }
        tessellator.draw();

        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.popMatrix();
    }

    private boolean checkStatus() {
        if (mc.theWorld == null) return false;
        Scoreboard scoreboard = mc.theWorld.getScoreboard();
        ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1);
        if (objective == null) return false;
        String title = StringUtils.stripControlCodes(objective.getDisplayName());
        return title != null && title.startsWith("HALLOWEEN SIMULATOR");
    }

    private float[] getRotations(BlockPos point, double eyeX, double eyeY, double eyeZ) {
        double x = point.getX() + 0.5 - eyeX;
        double y = point.getY() + 0.5 - eyeY;
        double z = point.getZ() + 0.5 - eyeZ;
        double dist = Math.sqrt(x * x + z * z);

        float yaw = (float) Math.toDegrees(Math.atan2(z, x)) - 90f;
        float pitch = (float) Math.toDegrees(-Math.atan2(y, dist));

        return new float[]{yaw, pitch};
    }
}