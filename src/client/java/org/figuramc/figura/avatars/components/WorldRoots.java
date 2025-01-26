package org.figuramc.figura.avatars.components;

import org.figuramc.figura.avatars.Avatar;
import org.figuramc.figura.avatars.AvatarComponent;
import org.figuramc.figura.data.AvatarMaterials;
import org.figuramc.figura.manage.AvatarLoadingException;
import org.figuramc.figura.model.part.WorldRootModelPart;
import org.figuramc.figura.util.ListUtils;

import java.util.List;

public class WorldRoots implements AvatarComponent {

    private List<WorldRootModelPart> worldRoots;

    @Override
    public void initialize(AvatarMaterials materials, Avatar<?> self) {
        Textures texturesComponent = self.assertDependency(Textures.class, getClass());
        worldRoots = ListUtils.map(materials.worldRoots(), mats -> new WorldRootModelPart(mats, texturesComponent.textures));
    }

    @Override
    public void destroy() {
        for (WorldRootModelPart worldRoot : worldRoots)
            worldRoot.destroy();
    }
}
