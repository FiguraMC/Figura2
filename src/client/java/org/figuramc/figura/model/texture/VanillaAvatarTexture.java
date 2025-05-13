package org.figuramc.figura.model.texture;

import org.figuramc.figura.data.ModuleMaterials;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector4f;

import java.util.concurrent.CompletableFuture;

/**
 * An AvatarTexture that delegates to a vanilla texture.
 */
public class VanillaAvatarTexture extends AvatarTexture {

    private final ResourceLocation location;

    public VanillaAvatarTexture(ModuleMaterials.TextureMaterials.VanillaTexture vanillaTexture) {
        location = ResourceLocation.parse(vanillaTexture.resourceLocation());
    }

    @Override public CompletableFuture<Void> upload() { return CompletableFuture.completedFuture(null); }
    @Override public CompletableFuture<Void> destroy() { return CompletableFuture.completedFuture(null); }
    @Override public ResourceLocation getLocation() { return location; }
    @Override public Vector4f getUvValues() { return new Vector4f(0, 0, 1, 1); }
    @Override public int getWidth() { throw new UnsupportedOperationException("Not yet implemented"); }
    @Override public int getHeight() { throw new UnsupportedOperationException("Not yet implemented"); }
    @Override public int getPixel(int x, int y) { throw new UnsupportedOperationException("Not yet implemented"); }

}
