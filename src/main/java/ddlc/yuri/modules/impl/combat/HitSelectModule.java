package ddlc.yuri.modules.impl.combat;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.client.ClientTickEvent;
import ddlc.yuri.api.events.impl.player.PlayerAttackEvent;
import ddlc.yuri.api.events.impl.world.WorldJoinEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.Potion;

import java.util.HashMap;

@ModuleInfo(label = "Hit Select", description = "Holds attacks to prioritise clean hits and combos", category = ModuleCategory.COMBAT)
public final class HitSelectModule extends Module {

    private final NumberProperty pause = new NumberProperty("Pause", 0.0, 0.0, 500.0, 50.0);
    private final NumberProperty onlyInCombo = new NumberProperty("Only in combo", 0.0, 0.0, 10.0, 1.0);
    private final Property<Boolean> disableOnLowHealth = new Property<>("Disable on low health", true);
    private final Property<Boolean> fakeSwing = new Property<>("Fake swing", false);
    private final Property<Boolean> forceFirstHit = new Property<>("Force first hit", false);
    private final Property<Boolean> onlyCriticalHits = new Property<>("Only critical hits", false);
    private final Property<Boolean> onlyWhileJumping = new Property<>("Only while jumping", false);
    private final Property<Boolean> resetComboOnSwap = new Property<>("Reset combo on swap", false);
    private final Property<Boolean> resetOnDamage = new Property<>("Reset on damage", false);

    private final HashMap<Integer, Long> firstHits = new HashMap<>();

    private int combo;
    private int lastTarget = -1;
    private int lastSlot = -1;
    private int prevTargetHurt;
    private int prevPlayerHurt;
    private long lastAttack;
    private long damagedAt;

    @Override
    public void onEnable() {
        reset();
    }

    @Override
    public void onDisable() {
        reset();
    }

    @EventHook
    public void onWorldJoin(WorldJoinEvent event) {
        reset();
    }

    @EventHook
    public void onAttack(PlayerAttackEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        Entity entity = AuraModule.target;
        if (entity == null && mc.objectMouseOver != null) {
            entity = mc.objectMouseOver.entityHit;
        }
        if (entity == null) {
            return;
        }

        if (hold(entity)) {
            event.setCancelled(true);
            return;
        }

        if (onlyInCombo.getValue() > 0.0) {
            lastTarget = entity.getEntityId();
            lastAttack = System.currentTimeMillis();
        }
    }

    @EventHook
    public void onTick(ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        long now = System.currentTimeMillis();

        int slot = mc.thePlayer.inventory.currentItem;
        if (lastSlot != -1 && slot != lastSlot && resetComboOnSwap.getValue()) {
            combo = 0;
        }
        lastSlot = slot;

        int playerHurt = mc.thePlayer.hurtTime;
        if (playerHurt > 0 && playerHurt == mc.thePlayer.maxHurtTime && playerHurt > prevPlayerHurt) {
            combo = 0;
            if (resetOnDamage.getValue()) {
                damagedAt = now;
            }
        }
        prevPlayerHurt = playerHurt;

        Entity tracked = lastTarget == -1 ? null : mc.theWorld.getEntityByID(lastTarget);
        if (tracked instanceof EntityLivingBase) {
            EntityLivingBase living = (EntityLivingBase) tracked;
            int hurt = living.hurtTime;
            if (onlyInCombo.getValue() > 0.0 && hurt > 0 && hurt == living.maxHurtTime && hurt > prevTargetHurt
                    && Math.abs(now - lastAttack) < 750L) {
                combo++;
            }
            prevTargetHurt = hurt;
        } else {
            prevTargetHurt = 0;
        }

        if (combo > 0 && Math.abs(now - lastAttack) > 1250L) {
            combo = 0;
        }
    }

    private boolean hold(Entity entity) {
        if (!(entity instanceof EntityLivingBase)) {
            return false;
        }
        if (disableOnLowHealth.getValue() && !mc.thePlayer.capabilities.isCreativeMode && mc.thePlayer.getHealth() <= 4.0f) {
            return false;
        }
        if (onlyInCombo.getValue() > 0.0 && combo < onlyInCombo.getValue().intValue()) {
            return false;
        }
        if (onlyWhileJumping.getValue() && !GameSettings.isKeyDown(mc.gameSettings.keyBindJump)) {
            return false;
        }
        if (damagedAt != 0L && Math.abs(System.currentTimeMillis() - damagedAt) < 170L) {
            return false;
        }

        boolean hold = shouldHold((EntityLivingBase) entity);

        if (hold && forceFirstHit.getValue()) {
            long now = System.currentTimeMillis();
            Long t = firstHits.get(entity.getEntityId());
            if (t == null || now - t > 10000L) {
                hold = false;
            }
            firstHits.put(entity.getEntityId(), now);
        }

        if (hold && fakeSwing.getValue()) {
            mc.thePlayer.isSwingInProgress = false;
            mc.thePlayer.swingItem();
        }

        return hold;
    }

    private boolean shouldHold(EntityLivingBase e) {
        int hurtLimit = 10 - pause.getValue().intValue() / 50;
        if (pause.getValue() > 0.0 && e.hurtTime > hurtLimit) {
            return true;
        }
        return !(!onlyCriticalHits.getValue() || mc.thePlayer.onGround || mc.thePlayer.fallDistance != 0.0f || mc.thePlayer.isInWater()
                || mc.thePlayer.ridingEntity != null || mc.thePlayer.isPotionActive(Potion.blindness) || mc.thePlayer.isOnLadder());
    }

    private void reset() {
        combo = 0;
        lastTarget = -1;
        lastSlot = -1;
        prevTargetHurt = 0;
        prevPlayerHurt = 0;
        lastAttack = 0L;
        damagedAt = 0L;
        firstHits.clear();
    }
}