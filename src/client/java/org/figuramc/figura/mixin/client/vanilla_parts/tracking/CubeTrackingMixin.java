package org.figuramc.figura.mixin.client.vanilla_parts.tracking;

import org.figuramc.figura.ducks.client.CubeTrackingAccess;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@SuppressWarnings("rawtypes")
@Mixin(ModelPart.Cube.class)
public class CubeTrackingMixin implements CubeTrackingAccess {

    // Add fields to track the passed "texture size" of the cube.
    @Unique
    private int textureWidth, textureHeight;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void setTextureSizeProbably(int i, int j, float f, float g, float h, float k, float l, float m, float n, float o, float p, boolean bl, float texWidthTimesScale, float texHeightTimesScale, Set set, CallbackInfo ci) {
        // Attempt to set the texture width and height to the passed values.
        // This works as long as the cube's "tex scale" is 1.0 in both directions.

        // This assumption holds for all vanilla parts except the player cloak part.
        // This particular exception, the cloak, is handled by CubeDefinitionMixin.
        // Together, these two mixins should be able to deal with everything vanilla
        // parts use, and hopefully most of the things that modded parts will use.
        textureWidth = (int) texWidthTimesScale;
        textureHeight = (int) texHeightTimesScale;
    }

    // And accessors.
    @Override
    public int figura$getTextureWidth() {
        return textureWidth;
    }
    @Override
    public int figura$getTextureHeight() {
        return textureHeight;
    }
    @Override
    public void figura$setTextureWidth(int width) {
        textureWidth = width;
    }
    @Override
    public void figura$setTextureHeight(int height) {
        textureHeight = height;
    }

}
