package org.figuramc.figura.script_languages.lua.events;

import org.figuramc.figura.script_hooks.Event;
import org.figuramc.figura.script_hooks.EventListener;
import org.figuramc.figura.script_languages.lua.FiguraMetatables;
import org.figuramc.figura.script_languages.lua.LuaCallback;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.*;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.function.LibFunction;

import java.util.List;
import java.util.Map;

/**
 * The "events" table contains the built-in EventListener instances.
 * It also has special helper syntax for registration via "function events.eventName() ...code... end".
 */
public class EventsTable {

    public static void createEventsTable(LuaState state, LuaTable _ENV, FiguraMetatables metatables, Map<Event, EventListener> listeners) throws LuaError {

        // Create tables and set up meta-stuff
        LuaTable events = new LuaTable(state.allocationTracker); // Dummy table, which has metatable __index and __newindex
        LuaTable byName = new LuaTable(state.allocationTracker); // Backing table used as __index and for __newindex registration
        LuaTable metatable = new LuaTable(state.allocationTracker); // Metatable
        events.setMetatable(state, metatable);
        metatable.rawset(Constants.INDEX, byName);
        metatable.rawset(Constants.NEWINDEX, LibFunction.create((s, eventsTab, name, func) -> {
            LuaValue event = byName.rawget(name);
            if (event.isNil()) {
                throw new LuaError("Event named \"" + name + "\" does not exist", s.allocationTracker);
            }
            EventListener eventListener = event.checkUserdata(s, EventListener.class);
            eventListener.registerCallback(new LuaCallback(eventListener.funcType, s, metatables, func));
            return Constants.NIL;
        }));

        // Store in environment
        _ENV.rawset("events", events);

        // Fill in the backing table with the built-in listeners provided
        listeners.forEach((e, el) -> {
            byName.rawset(e.name, EventListenerAPI.wrap(el, metatables));
        });
    }

}
