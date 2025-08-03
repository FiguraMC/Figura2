package org.figuramc.figura.mixin.client;

import net.minecraft.client.Minecraft;
import org.figuramc.figura.manage.AvatarManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Inject(method = "tick", at = @At("RETURN"))
    private void endOfTick(CallbackInfo ci) {
        // Tick the things that need ticking
        AvatarManager.tick();
    }

}
