package org.figuramc.figura.mixin.client.entity_render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.figuramc.figura.avatars.Avatar;
import org.figuramc.figura.avatars.components.EntityRoot;
import org.figuramc.figura.ducks.client.EntityRenderStateAccess;
import org.figuramc.figura.manage.AvatarManager;
import org.figuramc.figura.manage.CemManager;
import org.figuramc.figura.util.FiguraTransformStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {

    /**
     * Inject after calling getRenderType.
     * This will have all the necessary transforms of the model applied,
     * but it will also have some ones we don't want:
     * - Flipping the x and y axes. This is not necessary in our renderer,
     *   we try to keep it consistent with Blockbench.
     * - Translating vertically by 1.501.
     *   We choose to place (0,0,0) at the feet of the entity, rather than
     *   the neck of the entity like Minecraft does.
     * Both of these will need to be reverted.
     */
    @SuppressWarnings("UnreachableCode")
    @Inject(
            method = "render(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;getRenderType(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;ZZZ)Lnet/minecraft/client/renderer/RenderType;")
    )
    public void onRenderLivingEntity(LivingEntityRenderState renderState, PoseStack poseStack, MultiBufferSource multiBufferSource, int light, CallbackInfo ci) {
        // Fetch entity
        LivingEntity livingEntity = (LivingEntity) ((EntityRenderStateAccess) renderState).figura$getEntity();
        Avatar<UUID> avatar = AvatarManager.ENTITY_AVATARS.get(livingEntity.getUUID());
        if (avatar == null)  { CemManager.tryGetCem(livingEntity); return; }
        EntityRoot root = avatar.getComponent(EntityRoot.class);
        if (root == null) return;
        FiguraTransformStack matrixStack = new FiguraTransformStack(poseStack);
        // Undo the problematic translations above:
        // This has to be 1.500 exactly. NOT 1.501.
        // I have not been able to figure out why,
        // even though I have probably stared at the
        // vanilla source code for entity rendering for weeks in total.
        matrixStack.translate(0, 1.500f, 0);
        matrixStack.scale(-1, -1, 1);
        // Grab the overlay:
        float whiteOverlayProgress = ((LivingEntityRenderer) (Object) this).getWhiteOverlayProgress(renderState);
        int overlayCoords = LivingEntityRenderer.getOverlayCoords(renderState, whiteOverlayProgress);
        // Render
        float tickDelta = ((EntityRenderStateAccess) renderState).figura$getTickDelta();
        root.render(avatar, tickDelta, multiBufferSource, matrixStack, light, overlayCoords);
    }
}
