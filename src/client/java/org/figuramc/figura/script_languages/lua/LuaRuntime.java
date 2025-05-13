package org.figuramc.figura.script_languages.lua;

import net.minecraft.network.chat.Component;
import org.figuramc.figura.FiguraModClient;
import org.figuramc.figura.avatars.AvatarModules;
import org.figuramc.figura.avatars.components.Scripts;
import org.figuramc.figura.data.ModuleMaterials;
import org.figuramc.figura.manage.AvatarLoadingException;
import org.figuramc.figura.script_hooks.ScriptError;
import org.figuramc.figura.script_hooks.ScriptRuntime;
import org.figuramc.figura.script_hooks.callback.CallbackType;
import org.figuramc.figura.script_hooks.mem_count.MarkedObjectBase;
import org.figuramc.figura.script_hooks.mem_count.MemoryCounter;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.*;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.compiler.CompileException;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.compiler.LoadState;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.function.LibFunction;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.function.LuaClosure;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.function.LuaFunction;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.interrupt.InterruptAction;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.lib.Bit32Lib;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.lib.CoreLibraries;
import org.figuramc.figura.script_languages.lua.math.FiguraMath;
import org.figuramc.figura.script_languages.lua.model_parts.ModelPartAPI;
import org.figuramc.figura.script_languages.lua.vanilla.VanillaTable;
import org.figuramc.figura.util.ListUtils;
import oshi.util.tuples.Pair;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class LuaRuntime extends MarkedObjectBase implements ScriptRuntime {

    // Lua State
    public final LuaState state;

    // Figura metatable reference, for creating/converting objects
    public final FiguraMetatables metatables;

    // Reference each module that uses Lua, so we can initialize them
    private final List<Pair<AvatarModules.Module, LuaTable>> moduleEnvironments = new ArrayList<>();

    // Keep references to event objects, so we can call them on demand
    private final LuaValue tick, render;

    /**
     * Create a Lua runtime given all the allModules, and the scripts component.
     * The scripts component should be the method for component access.
     */
    public LuaRuntime(Scripts scriptsComponent, AvatarModules allModules) throws AvatarLoadingException {
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

            // Set up events (also fetch the event objects for calling later):
            LuaTable defaultEvents = FiguraEvents.init(state, "tick", "render");
            tick = defaultEvents.rawget("tick");
            render = defaultEvents.rawget("render");

            // Add to math and table APIs
            FiguraMath.init(state, metatables);
            FiguraTable.init(state);

            // Benchmark testers, todo remove
            state.globals().rawset("micros", LibFunction.create(s -> ValueFactory.valueOf(System.nanoTime() / 1000d)));
            state.globals().rawset("print", LibFunction.create((s, v) -> { System.out.println(v); return Constants.NIL; }));

            // If we have vanilla rendering, add the "vanilla" table
            if (scriptsComponent.vanillaRendering != null)
                state.globals().rawset("vanilla", VanillaTable.create(state, metatables, scriptsComponent.vanillaRendering));

            // A list-like table containing all allModules
            LuaTable allModulesTable = ValueFactory.tableOf(state.allocationTracker);
            for (AvatarModules.Module module : allModules.modules)
                allModulesTable.rawset(module.index, ModuleAPI.wrap(module, metatables));

            // Set up separate env for each lua module:
            for (AvatarModules.Module module : allModules.modules) {
                // Ensure this module uses Lua before setting it up
                if (!Objects.equals("lua", module.materials.metadata().language())) continue;
                // Create an _ENV for this module with its stuff
                LuaTable _ENV = new LuaTable(state.allocationTracker);
                // Set _ENV's metatable to have __index = globals, so things in state.globals() are shared across allModules
                _ENV.setMetatable(state, ValueFactory.tableOf(state.allocationTracker, Constants.INDEX, state.globals()));
                // Save the environment so we can initialize
                moduleEnvironments.add(new Pair<>(module, _ENV));
                // Create globals unique to this module:

                // models:
                LuaTable models = ValueFactory.tableOf(state.allocationTracker);
                _ENV.rawset("models", models);
                if (module.entityRoot != null) models.rawset("entity", ModelPartAPI.wrap(module.entityRoot, metatables));

                // modules:
                LuaTable modules = ValueFactory.tableOf(state.allocationTracker);
                _ENV.rawset("modules", modules);
                for (var entry : module.dependencyIndices.entrySet())
                    modules.rawset(entry.getKey(), allModulesTable.rawget(entry.getValue()));

                // Create require() for this module
                Map<String, byte[]> scripts = ListUtils.associateByTo(module.materials.scripts(), ModuleMaterials.ScriptMaterials::name, ModuleMaterials.ScriptMaterials::data);
                _ENV.rawset("require", FiguraRequire.createRequire(state, _ENV, module.index, scripts));
            }

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
        try {
            // Run "require 'main'" in each environment, and init modules
            for (var pair : moduleEnvironments) {
                AvatarModules.Module module = pair.getA();
                LuaTable env = pair.getB();
                // Compile and run entrypoint, which is just "require 'main'"
                LuaClosure entrypoint = LoadState.load(state, new ByteArrayInputStream("return require 'main'".getBytes(StandardCharsets.UTF_8)), "=ENTRYPOINT", env);
                LuaValue res = LuaThread.runMain(state, entrypoint).first();
                // Fetch resulting table, and generate script callbacks for the module
                for (var entry : module.materials.metadata().api().entrySet()) {
                    String funcName = entry.getKey();
                    CallbackType.Func funcType = entry.getValue();
                    LuaTable table = res.checkTable(state, "main.lua is expected to return a table to implement module's API");
                    LuaFunction func = table.rawget(funcName).checkFunction(state, "Expected main.lua to provide a function for key \"" + funcName + "\", but got " + table.rawget(funcName).typeName());
                    LuaCallback callback = new LuaCallback(funcType, this.state, this.metatables, func);
                    module.callbacks.put(funcName, callback);
                }
            }
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
