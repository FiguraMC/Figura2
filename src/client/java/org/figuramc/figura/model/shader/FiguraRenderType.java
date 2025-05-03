package org.figuramc.figura.model.shader;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Algebraic Data Type for all Figura render types.
 * Each model part has a render type and a priority for inheritance.
 * FiguraPartRenderer instances will need to figure out how to handle these.
 */
public sealed interface FiguraRenderType {

    /**
     * Get render types for the compatible backend
     */
    List<RenderType> compatibleRenderTypes();

    /**
     * Basic rendering setup. Most commonly used.
     */
    final class Basic implements FiguraRenderType {

        public final @Nullable ResourceLocation mainTex, emissiveTex;

        public Basic(@Nullable ResourceLocation mainTex, @Nullable ResourceLocation emissiveTex) {
            this.mainTex = mainTex;
            this.emissiveTex = emissiveTex;
        }

        private ArrayList<RenderType> compatibleCache = null;

        // Compatible mode: return a list of all the render types to use to draw this, in order
        @Override
        public synchronized List<RenderType> compatibleRenderTypes() {
            if (compatibleCache == null) {
                compatibleCache = new ArrayList<>(2);
                if (mainTex != null) compatibleCache.add(RenderType.entityTranslucent(mainTex, true));
                if (emissiveTex != null) compatibleCache.add(RenderType.eyes(emissiveTex));
                compatibleCache.trimToSize();
            }
            return compatibleCache;
        }
    }

//    /**
//     * A rendering type that uses a custom shader.
//     * Not all rendering backends need to support this.
//     */
//    record CustomShader(
//            FiguraCustomShader customShader
//    ) implements FiguraRenderType {}

    /**
     * One-off, non-customizable render types, with global INSTANCE objects.
     */
    final class EndPortal implements FiguraRenderType {
        public static final EndPortal INSTANCE = new EndPortal();
        private EndPortal() {}

        // Compatible mode: Just return the vanilla end portal
        private static final List<RenderType> compatibleRenderTypes = List.of(RenderType.endPortal());
        @Override public List<RenderType> compatibleRenderTypes() {
            return compatibleRenderTypes;
        }
    }

    final class EndGateway implements FiguraRenderType {
        public static final EndGateway INSTANCE = new EndGateway();
        private EndGateway() {}

        // Compatible mode: Just return the vanilla end gateway
        private static final List<RenderType> compatibleRenderTypes = List.of(RenderType.endGateway());
        @Override public List<RenderType> compatibleRenderTypes() {
            return compatibleRenderTypes;
        }
    }

}
