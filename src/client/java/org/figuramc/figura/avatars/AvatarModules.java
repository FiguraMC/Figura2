package org.figuramc.figura.avatars;

import com.google.common.collect.ImmutableMap;
import org.figuramc.figura.avatars.components.MolangStateComponent;
import org.figuramc.figura.data.ModuleImporter;
import org.figuramc.figura.data.ModuleImportingException;
import org.figuramc.figura.data.ModuleMaterials;
import org.figuramc.figura.directory.FiguraDir;
import org.figuramc.figura.model.part.FiguraModelPart;
import org.figuramc.figura.script_hooks.ScriptRuntime;
import org.figuramc.figura.script_hooks.callback.CallbackType;
import org.figuramc.figura.script_hooks.callback.ScriptCallback;
import org.figuramc.figura.script_hooks.mem_count.AllocationTracker;
import org.figuramc.figura.script_languages.molang.AllMolangQueries;
import org.figuramc.figura.script_languages.molang.MolangInstance;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

// Tracks all modules in an avatar, including the main module and its dependencies
public class AvatarModules {

    // Return a list of load-time modules.
    // The last module in the list is the main module.
    // Dependencies always come before the dependent.
    public static List<LoadTimeModule> loadModules(ModuleMaterials materials) throws ModuleImportingException, IOException {
        ArrayList<LoadTimeModule> list = new ArrayList<>();
        loadModule(list, materials, new HashMap<>());
        return list;
    }

    private static int loadModule(ArrayList<LoadTimeModule> out, ModuleMaterials materials, Map<String, Integer> alreadyImported) throws ModuleImportingException, IOException {
        Path commonModules = FiguraDir.COMMON_MODULES.get();
        LinkedHashMap<String, Integer> dependencyIndices = new LinkedHashMap<>();
        for (var dep : materials.metadata().dependencies().entrySet()) {
            String givenName = dep.getKey();
            String dependency = dep.getValue();
            // Get dependency if not already imported
            if (!alreadyImported.containsKey(dependency)) {
                Path dependencyPath = commonModules.resolve(dependency);
                ModuleMaterials dependencyMats = ModuleImporter.importPath(dependencyPath);
                int index = loadModule(out, dependencyMats, alreadyImported);
                alreadyImported.put(dependency, index);
            }
            // Save index to map
            dependencyIndices.put(givenName, alreadyImported.get(dependency));
        }
        // Now that dependencies are processed, add this module
        int index = out.size();
        LoadTimeModule module = new LoadTimeModule(index, materials, dependencyIndices);
        out.add(module);
        return index;
    }

    // "Module at load time" is different from "Module at runtime"!
    // After loading finishes, all LoadTimeModule should be deleted.
    // RuntimeModule will remain. RuntimeModule contains only information necessary after loading completes.

    // Representation of a singular module in the avatar, during load time.
    // May be updated and mutated by components it's used in
    public static class LoadTimeModule {

        public final int index; // Index of this module in the
        public ModuleMaterials materials; // This will take up lots of memory, so we will null it out at a later stage once it's done being used.

        public final LinkedHashMap<String, Integer> dependencyIndices; // Indices of dependent modules in the list, by name
        public @Nullable ScriptRuntime runtime; // The runtime used by this module, if any

        // Molang instance for this module
        private @Nullable MolangInstance<MolangStateComponent> molang;

        public @Nullable FiguraModelPart entityRoot; // This module's entity root

        private LoadTimeModule(int index, ModuleMaterials materials, LinkedHashMap<String, Integer> dependencyIndices) {
            this.index = index;
            this.materials = materials;
            this.dependencyIndices = dependencyIndices;
        }

        public MolangInstance<MolangStateComponent> getOrCreateMolang(@Nullable MolangStateComponent molangState, @Nullable AllocationTracker allocationTracker) throws AvatarError {
            if (molang == null) molang = new MolangInstance<>(molangState, allocationTracker, AllMolangQueries.AVATAR_QUERIES);
            return molang;
        }

    }

    // A module in the avatar, represented at runtime.
    // Contains necessary info for runtime.
    public static class RuntimeModule {

        public final int index; // Index of this module

        private final int[] dependencyIndices; // Indices of this module's dependencies
        private boolean initialized = false;
        private final boolean autoInitializeDependencies; // Whether this module will automatically initialize its dependencies
        public final ImmutableMap<String, CallbackType.Func<?, ?>> api; // The typed functions this module is expected to provide
        public final Map<String, ScriptCallback<?, ?>> callbacks; // The functions this module *did* provide (after initialization)

        public final @Nullable ScriptRuntime runtime; // The script runtime this module uses (if any)

        // The molang instance for this module.
        // It's shared between all molang expressions using this module.
        // The actor, the MolangStateComponent instance, is shared between all modules in an avatar.
        // Used for:
        // - Animation keyframes
        // - Custom fancy text components in future? { color = "math.lerp(#FF0000, #00FFFF, q.text_progress)" } for a gradient :3333
        public final MolangInstance<MolangStateComponent> molang;

        private static final int SIZE_ESTIMATE =
                AllocationTracker.OBJECT_SIZE * 4
                + AllocationTracker.REFERENCE_SIZE * 5
                + AllocationTracker.INT_SIZE
                + AllocationTracker.BOOLEAN_SIZE * 2;

        public RuntimeModule(LoadTimeModule loadTime, @Nullable AllocationTracker allocationTracker) throws AvatarError {
            this.index = loadTime.index;
            this.autoInitializeDependencies = loadTime.materials.metadata().autoRequireDependencies();
            this.api = ImmutableMap.copyOf(loadTime.materials.metadata().api());
            this.callbacks = new HashMap<>();
            this.dependencyIndices = loadTime.dependencyIndices.values().stream().mapToInt(x -> x).toArray();
            this.runtime = loadTime.runtime;
            this.molang = loadTime.molang;
            // Track!
            if (allocationTracker != null) {
                int totalSize = SIZE_ESTIMATE;
                totalSize += dependencyIndices.length * AllocationTracker.INT_SIZE;
                totalSize += api.size() + AllocationTracker.REFERENCE_SIZE * 4;
                for (var entry : api.entrySet()) {
                    totalSize += entry.getKey().length() * AllocationTracker.CHAR_SIZE + AllocationTracker.OBJECT_SIZE;
                    allocationTracker.track(entry.getValue(), entry.getValue().getSize());
                }
                allocationTracker.track(this, totalSize);
            }
        }

        // Initialize this module, given the list of all RuntimeModules.
        // (TODO Should we have some kind of cycle detection? Yes, but it doesn't have to be here at runtime, it can be at import time)
        public void initialize(List<RuntimeModule> allRuntimeModules) throws AvatarError {
            // If already initialized, we're done.
            if (initialized) return;
            initialized = true;
            // If we auto-initialize dependencies, do so now:
            if (autoInitializeDependencies)
                for (int index : dependencyIndices)
                    allRuntimeModules.get(index).initialize(allRuntimeModules);
            // Then initialize this module.
            if (runtime != null) runtime.initModule(this);
        }

    }


}
