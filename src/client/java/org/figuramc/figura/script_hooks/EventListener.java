package org.figuramc.figura.script_hooks;

import org.figuramc.figura.script_hooks.callback.CallbackType;
import org.figuramc.figura.script_hooks.callback.ScriptCallback;
import org.figuramc.figura.script_hooks.mem_count.MarkedObjectBase;
import org.figuramc.figura.script_hooks.mem_count.MemoryCounter;
import org.figuramc.figura.util.ListUtils;

import java.util.ArrayList;

/**
 * Each Avatar is given a built-in EventListener per Event.
 * Scripts are also allowed to dynamically create EventListeners. (This is TODO)
 * An EventListener is just a type plus a list of callbacks, which can be invoked.
 * If a callback returns true, it is removed from the list.
 *
 * The Java side maintains a collection of built-in EventListeners, which it can invoke when an event occurs.
 */
public class EventListener extends MarkedObjectBase {

    public final CallbackType.Func funcType; // Type for callbacks
    private final ArrayList<ScriptCallback> callbacks = new ArrayList<>();

    // Requires static param types on creation
    public EventListener(CallbackType... paramTypes) {
        this.funcType = new CallbackType.Func(CallbackType.Bool.INSTANCE, paramTypes);
    }

    // Only allow appending to the list at the end.
    // This is because of the potential for a callback to register additional callbacks.
    public void registerCallback(ScriptCallback callback) {
        this.callbacks.add(callback);
    }

    // Invoke the event listener with the given args.
    public void invoke(Object... args) throws ScriptError {
        // Any callback returning true will be removed.
        ListUtils.filterMut(callbacks, callback -> !(callback.call(args) instanceof Boolean b) || !b);
    }

    @Override
    protected long traceNoMark(MemoryCounter counter, int depth) {
        throw new UnsupportedOperationException("TODO");
    }
}
