package org.figuramc.figura.mixin.client.vanilla_parts.tracking;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;
import org.figuramc.figura.ducks.client.CubeTrackingAccess;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(ModelPart.Cube.class)
public class CubeTrackingMixin implements CubeTrackingAccess {

    // Add fields to track values of the cube.
    @Unique private final Vector2f textureSize = new Vector2f();
    @Unique private final Vector3f inflate = new Vector3f();
    @Unique private boolean mirrored;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void setTextureSizeProbably(int i, int j, float f, float g, float h, float k, float l, float m, float n, float o, float p, boolean bl, float texWidthTimesScale, float texHeightTimesScale, Set<Direction> set, CallbackInfo ci) {
        // Attempt to set the texture width and height to the passed values.
        // This works as long as the cube's "tex scale" is 1.0 in both directions.
        // This assumption holds for all vanilla parts except the player cape part and the arrow model.
        // These exceptions are handled by CubeDefinitionMixin.
        // Together, these two mixins should be able to deal with everything vanilla
        // parts use, and hopefully most of the things that modded parts will use.
        textureSize.set(texWidthTimesScale, texHeightTimesScale);
        // Also, store inflate values.
        inflate.set(n, o, p);
        // And mirroredness.
        mirrored = bl;
    }

    // And add accessors.
    @Override
    public Vector2f figura$getTextureSize() {
        return textureSize;
    }

    @Override
    public Vector3f figura$getInflate() {
        return inflate;
    }

    @Override
    public boolean figura$getMirrored() {
        return mirrored;
    }
}
