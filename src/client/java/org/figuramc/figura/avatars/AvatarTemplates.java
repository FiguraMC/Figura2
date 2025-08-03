package org.figuramc.figura.avatars;

import net.minecraft.client.renderer.entity.EntityRenderer;
import org.figuramc.figura.avatars.components.*;
import org.figuramc.figura.script_hooks.mem_count.AllocationTracker;

import java.util.List;
import java.util.UUID;

/**
 * Different types of Avatar which have a preset group of components
 */
public class AvatarTemplates {

    public static Avatar<UUID> localPlayer(UUID key, EntityRenderer<?, ?> entityRenderer, List<AvatarModules.LoadTimeModule> modules) throws AvatarError {
        AllocationTracker allocationTracker = new AllocationTracker(Integer.MAX_VALUE, 0, 0);
        Textures textures = new Textures(modules, allocationTracker);
        VanillaRendering vanillaRendering = new VanillaRendering(entityRenderer);
        EntityRoot entityRoot = new EntityRoot(modules, allocationTracker, textures, vanillaRendering);
        EntityUser entityUser = new EntityUser(key);
        CustomItems customItems = new CustomItems(modules, allocationTracker, textures, vanillaRendering);
        Scripts scripts = new Scripts(modules, allocationTracker, entityRoot, entityUser, vanillaRendering);
        return new Avatar<>(key, modules, allocationTracker, List.of(textures, entityRoot, entityUser, vanillaRendering, customItems, scripts));
    }

    public static Avatar<UUID> cemAvatar(UUID key, EntityRenderer<?, ?> entityRenderer, List<AvatarModules.LoadTimeModule> modules) throws AvatarError {
        AllocationTracker allocationTracker = null;
        Textures textures = new Textures(modules, allocationTracker);
        VanillaRendering vanillaRendering = new VanillaRendering(entityRenderer);
        EntityRoot entityRoot = new EntityRoot(modules, allocationTracker, textures, vanillaRendering);
        EntityUser entityUser = new EntityUser(key);
        CemSelfDeleter cemSelfDeleter = new CemSelfDeleter(key, entityUser); // Component that deletes CEM avatar when the entity unloads
        CustomItems customItems = new CustomItems(modules, allocationTracker, textures, vanillaRendering);
        Scripts scripts = new Scripts(modules, allocationTracker, entityRoot, entityUser, vanillaRendering);
        return new Avatar<>(key, modules, allocationTracker, List.of(textures, entityRoot, entityUser, cemSelfDeleter, vanillaRendering, customItems, scripts));
    }

}
