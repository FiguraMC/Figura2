package org.figuramc.figura.avatars.components;

import org.figuramc.figura.avatars.Avatar;
import org.figuramc.figura.avatars.AvatarComponent;
import org.figuramc.figura.data.AvatarMaterials;
import org.figuramc.figura.manage.AvatarLoadingException;
import org.figuramc.figura.script_hooks.ScriptEngine;
import org.figuramc.figura.util.exception.ThrowingRunnable;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public class Scripts implements AvatarComponent {

    // The ScriptEngine which is in use, or none if no script engine
    private @Nullable ScriptEngine engine;
    // The avatar
    private Avatar<?> self;

    @Override
    public void initialize(AvatarMaterials materials, Avatar<?> self) throws AvatarLoadingException {
        this.self = self;
        this.engine = null; // TODO, add an engine :P
        // TODO Add built-in APIs to the engine:
    }

    /**
     * Justification: The script instance must be created before any other components
     * can add APIs to it. However, all other components must register their APIs before
     * we can add the user's scripts to the instance.
     */
    @Override
    public void postInitialize(AvatarMaterials materials, Avatar<?> self) throws AvatarLoadingException {
        // TODO Add the user's scripts into the engine
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

    @Override
    public boolean mainThreadInitialize() {
        return tryOrError(() -> {
            if (engine != null) engine.init();
        }, "Error during script init");
    }

    @Override
    public boolean tick() {
        return tryOrError(() -> {
            if (engine != null) engine.tick();
        }, "Error during script tick");
    }

    public void renderEvent(float tickDelta) {
        tryOrError(() -> {
            if (engine != null) engine.render(tickDelta);
        }, "Error during script render");
    }

}
