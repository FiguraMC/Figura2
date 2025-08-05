package org.figuramc.figura.model.texture;

import net.minecraft.resources.ResourceLocation;
import org.figuramc.figura.avatars.AvatarError;
import org.figuramc.figura.avatars.components.Textures;
import org.figuramc.figura.data.ModuleMaterials;
import org.figuramc.figura.script_hooks.mem_count.AllocationTracker;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector4f;

import java.util.concurrent.CompletableFuture;

/**
 * A generic avatar texture as seen from a script.
 * Subclassed by "Standalone", "Atlased", and "Vanilla" variations.
 */
public abstract class AvatarTexture {

    // Create a texture and upload it.
    public static AvatarTexture from(ModuleMaterials.TextureMaterials materials, @Nullable AllocationTracker allocationTracker, Textures textureComponent, FiguraTextureAtlas.Builder atlasBuilder) throws AvatarError {
        switch (materials) {
            case ModuleMaterials.TextureMaterials.OwnedTexture owned -> {
                if (owned.noAtlas()) {
                    return StandaloneAvatarTexture.create(owned, allocationTracker);
                } else {
                    return new AtlasedAvatarTexture(textureComponent, owned, atlasBuilder);
                }
            }
            case ModuleMaterials.TextureMaterials.VanillaTexture vanilla -> {
                return new VanillaAvatarTexture(vanilla);
            }
        }
    }

    // Upload the texture, committing any changes.
    // This must happen on the render thread, so it returns a future indicating it's complete.
    public abstract CompletableFuture<Void> upload();
    // Ensure that any backing native resources are closed.
    // Must happen on the render thread, so it returns a future.
    // If you don't care when the destruction completes, only that it happens eventually, feel free to ignore result.
    public abstract CompletableFuture<Void> destroy();
    // Get the ResourceLocation of a backing texture for this texture.
    public abstract ResourceLocation getLocation();
    // Get UV values that let a part use this texture.
    // x offset, y offset, x scale, y scale. All 0 to 1.
    public abstract Vector4f getUvValues();

    // Width/Height, and ability to get a pixel at a given position
    public abstract int getWidth();
    public abstract int getHeight();
    public abstract int getPixel(int x, int y); // Use the Minecraft ARGB class with the returned int
}