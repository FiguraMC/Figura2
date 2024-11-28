package org.figuramc.figura.mixin.client.general_render;

import org.figuramc.figura.FiguraModClient;
import org.figuramc.figura.avatars.components.Scripts;
import org.figuramc.figura.manage.AvatarManager;
import org.figuramc.figura.util.ClientUtils;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "render", at = @At("HEAD"))
    public void preRender(DeltaTracker deltaTracker, boolean bl, CallbackInfo ci) {
        // TODO: Better solution than this for GUI rendering
        FiguraModClient.AVATAR_RENDERING_STACK.push(AvatarManager.ENTITY_AVATARS.get(ClientUtils.getLocalUUID()));

        // Run the render() event on each avatar:
        float tickDelta = deltaTracker.getGameTimeDeltaPartialTick(true);
        AvatarManager.forEachAvatar(avatar -> {
            Scripts scripts;
            if ((scripts = avatar.getComponent(Scripts.class)) != null)
                scripts.renderEvent(tickDelta);
        });
    }

    @Inject(method = "render", at = @At("RETURN"))
    public void postRender(DeltaTracker deltaTracker, boolean bl, CallbackInfo ci) {
        FiguraModClient.AVATAR_RENDERING_STACK.pop();
    }
}
