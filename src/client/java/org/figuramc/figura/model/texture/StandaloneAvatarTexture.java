package org.figuramc.figura.model.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatars.AvatarError;
import org.figuramc.figura.data.ModuleMaterials;
import org.figuramc.figura.script_hooks.mem_count.AllocationTracker;
import org.figuramc.figura.util.RenderUtils;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An avatar texture that is backed by its own image, and is not stored as part of an atlas.
 */
public class StandaloneAvatarTexture extends AvatarTexture {

    // Unique ResourceLocations
    private static final AtomicInteger next_id = new AtomicInteger();

    public final ResourceLocation location;
    public final DynamicTexture backingTexture;

    protected StandaloneAvatarTexture(ResourceLocation location, DynamicTexture backingTexture) {
        this.location = location;
        this.backingTexture = backingTexture;
        // Ensure on render thread to safely register the texture
        if (!RenderSystem.isOnRenderThread()) throw new IllegalStateException("Attempt to construct StandaloneAvatarTexture outside render thread");
        Minecraft.getInstance().getTextureManager().register(this.location, this.backingTexture);
    }

    // Create and upload the texture.
    public static StandaloneAvatarTexture create(ModuleMaterials.TextureMaterials.OwnedTexture materials, @Nullable AllocationTracker allocationTracker) throws AvatarError {
        int id = next_id.getAndIncrement();
        ResourceLocation location = FiguraMod.id("figura_textures/" + id);
        ByteBuffer buffer = BufferUtils.createByteBuffer(materials.data().length);
        buffer.put(materials.data());
        buffer.rewind();
        try {
            NativeImage image = NativeImage.read(buffer);
            String debugName = "Figura texture #" + id + ": " + materials.name();
            StandaloneAvatarTexture tex = new StandaloneAvatarTexture(location, new DynamicTexture(() -> debugName, image));
            // Track the texture if needed
            if (allocationTracker != null) allocationTracker.track(tex, image.getWidth() * image.getHeight() * 4); // 4 bytes per pixel (R, G, B, A)
            return tex;
        } catch (IOException e) {
            throw new AvatarError("figura.error.loading.invalid_png", e, materials.name());
        }
    }

    // AbstractAvatarTexture:
    @Override public CompletableFuture<Void> upload() {
        return RenderUtils.runOnRenderThread(() -> {
            // Upload texture and register it to texture manager
            backingTexture.upload();
            Minecraft.getInstance().getTextureManager().register(this.location, this.backingTexture);
        });
    }
    @Override public CompletableFuture<Void> destroy() {
        return RenderUtils.runOnRenderThread(() -> {
            // Close the backing texture and de-register from the texture manager
            backingTexture.close();
            Minecraft.getInstance().getTextureManager().release(this.location);
        });
    }
    @Override public ResourceLocation getLocation() { return location; }
    @Override public Vector4f getUvValues() { return new Vector4f(0, 0, 1, 1); }

    @Override public int getWidth() { return backingTexture.getPixels().getWidth(); }
    @Override public int getHeight() { return backingTexture.getPixels().getHeight(); }
    @Override public int getPixel(int x, int y) { return backingTexture.getPixels().getPixel(x, y); }

}
