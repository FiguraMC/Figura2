package org.figuramc.figura.model.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.data.AvatarMaterials;
import org.figuramc.figura.manage.AvatarLoadingException;
import org.figuramc.figura.util.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An avatar texture that is backed by its own image, and is not stored as part of an atlas.
 */
public class StandaloneAvatarTexture extends AvatarTexture {

    private final Backing backingTexture;

    public StandaloneAvatarTexture(AvatarMaterials.TextureMaterials.OwnedTexture materials) throws AvatarLoadingException {
        this.backingTexture = new Backing(materials);
    }

    // AbstractAvatarTexture:
    @Override public void upload() { backingTexture.upload(); }
    @Override public void destroy() { backingTexture.destroy(); }
    @Override public ResourceLocation getLocation() { return backingTexture.getLocation(); }
    @Override public Vector4f getUvValues() { return new Vector4f(0, 0, 1, 1); }

    @Override public int getWidth() { return backingTexture.getWidth(); }
    @Override public int getHeight() { return backingTexture.getHeight(); }
    @Override public int getPixelRGBA(int x, int y) { return backingTexture.image.getPixelRGBA(x, y); }

    // The actual AbstractTexture object backing this:
    private static class Backing extends AbstractTexture {

        private final ResourceLocation location;
        private final NativeImage image;

        private boolean isClosed = false;

        private static final AtomicInteger next_id = new AtomicInteger();

        public Backing(AvatarMaterials.TextureMaterials.OwnedTexture materials) throws AvatarLoadingException {
            // Give it a unique location using the atomic integer
            this.location = FiguraMod.id("figura_textures/" + next_id.getAndIncrement());

            // Store PNG data in a byte buffer and create a NativeImage
            ByteBuffer buffer = BufferUtils.createByteBuffer(materials.data().length);
            buffer.put(materials.data());
            buffer.rewind();
            try {
                this.image = NativeImage.read(buffer);
            } catch (IOException e) {
                throw new AvatarLoadingException("Somehow failed to read NativeImage? Please contact devs!", e);
            }
        }

        // Helpful getters:

        // Approximate size of the texture in bytes (including name and other data)
        public long numBytes() {
            return 20 + (long) image.getWidth() * image.getHeight() * image.format().components();
        }
        public ResourceLocation getLocation() { return this.location; }
        public int getWidth() { return image.getWidth(); }
        public int getHeight() { return image.getHeight(); }

        // Minecraft SimpleTexture class implementations:

        // Don't implement.
        // We don't get our textures from the resource manager,
        // we get them from our own loading systems.
        @Override
        public void load(ResourceManager manager) throws IOException {}

        @Override
        public void close() {
            if (isClosed) return;
            isClosed = true;
            image.close(); // Close the native image resource
        }

        public void destroy() {
            close();
            Minecraft.getInstance().getTextureManager().release(this.location); // Delete the texture from the game's texture manager
        }

        // Figura functions:

        // Uploads the texture to the texture manager,
        // committing any changes that happened CPU-side.
        public void upload() {
            if (isClosed) return;
            RenderUtils.executeOnRenderThread(() -> {
                if (isClosed) return;
                TextureManager manager = Minecraft.getInstance().getTextureManager();
                manager.register(this.location, this);
                TextureUtil.prepareImage(this.getId(), image.getWidth(), image.getHeight());
                image.upload(0, 0, 0, false);
            });
        }

    }

}
