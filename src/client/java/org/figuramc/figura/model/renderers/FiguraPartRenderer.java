package org.figuramc.figura.model.renderers;

import net.minecraft.client.renderer.MultiBufferSource;
import org.figuramc.figura.model.part.RootModelPart;
import org.figuramc.figura.script_hooks.ScriptError;
import org.figuramc.figura.script_hooks.mem_count.MemoryCountable;
import org.figuramc.figura.util.FiguraTransformStack;

/**
 * A renderer which can render a part. One exists per rendering backend.
 */
public interface FiguraPartRenderer {

    /**
     * Render the part, using the given context information.
     */
    void render(
            RootModelPart root,
            MultiBufferSource bufferSource,
            FiguraTransformStack matrixStack,
            float tickDelta,
            int light, int overlay
    ) throws ScriptError, StackOverflowError;
    
    /**
     * State for the renderer, must be memory-countable and destroyable.
     * Store it in the RootModelPart if necessary.
     */
    interface RootState extends MemoryCountable {
        /**
         * When the root is destroyed, invoke this.
         */
        void destroy();
    }

    /**
     * Optionally stored in every FiguraModelPart.
     * No destroy(), because we can't necessarily guarantee it will be called on normal parts, depending on scripts.
     * So these objects should not use any native resources that need to be closed!
     */
    interface NonRootState extends MemoryCountable {}

}
