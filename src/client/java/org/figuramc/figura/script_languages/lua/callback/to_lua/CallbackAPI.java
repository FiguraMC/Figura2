package org.figuramc.figura.script_languages.lua.callback.to_lua;

import org.figuramc.figura.avatars.AvatarError;
import org.figuramc.figura.script_hooks.callback.CallbackType;
import org.figuramc.figura.script_hooks.callback.ScriptCallback;
import org.figuramc.figura.script_hooks.callback.items.CallbackItem;
import org.figuramc.figura.script_hooks.mem_count.AllocationTracker;
import org.figuramc.figura.script_languages.lua.FiguraMetatables;
import org.figuramc.figura.script_languages.lua.LuaCallback;
import org.figuramc.figura.script_languages.lua.callback.CallbackTypeAPI;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.*;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.function.LibFunction;

/**
 * Wraps a Callback created elsewhere, passed **TO LUA**
 */
public class CallbackAPI {

    public static LuaUserdata wrap(ScriptCallback<?, ?> callback, LuaState state) {
        return new LuaUserdata(callback, state.figuraMetatables.callback);
    }

    public static LuaTable createMetatable(LuaState state) throws LuaError, AvatarError {
        LuaTable metatable = new LuaTable(state.allocationTracker);

        // Callback:type() -> CallbackType (TODO memory limiting on this?)
        metatable.rawset("type", LibFunction.create((s, callback) -> CallbackTypeAPI.wrap(callback.checkUserdata(s, ScriptCallback.class).type(), s)));

        // Callback(args) -> output
        metatable.rawset(Constants.CALL, LibFunction.createV((s, args) -> {
            // Fetch callback from userdata
            ScriptCallback<?, ?> callback = args.first().checkUserdata(s, ScriptCallback.class);
            // Fast track. If it's a LuaCallback and it uses the same LuaState, we can go faster with localCall.
            if (callback instanceof LuaCallback<?,?> luaCallback && luaCallback.state == s) {
                return luaCallback.localCall(args.subargs(2));
            } else {
                return callImpl(s, callback, args.subargs(2));
            }
        }));

        metatable.rawset(Constants.NAME, LuaString.valueOfNoAlloc("Callback"));

        FiguraMetatables.setupIndexing(state, metatable);

        return metatable;
    }

    // Separate into its own method so we can use generics properly
    private static <I extends CallbackItem, O extends CallbackItem> Varargs callImpl(LuaState s, ScriptCallback<I, O> callback, Varargs args) throws LuaError, AllocationTracker.AvatarOOMException {
        // Handle tuple args specially. We treat tuples as lua varargs at top level for convenience
        CallbackType.Func<I, O> ty = callback.type();
        // Typecheck (and count-check) the provided args against the expected args
        int requiredArgCount = ty.param() instanceof CallbackType.Tuple<?> tuple ? tuple.count() : 1;
        if (args.count() != requiredArgCount)
            throw new LuaError("Attempt to call callback with incorrect number of args. Expected " + requiredArgCount + ", got " + args.count(), s.allocationTracker);
        I input = ty.param() instanceof CallbackType.Tuple<I> tuple ? tuple.toItems(s.luaToCallbackItem, args.toArray()) : ty.param().toItem(s.luaToCallbackItem, args.first());
        // Invoke the function
        O output = callback.call(input);
        // Convert results back into Lua and return
        return ty.returnType() instanceof CallbackType.Tuple<O> tuple ? ValueFactory.varargsOf(tuple.fromItems(s.callbackItemToLua, output, LuaValue[]::new)) : ty.returnType().fromItem(s.callbackItemToLua, output);
    }

}
