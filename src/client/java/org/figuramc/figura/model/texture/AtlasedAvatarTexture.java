package org.figuramc.figura.model.texture;

import net.minecraft.resources.ResourceLocation;
import org.figuramc.figura.avatars.AvatarError;
import org.figuramc.figura.avatars.components.Textures;
import org.figuramc.figura.data.ModuleMaterials;
import org.joml.Vector4f;

import java.util.concurrent.CompletableFuture;

/**
 * An avatar texture that is backed by a FiguraTextureAtlas.
 * The atlas must therefore exist, so we assert it at compile time.
 */
public class AtlasedAvatarTexture extends AvatarTexture {

    // Keep track of where in the atlas we're located
    private final FiguraTextureAtlas.TextureRectangle locationInAtlas;
    // Keep a reference to the atlas through the texture component
    private final Textures texturesComponent;

    public AtlasedAvatarTexture(Textures texturesComponent, ModuleMaterials.TextureMaterials.OwnedTexture materials, FiguraTextureAtlas.Builder atlasBuilder) throws AvatarError {
        this.texturesComponent = texturesComponent;
        this.locationInAtlas = atlasBuilder.insert(materials.name(), materials.data());
    }

    @Override public CompletableFuture<Void> upload() { return CompletableFuture.completedFuture(null); }
    @Override public CompletableFuture<Void> destroy() { return CompletableFuture.completedFuture(null); }
    @Override public ResourceLocation getLocation() {
        assert texturesComponent.atlas != null;
        return this.texturesComponent.atlas.location;
    }
    @Override public Vector4f getUvValues() {
        assert texturesComponent.atlas != null;
        float atlasWidth = (float) texturesComponent.atlas.backingTexture.getPixels().getWidth();
        float atlasHeight = (float) texturesComponent.atlas.backingTexture.getPixels().getHeight();
        return new Vector4f(
                locationInAtlas.getX() / atlasWidth,
                locationInAtlas.getY() / atlasHeight,
                locationInAtlas.getWidth() / atlasWidth,
                locationInAtlas.getHeight() / atlasHeight
        );
    }
    @Override public int getWidth() { return this.locationInAtlas.getWidth(); }
    @Override public int getHeight() { return this.locationInAtlas.getHeight(); }
    @Override public int getPixel(int x, int y) {
        assert texturesComponent.atlas != null;
        return texturesComponent.atlas.backingTexture.getPixels().getPixel(this.locationInAtlas.getX() + x, this .locationInAtlas.getY() + y);
    }

}
