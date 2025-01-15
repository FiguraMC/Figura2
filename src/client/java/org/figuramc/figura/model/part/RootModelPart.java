package org.figuramc.figura.model.part;

import org.figuramc.figura.data.AvatarMaterials;
import org.figuramc.figura.model.renderers.FiguraPartRenderer;
import org.figuramc.figura.model.texture.AvatarTexture;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Special model part that exists at the root only.
 * Is responsible for controlling the optimized rendering system.
 */
public class RootModelPart extends FiguraModelPart {

    // FiguraPartRenderer instances can store their necessary state for the part in here.
    public @Nullable FiguraPartRenderer.RootState rootRenderState;

    public RootModelPart(AvatarMaterials.ModelPartMaterials materials, List<AvatarTexture> textures) {
        super(materials, textures, null);
    }

    // Root model part by extruding a texture
    public RootModelPart(String name, AvatarTexture texture) {
        super(name, texture);
    }

    // Call before GC'ing this part, or else we may have a memory leak!
    public void destroy() {
        if (rootRenderState != null) rootRenderState.destroy();
    }

}
