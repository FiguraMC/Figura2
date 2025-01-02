package org.figuramc.figura.script_hooks;

import org.figuramc.figura.script_hooks.mem_count.MemoryCountable;

/**
 * A callback within a ScriptRuntime that can be sent outside of that runtime.
 * Also need to be memory countable.
 */
public interface ScriptCallback extends MemoryCountable {

    /**
     * Invoke the callback with the given args.
     */
    Object call(Object... args) throws ScriptError;
}
