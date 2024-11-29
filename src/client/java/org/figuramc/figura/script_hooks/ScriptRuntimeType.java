package org.figuramc.figura.script_hooks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * One of these should exist for every language the mod supports.
 * The primary job of a ScriptRuntimeType is to create ScriptRuntime instances,
 * but it may have other jobs later, like maybe pre-processing files before their data is added to AvatarMaterials.
 */
public interface ScriptRuntimeType {

    /**
     * The collection of ALL ScriptRuntimeTypes, stored by name.
     */
    Map<String, ScriptRuntimeType> ALL_RUNTIME_TYPES = new ConcurrentHashMap<>();

    /**
     * Create a new ScriptRuntime.
     */
    ScriptRuntime newRuntime();

}
