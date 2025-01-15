package org.figuramc.figura.model.part;

import org.figuramc.figura.data.AvatarMaterials;
import org.figuramc.figura.model.texture.AvatarTexture;
import org.joml.Vector3d;

import java.util.List;

public class WorldRootModelPart extends RootModelPart {

    // Given as doubles for high precision!
    public final Vector3d worldPosition = new Vector3d();

    public WorldRootModelPart(AvatarMaterials.ModelPartMaterials materials, List<AvatarTexture> textures) {
        super(materials, textures);
    }

}
