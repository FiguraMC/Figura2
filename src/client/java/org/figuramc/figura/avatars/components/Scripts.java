package org.figuramc.figura.avatars.components;

import org.figuramc.figura.avatars.Avatar;
import org.figuramc.figura.avatars.AvatarComponent;
import org.figuramc.figura.data.AvatarMaterials;
import org.figuramc.figura.manage.AvatarLoadingException;
import org.figuramc.figura.model.part.*;
import org.figuramc.figura.script_hooks.ScriptRuntime;
import org.figuramc.figura.script_hooks.ScriptRuntimeType;
import org.figuramc.figura.util.IOUtils;
import org.figuramc.figura.util.exception.ThrowingRunnable;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// The Scripts component should generally be at or near the END of the list of components.
// Other components probably should not rely on Scripts, and instead Scripts should rely on them.
public class Scripts implements AvatarComponent {

    // The Script Runtimes which are in use by this avatar.
    private List<ScriptRuntime> scriptRuntimes;
    // The avatar
    private Avatar<?> self;

    @Override
    public void initialize(AvatarMaterials materials, Avatar<?> self) throws AvatarLoadingException {
        this.self = self;
        this.scriptRuntimes = sortScripts(materials, self);
    }

    // Helper to try running code, catch any error, and error out the Avatar if so
    private boolean tryOrError(ThrowingRunnable<?> runnable, String message) {
        // Try running the code
        try {
            runnable.run();
            return false;
        } catch (Throwable ex) {
            self.error(Component.literal(message), ex); // TODO translate
            return true;
        }
    }

    // Runs once on init
    @Override
    public boolean mainThreadInitialize() {
        return tryOrError(() -> {
            for (var runtime : scriptRuntimes) runtime.init();
        }, "Error during script init");
    }

    // Runs every tick
    @Override
    public boolean tick() {
        return tryOrError(() -> {
            for (var runtime : scriptRuntimes) runtime.tick();
        }, "Error during script tick");
    }

    // Runs every frame
    public void renderEvent(float tickDelta) {
        tryOrError(() -> {
            for (var runtime : scriptRuntimes) runtime.render(tickDelta);
        }, "Error during script render");
    }

    @Override
    public void destroy() {
        for (var runtime : scriptRuntimes) runtime.destroy();
    }

    /**
     * Static helper method that will sort scripts by extension into the proper runtime type,
     * and create the runtimes.
     */
    private static List<ScriptRuntime> sortScripts(AvatarMaterials materials, Avatar<?> avatar) throws AvatarLoadingException {
        Map<ScriptRuntimeType, Map<String, byte[]>> scriptsByType = new HashMap<>();
        for (var script : materials.scripts()) {
            String ext = IOUtils.getExtension(script.name());
            if (ext == null) throw new AvatarLoadingException("Script file \"" + script.name() + "\" has no extension.");
            ScriptRuntimeType runtime = ScriptRuntimeType.TYPE_BY_EXTENSION.get(ext);
            if (runtime == null) throw new AvatarLoadingException("Unrecognized script file extension: \"." + IOUtils.getExtension(script.name()) + "\"");
            // Otherwise, insert it to the result map.
            scriptsByType
                    .computeIfAbsent(runtime, t -> new HashMap<>())
                    .put(script.name(), script.data());
        }
        // Create the list:
        List<ScriptRuntime> runtimes = new ArrayList<>();
        for (var entry : scriptsByType.entrySet())
            runtimes.add(entry.getKey().newRuntime(avatar, entry.getValue()));
        return runtimes;
    }

}
