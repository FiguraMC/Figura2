package org.figuramc.figura.mixin.client.vanilla_parts.tracking;

import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.server.packs.resources.ResourceManager;
import org.figuramc.figura.manage.AvatarManager;
import org.figuramc.figura.manage.AvatarSubManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * After the entity render dispatcher updates its entity type maps,
 * we'll clear caches on the ModelPartTracker and update vanilla roots
 * for avatars.
 */
@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {
    @Inject(method = "onResourceManagerReload", at = @At("RETURN"))
    public void afterRendererMapsChange(ResourceManager resourceManager, CallbackInfo ci) {
        // Clear all avatars. TODO find a better place to put this code than the entity render dispatcher
        AvatarManager.forEachSubManager(AvatarSubManager::clear);
    }
}
