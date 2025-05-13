package org.figuramc.figura.avatars;

import net.minecraft.client.renderer.entity.EntityRenderer;
import org.figuramc.figura.avatars.components.*;
import org.figuramc.figura.manage.AvatarLoadingException;

import java.util.List;
import java.util.UUID;

/**
 * Different types of Avatar which have a preset group of components
 */
public class AvatarTemplates {

    public static Avatar<UUID> localPlayer(UUID key, EntityRenderer<?, ?> entityRenderer, AvatarModules modules) throws AvatarLoadingException {
        Textures textures = new Textures(modules);
        VanillaRendering vanillaRendering = new VanillaRendering(entityRenderer);
        EntityRoot entityRoot = new EntityRoot(modules, textures, vanillaRendering);
        EntityUser entityUser = new EntityUser(key);
        CustomItems customItems = new CustomItems(modules, textures, vanillaRendering);
        Scripts scripts = new Scripts(entityRoot, entityUser, vanillaRendering);
        return new Avatar<>(key, modules, List.of(textures, entityRoot, entityUser, vanillaRendering, customItems, scripts));
    }

    public static Avatar<UUID> cemAvatar(UUID key, EntityRenderer<?, ?> entityRenderer, AvatarModules modules) throws AvatarLoadingException {
        Textures textures = new Textures(modules);
        VanillaRendering vanillaRendering = new VanillaRendering(entityRenderer);
        EntityRoot entityRoot = new EntityRoot(modules, textures, vanillaRendering);
        EntityUser entityUser = new EntityUser(key);
        CemSelfDeleter cemSelfDeleter = new CemSelfDeleter(key, entityUser);
        CustomItems customItems = new CustomItems(modules, textures, vanillaRendering);
        Scripts scripts = new Scripts(entityRoot, entityUser, vanillaRendering);
        return new Avatar<>(key, modules, List.of(textures, entityRoot, entityUser, cemSelfDeleter, vanillaRendering, customItems, scripts));
    }

}
