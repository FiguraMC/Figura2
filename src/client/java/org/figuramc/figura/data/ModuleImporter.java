package org.figuramc.figura.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.lang3.tuple.MutablePair;
import org.figuramc.figura.script_hooks.callback.CallbackType;
import org.figuramc.figura.util.IOUtils;
import org.figuramc.figura.util.JsonUtils;
import org.figuramc.figura.util.ListUtils;
import org.figuramc.figura.util.MapUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ModuleImporter {

    public static ModuleMaterials importPath(Path root) throws ModuleImportingException, IOException {

        var metadata = readMetadata(root);

        // Find script importer and use it
        ScriptImporter scriptImporter = metadata.language() != null ? ScriptImporter.IMPORTERS.get(metadata.language()) : null;
        Path scriptsRoot = root.resolve("scripts");
        List<ModuleMaterials.ScriptMaterials> scripts;
        if (scriptImporter == null) {
            // If no language is set but the module is trying to use scripts anyway, error nicely.
            if (Files.exists(scriptsRoot)) throw new ModuleImportingException("figura.error.importing.scripts.no_language_set");
            scripts = List.of();
        } else scripts = scriptImporter.findScripts(root);

        // Textures
        Path texturesRoot = root.resolve("textures");
        var textures = IOUtils.recursiveProcess(texturesRoot,
                path -> path.toString().endsWith(".png") ? new ArrayList<>(List.of(readTexture(path, texturesRoot))) : null, (_p, s) -> ListUtils.flatten(s));

        // Read custom items first, because they can potentially add new textures which are then used later
        var items = readCustomItems(root, textures);

        @Nullable ModuleMaterials.ModelPartMaterials entity = readRecursiveModel(root, "entity", textures);
        @Nullable ModuleMaterials.ModelPartMaterials hud = readRecursiveModel(root, "hud", textures);

        @Nullable ModuleMaterials.ModelPartMaterials worldOver = readRecursiveModel(root, "world", textures);
        List<ModuleMaterials.ModelPartMaterials> world = worldOver != null ? worldOver.children() : List.of();

        return new ModuleMaterials(metadata, scripts, textures, world, entity, hud, items);
    }

    private static ModuleMaterials.MetadataMaterials readMetadata(Path root) throws ModuleImportingException, IOException {
        Path metadataPath = root.resolve("avatar.json");
        if (!Files.exists(metadataPath)) throw new ModuleImportingException("figura.error.importing.no_avatar_json");
        String str = Files.readString(metadataPath);
        // If empty, return default materials.
        if (str.isBlank()) return new ModuleMaterials.MetadataMaterials(null, new LinkedHashMap<>(), new LinkedHashMap<>());
        // Otherwise, parse as json and read materials.
        JsonObject obj = JsonParser.parseString(str).getAsJsonObject();
        // Parse language:
        @Nullable String language = JsonUtils.getStringOrDefault(obj, "language", null);
        // Parse dependencies:
        LinkedHashMap<String, String> dependencies;
        {
            JsonElement e = obj.get("dependencies");
            if (e == null) {
                dependencies = new LinkedHashMap<>();
            } else if (e.isJsonObject()) {
                dependencies = new LinkedHashMap<>();
                for (var entry : e.getAsJsonObject().entrySet()) {
                    String name = entry.getKey();
                    JsonElement dep = entry.getValue();
                    if (!dep.isJsonPrimitive() || !dep.getAsJsonPrimitive().isString())
                        throw new ModuleImportingException("figura.error.importing.metadata.dependency_format");
                    String depString = dep.getAsString();
                    dependencies.put(name, depString);
                }
            } else throw new ModuleImportingException("figura.error.importing.metadata.dependency_format");
        }
        // Parse API:
        LinkedHashMap<String, CallbackType.Func> api;
        {
            JsonElement e = obj.get("api");
            if (e == null) api = new LinkedHashMap<>();
            else if (e.isJsonObject()) {
                api = new LinkedHashMap<>();
                for (var entry : e.getAsJsonObject().entrySet()) {
                    if (entry.getValue().isJsonObject()) {
                        // Parse params and return type
                        CallbackType[] params = JsonUtils.getListOrEmpty(entry.getValue().getAsJsonObject(), "params", param -> {
                            if (!param.isJsonPrimitive() || !param.getAsJsonPrimitive().isString()) throw new ModuleImportingException("figura.error.importing.metadata.api_format");
                            String t = param.getAsString();
                            return switch (t) {
                                case "bool" -> CallbackType.Bool.INSTANCE;
                                case "f64" -> CallbackType.F64.INSTANCE;
                                default -> throw new ModuleImportingException("Unexpected type \"" + t + "\" (TODO translate and fix)");
                            };
                        }).toArray(new CallbackType[0]);
                        String returnTypeString = JsonUtils.getStringOrDefault(entry.getValue().getAsJsonObject(), "return", "unit");
                        CallbackType returnType = switch (returnTypeString) {
                            case "unit" -> CallbackType.Unit.INSTANCE;
                            case "bool" -> CallbackType.Bool.INSTANCE;
                            case "f64" -> CallbackType.F64.INSTANCE;
                            default -> throw new ModuleImportingException("Unexpected type \"" + returnTypeString + "\" (TODO translate and fix)");
                        };
                        // Add to map
                        api.put(entry.getKey(), new CallbackType.Func(returnType, params));
                    } else throw new ModuleImportingException("figura.error.importing.metadata.api_format");
                }
            } else throw new ModuleImportingException("figura.error.importing.metadata.api_format");
        }
        return new ModuleMaterials.MetadataMaterials(language, dependencies, api);
    }

    private static ModuleMaterials.TextureMaterials readTexture(Path path, Path texturesRoot) throws IOException {
        String name = IOUtils.stringRelativeTo(path, texturesRoot);
        name = IOUtils.stripExtension(name, "png");
        boolean noAtlas = name.endsWith(".noatlas");
        if (noAtlas) name = name.substring(0, name.length() - ".noatlas".length());
        byte[] data = Files.readAllBytes(path);
        return new ModuleMaterials.TextureMaterials.OwnedTexture(name, path, data, noAtlas);
    }

    private static @Nullable ModuleMaterials.ModelPartMaterials readRecursiveModel(Path root, String name, ArrayList<ModuleMaterials.TextureMaterials> textures) throws ModuleImportingException, IOException {
        Path modelPath = root.resolve(name);
        if (!Files.exists(modelPath)) return null;
        return IOUtils.recursiveProcess(modelPath,
                figmodel -> readFigModel(root, figmodel, textures),
                (folder, models) -> ModuleMaterials.ModelPartMaterials.wrapper(folder.toFile().getName(), models)
        );
    }

    private static TreeMap<String, ModuleMaterials.CustomItem> readCustomItems(Path root, ArrayList<ModuleMaterials.TextureMaterials> textures) throws ModuleImportingException, IOException {
        Map<String, MutablePair<ModuleMaterials.@Nullable CustomItemModel, Integer>> pairs = new TreeMap<>(); // Treemap for sorted keys, consistency
        // Recursive process, with no return value, just mutates the pairs.
        IOUtils.<Void, ModuleImportingException>recursiveProcess(root.resolve("items"),
                path -> {
                    if (path.toFile().getName().endsWith(".png")) {
                        // Get pattern and pair
                        String pattern = path.toFile().getName().substring(0, path.toFile().getName().length() - ".png".length());
                        var pair = pairs.computeIfAbsent(pattern, x -> new MutablePair<>(null, -1));
                        // If we already have a texture for this pattern, error out!
                        if (pair.right != -1) throw new ModuleImportingException("Custom Item pattern 'pattern' has multiple textures defined!");
                        // Figure out what int to give. If the tex already exists, reuse it, otherwise read a new one
                        int alreadyExists = ListUtils.findIndex(textures, tex -> tex instanceof ModuleMaterials.TextureMaterials.OwnedTexture owned && path.equals(owned.path()));
                        if (alreadyExists != -1) {
                            pair.right = alreadyExists;
                        } else {
                            textures.add(readTexture(path, root));
                            pair.right = textures.size() - 1;
                        }
                    } else if (path.toFile().getName().endsWith(".figmodel")) {
                        // Get pattern and pair
                        String pattern = path.toFile().getName().substring(0, path.toFile().getName().length() - ".figmodel".length());
                        var pair = pairs.computeIfAbsent(pattern, x -> new MutablePair<>(null, -1));
                        // If we already have a model for this pattern, error out!
                        if (pair.left != null) throw new ModuleImportingException("Custom Item pattern 'pattern' has multiple models defined!");
                        // Parse the model and store in pair
                        @Nullable ModuleMaterials.CustomItemModel model = readCustomItemModel(root, path, textures);
                        assert model != null;
                        pair.left = model;
                    }
                    return null;
                }, (p, m) -> null);
        // Map values and return
        return MapUtils.mapValues(pairs, pair -> new ModuleMaterials.CustomItem(pair.left, pair.right), TreeMap::new);
    }

    private static @Nullable ModuleMaterials.ModelPartMaterials readFigModel(Path root, Path path, ArrayList<ModuleMaterials.TextureMaterials> textures) throws ModuleImportingException, IOException {
        if (!path.toString().endsWith(".figmodel")) return null;
        String fullName = IOUtils.stringRelativeTo(path, root);
        int lastSlash = fullName.lastIndexOf('/');
        String prefix = fullName.substring(0, lastSlash);
        String fileName = fullName.substring(lastSlash + 1, fullName.length() - ".figmodel".length());
        JsonObject json = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
        return FigModelImporter.parseFigModel(fileName, prefix, json, textures);
    }

    private static @Nullable ModuleMaterials.CustomItemModel readCustomItemModel(Path root, Path path, ArrayList<ModuleMaterials.TextureMaterials> textures) throws ModuleImportingException, IOException {
        if (!path.toString().endsWith(".figmodel")) return null;
        String fullName = IOUtils.stringRelativeTo(path, root);
        int lastSlash = fullName.lastIndexOf('/');
        assert lastSlash != -1;
        String prefix = fullName.substring(0, lastSlash);
        String fileName = fullName.substring(lastSlash + 1, fullName.length() - ".figmodel".length());
        JsonObject json = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
        return FigModelImporter.parseCustomItemModel(fileName, prefix, json, textures);
    }

}
