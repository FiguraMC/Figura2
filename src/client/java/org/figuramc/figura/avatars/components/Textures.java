package org.figuramc.figura.avatars.components;

import org.figuramc.figura.avatars.Avatar;
import org.figuramc.figura.avatars.AvatarComponent;
import org.figuramc.figura.data.AvatarMaterials;
import org.figuramc.figura.manage.AvatarLoadingException;
import org.figuramc.figura.model.texture.AvatarTexture;
import org.figuramc.figura.model.texture.FiguraTextureAtlas;
import org.figuramc.figura.util.ListUtils;
import org.figuramc.figura.util.RenderUtils;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A component which holds all the textures for an Avatar.
 */
public class Textures implements AvatarComponent {

    // The atlas texture. Textures which don't opt out of being atlased go in here to keep things more efficient.
    public @Nullable FiguraTextureAtlas atlas;
    public List<AvatarTexture> textures;
    private boolean isReadyAsync = false;

    @Override
    public void initialize(AvatarMaterials materials, Avatar<?> self) throws AvatarLoadingException {

        // Create all the textures:
        FiguraTextureAtlas.Builder atlasBuilder = FiguraTextureAtlas.builder();
        textures = ListUtils.map(materials.textures(), mats -> AvatarTexture.from(this, mats, atlasBuilder));
        atlas = atlasBuilder.build();

        // Upload all textures
        if (atlas != null) atlas.upload();
        for (AvatarTexture texture : textures) texture.upload();
        // Once all textures are uploaded, mark as ready
        RenderUtils.executeOnRenderThread(() -> isReadyAsync = true);
    }

    @Override
    public boolean isReadyAsync() { return isReadyAsync; }
    @Override
    public void destroy() {
        RenderUtils.executeOnRenderThread(() -> {
            if (atlas != null) atlas.destroy();
            for (AvatarTexture texture : textures) texture.destroy();
        });
    }

}
