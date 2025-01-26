package org.figuramc.figura.util;

import com.mojang.blaze3d.pipeline.RenderCall;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4d;

import java.util.function.Supplier;

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

    public static <T extends Entity> EntityRenderer<? super T, ?> getRenderer(T entity) {
        return Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
    }

    public static void uploadTexture(Supplier<Boolean> closed, AbstractTexture tex, ResourceLocation location, NativeImage image) {
        if (closed.get()) return;
        RenderUtils.executeOnRenderThread(() -> {
            if (closed.get()) return;
            TextureManager manager = Minecraft.getInstance().getTextureManager();
            manager.register(location, tex);
            TextureUtil.prepareImage(tex.getId(), image.getWidth(), image.getHeight());
            image.upload(0, 0, 0, false);
        });
    }

}