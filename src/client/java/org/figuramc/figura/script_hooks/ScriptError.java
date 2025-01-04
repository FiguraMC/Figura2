package org.figuramc.figura.script_hooks;

/**
 * If an error occurs during script execution, throw one of these!
 */
public class ScriptError extends Exception {

    public ScriptError(String message) { super(message); }

}
