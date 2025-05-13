package org.figuramc.figura.script_languages.lua;

import org.figuramc.figura.script_hooks.ScriptError;
import org.figuramc.figura.script_hooks.callback.ScriptCallback;
import org.figuramc.figura.script_hooks.mem_count.AllocationTracker;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.LuaError;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.LuaState;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.LuaTable;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.LuaUserdata;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.function.LibFunction;

import static org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.Constants.CALL;
import static org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.Constants.NAME;
import static org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.ValueFactory.*;

public class CallbackAPI {

    public static LuaUserdata wrap(ScriptCallback callback, FiguraMetatables metatables) {
        return userdataOf(callback, metatables.callback);
    }

    public static LuaTable createMetatable(LuaState state, FiguraMetatables metatables) throws LuaError {
        AllocationTracker t = state.allocationTracker;
        LuaTable metatable = tableOf(t);

        // TODO add a type() method to fetch the type of the callback

        // Invoke the callback
        metatable.rawset(CALL, LibFunction.createV((s, args) -> {
            // Fetch callback userdata:
            ScriptCallback callback = args.first().checkUserdata(s, ScriptCallback.class);
            // Fast track! If it's a LuaCallback, and it uses the same LuaState, we can go faster with localCall!
            if (callback instanceof LuaCallback luaCallback && luaCallback.state == s)
                return luaCallback.localCall(args.subargs(2));
            try {
                // Convert the args into generic format:
                Object[] genericArgs = new Object[args.count() - 1];
                for (int i = 1; i < args.count(); i++)
                    genericArgs[i] = LuaCallback.fromLua(s, metatables, args.arg(i+1), callback.type().paramTypes()[i]);
                // Invoke the callback
                Object result = callback.call(genericArgs);
                // Convert result back to Lua
                return LuaCallback.toLua(s, metatables, result, callback.type().returnType());
            } catch (ScriptError err) {
                // Wrap any script error into a Lua error and rethrow
                throw new LuaError(err.getMessage(), s.allocationTracker);
            }
        }));

        metatable.rawset(NAME, valueOf("Callback", t));

        FiguraMetatables.setupInheritance(state, metatable, null, null);

        return metatable;
    }

}
