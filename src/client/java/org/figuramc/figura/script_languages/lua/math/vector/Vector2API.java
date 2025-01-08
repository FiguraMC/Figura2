package org.figuramc.figura.script_languages.lua.math.vector;

import org.figuramc.figura.script_languages.lua.FiguraMetatables;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.LuaUserdata;
import org.joml.Vector2d;

import static org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.ValueFactory.userdataOf;

public class Vector2API {

    // Wrap object into a userdata given the metatables
    public static LuaUserdata wrap(Vector2d vec, FiguraMetatables metatables) {
        return userdataOf(vec, metatables.vec2);
    }


}
