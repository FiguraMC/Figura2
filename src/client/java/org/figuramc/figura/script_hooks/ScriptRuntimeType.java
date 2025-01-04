package org.figuramc.figura.script_hooks;

import org.figuramc.figura.avatars.Avatar;
import org.figuramc.figura.data.AvatarMaterials;
import org.figuramc.figura.manage.AvatarLoadingException;
import org.figuramc.figura.script_languages.lua.LuaRuntime;
import org.figuramc.figura.util.IOUtils;
import org.figuramc.figura.util.StringUtils;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * One of these should exist for every language the mod supports.
 * The primary job of a ScriptRuntimeType is to create ScriptRuntime instances,
 * but it may have other jobs later, like maybe pre-processing files before their data is added to AvatarMaterials.
 */
public interface ScriptRuntimeType {

    /**
     * The collection of ALL Script Runtime Types, stored by name.
     */
    Map<String, ScriptRuntimeType> ALL_RUNTIME_TYPES = new LinkedHashMap<>() {{
       put("Lua", new ScriptRuntimeType() {
           @Override
           public ScriptRuntime newRuntime(Avatar<?> avatar, Map<String, byte[]> scripts) throws AvatarLoadingException {
               return new LuaRuntime(avatar, scripts);
           }
           @Override
           public boolean isValidExtension(String extension) {
               return "lua".equals(extension);
           }
       });
    }};

    /**
     * Create a new ScriptRuntime in the given Avatar.
     */
    ScriptRuntime newRuntime(Avatar<?> avatar, Map<String, byte[]> scripts) throws AvatarLoadingException;

    /**
     * Return true if this file extension should be part of this runtime type.
     */
    boolean isValidExtension(String extension);

}
