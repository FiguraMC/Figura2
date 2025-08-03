package org.figuramc.figura.script_languages.lua.events;

import org.figuramc.figura.avatars.Avatar;
import org.figuramc.figura.avatars.AvatarError;
import org.figuramc.figura.script_hooks.EventListener;
import org.figuramc.figura.script_hooks.callback.CallbackType;
import org.figuramc.figura.script_hooks.callback.items.CallbackItem;
import org.figuramc.figura.script_hooks.mem_count.AllocationTracker;
import org.figuramc.figura.script_languages.lua.FiguraMetatables;
import org.figuramc.figura.script_languages.lua.LuaCallback;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.*;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.function.LibFunction;

public class EventListenerAPI {

    public static LuaUserdata wrap(EventListener<?> eventListener, LuaState state) {
        return new LuaUserdata(eventListener, state.figuraMetatables.eventListener);
    }

    public static LuaTable createMetatable(LuaState state) throws LuaError, AvatarError {
        LuaTable metatable = new LuaTable(state.allocationTracker);

        // Create a new EventListener(?)
        // TODO make API for CallbackType. Expect vararg of these to act as params in constructor?
//        metatable.rawset("new", LibFunction.create(s -> EventListenerAPI.wrap(new EventListener(), metatables)));

        // Register a value to be called when this event listener fires
        metatable.rawset("register", LibFunction.create((s, self, arg) -> {
            EventListener<?> listener = self.checkUserdata(s, EventListener.class);
            registerImpl(s, listener, arg);
            return Constants.NIL;
        }));

        // Invoke the EventListener with the given args
        metatable.rawset(Constants.CALL, LibFunction.createV((s, args) -> {
            EventListener<?> listener = args.first().checkUserdata(s, EventListener.class);
            callImpl(s, listener, args.subargs(2));
            return Constants.NIL;
        }));

        metatable.rawset(Constants.NAME, LuaString.valueOfNoAlloc("EventListener"));

        FiguraMetatables.setupIndexing(state, metatable);
        return metatable;
    }

    // Helper method to let generics work
    public static <T extends CallbackItem> void registerImpl(LuaState state, EventListener<T> listener, LuaValue func) {
        listener.registerCallback(new LuaCallback<>(listener.funcType, state, func));
    }

    // Separate into its own method so we can use generics properly
    private static <I extends CallbackItem> void callImpl(LuaState s, EventListener<I> eventListener, Varargs args) throws LuaError, AllocationTracker.AvatarOOMException {
        // Handle tuple args specially. We treat tuples as lua varargs at top level for convenience
        CallbackType<I> paramType = eventListener.funcType.param();
        // Typecheck (and count-check) the provided args against the expected args
        int requiredArgCount = paramType instanceof CallbackType.Tuple<?> tuple ? tuple.count() : 1;
        if (args.count() != requiredArgCount)
            throw new LuaError("Attempt to call callback with incorrect number of args. Expected " + requiredArgCount + ", got " + args.count(), s.allocationTracker);
        I input = paramType instanceof CallbackType.Tuple<I> tuple ? tuple.toItems(s.luaToCallbackItem, args.toArray()) : paramType.toItem(s.luaToCallbackItem, args.first());
        // Invoke the function
        eventListener.invoke(input);
    }

}
