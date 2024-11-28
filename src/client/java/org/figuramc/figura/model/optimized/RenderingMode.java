package org.figuramc.figura.model.optimized;

import org.figuramc.figura.config.ConfigManager;
import org.figuramc.figura.manage.AvatarManager;
import org.figuramc.figura.manage.AvatarSubManager;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Methods for getting/setting rendering mode.
 *
 * Important note for design: The return value of `isOptimized()` is intended
 * to never change while an avatar is in use.
 * Hence, when the value is changed, all avatars are immediately cleared out.
 *
 * As a result, it *should* be safe to assume, for any given avatar, that the value
 * `isOptimized()` returns the same result every time. If this is not the case, there's
 * a bug!
 */
public class RenderingMode {

    // This will be called lots of times, so it caches the value
    private static boolean isCached = false;
    private static boolean okayToOptimize = false;
    public static boolean isOptimized() {
        if (!isCached) {
            // If not yet cached, compute whether it's okay to optimize:
            okayToOptimize =
                    !ConfigManager.FORCE_COMPATIBLE_MODE.getValue() && // - Not forcing compat mode
                    !FabricLoader.getInstance().isModLoaded("iris") // - Iris is not loaded
            ;
            isCached = true;
        }
        return okayToOptimize;
    }

    public static void setForceCompatible(boolean value) {
        // Do nothing if the setting doesn't change
        if (ConfigManager.FORCE_COMPATIBLE_MODE.getValue() == value) return;
        // Clear all avatars in existence and start over with a clean slate
        AvatarManager.forEachSubManager(AvatarSubManager::clear);
        // Set the force compat mode, clear the cache
        ConfigManager.FORCE_COMPATIBLE_MODE.setValue(value);
        isCached = false;
    }

}
