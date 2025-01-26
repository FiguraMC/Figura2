package org.figuramc.figura.script_hooks;

import org.figuramc.figura.avatars.Avatar;
import org.figuramc.figura.manage.AvatarLoadingException;
import org.figuramc.figura.script_languages.lua.LuaRuntime;
import oshi.util.tuples.Pair;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * One of these should exist for every language the mod supports.
 * The primary job of a ScriptRuntimeType is to create ScriptRuntime instances,
 * but it may have other jobs later, like maybe pre-processing files before their data is added to AvatarMaterials.
 */
public interface ScriptRuntimeType {

    /**
     * All runtime types. Order doesn't matter.
     */
    List<ScriptRuntimeType> ALL_RUNTIME_TYPES = List.of(
       new ScriptRuntimeType() {
           @Override
           public ScriptRuntime newRuntime(Avatar<?> avatar, Map<String, byte[]> scripts) throws AvatarLoadingException {
               return new LuaRuntime(avatar, scripts);
           }

           @Override
           public String name() {
               return "Lua";
           }

           @Override
           public List<String> validFileExtensions() {
               return List.of("lua");
           }
       }
    );

    /**
     * Helper map which sends file extension -> runtime type.
     */
    Map<String, ScriptRuntimeType> TYPE_BY_EXTENSION =
            ALL_RUNTIME_TYPES.stream()
                    .map(t -> new Pair<>(t, t.validFileExtensions()))
                    .flatMap(p -> p.getB().stream()
                            .map(ext -> new Pair<>(p.getA(), ext)))
                    .collect(Collectors.toMap(Pair::getB, Pair::getA));

    /**
     * Create a new ScriptRuntime in the given Avatar.
     */
    ScriptRuntime newRuntime(Avatar<?> avatar, Map<String, byte[]> scripts) throws AvatarLoadingException;

    /**
     * Get the name of this runtime type
     */
    String name();

    /**
     * Return all valid file extensions for this runtime type.
     */
    List<String> validFileExtensions();

}
