package org.figuramc.figura.script_languages.lua;

import org.figuramc.figura.avatars.Avatar;
import org.figuramc.figura.avatars.components.EntityRoot;
import org.figuramc.figura.avatars.components.Scripts;
import org.figuramc.figura.manage.AvatarLoadingException;
import org.figuramc.figura.script_hooks.ScriptError;
import org.figuramc.figura.script_hooks.ScriptRuntime;
import org.figuramc.figura.script_hooks.mem_count.MarkedObjectBase;
import org.figuramc.figura.script_hooks.mem_count.MemoryCounter;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.*;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.compiler.CompileException;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.compiler.LoadState;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.function.*;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.interrupt.InterruptAction;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.lib.*;
import org.figuramc.figura.script_languages.lua.model_parts.ModelPartAPI;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class LuaRuntime extends MarkedObjectBase implements ScriptRuntime {

    private final Avatar<?> avatar;
    private final LuaState state;

    // Figura metatable reference, for creating/converting objects
    private final FiguraMetatables metatables;

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

            // Add basic globals
            CoreLibraries.standardGlobals(state);
            Bit32Lib.add(state, state.globals());

            // Add types with metatables
            this.metatables = new FiguraMetatables(state);

            // Other
            FiguraRequire.createRequire(state, scripts); // Setup require()

            // Set up events (also fetch the event objects for calling later):
            LuaTable defaultEvents = FiguraEvents.init(state, "tick", "render");
            tick = defaultEvents.rawget("tick");
            render = defaultEvents.rawget("render");

            // Benchmark testers, todo remove
            state.globals().rawset("micros", LibFunction.create(s -> LuaDouble.valueOf(System.nanoTime() / 1000d)));
            state.globals().rawset("print", LibFunction.create((s, v) -> { System.out.println(v); return Constants.NIL; }));

            // Add global variable "models" because why not. Also todo make more organized.
            LuaTable models = ValueFactory.tableOf(state.allocationTracker);
            state.globals().rawset("models", models);
            if (avatar.optionalDependency(EntityRoot.class, Scripts.class) != null)
                models.rawset("entity", ModelPartAPI.wrap(avatar.assertDependency(EntityRoot.class, Scripts.class).getModelPart(), metatables));

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
        // TODO maybe add some kind of auto scripts behavior? For now it just does require 'main.lua'
        try {
            LuaClosure entrypoint = LoadState.load(state, new ByteArrayInputStream("require 'main.lua'".getBytes(StandardCharsets.UTF_8)), "=Figura::Entrypoint", state.globals());
            LuaThread.runMain(state, entrypoint);
        } catch (LuaError luaError) {
            throw new ScriptError("Error in Lua during init", luaError);
        } catch (CompileException impossible) {
            throw new IllegalStateException("Failed to compile Lua entrypoint. Should be impossible. Bug in Figura, please report!", impossible);
        }
    }

    private void call(LuaValue event, String name, Varargs args) throws ScriptError {
        try {
            LuaThread.runMain(state, event, args);
        } catch (LuaError luaError) {
            throw new ScriptError("Error in Lua during \"" + name + "\" event", luaError);
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
