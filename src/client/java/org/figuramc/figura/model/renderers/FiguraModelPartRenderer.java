package org.figuramc.figura.model.renderers;

import net.minecraft.client.renderer.MultiBufferSource;
import org.figuramc.figura.util.FiguraTransformStack;

/**
 * Something that can render a particular FiguraModelPart, given inputs.
 * - It must be memory-countable, because it's stored by avatars
 * - Must be destroyed, to clean up any native resources such as VBOs
 */
public interface FiguraModelPartRenderer {

    /**
     * Render the model part with the given inputs
     */
    void render(MultiBufferSource bufferSource, FiguraTransformStack matrixStack, float tickDelta, int light, int overlay) throws StackOverflowError, Throwable;

    /**
     * Destroy this renderer, cleaning up any native resources.
     */
    void destroy();

}
