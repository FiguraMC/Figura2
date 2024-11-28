package org.figuramc.figura.script_hooks;

public interface ScriptFunction {
    // Unsure what to put here yet...
    Object call(Object... args) throws ScriptError;
}
