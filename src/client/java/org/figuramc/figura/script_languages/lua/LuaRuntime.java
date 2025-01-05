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
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.function.*;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.interrupt.InterruptAction;
import org.joml.Vector3f;

import static org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.ValueFactory.*;

import java.io.ByteArrayInputStream;
import java.util.Map;

public class LuaRuntime extends MarkedObjectBase implements ScriptRuntime {

    private final Avatar<?> avatar;
    private final LuaState state;

    // Figura metatable reference, for creating/converting objects
    private final FiguraMetatables metatables;

    // Keys to the registry
    public static final LuaString REQUIRE_KEY = LuaString.valueOf(null, "figura_require");

    // Keep references to event objects
    private final LuaValue tick, render;

    /**
     * The starting point for the creation of a Lua Runtime.
     */
    public LuaRuntime(Avatar<?> avatar, Map<String, byte[]> scripts) throws AvatarLoadingException {
        // Save avatar
        this.avatar = avatar;
        // LuaError can happen at basically any time, so wrap the whole thing :P
        try {
            // Create the LuaState.
            this.state = LuaState.builder()
                    .interruptHandler(() -> {
                        // TODO throw an uncatchable error for timeout
                        return InterruptAction.CONTINUE;
                    })
                    .allocationTracker(avatar.getAllocationTracker()) // Pass the allocation tracker
                    .build();

            // Add types with metatables
            this.metatables = new FiguraMetatables(state);

            // Add lua-based APIs

            // Event / events:
            LuaTable defaultEvents = EventsAPI.init(state, "tick", "render");
            tick = defaultEvents.rawget("tick");
            render = defaultEvents.rawget("render");

            // Define require() using the passed scripts:
            // Fill in the require data table in the registry.
            // Use a registry table for memory tracing
            // TODO move to new file so i dont have to look at it
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

    private void call(LuaValue event, String name, Varargs args) throws ScriptError {
        try {
            Dispatch.invoke(state, event, args);
        } catch (LuaError luaError) {
            throw new ScriptError("Error in Lua during \"" + name + "\" event", luaError);
        } catch (UnwindThrowable impossible) {
            throw new IllegalStateException("Should be impossible. Bug in Figura, please report!", impossible);
        }
    }

    @Override
    public void tick() throws ScriptError {
        call(tick, "tick", Constants.NONE);
    }

    @Override
    public void render(float tickDelta) throws ScriptError {
        call(render, "render", LuaDouble.valueOf(tickDelta));
    }

    @Override
    public void destroy() {

    }

    // Memory tracing!
    @Override
    protected long traceNoMark(MemoryCounter counter, int depth) {
        counter.trace(state, depth);
        counter.trace(metatables, depth);
        counter.trace(tick, depth);
        counter.trace(render, depth);
        return 64; // Idk, random guess
    }


}
