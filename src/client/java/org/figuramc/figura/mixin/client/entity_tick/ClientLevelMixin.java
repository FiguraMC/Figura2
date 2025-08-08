package org.figuramc.figura.mixin.client.entity_tick;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.figuramc.figura.avatars.Avatar;
import org.figuramc.figura.avatars.components.Scripts;
import org.figuramc.figura.manage.AvatarManager;
import org.figuramc.figura.script_hooks.Event;
import org.figuramc.figura.script_hooks.callback.items.EntityView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

// Mixin to run the entity_tick event
@Mixin(ClientLevel.class)
public class ClientLevelMixin {

    @Inject(method = "tickNonPassenger", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;tick()V"))
    public void afterTick(Entity entity, CallbackInfo ci) {
        callTickMethod(entity);
    }

    @Inject(method = "tickPassenger", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;rideTick()V"))
    public void afterRideTick(Entity vehicle, Entity rider, CallbackInfo ci) {
        callTickMethod(rider);
    }

    @Unique
    private static void callTickMethod(Entity entity) {
        Avatar<UUID> avatar = AvatarManager.tryGetEntityAvatar(entity);
        if (avatar == null) return;
        Scripts scriptsComponent = avatar.getComponent(Scripts.TYPE);
        if (scriptsComponent == null) return;
        EntityView<Entity> entityView = new EntityView<>(entity);
        scriptsComponent.runEvent(avatar, Event.ENTITY_TICK, entityView);
        entityView.revoke();
    }

}
