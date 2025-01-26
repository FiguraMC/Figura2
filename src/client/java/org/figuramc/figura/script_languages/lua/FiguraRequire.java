package org.figuramc.figura.script_languages.lua;

import org.figuramc.figura.manage.AvatarLoadingException;
import org.figuramc.figura.script_languages.lua.cobalt.cc.tweaked.cobalt.internal.unwind.SuspendedAction;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.*;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.compiler.CompileException;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.compiler.LoadState;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.function.Dispatch;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.function.LibFunction;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.function.LuaClosure;

import java.io.ByteArrayInputStream;
import java.util.Map;

/**
 * Class for setup of require() function, given the provided scripts map
 */
public class FiguraRequire {

    // Helpful constants
    public static final LuaString REQUIRE_KEY = LuaString.valueOf(null, "figura_require");
    public static final LuaString LOADED_KEY = LuaString.valueOf(null, "figura_loaded");
    public static final LuaString DOT_LUA = LuaString.valueOf(null, ".lua");

    public static void createRequire(LuaState state, Map<String, byte[]> scripts) throws LuaError, AvatarLoadingException {

        // Define require() using the passed scripts:
        // Fill in the require table in the registry.
        // Use a registry table for memory tracing
        LuaTable functionStorage = state.registry().getSubTable(REQUIRE_KEY);
        for (var script : scripts.entrySet()) {
            String name = script.getKey();
            byte[] code = script.getValue();
            try {
                // Compile to a closure, and put it in the require() table.
                // Use @ because it's a file name.
                LuaClosure closure = LoadState.load(state, new ByteArrayInputStream(code), "@" + name, state.globals());
                functionStorage.rawset(name, closure);
            } catch (CompileException ex) {
                throw new AvatarLoadingException("figura.error.loading.script.lua.compile_error", ex, false, name, ex.getMessage());
            } catch (LuaError ex) {
                throw new AvatarLoadingException("figura.error.loading.script.lua.compile_error", ex, true, name, ex.getMessage());
            }
        }
        // Create require function
        state.globals().rawset("require", LibFunction.createS((s, di, args) -> {
            LuaString nonfinal_fileName = args.first().checkLuaString(s);
            // Append with .lua if not already
            if (!nonfinal_fileName.endsWith(".lua"))
                nonfinal_fileName = LuaString.valueOfStrings(s.allocationTracker, new LuaValue[]{ nonfinal_fileName, DOT_LUA }, 0, 2, nonfinal_fileName.length() + 4);
            final LuaString fileName = nonfinal_fileName; // Thank you java lambdas for requiring this!!!!! /s
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

    }

}
