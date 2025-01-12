package org.figuramc.figura.mixin.client.entity_render;

import com.mojang.blaze3d.vertex.PoseStack;
import org.figuramc.figura.util.DeferredVanillaPartRenderQueue;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {

    @Inject(method = "renderHandsWithItems", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;endBatch()V"))
    private void flushRenderQueueAfterHands(float tickDelta, PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, LocalPlayer localPlayer, int i, CallbackInfo ci) {
        // Flush the queue here, BEFORE we end batch. The vanilla hand might be rendered, and we need to flush now before the global state
        // corresponding to "first-person hand rendering" is undone.
        DeferredVanillaPartRenderQueue.flush(bufferSource, tickDelta);
    }

}
