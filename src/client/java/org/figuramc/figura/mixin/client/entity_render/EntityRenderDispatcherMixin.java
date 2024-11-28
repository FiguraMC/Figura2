package org.figuramc.figura.mixin.client.entity_render;

import com.mojang.blaze3d.vertex.PoseStack;
import org.figuramc.figura.FiguraModClient;
import org.figuramc.figura.avatars.Avatar;
import org.figuramc.figura.manage.AvatarManager;
import org.figuramc.figura.util.DeferredVanillaPartRenderQueue;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * - Manages the Avatar rendering stack, for vanilla parts usage
 */
@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {

    // x, y, z are the entity's position in world space relative to the camera.
    // Not relevant to the mixin, just felt like explaining it.
    @Inject(method = "render", at = @At("HEAD"))
    public void pushAvatar(Entity entity, double x, double y, double z, float yaw, float tickDelta, PoseStack poseStack, MultiBufferSource multiBufferSource, int light, CallbackInfo ci) {
        Avatar<UUID> avatar = AvatarManager.ENTITY_AVATARS.get(entity.getUUID());
        if (avatar != null)
            FiguraModClient.AVATAR_RENDERING_STACK.push(avatar);
    }

    @Inject(method = "render", at = @At("RETURN"))
    public void popAvatar(Entity entity, double x, double y, double z, float yaw, float tickDelta, PoseStack poseStack, MultiBufferSource multiBufferSource, int light, CallbackInfo ci) {
        Avatar<UUID> avatar = AvatarManager.ENTITY_AVATARS.get(entity.getUUID());
        if (avatar != null)
            if (FiguraModClient.AVATAR_RENDERING_STACK.pop() != avatar)
                throw new IllegalStateException("Illegal Avatar rendering stack manipulation - either a bug in the mod, or a compat issue!");

        // Also, flush the deferred render queue!
        DeferredVanillaPartRenderQueue.flush(multiBufferSource);
    }

}
