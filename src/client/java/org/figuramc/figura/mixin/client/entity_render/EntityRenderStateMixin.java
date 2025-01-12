package org.figuramc.figura.mixin.client.entity_render;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.figuramc.figura.ducks.client.EntityRenderStateAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Because EntityRenderState doesn't contain the actual entity instance, we need to add it manually.
 */
@Mixin(EntityRenderState.class)
public class EntityRenderStateMixin implements EntityRenderStateAccess {

    /**
     * New field in the EntityRenderState containing the entity itself.
     * Only needs to be initialized inside EntityRenderer.extractRenderState.
     */
    @Unique public Entity entity;
    @Unique public float tickDelta;

    // Getters/setters for duck access
    @Override public Entity figura$getEntity() { return entity; }
    @Override public void figura$setEntity(Entity entity) { this.entity = entity; }
    @Override public float figura$getTickDelta() { return tickDelta; }
    @Override public void figura$setTickDelta(float tickDelta) { this.tickDelta = tickDelta; }
}
