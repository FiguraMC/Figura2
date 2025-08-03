package org.figuramc.figura.script_languages.lua.math.vector;

import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.LuaState;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.LuaUserdata;
import org.joml.Vector3d;

public class Vector3API {

    // Wrap object into a userdata given the metatables
    public static LuaUserdata wrap(Vector3d vec, LuaState state) {
        return new LuaUserdata(vec, state.figuraMetatables.vec3);
    }


}
