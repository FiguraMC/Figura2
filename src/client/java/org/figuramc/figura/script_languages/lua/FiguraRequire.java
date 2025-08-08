package org.figuramc.figura.script_languages.lua;

import org.figuramc.figura.avatars.Avatar;
import org.figuramc.figura.avatars.AvatarError;
import org.figuramc.figura.avatars.AvatarModules;
import org.figuramc.figura.script_languages.lua.type_apis.callback.CallbackAPI;
import org.figuramc.figura.script_languages.lua.cobalt.cc.tweaked.cobalt.internal.unwind.SuspendedAction;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.*;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.compiler.CompileException;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.compiler.LoadState;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.function.Dispatch;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.function.LibFunction;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.function.LuaClosure;
import org.figuramc.figura.util.IOUtils;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Objects;

/**
 * Class for setup of a module's require() function
 */
public class FiguraRequire {

    // Helpful constants
    public static final LuaString REQUIRE_KEY = LuaString.valueOfNoAlloc("figura_require");
    public static final LuaString LOADED_KEY = LuaString.valueOfNoAlloc("figura_loaded");

    public static LuaValue createRequire(LuaState state, LuaTable _ENV, AvatarModules.LoadTimeModule module) throws LuaError, AvatarError {

        int index = module.index;

        // Define require() for this module using its scripts
        // Use a registry table for memory tracing
        LuaTable functionStorage = new LuaTable(state.allocationTracker);
        state.registry().getSubTable(REQUIRE_KEY).rawset(index + 1, functionStorage);
        state.registry().getSubTable(LOADED_KEY).rawset(index + 1, new LuaTable(state.allocationTracker));

        // Add all the scripts to the require table, and also add dependency modules, prefixed with "@".
        for (var script : module.materials.scripts().entrySet()) {
            String name = IOUtils.stripExtension(script.getKey(), "lua"); // Strip "lua" extension
            byte[] code = script.getValue();
            try {
                // Compile to a closure, and put it in the require() table.
                // Use @ because it's a file name, and Cobalt does something with that internally.
                LuaClosure closure = LoadState.load(state, new ByteArrayInputStream(code), "@" + name, _ENV);
                functionStorage.rawset(name, closure);
            } catch (CompileException ex) {
                throw new AvatarError("figura.error.loading.script.lua.compile_error", ex, name, ex.getMessage());
            }
        }
        for (var entry : module.dependencyIndices.entrySet()) {
            String name = entry.getKey();
            int dependencyIndex = entry.getValue();
            functionStorage.rawset("@" + name, LibFunction.create(s -> {
                // Fetch the list of runtime modules
                Avatar<?> avatar = Objects.requireNonNull(s.avatar, "Attempt to require inside state not yet initialized? Bug in Figura, please report!");
                List<AvatarModules.RuntimeModule> runtimeModules = avatar.modules;
                // Create a table to hold API functions
                LuaTable tab = new LuaTable(s.allocationTracker);
                // Fetch the dependency at runtime given its index, and initialize it
                AvatarModules.RuntimeModule dependency = runtimeModules.get(dependencyIndex);
                dependency.initialize(runtimeModules);
                // Grab its callbacks, put them in the table
                for (var apiEntry : dependency.callbacks.entrySet()) {
                    tab.rawset(apiEntry.getKey(), CallbackAPI.wrap(apiEntry.getValue(), state));
                }
                return tab;
            }));
        }

        // Create require function (only captured variable is a single int, so we don't need to worry about tracing this lambda)
        return LibFunction.createS((s, di, args) -> {
            // First arg is file name (without the .lua), or "@" + dependency module name
            LuaString fileName = args.first().checkLuaString(s);
            // Fetch tables
            // String -> boolean. Nil = not loaded, false = currently being loaded (detect loops), true = fully loaded and done
            LuaTable isLoaded = s.registry().getSubTable(LOADED_KEY).rawget(index + 1).checkTable(s, "Bug with Figura Lua registry for require(); expected table but did not find");
            // String -> value, either the function or the cached return value
            LuaTable requireTable = s.registry().getSubTable(REQUIRE_KEY).rawget(index + 1).checkTable(s, "Bug with Figura Lua registry for require(); expected table but did not find");
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
        });
    }

}
