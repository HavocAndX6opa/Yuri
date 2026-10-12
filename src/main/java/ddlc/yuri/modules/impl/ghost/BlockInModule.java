package ddlc.yuri.modules.impl.ghost;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.player.MotionEvent;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.api.events.impl.render.Render2DEvent;
import ddlc.yuri.api.events.impl.world.WorldJoinEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.managers.impl.BadPacketsManager;
import ddlc.yuri.managers.impl.RotationManager;
import ddlc.yuri.managers.impl.SlotManager;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.modules.impl.player.ScaffoldModule;
import ddlc.yuri.utils.player.RayCastUtils;
import ddlc.yuri.utils.player.packet.PacketUtils;
import net.minecraft.block.*;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.util.*;
import net.minecraft.world.WorldSettings;
import org.lwjgl.input.Mouse;
import org.lwjgl.util.vector.Vector2f;

import java.util.*;

@ModuleInfo(label = "Block In", description = "Automatically places blocks around you to enclose yourself", category = ModuleCategory.GHOST)
public final class BlockInModule extends Module {

    private static final float ROT_SPEED = 10.0f;
    private static final int MAX_ATTEMPTS = 5;
    private static final EnumFacing[] ORDER = {EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.EAST, EnumFacing.UP, EnumFacing.DOWN};
    private static final EnumFacing[] ORDER_UP = {EnumFacing.UP, EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.EAST, EnumFacing.DOWN};

    private final NumberProperty delay = new NumberProperty("Delay", 0.0, 0.0, 10.0, 0.5);
    private final Property<Boolean> disableInCreative = new Property<>("Disable In Creative", true);
    private final Property<Boolean> disableObsidian = new Property<>("Disable Obsidian", false);
    private final Property<Boolean> disableWhenComplete = new Property<>("Disable When Complete", false);
    private final Property<Boolean> onlyWhileMiddleClicking = new Property<>("Only While Middle Clicking", false);
    private final Property<Boolean> prioritizeStrongerBlocks = new Property<>("Prioritize Stronger Blocks", false);
    private final Property<Boolean> showPercentage = new Property<>("Show Percentage", true);
    private final Property<Boolean> silentAim = new Property<>("Silent Aim", true);
    private final Property<Boolean> silentSwing = new Property<>("Silent Swing", false);
    private final Property<Boolean> skipNearBed = new Property<>("Skip Near Bed", false);

    private static final class Node {
        final BlockPos pos;
        final int score;

        Node(BlockPos pos, int score) {
            this.pos = pos;
            this.score = score;
        }
    }

    private static final class AimPoint {
        final float yaw;
        final float pitch;

        AimPoint(float yaw, float pitch) {
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }

    private boolean active = false;
    private boolean swapped = false;
    private boolean waitRelease = false;
    private String percentText = null;
    private int percentColor = 0xFFFFFF;
    private long startedAt = 0L;
    private long stoppedAt = 0L;
    private int blockSlot = -1;

    private BlockPos against;
    private EnumFacing face;
    private BlockPos targetPos;
    private BlockPos aimedAgainst;
    private EnumFacing aimedFace;
    private Vec3 pendingHit;
    private List<BlockPos> path;
    private final Deque<BlockPos> retry = new ArrayDeque<>();
    private final Set<BlockPos> retrySet = new HashSet<>();
    private final Set<BlockPos> failed = new HashSet<>();
    private EnumFacing[] facings;

    private float aimYaw;
    private float aimPitch;
    private Float fromYaw;
    private Float fromPitch;
    private Float lastYaw;
    private Float lastPitch;
    private Float refYaw;
    private int steps;
    private int step;
    private boolean roundUp = true;

    private int boxMinX, boxMaxX, boxMinY, boxMaxY, boxMinZ, boxMaxZ;

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        pendingHit = null;

        if (mc.thePlayer == null || mc.theWorld == null || mc.playerController == null) {
            if (active) stop();
            return;
        }

        ScaffoldModule scaffold = Yuri.INSTANCE.getModuleManager().getModule(ScaffoldModule.class);
        WorldSettings.GameType type = mc.playerController.getCurrentGameType();
        boolean invalid = !mc.inGameHasFocus
                || mc.currentScreen != null
                || type == WorldSettings.GameType.ADVENTURE
                || type == WorldSettings.GameType.SPECTATOR
                || (scaffold != null && scaffold.isEnabled())
                || (disableInCreative.getValue() && mc.thePlayer.capabilities.isCreativeMode);
        if (invalid) {
            if (active) stop();
            return;
        }

        if (onlyWhileMiddleClicking.getValue() && waitRelease) {
            if (Mouse.isButtonDown(2)) return;
            waitRelease = false;
        }

        boolean run = !onlyWhileMiddleClicking.getValue() || Mouse.isButtonDown(2);
        if (!run) {
            if (active) stop();
            return;
        }

        if (BadPacketsManager.bad(false, false, false, true, false)) return;

        if (!active) {
            blockSlot = findSlot();
            if (blockSlot == -1) return;
            SlotManager.swap(blockSlot, false);
            swapped = true;
            active = true;
            startedAt = System.currentTimeMillis();
            resetSteps();
            resetAim();
            captureFacings();
            mc.gameSettings.keyBindAttack.setPressed(false);
            mc.gameSettings.keyBindUseItem.setPressed(false);
        }

        updatePlayerBox();

        int px = MathHelper.floor_double(mc.thePlayer.posX);
        int py = MathHelper.floor_double(mc.thePlayer.posY);
        int pz = MathHelper.floor_double(mc.thePlayer.posZ);
        int percent = Math.round(enclosure(px, py, pz) * 100.0f);
        percentText = percent + "%";
        percentColor = percent != 100 ? 0xFFFFFF : 0x00FF00;
        if (percent == 100 && disableWhenComplete.getValue()) {
            if (!onlyWhileMiddleClicking.getValue()) {
                toggle();
            } else {
                stop();
                waitRelease = true;
            }
            return;
        }

        ItemStack stack = mc.thePlayer.inventory.getStackInSlot(mc.thePlayer.inventory.currentItem);
        if (!usable(stack)) {
            int n = findSlot();
            if (n == -1) {
                stop();
                return;
            }
            blockSlot = n;
            SlotManager.swapBack();
            SlotManager.swap(blockSlot, false);
            stack = mc.thePlayer.inventory.getStackInSlot(mc.thePlayer.inventory.currentItem);
        }

        failed.clear();
        AimPoint last = null;
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            if (!(hasTarget() || findTarget() || retryTarget())) {
                clearTarget();
                break;
            }
            AimPoint aim = aimAtFace(against, face);
            if (aim == null || !canPlace(stack)) {
                markFailed();
                clearTarget();
                continue;
            }
            aimAt(aim);
            if (steps > 0 && step < steps) {
                stepToward(steps);
                return;
            }
            double reach = mc.playerController.getBlockReachDistance();
            MovingObjectPosition hit = RayCastUtils.rayCastBlock(new Vector2f(aimYaw, aimPitch), reach, against, face);
            if (hit != null) {
                pendingHit = hit.hitVec;
                finish(aimYaw, aimPitch);
                return;
            }
            last = aim;
            markFailed();
            clearTarget();
        }

        if (fromYaw != null || lastYaw != null) {
            int n = (int) delay.getValue().doubleValue();
            if (n > 0 && step < n) {
                stepToward(n);
            } else {
                finish(aimYaw, aimPitch);
            }
            return;
        }
        if (last != null) {
            finish(last.yaw, last.pitch);
        }
    }

    @EventHook
    public void onMotion(MotionEvent event) {
        if (!event.isPre() || !active || pendingHit == null || mc.thePlayer == null || mc.theWorld == null) return;

        Vec3 hitVec = pendingHit;
        pendingHit = null;

        ItemStack stack = mc.thePlayer.inventory.getStackInSlot(mc.thePlayer.inventory.currentItem);
        if (against == null || face == null || targetPos == null || !usable(stack)) {
            clearTarget();
            return;
        }

        if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, stack, against, face, hitVec)) {
            if (silentSwing.getValue()) {
                PacketUtils.sendPacket(new C0APacketAnimation());
            } else {
                mc.thePlayer.swingItem();
            }
            if (path != null && !path.isEmpty() && path.get(0).equals(targetPos)) {
                path.remove(0);
            }
            clearTarget();
            resetSteps();
            float y = lastYaw != null ? lastYaw : mc.thePlayer.rotationYaw;
            if (refYaw == null) refYaw = y;
            setFrom(unwrap(y, refYaw), lastPitch != null ? lastPitch : mc.thePlayer.rotationPitch);
        } else {
            markFailed();
            clearTarget();
        }
    }

    @EventHook
    public void onRender2D(Render2DEvent event) {
        if (!showPercentage.getValue() || percentText == null || mc.fontRendererObj == null) return;

        long now = System.currentTimeMillis();
        int a;
        if (!active) {
            long age = now - stoppedAt;
            if (age >= 125L) {
                percentText = null;
                return;
            }
            a = (int) (240.0f - (float) age / 125.0f * 240.0f);
        } else {
            long age = now - startedAt;
            a = age >= 125L ? 240 : (int) ((float) age / 125.0f * 240.0f);
        }
        if (a <= 4) return;

        ScaledResolution sr = new ScaledResolution(mc);
        float x = sr.getScaledWidth() / 2.0f - mc.fontRendererObj.getStringWidth(percentText) / 2.0f;
        float y = sr.getScaledHeight() / 2.0f + 11.0f;

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        mc.fontRendererObj.drawString(percentText, x, y, (a << 24) | (percentColor & 0xFFFFFF), true);
        GlStateManager.disableBlend();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    }

    @EventHook
    public void onWorldJoin(WorldJoinEvent event) {
        swapped = false;
        reset();
    }

    @Override
    public void onDisable() {
        if (active) {
            stop();
        } else {
            reset();
        }
        waitRelease = false;
        super.onDisable();
    }

    private void stop() {
        if (swapped) {
            SlotManager.swapBack();
            swapped = false;
        }
        reset();
    }

    private void reset() {
        if (active) {
            active = false;
            stoppedAt = System.currentTimeMillis();
        }
        roundUp = true;
        blockSlot = -1;
        path = null;
        retry.clear();
        retrySet.clear();
        failed.clear();
        step = 0;
        pendingHit = null;
        resetAim();
        lastPitch = null;
        lastYaw = null;
        refYaw = null;
        clearTarget();
        facings = null;
    }

    private void updatePlayerBox() {
        AxisAlignedBB bb = mc.thePlayer.getEntityBoundingBox();
        boxMinX = MathHelper.floor_double(bb.minX);
        boxMaxX = (int) Math.ceil(bb.maxX) - 1;
        boxMinY = MathHelper.floor_double(bb.minY);
        boxMaxY = (int) Math.ceil(bb.maxY) - 1;
        boxMinZ = MathHelper.floor_double(bb.minZ);
        boxMaxZ = (int) Math.ceil(bb.maxZ) - 1;
    }

    private boolean isPlayerBlock(BlockPos p) {
        return p.getX() >= boxMinX && p.getX() <= boxMaxX
                && p.getY() >= boxMinY && p.getY() <= boxMaxY
                && p.getZ() >= boxMinZ && p.getZ() <= boxMaxZ;
    }

    private BlockPos playerPos() {
        return new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
    }

    private Block blockAt(BlockPos p) {
        return mc.theWorld.getBlockState(p).getBlock();
    }

    private boolean isReplaceable(BlockPos p) {
        return blockAt(p).isReplaceable(mc.theWorld, p);
    }

    private boolean isSupport(Block block) {
        return block.getMaterial().isSolid()
                && block.isFullCube()
                && !(block instanceof BlockContainer)
                && !(block instanceof BlockWorkbench)
                && !(block instanceof BlockAnvil);
    }

    private boolean hasSupport(BlockPos p) {
        for (EnumFacing f : EnumFacing.VALUES) {
            if (isSupport(blockAt(p.offset(f)))) return true;
        }
        return false;
    }

    private boolean bedAdjacent(BlockPos p) {
        for (EnumFacing f : EnumFacing.VALUES) {
            if (blockAt(p.offset(f)) instanceof BlockBed) return true;
        }
        return false;
    }

    private boolean isBlockItem(ItemStack stack) {
        return stack != null && stack.getItem() instanceof ItemBlock;
    }

    private Block blockOf(ItemStack stack) {
        return ((ItemBlock) stack.getItem()).getBlock();
    }

    private boolean isPlaceable(Block block) {
        return block.getMaterial().isSolid()
                && block.isFullCube()
                && !(block instanceof BlockFalling)
                && !(block instanceof BlockContainer)
                && !(block instanceof BlockWorkbench)
                && !(block instanceof BlockAnvil)
                && block != Blocks.tnt
                && block != Blocks.cactus;
    }

    private boolean usable(ItemStack stack) {
        if (!isBlockItem(stack)) return false;
        Block block = blockOf(stack);
        if (!isPlaceable(block)) return false;
        if (block == Blocks.pumpkin || (disableObsidian.getValue() && block == Blocks.obsidian)) return false;
        return stack.stackSize > 0;
    }

    private int findSlot() {
        int slot = -1;
        int most = 0;
        float best = -1.0f;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (!usable(stack)) continue;
            Block block = blockOf(stack);
            int size = stack.stackSize;
            if (prioritizeStrongerBlocks.getValue()) {
                float hardness = block.getBlockHardness(mc.theWorld, BlockPos.ORIGIN);
                if (hardness < 0.0f) continue;
                if (!(hardness > best || (hardness == best && size > most))) continue;
                best = hardness;
                most = size;
                slot = i;
                continue;
            }
            if (size <= most) continue;
            most = size;
            slot = i;
        }
        return slot;
    }

    private boolean canPlace(ItemStack stack) {
        if (!isBlockItem(stack) || against == null || face == null || targetPos == null) return false;
        return ((ItemBlock) stack.getItem()).canPlaceBlockOnSide(mc.theWorld, against, face, mc.thePlayer, stack);
    }

    private Vec3 facePoint(BlockPos pos, EnumFacing side, double u, double v) {
        double x = pos.getX();
        double y = pos.getY();
        double z = pos.getZ();
        switch (side) {
            case UP:
                return new Vec3(x + u, y + 1.0, z + v);
            case DOWN:
                return new Vec3(x + u, y, z + v);
            case NORTH:
                return new Vec3(x + u, y + v, z);
            case SOUTH:
                return new Vec3(x + u, y + v, z + 1.0);
            case WEST:
                return new Vec3(x, y + u, z + v);
            default:
                return new Vec3(x + 1.0, y + u, z + v);
        }
    }

    private AimPoint aimAtFace(BlockPos pos, EnumFacing side) {
        Vec3 eyes = mc.thePlayer.getPositionEyes(1.0f);
        double reach = mc.playerController.getBlockReachDistance();
        float curYaw = lastYaw != null ? lastYaw : mc.thePlayer.rotationYaw;
        float curPitch = lastPitch != null ? lastPitch : mc.thePlayer.rotationPitch;
        AimPoint best = null;
        float bestDelta = Float.MAX_VALUE;
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                Vec3 point = facePoint(pos, side, 0.1 + 0.2 * i, 0.1 + 0.2 * j);
                double dx = point.xCoord - eyes.xCoord;
                double dy = point.yCoord - eyes.yCoord;
                double dz = point.zCoord - eyes.zCoord;
                double flat = Math.sqrt(dx * dx + dz * dz);
                float yaw = MathHelper.wrapAngleTo180_float((float) (Math.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F);
                float pitch = MathHelper.clamp_float((float) (-(Math.atan2(dy, flat) * 180.0D / Math.PI)), -90.0f, 90.0f);
                if (RayCastUtils.rayCastBlock(new Vector2f(yaw, pitch), reach, pos, side) == null) continue;
                float delta = Math.abs(wrap(yaw - curYaw)) + Math.abs(pitch - curPitch);
                if (delta < bestDelta) {
                    bestDelta = delta;
                    best = new AimPoint(yaw, pitch);
                }
            }
        }
        return best;
    }

    private void aimAt(AimPoint aim) {
        boolean changed = aimedAgainst == null || !aimedAgainst.equals(against) || aimedFace != face;
        float dYaw = Math.abs(wrap(aim.yaw - aimYaw));
        float dPitch = Math.abs(aim.pitch - aimPitch);
        boolean moved = dYaw > 1.0f || dPitch > 1.0f;
        if (!changed && !moved && fromYaw != null) {
            float ref = refYaw != null ? refYaw : mc.thePlayer.rotationYaw;
            aimYaw = unwrap(aim.yaw, ref);
            aimPitch = aim.pitch;
        } else {
            float y = lastYaw != null ? lastYaw : mc.thePlayer.rotationYaw;
            float p = lastPitch != null ? lastPitch : mc.thePlayer.rotationPitch;
            if (refYaw == null) refYaw = y;
            float u = unwrap(y, refYaw);
            float target = unwrap(aim.yaw, refYaw);
            setFrom(u, p);
            aimYaw = target;
            aimPitch = aim.pitch;
            aimedAgainst = against;
            aimedFace = face;
        }
    }

    private void stepToward(int total) {
        float t = clamp01(((float) step + 1.0f) / (float) total);
        float startYaw = fromYaw != null ? fromYaw : (lastYaw != null ? unwrap(lastYaw, refYaw != null ? refYaw : mc.thePlayer.rotationYaw) : mc.thePlayer.rotationYaw);
        float startPitch = fromPitch != null ? fromPitch : (lastPitch != null ? lastPitch : mc.thePlayer.rotationPitch);
        ++step;
        finish(lerp(startYaw, aimYaw, t), lerp(startPitch, aimPitch, t));
    }

    private void finish(float yaw, float pitch) {
        float clampedPitch = MathHelper.clamp_float(pitch, -90.0f, 90.0f);
        lastYaw = yaw;
        lastPitch = clampedPitch;
        if (refYaw == null) refYaw = yaw;
        refYaw = unwrap(yaw, refYaw);
        RotationManager.setRotations(new float[]{yaw, clampedPitch}, ROT_SPEED, RotationManager.MovementFix.NORMAL);
        if (!silentAim.getValue() && mc.thePlayer != null) {
            mc.thePlayer.rotationYaw = yaw;
            mc.thePlayer.rotationPitch = clampedPitch;
        }
    }

    private boolean ownVertical(BlockPos pos, EnumFacing facing) {
        return pos.equals(playerPos()) && (facing == EnumFacing.UP || facing == EnumFacing.DOWN);
    }

    private boolean skip(BlockPos pos) {
        return failed.contains(pos) || isPlayerBlock(pos) || (skipNearBed.getValue() && bedAdjacent(pos));
    }

    private boolean hasTarget() {
        return against != null && face != null && targetPos != null && !isPlayerBlock(targetPos) && isReplaceable(targetPos);
    }

    private boolean retryTarget() {
        int n = retry.size();
        while (n-- > 0 && !retry.isEmpty()) {
            BlockPos pos = retry.pollFirst();
            if (pos == null) continue;
            if (skip(pos) || !isReplaceable(pos)) {
                retrySet.remove(pos);
                continue;
            }
            if (findSupport(pos)) {
                retrySet.remove(pos);
                return true;
            }
            retry.addLast(pos);
        }
        return false;
    }

    private boolean usePath(List<BlockPos> list) {
        if (list == null || list.isEmpty()) return false;
        path = list;
        while (!path.isEmpty() && isPlayerBlock(path.get(0))) {
            path.remove(0);
        }
        return !path.isEmpty() && tryPathHead();
    }

    private boolean tryPathHead() {
        BlockPos next = path.get(0);
        return !skip(next) && findSupport(next);
    }

    private boolean findTarget() {
        BlockPos own = playerPos();
        BlockPos roof = own.up(2);

        if (path != null && !path.isEmpty()) {
            while (!path.isEmpty() && (retrySet.contains(path.get(0)) || isPlayerBlock(path.get(0)))) {
                path.remove(0);
            }
            if (!path.isEmpty() && tryPathHead()) return true;
        }

        if (isReplaceable(roof) && hasSupport(roof) && !skip(roof) && findSupport(roof)) return true;

        if (isReplaceable(roof) && usePath(findPath(roof, own, 8, -2, 6))) return true;

        for (EnumFacing facing : facings()) {
            BlockPos below = own.down().offset(facing);
            BlockPos feet = own.offset(facing);
            BlockPos head = feet.up();

            if (isReplaceable(below)) {
                if (hasSupport(below)) {
                    if (!skip(below) && findSupportUp(below)) return true;
                } else if (usePath(findPath(below, own, 4, -6, 0))) {
                    return true;
                }
            }

            if (isReplaceable(feet)) {
                if (!skip(feet) && findSupportBeside(feet, own, facing)) return true;
                if (!hasSupport(feet)) {
                    if (usePath(findPath(feet, own, 4, -3, 1))) return true;
                } else if (!skip(feet) && findSupport(feet, ORDER)) {
                    return true;
                }
            }

            if (!isReplaceable(head)) continue;
            if (hasSupport(head)) {
                if (!skip(head) && findSupport(head, ORDER)) return true;
                continue;
            }
            if (usePath(findPath(head, own, 4, -2, 2))) return true;
        }

        return isReplaceable(roof) && hasSupport(roof) && !skip(roof) && findSupport(roof);
    }

    private boolean findSupportBeside(BlockPos pos, BlockPos own, EnumFacing facing) {
        if (isPlayerBlock(pos)) return false;
        if (pos.getY() == own.getY()) {
            if (!ownVertical(own, facing) && isSupport(blockAt(own))) {
                against = own;
                face = facing;
                targetPos = pos;
                return true;
            }
            BlockPos below = pos.down();
            if (!ownVertical(below, EnumFacing.UP) && isSupport(blockAt(below))) {
                against = below;
                face = EnumFacing.UP;
                targetPos = pos;
                return true;
            }
        }
        return findSupport(pos, ORDER);
    }

    private boolean findSupportUp(BlockPos pos) {
        if (isPlayerBlock(pos)) return false;
        return findSupport(pos, ORDER_UP);
    }

    private boolean findSupport(BlockPos pos) {
        BlockPos own = playerPos();
        if (!pos.equals(own.north()) && !pos.equals(own.south())) {
            BlockPos below = own.down();
            return !pos.equals(below.east()) && !pos.equals(below.west()) && !pos.equals(below.north()) && !pos.equals(below.south())
                    ? findSupport(pos, ORDER) : findSupportUp(pos);
        }
        return false;
    }

    private boolean findSupport(BlockPos pos, EnumFacing[] order) {
        if (isPlayerBlock(pos)) return false;
        for (EnumFacing f : order) {
            BlockPos n = pos.offset(f.getOpposite());
            if (ownVertical(n, f)) continue;
            if (isSupport(blockAt(n))) {
                against = n;
                face = f;
                targetPos = pos;
                return true;
            }
        }
        return false;
    }

    private void scan(Set<BlockPos> solids, Set<BlockPos> open, BlockPos own, int r, int minY, int maxY) {
        for (int x = -r; x <= r; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos p = own.add(x, y, z);
                    if (isSupport(blockAt(p))) {
                        solids.add(p);
                    } else if (isReplaceable(p) && !isPlayerBlock(p)) {
                        open.add(p);
                    }
                }
            }
        }
    }

    private int nearestDistance(BlockPos p, Set<BlockPos> goals) {
        int best = Integer.MAX_VALUE;
        for (BlockPos g : goals) {
            int d = Math.abs(p.getX() - g.getX()) + Math.abs(p.getY() - g.getY()) + Math.abs(p.getZ() - g.getZ());
            if (d < best) best = d;
        }
        return best;
    }

    private List<BlockPos> findPath(BlockPos pos, BlockPos own, int r, int minY, int maxY) {
        Set<BlockPos> solids = new HashSet<>();
        Set<BlockPos> open = new HashSet<>();
        scan(solids, open, own, r, minY, maxY);
        if (open.isEmpty()) return null;

        for (EnumFacing f : EnumFacing.VALUES) {
            if (solids.contains(pos.offset(f.getOpposite()))) return null;
        }

        List<BlockPos> starts = new ArrayList<>();
        for (BlockPos p : open) {
            for (EnumFacing f : EnumFacing.VALUES) {
                if (solids.contains(p.offset(f))) {
                    starts.add(p);
                    break;
                }
            }
        }
        if (starts.isEmpty()) return null;

        Set<BlockPos> goals = new HashSet<>();
        for (EnumFacing f : EnumFacing.VALUES) {
            BlockPos p = pos.offset(f.getOpposite());
            if (open.contains(p) && !retrySet.contains(p)) goals.add(p);
        }
        if (goals.isEmpty()) return null;

        int size = open.size();
        int limit = Math.min(8000, Math.max(500, size * 2));
        PriorityQueue<Node> queue = new PriorityQueue<>(Comparator.comparingInt((Node n) -> n.score));
        Map<BlockPos, Integer> cost = new HashMap<>(size);
        Map<BlockPos, BlockPos> parent = new HashMap<>(size);
        for (BlockPos p : starts) {
            cost.put(p, 0);
            parent.put(p, null);
            queue.add(new Node(p, nearestDistance(p, goals)));
        }

        Set<BlockPos> closed = new HashSet<>();
        int visited = 0;
        BlockPos found = null;
        while (!queue.isEmpty()) {
            Node node = queue.poll();
            BlockPos p = node.pos;
            if (closed.contains(p)) continue;
            closed.add(p);
            if (goals.contains(p)) {
                found = p;
                break;
            }
            if (++visited > limit) break;
            int c = cost.get(p);
            for (EnumFacing f : EnumFacing.VALUES) {
                BlockPos n = p.offset(f);
                if (!open.contains(n) || closed.contains(n)) continue;
                int nc = c + 1 + (p.getY() != n.getY() ? 1 : 0);
                Integer old = cost.get(n);
                if (old == null || nc < old) {
                    cost.put(n, nc);
                    parent.put(n, p);
                    queue.add(new Node(n, nc + nearestDistance(n, goals)));
                }
            }
        }
        if (found == null) return null;

        List<BlockPos> route = new ArrayList<>();
        for (BlockPos p = found; p != null; p = parent.get(p)) {
            route.add(p);
        }
        Collections.reverse(route);
        List<BlockPos> out = new ArrayList<>();
        for (BlockPos p : route) {
            if (!isSupport(blockAt(p)) && !isPlayerBlock(p)) out.add(p);
        }
        return out;
    }

    private void markFailed() {
        if (targetPos == null) return;
        failed.add(targetPos);
        if (path != null && !path.isEmpty() && targetPos.equals(path.get(0))) {
            if (!retrySet.contains(targetPos)) addRetry(targetPos);
            path = null;
        }
        if (!retrySet.contains(targetPos) && !isPlayerBlock(targetPos)) {
            addRetry(targetPos);
        }
    }

    private void addRetry(BlockPos pos) {
        if (retry.size() >= 3) {
            BlockPos old = retry.pollFirst();
            if (old != null) retrySet.remove(old);
        }
        retry.addLast(pos);
        retrySet.add(pos);
    }

    private float enclosure(int x, int y, int z) {
        int n = 0;
        if (solid(x, y + 2, z)) n++;
        if (solid(x + 1, y, z)) n++;
        if (solid(x - 1, y, z)) n++;
        if (solid(x, y, z + 1)) n++;
        if (solid(x, y, z - 1)) n++;
        if (solid(x + 1, y + 1, z)) n++;
        if (solid(x - 1, y + 1, z)) n++;
        if (solid(x, y + 1, z + 1)) n++;
        if (solid(x, y + 1, z - 1)) n++;
        return (float) n / 9.0f;
    }

    private boolean solid(int x, int y, int z) {
        return isSupport(blockAt(new BlockPos(x, y, z)));
    }

    private void captureFacings() {
        EnumFacing f = mc.thePlayer.getHorizontalFacing();
        facings = new EnumFacing[]{f, f.rotateY(), f.getOpposite(), f.rotateYCCW()};
    }

    private EnumFacing[] facings() {
        return facings != null ? facings : new EnumFacing[]{EnumFacing.EAST, EnumFacing.WEST, EnumFacing.NORTH, EnumFacing.SOUTH};
    }

    private void resetAim() {
        fromYaw = null;
        fromPitch = null;
        aimYaw = 0.0f;
        aimPitch = 0.0f;
        aimedAgainst = null;
        aimedFace = null;
    }

    private void resetSteps() {
        double d = delay.getValue().doubleValue();
        if (d % 1.0 == 0.5) {
            d = Math.floor(d);
            if (roundUp) d++;
            roundUp = !roundUp;
        }
        steps = (int) d;
        step = 0;
    }

    private void clearTarget() {
        against = null;
        face = null;
        targetPos = null;
    }

    private void setFrom(float yaw, float pitch) {
        fromYaw = yaw;
        fromPitch = pitch;
    }

    private float clamp01(float f) {
        return f < 0.0f ? 0.0f : Math.min(f, 1.0f);
    }

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private float wrap(float f) {
        f %= 360.0f;
        if (f >= 180.0f) f -= 360.0f;
        if (f < -180.0f) f += 360.0f;
        return f;
    }

    private float unwrap(float yaw, float ref) {
        return ref + wrap(yaw - ref);
    }
}