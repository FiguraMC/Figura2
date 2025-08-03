package org.figuramc.figura.model.renderers.compatible;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.figuramc.figura.model.part.FiguraModelPart;
import org.figuramc.figura.model.renderers.FiguraModelPartRenderer;
import org.figuramc.figura.script_hooks.callback.items.CallbackItem;
import org.figuramc.figura.util.FiguraTransformStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.List;

public class CompatibleRenderer implements FiguraModelPartRenderer {

    private final FiguraModelPart root;

    public CompatibleRenderer(FiguraModelPart root) {
        this.root = root;
    }

    @Override
    public void render(
            MultiBufferSource bufferSource,
            FiguraTransformStack matrixStack,
            float tickDelta,
            int light, int overlay
    ) throws StackOverflowError {
        recursiveRender(root, bufferSource, List.of(), 0, matrixStack, tickDelta, light, overlay);
    }

    @Override
    public void destroy() {
        // Nothing to destroy
    }

    private void recursiveRender(
            FiguraModelPart part, // The part being rendered
            MultiBufferSource bufferSource, // The source for render buffers
            List<RenderType> currentRenderTypes, // The currently cached render types which vertices will be passed to
            int renderTypePriority, // The current render type priority. The render type may only be changed if part.renderTypePriority >= renderTypePriority.
            FiguraTransformStack matrixStack, // The current matrix stack
            float tickDelta,
            int light,
            int overlay
    ) throws StackOverflowError {

        for (var callback : part.preRenderCallbacks) callback.call(new CallbackItem.F32(tickDelta));

        // Cancel if not invisible
        if (!part.transform.getVisible())
            return;

        // Update render types if we can and this has priority
        if (part.getRenderType() != null && part.renderTypePriority >= renderTypePriority) {
            currentRenderTypes = part.getRenderType().compatibleRenderTypes();
            renderTypePriority = part.renderTypePriority;
        }

        // Push matrix stack, apply transforms
        matrixStack.push();
        part.transform.affect(matrixStack);

        for (var callback : part.midRenderCallbacks)
            callback.call(new CallbackItem.F32(tickDelta));

        // Render children recursively
        for (FiguraModelPart child : part.children.values())
            recursiveRender(child, bufferSource, currentRenderTypes, renderTypePriority, matrixStack, tickDelta, light, overlay);

        // If this has vertices, send them in
        if (part.vertices.length > 0) {
            Vector4f pos = new Vector4f();
            Vector3f norm = new Vector3f();
            Matrix4f posMatrix = matrixStack.peekPosition();
            Matrix3f normalMatrix = matrixStack.peekNormal();
            Vector4f color = matrixStack.peekColor();
            float[] vertices = part.vertices;
            for (RenderType renderType : currentRenderTypes) {
                VertexConsumer consumer = bufferSource.getBuffer(renderType);
                for (int i = 0; i < vertices.length; i += 16) {
                    pos.set(vertices[i], vertices[i+1], vertices[i+2], 1.0).mul(posMatrix);
                    norm.set(vertices[i+3], vertices[i+4], vertices[i+5]).mul(normalMatrix);

                    // Skinning is ignored in compatible mode (TODO add, even if not performant)
                    consumer.addVertex(pos.x, pos.y, pos.z)
                            .setColor(color.x, color.y, color.z, color.w)
                            .setUv(vertices[i+6], vertices[i+7])
                            .setNormal(norm.x, norm.y, norm.z)
                            .setLight(light)
                            .setOverlay(overlay);
                }
            }
        }

        for (var callback : part.postRenderCallbacks) callback.call(new CallbackItem.F32(tickDelta));

        // Pop matrix stack
        matrixStack.pop();
    }

}
