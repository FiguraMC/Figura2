package org.figuramc.figura.comptime.lua.annotations;

/**
 * Indicates a function that should run directly.
 * Should accept a LuaState and Varargs, and return Varargs.
 * It cannot have overloads.
 */
public @interface LuaDirect {
}
