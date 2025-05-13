package org.figuramc.figura.data;

import org.figuramc.figura.util.IOUtils;
import org.figuramc.figura.util.ListUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Something responsible for importing and checking scripts.
 * One is registered per language.
 */
public interface ScriptImporter {

    // Find the scripts in this module, and return them in a consistently-ordered list.
    List<ModuleMaterials.ScriptMaterials> findScripts(Path root) throws ModuleImportingException, IOException;

    // Built-in implementations
    Map<String, ScriptImporter> IMPORTERS = new HashMap<>() {{
        put("lua", new ScriptImporter() {
            @Override
            public List<ModuleMaterials.ScriptMaterials> findScripts(Path root) throws ModuleImportingException, IOException {
                Path scriptsRoot = root.resolve("scripts");
                // Ensure scripts/main.lua exists
                Path mainLua = scriptsRoot.resolve("main.lua");
                if (!Files.exists(mainLua))
                    throw new ModuleImportingException("figura.error.importing.scripts.lua.no_main");
                // Fetch all files. IOUtils.recursiveProcess returns a consistently ordered list.
                return IOUtils.recursiveProcess(scriptsRoot, path -> {
                    String name = IOUtils.stringRelativeTo(path, scriptsRoot);
                    // Ensure it's using .lua file extension, then strip extension
                    if (!name.endsWith(".lua"))
                        throw new ModuleImportingException("figura.error.importing.scripts.unexpected_extension", name, "lua", ".lua");
                    name = name.substring(0, name.length() - ".lua".length());
                    // Return script
                    byte[] data = Files.readAllBytes(path);
                    return List.of(new ModuleMaterials.ScriptMaterials(name, data));
                }, (path, inner) -> ListUtils.flatten(inner));
            }
        });
    }};

}
