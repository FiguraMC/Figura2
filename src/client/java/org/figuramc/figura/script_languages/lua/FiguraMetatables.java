package org.figuramc.figura.script_languages.lua;

import org.figuramc.figura.avatars.components.Scripts;
import org.figuramc.figura.script_hooks.mem_count.MarkedObjectBase;
import org.figuramc.figura.script_hooks.mem_count.MemoryCounter;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.*;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.function.Dispatch;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.function.LibFunction;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.function.LuaFunction;
import org.figuramc.figura.script_languages.lua.math.vector.Vector4API;
import org.figuramc.figura.script_languages.lua.model_parts.ModelPartAPI;
import org.figuramc.figura.script_languages.lua.model_parts.TransformableAPI;
import org.figuramc.figura.script_languages.lua.model_parts.VanillaPartAPI;
import org.jetbrains.annotations.Nullable;

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
    public final LuaTable transformable;
    public final LuaTable modelPart;
    public final LuaTable worldRootModelPart;
    public final LuaTable customItemModelPart;

    // Vanilla rendering, requires VanillaRendering component
    public final @Nullable LuaTable vanillaModelPart; // VanillaRendering.ScriptVanillaPart
//    public final LuaTable vanillaRenderLayer;


    public FiguraMetatables(LuaState state, Scripts scriptsComponent) throws LuaError {
        // Math objects
        vec2 = null; // TODO
        vec3 = null;
        vec4 = Vector4API.createMetatable(state, this);

        // Model part
        transformable = TransformableAPI.createMetatable(state, this);
        modelPart = ModelPartAPI.createMetatable(state, this);
        worldRootModelPart = modelPart; // TODO
        customItemModelPart = modelPart;

        // Vanilla rendering, if we have the component for it
        if (scriptsComponent.vanillaRendering != null)
            vanillaModelPart = VanillaPartAPI.createMetatable(state, this, scriptsComponent.vanillaRendering);
        else vanillaModelPart = null;
    }

    // Helper method to set up inheritance relationship between subclass and superclass metatables,
    // and also deals with any custom __index implementations.
    // Make sure to call this on EVERY created metatable, even ones without superclasses, so that indexing works as it should.
    // This should also be the last thing called in the class, I think. (Maybe it doesn't matter...?)
    public static void setupInheritance(LuaState state, LuaTable subclassMetatable, @Nullable LuaTable superclassMetatable, @Nullable LuaFunction customIndexer) throws LuaError {
        // If there's no superclass metatable, and no custom indexer, just make it simple.
        if (superclassMetatable == null && customIndexer == null) {
            subclassMetatable.rawset(INDEX, subclassMetatable); // Set __index to itself, and we're done
            return;
        }
        // If there's a superclass involved:
        if (superclassMetatable != null) {
            // If there's a superclass involved, make sure to copy all metamethods from the superclass
            // (lua doesn't provide a way to do this any better, sadly :/)
            superclassMetatable.forEach((k, v) -> {
                if (!k.isString()) return;
                if (!subclassMetatable.rawget(k).isNil()) return; // If subclass overrides this metamethod, ignore
                String method = k.checkString(state);
                if (method.equals("__index")) return; // __index handled separately
                if (method.startsWith("__")) subclassMetatable.rawset(k, v); // Copy other metamethods into subclass
            });
            // Deal with the __index function separately
            LuaValue superclassIndex = superclassMetatable.rawget(INDEX);
            // If the superclass has __index as a function:
            if (superclassIndex instanceof LuaFunction superIndexFunc) {
                // If we also have a custom __index, we need to weave them together:
                if (customIndexer != null) {
                    subclassMetatable.rawset(INDEX, LibFunction.createS((s, di, args) -> {
                        // First, try to fetch a method:
                        LuaValue k = args.arg(2);
                        LuaValue method = OperationHelper.getTable(s, subclassMetatable, k);
                        if (!method.isNil()) return method;
                        // If no method was found, try our indexer:
                        LuaValue subclassIndexerResult = Dispatch.invoke(s, customIndexer, args).first();
                        if (!subclassIndexerResult.isNil()) return subclassIndexerResult;
                        // Finally, defer to superclass indexer.
                        return Dispatch.invoke(s, superIndexFunc, args);
                    }));
                } else {
                    // We don't have a custom indexer, so just defer to the superclass's.
                    subclassMetatable.rawset(INDEX, LibFunction.createS((s, di, args) -> {
                        // First, try to fetch a method:
                        LuaValue k = args.arg(2);
                        LuaValue method = OperationHelper.getTable(s, subclassMetatable, k);
                        if (!method.isNil()) return method;
                        // If no method was found, fall back to the superclass's indexer.
                        return Dispatch.invoke(s, superIndexFunc, args);
                    }));
                }
                return;
            } else {
                // The superclass does not have a custom __index function, so we can use table-based inheritance
                LuaTable subclassMetatableMetatable = ValueFactory.tableOf(state.allocationTracker);
                subclassMetatableMetatable.rawset(INDEX, superclassMetatable);
                subclassMetatable.setMetatable(state, subclassMetatableMetatable);
            }
        }
        // If we have a custom index function, wire it up:
        if (customIndexer != null) {
            subclassMetatable.rawset(INDEX, LibFunction.createS((s, di, args) -> {
                // First, try to fetch a method:
                LuaValue k = args.arg(2);
                LuaValue method = OperationHelper.getTable(s, subclassMetatable, k);
                if (!method.isNil()) return method;
                // If no method was found, fall back to the custom indexer.
                return Dispatch.invoke(s, customIndexer, args);
            }));
        } else {
            subclassMetatable.rawset(INDEX, subclassMetatable);
        }
    }

    @Override
    protected long traceNoMark(MemoryCounter counter, int depth) {
        // Trace everything
        counter.trace(vec2, depth);
        counter.trace(vec3, depth);
        counter.trace(vec4, depth);
        counter.trace(transformable, depth);
        counter.trace(modelPart, depth);
        counter.trace(worldRootModelPart, depth);
        counter.trace(customItemModelPart, depth);
        if (vanillaModelPart != null) counter.trace(vanillaModelPart, depth);
        return OBJECT_SIZE + POINTER_SIZE * 32; // Idk guess
    }
}
