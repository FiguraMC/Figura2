package org.figuramc.figura.mixin.client.entity_render;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.figuramc.figura.FiguraModClient;
import org.figuramc.figura.avatars.Avatar;
import org.figuramc.figura.manage.AvatarManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.UUID;

/**
 * - Manages the Avatar rendering stack, for vanilla parts usage
 */
@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {

    // x, y, z are the entity's position in world space relative to the camera.
    // Not relevant to the mixin, just felt like explaining it.
    @WrapMethod(method = "render(Lnet/minecraft/world/entity/Entity;DDDFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V")
    public void pushPopAvatar(Entity entity, double x, double y, double z, float tickDelta, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, Operation<Void> original) {
        // Push the avatar before rendering, and pop afterward.
        Avatar<UUID> avatar = AvatarManager.ENTITY_AVATARS.get(entity.getUUID());
        FiguraModClient.AVATAR_RENDERING_STACK.push(avatar);
        try {
            original.call(entity, x, y, z, tickDelta, poseStack, multiBufferSource, i);
        } catch (Throwable any) {
            if (FiguraModClient.AVATAR_RENDERING_STACK.pop() != avatar) // Ensure pop always occurs, and ensure same element was pushed as popped
                throw new IllegalStateException("Illegal Avatar rendering stack manipulation - either a bug in Figura, or a compat issue!");
            throw any;
        }
        if (FiguraModClient.AVATAR_RENDERING_STACK.pop() != avatar) // Ensure pop always occurs, and ensure same element was pushed as popped
            throw new IllegalStateException("Illegal Avatar rendering stack manipulation - either a bug in Figura, or a compat issue!");
    }

}
