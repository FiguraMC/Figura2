package org.figuramc.figura.model.renderers;

import org.figuramc.figura.config.ConfigManager;
import net.fabricmc.loader.api.FabricLoader;
import org.figuramc.figura.model.renderers.compatible.CompatibleRenderer;
import org.figuramc.figura.model.renderers.vanilla_optimized.VanillaOptimizedRenderer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Methods for getting/setting the current renderer.
 *
 * Important note for design: The return value of `getCurrentRenderer()` is intended
 * to never change while an avatar is in use.
 * Hence, when the value is changed, all avatars are immediately cleared out.
 *
 * As a result, it *should* be safe to assume, for any given avatar, that
 * `getCurrentRenderer()` returns the same result every time. If this is not the case, there's
 * a bug!
 */
public class FiguraRenderers {

    // The current renderer, or null if it hasn't been decided on yet.
    private static @Nullable FiguraPartRenderer currentRenderer;

    // Fetch the current renderer. Called many times, so it caches.
    public static @NotNull FiguraPartRenderer getCurrentRenderer() {
        // If we have it cached, we're done.
        if (currentRenderer != null) return currentRenderer;
        // If we force compatible mode, use compatible mode.
        if (ConfigManager.FORCE_COMPATIBLE_MODE.getValue())
            return currentRenderer = CompatibleRenderer.INSTANCE;
        // If we have iris installed, use compatible mode (for now)
        if (FabricLoader.getInstance().isModLoaded("iris"))
            return currentRenderer = CompatibleRenderer.INSTANCE;
        // Otherwise, return vanilla optimized mode.
        return currentRenderer = VanillaOptimizedRenderer.INSTANCE;
    }

}
