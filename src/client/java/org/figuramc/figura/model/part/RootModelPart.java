package org.figuramc.figura.model.part;

import org.figuramc.figura.data.AvatarMaterials;
import org.figuramc.figura.model.optimized.OptimizedBufferBuilder;
import org.figuramc.figura.model.optimized.PartDataStorageBuffer;
import org.figuramc.figura.model.optimized.RenderingMode;
import org.figuramc.figura.model.texture.AvatarTexture;
import org.figuramc.figura.script_hooks.ScriptError;
import org.figuramc.figura.util.FiguraMatrixStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Special model part that exists at the root only.
 * Is responsible for controlling the optimized rendering system.
 */
public class RootModelPart extends FiguraModelPart {

    // These fields are only relevant for optimized rendering mode!
    private boolean needsRebuild;
    private @Nullable OptimizedBufferBuilder optimizedBufferBuilder;
    private @Nullable PartDataStorageBuffer partDataStorageBuffer;

    public RootModelPart(AvatarMaterials.ModelPartMaterials materials, List<AvatarTexture> textures, boolean forceCompatible) {
        super(materials, textures, null, forceCompatible);
        // If we're in an optimized context, create some additional resources to facilitate that rendering
        if (!forceCompatible && RenderingMode.isOptimized()) {
            needsRebuild = true;
            optimizedBufferBuilder = new OptimizedBufferBuilder();
            partDataStorageBuffer = new PartDataStorageBuffer();
        }
    }

    // Root model part by extruding a texture
    public RootModelPart(String name, AvatarTexture texture, boolean forceCompatible) {
        super(name, texture, forceCompatible);
        // If we're in an optimized context, create some additional resources to facilitate that rendering
        if (!forceCompatible && RenderingMode.isOptimized()) {
            needsRebuild = true;
            optimizedBufferBuilder = new OptimizedBufferBuilder();
            partDataStorageBuffer = new PartDataStorageBuffer();
        }
    }

    // Render using compatible/immediate mode (compute matrix transforms on CPU, push all vertices, etc.)
    public void renderImmediate(MultiBufferSource bufferSource, FiguraMatrixStack matrixStack, float tickDelta, int light, int overlay) throws StackOverflowError, ScriptError {
        List<RenderType> testRenderTypes = List.of(RenderType.entityCutout(ResourceLocation.withDefaultNamespace("textures/entity/creeper/creeper.png"))); // aww man
        renderImmediate(bufferSource, testRenderTypes, 0, matrixStack, tickDelta, light, overlay);
    }

    // Render using optimized mode
    public void renderOptimized(FiguraMatrixStack baseMatrixStack, float tickDelta) throws StackOverflowError, ScriptError {
        if (!RenderingMode.isOptimized() || optimizedBufferBuilder == null || partDataStorageBuffer == null)
            throw new IllegalStateException("Attempt to call renderOptimized() while RenderingMode is not optimized - bug in Figura, please report!");
        // If we need a rebuild, then do it:
        if (needsRebuild) {
            optimizedBufferBuilder.clear();
            MutableInt currentPartId = new MutableInt();
            try {
                this.buildOptimizedBuffers(optimizedBufferBuilder, List.of(), 0, currentPartId);
            } catch (RuntimeException | Error t) {
                optimizedBufferBuilder.clear();
                throw t;
            }
            optimizedBufferBuilder.build();
            int partCount = currentPartId.getValue();
            partDataStorageBuffer.build(partCount);
            needsRebuild = false;
        }
        // Now, let's do the per-frame render operation: update the part data, bind it, then draw.
        partDataStorageBuffer.updatePartData(partData -> {
            this.calculateOptimizedTransforms(partData, new FiguraMatrixStack(), tickDelta, false, true, new MutableInt(0));
        });
        partDataStorageBuffer.bind();
        optimizedBufferBuilder.draw(baseMatrixStack);
    }

    // Call before GC'ing this part, or else we have a memory leak!
    public void destroy() {
        if (optimizedBufferBuilder != null) optimizedBufferBuilder.clear();
        if (partDataStorageBuffer != null) partDataStorageBuffer.clear();
    }

    // // // // // //           // // // // // //
    // // // // // // SCRIPTING // // // // // //
    // // // // // //           // // // // // //

}
