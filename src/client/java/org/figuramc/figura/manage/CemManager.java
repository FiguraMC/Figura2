package org.figuramc.figura.manage;

import org.figuramc.figura.avatars.AvatarTemplate;
import org.figuramc.figura.data.AvatarImporter;
import org.figuramc.figura.data.AvatarMaterials;
import org.figuramc.figura.directory.FiguraDir;
import org.figuramc.figura.util.exception.ExceptionUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * The central point for CEM operations.
 * Keeps cached AvatarMaterials for all entity types CEM.
 */
public class CemManager {

    private static final HashMap<EntityType<?>, @Nullable AvatarMaterials> IMPORTED_MATERIALS = new HashMap<>();

    public static void clear() {
        synchronized (IMPORTED_MATERIALS) {
            IMPORTED_MATERIALS.clear();
        }
    }

    // Tries to set up CEM for this entity.
    // - If entity already has an avatar, does nothing
    // - If the entity has an avatar loading in progress, does nothing
    // - If this entity type has no CEM in the folder, does nothing
    public static void tryGetCem(Entity entity) {
        UUID uuid = entity.getUUID();
        if (AvatarManager.ENTITY_AVATARS.get(uuid) != null) return;
        if (AvatarManager.ENTITY_AVATARS.isInProgress(uuid)) return;
        EntityType<?> type = entity.getType();
        AvatarManager.ENTITY_AVATARS.load(uuid, CompletableFuture.supplyAsync(ExceptionUtils.wrapChecked(() -> {
            AvatarMaterials materials;
            synchronized (IMPORTED_MATERIALS) {
                // Can't use computeIfAbsent, because want to store null values
                if (!IMPORTED_MATERIALS.containsKey(type)) {
                    // Try to load for this type
                    ResourceLocation entityTypeLocation = BuiltInRegistries.ENTITY_TYPE.getKey(type);
                    String modId = entityTypeLocation.getNamespace();
                    String entityName = entityTypeLocation.getPath();
                    Path entityDir = FiguraDir.CEM.get().resolve(modId).resolve(entityName);
                    if (!Files.exists(entityDir))
                        IMPORTED_MATERIALS.put(type, null);
                    else
                        IMPORTED_MATERIALS.put(type, AvatarImporter.importFolder(entityDir));
                }
                materials = IMPORTED_MATERIALS.get(type);
            }
            if (materials == null)
                return null;
            return AvatarTemplate.CEM_AVATAR.construct(uuid, materials);
        }, CompletionException::new)));
    }

}
