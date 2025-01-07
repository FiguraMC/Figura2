package org.figuramc.figura.script_languages.lua;

import org.figuramc.figura.avatars.Avatar;
import org.figuramc.figura.avatars.components.EntityRoot;
import org.figuramc.figura.avatars.components.Scripts;
import org.figuramc.figura.manage.AvatarLoadingException;
import org.figuramc.figura.model.part.FiguraModelPart;
import org.figuramc.figura.script_hooks.ScriptError;
import org.figuramc.figura.script_hooks.ScriptRuntime;
import org.figuramc.figura.script_hooks.mem_count.MarkedObjectBase;
import org.figuramc.figura.script_hooks.mem_count.MemoryCounter;
import org.figuramc.figura.script_languages.lua.cobalt.cc.tweaked.cobalt.internal.unwind.SuspendedAction;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.*;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.compiler.CompileException;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.compiler.LoadState;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.function.*;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.interrupt.InterruptAction;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.lib.*;
import org.figuramc.figura.script_languages.lua.model_parts.ModelPartAPI;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class LuaRuntime extends MarkedObjectBase implements ScriptRuntime {

    private final Avatar<?> avatar;
    private final LuaState state;

    // Figura metatable reference, for creating/converting objects
    private final FiguraMetatables metatables;

    // Keys to the registry
    public static final LuaString REQUIRE_KEY = LuaString.valueOf(null, "figura_require");
    public static final LuaString LOADED_KEY = LuaString.valueOf(null, "figura_loaded");
    public static final LuaString DOT_LUA = LuaString.valueOf(null, ".lua");

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

            // First add metatable types, by creating FiguraMetatables
            this.metatables = new FiguraMetatables(state);

            // Add globals

            // Initialize with internal scripts written in Lua

            // Event / events (also fetch the event objects for later calling):
            LuaTable defaultEvents = EventsAPI.init(state, "tick", "render");
            tick = defaultEvents.rawget("tick");
            render = defaultEvents.rawget("render");

            // Benchmark testers
            state.globals().rawset("micros", LibFunction.create(s -> LuaDouble.valueOf(System.nanoTime() / 1000d)));
            state.globals().rawset("print", LibFunction.create((s, v) -> { System.out.println(v); return Constants.NIL; }));

            // Define require() using the passed scripts:
            // Fill in the require data table in the registry.
            // Use a registry table for memory tracing
            // TODO move to new file so i dont have to look at it
            LuaTable functionStorage = state.registry().getSubTable(REQUIRE_KEY);
            for (var script : scripts.entrySet()) {
                String name = script.getKey();
                byte[] code = script.getValue();
                try {
                    // Compile to a closure, and put it in the require() table.
                    LuaClosure closure = LoadState.load(state, new ByteArrayInputStream(code), "@" + name, state.globals());
                    functionStorage.rawset(name, closure);
                } catch (CompileException compileException) {
                    throw new AvatarLoadingException("Failed to compile Lua script", compileException);
                } catch (LuaError error) {
                    throw new AvatarLoadingException("Error while compiling Lua script", error);
                }
            }
            // Create require function
            state.globals().rawset("require", LibFunction.createS((s, di, args) -> {
                LuaString nonfinal_fileName = args.first().checkLuaString(s);
                // Append with .lua if not already
                if (!nonfinal_fileName.endsWith(".lua"))
                    nonfinal_fileName = LuaString.valueOfStrings(s.allocationTracker, new LuaValue[]{ nonfinal_fileName, DOT_LUA }, 0, 2, nonfinal_fileName.length() + 4);
                LuaString fileName = nonfinal_fileName;
                // Fetch tables
                LuaTable isLoaded = s.registry().getSubTable(LOADED_KEY); // String -> boolean. Nil = not loaded, false = currently being loaded (detect loops), true = fully loaded and done
                LuaTable requireTable = s.registry().getSubTable(REQUIRE_KEY); // String -> value, either the function or the cached return value
                // If already loaded, return from cache
                LuaValue alreadyLoaded = isLoaded.rawget(fileName);
                if (alreadyLoaded == Constants.TRUE) return requireTable.rawget(fileName);
                if (alreadyLoaded == Constants.FALSE) throw new LuaError("Recursive require(): attempting to require file \"" + fileName + "\" from within itself", s.allocationTracker);
                // Ensure the function exists
                LuaValue toCall = requireTable.rawget(fileName);
                if (toCall.isNil()) throw new LuaError("Attempt to require non-existent file \"" + fileName + "\"", s.allocationTracker);
                // Before running function, mark it as in-progress
                isLoaded.rawset(fileName, Constants.FALSE);
                // Run the function, passing the file name as the varargs.
                LuaValue result = SuspendedAction.run(di, () -> Dispatch.invoke(s, toCall, fileName)).first();
                // Mark it as complete, and store result in cache for future use.
                isLoaded.rawset(fileName, Constants.TRUE);
                requireTable.rawset(fileName, result);
                // Return the result.
                return result;
            }));

            // Add global variable "models" because why not. Also todo make more organized.
            LuaTable models = ValueFactory.tableOf(state.allocationTracker);
            state.globals().rawset("models", models);
            if (avatar.optionalDependency(EntityRoot.class, Scripts.class) != null)
                models.rawset("entity", ModelPartAPI.wrap(avatar.assertDependency(EntityRoot.class, Scripts.class).getModelPart(), metatables));

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
        // TODO add some kind of auto scripts behavior, for now it just does require 'main.lua'
        try {
            LuaClosure entrypoint = LoadState.load(state, new ByteArrayInputStream("require 'main.lua'".getBytes(StandardCharsets.UTF_8)), "=Figura::Entrypoint", state.globals());
            LuaThread.runMain(state, entrypoint);
        } catch (LuaError luaError) {
            throw new ScriptError("Error in Lua script during init", luaError);
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


}
