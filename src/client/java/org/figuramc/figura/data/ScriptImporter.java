package org.figuramc.figura.data;

import org.figuramc.figura.util.IOUtils;
import org.figuramc.figura.util.ListUtils;
import org.figuramc.figura.util.MapUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Something responsible for importing and checking scripts.
 * One is registered per language.
 */
public interface ScriptImporter {

    // Find the scripts in this module, and return them in a consistently-ordered list.
    TreeMap<String, byte[]> findScripts(Path root) throws ModuleImportingException, IOException;

    // Built-in implementations
    Map<String, ScriptImporter> IMPORTERS = new HashMap<>() {{
        put("lua", new ScriptImporter() {
            @Override
            public TreeMap<String, byte[]> findScripts(Path root) throws ModuleImportingException, IOException {
                Path scriptsRoot = root.resolve("scripts");
                // Ensure scripts/main.lua exists
                Path mainLua = scriptsRoot.resolve("main.lua");
                if (!Files.exists(mainLua))
                    throw new ModuleImportingException("figura.error.importing.scripts.lua.no_main");
                // Fetch all files. IOUtils.recursiveProcess returns a consistently ordered list.
                return new TreeMap<>(IOUtils.<Map<String, byte[]>, ModuleImportingException>recursiveProcess(scriptsRoot,
                        path -> Map.of("", Files.readAllBytes(path)),
                        (path, inner) -> MapUtils.mergeAssertUnique(MapUtils.mapEntries(inner, (prefix, sub) -> MapUtils.mapKeys(sub, rest -> rest.isEmpty() ? prefix : prefix + "/" + rest))),
                        "lua", true
                ));
            }
        });
    }};

}
