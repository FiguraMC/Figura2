package org.figuramc.figura.avatars.components;

import net.minecraft.network.chat.Component;
import org.figuramc.figura.avatars.Avatar;
import org.figuramc.figura.avatars.AvatarComponent;
import org.figuramc.figura.avatars.AvatarError;
import org.figuramc.figura.avatars.AvatarModules;
import org.figuramc.figura.manage.AvatarLoadingException;
import org.figuramc.figura.script_hooks.Event;
import org.figuramc.figura.script_hooks.EventListener;
import org.figuramc.figura.script_hooks.ScriptError;
import org.figuramc.figura.script_hooks.ScriptRuntime;
import org.figuramc.figura.script_hooks.mem_count.AllocationTracker;
import org.figuramc.figura.script_languages.lua.LuaRuntime;
import org.figuramc.figura.util.enumlike.IdMap;
import org.jetbrains.annotations.Nullable;

import java.util.*;

// The Scripts component should generally be at or near the END of the list of components.
// Other components probably should not rely on Scripts, and instead Scripts should rely on them.
public class Scripts implements AvatarComponent<Scripts> {

    // It's important that all possible dependencies be mentioned here!
    public static final Type<Scripts> TYPE = new Type<>(EntityRoot.TYPE, EntityUser.TYPE, VanillaRendering.TYPE);
    public Type<Scripts> getType() { return TYPE; }

    // Listeners for built-in events, indexed using the event's ID.
    public IdMap<Event, EventListener> eventListeners;

    // The Script Runtimes which are in use by this avatar.
    private List<ScriptRuntime> scriptRuntimes;
    private AvatarModules modules;

    // The avatar, set during initialize()
    private Avatar<?> self;

    // Optional dependency components
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
        this.modules = modules;
        this.eventListeners = new IdMap<>(Event.class, e -> new EventListener(e.type.paramTypes()));
        this.scriptRuntimes = new ArrayList<>(setupRuntimes(modules).values());
    }

    public @Nullable AllocationTracker getAllocationTracker() {
        return self.getAllocationTracker();
    }

    // Runs once on init
    @Override
    public void mainThreadInitialize() {
        try {
            modules.mainModule().initScript();
        } catch (ScriptError ex) {
            self.error(new AvatarError("figura.error.runtime.script.script_error", ex, true, Component.translatable("figura.error.runtime.script.stage.init")));
        } catch (Throwable ex) {
            self.error(new AvatarError("figura.error.runtime.script.unexpected_error", ex, false, Component.translatable("figura.error.runtime.script.stage.init")));
        }
    }

    public void runEvent(Event event, Object... args) {
        try {
            // If there's nothing registered to this event, quit.
            if (event.id >= eventListeners.size()) return;
            // Fetch the event listener and invoke it.
            eventListeners.get(event).invoke(args);
        } catch (ScriptError ex) {
            self.error(new AvatarError("figura.error.runtime.script.script_error", ex, true, Component.translatable("figura.error.runtime.script.stage.event", event.name)));
        } catch (Throwable ex) {
            self.error(new AvatarError("figura.error.runtime.script.unexpected_error", ex, false, Component.translatable("figura.error.runtime.script.stage.event", event.name)));
        }
    }

    // Runs every tick
    @Override
    public void tick() {
        runEvent(Event.CLIENT_TICK);
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
    private Map<String, ScriptRuntime> setupRuntimes(AvatarModules modules) throws AvatarLoadingException {
        Map<String, ScriptRuntime> runtimes = new TreeMap<>();
        for (AvatarModules.Module module : modules.modules) {
            String lang = module.materials.metadata().language();
            if (lang == null) continue;
            if (!runtimes.containsKey(lang)) {
                RuntimeCreator creator = RUNTIME_CREATORS.get(lang);
                if (creator == null) throw new AvatarLoadingException("figura.error.loading.script.unknown_language", lang);
                runtimes.put(lang, creator.createRuntime(this, modules));
            }
            module.runtime = runtimes.get(lang);
        }
        return runtimes;
    }

}
