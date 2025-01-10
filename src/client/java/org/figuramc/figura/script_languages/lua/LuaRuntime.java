package org.figuramc.figura.script_languages.lua;

import org.figuramc.figura.FiguraModClient;
import org.figuramc.figura.avatars.Avatar;
import org.figuramc.figura.avatars.components.EntityRoot;
import org.figuramc.figura.avatars.components.Scripts;
import org.figuramc.figura.avatars.components.VanillaParts;
import org.figuramc.figura.manage.AvatarLoadingException;
import org.figuramc.figura.model.part.VanillaRootModelPart;
import org.figuramc.figura.script_hooks.ScriptError;
import org.figuramc.figura.script_hooks.ScriptRuntime;
import org.figuramc.figura.script_hooks.mem_count.MarkedObjectBase;
import org.figuramc.figura.script_hooks.mem_count.MemoryCounter;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.*;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.compiler.CompileException;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.compiler.LoadState;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.function.*;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.interrupt.InterruptAction;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.lib.*;
import org.figuramc.figura.script_languages.lua.math.FiguraMath;
import org.figuramc.figura.script_languages.lua.model_parts.ModelPartAPI;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class LuaRuntime extends MarkedObjectBase implements ScriptRuntime {

    private final Avatar<?> avatar;
    private final LuaState state;

    // Figura metatable reference, for creating/converting objects
    private final FiguraMetatables metatables;

    // Keep references to event objects
    private final LuaValue tick, render;

    /**
     * The starting point for the creation of a Lua Runtime.
     */
    public LuaRuntime(Avatar<?> avatar, Map<String, byte[]> scripts) throws AvatarLoadingException {
        // Save avatar
        this.avatar = avatar;
        // LuaError can happen at basically any time, so wrap the whole thing :P
        try {
            // Create the LuaState.
            this.state = LuaState.builder()
                    .interruptHandler(() -> {
                        // TODO throw an uncatchable error for timeout
                        return InterruptAction.CONTINUE;
                    })
                    .allocationTracker(avatar.getAllocationTracker()) // Pass the allocation tracker
                    .build();

            // Add basic globals
            CoreLibraries.standardGlobals(state);
            Bit32Lib.add(state, state.globals());

            // Add types with metatables
            this.metatables = new FiguraMetatables(state);

            // Other
            FiguraRequire.createRequire(state, scripts); // Setup require()
            FiguraMath.init(state, metatables);
            FiguraTable.init(state);

            // Set up events (also fetch the event objects for calling later):
            LuaTable defaultEvents = FiguraEvents.init(state, "tick", "render");
            tick = defaultEvents.rawget("tick");
            render = defaultEvents.rawget("render");

            // Benchmark testers, todo remove
            state.globals().rawset("micros", LibFunction.create(s -> ValueFactory.valueOf(System.nanoTime() / 1000d)));
            state.globals().rawset("print", LibFunction.create((s, v) -> { System.out.println(v); return Constants.NIL; }));

            // Add global variable "models" because why not. Also todo make more organized.
            LuaTable models = ValueFactory.tableOf(state.allocationTracker);
            state.globals().rawset("models", models);
            if (avatar.optionalDependency(EntityRoot.class, Scripts.class) != null)
                models.rawset("entity", ModelPartAPI.wrap(avatar.assertDependency(EntityRoot.class, Scripts.class).getModelPart(), metatables));

            // Create "vanilla" table if we have vanilla parts
            if (avatar.optionalDependency(VanillaParts.class, Scripts.class) != null) {
                VanillaParts component = avatar.assertDependency(VanillaParts.class, Scripts.class);

                // vanilla
                LuaTable vanilla = ValueFactory.tableOf(state.allocationTracker);
                state.globals().rawset("vanilla", vanilla);

                // vanilla.cancelAllParts(). 0 arg getter, 1 arg setter.
                vanilla.rawset("cancelAllParts", LibFunction.createV((s, args) -> {
                    switch (args.count()) {
                        case 0 -> { return ValueFactory.valueOf(component.cancelAllModelParts); }
                        case 1 -> component.cancelAllModelParts = args.first().checkBoolean(s);
                        default -> throw new LuaError("Invalid number of args to cancelAllParts(): expected 0 or 1", s.allocationTracker);
                    }
                    return Constants.NONE;
                }));

                // vanilla.parts
                LuaTable parts = ValueFactory.tableOf(state.allocationTracker);
                vanilla.rawset("parts", parts);
                // Fill it with current parts:
                for (var entry : component.partNameMap.entrySet())
                    parts.rawset(entry.getKey(), ModelPartAPI.wrap(entry.getValue(), metatables));
                // Add __index which will create a new part if none exists:
                LuaTable partsMetatable = ValueFactory.tableOf(state.allocationTracker);
                partsMetatable.rawset("__index", LibFunction.create((s, self, key) -> {
                    String name = key.checkString(s);
                    // If we don't already have a part for this key, create a new one
                    VanillaRootModelPart newRoot = component.getOrCreatePart(name);
                    // Store it back in the table for next time, and return it
                    LuaValue wrapped = ModelPartAPI.wrap(newRoot, metatables);
                    parts.rawset(key, wrapped);
                    return wrapped;
                }));
                parts.setMetatable(state, partsMetatable);
            }

        } catch (LuaError error) {
            throw new AvatarLoadingException("Problem instantiating the Lua runtime", error);
        }
    }

    @Override
    public void runCode(String snippet) throws ScriptError {
        throw new ScriptError("Not yet implemented");
    }

    @Override
    public void init() throws ScriptError {
        // TODO maybe add some kind of auto scripts behavior? For now it just does require 'main.lua'
        try {
            LuaClosure entrypoint = LoadState.load(state, new ByteArrayInputStream("require 'main.lua'".getBytes(StandardCharsets.UTF_8)), "=ENTRYPOINT", state.globals());
            LuaThread.runMain(state, entrypoint);
        } catch (LuaError luaError) {
            throw new ScriptError("Error in Lua during init", luaError);
        } catch (CompileException impossible) {
            throw new IllegalStateException("Failed to compile Lua entrypoint. Should be impossible. Bug in Figura, please report!", impossible);
        }
    }

    private void call(LuaValue event, String name, Varargs args) throws ScriptError {
        try {
            LuaThread.runMain(state, event, args);
        } catch (LuaError luaError) {
            throw new ScriptError("Error in Lua during \"" + name + "\" event", luaError);
        }
    }

    @Override
    public void tick() throws ScriptError {
        call(tick, "tick", Constants.NONE);
    }

    @Override
    public void render(float tickDelta) throws ScriptError {
        call(render, "render", LuaDouble.valueOf(tickDelta));
    }

    @Override
    public void destroy() {

    }

    // Memory tracing!
    @Override
    protected long traceNoMark(MemoryCounter counter, int depth) {
        counter.trace(state, depth);
        counter.trace(metatables, depth);
        counter.trace(tick, depth);
        counter.trace(render, depth);
        return 64; // Idk, random guess
    }

    // Helper methods to run a file defined in /assets/figura/scripts/lua/
    public static Varargs runAssetFile(LuaState state, String name) throws AvatarLoadingException {
        return runAssetFile(state, name, Constants.NONE);
    }
    public static Varargs runAssetFile(LuaState state, String name, Varargs args) throws AvatarLoadingException {
        try(InputStream input = FiguraModClient.class.getResourceAsStream("/assets/figura/scripts/lua/" + name + ".lua")) {
            // Compile the file
            if (input == null) throw new AvatarLoadingException("Figura was unable to find builtin file \"" + name + ".lua\"? Likely bug in Figura, please report.");
            LuaClosure c = LoadState.load(state, input, "=" + name.toUpperCase(), state.globals());
            // Execute the file
            return LuaThread.runMain(state, c, args);
        } catch (IOException e) {
            throw new AvatarLoadingException("Figura was unable to load \"" + name + ".lua\"? Likely bug in Figura, please report.", e);
        } catch (CompileException e) {
            throw new AvatarLoadingException("Figura internal \"" + name + ".lua\" failed to compile! Likely bug in Figura, please report.", e);
        } catch (LuaError e) {
            throw new AvatarLoadingException("Error while initializing Figura internal \"" + name + ".lua\". Likely bug in Figura, please report.", e);
        }
    }


}
