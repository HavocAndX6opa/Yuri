package ddlc.yuri.managers.impl;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static ddlc.yuri.utils.misc.IMinecraft.mc;

public class TargetManager {

    @Getter
    private static List<Entity> targetList = new CopyOnWriteArrayList<>();
    @Getter
    @Setter
    private static List<Enum> targets;
    @Getter
    @Setter
    private static boolean searching;

    public TargetManager() {
        targets = Arrays.asList(Targets.PLAYERS, Targets.HOSTILES, Targets.TEAMMATES, Targets.INVISIBLES);
        searching = false;
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

        public String toString() {
            return name;
        }
    }

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        if (searching) {
            targetList = getTargets();
        }
    }

    private List<Entity> getTargets() {
        return mc.theWorld.loadedEntityList.stream()
                .filter(entity -> entity instanceof EntityLivingBase)
                .filter(entity -> entity != mc.thePlayer)
                .filter(entity -> !entity.isDead)
                .filter(entity -> ((EntityLivingBase) entity).getHealth() > 0)
                .filter(entity -> mc.thePlayer.getDistanceToEntity(entity) <= 6.0f)
                .filter(this::isValidEntity)
                .collect(Collectors.toList());
    }

    private boolean isValidEntity(Entity entity) {
        if (!targets.contains(Targets.TEAMMATES) && inTeam(mc.thePlayer, entity)) return false;

        if (targets.contains(Targets.PLAYERS) && entity instanceof EntityPlayer) return true;
        if (targets.contains(Targets.HOSTILES) && entity instanceof EntityMob) return true;
        if (targets.contains(Targets.ANIMALS) && entity instanceof EntityAnimal) return true;
        if (targets.contains(Targets.INVISIBLES) && entity.isInvisible()) return true;

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
