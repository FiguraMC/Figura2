package org.figuramc.figura.avatars.components;

import org.figuramc.figura.avatars.AvatarComponent;
import org.figuramc.figura.manage.AvatarManager;

import java.util.UUID;

/**
 * Deletes the avatar when its entity is unloaded.
 * This prevents memory leaks for CEM avatars when their corresponding entity dies or is otherwise removed.
 */
public class CemSelfDeleter implements AvatarComponent<CemSelfDeleter> {

    private final UUID key;
    private final EntityUser entityUser;

    public static final Type<CemSelfDeleter> TYPE = new Type<>(EntityUser.TYPE);
    public Type<CemSelfDeleter> getType() { return TYPE; }

    public CemSelfDeleter(UUID key, EntityUser entityUser) {
        this.key = key;
        this.entityUser = entityUser;
    }

    @Override
    public void tick() {
        // If entity is gone, unload this avatar.
        if (entityUser.getEntity() == null)
            AvatarManager.ENTITY_AVATARS.unload(this.key);
    }
}
