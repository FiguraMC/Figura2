package org.figuramc.figura.model.renderers.compatible;

import org.figuramc.figura.model.part.FiguraModelPart;
import org.figuramc.figura.model.renderers.FiguraModelPartRenderer;
import org.figuramc.figura.model.renderers.FiguraRenderingBackend;

public class CompatibleBackend implements FiguraRenderingBackend {

    public static final CompatibleBackend INSTANCE = new CompatibleBackend();
    private CompatibleBackend() {}

    @Override
    public FiguraModelPartRenderer createRenderer(FiguraModelPart part) {
        return new CompatibleRenderer(part);
    }
}
