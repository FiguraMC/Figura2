package org.figuramc.figura.script_languages.lua.math.vector;

import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.LuaState;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.LuaUserdata;
import org.joml.Vector2d;

public class Vector2API {

    // Wrap object into a userdata given the metatables
    public static LuaUserdata wrap(Vector2d vec, LuaState state) {
        return new LuaUserdata(vec, state.figuraMetatables.vec2);
    }


}
