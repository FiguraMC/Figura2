package org.figuramc.figura.script_languages.lua;

import org.figuramc.figura.script_hooks.mem_count.MarkedObjectBase;
import org.figuramc.figura.script_hooks.mem_count.MemoryCounter;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.LuaError;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.LuaState;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.LuaTable;
import org.figuramc.figura.script_languages.lua.math.Vector4API;

/**
 * Class where all metatables for types are stored
 */
public class FiguraMetatables extends MarkedObjectBase {

    // Fields containing metatables, to access quickly java-side

    // Math objects
    public final LuaTable vec2;
    public final LuaTable vec3;
    public final LuaTable vec4;


    public FiguraMetatables(LuaState state) throws LuaError {

        // Math objects

        vec2 = null; // TODO
        vec3 = null;
        vec4 = Vector4API.createMetatable(state, this);

    }


    @Override
    protected long traceNoMark(MemoryCounter counter, int depth) {
        // Trace everything
        counter.trace(vec2, depth);
        counter.trace(vec3, depth);
        counter.trace(vec4, depth);
        return OBJECT_SIZE + POINTER_SIZE * 32; // Idk guess
    }
}
