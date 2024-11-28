package org.figuramc.figura.avatars.components;

import org.figuramc.figura.avatars.Avatar;
import org.figuramc.figura.avatars.AvatarComponent;
import org.figuramc.figura.data.AvatarMaterials;
import org.figuramc.figura.manage.AvatarLoadingException;
import org.figuramc.figura.model.part.WorldRootModelPart;

import java.util.List;

public class WorldRoots implements AvatarComponent {

    private List<WorldRootModelPart> worldRoots = List.of();

    @Override
    public void initialize(AvatarMaterials materials, Avatar<?> self) throws AvatarLoadingException {

    }

    @Override
    public void destroy() {
        for (WorldRootModelPart worldRoot : worldRoots)
            worldRoot.destroy();
    }
}
