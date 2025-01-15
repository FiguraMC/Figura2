package org.figuramc.figura.model.shader;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Algebraic Data Type for all Figura render types.
 * Each model part has a render type and a priority for inheritance.
 * FiguraPartRenderer instances will need to figure out how to handle these.
 */
public sealed interface FiguraRenderType {

    /**
     * Basic rendering setup. Customizable, most commonly used.
     */
    record Basic(
            @Nullable ResourceLocation mainTex,
            @Nullable ResourceLocation emissiveTex
    ) implements FiguraRenderType {}

    /**
     * One-off, non-customizable render types, with global INSTANCE objects.
     */
    final class EndPortal implements FiguraRenderType { public static final EndPortal INSTANCE = new EndPortal(); private EndPortal() {} }
    final class EndGateway implements FiguraRenderType { public static final EndGateway INSTANCE = new EndGateway(); private EndGateway() {} }

}
