package org.figuramc.figura.model.texture;

import org.figuramc.figura.avatars.components.Textures;
import org.figuramc.figura.data.AvatarMaterials;
import org.figuramc.figura.manage.AvatarLoadingException;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector4f;

/**
 * A generic avatar texture as seen from a script.
 * Subclassed by "Standalone", "Atlased", and "Vanilla" variations.
 */
public abstract class AvatarTexture {

    public static AvatarTexture from(Textures textureComponent, AvatarMaterials.TextureMaterials materials, FiguraTextureAtlas.Builder atlasBuilder) throws AvatarLoadingException {
        switch (materials) {
            case AvatarMaterials.TextureMaterials.OwnedTexture owned -> {
                if (owned.noAtlas()) {
                    return new StandaloneAvatarTexture(owned);
                } else {
                    return new AtlasedAvatarTexture(textureComponent, owned, atlasBuilder);
                }
            }
            case AvatarMaterials.TextureMaterials.VanillaTexture vanilla -> {
                return new VanillaAvatarTexture(vanilla);
            }
        }
    }

    // Begin uploading the texture using `RenderUtils.executeOnRenderThread(...)`.
    public abstract void upload();
    // Ensure that any backing native resources are closed.
    public abstract void destroy();
    // Get the ResourceLocation of a backing texture for this texture.
    public abstract ResourceLocation getLocation();
    // Get UV values that let a part use this texture.
    // x offset, y offset, x scale, y scale. All 0 to 1.
    public abstract Vector4f getUvValues();

    // Width/Height, and ability to get a pixel at a given position
    public abstract int getWidth();
    public abstract int getHeight();
    public abstract int getPixelRGBA(int x, int y); // x, y relative to texture position

}