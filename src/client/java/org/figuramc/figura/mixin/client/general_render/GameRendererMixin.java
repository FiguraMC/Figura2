package org.figuramc.figura.mixin.client.general_render;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.figuramc.figura.avatars.components.Scripts;
import org.figuramc.figura.manage.AvatarManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "render", at = @At("HEAD"))
    public void preRender(DeltaTracker deltaTracker, boolean bl, CallbackInfo ci) {
        // Run the render event on each avatar:
        float tickDelta = deltaTracker.getGameTimeDeltaPartialTick(true);
        AvatarManager.forEachAvatar(avatar -> {
            Scripts scripts;
            if ((scripts = avatar.getComponent(Scripts.class)) != null)
                scripts.renderEvent(tickDelta);
        });
    }

}
