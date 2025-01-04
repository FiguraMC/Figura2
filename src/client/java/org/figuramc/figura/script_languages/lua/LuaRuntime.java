package org.figuramc.figura.script_languages.lua;

import net.minecraft.client.Minecraft;
import org.figuramc.figura.avatars.Avatar;
import org.figuramc.figura.avatars.components.EntityRoot;
import org.figuramc.figura.avatars.components.Scripts;
import org.figuramc.figura.manage.AvatarLoadingException;
import org.figuramc.figura.model.part.FiguraModelPart;
import org.figuramc.figura.script_hooks.ScriptError;
import org.figuramc.figura.script_hooks.ScriptRuntime;
import org.figuramc.figura.script_hooks.mem_count.AllocationTracker;
import org.figuramc.figura.script_hooks.mem_count.MarkedObjectBase;
import org.figuramc.figura.script_hooks.mem_count.MemoryCounter;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.*;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.compiler.CompileException;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.compiler.LuaC;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.function.LibFunction;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.function.LuaClosure;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.function.LuaInterpretedFunction;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.interrupt.InterruptAction;
import org.joml.Vector3d;
import org.joml.Vector3f;

import static org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.ValueFactory.*;

import java.io.ByteArrayInputStream;
import java.util.Map;

public class LuaRuntime extends MarkedObjectBase implements ScriptRuntime {

    private final Avatar<?> avatar;
    private final LuaState state;

    private static final LuaString REQUIRE_KEY = LuaString.valueOf(null, "figura_require");

    /**
     * The starting point for the creation of a Lua Runtime.
     */
    public LuaRuntime(Avatar<?> avatar, Map<String, byte[]> scripts) throws AvatarLoadingException {
        // Save avatar
        this.avatar = avatar;
        // LuaError can happen at any time, so wrap the whole thing :P
        try {
            // Create the LuaState.
            this.state = LuaState.builder()
                    .interruptHandler(() -> {
                        // TODO throw an uncatchable error for timeout
                        return InterruptAction.CONTINUE;
                    })
                    .allocationTracker(avatar.getAllocationTracker()) // Pass the allocation tracker
                    .build();

            // Temp placeholder api, for some funsies?
            AllocationTracker t = avatar.getAllocationTracker();
            EntityRoot entityRoot = avatar.optionalDependency(EntityRoot.class, Scripts.class);
            if (entityRoot != null) {
                state.globals().rawset("models", userdataOf(entityRoot.getModelPart(), tableOf(t, Constants.INDEX, tableOf(
                        t,
                        LuaString.valueOf(t, "setPos"), LibFunction.createV((state, args) -> {
                            FiguraModelPart part = args.arg(1).checkUserdata(state, FiguraModelPart.class);
                            Vector3f pos = new Vector3f(
                                    (float) args.arg(2).checkDouble(state),
                                    (float) args.arg(3).checkDouble(state),
                                    (float) args.arg(4).checkDouble(state)
                            );
                            part.setPosition(pos);
                            return Constants.NONE;
                        })
                ))));
            }
            state.globals().rawset("time", LibFunction.create(state -> {
                return LuaDouble.valueOf(Minecraft.getInstance().level.getGameTime());
            }));

            // Define require() using the passed scripts:
            // Fill in the require data table in the registry.
            // Use a registry table for memory tracing
            LuaTable requireTable = state.registry().getSubTable(REQUIRE_KEY);
            for (var script : scripts.entrySet()) {
                String name = script.getKey();
                byte[] code = script.getValue();
                try {
                    // Compile to a closure, and put it in the require table.
                    Prototype prototype = LuaC.compile(state, new ByteArrayInputStream(code), name);
                    LuaClosure closure = new LuaInterpretedFunction(prototype);
                    requireTable.rawset(name, closure);
                } catch (CompileException compileException) {
                    throw new AvatarLoadingException("Failed to compile Lua script", compileException);
                } catch (LuaError error) {
                    throw new AvatarLoadingException("Error while compiling Lua script", error);
                }
            }
        } catch (LuaError error) {
            throw new AvatarLoadingException("Problem instantiating the Lua runtime", error);
        }
    }

    @Override
    public void runCode(String snippet) throws ScriptError {
        throw new ScriptError("Not yet implemented");
    }

    @Override
    public void init() throws ScriptError {

    }

    @Override
    public void tick() throws ScriptError {

    }

    @Override
    public void render(float tickDelta) throws ScriptError {

    }

    @Override
    public void destroy() {

    }

    // Memory tracing!
    @Override
    protected long traceNoMark(MemoryCounter counter, int depth) {
        counter.trace(state, depth);
        return 64; // Idk, random guess
    }


}
