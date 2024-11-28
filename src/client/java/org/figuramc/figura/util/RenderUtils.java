package org.figuramc.figura.util;

import com.mojang.blaze3d.pipeline.RenderCall;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4d;

public class RenderUtils {

    /**
     * Says "RenderCall", but is just a runnable
     */
    public static void executeOnRenderThread(RenderCall r) {
        if (RenderSystem.isOnRenderThreadOrInit())
            r.execute();
        else
            RenderSystem.recordRenderCall(r);
    }

    public static <T extends Entity> EntityRenderer<? super T> getRenderer(T entity) {
        return Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
    }

    public static @Nullable EntityModel<?> getModel(Entity entity) {
        EntityRenderer<?> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
        if (renderer instanceof LivingEntityRenderer<?,?> living) {
            return living.getModel();
        }
        return null;
    }

    /**
     * Keep track of the world <=> view matrices, update each frame
     */
    public static final Matrix4d WORLD_TO_VIEW_MATRIX = new Matrix4d();
    public static final Matrix4d VIEW_TO_WORLD_MATRIX = new Matrix4d();
    public static void updateWorldViewMatrices(Matrix4d worldToView) {
        WORLD_TO_VIEW_MATRIX.set(worldToView);
        WORLD_TO_VIEW_MATRIX.invert(VIEW_TO_WORLD_MATRIX);
    }

}