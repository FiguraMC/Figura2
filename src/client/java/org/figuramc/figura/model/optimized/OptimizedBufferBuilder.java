package org.figuramc.figura.model.optimized;

import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import org.figuramc.figura.util.FiguraMatrixStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

/**
 * - Repeatedly call getBuffer() for BufferBuilders, and put vertices into them
 * - Call .build()
 * - May repeatedly call .draw() to draw all buffers.
 * - Once you call .clear(), you may restart the process.
 *   Make sure clear() is called before this is GC'ed, or else memory leak
 */
public class OptimizedBufferBuilder implements MultiBufferSource {

    private final Map<RenderType, BufferState> buffers = new HashMap<>();
    private boolean built;

    @Override
    public VertexConsumer getBuffer(RenderType renderType) {
        if (built) throw new IllegalStateException("Figura BufferSource was already built!");
        return buffers.computeIfAbsent(renderType, t -> {
            ByteBufferBuilder byteBufferBuilder = new ByteBufferBuilder(1024);
            BufferBuilder bufferBuilder = new BufferBuilder(byteBufferBuilder, VertexFormat.Mode.QUADS, OptimizedRendering.OPTIMIZED_FORMAT);
            VertexBuffer vertexBuffer = new VertexBuffer(BufferUsage.STATIC_WRITE); // We live in hope
            return new BufferState(byteBufferBuilder, bufferBuilder, vertexBuffer);
        }).bufferBuilder();
    }

    public void build() {
        if (built) throw new IllegalStateException("Figura BufferSource was already built!");
        built = true;
        // Upload all buffer builders into their corresponding vertex buffers
        for (BufferState buffer : buffers.values()) {
            MeshData meshData = buffer.bufferBuilder.build();
            buffer.vertexBuffer.bind();
            buffer.vertexBuffer.upload(meshData);
        }
    }

    public void draw(FiguraMatrixStack baseMatrixStack) {
        if (!built) throw new IllegalStateException("Figura BufferSource not yet built!");
        for (var entry : buffers.entrySet()) {
            entry.getKey().setupRenderState();
            entry.getValue().vertexBuffer.bind();
            // Cursed uniform setting
            Matrix4f figuraRootMatrix = baseMatrixStack.peekPosition();
            RenderSystem.getShader().getUniform("FiguraRootMatrix").set(figuraRootMatrix);
            entry.getValue().vertexBuffer.drawWithShader(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
            entry.getKey().clearRenderState();
        }
    }

    // Reset the object, and free memory
    public void clear() {
        for (BufferState buffer : buffers.values()) {
            // Close native resources
            buffer.byteBufferBuilder.close();
            buffer.vertexBuffer.close();
        }
        buffers.clear();
        built = false;
    }

    private record BufferState(ByteBufferBuilder byteBufferBuilder, BufferBuilder bufferBuilder, VertexBuffer vertexBuffer) {}
}
