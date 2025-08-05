package org.figuramc.figura.model.texture;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.PngInfo;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatars.AvatarError;
import org.figuramc.figura.script_hooks.mem_count.AllocationTracker;
import org.figuramc.figura.util.ListUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class FiguraTextureAtlas extends StandaloneAvatarTexture {

    private static final AtomicInteger next_id = new AtomicInteger();

    protected FiguraTextureAtlas(ResourceLocation location, DynamicTexture backingTexture) {
        super(location, backingTexture);
    }

    /**
     * Create and upload the atlas.
     */
    private static FiguraTextureAtlas create(int totalWidth, int totalHeight, List<TextureRectangle> rectangles, @Nullable AllocationTracker allocationTracker) throws AvatarError {
        // Unique location
        int id = next_id.getAndIncrement();
        ResourceLocation location = FiguraMod.id("figura_atlases/" + id);
        // Power-of-two-ify
        int width = Mth.smallestEncompassingPowerOfTwo(totalWidth);
        int height = Mth.smallestEncompassingPowerOfTwo(totalHeight);
        // Create the NativeImage and fill it
        NativeImage backingImage = new NativeImage(width, height, true);
        for (TextureRectangle rectangle : rectangles) {
            try(NativeImage png = NativeImage.read(rectangle.data)) { // Try with resources to close the image
                png.copyRect(backingImage,
                        0, 0, // Png pos
                        rectangle.x, rectangle.y, // Atlas pos
                        rectangle.width, rectangle.height, // Size to copy
                        false, false // Mirror horizontal/vertical
                );
            } catch (IOException ex) {
                throw new AvatarError("figura.error.loading.invalid_png", ex, rectangle.name);
            } finally {
                // Free rectangle name and data
                rectangle.name = null;
                rectangle.data = null;
            }
        }
        // (TODO) Return a future on the render thread, creating and uploading the image?
        String debugName = "Figura Texture Atlas #" + id;
        FiguraTextureAtlas atlas = new FiguraTextureAtlas(location, new DynamicTexture(() -> debugName, backingImage));
        // Track it in memory
        if (allocationTracker != null)
            allocationTracker.track(atlas, totalWidth * totalHeight * 4); // 4 bytes per pixel (R, G, B, A)
        return atlas;
    }

    // The texture will be destroyed eventually. There is no need to use the returned future if you don't care when the texture is destroyed.

    // Uploads the texture to the texture manager, committing any changes that happened CPU-side.
    // This happens on the render thread, so this returns a future indicating uploading is complete.

    // Builder
    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private final List<TextureRectangle> rectangles = new ArrayList<>();

        private Builder() {}

        // Get back a TextureRectangle.
        // Later, once you call .build(), the rectangle will be updated to have
        // its "x" and "y" values set.
        public TextureRectangle insert(String texName, byte[] png) throws AvatarError {
            TextureRectangle rect = new TextureRectangle(texName, png);
            rectangles.add(rect);
            return rect;
        }

        /**
         * Create and upload the atlas. If there are no textures to atlas, returns null.
         */
        public @Nullable FiguraTextureAtlas build(@Nullable AllocationTracker allocationTracker) throws AvatarError {
            if (rectangles.isEmpty()) return null;

            // Guess a width as sqrt(sum(rectangle areas))
            int totalArea = 0;
            for (TextureRectangle rectangle : rectangles) totalArea += rectangle.getArea();
            int widthGuess = Mth.smallestEncompassingPowerOfTwo((int) Math.ceil(Math.sqrt(totalArea)));

            // Sort by height; the tallest is at the end
            rectangles.sort(Comparator.comparingInt(TextureRectangle::getHeight));
            List<Region> unusedRegions = new ArrayList<>();

            int totalWidth = 0;
            int totalHeight = rectangles.getLast().getHeight();
            int currentX = 0;
            int currentY = 0;

            for (int index = rectangles.size() - 1; index >= 0; index--) {
                TextureRectangle rect = rectangles.get(index);
                // Search for the shortest unused region which can fit the rect:
                int bestUnusedRegionIndex = ListUtils.filteredIndexOfMinimal(unusedRegions, region -> region.canFit(rect), Region::height);
                if (bestUnusedRegionIndex == -1) {
                    // No region can fit this rect, so let's add it as we go
                    if (currentX + rect.width > widthGuess) {
                        // Wrap to the next line
                        rect.x = 0;
                        rect.y = totalHeight;
                        currentX = rect.width;
                        currentY = totalHeight;
                        totalHeight += rect.height;
                    } else {
                        // Add this horizontally
                        rect.x = currentX;
                        rect.y = currentY;
                        currentX += rect.width;
                        totalWidth = Math.max(totalWidth, currentX);
                        // Add an unused region below it if needed
                        if (totalHeight - rect.getHeight() != currentY)
                            unusedRegions.add(new Region(rect.getX(), rect.getBottom(), rect.getWidth(), totalHeight - rect.getHeight()));
                    }
                } else {
                    // There's an unused region that can fit this rect, so let's use it:
                    Region region = unusedRegions.remove(bestUnusedRegionIndex);
                    rect.x = region.x;
                    rect.y = region.y;
                    if (rect.width != region.width)
                        unusedRegions.add(new Region(rect.getRight(), rect.getY(), region.width - rect.width, region.height));
                    if (rect.height != region.height)
                        unusedRegions.add(new Region(rect.getX(), rect.getBottom(), rect.width, region.height - rect.height));
                }
            }

            return FiguraTextureAtlas.create(totalWidth, totalHeight, rectangles, allocationTracker);
        }

    }

    public static class TextureRectangle {
        private String name;
        private byte[] data;
        private int x = -1, y = -1;
        private final int width, height;

        private TextureRectangle(String texName, byte[] png) throws AvatarError {
            try {
                this.name = texName;
                PngInfo info = PngInfo.fromBytes(png);
                if (info.width() < 1 || info.height() < 1) {
                    throw new AvatarError("figura.error.loading.invalid_png", texName);
                }
                this.width = info.width();
                this.height = info.height();
                this.data = png;
            } catch (IOException ioException) {
                throw new AvatarError("figura.error.loading.invalid_png", texName);
            }
        }
        public int getX() { return x; }
        public int getY() { return y; }
        public int getWidth() { return width; }
        public int getHeight() { return height; }
        public int getMaxSide() { return Math.max(width, height); }
        public int getArea() { return width * height; }
        public int getRight() { return x + width; }
        public int getBottom() { return y + height; }

        boolean contains(int x, int y) { return x >= this.x && y >= this.y && x < this.x + this.width && y < this.y + this.height; }
    }

    private record Region(int x, int y, int width, int height) {
        boolean canFit(TextureRectangle rectangle) {
            return rectangle.width <= this.width && rectangle.height <= this.height;
        }
    }
}
