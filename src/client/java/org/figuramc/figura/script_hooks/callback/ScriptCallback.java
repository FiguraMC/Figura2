package org.figuramc.figura.script_hooks.callback;

import org.figuramc.figura.script_hooks.ScriptError;
import org.figuramc.figura.script_hooks.mem_count.MemoryCountable;

/**
 * A callback within a ScriptRuntime that can be sent outside of that runtime.
 * Also need to be memory countable.
 */
public interface ScriptCallback extends MemoryCountable {

    /**
     * Get the type of this callback
     */
    CallbackType.Func type();

    /**
     * Invoke the callback, applying translations, with the given args.
     */
    Object call(Object... args) throws ScriptError;

}
