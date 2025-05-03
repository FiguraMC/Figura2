package org.figuramc.figura.script_languages.lua;

import net.minecraft.network.chat.Component;
import org.figuramc.figura.FiguraModClient;
import org.figuramc.figura.avatars.Avatar;
import org.figuramc.figura.avatars.components.EntityRoot;
import org.figuramc.figura.avatars.components.Scripts;
import org.figuramc.figura.avatars.components.VanillaRendering;
import org.figuramc.figura.manage.AvatarLoadingException;
import org.figuramc.figura.script_hooks.ScriptError;
import org.figuramc.figura.script_hooks.ScriptRuntime;
import org.figuramc.figura.script_hooks.mem_count.MarkedObjectBase;
import org.figuramc.figura.script_hooks.mem_count.MemoryCounter;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.*;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.compiler.CompileException;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.compiler.LoadState;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.function.LibFunction;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.function.LuaClosure;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.interrupt.InterruptAction;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.lib.Bit32Lib;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.lib.CoreLibraries;
import org.figuramc.figura.script_languages.lua.math.FiguraMath;
import org.figuramc.figura.script_languages.lua.model_parts.ModelPartAPI;
import org.figuramc.figura.script_languages.lua.vanilla.VanillaTable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class LuaRuntime extends MarkedObjectBase implements ScriptRuntime {

    // Lua State
    private final LuaState state;

    // Figura metatable reference, for creating/converting objects
    private final FiguraMetatables metatables;

    // Keep references to event objects, so we can call them on demand
    private final LuaValue tick, render;

    /**
     * The starting point for the creation of a Lua Runtime.
     */
    public LuaRuntime(Scripts scriptsComponent, Map<String, byte[]> scripts) throws AvatarLoadingException {
        // LuaError can happen at basically any time, so wrap the whole thing :P
        try {
            // Create the LuaState.
            this.state = LuaState.builder()
                    .interruptHandler(() -> {
                        // TODO throw an uncatchable error for timeout
                        return InterruptAction.CONTINUE;
                    })
                    .allocationTracker(scriptsComponent.getAllocationTracker()) // Pass the allocation tracker
                    .build();

            // Add basic globals
            CoreLibraries.standardGlobals(state);
            Bit32Lib.add(state, state.globals());

            // Add types with metatables
            this.metatables = new FiguraMetatables(state, scriptsComponent);

            // Other
            FiguraRequire.createRequire(state, scripts); // Setup require()
            FiguraMath.init(state, metatables);
            FiguraTable.init(state);

            // Set up events (also fetch the event objects for calling later):
            LuaTable defaultEvents = FiguraEvents.init(state, "tick", "render");
            tick = defaultEvents.rawget("tick");
            render = defaultEvents.rawget("render");

            // Benchmark testers, todo remove
            state.globals().rawset("micros", LibFunction.create(s -> ValueFactory.valueOf(System.nanoTime() / 1000d)));
            state.globals().rawset("print", LibFunction.create((s, v) -> { System.out.println(v); return Constants.NIL; }));

            // Add global variable "models" because why not. Also todo make more organized.
            LuaTable models = ValueFactory.tableOf(state.allocationTracker);
            state.globals().rawset("models", models);
            if (scriptsComponent.entityRoot != null)
                models.rawset("entity", ModelPartAPI.wrap(scriptsComponent.entityRoot.getModelPart(), metatables));

            // If we have the vanilla rendering component, add the vanilla rendering script elements
            if (scriptsComponent.vanillaRendering != null)
                state.globals().rawset("vanilla", VanillaTable.create(state, metatables, scriptsComponent.vanillaRendering));

        } catch (LuaError error) {
            throw new AvatarLoadingException("figura.error.internal.script.lua.setup_failed", error, false);
        }
    }

    @Override
    public void runCode(String snippet) throws ScriptError {
        throw new ScriptError(Component.literal("TODO"));
    }

    @Override
    public void init() throws ScriptError {
        // TODO maybe add some kind of auto scripts behavior? For now it just does require 'main.lua'
        try {
            LuaClosure entrypoint = LoadState.load(state, new ByteArrayInputStream("require 'main.lua'".getBytes(StandardCharsets.UTF_8)), "=ENTRYPOINT", state.globals());
            LuaThread.runMain(state, entrypoint);
        } catch (LuaError luaError) {
            throw new ScriptError(Component.literal(luaError.getMessage().replace("\t", "  ")));
        } catch (CompileException impossible) {
            throw new IllegalStateException("Failed to compile Lua entrypoint. Should be impossible. Bug in Figura, please report!", impossible);
        }
    }

    private void call(LuaValue event, Varargs args) throws ScriptError {
        try {
            LuaThread.runMain(state, event, args);
        } catch (LuaError luaError) {
            throw new ScriptError(Component.literal(luaError.getMessage().replace("\t", "  ")));
        }
    }

    @Override
    public void tick() throws ScriptError {
        call(tick, Constants.NONE);
    }

    @Override
    public void render(float tickDelta) throws ScriptError {
        call(render, LuaDouble.valueOf(tickDelta));
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

    // Helper methods to run a file defined in /assets/figura/scripts/lua/
    public static Varargs runAssetFile(LuaState state, String name) throws AvatarLoadingException {
        return runAssetFile(state, name, Constants.NONE);
    }
    public static Varargs runAssetFile(LuaState state, String name, Varargs args) throws AvatarLoadingException {
        try(InputStream input = FiguraModClient.class.getResourceAsStream("/assets/figura/scripts/lua/" + name + ".lua")) {
            // Compile the file
            if (input == null) throw new AvatarLoadingException("figura.error.internal.missing_file", name + ".lua");
            LuaClosure c = LoadState.load(state, input, "=" + name.toUpperCase(), state.globals());
            // Execute the file
            return LuaThread.runMain(state, c, args);
        } catch (IOException e) {
            throw new AvatarLoadingException("figura.error.internal.missing_file", e, false, name + ".lua");
        } catch (CompileException e) {
            throw new AvatarLoadingException("figura.error.internal.script.lua.compile_error", e, false, name + ".lua");
        } catch (LuaError e) {
            throw new AvatarLoadingException("figura.error.internal.script.lua.runtime_error", e, false, name + ".lua");
        }
    }


}
