package org.figuramc.figura.script_hooks;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the given item should be exposed to Script Engines.
 *
 * Only some classes should ever be used as parameters to such a method:
 * - Primitives, like boolean, short, int, float, etc.
 * - Nullable versions of these primitives
 * - Strings
 * - ArrayList and HashMap
 * - Any class which has been sent through ScriptRuntime.registerClass()
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ScriptAllow {}