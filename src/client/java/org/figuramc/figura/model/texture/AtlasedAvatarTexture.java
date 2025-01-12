package org.figuramc.figura.model.texture;

import org.figuramc.figura.avatars.components.Textures;
import org.figuramc.figura.data.AvatarMaterials;
import org.figuramc.figura.manage.AvatarLoadingException;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector4f;

/**
 * An avatar texture that is backed by a FiguraTextureAtlas.
 */
public class AtlasedAvatarTexture extends AvatarTexture {

    // Keep track of where in the atlas we're located
    private final FiguraTextureAtlas.TextureRectangle locationInAtlas;
    // Keep a reference to the atlas through the texture component
    private final Textures texturesComponent;

    public AtlasedAvatarTexture(Textures texturesComponent, AvatarMaterials.TextureMaterials.OwnedTexture materials, FiguraTextureAtlas.Builder atlasBuilder) throws AvatarLoadingException {
        this.texturesComponent = texturesComponent;
        this.locationInAtlas = atlasBuilder.insert(materials.data());
    }

    @Override public void upload() {}
    @Override public void destroy() {}
    @Override public ResourceLocation getLocation() { return this.texturesComponent.atlas.location; }
    @Override public Vector4f getUvValues() {
        float atlasWidth = (float) texturesComponent.atlas.width;
        float atlasHeight = (float) texturesComponent.atlas.height;
        return new Vector4f(
                locationInAtlas.getX() / atlasWidth,
                locationInAtlas.getY() / atlasHeight,
                locationInAtlas.getWidth() / atlasWidth,
                locationInAtlas.getHeight() / atlasHeight
        );
    }
    @Override public int getWidth() { return this.locationInAtlas.getWidth(); }
    @Override public int getHeight() { return this.locationInAtlas.getHeight(); }
    @Override public int getPixelRGBA(int x, int y) { return texturesComponent.atlas.image.getPixelABGR(this.locationInAtlas.getX() + x, this .locationInAtlas.getY() + y); }

}
