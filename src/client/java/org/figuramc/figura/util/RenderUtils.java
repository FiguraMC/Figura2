package org.figuramc.figura.util;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class RenderUtils {

    public static <T extends Entity> EntityRenderer<? super T, ?> getRenderer(T entity) {
        return Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
    }
    @SuppressWarnings("unchecked")
    public static <T extends Entity> EntityRenderer<? super T, ?> getRenderer(EntityType<T> type, @Nullable PlayerSkin.Model playerType) {
        if (type == EntityType.PLAYER)
            return (EntityRenderer<? super T, ?>) Minecraft.getInstance().getEntityRenderDispatcher().playerRenderers.get(playerType);
        return (EntityRenderer<? super T, ?>) Minecraft.getInstance().getEntityRenderDispatcher().renderers.get(type);
    }

    // Create an object on the render thread, and return a future of it.
    public static <T> CompletableFuture<T> createOnRenderThread(Supplier<T> supplier) {
        // If on render thread, run right away, otherwise queue it
        return CompletableFuture.supplyAsync(supplier, RenderSystem.isOnRenderThread() ? Runnable::run : RenderSystem::queueFencedTask);
    }
    // Run a task on the render thread, returning a future for its completion
    public static CompletableFuture<Void> runOnRenderThread(Runnable runnable) {
        // If on render thread, run right away, otherwise queue it
        return CompletableFuture.runAsync(runnable, RenderSystem.isOnRenderThread() ? Runnable::run : RenderSystem::queueFencedTask);
    }

}