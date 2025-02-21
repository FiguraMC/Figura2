package org.figuramc.figura.model.renderers.vanilla_optimized;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.model.part.RootModelPart;
import org.figuramc.figura.model.renderers.FiguraPartRenderer;
import org.figuramc.figura.model.renderers.vbo.OptimizedBufferBuilder;
import org.figuramc.figura.model.renderers.vbo.OptimizedHelpers;
import org.figuramc.figura.model.renderers.vbo.PartDataStorageBuffer;
import org.figuramc.figura.model.shader.FiguraRenderType;
import org.figuramc.figura.script_hooks.ScriptError;
import org.figuramc.figura.script_hooks.mem_count.MarkedObjectBase;
import org.figuramc.figura.script_hooks.mem_count.MemoryCounter;
import org.figuramc.figura.util.FiguraTransformStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.List;

public class VanillaOptimizedRenderer implements FiguraPartRenderer {

    public static final VanillaOptimizedRenderer INSTANCE = new VanillaOptimizedRenderer();
    private VanillaOptimizedRenderer() {}

    // Black transparent pixel to use as a placeholder for no texture
    private static final ResourceLocation ZERO_PIXEL = Util.make(() -> {
        ResourceLocation loc = FiguraMod.id("zero_pixel");
        DynamicTexture tex = new DynamicTexture(Util.make(() -> {
            NativeImage image = new NativeImage(1, 1, false);
            image.setPixel(0, 0, ARGB.color(0, 0, 0, 0));
            return image;
        }));
        Minecraft.getInstance().getTextureManager().register(loc, tex);
        return loc;
    });

    @Override
    public void render(
            RootModelPart root,
            MultiBufferSource bufferSource,
            FiguraTransformStack matrixStack,
            float tickDelta,
            int light, int overlay
    ) throws ScriptError, StackOverflowError {
        // Fetch state from the part, or create new state:
        RootState state = (RootState) root.rootRenderState;
        if (state == null) root.rootRenderState = state = new RootState();
        // If the state needs a rebuild, do so.
        if (state.needsRebuild) {
            OptimizedHelpers.rebuild(root, state.optimizedBufferBuilder, state.partDataStorageBuffer, part -> {
                // Create/get the optimized render type from the part's state
                @Nullable NonRootState nonRootState = (NonRootState) part.nonRootRenderState;
                if (nonRootState == null) {
                    assert part.getRenderType() != null;
                    part.nonRootRenderState = nonRootState = new NonRootState(switch (part.getRenderType()) {
                        case FiguraRenderType.EndPortal p -> OptimizedVanillaShaders.OPTIMIZED_END_PORTAL;
                        case FiguraRenderType.EndGateway g -> OptimizedVanillaShaders.OPTIMIZED_END_GATEWAY;
                        case FiguraRenderType.Basic(@Nullable ResourceLocation mainTex, @Nullable ResourceLocation emissiveTex) -> {
                            if (mainTex == null) mainTex = ZERO_PIXEL;
                            if (emissiveTex == null) emissiveTex = ZERO_PIXEL;
                            yield OptimizedVanillaShaders.OPTIMIZED_BASIC.apply(mainTex, emissiveTex);
                        }
                    });
                }
                return List.of(nonRootState.minecraftRenderType);
            });
            state.needsRebuild = false;
        }
        // Update transforms:
        OptimizedHelpers.updateTransforms(root, state.partDataStorageBuffer, tickDelta);

        // Now render (this is VanillaOptimizedRenderer-specific!)
        state.partDataStorageBuffer.bind(0);
        state.optimizedBufferBuilder.draw(() -> {
            // Cursed uniform setting... TODO make better
            Matrix4f figuraRootMatrix = matrixStack.peekPosition();
            RenderSystem.getShader()
                    .getUniform("FiguraRootMatrix")
                    .set(figuraRootMatrix);
        }, () -> {});
    }

    public static class RootState extends MarkedObjectBase implements FiguraPartRenderer.RootState {

        public boolean needsRebuild = true;
        public OptimizedBufferBuilder optimizedBufferBuilder = new OptimizedBufferBuilder();
        public PartDataStorageBuffer partDataStorageBuffer = new PartDataStorageBuffer();

        @Override
        public void destroy() {
            optimizedBufferBuilder.clear();
            partDataStorageBuffer.clear();
            // Ensure they're not used after destruction
            optimizedBufferBuilder = null;
            partDataStorageBuffer = null;
        }

        @Override
        protected long traceNoMark(MemoryCounter counter, int depth) {
            throw new UnsupportedOperationException("TODO");
        }
    }

    public static class NonRootState extends MarkedObjectBase implements FiguraPartRenderer.NonRootState {

        public final RenderType minecraftRenderType;

        public NonRootState(RenderType type) {
            this.minecraftRenderType = type;
        }

        @Override
        protected long traceNoMark(MemoryCounter counter, int depth) {
            throw new UnsupportedOperationException("TODO");
        }
    }

}
