package org.figuramc.figura.script_languages.lua;

import org.figuramc.figura.avatars.AvatarModules;
import org.figuramc.figura.script_hooks.callback.ScriptCallback;
import org.figuramc.figura.script_hooks.mem_count.AllocationTracker;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.LuaError;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.LuaState;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.LuaTable;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.LuaUserdata;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.function.LibFunction;

import static org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.Constants.*;
import static org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.ValueFactory.*;

public class ModuleAPI {

    public static LuaUserdata wrap(AvatarModules.Module module, FiguraMetatables metatables) {
        return userdataOf(module, metatables.module);
    }

    public static LuaTable createMetatable(LuaState state, FiguraMetatables metatables) throws LuaError {
        AllocationTracker t = state.allocationTracker;
        LuaTable metatable = tableOf(t);

        metatable.rawset(NAME, valueOf("Module", t));

        // Custom __index will look for API callbacks
        FiguraMetatables.setupInheritance(state, metatable, null, LibFunction.create((s, m, k) -> {
            AvatarModules.Module module = m.checkUserdata(s, AvatarModules.Module.class);
            String key = k.checkString(s);
            ScriptCallback callback = module.callbacks.get(key);
            if (callback == null) return NIL;
            return CallbackAPI.wrap(callback, metatables);
        }));

        return metatable;
    }

}
