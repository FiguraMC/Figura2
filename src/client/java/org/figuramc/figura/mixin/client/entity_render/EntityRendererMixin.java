package org.figuramc.figura.mixin.client.entity_render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.figuramc.figura.avatars.Avatar;
import org.figuramc.figura.avatars.components.EntityRoot;
import org.figuramc.figura.ducks.client.EntityRenderStateAccess;
import org.figuramc.figura.manage.AvatarManager;
import org.figuramc.figura.manage.CemManager;
import org.figuramc.figura.util.FiguraMatrixStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin {

    @Inject(method = "render", at = @At("HEAD"))
    public void onRender(EntityRenderState renderState, PoseStack poseStack, MultiBufferSource multiBufferSource, int light, CallbackInfo ci) {
        // If this is a living entity renderer, then the more specific mixin will already have been run.
        // So this one is unnecessary.
        if (((Object) this) instanceof LivingEntityRenderer<?,?,?>)
            return;

        // Otherwise, render the Avatar's entity root if it exists.
        Entity entity = ((EntityRenderStateAccess) renderState).figura$getEntity();
        Avatar<UUID> avatar = AvatarManager.ENTITY_AVATARS.get(entity.getUUID());
        if (avatar == null) { CemManager.tryGetCem(entity); return; }
        EntityRoot root = avatar.getComponent(EntityRoot.class);
        if (root == null) return;
        float tickDelta = ((EntityRenderStateAccess) renderState).figura$getTickDelta();
        root.render(avatar, tickDelta, multiBufferSource, new FiguraMatrixStack(poseStack), light, OverlayTexture.NO_OVERLAY);
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    public void extractEntityInstance(Entity entity, EntityRenderState entityRenderState, float tickDelta, CallbackInfo ci) {
        ((EntityRenderStateAccess) entityRenderState).figura$setEntity(entity);
        ((EntityRenderStateAccess) entityRenderState).figura$setTickDelta(tickDelta);
    }
}