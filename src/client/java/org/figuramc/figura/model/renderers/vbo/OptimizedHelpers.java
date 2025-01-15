package org.figuramc.figura.model.renderers.vbo;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.RenderType;
import org.apache.commons.lang3.mutable.MutableInt;
import org.figuramc.figura.model.part.FiguraModelPart;
import org.figuramc.figura.model.part.RootModelPart;
import org.figuramc.figura.script_hooks.ScriptError;
import org.figuramc.figura.util.FiguraMatrixStack;
import org.joml.Vector4f;

import java.util.List;
import java.util.function.Function;

/**
 * Static helper methods for using the optimized objects.
 */
public class OptimizedHelpers {

    // Rebuild the buffer builder and storage buffer
    public static void rebuild(RootModelPart root, OptimizedBufferBuilder optimizedBufferBuilder, PartDataStorageBuffer partDataStorageBuffer, Function<FiguraModelPart, List<RenderType>> renderTypeGetter) {
        optimizedBufferBuilder.clear();
        MutableInt currentPartId = new MutableInt();
        try {
            buildOptimizedBuffers(root, optimizedBufferBuilder, List.of(), 0, renderTypeGetter, currentPartId);
        } catch (RuntimeException | Error t) {
            optimizedBufferBuilder.clear();
            throw t;
        }
        optimizedBufferBuilder.build();
        int partCount = currentPartId.getValue();
        partDataStorageBuffer.build(partCount);
    }

    // Private helper method for recursing through model parts and building optimized buffers.
    private static void buildOptimizedBuffers(FiguraModelPart part, OptimizedBufferBuilder bufferSource, List<RenderType> renderTypes, int renderTypePriority, Function<FiguraModelPart, List<RenderType>> renderTypeGetter, MutableInt currentId) {
        // Update render types if we can and this has priority
        if (part.renderType != null && part.renderTypePriority >= renderTypePriority) {
            renderTypes = renderTypeGetter.apply(part);
            renderTypePriority = part.renderTypePriority;
        }
        // Fetch the part ID
        int partId = currentId.getAndIncrement();
        // If this has vertices, send them in
        if (part.vertices.length > 0) {
            float[] vertices = part.vertices;
            for (RenderType renderType : renderTypes) {
                VertexConsumer consumer = bufferSource.getBuffer(renderType);
                for (int i = 0; i < vertices.length; i += 16) {
                    // Pos, Normal, and UV are all as usual
                    consumer.addVertex(vertices[i], vertices[i+1], vertices[i+2])
                            .setNormal(vertices[i+3], vertices[i+4], vertices[i+5])
                            .setUv(vertices[i+6], vertices[i+7])
                            // UV1 and UV2 contain part IDs:
                            .setUv1(vertices[i+8] == -1 ? -1 : (partId + (int) vertices[i+8]), vertices[i+9] == -1 ? -1 : (partId + (int) vertices[i+9]))
                            .setUv2(vertices[i+10] == -1 ? -1 : (partId + (int) vertices[i+10]), vertices[i+11] == -1 ? -1 : (partId + (int) vertices[i+11]))
                            // Color contains the weights for the 4 part IDs.
                            .setColor(vertices[i+12], vertices[i+13], vertices[i+14], vertices[i+15]);
                }
            }
        }
        // Recurse on children
        for (FiguraModelPart child : part.children)
            buildOptimizedBuffers(child, bufferSource, renderTypes, renderTypePriority, renderTypeGetter, currentId);
    }

    // Helper method to update transforms for a given root and store them in the PartDataStorageBuffer.
    public static void updateTransforms(RootModelPart root, PartDataStorageBuffer partDataStorageBuffer, float tickDelta) throws StackOverflowError, ScriptError {
        partDataStorageBuffer.updatePartData(partData -> {
            calculateTransforms(root, partData, new FiguraMatrixStack(), tickDelta, false, true, new MutableInt(0));
        });
    }

    // Private recursive helper to traverse parts and calculate their transforms.
    private static void calculateTransforms(
            FiguraModelPart part,
            PartDataStorageBuffer.StorageBufferUpdater partData,
            FiguraMatrixStack matrixStack,
            float tickDelta,
            boolean currentlyDirty, boolean currentlyVisible,
            MutableInt currentId
    ) throws StackOverflowError, ScriptError {
        // Pre-render callback
        part.invokeCallbacks(part.preRenderCallbacks, tickDelta);
        // Update visibility
        currentlyVisible = currentlyVisible && part.transform.getVisible();
        // Get new id
        int id = currentId.getAndIncrement();
        // Calculate transforms if needed
        matrixStack.push();
        part.transform.affect(matrixStack);
        // TODO Animations
        // Mid-render callback
        if (currentlyVisible) part.invokeCallbacks(part.midRenderCallbacks, tickDelta);
        // Update the storage buffer updater if this subtree is dirty
        currentlyDirty |= part.transform.fetchDirty();
        if (currentlyDirty) {
            partData.updatePartData(id).fillFromStack(matrixStack, new Vector4f(1.0f), currentlyVisible);
        }
        // Recurse to children, passing down dirt
        for (FiguraModelPart child : part.children)
            calculateTransforms(child, partData, matrixStack, tickDelta, currentlyDirty, currentlyVisible, currentId);
        // Post-render callback
        if (currentlyVisible) part.invokeCallbacks(part.postRenderCallbacks, tickDelta);
        // Pop matrix stack
        matrixStack.pop();
    }

}
