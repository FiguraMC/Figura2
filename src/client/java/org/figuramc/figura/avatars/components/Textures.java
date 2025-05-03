package org.figuramc.figura.avatars.components;

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

    public static final int ID = AvatarComponent.createId();
    public int getId() { return ID; }

    // The atlas texture. Textures which don't opt out of being atlased go in here to keep things more efficient.
    public final @Nullable FiguraTextureAtlas atlas;
    public final List<AvatarTexture> textures;

    public Textures(AvatarMaterials materials) throws AvatarLoadingException {
        FiguraTextureAtlas.Builder atlasBuilder = FiguraTextureAtlas.builder();
        textures = ListUtils.map(materials.textures(), mats -> AvatarTexture.from(this, mats, atlasBuilder));
        atlas = atlasBuilder.build();
    }

    @Override
    public void destroy() {
        RenderUtils.runOnRenderThread(() -> {
            if (atlas != null) atlas.destroy();
            for (AvatarTexture texture : textures) texture.destroy();
            textures.clear();
        });
    }

}
