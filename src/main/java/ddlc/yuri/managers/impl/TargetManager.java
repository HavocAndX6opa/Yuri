package ddlc.yuri.managers.impl;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.player.KillEvent;
import ddlc.yuri.api.events.impl.player.PlayerAttackEvent;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.utils.client.TimerUtils;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static ddlc.yuri.utils.misc.IMinecraft.mc;

public class TargetManager {

    @Getter
    @Setter
    private static EntityLivingBase target;
    @Getter
    private static List<Entity> targetList = new CopyOnWriteArrayList<>();
    private static final TimerUtils switchTimer = new TimerUtils();
    @Getter
    @Setter
    private static Mode mode;
    @Getter
    @Setter
    private static List<Targets> targets;
    @Getter
    @Setter
    private static float seekRange;
    @Getter
    @Setter
    private static int switchTime;
    private int targetIndex;

    public TargetManager(float seekRange) {
        mode = Mode.ADAPTIVE;
        targets = Arrays.asList(Targets.PLAYERS, Targets.HOSTILES);
        TargetManager.seekRange = seekRange;
        switchTime = 2;
    }

    public TargetManager() {
        mode = Mode.ADAPTIVE;
        targets = Arrays.asList(Targets.PLAYERS, Targets.HOSTILES);
        seekRange = 6.0f;
        switchTime = 2;
    }

    public static void configure(List<Targets> targets) {
        TargetManager.targets = targets;
    }

    public enum Mode {
        ADAPTIVE("Adaptive"),
        SWITCH("Switch"),
        SINGLE("Single");

        public final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum Targets {
        PLAYERS("Players"),
        TEAMMATES("Teammates"),
        INVISIBLES("Invisibles"),
        HOSTILES("Hostiles"),
        ANIMALS("Animals");

        public final String name;

        Targets(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        targetList = getTargets();

        if (targetList.isEmpty()) {
            target = null;
            return;
        }

        selectTarget();

        // kill event shiz
        if (target != null && !mc.theWorld.loadedEntityList.contains(target)) {
            Yuri.INSTANCE.getEventBus().post(new KillEvent(target));
            target = null;
        }
    };

    @EventHook
    public void onPlayerAttack(PlayerAttackEvent event) {
        event.target = target;
    }

    private void selectTarget() {
        if (targetList.isEmpty()) {
            target = null;
            return;
        }

        if (mode.equals(Mode.SINGLE)) {
            target = (EntityLivingBase) targetList.get(0);
        } else if (mode.equals(Mode.SWITCH)) {
            if (targetIndex >= targetList.size()) {
                targetIndex = 0;
            }

            if (switchTimer.hasTimeElapsed(switchTime * 100)) {
                targetIndex = (targetIndex + 1) % targetList.size();
                switchTimer.reset();
            }
            target = (EntityLivingBase) targetList.get(targetIndex);
        } else if (mode.equals(Mode.ADAPTIVE)) {
            target = (EntityLivingBase) targetList.stream()
                    .min(Comparator.comparingDouble(e -> mc.thePlayer.getDistanceToEntity(e)))
                    .orElse(null);
        } else {
            throw new IllegalStateException("Unexpected value: " + this.mode);
        }
    }

    private List<Entity> getTargets() {
        return mc.theWorld.loadedEntityList.stream()
                .filter(entity -> entity instanceof EntityLivingBase)
                .filter(entity -> entity != mc.thePlayer)
                .filter(entity -> !entity.isDead)
                .filter(entity -> ((EntityLivingBase) entity).getHealth() > 0)
                .filter(entity -> mc.thePlayer.getDistanceToEntity(entity) <= seekRange)
                .filter(this::isValidEntity)
                .collect(Collectors.toList());
    }

    private boolean isValidEntity(Entity entity) {
        boolean teammate = entity instanceof EntityPlayer && inTeam(mc.thePlayer, entity);
        if (targets.contains(Targets.PLAYERS) && entity instanceof EntityPlayer) return true;
        if (targets.contains(Targets.HOSTILES) && entity instanceof EntityMob) return true;
        if (targets.contains(Targets.ANIMALS) && entity instanceof EntityAnimal) return true;
        if (targets.contains(Targets.INVISIBLES) && entity.isInvisible()) return true;
        if (targets.contains(Targets.TEAMMATES) && teammate) return true;

        return false;
    }

    private static boolean inTeam(@NonNull ICommandSender entity0, @NonNull ICommandSender entity1) {
        String s = "\u00a7" + teamColor(entity0);

        return entity0.getDisplayName().getFormattedText().contains(s)
                && entity1.getDisplayName().getFormattedText().contains(s);
    }

    private static @NonNull String teamColor(@NonNull ICommandSender player) {
        Matcher matcher = Pattern.compile("\u00a7(.).*\u00a7r").matcher(player.getDisplayName().getFormattedText());
        return matcher.find() ? matcher.group(1) : "f";
    }
}