package org.figuramc.figura.avatars.components;

import org.figuramc.figura.avatars.Avatar;
import org.figuramc.figura.avatars.AvatarComponent;
import org.figuramc.figura.data.AvatarMaterials;
import org.figuramc.figura.manage.AvatarLoadingException;
import org.figuramc.figura.model.part.*;
import org.figuramc.figura.script_hooks.ScriptRuntime;
import org.figuramc.figura.util.exception.ThrowingRunnable;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public class Scripts implements AvatarComponent {

    // The ScriptEngine which is in use, or none if no script engine
    private @Nullable ScriptRuntime engine;
    // The avatar
    private Avatar<?> self;

    @Override
    public void initialize(AvatarMaterials materials, Avatar<?> self) throws AvatarLoadingException {
        this.self = self;
        this.engine = null; // TODO, add an engine so this isnt null :P

        if (engine != null) {
            // Register built-in APIs:

            // Model parts
            engine.registerClass(FiguraModelPart.class);
            engine.registerClass(RootModelPart.class);
            engine.registerClass(CustomItemModelPart.class);
            engine.registerClass(WorldRootModelPart.class);
            engine.registerClass(VanillaRootModelPart.class);

            // And finally, add in the user's scripts:
            for (AvatarMaterials.ScriptMaterials script : materials.scripts())
                engine.addScript(script.name(), script.data());
        }
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
            if (engine != null) engine.init();
        }, "Error during script init");
    }

    // Runs every tick
    @Override
    public boolean tick() {
        return tryOrError(() -> {
            if (engine != null) engine.tick();
        }, "Error during script tick");
    }

    // Runs every frame
    public void renderEvent(float tickDelta) {
        tryOrError(() -> {
            if (engine != null) engine.render(tickDelta);
        }, "Error during script render");
    }

}
