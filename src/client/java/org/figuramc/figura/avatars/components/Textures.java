package org.figuramc.figura.avatars.components;

import org.figuramc.figura.avatars.AvatarComponent;
import org.figuramc.figura.avatars.AvatarModules;
import org.figuramc.figura.data.ModuleMaterials;
import org.figuramc.figura.manage.AvatarLoadingException;
import org.figuramc.figura.model.texture.AvatarTexture;
import org.figuramc.figura.model.texture.FiguraTextureAtlas;
import org.figuramc.figura.util.RenderUtils;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A component which holds all the textures for an Avatar.
 */
public class Textures implements AvatarComponent {

    public static final int ID = AvatarComponent.createId();
    public int getId() { return ID; }

    // The atlas texture. Textures which don't opt out of being atlased go in here to keep things more efficient.
    public final @Nullable FiguraTextureAtlas atlas;
    private final List<AvatarTexture> textures;
    private final int[] indexOffsetsByModule;

    public Textures(AvatarModules modules) throws AvatarLoadingException {
        FiguraTextureAtlas.Builder atlasBuilder = FiguraTextureAtlas.builder();
        textures = new ArrayList<>();
        indexOffsetsByModule = new int[modules.modules.size()];
        for (int i = 0; i < modules.modules.size(); i++) {
            AvatarModules.Module module = modules.modules.get(i);
            indexOffsetsByModule[i] = textures.size();
            for (ModuleMaterials.TextureMaterials mats : module.materials.textures())
                textures.add(AvatarTexture.from(this, mats, atlasBuilder));
        }
        atlas = atlasBuilder.build();
    }

    public AvatarTexture getTexture(int moduleIndex, int textureIndex) {
        return textures.get(indexOffsetsByModule[moduleIndex] + textureIndex);
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
