package org.figuramc.figura.model.renderers;

import org.figuramc.figura.model.part.FiguraModelPart;

/**
 * A rendering backend, which can create renderers for Figura model parts.
 */
public interface FiguraRenderingBackend {

    /**
     * Create a renderer that can render the given part.
     * This may be an expensive operation!
     */
    FiguraModelPartRenderer createRenderer(FiguraModelPart part);

}
