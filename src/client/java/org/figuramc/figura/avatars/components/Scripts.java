package org.figuramc.figura.avatars.components;

import com.demonwav.mcdev.annotations.Translatable;
import net.minecraft.network.chat.Component;
import org.figuramc.figura.avatars.Avatar;
import org.figuramc.figura.avatars.AvatarComponent;
import org.figuramc.figura.avatars.AvatarError;
import org.figuramc.figura.avatars.AvatarModules;
import org.figuramc.figura.manage.AvatarLoadingException;
import org.figuramc.figura.script_hooks.ScriptError;
import org.figuramc.figura.script_hooks.ScriptRuntime;
import org.figuramc.figura.script_hooks.mem_count.AllocationTracker;
import org.figuramc.figura.script_languages.lua.LuaRuntime;
import org.figuramc.figura.util.exception.functional.ThrowingRunnable;
import org.jetbrains.annotations.Nullable;

import java.util.*;

// The Scripts component should generally be at or near the END of the list of components.
// Other components probably should not rely on Scripts, and instead Scripts should rely on them.
public class Scripts implements AvatarComponent {

    // It's important that all possible dependents be mentioned here!
    public static final int ID = AvatarComponent.createId(EntityRoot.class, EntityUser.class, VanillaRendering.class);
    public int getId() { return ID; }

    // The Script Runtimes which are in use by this avatar.
    private List<ScriptRuntime> scriptRuntimes;

    // The avatar, set during initialize()
    private Avatar<?> self;

    // Optional dependent components
    public @Nullable EntityRoot entityRoot;
    public @Nullable EntityUser entityUser;
    public @Nullable VanillaRendering vanillaRendering;

    public Scripts(@Nullable EntityRoot entityRoot, @Nullable EntityUser entityUser, @Nullable VanillaRendering vanillaRendering) {
        this.entityRoot = entityRoot;
        this.entityUser = entityUser;
        this.vanillaRendering = vanillaRendering;
    }

    @Override
    public void initialize(AvatarModules modules, Avatar<?> self) throws AvatarLoadingException {
        this.self = self;
        this.scriptRuntimes = setupRuntimes(modules);
    }

    public @Nullable AllocationTracker getAllocationTracker() { return self.getAllocationTracker(); }

    // Helper to try running code, catch any error, and error out the Avatar if so
    private boolean tryOrError(ThrowingRunnable<ScriptError> runnable, @Translatable String stage) {
        try {
            runnable.run();
            // If successful, return false
            return false;
        } catch (ScriptError ex) {
            self.error(new AvatarError("figura.error.runtime.script.script_error", ex, true, Component.translatable(stage)));
        } catch (Throwable ex) {
            self.error(new AvatarError("figura.error.runtime.script.unexpected_error", ex, false, Component.translatable(stage)));
        }
        return true;
    }

    // Runs once on init
    @Override
    public boolean mainThreadInitialize() {
        return tryOrError(() -> {
            for (var runtime : scriptRuntimes) runtime.init();
        }, "figura.error.runtime.script.stage.init");
    }

    // Runs every tick
    @Override
    public boolean tick() {
        return tryOrError(() -> {
            for (var runtime : scriptRuntimes) runtime.tick();
        }, "figura.error.runtime.script.stage.tick");
    }

    // Runs every frame
    public void renderEvent(float tickDelta) {
        tryOrError(() -> {
            for (var runtime : scriptRuntimes) runtime.render(tickDelta);
        }, "figura.error.runtime.script.stage.render");
    }

    @Override
    public void destroy() {
        for (var runtime : scriptRuntimes) runtime.destroy();
    }

    // TODO expose this as an API to mods/addons in an official way, so they can add languages
    @FunctionalInterface
    public interface RuntimeCreator {
        ScriptRuntime createRuntime(Scripts scriptsComponent, AvatarModules allModules) throws AvatarLoadingException;
    }
    public static final Map<String, RuntimeCreator> RUNTIME_CREATORS = new HashMap<>();
    static {
        RUNTIME_CREATORS.put("lua", LuaRuntime::new);
    }

    // Determine which runtimes these modules use, and create them accordingly
    private List<ScriptRuntime> setupRuntimes(AvatarModules modules) throws AvatarLoadingException {
        List<ScriptRuntime> runtimes = new ArrayList<>();
        Set<String> done = new HashSet<>();
        for (AvatarModules.Module module : modules.modules) {
            String lang = module.materials.metadata().language();
            if (lang == null) continue;
            if (!done.add(lang)) continue;
            RuntimeCreator creator = RUNTIME_CREATORS.get(lang);
            if (creator == null) throw new AvatarLoadingException("figura.error.loading.script.unknown_language", lang);
            runtimes.add(creator.createRuntime(this, modules));
        }
        return runtimes;
    }

}
