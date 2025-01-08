package org.figuramc.figura.script_languages.lua.model_parts;

import org.figuramc.figura.script_hooks.mem_count.AllocationTracker;
import org.figuramc.figura.script_languages.lua.FiguraMetatables;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.LuaError;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.LuaState;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.LuaTable;
import org.jetbrains.annotations.Nullable;

import static org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.ValueFactory.*;
import static org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.Constants.*;

public class RootModelPartAPI {


    public static LuaTable createMetatable(LuaState state, FiguraMetatables metatables) throws LuaError {
        @Nullable AllocationTracker t = state.allocationTracker;
        LuaTable metatable = tableOf(t);
        FiguraMetatables.setupInheritance(metatable, metatables.modelPart);

        metatable.rawset(NAME, valueOf("RootModelPart", t));

        return metatable;
    }

}
