package ddlc.yuri.modules.impl.player;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.client.ClientTickEvent;
import ddlc.yuri.api.events.impl.client.PacketSendEvent;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.modules.impl.movement.SprintModule;
import ddlc.yuri.utils.player.packet.PacketUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import net.minecraft.network.play.client.C16PacketClientStatus;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@ModuleInfo(label = "GUI Move", description = "Allows movement inside of GUI elements", category = ModuleCategory.PLAYER)
public class GuiMoveModule extends Module {

    public final ModeProperty<Mode> mode = new ModeProperty("Mode", Mode.VANILLA);
    public final NumberProperty openDelay = new NumberProperty("Open Delay", 0, 0, 20, 1, () -> mode.getValue() == Mode.LEGIT);
    public final NumberProperty closeDelay = new NumberProperty("Close Delay", 4, 0, 20, 1, () -> mode.getValue() == Mode.LEGIT);
    public final Property<Boolean> lockMoveKey = new Property<Boolean>("Lock Move Keys", false);

    public enum Mode {
        VANILLA("Vanilla"),
        LEGIT("Legit");

        public final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static final Minecraft mc = Minecraft.getMinecraft();
    private final Queue<C0EPacketClickWindow> clickQueue = new ConcurrentLinkedQueue<>();
    private boolean keysPressed = false;
    private C16PacketClientStatus pendingStatus = null;
    private int delayTicks = 0;
    private int openDelayTicks = -1;
    private int closeDelayTicks = -1;
    private final Map<KeyBinding, Boolean> movementKeys = new HashMap<KeyBinding, Boolean>(8) {{
        put(mc.gameSettings.keyBindForward, false);
        put(mc.gameSettings.keyBindBack, false);
        put(mc.gameSettings.keyBindLeft, false);
        put(mc.gameSettings.keyBindRight, false);
        put(mc.gameSettings.keyBindJump, false);
        put(mc.gameSettings.keyBindSneak, false);
        put(mc.gameSettings.keyBindSprint, false);
    }};

    public void pressMovementKeys(boolean skipSneak) {
        this.movementKeys.keySet().stream()
                .filter(key -> !skipSneak || key != mc.gameSettings.keyBindSneak)
                .forEach(key -> KeyBinding.setKeyBindState(key.getKeyCode(), key.getKeyCode() < 0 ? Mouse.isButtonDown(key.getKeyCode() + 100) : Keyboard.isKeyDown(key.getKeyCode())));
        if (Yuri.INSTANCE.getModuleManager().getModule(SprintModule.class).isEnabled()) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), true);
        }
        this.keysPressed = true;
    }

    public void resetMovementKeys() {
        this.movementKeys.replaceAll((k, v) -> false);
    }

    public boolean isSetMovementKeys() {
        return this.movementKeys.values().stream().anyMatch(Boolean::booleanValue);
    }

    public void storeMovementKeys() {
        this.movementKeys.replaceAll((k, v) -> Keyboard.isKeyDown(k.getKeyCode()));
    }

    public void restoreMovementKeys() {
        for (Map.Entry<KeyBinding, Boolean> keyBinding : movementKeys.entrySet()) {
            KeyBinding.setKeyBindState(keyBinding.getKey().getKeyCode(), keyBinding.getValue());
        }
        if (Yuri.INSTANCE.getModuleManager().getModule(SprintModule.class).isEnabled()) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), true);
        }
        this.keysPressed = true;
    }

    public boolean canGuiMove() {
        if (!(mc.currentScreen instanceof GuiContainer)) return false;
        if (mc.currentScreen instanceof GuiContainerCreative) return false;

        switch (this.mode.getValue()) {
            case VANILLA: // Vanilla
                return true;
            case LEGIT: // Legit
                if (!(mc.currentScreen instanceof GuiInventory)) return false;
                return this.closeDelayTicks == -1 && this.clickQueue.isEmpty();
            default:
                return false;
        }
    }

    public boolean temporaryStackIsEmpty() {
        if (mc.thePlayer.inventory.getItemStack() != null) return false;
        if (mc.thePlayer.inventoryContainer instanceof ContainerPlayer) {
            ContainerPlayer containerPlayer = (ContainerPlayer) mc.thePlayer.inventoryContainer;
            for (int i = 0; i < containerPlayer.craftMatrix.getSizeInventory(); i++) {
                ItemStack stack = containerPlayer.craftMatrix.getStackInSlot(i);
                if (stack != null) {
                    return false;
                }
            }
        }
        return true;
    }

    @EventHook
    public void onClientTick(ClientTickEvent event) {
        if (this.openDelayTicks >= 0) {
            this.openDelayTicks--;
            return;
        }

        while (!this.clickQueue.isEmpty()) {
            PacketUtils.sendSilentPacket(this.clickQueue.poll());
        }
        if (this.closeDelayTicks > 0) {
            if (this.temporaryStackIsEmpty()) {
                this.closeDelayTicks--;
            }
        } else if (this.closeDelayTicks == 0) {
            if (mc.currentScreen instanceof GuiInventory)
                PacketUtils.sendSilentPacket(new C0DPacketCloseWindow(0));
            this.closeDelayTicks = -1;
        }
    }

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        setSuffix(mode.getValue().toString());
        if (this.canGuiMove()) {
            if (this.isSetMovementKeys() && this.lockMoveKey.getValue()) {
                this.restoreMovementKeys();
            } else {
                this.pressMovementKeys(true);
            }
        } else {
            if (this.keysPressed) {
                if (mc.currentScreen != null) {
                    KeyBinding.unPressAllKeys();
                } else if (this.isSetMovementKeys()) {
                    this.resetMovementKeys();
                    this.pressMovementKeys(false);
                }
                this.keysPressed = false;
            }
            if (this.pendingStatus != null) {
                PacketUtils.sendSilentPacket(this.pendingStatus);
                this.pendingStatus = null;
            }
            if (this.delayTicks > 0) {
                this.delayTicks--;
            }
        }
    }

    @EventHook
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacket() instanceof C16PacketClientStatus) {
            this.storeMovementKeys();
            if (this.mode.getValue() == Mode.LEGIT) {
                C16PacketClientStatus packet = (C16PacketClientStatus) event.getPacket();
                if (packet.getStatus() == C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT) {
                    event.setCancelled(true);
                }
            }
        } else if (!(event.getPacket() instanceof C0EPacketClickWindow)) {
            if (event.getPacket() instanceof C0DPacketCloseWindow) {
                C0DPacketCloseWindow packet = (C0DPacketCloseWindow) event.getPacket();
                if (((C0DPacketCloseWindow) packet).getWindowId() == 0) {
                    if (this.mode.getValue() == Mode.LEGIT) {
                        if (!this.clickQueue.isEmpty()) {
                            this.clickQueue.clear();
                        }
                        if (this.openDelayTicks >= 0) {
                            this.openDelayTicks = -1;
                        }
                        if (this.closeDelayTicks >= 0) {
                            this.closeDelayTicks = -1;
                        } else {
                            event.setCancelled(true);
                        }
                    } else if (this.pendingStatus != null) {
                        this.pendingStatus = null;
                        event.setCancelled(true);
                    }
                } else {
                    if (!this.clickQueue.isEmpty()) {
                        this.clickQueue.clear();
                    }
                    if (this.openDelayTicks >= 0) {
                        this.openDelayTicks = -1;
                    }
                    if (this.closeDelayTicks >= 0) {
                        this.closeDelayTicks = -1;
                    }
                }
            }
        } else {
            C0EPacketClickWindow packet = (C0EPacketClickWindow) event.getPacket();
            switch (this.mode.getValue()) {
                case LEGIT: // Legit
                    if (packet.getWindowId() == 0) { // inventory
                        if ((packet.getMode() == 3 || packet.getMode() == 4) && packet.getSlotId() == -999) {
                            event.setCancelled(true);
                            return;
                        }
                        KeyBinding.unPressAllKeys();
                        event.setCancelled(true);
                        this.clickQueue.offer(packet);
                        if (this.closeDelayTicks < 0 && this.openDelayTicks < 0) {
                            this.pendingStatus = new C16PacketClientStatus(C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT);
                            this.openDelayTicks = openDelay.getValue().intValue();
                        }
                        this.closeDelayTicks = closeDelay.getValue().intValue();
                    }
                    break;
            }
            if (this.pendingStatus != null) {
                PacketUtils.sendSilentPacket(this.pendingStatus);
                this.pendingStatus = null;
            }
        }
    }

    @Override
    public void onDisable() {
        if (this.keysPressed) {
            if (mc.currentScreen != null) {
                KeyBinding.unPressAllKeys();
            }
            this.keysPressed = false;
        }
        if (this.pendingStatus != null) {
            PacketUtils.sendSilentPacket(this.pendingStatus);
            this.pendingStatus = null;
        }
        this.delayTicks = 0;
    }
}
