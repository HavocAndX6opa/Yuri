package ddlc.yuri.modules.impl.movement;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.annotations.EventPriority;
import ddlc.yuri.api.events.impl.client.PacketSendEvent;
import ddlc.yuri.api.events.impl.player.MoveEvent;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.api.events.impl.world.WorldJoinEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.managers.impl.BlinkManager;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.utils.player.MoveUtils;
import ddlc.yuri.utils.player.packet.PacketUtils;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.*;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import net.minecraft.network.play.client.C16PacketClientStatus;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Keyboard;

import java.util.*;

@ModuleInfo(label = "Inv Move", description = "Lets you move while inventories are open", category = ModuleCategory.MOVEMENT)
public final class InvMoveModule extends Module {

    public enum Inventory {
        DISABLED("Disabled"),
        VANILLA("Vanilla"),
        LEGIT("Legit"),
        LEGIT_SLOW("Legit Slow"),
        PAUSE("Pause"),
        BLINK("Blink"),
        CLOSE("Close");

        public final String name;

        Inventory(String name) {
            this.name = name;
        }

        public boolean isLegit() {
            return this == LEGIT || this == LEGIT_SLOW;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum Chest {
        DISABLED("Disabled"),
        VANILLA("Vanilla"),
        PAUSE("Pause"),
        BLINK("Blink");

        public final String name;

        Chest(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private enum State {
        IDLE, OPEN, ACTIVE, FINISH
    }

    private final ModeProperty<Inventory> inventoryMode = new ModeProperty("Inventory", Inventory.VANILLA);
    private final ModeProperty<Chest> chestMode = new ModeProperty("Chest & others", Chest.VANILLA);
    private final NumberProperty motion = new NumberProperty("Motion", 1.0, 0.0, 1.0, 0.01);
    private final Property<Boolean> modifyMotionAfterClick = new Property<>("Modify motion after click", false);
    private final Property<Boolean> slowMotionWhenNecessary = new Property<>("Slow motion when necessary", false);
    private final Property<Boolean> allowJumping = new Property<>("Allow jumping", true);
    private final Property<Boolean> allowMovement = new Property<>("Allow movement", true);
    private final Property<Boolean> allowMidair = new Property<>("Allow midair", true);
    private final Property<Boolean> allowRotating = new Property<>("Allow rotating", true);
    private final Property<Boolean> allowSpeedPotion = new Property<>("Allow speed potion", true);
    private final Property<Boolean> allowSprinting = new Property<>("Allow sprinting", false);

    private final LinkedHashMap<Integer, List<C0EPacketClickWindow>> queued = new LinkedHashMap<>();
    private final Map<Integer, Boolean> emptyCursor = new HashMap<>();

    private State state = State.IDLE;
    private boolean closeQueued;
    private boolean clickedThisTick;
    private boolean clicked;
    private boolean blinking;
    private boolean sendClose;
    private boolean own;
    private boolean lastContainer;
    private int slowTicks;
    private int releaseTicks;

    private Inventory inventory() {
        return inventoryMode.getValue();
    }

    private Chest chest() {
        return chestMode.getValue();
    }

    @Override
    public void onEnable() {
        reset();
    }

    @Override
    public void onDisable() {
        closeBlink();
        reset();
    }

    @EventHook
    public void onWorldJoin(WorldJoinEvent event) {
        blinking = false;
        reset();
    }

    private void reset() {
        sendClose = false;
        clickedThisTick = false;
        clicked = false;
        releaseTicks = 0;
        slowTicks = 0;
        lastContainer = false;
        state = State.IDLE;
        queued.clear();
        emptyCursor.clear();
        closeQueued = false;
    }

    private boolean inInventory() {
        return mc.currentScreen instanceof GuiInventory;
    }

    private boolean inChest() {
        return mc.currentScreen instanceof GuiChest;
    }

    private boolean inCreative() {
        return mc.currentScreen instanceof GuiContainerCreative;
    }

    private boolean inChestLike() {
        GuiScreen screen = mc.currentScreen;
        return screen instanceof GuiChest || screen instanceof GuiCrafting || screen instanceof GuiFurnace || screen instanceof GuiBeacon
                || screen instanceof GuiBrewingStand || screen instanceof GuiDispenser;
    }

    private boolean shouldStopSprint() {
        if (inInventory()) {
            Inventory current = inventory();
            return current == Inventory.PAUSE || current == Inventory.BLINK || current == Inventory.CLOSE;
        }

        if (inChestLike()) {
            return chest() == Chest.PAUSE || chest() == Chest.BLINK;
        }

        return false;
    }

    private void sendLagged(Packet<?> packet) {
        own = true;
        try {
            PacketUtils.sendPacket(packet);
        } finally {
            own = false;
        }
    }

    private void sendBypass(Packet<?> packet) {
        own = true;
        try {
            PacketUtils.queue(packet);
        } finally {
            own = false;
        }
    }

    private void openBlink() {
        if (blinking || BlinkManager.enabled) {
            return;
        }
        blinking = true;
        BlinkManager.enable(false);
    }

    private void closeBlink() {
        if (!blinking) {
            return;
        }
        blinking = false;
        releaseTicks = 0;
        BlinkManager.disable();
    }

    @EventHook(EventPriority.VERY_HIGH)
    public void onPacketSend(PacketSendEvent event) {
        if (own || event.isCancelled() || mc.thePlayer == null) {
            return;
        }

        Packet<?> packet = event.getPacket();
        boolean allow = true;

        if (packet instanceof C16PacketClientStatus) {
            allow = onOpen((C16PacketClientStatus) packet);
        } else if (packet instanceof C0DPacketCloseWindow) {
            allow = onClose();
        } else if (packet instanceof C0EPacketClickWindow) {
            allow = onClick((C0EPacketClickWindow) packet);
        }

        if (!allow) {
            event.setCancelled(true);
        }
    }

    private boolean onOpen(C16PacketClientStatus packet) {
        if (packet.getStatus() != C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT) {
            return true;
        }

        Inventory current = inventory();

        if (current.isLegit()) {
            return false;
        }

        if (current == Inventory.BLINK) {
            openBlink();
        } else if (current == Inventory.CLOSE) {
            return false;
        }

        return true;
    }

    private boolean onClose() {
        clicked = false;
        Inventory current = inventory();

        if (current.isLegit() && (inInventory() || state != State.IDLE || !queued.isEmpty())) {
            if (state != State.IDLE || !queued.isEmpty()) {
                closeQueued = true;
            }
            return false;
        }

        if (current == Inventory.BLINK || chest() == Chest.BLINK) {
            if (blinking) {
                releaseTicks = 2;
            }
        } else if (current == Inventory.CLOSE && inInventory()) {
            return false;
        }

        return true;
    }

    private boolean onClick(C0EPacketClickWindow packet) {
        Inventory current = inventory();

        if (current.isLegit() && inInventory()) {
            boolean holding = mc.thePlayer.inventory.getItemStack() != null;

            if (!holding) {
                int mode = packet.getMode();

                if (mode == 3) {
                    return false;
                }

                if (mode == 0 && packet.getUsedButton() == 1) {
                    int slot = packet.getSlotId();
                    if (slot >= 0 && slot < mc.thePlayer.inventoryContainer.inventorySlots.size()
                            && mc.thePlayer.inventoryContainer.getSlot(slot).getStack() == null) {
                        return false;
                    }
                } else if (mode == 4 && packet.getSlotId() == -999) {
                    return false;
                }
            }

            int tick = mc.thePlayer.ticksExisted;
            queued.computeIfAbsent(tick, k -> new ArrayList<>()).add(packet);
            emptyCursor.put(tick, !holding);
            clicked = true;
            clickedThisTick = true;

            if (state == State.IDLE) {
                state = State.OPEN;
            }

            return false;
        }

        if (current == Inventory.CLOSE && !inChestLike()) {
            sendClose = true;
            if (!blinking) {
                openBlink();
                if (blinking) {
                    releaseTicks = 3;
                }
            }
        }

        clickedThisTick = true;
        clicked = true;
        return true;
    }

    private void sendCloseNow() {
        sendLagged(new C0DPacketCloseWindow(0));

        if (inventory() == Inventory.LEGIT_SLOW) {
            slowTicks = 1;
        }

        closeQueued = false;
        state = State.FINISH;
    }

    private boolean waiting() {
        if (inventory() == Inventory.LEGIT_SLOW) {
            return true;
        }

        if (closeQueued) {
            return queued.size() > 1;
        }

        if (queued.size() != 1) {
            return true;
        }

        Map.Entry<Integer, List<C0EPacketClickWindow>> entry = queued.entrySet().iterator().next();
        return !Boolean.TRUE.equals(emptyCursor.get(entry.getKey()));
    }

    @EventHook
    public void onMove(MoveEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        Inventory current = inventory();

        if (current.isLegit() && (slowTicks > 0 || state == State.ACTIVE && waiting())) {
            event.setForward(0.0f);
            event.setStrafe(0.0f);
            mc.thePlayer.movementInput.jump = false;
            mc.thePlayer.movementInput.sneak = false;
        } else if (clickedThisTick && (current == Inventory.PAUSE && inInventory() || chest() == Chest.PAUSE && inChest())) {
            event.setForward(0.0f);
            event.setStrafe(0.0f);
        }

        clickedThisTick = false;
    }

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        Inventory current = inventory();

        setSuffix(current.isLegit() ? "Legit" : current != Inventory.DISABLED ? current.toString() : chest() != Chest.DISABLED ? chest().toString() : "");

        if (blinking && !BlinkManager.enabled) {
            blinking = false;
            releaseTicks = 0;
        }

        if (blinking && releaseTicks == 0 && mc.currentScreen == null) {
            releaseTicks = 2;
        }

        boolean container = inInventory() || inChestLike() || inCreative();
        if (lastContainer && !container && mc.currentScreen == null) {
            setKeys(false, true, false);
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), GameSettings.isKeyDown(mc.gameSettings.keyBindSprint));
        }
        lastContainer = container;

        if (shouldStopSprint()) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), false);
            mc.thePlayer.setSprinting(false);
        }

        if (current.isLegit()) {
            if (state == State.FINISH) {
                state = queued.isEmpty() ? State.IDLE : State.OPEN;
            }
            if (slowTicks > 0) {
                slowTicks--;
            }
        }

        if (releaseTicks > 0 && --releaseTicks == 0) {
            closeBlink();
        }

        if (current.isLegit()) {
            if (state == State.OPEN) {
                sendLagged(new C16PacketClientStatus(C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT));
                state = State.ACTIVE;
            } else if (state == State.ACTIVE) {
                if (!queued.isEmpty()) {
                    Map.Entry<Integer, List<C0EPacketClickWindow>> entry = queued.entrySet().iterator().next();
                    int tick = entry.getKey();

                    for (C0EPacketClickWindow packet : entry.getValue()) {
                        sendLagged(packet);
                    }

                    boolean empty = Boolean.TRUE.equals(emptyCursor.remove(tick));
                    queued.remove(tick);

                    if (queued.isEmpty() && (empty || closeQueued)) {
                        sendCloseNow();
                    }
                } else if (closeQueued) {
                    sendCloseNow();
                }
            }
        } else if (state != State.IDLE || !queued.isEmpty() || closeQueued) {
            state = State.IDLE;
            queued.clear();
            emptyCursor.clear();
            closeQueued = false;
        }

        if (sendClose) {
            sendClose = false;
            sendBypass(new C0DPacketCloseWindow(0));
        }

        if (mc.currentScreen == null || mc.currentScreen instanceof GuiChat) {
            return;
        }

        if (!allowMidair.getValue() && mc.thePlayer.offGroundTicks > 2) {
            return;
        }

        if (!allowSpeedPotion.getValue() && speedLevel() > 0) {
            return;
        }

        boolean inContainer = true;

        if (inInventory()) {
            if (current == Inventory.DISABLED) {
                return;
            }
        } else if (inChestLike()) {
            if (chest() == Chest.DISABLED) {
                return;
            }
            if (chest() == Chest.BLINK) {
                openBlink();
            }
        } else {
            if (inCreative()) {
                return;
            }
            inContainer = false;
        }

        if (inContainer && motion.getValue() != 1.0 && (!modifyMotionAfterClick.getValue() || clicked) && MoveUtils.isMoving()) {
            double scale = motion.getValue();

            if (slowMotionWhenNecessary.getValue()) {
                if (!mc.thePlayer.onGround) {
                    scale -= 0.2;
                    if (!allowSprinting.getValue()) {
                        scale -= 0.05;
                    }
                }
                scale -= speedLevel() * 0.25;
                scale = Math.max(0.0, scale);
            }

            mc.thePlayer.motionX *= scale;
            mc.thePlayer.motionZ *= scale;
        }

        if (allowMovement.getValue()) {
            setKeys(false, false, inContainer);
        }

        if (allowRotating.getValue()) {
            arrowRotate();
        }
    }

    private int speedLevel() {
        PotionEffect effect = mc.thePlayer.getActivePotionEffect(Potion.moveSpeed);
        return effect == null ? 0 : effect.getAmplifier() + 1;
    }

    private void setKeys(boolean release, boolean all, boolean container) {
        KeyBinding[] keys = {
                mc.gameSettings.keyBindForward,
                mc.gameSettings.keyBindBack,
                mc.gameSettings.keyBindRight,
                mc.gameSettings.keyBindLeft,
                mc.gameSettings.keyBindJump,
                mc.gameSettings.keyBindSneak
        };

        for (KeyBinding key : keys) {
            if (!all && container) {
                if (key == mc.gameSettings.keyBindSneak) {
                    continue;
                }
                if (key == mc.gameSettings.keyBindJump && !allowJumping.getValue()) {
                    continue;
                }
            }
            KeyBinding.setKeyBindState(key.getKeyCode(), !release && GameSettings.isKeyDown(key));
        }
    }

    private void arrowRotate() {
        float yaw = 0.0f;
        float pitch = 0.0f;

        if (Keyboard.isKeyDown(Keyboard.KEY_RIGHT)) {
            yaw += 8.0f;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_LEFT)) {
            yaw -= 8.0f;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_UP)) {
            pitch -= 5.0f;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_DOWN)) {
            pitch += 5.0f;
        }

        if (yaw != 0.0f) {
            mc.thePlayer.rotationYaw += yaw + (float) (Math.random() - 0.5);
        }
        if (pitch != 0.0f) {
            mc.thePlayer.rotationPitch = MathHelper.clamp_float(mc.thePlayer.rotationPitch + pitch + (float) (Math.random() - 0.5), -90.0f, 90.0f);
        }
    }
}