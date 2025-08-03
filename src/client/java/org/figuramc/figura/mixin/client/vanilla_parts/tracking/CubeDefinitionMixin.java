package org.figuramc.figura.mixin.client.vanilla_parts.tracking;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDefinition;
import org.figuramc.figura.ducks.client.CubeTrackingAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CubeDefinition.class)
public class CubeDefinitionMixin {

    // Ensure the texture width/height are set to the actual values whenever possible,
    // instead of being multiplied by the "texture scale" values.

    // This may not always work if other mods decide to create Cube objects without
    // using the bake() method, but it's the best we have.
    @ModifyReturnValue(method = "bake", at = @At("RETURN"))
    public ModelPart.Cube ensureTextureSize(ModelPart.Cube cube, int trueTextureWidth, int trueTextureHeight) {
        ((CubeTrackingAccess) cube).figura$getTextureSize().set(trueTextureWidth, trueTextureHeight);
        return cube;
    }
}
