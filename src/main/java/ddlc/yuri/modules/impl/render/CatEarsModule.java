package ddlc.yuri.modules.impl.render;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@ModuleInfo(label = "Cat Ears", description = "ears", category = ModuleCategory.RENDER)
public final class CatEarsModule extends Module {

    private final Property<Boolean> bypass = new Property<>("Bypass", true);

    private final Map<Integer, Rig> rigs = new HashMap<>();
    private int displayList = -1;
    private long lastCleanup;

    @Override
    public void onDisable() {
        rigs.clear();
    }

    public void renderEars(AbstractClientPlayer player, ModelRenderer head, float partialTicks) {
        if (player.isInvisible() || head.isHidden || !head.showModel || (player != mc.thePlayer && !bypass.getValue())) {
            return;
        }
        final long now = System.nanoTime();
        final Rig rig = rigs.computeIfAbsent(player.getEntityId(), id -> new Rig());
        final boolean preview = !mc.getRenderManager().isRenderShadow();
        if (!preview || now - rig.lastWorldUpdate > 200_000_000L) {
            rig.update(player, partialTicks, now, preview);
        }
        if (now - lastCleanup > 5_000_000_000L) {
            lastCleanup = now;
            rigs.values().removeIf(r -> now - r.lastUpdate > 10_000_000_000L);
        }
        if (displayList == -1) {
            displayList = compile();
        }
        GlStateManager.pushMatrix();
        if (player.isSneaking()) {
            GlStateManager.translate(0.0F, 0.2F, 0.0F);
        }
        head.postRender(0.0625F);
        GlStateManager.disableTexture2D();
        GlStateManager.disableCull();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        rig.left.render();
        rig.right.render();
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
        GlStateManager.resetColor();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }
    private static int compile() {
        final int list = GLAllocation.generateDisplayLists(1);
        GL11.glNewList(list, GL11.GL_COMPILE);
        GL11.glBegin(GL11.GL_QUADS);
        for (int ring = 0; ring <= 32; ring++) {
            for (int segment = 0; segment < 64; segment++) {
                vertex(ring, segment);
                vertex(ring, segment + 1);
                vertex(ring + 1, segment + 1);
                vertex(ring + 1, segment);
            }
        }
        GL11.glEnd();
        GL11.glEndList();
        return list;
    }
    private static void vertex(int ring, int segment) {
        final float v = ring == 0 ? -0.2586F : (ring - 1) / 32.0F;
        final float h = Math.max(0.0F, v);
        final float theta = (float) (Math.PI * 2.0 * segment / 64.0);
        final float sin = (float) Math.sin(theta);
        final float vn = clamp(v, 0.002F, 0.985F);

        final float[] up = shape(theta, vn + 0.002F);
        final float[] down = shape(theta, vn - 0.002F);
        final float[] next = shape(theta + 0.002F, vn);
        final float[] prev = shape(theta - 0.002F, vn);
        final float ax = up[0] - down[0], ay = up[1] - down[1], az = up[2] - down[2];
        final float bx = next[0] - prev[0], by = next[1] - prev[1], bz = next[2] - prev[2];
        final float nx = ay * bz - az * by, ny = az * bx - ax * bz, nz = ax * by - ay * bx;
        final float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);

        final float base = smoothstep(-0.05F, 0.45F, h);
        final float fur = 0.6F + 0.4F * base;
        final float sheen = 0.09F * (float) Math.pow(1.0F - Math.abs(sin), 4.0) * base;
        final float pink = 0.85F + 0.15F * base;
        final float deep = clamp(1.0F - h * 1.4F, 0.0F, 1.0F);
        final float inner = smoothstep(0.55F, 0.8F, sin) * smoothstep(0.84F, 0.64F, v) * smoothstep(-0.02F, 0.08F, v);

        GL11.glColor3f(
                lerp((0.13F + 0.11F * h) * fur + sheen, (1.0F - 0.08F * deep) * pink, inner),
                lerp((0.12F + 0.10F * h) * fur + sheen, (0.72F - 0.25F * deep) * pink, inner),
                lerp((0.15F + 0.12F * h) * fur + sheen * 1.15F, (0.78F - 0.20F * deep) * pink, inner));
        GL11.glNormal3f(nx / length, ny / length, nz / length);
        final float[] point = shape(theta, v);
        GL11.glVertex3f(point[0], point[1], point[2]);
    }

    private static float[] shape(float theta, float v) {
        final float t = clamp(v, 0.0F, 1.0F);
        final float skirt = v < 0.0F ? 1.0F + v * 1.5F : 1.0F;
        final float halfWidth = 1.95F * (1.0F - t) * (1.0F + 0.45F * t) * skirt;
        final float halfThickness = 0.425F * (float) Math.sqrt(1.0F - t);
        final float cup = (float) Math.pow(1.0F - t, 1.2F);
        final float sin = (float) Math.sin(theta);

        return new float[]{
                0.5F * t * t + halfWidth * (float) Math.cos(theta),
                v * 5.8F,
                0.6F * t * t + halfThickness * sin - cup * sin * sin
        };
    }

    private static float smoothstep(float edge0, float edge1, float x) {
        final float t = clamp((x - edge0) / (edge1 - edge0), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    private static float clamp(float value, float min, float max) {
        return value < min ? min : (value > max ? max : value);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static final class Spring {
        private float value;
        private float velocity;

        private Spring(float value) {
            this.value = value;
        }

        private void step(float target, float stiffness, float damping, float dt) {
            velocity += (stiffness * (target - value) - damping * velocity) * dt;
            value += velocity * dt;
        }
    }
// animation for ears
    private final class Rig {
        private final Random random = new Random();
        private final Ear left = new Ear(1);
        private final Ear right = new Ear(-1);
        private long lastUpdate;
        private long lastWorldUpdate;
        private float time, forward, horizontal, motionY, yawVelocity, pitchVelocity, hurt, swing;
        private boolean sneaking;

        private void update(AbstractClientPlayer player, float partialTicks, long now, boolean preview) {
            final float dt = lastUpdate == 0L ? 0.0F : Math.min((now - lastUpdate) / 1.0E9F, 0.1F);
            lastUpdate = now;
            if (!preview) {
                lastWorldUpdate = now;
            }

            final double motionX = player.posX - player.lastTickPosX;
            final double motionZ = player.posZ - player.lastTickPosZ;
            final float bodyYaw = (float) Math.toRadians(player.renderYawOffset);
            time += dt;
            forward = (float) (motionZ * MathHelper.cos(bodyYaw) - motionX * MathHelper.sin(bodyYaw));
            horizontal = (float) Math.sqrt(motionX * motionX + motionZ * motionZ);
            motionY = preview ? 0.0F : (float) (player.posY - player.lastTickPosY);
            yawVelocity = preview ? 0.0F : MathHelper.wrapAngleTo180_float(player.rotationYawHead - player.prevRotationYawHead);
            pitchVelocity = preview ? 0.0F : player.rotationPitch - player.prevRotationPitch;
            hurt = player.maxHurtTime > 0 ? clamp((player.hurtTime - partialTicks) / player.maxHurtTime, 0.0F, 1.0F) : 0.0F;
            swing = MathHelper.sin(player.getSwingProgress(partialTicks) * (float) Math.PI);
            sneaking = player.isSneaking();

            left.update(dt);
            right.update(dt);
        }

        private final class Ear {
            private final int side;
            private final float phase;
            private final Spring pitch = new Spring(-4.0F);
            private final Spring roll = new Spring(11.0F);
            private final Spring swivel = new Spring(0.0F);
            private float twitchTimer;
            private float doubleTwitch;
            private float swivelTimer;
            private float swivelGoal;

            private Ear(int side) {
                this.side = side;
                this.phase = random.nextFloat() * (float) Math.PI * 2.0F;
                this.twitchTimer = 1.0F + random.nextFloat() * 4.0F;
            }

            private void update(float dt) {
                if ((twitchTimer -= dt) <= 0.0F) {
                    flick(1.0F);
                    twitchTimer = 2.0F + random.nextFloat() * 5.5F;
                    doubleTwitch = random.nextFloat() < 0.3F ? 0.15F : 0.0F;
                }
                if (doubleTwitch > 0.0F && (doubleTwitch -= dt) <= 0.0F) {
                    flick(0.7F);
                }
                if ((swivelTimer -= dt) <= 0.0F) {
                    swivelGoal = -6.0F + random.nextFloat() * 34.0F;
                    swivelTimer = 1.2F + random.nextFloat() * 4.0F;
                }

                final float sneak = sneaking ? 1.0F : 0.0F;
                final float pitchTarget = -4.0F + MathHelper.sin(time * 1.7F + phase) * 2.0F
                        + clamp(forward * 80.0F, -8.0F, 26.0F)
                        + clamp(pitchVelocity * 0.8F, -12.0F, 12.0F)
                        + sneak * 16.0F + hurt * 48.0F - swing * 12.0F;
                final float rollTarget = 11.0F + MathHelper.sin(time * 1.2F + phase * 1.3F) * 1.6F
                        + clamp(horizontal * 25.0F, 0.0F, 7.0F)
                        + clamp(yawVelocity * side * 0.9F, -16.0F, 16.0F)
                        + clamp(motionY * 40.0F, -10.0F, 12.0F)
                        + sneak * 20.0F + hurt * 30.0F;
                final float swivelTarget = (swivelGoal + sneak * 18.0F + hurt * 25.0F) * (1.0F - swing);

                final int steps = Math.min(8, (int) Math.ceil(dt * 120.0F));
                for (int i = 0; i < steps; i++) {
                    pitch.step(pitchTarget, 170.0F, 10.95F, dt / steps);
                    roll.step(rollTarget, 170.0F, 10.95F, dt / steps);
                    swivel.step(swivelTarget, 102.0F, 8.76F, dt / steps);
                }
            }

            private void flick(float strength) {
                roll.velocity += (random.nextBoolean() ? 1.0F : -1.0F) * (160.0F + random.nextFloat() * 160.0F) * strength;
                swivel.velocity += (random.nextBoolean() ? 1.0F : -1.0F) * (200.0F + random.nextFloat() * 220.0F) * strength;
                pitch.velocity += (random.nextFloat() - 0.5F) * 180.0F * strength;
            }

            private void render() {
                GlStateManager.pushMatrix();
                GlStateManager.translate(side * 0.128125F, -0.471875F, 0.021875F);
                GlStateManager.scale(side * 0.0625F, -0.0625F, -0.0625F);
                GlStateManager.rotate(-clamp(roll.value, -25.0F, 60.0F), 0.0F, 0.0F, 1.0F);
                GlStateManager.rotate(-clamp(pitch.value, -35.0F, 75.0F), 1.0F, 0.0F, 0.0F);
                GlStateManager.rotate(clamp(swivel.value, -30.0F, 60.0F), 0.0F, 1.0F, 0.0F);
                GlStateManager.callList(displayList);
                GlStateManager.popMatrix();
            }
        }
    }
}
