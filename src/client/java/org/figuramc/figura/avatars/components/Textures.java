package org.figuramc.figura.avatars.components;

import org.figuramc.figura.avatars.AvatarComponent;
import org.figuramc.figura.avatars.AvatarError;
import org.figuramc.figura.avatars.AvatarModules;
import org.figuramc.figura.data.ModuleMaterials;
import org.figuramc.figura.model.texture.AvatarTexture;
import org.figuramc.figura.model.texture.FiguraTextureAtlas;
import org.figuramc.figura.script_hooks.mem_count.AllocationTracker;
import org.figuramc.figura.util.RenderUtils;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A component which holds all the textures for an Avatar.
 */
public class Textures implements AvatarComponent<Textures> {

    public static final Type<Textures> TYPE = new Type<>();
    public Type<Textures> getType() { return TYPE; }

    // The atlas texture. Textures which don't opt out of being atlased go in here to keep things more efficient.
    public final @Nullable FiguraTextureAtlas atlas;
    private final List<List<AvatarTexture>> textures; // textures[moduleIndex][textureIndex]

    public Textures(List<AvatarModules.LoadTimeModule> modules, @Nullable AllocationTracker allocationTracker) throws AvatarError {
        FiguraTextureAtlas.Builder atlasBuilder = FiguraTextureAtlas.builder();
        textures = new ArrayList<>();
        for (var module : modules) {
            ArrayList<AvatarTexture> moduleTextures = new ArrayList<>();
            for (ModuleMaterials.TextureMaterials mats : module.materials.textures())
                moduleTextures.add(AvatarTexture.from(mats, allocationTracker, this, atlasBuilder));
            textures.add(moduleTextures);
        }
        atlas = atlasBuilder.build(allocationTracker);
    }

    public AvatarTexture getTexture(int moduleIndex, int textureIndex) {
        return textures.get(moduleIndex).get(textureIndex);
    }

    @Override
    public void destroy() {
        RenderUtils.runOnRenderThread(() -> {
            if (atlas != null) atlas.destroy();
            for (var moduleTextures : textures)
                for (var texture : moduleTextures)
                    texture.destroy();
            textures.clear();
        });
    }

}
