package org.figuramc.figura.model.renderers.compatible;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.figuramc.figura.model.part.FiguraModelPart;
import org.figuramc.figura.model.part.RootModelPart;
import org.figuramc.figura.model.renderers.FiguraPartRenderer;
import org.figuramc.figura.model.shader.FiguraRenderType;
import org.figuramc.figura.script_hooks.ScriptError;
import org.figuramc.figura.script_hooks.mem_count.MarkedObjectBase;
import org.figuramc.figura.script_hooks.mem_count.MemoryCounter;
import org.figuramc.figura.util.FiguraTransformStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

public class CompatibleRenderer implements FiguraPartRenderer {

    public static final CompatibleRenderer INSTANCE = new CompatibleRenderer();
    private CompatibleRenderer() {}

    @Override
    public void render(
            RootModelPart root,
            MultiBufferSource bufferSource,
            FiguraTransformStack matrixStack,
            float tickDelta,
            int light, int overlay
    ) throws ScriptError, StackOverflowError {
        recursiveRender(root, bufferSource, List.of(), 0, matrixStack, tickDelta, light, overlay);
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
    ) throws ScriptError, StackOverflowError {

        part.invokeCallbacks(part.preRenderCallbacks, tickDelta);

        // Cancel if not invisible
        if (!part.transform.getVisible())
            return;

        // Update render types if we can and this has priority
        if (part.getRenderType() != null && part.renderTypePriority >= renderTypePriority) {
            // If it already has a cached render type, use it! Otherwise, compute and cache.
            @Nullable State partState = (State) part.nonRootRenderState;
            if (partState == null) {
                // Create the Minecraft RenderTypes by reading the part's figura render type.
                List<RenderType> types = switch (part.getRenderType()) {
                    case FiguraRenderType.EndPortal p -> List.of(RenderType.endPortal());
                    case FiguraRenderType.EndGateway g -> List.of(RenderType.endGateway());
                    case FiguraRenderType.Basic(@Nullable ResourceLocation mainTex, @Nullable ResourceLocation emissiveTex) -> {
                        List<RenderType> layers = new ArrayList<>();
                        if (mainTex != null) layers.add(RenderType.entityTranslucent(mainTex, true));
                        if (emissiveTex != null) layers.add(RenderType.eyes(emissiveTex));
                        yield layers;
                    }
                };
                part.nonRootRenderState = partState = new State(types);
            }
            currentRenderTypes = partState.minecraftRenderTypes;
            renderTypePriority = part.renderTypePriority;
        }

        // Push matrix stack, apply transforms
        matrixStack.push();
        part.transform.affect(matrixStack);
        // TODO Animations
//        for (Animator animator : animators)
//            animator.affect(matrixStack);

        part.invokeCallbacks(part.midRenderCallbacks, tickDelta);

        // Render children recursively
        for (FiguraModelPart child : part.children)
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

        part.invokeCallbacks(part.postRenderCallbacks, tickDelta);

        // Pop matrix stack
        matrixStack.pop();
    }

    public static class State extends MarkedObjectBase implements NonRootState {

        public final List<RenderType> minecraftRenderTypes;

        public State(List<RenderType> types) {
            this.minecraftRenderTypes = types;
        }

        @Override
        protected long traceNoMark(MemoryCounter counter, int depth) {
            throw new UnsupportedOperationException("TODO");
        }
    }


}
