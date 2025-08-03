package org.figuramc.figura.model.renderers;

import net.fabricmc.loader.api.FabricLoader;
import org.figuramc.figura.config.ConfigManager;
import org.figuramc.figura.model.renderers.compatible.CompatibleBackend;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Methods for getting/setting the current default rendering backend.
 * Unknown if this will be useful in the future, since it might be decided
 * in another more granular way than just a global variable.
 */
public class FiguraRenderingBackends {

    // The current backend, or null if it hasn't been decided on yet.
    private static @Nullable FiguraRenderingBackend currentBackend;

    // Fetch the current backend. Called many times, so it caches.
    public static @NotNull FiguraRenderingBackend getCurrentBackend() {
        // If we have it cached, we're done.
        if (currentBackend != null) return currentBackend;
        // If we force compatible mode, use compatible mode.
        if (ConfigManager.FORCE_COMPATIBLE_MODE.getValue())
            return currentBackend = CompatibleBackend.INSTANCE;
        // If we have iris installed, use compatible mode (for now)
        if (FabricLoader.getInstance().isModLoaded("iris"))
            return currentBackend = CompatibleBackend.INSTANCE;
        // Otherwise, TODO: return vanilla optimized mode.
        return currentBackend = CompatibleBackend.INSTANCE;
    }

}
