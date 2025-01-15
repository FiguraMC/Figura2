package org.figuramc.figura.avatars.components;

import org.figuramc.figura.avatars.Avatar;
import org.figuramc.figura.avatars.AvatarComponent;
import org.figuramc.figura.data.AvatarMaterials;
import org.figuramc.figura.manage.AvatarLoadingException;
import org.figuramc.figura.manage.AvatarManager;

import java.util.UUID;

/**
 * Deletes the avatar when its entity is unloaded.
 * This prevents memory leaks for CEM avatars when their corresponding entity dies or is otherwise removed.
 */
public class CemSelfDeleter implements AvatarComponent {

    private UUID key;
    private EntityUser entityUser;

    @Override
    public void initialize(AvatarMaterials materials, Avatar<?> self) throws AvatarLoadingException {
        key = (UUID) self.user; // Assert, must be on an entity avatar
        entityUser = self.assertDependency(EntityUser.class, getClass());
    }

    @Override
    public boolean tick() {
        // If entity is gone, unload this avatar.
        if (entityUser.getEntity() == null) {
            AvatarManager.ENTITY_AVATARS.unload(this.key);
            return true;
        }
        return false;
    }
}
