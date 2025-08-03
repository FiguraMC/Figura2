package org.figuramc.figura.script_languages.lua;

import net.minecraft.network.chat.Component;
import org.figuramc.figura.FiguraModClient;
import org.figuramc.figura.animation.Animation;
import org.figuramc.figura.animation.AnimationInstance;
import org.figuramc.figura.animation.Vec3Keyframe;
import org.figuramc.figura.avatars.AvatarError;
import org.figuramc.figura.avatars.AvatarModules;
import org.figuramc.figura.avatars.components.Scripts;
import org.figuramc.figura.model.part.RiggedHierarchy;
import org.figuramc.figura.script_hooks.ScriptRuntime;
import org.figuramc.figura.script_hooks.callback.CallbackType;
import org.figuramc.figura.script_languages.lua.animations.AnimationInstanceAPI;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.*;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.compiler.CompileException;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.compiler.LoadState;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.function.LibFunction;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.function.LuaClosure;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.function.LuaFunction;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.interrupt.InterruptAction;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.lib.Bit32Lib;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.lib.CoreLibraries;
import org.figuramc.figura.script_languages.lua.events.EventsTable;
import org.figuramc.figura.script_languages.lua.math.FiguraMath;
import org.figuramc.figura.script_languages.lua.model_parts.ModelPartAPI;
import org.figuramc.figura.script_languages.lua.vanilla.VanillaTable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class LuaRuntime implements ScriptRuntime {

    // Lua State
    public final LuaState state;

    // Map each module to its environment, so we can initialize them
    private final Map<AvatarModules.Module, LuaTable> moduleEnvironments = new IdentityHashMap<>();

    /**
     * Create a Lua runtime, but do not run any user code yet. This requires:
     * - A Scripts component (through which we fetch the avatar)
     * - AvatarModules
     */
    public LuaRuntime(Scripts scriptsComponent, AvatarModules allModules) throws AvatarError {

        // LuaError can happen at basically any time, so wrap the whole thing :P
        try {
            // Create the LuaState.
            this.state = LuaState.builder(scriptsComponent.getAvatar())
                    .interruptHandler(() -> {
                        // TODO throw an uncatchable error for timeout
                        return InterruptAction.CONTINUE;
                    })
                    .allocationTracker(scriptsComponent.getAllocationTracker()) // Pass the allocation tracker
                    .build();

            // Add basic globals
            CoreLibraries.standardGlobals(state);
            Bit32Lib.add(state);

            // Type metatables are shared across modules
            state.figuraMetatables.addTypesTo(state.globals());

            // Add more stuff to math and table APIs
            FiguraMath.init(state);
            FiguraTable.init(state);

            // Add events
            EventsTable.createEventsTable(state, scriptsComponent.eventListeners);

            // Benchmark testers, todo remove
            state.globals().rawset("micros", LibFunction.create(s -> LuaDouble.valueOf(System.nanoTime() / 1000d)));
            state.globals().rawset("print", LibFunction.create((s, v) -> { System.out.println(v); return Constants.NIL; }));

            // Animation testing, todo remove
            Animation testAnimation = new Animation(Map.of(
                    "left_arm", new Animation.TransformKeyframes(
                            null,
                            List.of(
                                    new Vec3Keyframe(0, 0, 0, 0),
                                    new Vec3Keyframe(1, 90, 0, 0),
                                    new Vec3Keyframe(2, 0, 0, 0)
                            ),
                            null
                    ),
                    "head", new Animation.TransformKeyframes(
                            null,
                            null,
                            List.of(
                                    new Vec3Keyframe(0, 1, 1, 1),
                                    new Vec3Keyframe(1, 2, 1, 1),
                                    new Vec3Keyframe(2, 1, 2, 1),
                                    new Vec3Keyframe(3, 2, 2, 1),
                                    new Vec3Keyframe(4, 1, 1, 1)
                            )
                    )
            ));
            state.globals().rawset("testAnimationBind", LibFunction.create((s, v) -> {
                RiggedHierarchy<?> part = v.checkUserdata(s, RiggedHierarchy.class);
                AnimationInstance instance = new AnimationInstance(testAnimation, part);
                return AnimationInstanceAPI.wrap(instance, s);
            }));

            // If we have vanilla rendering, add the "vanilla" table
            if (scriptsComponent.vanillaRendering != null)
                state.globals().rawset("vanilla", VanillaTable.create(state, scriptsComponent.vanillaRendering));

            // Set up separate env for each lua module:
            for (AvatarModules.Module module : allModules.modules) {
                // Ensure this module uses Lua before setting it up here
                if (!"lua".equals(module.materials.metadata().language())) continue;
                // Create an _ENV for this module with its stuff
                LuaTable _ENV = new LuaTable(state.allocationTracker);
                // Set _ENV's metatable to have __index = globals, so things in state.globals() are shared across allModules
                _ENV.setMetatable(state, ValueFactory.tableOf(state.allocationTracker, Constants.INDEX, state.globals()));
                // Save the environment so we can initialize
                moduleEnvironments.put(module, _ENV);
                // Create globals unique to this module:

                // models:
                LuaTable models = ValueFactory.tableOf(state.allocationTracker);
                _ENV.rawset("models", models);
                if (module.entityRoot != null) models.rawset("entity", ModelPartAPI.wrap(module.entityRoot, state));

                // Create require() for this module
                _ENV.rawset("require", FiguraRequire.createRequire(state, _ENV, module));
            }
        } catch (LuaError error) {
            throw new AvatarError("figura.error.internal.general", error, "Lua state failed to initialize");
        }
    }

    @Override
    public void runCode(String snippet) throws AvatarError {
        throw new AvatarError(Component.literal("TODO"));
    }

    @Override
    public void initModule(AvatarModules.Module module) throws AvatarError {
        try {
            LuaTable env = moduleEnvironments.get(module);
            if (env == null) throw new AvatarError("figura.error.internal.general", "Attempt to initialize non-Lua module with Lua runtime?");
            // Compile and run entrypoint, which is just "require 'main'"
            LuaClosure entrypoint = LoadState.load(state, new ByteArrayInputStream("return require 'main'".getBytes(StandardCharsets.UTF_8)), "=ENTRYPOINT", env);
            LuaValue res = LuaThread.run(new LuaThread(state, entrypoint), Constants.NONE).first();
            // Fetch resulting table, and generate script callbacks for the module
            for (var entry : module.api.entrySet()) {
                String funcName = entry.getKey();
                CallbackType.Func<?, ?> funcType = entry.getValue();
                LuaTable table = res.checkTable(state, "main.lua is expected to return a table to implement module's API");
                LuaFunction func = table.rawget(funcName).checkFunction(state, "Expected main.lua to provide a function for key \"" + funcName + "\", but got " + table.rawget(funcName).typeName());
                LuaCallback<?, ?> callback = new LuaCallback<>(funcType, state, func);
                module.callbacks.put(funcName, callback);
            }
        } catch (LuaError luaError) {
            throw new AvatarError(Component.literal(luaError.getMessage().replace("\t", "  ")), luaError);
        } catch (CompileException impossible) {
            throw new IllegalStateException("Failed to compile Lua entrypoint. Should be impossible. Bug in Figura, please report!", impossible);
        }
    }

    @Override
    public void destroy() {

    }

    // Helper methods to run a file defined in /assets/figura/scripts/lua/
    public static Varargs runAssetFile(LuaState state, String name) throws AvatarError {
        return runAssetFile(state, name, Constants.NONE);
    }
    public static Varargs runAssetFile(LuaState state, String name, Varargs args) throws AvatarError {
        try(InputStream input = FiguraModClient.class.getResourceAsStream("/assets/figura/scripts/lua/" + name + ".lua")) {
            // Compile the file
            if (input == null) throw new AvatarError("figura.error.internal.general", "Figura is missing the builtin file \"" + name + ".lua\"");
            LuaClosure c = LoadState.load(state, input, "=" + name.toUpperCase(), state.globals());
            // Execute the file
            return LuaThread.run(new LuaThread(state, c), args);
        } catch (IOException e) {
            throw new AvatarError("figura.error.internal.general", e, "Figura is missing the builtin file \"" + name + ".lua\"");
        } catch (CompileException e) {
            throw new AvatarError("figura.error.internal.general", e, "Figura failed to compile builtin file \"" + name + ".lua\"");
        } catch (LuaError e) {
            throw new AvatarError("figura.error.internal.general", e, "Figura failed to run builtin file \"" + name + ".lua\"");
        }
    }


}
