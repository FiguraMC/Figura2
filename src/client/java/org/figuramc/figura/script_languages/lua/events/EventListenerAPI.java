package org.figuramc.figura.script_languages.lua.events;

import org.figuramc.figura.script_hooks.EventListener;
import org.figuramc.figura.script_hooks.ScriptError;
import org.figuramc.figura.script_languages.lua.FiguraMetatables;
import org.figuramc.figura.script_languages.lua.LuaCallback;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.*;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.function.LibFunction;

public class EventListenerAPI {

    public static LuaUserdata wrap(EventListener eventListener, FiguraMetatables metatables) {
        return new LuaUserdata(eventListener, metatables.eventListener);
    }

    public static LuaTable createMetatable(LuaState state, FiguraMetatables metatables) throws LuaError {
        LuaTable metatable = new LuaTable(state.allocationTracker);

        // Create a new EventListener
        // TODO make API for CallbackType. Expect varargs of these to act as params in constructor
//        metatable.rawset("new", LibFunction.create(s -> EventListenerAPI.wrap(new EventListener(), metatables)));

        // Register a value to be called when this event listener fires
        metatable.rawset("register", LibFunction.create((s, self, arg) -> {
            EventListener listener = self.checkUserdata(s, EventListener.class);
            listener.registerCallback(new LuaCallback(listener.funcType, s, metatables, arg));
            return Constants.NIL;
        }));

        // Invoke the EventListener with the given args
        metatable.rawset(Constants.CALL, LibFunction.createV((s, args) -> {
            // Extract self
            EventListener listener = args.first().checkUserdata(s, EventListener.class);
            try {
                // Convert the args to generic args
                args = args.subargs(2);
                Object[] genericArgs = new Object[args.count()];
                for (int i = 0; i < args.count(); i++)
                    genericArgs[i] = LuaCallback.fromLua(s, metatables, args.arg(i+1), listener.funcType.paramTypes()[i]);
                // Invoke the listener with the generic args
                listener.invoke(genericArgs);
            } catch (ScriptError err) {
                // Wrap any script error into a Lua error and rethrow
                throw new LuaError(err.getMessage(), s.allocationTracker);
            }
            // We're done, doesn't return anything
            return Constants.NIL;
        }));

        metatable.rawset(Constants.NAME, ValueFactory.valueOf("EventListener", state.allocationTracker));

        FiguraMetatables.setupIndexing(state, metatable, null, null);
        return metatable;
    }

}
