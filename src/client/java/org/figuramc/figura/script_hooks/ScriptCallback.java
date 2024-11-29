package org.figuramc.figura.script_hooks;

/**
 * A callback within a ScriptRuntime that can be sent outside of that runtime.
 */
public interface ScriptCallback {

    /**
     * Invoke the callback with the given args.
     */
    Object call(Object... args) throws ScriptError;
}
