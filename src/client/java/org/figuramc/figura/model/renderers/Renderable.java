package org.figuramc.figura.model.renderers;

import org.figuramc.figura.model.part.FiguraModelPart;
import org.figuramc.figura.script_hooks.mem_count.MarkedObjectBase;
import org.figuramc.figura.script_hooks.mem_count.MemoryCounter;

/**
 * A wrapper around a model part and a renderer for it
 */
public class Renderable<T extends FiguraModelPart> extends MarkedObjectBase {

    public final T part;
    public final FiguraModelPartRenderer renderer;

    public Renderable(T part) {
        this.part = part;
        this.renderer = FiguraRenderingBackends.getCurrentBackend().createRenderer(part);
    }

    public void destroy() {
        this.renderer.destroy();
    }

    @Override
    protected long traceNoMark(MemoryCounter counter, int depth) {
        counter.trace(part, depth);
        counter.trace(renderer, depth);
        return OBJECT_SIZE;
    }
}
