package org.figuramc.figura.manage;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.figuramc.figura.avatars.AvatarModules;
import org.figuramc.figura.avatars.AvatarTemplates;
import org.figuramc.figura.data.ModuleImporter;
import org.figuramc.figura.data.ModuleMaterials;
import org.figuramc.figura.directory.FiguraDir;
import org.figuramc.figura.util.ErrorReporting;
import org.figuramc.figura.util.RenderUtils;
import org.figuramc.figura.util.exception.ExceptionUtils;
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

    private static final ConcurrentHashMap<EntityType<?>, CompletableFuture<@Nullable ModuleMaterials>> IMPORTED_MATERIALS = new ConcurrentHashMap<>();

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
        CompletableFuture<@Nullable ModuleMaterials> materials = IMPORTED_MATERIALS.computeIfAbsent(type,
                t -> CompletableFuture.supplyAsync(ExceptionUtils.wrapChecked(() -> {
                    // Try to load for this type
                    ResourceLocation entityTypeLocation = BuiltInRegistries.ENTITY_TYPE.getKey(t);
                    String modId = entityTypeLocation.getNamespace();
                    String entityName = entityTypeLocation.getPath();
                    Path entityDir = FiguraDir.CEM.get().resolve(modId).resolve(entityName);
                    return Files.exists(entityDir) ? ModuleImporter.importPath(entityDir) : null;
                }, CompletionException::new)));
        // If the task isn't complete yet, just return out.
        if (!materials.isDone()) return;
        try {
            // Fetch the result. If this doesn't throw, then it completed without error.
            @Nullable ModuleMaterials result = materials.getNow(null);
            // If the result is null, this entity has no CEM, so just do nothing and return.
            if (result == null) return;
            // Otherwise, this entity has CEM, so launch a task to load it.
            AvatarManager.ENTITY_AVATARS.load(uuid, CompletableFuture.supplyAsync(ExceptionUtils.wrapChecked(
                    () -> AvatarTemplates.cemAvatar(uuid, RenderUtils.getRenderer(entity), AvatarModules.loadModules(result)),
                    CompletionException::new
            ), Runnable::run)); // TODO: Fix texture loading so it can work in an off thread instead of render thread
        } catch (CompletionException ex) {
            // For now, always report CEM errors to chat.
            ErrorReporting.reportError(ex.getCause());
        } catch (Throwable unexpected) {
            ErrorReporting.reportError(unexpected);
        }
    }

}
