package ddlc.yuri.modules.impl.combat;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.client.ClientTickEvent;
import ddlc.yuri.api.events.impl.player.PlayerAttackEvent;
import ddlc.yuri.api.events.impl.world.WorldJoinEvent;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;

@ModuleInfo(label = "Hit Select", description = "Selectively cancels your hits to win more fights", category = ModuleCategory.COMBAT)
public final class HitSelectModule extends Module {

    public static ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.SPRINT);
    private static final NumberProperty chance = new NumberProperty("Chance", 100.0, 10.0, 100.0, 1.0);
    private static final NumberProperty delay = new NumberProperty("Delay", 40.0, 30.0, 50.0, 1.0);

    public enum Mode {
        SPRINT("Sprint"),
        KB_REDUCE("KB Reduce"),
        CRITICALS("Criticals");

        public final String name;

        Mode(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    private long lastAttackTime = -1L;
    private boolean currentShouldAttack = false;

    @EventHook
    public void onWorldJoin(WorldJoinEvent event) {
        reset();
    }

    @EventHook
    public void onAttack(PlayerAttackEvent event) {
        if (!currentShouldAttack) {
            event.setCancelled(true);
            return;
        }

        lastAttackTime = System.currentTimeMillis();
    }

    @EventHook
    public void onTick(ClientTickEvent event) {

        setSuffix(mode.getValue().toString());

        currentShouldAttack = false;

        if (Math.random() * 100 > chance.getValue()) {
            currentShouldAttack = true;
        } else {
            switch (mode.getValue()) {
                case SPRINT: {
                    double dx = mc.thePlayer.posX - mc.thePlayer.prevPosX;
                    double dz = mc.thePlayer.posZ - mc.thePlayer.prevPosZ;
                    double speed = Math.sqrt(dx * dx + dz * dz);
                    currentShouldAttack = speed > 0.1;
                    break;
                }

                case KB_REDUCE: {
                    currentShouldAttack = mc.thePlayer.hurtTime > 0 && !mc.thePlayer.onGround;
                    break;
                }

                case CRITICALS: {
                    currentShouldAttack = !mc.thePlayer.onGround && mc.thePlayer.motionY < 0;
                    break;
                }
            }

            if (!currentShouldAttack) {
                currentShouldAttack = System.currentTimeMillis() - lastAttackTime >= delay.getValue() * 10;
            }
        }

        if (Yuri.INSTANCE.getModuleManager().getModule(AuraModule.class).isEnabled() && AuraModule.target != null) {
            AuraModule.canAttack = currentShouldAttack;
        }
    }

    private void reset() {
        lastAttackTime = -1L;
        currentShouldAttack = false;
    }

    @Override
    public void onDisable() {
        reset();
    }
}
