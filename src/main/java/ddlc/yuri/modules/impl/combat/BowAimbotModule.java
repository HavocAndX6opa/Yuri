package ddlc.yuri.modules.impl.combat;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.api.events.impl.render.Render2DEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.managers.impl.RotationManager;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.modules.impl.misc.AntiBotModule;
import ddlc.yuri.utils.client.MathUtils;
import ddlc.yuri.utils.player.FriendUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.IAnimals;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBow;
import net.minecraft.util.MathHelper;

import java.util.ArrayList;
import java.util.Comparator;

@ModuleInfo(label = "Bow Aimbot", category = ModuleCategory.COMBAT, description = "Automatically aims your bow at targets")
public final class BowAimbotModule extends Module {

    // the one thing nazuna actually made that was alright. so yes I did port this from old euphoria. no credit to nazuna, fuck him. -lumie

    private final ModeProperty<TargetMode> mode = new ModeProperty<>("Mode", TargetMode.DISTANCE);
    private final NumberProperty range = new NumberProperty("Range", 32, 0, 256, 1);
    private final Property<Boolean> lockView = new Property<Boolean>("Lock View", true);
    private final NumberProperty minRotSpeed = new NumberProperty("Min Rotation Speed", 3, 0, 10, 0.5f, () -> !lockView.getValue());
    private final NumberProperty maxRotSpeed = new NumberProperty("Max Rotation Speed", 7, 0, 10, 0.5f, () -> !lockView.getValue());
    private final Property<Boolean> monsters = new Property<Boolean>("Monsters", false);
    private final Property<Boolean> players = new Property<Boolean>("Players", true);
    private final Property<Boolean> animals = new Property<Boolean>("Animals", false);
    private final Property<Boolean> invisibles = new Property<Boolean>("Invisible", false);

    private final ArrayList<Entity> attackList = new ArrayList<>();

    private int currentTarget;

    public enum TargetMode {
        HEALTH("Health"),
        DISTANCE("Distance");

        private final String name;

        TargetMode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        attackList.clear();
        currentTarget = 0;
    }

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {

        attackList.clear();

        for (Object obj : mc.theWorld.loadedEntityList) {
            Entity entity = (Entity) obj;
            if (isValidTarget(entity)) {
                attackList.add(entity);
            }
        }

        if (attackList.isEmpty() || !mc.thePlayer.isUsingItem() || mc.thePlayer.getCurrentEquippedItem() == null || !(mc.thePlayer.getCurrentEquippedItem().getItem() instanceof ItemBow)) {
            setSuffix(mode.getValue().toString());
            return;
        }

        if (!mc.thePlayer.isUsingItem()) {
            return;
        }

        if (mc.thePlayer.getCurrentEquippedItem() == null) {
            return;
        }

        if (!(mc.thePlayer.getCurrentEquippedItem().getItem() instanceof ItemBow)) {
            return;
        }

        sortTargets();
        currentTarget = Math.min(currentTarget, attackList.size() - 1);
        Entity target = attackList.get(currentTarget);
        setSuffix(target.getName()); ;

        int bowCurrentCharge = mc.thePlayer.getItemInUseDuration();

        float bowVelocity = bowCurrentCharge / 20.0F;
        bowVelocity = (bowVelocity * bowVelocity + bowVelocity * 2.0F) / 3.0F;
        bowVelocity = MathHelper.clamp_float(bowVelocity, 0.0F, 1.0F);

        if (bowVelocity < 0.1F) {
            return;
        }

        double v = bowVelocity * 3.0F;
        double g = 0.05000000074505806D;

        EntityLivingBase livingTarget = (EntityLivingBase) target;

        double xDistance = target.posX - mc.thePlayer.posX
                + (target.posX - target.lastTickPosX)
                * (bowVelocity * 10.0F);

        double zDistance = target.posZ - mc.thePlayer.posZ
                + (target.posZ - target.lastTickPosZ)
                * (bowVelocity * 10.0F);

        float yaw = (float) Math.toDegrees(Math.atan2(zDistance, xDistance)) - 90.0F;
        float pitch = (float) -Math.toDegrees(getLaunchAngle(livingTarget, v, g)) - 3.0F;

        float rotSpeed = (float) MathUtils.getRandom(
                minRotSpeed.getValue(),
                maxRotSpeed.getValue()
        );

        if (!lockView.getValue()) {
            RotationManager.setRotations(
                    yaw, pitch, rotSpeed,
                    RotationManager.MovementFix.NORMAL
            );
        }
    }

    @EventHook
    public void onRender2D(Render2DEvent event) {
        if (attackList.isEmpty()) {
            return;
        }

        Entity target = attackList.get(currentTarget);

        if (!mc.thePlayer.isUsingItem()) {
            return;
        }

        if (mc.thePlayer.getCurrentEquippedItem() == null) {
            return;
        }

        if (!(mc.thePlayer.getCurrentEquippedItem().getItem() instanceof ItemBow)) {
            return;
        }

        int bowCurrentCharge = mc.thePlayer.getItemInUseDuration();

        float bowVelocity = bowCurrentCharge / 20.0F;
        bowVelocity = (bowVelocity * bowVelocity + bowVelocity * 2.0F) / 3.0F;
        bowVelocity = MathHelper.clamp_float(bowVelocity, 0.0F, 1.0F);

        if (bowVelocity < 0.1F) {
            return;
        }

        double v = bowVelocity * 3.0F;
        double g = 0.05000000074505806D;

        EntityLivingBase livingTarget = (EntityLivingBase) target;

        double xDistance = target.posX - mc.thePlayer.posX
                + (target.posX - target.lastTickPosX)
                * (bowVelocity * 10.0F);

        double zDistance = target.posZ - mc.thePlayer.posZ
                + (target.posZ - target.lastTickPosZ)
                * (bowVelocity * 10.0F);

        float yaw = (float) Math.toDegrees(Math.atan2(zDistance, xDistance)) - 90.0F;
        float pitch = (float) -Math.toDegrees(getLaunchAngle(livingTarget, v, g)) - 3.0F;

        if (lockView.getValue()) {
            mc.thePlayer.rotationYaw = yaw;
            mc.thePlayer.rotationPitch = pitch;
        }
    }

    public boolean isValidTarget(Entity entity) {
        if (entity == null) {
            return false;
        }

        if (entity == mc.thePlayer) {
            return false;
        }

        if (entity == mc.thePlayer.ridingEntity) {
            return false;
        }

        if (!entity.isEntityAlive()) {
            return false;
        }

        if (mc.thePlayer.getDistanceToEntity(entity) > range.getValue()) {
            return false;
        }

        if (entity.isInvisible() && !invisibles.getValue()) {
            return false;
        }

        if (entity instanceof EntityPlayer) {

            if (!players.getValue()) {
                return false;
            }

            return !FriendUtils.isFriend(entity.getName()) && !Yuri.INSTANCE.getModuleManager().getModule(AntiBotModule.class).isBot((EntityPlayer) entity);
        }

        if (entity instanceof IMob) {
            return monsters.getValue();
        }

        if (entity instanceof IAnimals && !(entity instanceof IMob)) {
            return animals.getValue();
        }

        return false;
    }

    public void sortTargets() {
        switch (mode.getValue()) {

            case DISTANCE:
                attackList.sort(Comparator.comparingDouble(ent -> mc.thePlayer.getDistanceToEntity(ent)));
                break;

            case HEALTH:
                attackList.sort((ent1, ent2) -> Float.compare(
                        ((EntityLivingBase) ent1).getHealth(),
                        ((EntityLivingBase) ent2).getHealth()
                ));
                break;
        }
    }

    private float getLaunchAngle(EntityLivingBase targetEntity, double v, double g) {

        double yDif = targetEntity.posY + targetEntity.getEyeHeight() / 2.0F
                - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());

        double xDif = targetEntity.posX - mc.thePlayer.posX;
        double zDif = targetEntity.posZ - mc.thePlayer.posZ;

        double xCoord = Math.sqrt(xDif * xDif + zDif * zDif);

        return theta(v + 2, g, xCoord, yDif);
    }

    private float theta(double v, double g, double x, double y) {
        double yv = 2.0D * y * (v * v);
        double gx = g * (x * x);
        double g2 = g * (gx + yv);
        double insqrt = v * v * v * v - g2;

        if (insqrt < 0) {
            return 0;
        }

        double sqrt = Math.sqrt(insqrt);

        double numerator = v * v + sqrt;
        double numerator2 = v * v - sqrt;

        double atan1 = Math.atan2(numerator, g * x);
        double atan2 = Math.atan2(numerator2, g * x);

        return (float) Math.min(atan1, atan2);
    }
}