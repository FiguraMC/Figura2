package org.figuramc.figura.model.renderers;

import org.figuramc.figura.model.part.FiguraModelPart;
/**
 * A wrapper around a model part and a renderer for it
 */
public class Renderable<T extends FiguraModelPart> {

    public final T part;
    public final FiguraModelPartRenderer renderer;

    public Renderable(T part) {
        this.part = part;
        this.renderer = FiguraRenderingBackends.getCurrentBackend().createRenderer(part);
    }

    public void destroy() {
        this.renderer.destroy();
    }

}
