package org.figuramc.figura.avatars;

import org.figuramc.figura.data.ModuleImporter;
import org.figuramc.figura.data.ModuleImportingException;
import org.figuramc.figura.data.ModuleMaterials;
import org.figuramc.figura.directory.FiguraDir;
import org.figuramc.figura.model.part.FiguraModelPart;
import org.figuramc.figura.script_hooks.Event;
import org.figuramc.figura.script_hooks.ScriptError;
import org.figuramc.figura.script_hooks.ScriptRuntime;
import org.figuramc.figura.script_hooks.callback.ScriptCallback;
import org.figuramc.figura.script_hooks.mem_count.MarkedObjectBase;
import org.figuramc.figura.script_hooks.mem_count.MemoryCounter;
import org.figuramc.figura.util.MapUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

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
    public class Module extends MarkedObjectBase {

        public final int index; // Index of this module in the
        public ModuleMaterials materials;
        public final Map<String, Integer> dependencyIndices; // Indices of dependent modules in the list, by name
        public @Nullable ScriptRuntime runtime; // The runtime used by this module, if any
        private boolean initialized = false; // Whether it's been initialized yet

        public @Nullable FiguraModelPart entityRoot; // This module's entity root
        public final Map<String, ScriptCallback> callbacks = new HashMap<>(); // Exposed callbacks

        private Module(int index, ModuleMaterials materials, Map<String, Integer> dependencyIndices) {
            this.index = index;
            this.materials = materials;
            this.dependencyIndices = dependencyIndices;
        }

        // Get dependencies as a map...
        public Map<String, AvatarModules.Module> dependencies() {
            return MapUtils.mapValues(dependencyIndices, AvatarModules.this.modules::get);
        }

        // Initialize this module, should run on the main thread.
        // By default, modules will init() their dependencies before themselves.
        // (TODO Should we have some kind of cycle detection? Yes, but it doesn't have to be here at runtime, it can be at import time!)
        public void initScript() throws ScriptError, Throwable {
            if (initialized) return;
            initialized = true;
            // If we auto-require dependencies, do so now:
            if (materials.metadata().autoRequireDependencies()) {
                for (var index : dependencyIndices.values()) {
                    AvatarModules.this.modules.get(index).initScript();
                }
            }
            // Then initialize this module.
            if (runtime != null) runtime.initModule(this);
        }



        @Override
        protected long traceNoMark(MemoryCounter counter, int depth) {
            counter.trace(entityRoot, depth);
            long namesSize = 0;
            for (var entry : callbacks.entrySet()) {
                namesSize += entry.getKey().length() * CHAR_SIZE;
                counter.trace(entry.getValue(), depth);
            }
            return 48 + namesSize;
        }
    }


}
