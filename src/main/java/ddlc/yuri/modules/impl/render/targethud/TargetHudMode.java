package ddlc.yuri.modules.impl.render.targethud;

import ddlc.yuri.modules.impl.render.TargetHudModule;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;

public abstract class TargetHudMode {
    protected final Minecraft mc = Minecraft.getMinecraft();
    @Getter
    private final String name;

    public TargetHudMode(String name) {
        this.name = name;
    }

    public abstract int getMinWidth();
    public abstract int getHudHeight();
    public abstract int getLabelHeight();

    public abstract void draw(EntityLivingBase targetEntity, TargetHudModule.TargetState state,
                              double x, double y, long now, float delta);
}