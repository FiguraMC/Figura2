package org.figuramc.figura.model.part;

import org.figuramc.figura.avatars.components.VanillaRendering;
import org.figuramc.figura.data.AvatarMaterials;
import org.figuramc.figura.model.texture.AvatarTexture;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import java.util.List;

/**
 * Has a double-precision offset value, so it can be placed in the world at high coordinates.
 */
public class WorldRootedModelPart extends FiguraModelPart {

    // Given as doubles for high precision!
    public final Vector3d worldPosition = new Vector3d();

    public WorldRootedModelPart(AvatarMaterials.ModelPartMaterials materials, List<AvatarTexture> textures, @Nullable VanillaRendering vanilla) {
        super(materials, null, textures, vanilla);
    }

}
