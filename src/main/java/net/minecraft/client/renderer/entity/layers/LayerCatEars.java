package net.minecraft.client.renderer.entity.layers;

import ddlc.yuri.Yuri;
import ddlc.yuri.modules.impl.render.CatEarsModule;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.RenderPlayer;

public class LayerCatEars implements LayerRenderer<AbstractClientPlayer> {
    private final RenderPlayer playerRenderer;

    public LayerCatEars(RenderPlayer playerRendererIn) {
        this.playerRenderer = playerRendererIn;
    }

    public void doRenderLayer(AbstractClientPlayer entitylivingbaseIn, float p_177141_2_, float p_177141_3_, float partialTicks, float p_177141_5_, float p_177141_6_, float p_177141_7_, float scale) {
        CatEarsModule catEars = Yuri.INSTANCE.getModuleManager().getModule(CatEarsModule.class);

        if (catEars != null && catEars.isEnabled()) {
            catEars.renderEars(entitylivingbaseIn, this.playerRenderer.getMainModel().bipedHead, partialTicks);
        }
    }

    public boolean shouldCombineTextures() {
        return false;
    }
}
