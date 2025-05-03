package org.figuramc.figura.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.lang3.tuple.MutablePair;
import org.figuramc.figura.util.IOUtils;
import org.figuramc.figura.util.JsonUtils;
import org.figuramc.figura.util.ListUtils;
import org.figuramc.figura.util.MapUtils;
import org.jetbrains.annotations.Nullable;
import oshi.util.tuples.Pair;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class NewAvatarImporter {

    public static AvatarMaterials importPath(Path root) throws AvatarImportingException, IOException {

        var metadata = readMetadata(root);

        Path scriptsRoot = root.resolve("scripts");
        var scripts = IOUtils.recursiveProcess(scriptsRoot, path -> List.of(readScript(path, scriptsRoot)), (_p, s) -> ListUtils.flatten(s));

        Path texturesRoot = root.resolve("textures");
        var textures = IOUtils.recursiveProcess(texturesRoot,
                path -> path.toString().endsWith(".png") ? new ArrayList<>(List.of(readTexture(path, texturesRoot))) : null, (_p, s) -> ListUtils.flatten(s));

        // Read custom items first, because they can add new textures which are then used later
        var items = readCustomItems(root, textures);

        var entity = readRecursiveModel(root, "entity", textures);
        var hud = readRecursiveModel(root, "hud", textures);
        var world = readRecursiveModel(root, "world", textures).children();

        return new AvatarMaterials(metadata, scripts, textures, world, entity, hud, items);
    }

    private static AvatarMaterials.MetadataMaterials readMetadata(Path root) throws AvatarImportingException, IOException {
        Path metadataPath = root.resolve("avatar.json");
        if (!Files.exists(metadataPath)) throw new AvatarImportingException("figura.error.importing.no_avatar_json");
        String str = Files.readString(metadataPath);
        // If empty, return default materials.
        if (str.isBlank()) return new AvatarMaterials.MetadataMaterials();
        // Otherwise, parse as json and read materials.
        JsonObject obj = JsonParser.parseString(str).getAsJsonObject();
        return new AvatarMaterials.MetadataMaterials();
    }

    private static AvatarMaterials.ScriptMaterials readScript(Path path, Path scriptsRoot) throws IOException {
        String name = IOUtils.stringRelativeTo(path, scriptsRoot);
        byte[] data = Files.readAllBytes(path);
        return new AvatarMaterials.ScriptMaterials(name, data);
    }
    private static AvatarMaterials.TextureMaterials readTexture(Path path, Path texturesRoot) throws IOException {
        String name = IOUtils.stringRelativeTo(path, texturesRoot);
        name = IOUtils.stripExtension(name, "png");
        boolean noAtlas = name.endsWith(".noatlas");
        if (noAtlas) name = name.substring(0, name.length() - ".noatlas".length());
        byte[] data = Files.readAllBytes(path);
        return new AvatarMaterials.TextureMaterials.OwnedTexture(name, path, data, noAtlas);
    }

    private static AvatarMaterials.ModelPartMaterials readRecursiveModel(Path root, String name, ArrayList<AvatarMaterials.TextureMaterials> textures) throws AvatarImportingException, IOException {
        return IOUtils.recursiveProcess(root.resolve(name),
                figmodel -> readFigModel(root, figmodel, textures),
                (folder, models) -> AvatarMaterials.ModelPartMaterials.wrapper(folder.toFile().getName(), models)
        );
    }

    private static Map<String, AvatarMaterials.CustomItem> readCustomItems(Path root, ArrayList<AvatarMaterials.TextureMaterials> textures) throws AvatarImportingException, IOException {
        Map<String, MutablePair<AvatarMaterials.@Nullable CustomItemModel, Integer>> pairs = new TreeMap<>(); // Treemap for sorted keys, consistency
        // Recursive process, with no return value, just mutates the pairs.
        IOUtils.<Void, AvatarImportingException>recursiveProcess(root.resolve("items"),
                path -> {
                    if (path.toFile().getName().endsWith(".png")) {
                        // Get pattern and pair
                        String pattern = path.toFile().getName().substring(0, path.toFile().getName().length() - ".png".length());
                        var pair = pairs.computeIfAbsent(pattern, x -> new MutablePair<>(null, -1));
                        // If we already have a texture for this pattern, error out!
                        if (pair.right != -1) throw new AvatarImportingException("Custom Item pattern 'pattern' has multiple textures defined!");
                        // Figure out what int to give. If the tex already exists, reuse it, otherwise read a new one
                        int alreadyExists = ListUtils.findIndex(textures, tex -> tex instanceof AvatarMaterials.TextureMaterials.OwnedTexture owned && path.equals(owned.path()));
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
                        if (pair.left != null) throw new AvatarImportingException("Custom Item pattern 'pattern' has multiple models defined!");
                        // Parse the model and store in pair
                        @Nullable AvatarMaterials.CustomItemModel model = readCustomItemModel(root, path, textures);
                        assert model != null;
                        pair.left = model;
                    }
                    return null;
                }, (p, m) -> null);
        // Map values and return
        return MapUtils.mapValues(pairs, pair -> new AvatarMaterials.CustomItem(pair.left, pair.right));
    }

    private static @Nullable AvatarMaterials.ModelPartMaterials readFigModel(Path root, Path path, ArrayList<AvatarMaterials.TextureMaterials> textures) throws AvatarImportingException, IOException {
        if (!path.toString().endsWith(".figmodel")) return null;
        String fullName = IOUtils.stringRelativeTo(path, root);
        int lastSlash = fullName.lastIndexOf('/');
        String prefix = fullName.substring(0, lastSlash);
        String fileName = fullName.substring(lastSlash + 1, fullName.length() - ".figmodel".length());
        JsonObject json = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
        return FigModelImporter.parseFigModel(fileName, prefix, json, textures);
    }

    private static @Nullable AvatarMaterials.CustomItemModel readCustomItemModel(Path root, Path path, ArrayList<AvatarMaterials.TextureMaterials> textures) throws AvatarImportingException, IOException {
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
