package org.figuramc.figura.avatars.components;

import org.figuramc.figura.avatars.Avatar;
import org.figuramc.figura.avatars.AvatarComponent;
import org.figuramc.figura.avatars.AvatarError;
import org.figuramc.figura.avatars.AvatarModules;
import org.figuramc.figura.script_hooks.Event;
import org.figuramc.figura.script_hooks.EventListener;
import org.figuramc.figura.script_hooks.ScriptRuntime;
import org.figuramc.figura.script_hooks.callback.items.CallbackItem;
import org.figuramc.figura.script_hooks.mem_count.AllocationTracker;
import org.figuramc.figura.script_languages.lua.LuaRuntime;
import org.figuramc.figura.util.enumlike.IdMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

// The Scripts component should generally be at or near the END of the list of components.
// Other components probably should not rely on Scripts, and instead Scripts should rely on them.
public class Scripts implements AvatarComponent<Scripts> {

    // It's important that all possible dependencies be mentioned here!
    public static final Type<Scripts> TYPE = new Type<>(EntityRoot.TYPE, EntityUser.TYPE, VanillaRendering.TYPE);
    public Type<Scripts> getType() { return TYPE; }

    // Listeners for built-in events, indexed using the event's ID.
    public IdMap<Event<?>, EventListener<?>> eventListeners;
    // Type-safe EventListener<T> getter from Event<T>
    @SuppressWarnings("unchecked") public <T extends CallbackItem> EventListener<T> getEventListener(Event<T> event) { return (EventListener<T>) eventListeners.get(event); }

    // The Script Runtimes which are in use by this avatar.
    private Collection<ScriptRuntime> scriptRuntimes;

    // The avatar, set during initialize()
    private Avatar<?> avatar;
    public @NotNull Avatar<?> getAvatar() {
        return Objects.requireNonNull(avatar, "Attempt to fetch Avatar from Scripts before it's initialized");
    }

    // Temp reference kept to main module, so we can initialize it
    private AvatarModules.Module mainModule;

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
    public void initialize(AvatarModules modules, Avatar<?> self) throws AvatarError {
        this.avatar = self;
        this.mainModule = modules.mainModule();
        this.eventListeners = new IdMap<>(Event.class, e -> new EventListener<>(e.type));
        this.scriptRuntimes = setupRuntimes(modules).values();
    }

    public @Nullable AllocationTracker getAllocationTracker() {
        return avatar.getAllocationTracker();
    }

    // Runs once on init
    @Override
    public void mainThreadInitialize() {
        try {
            mainModule.initScript(); // Init the main module
        } catch (AvatarError ex) {
            avatar.error(ex);
        } catch (Throwable ex) {
            avatar.unexpectedError(ex);
        } finally {
            mainModule = null;
        }
    }

    // Run the given event, with its args
    public <Args extends CallbackItem> void runEvent(Event<Args> event, Args args) {
        try {
            // If there's nothing registered to this event, quit.
            if (event.id >= eventListeners.size()) return;
            // Fetch the event listener and invoke it.
            getEventListener(event).invoke(args);
        } catch (Throwable ex) {
            avatar.unexpectedError(ex);
        }
    }

    // Runs every tick
    @Override
    public void tick() {
        runEvent(Event.CLIENT_TICK, CallbackItem.Unit.INSTANCE);
    }

    @Override
    public void destroy() {
        scriptRuntimes.forEach(ScriptRuntime::destroy);
    }

    // TODO expose this as an API to mods/addons in an official way, so they can add languages
    @FunctionalInterface
    public interface RuntimeCreator {
        // When creating the runtime, you may set up environments for modules, but do not *initialize* the modules (i.e. run any user code!) yet.
        // Wait until the initModule() method is called to initialize the module.
        ScriptRuntime createRuntime(Scripts scriptsComponent, AvatarModules modules) throws AvatarError;
    }
    public static final Map<String, RuntimeCreator> RUNTIME_CREATORS = new HashMap<>();
    static {
        RUNTIME_CREATORS.put("lua", LuaRuntime::new);
    }

    // Determine which runtimes these modules use, and create them accordingly
    // (But do not run user code yet!)
    private Map<String, ScriptRuntime> setupRuntimes(AvatarModules modules) throws AvatarError {
        Map<String, ScriptRuntime> runtimes = new TreeMap<>();
        for (AvatarModules.Module module : modules.modules) {
            String lang = module.materials.metadata().language();
            if (lang == null) continue;
            if (!runtimes.containsKey(lang)) {
                RuntimeCreator creator = RUNTIME_CREATORS.get(lang);
                if (creator == null) throw new AvatarError("figura.error.loading.script.unknown_language", lang);
                runtimes.put(lang, creator.createRuntime(this, modules));
            }
            module.runtime = runtimes.get(lang);
        }
        return runtimes;
    }

}
