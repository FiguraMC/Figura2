package org.figuramc.figura.script_languages.lua;

import org.figuramc.figura.script_hooks.mem_count.MarkedObjectBase;
import org.figuramc.figura.script_hooks.mem_count.MemoryCounter;
import org.figuramc.figura.script_languages.lua.animations.AnimationInstanceAPI;
import org.figuramc.figura.script_languages.lua.cobalt.cc.tweaked.cobalt.internal.unwind.SuspendedAction;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.*;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.function.Dispatch;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.function.LibFunction;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.function.LuaFunction;
import org.figuramc.figura.script_languages.lua.events.EventListenerAPI;
import org.figuramc.figura.script_languages.lua.math.vector.Vector4API;
import org.figuramc.figura.script_languages.lua.model_parts.FigmodelAPI;
import org.figuramc.figura.script_languages.lua.model_parts.ModelPartAPI;
import org.figuramc.figura.script_languages.lua.model_parts.PartLikeAPI;
import org.figuramc.figura.script_languages.lua.vanilla.VanillaPartAPI;
import org.jetbrains.annotations.Nullable;

import static org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.Constants.INDEX;

/**
 * Class where metatables for wrapped java types are stored
 */
public class FiguraMetatables extends MarkedObjectBase {

    // Fields containing metatables, to access quickly java-side

    // General
    public final LuaTable callback; // ScriptCallback
    public final LuaTable eventListener; // EventListener

    // Math objects
    public final LuaTable vec2;
    public final LuaTable vec3;
    public final LuaTable vec4;

    // Model parts
    public final LuaTable transformable;
    public final LuaTable modelPart;
    public final LuaTable figmodelModelPart;
    public final LuaTable customItemModelPart;

    // Vanilla rendering
    public final LuaTable vanillaPart; // VanillaRendering.ScriptVanillaPart
//    public final LuaTable vanillaRenderLayer;

    // Animation stuff
    public final LuaTable animationInstance;

    // Lua-specific
//    public final LuaTable promise; // Promise

    public FiguraMetatables(LuaState state) throws LuaError {
        // General
        callback = CallbackAPI.createMetatable(state, this);
        eventListener = EventListenerAPI.createMetatable(state, this);

        // Math objects
        vec2 = null; // TODO
        vec3 = null;
        vec4 = Vector4API.createMetatable(state, this);

        // Model part
        transformable = PartLikeAPI.createMetatable(state, this);
        modelPart = ModelPartAPI.createMetatable(state, this);
        figmodelModelPart = FigmodelAPI.createMetatable(state, this);
        customItemModelPart = modelPart; // TODO

        // Vanilla rendering
        vanillaPart = VanillaPartAPI.createMetatable(state, this);

        // Animations
        animationInstance = AnimationInstanceAPI.createMetatable(state, this);

        // Lua-specific
//        promise = LuaPromise.createMetatable(state, this);
//        eventLoop = LuaEventLoop.createMetatable(state, this);
    }

    // Add type metatables, with PascalCase keys, to the given table
    public void addTypesTo(LuaTable table) throws LuaError {
        table.rawset("Callback", callback);
        table.rawset("EventListener", eventListener);
//        table.rawset("Promise", promise);
//        table.rawset("Vec2", vec2);
//        table.rawset("Vec3", vec3);
        table.rawset("Vec4", vec4);
        table.rawset("Part", transformable);
        table.rawset("FiguraPart", modelPart);
        table.rawset("Figmodel", figmodelModelPart);
        table.rawset("VanillaPart", vanillaPart);
        table.rawset("AnimationInstance", animationInstance);
    }

    // Helper method to set up inheritance relationship between subclass and superclass metatables,
    // and also deals with any custom __index implementations.
    // Make sure to call this on EVERY created metatable, even ones without superclasses, so that indexing works as it should.
    // This should also be the last thing called in the class, I think. (Maybe it doesn't matter...?)
    public static void setupIndexing(LuaState state, LuaTable thisMetatable, @Nullable LuaTable superclassMetatable, @Nullable LuaFunction customIndexer) throws LuaError {
        // If there's no superclass metatable, and no custom indexer, just make it simple.
        if (superclassMetatable == null && customIndexer == null) {
            thisMetatable.rawset(INDEX, thisMetatable); // Set __index to itself, and we're done
            return;
        }
        // If there's a superclass involved:
        if (superclassMetatable != null) {
            // If there's a superclass involved, make sure to copy all metamethods from the superclass
            // (lua doesn't provide a way to do this any better, sadly :/)
            superclassMetatable.forEach((k, v) -> {
                if (!k.isString()) return;
                if (!thisMetatable.rawget(k).isNil()) return; // If subclass overrides this metamethod, ignore
                String method = k.checkString(state);
                if (method.equals("__index")) return; // __index handled separately
                if (method.equals("__name")) return; // __name shouldn't be inherited
                if (method.startsWith("__")) thisMetatable.rawset(k, v); // Copy other metamethods into subclass
            });
            // Deal with the __index function separately
            LuaValue superclassIndex = superclassMetatable.rawget(INDEX);
            // If the superclass has __index as a function:
            if (superclassIndex instanceof LuaFunction superIndexFunc) {
                // If we also have a custom __index, we need to weave them together:
                if (customIndexer != null) {
                    thisMetatable.rawset(INDEX, LibFunction.createS((s, di, args) -> {
                        // First, try to fetch a method:
                        LuaValue k = args.arg(2);
                        LuaValue method = OperationHelper.getTable(s, thisMetatable, k);
                        if (!method.isNil()) return method;
                        // If no method was found, try our indexer:
                        LuaValue subclassIndexerResult = SuspendedAction.run(di, () -> Dispatch.invoke(s, customIndexer, args)).first();
                        if (!subclassIndexerResult.isNil()) return subclassIndexerResult;
                        // Finally, defer to superclass indexer.
                        return SuspendedAction.run(di, () -> Dispatch.invoke(s, superIndexFunc, args));
                    }));
                } else {
                    // We don't have a custom indexer, so just defer to the superclass's.
                    thisMetatable.rawset(INDEX, LibFunction.createS((s, di, args) -> {
                        // First, try to fetch a method:
                        LuaValue k = args.arg(2);
                        LuaValue method = OperationHelper.getTable(s, thisMetatable, k);
                        if (!method.isNil()) return method;
                        // If no method was found, fall back to the superclass's indexer.
                        return SuspendedAction.run(di, () -> Dispatch.invoke(s, superIndexFunc, args));
                    }));
                }
                return;
            } else {
                // The superclass does not have a custom __index function, so we can use table-based inheritance
                LuaTable subclassMetatableMetatable = ValueFactory.tableOf(state.allocationTracker);
                subclassMetatableMetatable.rawset(INDEX, superclassMetatable);
                thisMetatable.setMetatable(state, subclassMetatableMetatable);
            }
        }
        // If we have a custom index function, wire it up:
        if (customIndexer != null) {
            thisMetatable.rawset(INDEX, LibFunction.createS((s, di, args) -> {
                // First, try to fetch a method:
                LuaValue k = args.arg(2);
                LuaValue method = OperationHelper.getTable(s, thisMetatable, k);
                if (!method.isNil()) return method;
                // If no method was found, fall back to the custom indexer.
                return SuspendedAction.run(di, () -> Dispatch.invoke(s, customIndexer, args));
            }));
        } else {
            thisMetatable.rawset(INDEX, thisMetatable);
        }
    }

    @Override
    protected long traceNoMark(MemoryCounter counter, int depth) {
        // Trace everything
        counter.trace(callback, depth);
        counter.trace(eventListener, depth);
//        counter.trace(promise, depth);
        counter.trace(vec2, depth);
        counter.trace(vec3, depth);
        counter.trace(vec4, depth);
        counter.trace(transformable, depth);
        counter.trace(modelPart, depth);
        counter.trace(figmodelModelPart, depth);
        counter.trace(customItemModelPart, depth);
        counter.trace(vanillaPart, depth);
        counter.trace(animationInstance, depth);
        return OBJECT_SIZE + POINTER_SIZE * 32; // Idk guess
    }
}
