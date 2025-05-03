package org.figuramc.figura.script_languages.lua;

import org.figuramc.figura.manage.AvatarLoadingException;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.*;

/**
 * Events logic in another class for safe keeping.
 * Uses 'events.lua' resource to define most logic in Lua directly.
 */
public class FiguraEvents {

    /**
     * Runs the "events" script in the given state, and create several default events.
     * These default events are then returned in the table.
     * Make sure to run this before any user scripts are added.
     */
    public static LuaTable init(LuaState state, String... defaultEvents) throws AvatarLoadingException, LuaError {
        // Create args to the file
        LuaString[] eventNames = new LuaString[defaultEvents.length];
        for (int i = 0; i < defaultEvents.length; i++)
            eventNames[i] = LuaString.valueOf(state.allocationTracker, defaultEvents[i]);
        Varargs args = ValueFactory.varargsOf(eventNames);
        // Get result
        return LuaRuntime.runAssetFile(state, "events", args).first().checkTable(state);
    }


}
