package org.figuramc.figura.avatars;

import org.figuramc.figura.data.ModuleImporter;
import org.figuramc.figura.data.ModuleImportingException;
import org.figuramc.figura.data.ModuleMaterials;
import org.figuramc.figura.directory.FiguraDir;
import org.figuramc.figura.model.part.FiguraModelPart;
import org.figuramc.figura.script_hooks.ScriptRuntime;
import org.figuramc.figura.script_hooks.callback.CallbackType;
import org.figuramc.figura.script_hooks.callback.ScriptCallback;
import org.figuramc.figura.util.MapUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

// Tracks all modules in an avatar, including the main module and its dependencies
public class AvatarModules {

    // List of all modules; last module in the list is the main module.
    // Dependencies always come before the dependent.
    public final ArrayList<Module> modules;

    public AvatarModules(ModuleMaterials mainModuleMaterials) throws ModuleImportingException, IOException {
        modules = new ArrayList<>();
        addModule(mainModuleMaterials, new HashMap<>());
    }

    // Return the index of the module added
    // TODO detect and error on cyclic dependencies!!!
    private int addModule(ModuleMaterials materials, Map<String, Integer> alreadyImported) throws ModuleImportingException, IOException {
        Path commonModules = FiguraDir.COMMON_MODULES.get();
        Map<String, Integer> dependencyIndices = new HashMap<>();
        for (var dep : materials.metadata().dependencies().entrySet()) {
            String givenName = dep.getKey();
            String dependency = dep.getValue();
            // Get dependency if not already imported
            if (!alreadyImported.containsKey(dependency)) {
                Path dependencyPath = commonModules.resolve(dependency);
                ModuleMaterials dependencyMats = ModuleImporter.importPath(dependencyPath);
                int index = addModule(dependencyMats, alreadyImported);
                alreadyImported.put(dependency, index);
            }
            // Save index to map
            dependencyIndices.put(givenName, alreadyImported.get(dependency));
        }
        // Now that dependencies are processed, add this module
        int index = modules.size();
        Module module = new Module(index, materials, dependencyIndices);
        modules.add(module);
        return index;
    }

    public Module mainModule() {
        return modules.getLast();
    }

    // Representation of a singular module in the avatar
    // May be updated and mutated by components it's used in
    public class Module {

        public final int index; // Index of this module in the
        public ModuleMaterials materials; // This will take up lots of memory, so we will null it out at a later stage once it's done being used.
        private final boolean autoRequireDependencies;
        public final LinkedHashMap<String, CallbackType.Func<?, ?>> api;

        public final Map<String, Integer> dependencyIndices; // Indices of dependent modules in the list, by name
        public @Nullable ScriptRuntime runtime; // The runtime used by this module, if any
        private boolean initialized = false; // Whether it's been initialized yet

        public @Nullable FiguraModelPart entityRoot; // This module's entity root
        public final Map<String, ScriptCallback<?, ?>> callbacks = new HashMap<>(); // Exposed callbacks

        private Module(int index, ModuleMaterials materials, Map<String, Integer> dependencyIndices) {
            this.index = index;
            this.materials = materials;
            autoRequireDependencies = materials.metadata().autoRequireDependencies();
            api = materials.metadata().api();
            this.dependencyIndices = dependencyIndices;
        }

        // Get dependencies as a map...
        public Map<String, AvatarModules.Module> dependencies() {
            return MapUtils.mapValues(dependencyIndices, AvatarModules.this.modules::get);
        }

        // Free the materials, so they don't take up memory unnecessarily.
        // This will run at the end of initialization, BEFORE main thread initialization.
        // Materials are never tracked in memory, so don't hold onto them after using them to init the Avatar!
        public void freeMaterials() {
            materials = null;
        }

        // Initialize this module, should run on the main thread.
        // By default, modules will init() their dependencies before themselves.
        // (TODO Should we have some kind of cycle detection? Yes, but it doesn't have to be here at runtime, it can be at import time!)
        public void initScript() throws AvatarError {
            assert materials == null; // It's been cleared by the time this runs!
            if (initialized) return;
            initialized = true;
            // If we auto-require dependencies, do so now:
            if (autoRequireDependencies) {
                for (var index : dependencyIndices.values()) {
                    AvatarModules.this.modules.get(index).initScript();
                }
            }
            // Then initialize this module.
            if (runtime != null) runtime.initModule(this);
        }

    }


}
