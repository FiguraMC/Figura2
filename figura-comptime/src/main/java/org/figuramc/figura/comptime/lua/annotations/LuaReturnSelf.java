package org.figuramc.figura.comptime.lua.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Indicates this method should return the "self" parameter passed to it.
 */
@Target(ElementType.METHOD)
public @interface LuaReturnSelf {
}
