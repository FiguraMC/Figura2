package org.figuramc.figura.manage;

import org.figuramc.figura.avatars.AvatarTemplate;
import org.figuramc.figura.data.AvatarImportingException;
import org.figuramc.figura.data.AvatarMaterials;
import org.figuramc.figura.data.NewAvatarImporter;
import org.figuramc.figura.directory.FiguraDir;
import org.figuramc.figura.util.ErrorReporting;
import org.figuramc.figura.util.exception.ExceptionUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The central point for CEM operations.
 * Keeps cached AvatarMaterials for all entity types CEM.
 */
public class CemManager {

    private static final ConcurrentHashMap<EntityType<?>, CompletableFuture<@Nullable AvatarMaterials>> IMPORTED_MATERIALS = new ConcurrentHashMap<>();

    public static void clear() {
        synchronized (IMPORTED_MATERIALS) {
            IMPORTED_MATERIALS.clear();
        }
    }

    // Tries to set up CEM for this entity.
    // We already know the entity doesn't have an avatar equipped.
    // - If the entity already has an avatar loading in progress, does nothing
    // - If this entity type has no CEM in the folder, does nothing
    // - Otherwise, will launch a task to give this entity its CEM avatar.
    public static void tryGetCem(Entity entity) {
        UUID uuid = entity.getUUID();
        if (AvatarManager.ENTITY_AVATARS.isInProgress(uuid)) return;
        EntityType<?> type = entity.getType();
        // Fetch the materials, or begin a task for them
        CompletableFuture<@Nullable AvatarMaterials> materials = IMPORTED_MATERIALS.computeIfAbsent(type,
                t -> CompletableFuture.supplyAsync(ExceptionUtils.wrapChecked(() -> {
                    // Try to load for this type
                    ResourceLocation entityTypeLocation = BuiltInRegistries.ENTITY_TYPE.getKey(t);
                    String modId = entityTypeLocation.getNamespace();
                    String entityName = entityTypeLocation.getPath();
                    Path entityDir = FiguraDir.CEM.get().resolve(modId).resolve(entityName);
                    return Files.exists(entityDir) ? NewAvatarImporter.importPath(entityDir) : null;
                }, CompletionException::new)));
        // If the task isn't complete yet, just return out.
        if (!materials.isDone()) return;
        try {
            // Fetch the result. If this doesn't throw, then it completed without error.
            @Nullable AvatarMaterials result = materials.getNow(null);
            // If the result is null, this entity has no CEM, so just do nothing and return.
            if (result == null) return;
            // Otherwise, this entity has CEM, so launch a task to load it.
            AvatarManager.ENTITY_AVATARS.load(uuid, CompletableFuture.supplyAsync(ExceptionUtils.wrapChecked(() ->
                    AvatarTemplate.CEM_AVATAR.construct(uuid, result), CompletionException::new)));
        } catch (CompletionException ex) {
            Throwable cause = ex.getCause();
            // For now, always report CEM errors to chat.
            //noinspection SwitchStatementWithTooFewBranches
            switch (cause) {
                case AvatarImportingException importingException -> ErrorReporting.avatarImporting(importingException);
                default -> ErrorReporting.unexpectedError(cause);
            }
        } catch (Throwable unexpected) {
            ErrorReporting.unexpectedError(unexpected);
        }
    }

}
