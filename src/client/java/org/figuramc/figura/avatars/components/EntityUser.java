package org.figuramc.figura.avatars.components;

import org.figuramc.figura.avatars.Avatar;
import org.figuramc.figura.avatars.AvatarComponent;
import org.figuramc.figura.data.AvatarMaterials;
import org.figuramc.figura.manage.AvatarLoadingException;
import org.figuramc.figura.util.ClientUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Must be placed on an Avatar<UUID> used by an Entity.
 * Tracks the concept of "current entity" and can notify when it's changed, for other components to react to.
 * If another component wants to depend on this one's entity change flag, it should use Avatar.assertDependency() during
 * initialize().
 */
public class EntityUser implements AvatarComponent {

    private UUID uuid; // Constant, set at initialization
    private boolean justChanged; // Tracks whether the entity was just updated. Works as a flag for other components.
    private @Nullable Entity entity; // Changes during tick(). Other components access it and use it.


    @Override
    public void initialize(AvatarMaterials materials, Avatar<?> self) throws AvatarLoadingException {
        // Obtain the UUID
        if (!(self.user instanceof UUID userUUID)) throw new IllegalArgumentException("EntityUser AvatarComponent requires an Avatar with a UUID (Tied to an entity)");
        this.uuid = userUUID;
    }

    // Each tick, maybe update entity
    // If it changed, mark as changed for 1 tick
    @Override
    public boolean tick() {
        justChanged = false; // Set to false at the beginning
        if (entity == null || entity.isRemoved() || entity.level() != Minecraft.getInstance().level) { // If we don't have the entity...
            // Then search for it!
            Entity prevEntity = entity;
            entity = ClientUtils.getEntityByUUID(uuid);
            // If the entity changed, then say so.
            if (entity != prevEntity)
                justChanged = true;
        }
        return false;
    }

    public boolean changed() {
        return justChanged;
    }

    public @Nullable Entity getEntity() {
        return entity;
    }

}
