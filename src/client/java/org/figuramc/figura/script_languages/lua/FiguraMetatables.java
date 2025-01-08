package org.figuramc.figura.script_languages.lua;

import org.figuramc.figura.script_hooks.mem_count.MarkedObjectBase;
import org.figuramc.figura.script_hooks.mem_count.MemoryCounter;
import org.figuramc.figura.script_languages.lua.cobalt.cc.tweaked.cobalt.internal.unwind.SuspendedAction;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.*;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.function.Dispatch;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.function.LibFunction;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.function.LuaFunction;
import org.figuramc.figura.script_languages.lua.math.vector.Vector4API;
import org.figuramc.figura.script_languages.lua.model_parts.ModelPartAPI;
import org.figuramc.figura.script_languages.lua.model_parts.RootModelPartAPI;

import static org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.Constants.INDEX;

/**
 * Class where metatables for wrapped java types are stored
 */
public class FiguraMetatables extends MarkedObjectBase {

    // Fields containing metatables, to access quickly java-side

    // Math objects
    public final LuaTable vec2;
    public final LuaTable vec3;
    public final LuaTable vec4;

    // Model parts
    public final LuaTable modelPart;
    public final LuaTable rootModelPart;
    public final LuaTable vanillaRootModelPart;
    public final LuaTable worldRootModelPart;
    public final LuaTable customItemModelPart;


    public FiguraMetatables(LuaState state) throws LuaError {
        // Math objects
        vec2 = null; // TODO
        vec3 = null;
        vec4 = Vector4API.createMetatable(state, this);

        // Model part
        modelPart = ModelPartAPI.createMetatable(state, this);
        rootModelPart = RootModelPartAPI.createMetatable(state, this);
        vanillaRootModelPart = rootModelPart;
        worldRootModelPart = rootModelPart;
        customItemModelPart = rootModelPart;
    }

    // Helper method to set up inheritance relationship between subclass and superclass metatables.
    public static void setupInheritance(LuaTable subclassMetatable, LuaTable superclassMetatable) throws LuaError {
        // Create superclass __index relationship:
        LuaValue superclassIndex = superclassMetatable.rawget(INDEX);
        if (superclassIndex instanceof LuaFunction superIndexFunc) {
            subclassMetatable.rawset(INDEX, LibFunction.createS((s, di, args) -> {
                // If it's a method in subclass metatable, return it
                LuaValue method = subclassMetatable.rawget(args.arg(2)); // arg 2 is key
                if (!method.isNil()) return method;
                // Otherwise defer to superclass's __index function
                return SuspendedAction.run(di, () -> Dispatch.invoke(s, superIndexFunc, args));
            }));
        } else {
            // Superclass doesn't have an __index, so we can just set __index to the superclass metatable.
            subclassMetatable.rawset(INDEX, superclassMetatable);
        }
    }


    @Override
    protected long traceNoMark(MemoryCounter counter, int depth) {
        // Trace everything
        counter.trace(vec2, depth);
        counter.trace(vec3, depth);
        counter.trace(vec4, depth);
        counter.trace(modelPart, depth);
        counter.trace(rootModelPart, depth);
        counter.trace(vanillaRootModelPart, depth);
        counter.trace(worldRootModelPart, depth);
        counter.trace(customItemModelPart, depth);
        return OBJECT_SIZE + POINTER_SIZE * 32; // Idk guess
    }
}
