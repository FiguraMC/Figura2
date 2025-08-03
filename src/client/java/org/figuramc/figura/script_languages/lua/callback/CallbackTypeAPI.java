package org.figuramc.figura.script_languages.lua.callback;

import org.figuramc.figura.avatars.AvatarError;
import org.figuramc.figura.script_hooks.callback.CallbackType;
import org.figuramc.figura.script_languages.lua.FiguraMetatables;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.*;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.function.LibFunction;

/**
 * API for creating CallbackType.
 * TODO memory counting for CallbackType instances
 */
public class CallbackTypeAPI {

    public static LuaUserdata wrap(CallbackType<?> callbackType, LuaState state) {
        return new LuaUserdata(callbackType, state.figuraMetatables.callbackType);
    }

    public static LuaTable createMetatable(LuaState state) throws LuaError, AvatarError {
        LuaTable metatable = new LuaTable(state.allocationTracker);

        metatable.rawset("UNIT", new LuaUserdata(CallbackType.Unit.INSTANCE, metatable));
        metatable.rawset("ANY", new LuaUserdata(CallbackType.Any.INSTANCE, metatable));
        metatable.rawset("BOOL", new LuaUserdata(CallbackType.Bool.INSTANCE, metatable));
        metatable.rawset("F32", new LuaUserdata(CallbackType.F32.INSTANCE, metatable));
        metatable.rawset("F64", new LuaUserdata(CallbackType.F64.INSTANCE, metatable));
        metatable.rawset("STRING", new LuaUserdata(CallbackType.Str.INSTANCE, metatable));

        metatable.rawset("List", LibFunction.create((s, arg) -> new LuaUserdata(new CallbackType.List<>(arg.checkUserdata(s, CallbackType.class)), metatable)));
        metatable.rawset("Map", LibFunction.create((s, key, value) -> new LuaUserdata(new CallbackType.Map<>(key.checkUserdata(s, CallbackType.class), value.checkUserdata(s, CallbackType.class)), metatable)));

        metatable.rawset(Constants.NAME, LuaString.valueOfNoAlloc("CallbackType"));

        FiguraMetatables.setupIndexing(state, metatable);
        return metatable;
    }


}
