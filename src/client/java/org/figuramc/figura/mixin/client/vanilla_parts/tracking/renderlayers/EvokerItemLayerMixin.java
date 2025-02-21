package org.figuramc.figura.mixin.client.vanilla_parts.tracking.renderlayers;

import net.minecraft.client.renderer.entity.EvokerRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import org.figuramc.figura.vanillamodel.RenderLayerAliases;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings({"unchecked", "rawtypes"})
@Mixin(targets = "net.minecraft.client.renderer.entity.EvokerRenderer$1")
public class EvokerItemLayerMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    public void registerToTracker(EvokerRenderer evokerRenderer, RenderLayerParent renderLayerParent, CallbackInfo ci) {
        RenderLayerAliases.RENDER_LAYER_ALIASES.put((Class<? extends RenderLayer>) (Object) this.getClass(), "EvokerItem");
    }
}
