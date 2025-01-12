package org.figuramc.figura.ducks.client;

import net.minecraft.world.entity.Entity;

/**
 * Access the new fields in EntityRenderStateMixin
 */
public interface EntityRenderStateAccess {
    Entity figura$getEntity();
    void figura$setEntity(Entity entity);
    float figura$getTickDelta();
    void figura$setTickDelta(float tickDelta);
}
