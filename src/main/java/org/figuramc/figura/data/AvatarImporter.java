package org.figuramc.figura.data;

import com.demonwav.mcdev.annotations.Translatable;
import com.google.gson.*;
import net.minecraft.network.chat.Component;
import org.figuramc.figura.util.IOUtils;
import org.figuramc.figura.util.ListUtils;
import org.figuramc.figura.util.MapUtils;
import net.minecraft.world.item.ItemDisplayContext;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.tuple.MutablePair;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.joml.Vector4i;
import oshi.util.tuples.Pair;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;

/**
 * This class is responsible for reading a folder and outputting AvatarMaterials.
 * This is as good a place as any to specify the file layout!
 * This class is also a massive mess.
 * In the future we should make a custom blockbench model format, and clean this up dramatically.
 * <p>
 * my_avatar/
 * - avatar.json (metadata file)
 * - world/
 * - - various folders or bbmodels, which are each a world root
 * - (entity, hud)/
 * - - various folders or bbmodels which are children of the (entity, hud) root
 * - vanilla/
 * - - various folders, eventually reaching a .bbmodel
 * - - - root parts inside the bbmodel have a "figura_vanilla_root" field, with a string value.
 *       parts are grouped by what their "figura_vanilla_root" reads.
 * - items/
 * - - filename.bbmodel
 *     The file name is used to determine an item type.
 *     The content of the BBModel is used to replace the item in question when it's rendered by this avatar.
 * - textures/
 * - - various .png files which are to be imported as textures and made available to scripts etc.
 * - scripts/
 * - - various data files, possibly organized in folders. Files of all file extensions will be stored.
 */
public class AvatarImporter {

    public static final String VANILLA_ROOT_KEY = "figura_vanilla_root";
    public static final String VANILLA_ROOT_REPLACE_KEY = "figura_vanilla_root_replace";
    public static final String MESH_SKINNING_DATA_KEY = "figura_mesh_skinning_data";
    public static final String VANILLA_TEXTURE_OVERRIDE_KEY = "figura_vanilla_texture_override";

    // Call this method in another thread, most likely
    public static AvatarMaterials importFolder(Path rootFolder) throws AvatarImportingException, IOException {

        // Fetch the metadata:
        Path metadataJsonPath = rootFolder.resolve("avatar.json");
        if (!Files.exists(metadataJsonPath)) throw new AvatarImportingException("figura.error.importing.no_avatar_json");
        AvatarMaterials.MetadataMaterials metadata = getMetadata(metadataJsonPath);

        // Read in scripts and textures:
        List<AvatarMaterials.ScriptMaterials> scripts = getScripts(rootFolder.resolve("scripts"));
        ArrayList<AvatarMaterials.TextureMaterials> textures = getTextures(rootFolder.resolve("textures"));

        // Read custom items as well. Other models might have textures that link to ones defined here, so it has to happen first!
        Map<String, AvatarMaterials.CustomItemPartMaterials> customItems = parseCustomItems(rootFolder.resolve("items"), textures);

        // Handle part roots
        List<AvatarMaterials.ModelPartMaterials> worldRoots = parseModelFolder(rootFolder.resolve("world"), "world", textures).children();
        AvatarMaterials.ModelPartMaterials entityRoot = parseModelFolder(rootFolder.resolve("entity"), "entity", textures);
        AvatarMaterials.ModelPartMaterials hudRoot = parseModelFolder(rootFolder.resolve("hud"), "hud", textures);

        // Read vanilla parts
        Map<String, AvatarMaterials.VanillaRootPartMaterials> vanillaRoots = parseVanillaRoots(rootFolder.resolve("vanilla"), "vanilla", textures);

        // Return!
        return new AvatarMaterials(metadata, scripts, textures, worldRoots, entityRoot, hudRoot, vanillaRoots, customItems);
    }

    private static AvatarMaterials.MetadataMaterials getMetadata(Path path) throws AvatarImportingException, IOException {
        // Read and parse the json
        String metadataString = Files.readString(path);
        JsonElement metadata;
        try {
            metadata = metadataString.isBlank() ? new JsonObject() : JsonParser.parseString(metadataString);
        } catch (JsonSyntaxException ex) {
            throw new AvatarImportingException("figura.error.importing.invalid_json", "avatar.json");
        }
        if (!metadata.isJsonObject()) throw new AvatarImportingException("figura.error.importing.invalid_json", "avatar.json");

//        JsonObject metadataObject = metadata.getAsJsonObject();
//        JsonElement arrMaybe = metadataObject.get("shared_scripts");
//        if (arrMaybe == null) arrMaybe = new JsonArray();
//        if (!arrMaybe.isJsonArray()) throw new AvatarImportingException("Invalid metadata file - \"shared_scripts\" key should be a list of strings!");
//        List<String> sharedScripts = ListUtils.map(arrMaybe.getAsJsonArray().asList(), ExceptionUtils.wrapAny(
//                JsonElement::getAsString,
//                t -> new AvatarImportingException("Invalid metadata file - \"shared_scripts\" key should be a list of strings!", t)
//        ));

        // Return the metadata.
        return new AvatarMaterials.MetadataMaterials();
    }

    private static List<AvatarMaterials.ScriptMaterials> getScripts(Path scriptRoot) throws IOException {
        if (!Files.exists(scriptRoot)) return List.of();
        if (!Files.isDirectory(scriptRoot)) return List.of();
        ArrayList<AvatarMaterials.ScriptMaterials> scripts = ListUtils.map(FileUtils.listFiles(scriptRoot.toFile(), null, true), file -> {
            Path path = file.toPath();
            String relativizedName = scriptRoot.relativize(path).toString().replace(File.pathSeparatorChar, '/'); // Cursed path OS-specific stuff
            byte[] data = Files.readAllBytes(path);
            return new AvatarMaterials.ScriptMaterials(relativizedName, data);
        });
        scripts.sort(Comparator.comparing(AvatarMaterials.ScriptMaterials::name)); // Sort for consistency
        return scripts;
    }

    private static ArrayList<AvatarMaterials.TextureMaterials> getTextures(Path texturesRoot) throws IOException {
        if (!Files.exists(texturesRoot)) return new ArrayList<>();
        if (!Files.isDirectory(texturesRoot)) return new ArrayList<>();
        ArrayList<AvatarMaterials.TextureMaterials> textures = ListUtils.map(FileUtils.listFiles(texturesRoot.toFile(), new String[]{"png"}, true), file -> {
            Path path = file.toPath();
            String relativizedName = texturesRoot.relativize(path).toString().replace(File.pathSeparatorChar, '/'); // Cursed path OS-specific stuff
            relativizedName = IOUtils.stripExtension(relativizedName, "png"); // Strip png extension
            boolean noAtlas = relativizedName.endsWith(".noatlas");
            if (noAtlas) relativizedName = relativizedName.substring(0, relativizedName.length() - ".noatlas".length());
            byte[] data = Files.readAllBytes(path);

            return new AvatarMaterials.TextureMaterials.OwnedTexture(relativizedName, path, data, noAtlas);
        });
        textures.sort(Comparator.comparing(AvatarMaterials.TextureMaterials::name)); // Sort for consistency
        return textures;
    }

    // If the file is a .bbmodel, parse it.
    // If it's a folder, recurse.
    private static Map<String, AvatarMaterials.VanillaRootPartMaterials> parseVanillaRoots(Path path, String prefix, ArrayList<AvatarMaterials.TextureMaterials> textures) throws AvatarImportingException, IOException {
        File file = path.toFile();
        // If nonexistent: Return empty map
        if (!file.exists()) return Map.of();
        // If directory: Recurse
        if (file.isDirectory()) {
            Map<String, AvatarMaterials.VanillaRootPartMaterials> children = new HashMap<>();
            String newPrefix = prefix + "/" + file.getName();
            for (File subfile : getSubFiles(file)) {
                Map<String, AvatarMaterials.VanillaRootPartMaterials> childParts = parseVanillaRoots(subfile.toPath(), subfile.isDirectory() ? newPrefix : prefix, textures);
                for (Map.Entry<String, AvatarMaterials.VanillaRootPartMaterials> childEntry : childParts.entrySet()) {
                    // Get or create the root part, with the file's name
                    AvatarMaterials.VanillaRootPartMaterials root = children.computeIfAbsent(childEntry.getKey(), k ->
                        new AvatarMaterials.VanillaRootPartMaterials(new AvatarMaterials.ModelPartMaterials(file.getName(), new Vector3f(), new Vector3f(), new ArrayList<>(), -1, List.of(), List.of()), new MutableBoolean(false)));

                    // Add this entry as child of the root part, and mark it replace-root if it should be
                    root.replaceRoot().setValue(root.replaceRoot().getValue() || childEntry.getValue().replaceRoot().getValue());
                    root.modelPartMaterials().children().add(childEntry.getValue().modelPartMaterials());
                }
            }
            return children;
        }
        // If it's a bbmodel: process the json.
        // The following code is rife with potential for various runtime exceptions from badly formatted files... TODO deal with this?
        if (file.getName().endsWith(".bbmodel")) {
            String fileName = IOUtils.stripExtension(file.getName(), "bbmodel");
            String jsonText = Files.readString(path);
            JsonObject bbmodel = JsonParser.parseString(jsonText).getAsJsonObject();

            // Read textures and elements
            JsonObject dummyFakePart = new JsonObject();
            dummyFakePart.add("children", bbmodel.getAsJsonArray("outliner"));
            Map<String, Integer> relativeGroupIds = readRelativeGroupIds(dummyFakePart, new MutableInt(0), 0, new HashMap<>());
            String newPrefix = prefix + "/" + fileName;
            Map<String, Element> elements = readTexturesAndElements(bbmodel, newPrefix, textures, relativeGroupIds);

            // Read the outliner!
            JsonArray outliner = bbmodel.getAsJsonArray("outliner");
            Map<String, AvatarMaterials.VanillaRootPartMaterials> modelParts = new HashMap<>();
            for (JsonElement element : outliner) {
                JsonObject outlinerMember = element.getAsJsonObject();
                // Figure out which root this is part of:
                @Nullable String vanillaRoot = getStringOrDefault(outlinerMember, VANILLA_ROOT_KEY, null);
                if (vanillaRoot == null || vanillaRoot.isBlank()) {
                    Component partName = getErrorStringOrDefault(outlinerMember, "name", "figura.error.importing.unnamed_group");
                    String fullFileName = prefix + "/" + file.getName();
                    throw new AvatarImportingException("figura.error.importing.no_vanilla_root", partName, fullFileName);
                }
                boolean replace = outlinerMember.has(VANILLA_ROOT_REPLACE_KEY) && outlinerMember.get(VANILLA_ROOT_REPLACE_KEY).getAsBoolean();
                // Get or create the part for this root. Note the ArrayList<> to make it editable!
                AvatarMaterials.VanillaRootPartMaterials partRoot = modelParts.computeIfAbsent(vanillaRoot, v ->
                        new AvatarMaterials.VanillaRootPartMaterials(new AvatarMaterials.ModelPartMaterials(fileName, new Vector3f(), new Vector3f(), new ArrayList<>(), -1, List.of(), List.of()), new MutableBoolean(false)));
                // Parse this model part recursively and add it as a child of the root
                AvatarMaterials.ModelPartMaterials mats = readOutlinerMember(outlinerMember, new Vector3f(), elements);
                // NOW ZERO OUT ORIGIN AND ROTATION! THOSE ARE IGNORED, AS IT ALL INHERITS FROM THE VANILLA PART!
                mats.origin().zero();
                mats.rotation().zero();
                // Modify the part accordingly
                partRoot.modelPartMaterials().children().add(mats);
                partRoot.replaceRoot().setValue(partRoot.replaceRoot().getValue() || replace);
            }
            // And return the map!
            return modelParts;
        }
        // Nothing here, just return empty map
        return Map.of();
    }

    private static final Map<String, ItemDisplayContext> DISPLAY_CONTEXTS_BY_KEY = ListUtils.associateBy(Arrays.asList(ItemDisplayContext.values()), ItemDisplayContext::getSerializedName);

    private static Map<String, AvatarMaterials.CustomItemPartMaterials> parseCustomItems(Path path, ArrayList<AvatarMaterials.TextureMaterials> textures) throws AvatarImportingException, IOException {
        File file = path.toFile();
        if (!file.exists()) return Map.of();
        if (!file.isDirectory()) return Map.of();

        Map<String, MutablePair<Pair<AvatarMaterials.ModelPartMaterials, EnumMap<ItemDisplayContext, AvatarMaterials.ItemPartTransform>>, Integer>> discovered = new HashMap<>();

        // Get valid files, put pngs first in case bbmodels reference them.
        List<File> files = new ArrayList<>();
        for (File subfile : getSubFiles(file)) {
            if (subfile.getName().endsWith(".bbmodel"))
                files.addLast(subfile);
            else if (subfile.getName().endsWith(".png"))
                files.addFirst(subfile);
        }

        // Iterate
        for (File subfile : files) {
            if (subfile.getName().endsWith(".bbmodel")) {

                // Fetch the pattern, which is just the file name
                String pattern = IOUtils.stripExtension(subfile.getName(), "bbmodel");

                var pair = discovered.computeIfAbsent(pattern, k -> new MutablePair<>(null, null));

                // Parse the bbmodel normally:
                String jsonText = Files.readString(subfile.toPath());
                JsonObject bbmodel = JsonParser.parseString(jsonText).getAsJsonObject();
                AvatarMaterials.ModelPartMaterials materials = parseModel(bbmodel, subfile.getName(), "items", textures);

                // Now also parse the "display" tag:
                EnumMap<ItemDisplayContext, AvatarMaterials.ItemPartTransform> transforms = new EnumMap<>(ItemDisplayContext.class);
                JsonObject display;
                if ((display = bbmodel.getAsJsonObject("display")) != null) {
                    for (var entry : display.entrySet()) {
                        String key = entry.getKey();
                        ItemDisplayContext context = DISPLAY_CONTEXTS_BY_KEY.get(key);
                        if (context == null) {
                            String fullFileName = "items/" + file.getName();
                            String allContexts = String.join(", ", DISPLAY_CONTEXTS_BY_KEY.keySet());
                            throw new AvatarImportingException("figura.error.importing.unknown_item_display_context", fullFileName, key, allContexts);
                        }
                        JsonObject value = entry.getValue().getAsJsonObject();
                        Vector3f translation = parseVec3(value.get("translation"));
                        Vector3f rotation = parseVec3(value.get("rotation"));
                        Vector3f scale = parseVec3(value.get("scale"), () -> new Vector3f(1, 1, 1));
                        AvatarMaterials.ItemPartTransform transform = new AvatarMaterials.ItemPartTransform(translation, rotation, scale);
                        transforms.put(context, transform);
                    }
                }

                pair.left = new Pair<>(materials, transforms);
            } else if (subfile.getName().endsWith(".png")) {
                String pattern = IOUtils.stripExtension(subfile.getName(), "png");
                boolean noAtlas = pattern.endsWith(".noatlas");
                pattern = IOUtils.stripExtension(pattern, "noatlas");

                var pair = discovered.computeIfAbsent(pattern, k -> new MutablePair<>(null, null));

                byte[] data = Files.readAllBytes(subfile.toPath());
                textures.add(new AvatarMaterials.TextureMaterials.OwnedTexture("items/" + pattern, subfile.toPath(), data, noAtlas));
                pair.right = textures.size() - 1;
            }
        }

        return MapUtils.mapValues(discovered, pair1 -> {
            AvatarMaterials.ModelPartMaterials partMats = pair1.left != null ? pair1.left.getA() : null;
            EnumMap<ItemDisplayContext, AvatarMaterials.ItemPartTransform> transforms = pair1.left != null ? pair1.left.getB() : null;
            int textureId = pair1.right != null ? pair1.right : -1;
            return new AvatarMaterials.CustomItemPartMaterials(partMats, transforms, textureId);
        });
    }

    private static AvatarMaterials.ModelPartMaterials parseModelFolder(Path path, String prefix, ArrayList<AvatarMaterials.TextureMaterials> textures) throws AvatarImportingException, IOException {
        File file = path.toFile();
        // If nonexistent: Return empty part
        if (!file.exists() || !file.isDirectory()) return new AvatarMaterials.ModelPartMaterials(file.getName(), new Vector3f(), new Vector3f(), List.of(), -1, List.of(), List.of());
        // It exists and is a directory: recurse on valid files
        if (file.isDirectory()) {
            List<AvatarMaterials.ModelPartMaterials> children = new ArrayList<>();
            String newPrefix = prefix + "/" + file.getName();
            for (File subfile : getSubFiles(file)) {
                if (subfile.isDirectory()) {
                    children.add(parseModelFolder(subfile.toPath(), newPrefix, textures));
                } else if (subfile.getName().endsWith(".bbmodel")) {
                    String jsonText = Files.readString(subfile.toPath());
                    JsonObject bbmodel = JsonParser.parseString(jsonText).getAsJsonObject();
                    children.add(parseModel(bbmodel, subfile.getName(), prefix, textures));
                }
            }
            return new AvatarMaterials.ModelPartMaterials(file.getName(), new Vector3f(), new Vector3f(), children, -1, List.of(), List.of());
        }
        throw new IllegalStateException("Internal error in Figura avatar importing; attempt to import invalid model folder? Please report to devs!");
    }

    private static AvatarMaterials.ModelPartMaterials parseModel(JsonObject bbmodel, String fileName, String prefix, ArrayList<AvatarMaterials.TextureMaterials> textures) throws AvatarImportingException {
        // Adjust file name and prefix
        fileName = IOUtils.stripExtension(fileName, "bbmodel");
        prefix = prefix + "/" + fileName;

        // Create a dummy json object, so we can treat the "outliner" list as a "children" list,
        // and just recurse using readOutlinerMember().
        JsonObject dummyFakePart = new JsonObject();
        dummyFakePart.add("name", new JsonPrimitive(fileName));
        dummyFakePart.add("children", bbmodel.getAsJsonArray("outliner"));

        // Read textures and elements, also group IDs
        Map<String, Integer> relativeGroupIds = readRelativeGroupIds(dummyFakePart, new MutableInt(0), 0, new HashMap<>());
        Map<String, Element> elements = readTexturesAndElements(bbmodel, prefix, textures, relativeGroupIds);

        return readOutlinerMember(dummyFakePart, new Vector3f(), elements);
    }


    private static final String[] DIRECTIONS = new String[] { "north", "east", "south", "west", "up", "down" };
    private static Map<String, Element> readTexturesAndElements(JsonObject bbmodel, String bbmodelPath, ArrayList<AvatarMaterials.TextureMaterials> textures, Map<String, Integer> relativeGroupIds) throws AvatarImportingException {
        // Read textures from the bbmodel and deal with them
        List<Integer> localToGlobalTextureMapping = new ArrayList<>();
        List<Vector2f> localTextureUvMappers = new ArrayList<>();
        for (JsonElement textureElement : bbmodel.getAsJsonArray("textures")) {
            JsonObject texture = textureElement.getAsJsonObject();
            // Create the local texture uv mapper:
            float uv_width = texture.get("uv_width").getAsFloat();
            float uv_height = texture.get("uv_height").getAsFloat();
            localTextureUvMappers.add(new Vector2f(uv_width, uv_height));

            // If the texture has a vanilla override, then cancel out the rest and just do this:
            String override;
            if (texture.has(VANILLA_TEXTURE_OVERRIDE_KEY) && (override = texture.get(VANILLA_TEXTURE_OVERRIDE_KEY).getAsString()) != null && !override.isBlank()) {
                textures.add(new AvatarMaterials.TextureMaterials.VanillaTexture(override));
                localToGlobalTextureMapping.add(textures.size() - 1);
            } else {
                // If the texture has a path, and the path points to something in the avatar folder, use it instead.
                @Nullable Path linkedPath = null;
                if (texture.has("path")) {
                    JsonElement path = texture.get("path");
                    if (path.isJsonPrimitive() && ((JsonPrimitive) path).isString()) {
                        linkedPath = Path.of(path.getAsString());
                    }
                }

                // If this texture has a linked path that pointed to an existing texture, reuse that one. Otherwise, add a new one.
                @Nullable Path finalLinkedPath = linkedPath;
                int globalTextureIndex = finalLinkedPath == null ? -1 : ListUtils.findIndex(textures, tex ->
                    tex instanceof AvatarMaterials.TextureMaterials.OwnedTexture ownedTex && finalLinkedPath.equals(ownedTex.path()));
                if (globalTextureIndex != -1) {
                    // Already existed, reuse it
                    localToGlobalTextureMapping.add(globalTextureIndex);
                } else {
                    // Didn't exist, need to add a new texture

                    // Get the name of the texture. Strip ".png" if it has it.
                    String textureName = IOUtils.stripExtension(texture.get("name").getAsString(), "png");
                    // Textures named .noatlas.png will have their noAtlas flag set to true!
                    boolean noAtlas = textureName.endsWith(".noatlas");
                    if (noAtlas) textureName = textureName.substring(0, textureName.length() - ".noatlas".length());

                    String longTextureName = bbmodelPath + "/" + textureName;
                    String base64Source = texture.get("source").getAsString();
                    if (!base64Source.startsWith("data:image/png;base64,"))
                        throw new AvatarImportingException("figura.error.importing.unknown_texture_data_header", longTextureName);
                    String rest = base64Source.substring("data:image/png;base64,".length());
                    byte[] pngData = Base64.getDecoder().decode(rest);
                    // Texture has no path
                    AvatarMaterials.TextureMaterials newTexture = new AvatarMaterials.TextureMaterials.OwnedTexture(longTextureName, null, pngData, noAtlas);
                    textures.add(newTexture);
                    localToGlobalTextureMapping.add(textures.size() - 1);
                }
            }
        }

        // Read elements and build a map with UUID keys
        Map<String, Element> elements = new HashMap<>();
        for (JsonElement elementElement : bbmodel.getAsJsonArray("elements")) {
            JsonObject elementJson = elementElement.getAsJsonObject();
            String uuid = elementJson.get("uuid").getAsString();
            String type = elementJson.get("type").getAsString();
            Element element;
            Vector3f origin = parseVec3(elementJson.get("origin"));
            Vector3f rotation = parseVec3(elementJson.get("rotation"));
            if (type.equals("cube")) {
                // Parse cube data
                Map<Integer, AvatarMaterials.CubeFace[]> cubeFacesByTexture = new HashMap<>();
                JsonObject facesJson = elementJson.getAsJsonObject("faces");

                for (int i = 0; i < 6; i++) {
                    String direction = DIRECTIONS[i];
                    JsonObject face = facesJson.getAsJsonObject(direction);
                    JsonElement tex = face.get("texture");
                    if (tex != null && !tex.isJsonNull()) {
                        int localTex = tex.getAsInt();
                        int mappedTex = localToGlobalTextureMapping.get(localTex);
                        AvatarMaterials.CubeFace[] facesForThisTexture = cubeFacesByTexture.computeIfAbsent(mappedTex, x -> new AvatarMaterials.CubeFace[6]);
                        int rot = face.has("rotation") ? face.get("rotation").getAsInt() / 90 : 0;
                        Vector4f uv = parseVec4(face.get("uv"));
                        // Fix the uv based off of local texture (NOT the mapped texture!)
                        Vector2f localUvMapper = localTextureUvMappers.get(localTex);
                        uv.div(localUvMapper.x, localUvMapper.y, localUvMapper.x, localUvMapper.y);
                        facesForThisTexture[i] = new AvatarMaterials.CubeFace(uv, rot);
                    }
                }

                Vector3f from = parseVec3(elementJson.get("from"));
                Vector3f to = parseVec3(elementJson.get("to"));
                element = new Element(MapUtils.mapValues(cubeFacesByTexture, faces ->
                        new VertexHaver.Cube(new AvatarMaterials.CubeData(from, to, origin, rotation, faces))
                ));
            } else if (type.equals("mesh")) {
                // Parse mesh data

                // Get vertex skinning data, if present
                Map<String, AvatarMaterials.@Nullable SkinningData> skinningData = new HashMap<>();
                if (elementJson.has(MESH_SKINNING_DATA_KEY)) {
                    JsonObject skinningDataJson = elementJson.getAsJsonObject(MESH_SKINNING_DATA_KEY);
                    for (var vert : skinningDataJson.entrySet()) {
                        String key = vert.getKey();
                        JsonArray parts = vert.getValue().getAsJsonArray();
                        if (parts.isEmpty()) continue; // Skip if there's not actually any data in there
                        if (parts.size() > 4) throw new AvatarImportingException("figura.error.importing.skinning_part_cap", getErrorStringOrDefault(elementJson, "name", "figura.error.importing.unnamed_mesh"), bbmodelPath);
                        Vector4i offsets = new Vector4i(-1);
                        Vector4f weights = new Vector4f(0);
                        int component = 0;
                        for (JsonElement partElem : parts) {
                            JsonObject part = partElem.getAsJsonObject();
                            String partUUID = part.get("uuid").getAsString();
                            float weight = part.get("weight").getAsFloat();
                            int relativeOffset = relativeGroupIds.get(partUUID) - relativeGroupIds.get(uuid);
                            if (relativeOffset < 0) throw new AvatarImportingException("figura.error.importing.negative_skinning_offset", getErrorStringOrDefault(elementJson, "name", "figura.error.importing.unnamed_mesh"), bbmodelPath);
                            offsets.setComponent(component, relativeOffset);
                            weights.setComponent(component++, weight);
                        }
                        skinningData.put(key, new AvatarMaterials.SkinningData(offsets, weights));
                    }
                }

                // Read all vertices, and get a map from their string ID to their index:
                Map<String, Integer> indices = new HashMap<>();
                List<AvatarMaterials.VertexData> vertices = new ArrayList<>();
                for (var vert : elementJson.getAsJsonObject("vertices").entrySet()) {
                    Vector3f pos = parseVec3(vert.getValue());
                    @Nullable AvatarMaterials.SkinningData vertSkinningData = skinningData.get(vert.getKey());
                    vertices.add(new AvatarMaterials.VertexData(pos, vertSkinningData));
                    indices.put(vert.getKey(), vertices.size() - 1);
                }

                Map<Integer, Pair<List<Vector2f>, List<Vector4i>>> meshFacesByTexture = new HashMap<>();

                // Read all faces
                for (var facePair : elementJson.getAsJsonObject("faces").entrySet()) {
                    JsonObject face = facePair.getValue().getAsJsonObject();
                    JsonObject uvs = face.getAsJsonObject("uv");
                    JsonArray faceVertices = face.getAsJsonArray("vertices");
                    // Only take triangles and quads
                    if (faceVertices.size() < 3 || faceVertices.size() > 4) continue;
                    JsonElement tex = face.get("texture");
                    // Skip null faces
                    if (!tex.isJsonNull()) {
                        int localTex = tex.getAsInt();
                        int mappedTex = localToGlobalTextureMapping.get(localTex);
                        Pair<List<Vector2f>, List<Vector4i>> facesForThisTexture = meshFacesByTexture.computeIfAbsent(mappedTex, x -> new Pair<>(new ArrayList<>(), new ArrayList<>()));
                        List<Vector2f> uvsForThisTexture = facesForThisTexture.getA();
                        List<Vector4i> indicesForThisTexture = facesForThisTexture.getB();

                        int i = 0;
                        Vector4i faceIndices = new Vector4i();
                        Vector2f[] faceUvs = new Vector2f[faceVertices.size()];
                        for (JsonElement vertexKeyE : faceVertices) {
                            String vertexKey = vertexKeyE.getAsString();
                            int index = indices.get(vertexKey);
                            Vector2f uv = parseVec2(uvs.get(vertexKey));
                            uv.div(localTextureUvMappers.get(localTex)); // Fix uv using local texture (NOT mapped!)
                            faceUvs[i] = uv;
                            faceIndices.setComponent(i++, index);
                        }

                        if (faceVertices.size() == 4) {
                            // If a quad, need to untwist it
                            untwistQuad(faceIndices, faceUvs, vertices);
                        } else {
                            // Otherwise, set 4th component to -1
                            faceIndices.w = -1;
                        }

                        // Add the UVs and indices for this texture
                        uvsForThisTexture.addAll(Arrays.asList(faceUvs));
                        indicesForThisTexture.add(faceIndices);
                    }
                }

                element = new Element(MapUtils.mapValues(meshFacesByTexture, pair ->
                        new VertexHaver.Mesh(new AvatarMaterials.MeshData(origin, rotation, vertices, pair.getA(), pair.getB()))
                ));
            } else {
                throw new RuntimeException("Unknown element type: " + type);
            }
            elements.put(uuid, element);
        }

        return elements;
    }

    private static Map<String, Integer> readRelativeGroupIds(JsonElement part, MutableInt currentId, int parentId, Map<String, Integer> mapToFill) {
        if (part.isJsonPrimitive()) {
            mapToFill.put(part.getAsString(), parentId);
            return mapToFill;
        }
        JsonObject obj = part.getAsJsonObject();
        String uuid = obj.has("uuid") ? obj.get("uuid").getAsString() : null;
        int id = currentId.getAndIncrement();
        if (uuid != null)
            mapToFill.put(uuid, id);
        for (JsonElement e : obj.getAsJsonArray("children"))
            readRelativeGroupIds(e, currentId, id, mapToFill);
        return mapToFill;
    }

    // Quad untwisting is from blockbench:
    // https://github.com/JannisX11/blockbench/blob/813379d114aab14a2ff1e40e4bba6985dfa6844e/js/outliner/mesh.js#L184
    private static final Vector3f p0 = new Vector3f(), p1 = new Vector3f(), p2 = new Vector3f(), p3 = new Vector3f();
    @SuppressWarnings("SuspiciousNameCombination")
    private static void untwistQuad(Vector4i indices, Vector2f[] faceUVs, List<AvatarMaterials.VertexData> vertices) {
        // Set up vertices
        p0.set(vertices.get(indices.x).pos());
        p1.set(vertices.get(indices.y).pos());
        p2.set(vertices.get(indices.z).pos());
        p3.set(vertices.get(indices.w).pos());
        if (testOppositeSides(p1, p2, p0, p3)) {
            // Want to convert (0, 1, 2, 3) to (2, 0, 1, 3)
            indices.set(indices.z, indices.x, indices.y, indices.w);
            Vector2f temp = faceUVs[2];
            faceUVs[2] = faceUVs[1];
            faceUVs[1] = faceUVs[0];
            faceUVs[0] = temp;
        } else if (testOppositeSides(p0, p1, p2, p3)) {
            // Convert (0, 1, 2, 3) to (0, 2, 1, 3)
            indices.set(indices.x, indices.z, indices.y, indices.w);
            Vector2f temp = faceUVs[2];
            faceUVs[2] = faceUVs[1];
            faceUVs[1] = temp;
        }
    }

    // Tests if point1 and point2 are on opposite sides of the line.
    // Assumes the points are coplanar; otherwise this idea doesn't make any sense.
    private static final Vector3f t0 = new Vector3f(), t1 = new Vector3f(), t2 = new Vector3f(), t3 = new Vector3f();
    @SuppressWarnings("SameParameterValue")
    private static boolean testOppositeSides(Vector3f linePoint1, Vector3f linePoint2, Vector3f point1, Vector3f point2) {
        // Formula: (x = cross product, . = dot product)
        // ((linePoint2 - linePoint1) x (point1 - linePoint1)) . ((linePoint2 - linePoint1) x (point2 - linePoint1)) < 0
        t0.set(linePoint1);
        t1.set(linePoint2);
        t2.set(point1);
        t3.set(point2);
        t1.sub(t0);
        t2.sub(t0);
        t3.sub(t0);
        t0.set(t1);
        t0.cross(t2);
        t1.cross(t3);
        return t0.dot(t1) < 0;
    }


    // Needs to recurse for children, that's why it's its own method
    private static AvatarMaterials.ModelPartMaterials readOutlinerMember(JsonObject outlinerMember, Vector3f absoluteParentOrigin, Map<String, Element> elements) {
        List<AvatarMaterials.ModelPartMaterials> children = new ArrayList<>();

        Map<Integer, List<VertexHaver>> vertexHavers = new HashMap<>();

        String name = outlinerMember.get("name").getAsString();
        Vector3f absoluteOrigin = parseVec3(outlinerMember.get("origin"));
        Vector3f origin = absoluteOrigin.sub(absoluteParentOrigin, new Vector3f()); // Relative origins!
        Vector3f rotation = parseVec3(outlinerMember.get("rotation"));

        for (JsonElement child : outlinerMember.getAsJsonArray("children")) {
            if (child.isJsonObject())
                children.add(readOutlinerMember(child.getAsJsonObject(), absoluteOrigin, elements)); // Recurse
            else {
                String uuid = child.getAsString();
                Element element = elements.get(uuid);
                element.byTexture.forEach((tex, haver) -> {
                    List<VertexHaver> list = vertexHavers.computeIfAbsent(tex, x -> new ArrayList<>());
                    haver.relativizeOrigin(absoluteOrigin); // Relativize the origin
                    list.add(haver);
                });
            }
        }

        // Do different things depending on the size of the vertexHavers map.
        // If it's more than 1, split the model part!
        return switch (vertexHavers.size()) {
            case 0 -> new AvatarMaterials.ModelPartMaterials(
                    name, origin, rotation, children,
                    -1, List.of(), List.of()
            );
            case 1 -> {
                var entry = vertexHavers.entrySet().iterator().next();
                int tex = entry.getKey();
                List<VertexHaver> vertexHaversForTex = entry.getValue();
                yield new AvatarMaterials.ModelPartMaterials(
                        name, origin, rotation, children,
                        tex,
                        ListUtils.mapNonNull(vertexHaversForTex, x -> x instanceof VertexHaver.Cube cube ? cube.cube : null),
                        ListUtils.mapNonNull(vertexHaversForTex, x -> x instanceof VertexHaver.Mesh mesh ? mesh.mesh : null)
                );
            }
            default -> {
                List<AvatarMaterials.ModelPartMaterials> splitInto = ListUtils.map(vertexHavers.entrySet(), entry -> {
                    int tex = entry.getKey();
                    List<VertexHaver> vertexHaversForTex = entry.getValue();
                    return new AvatarMaterials.ModelPartMaterials(
                            name + "$" + tex, // Give it a name with the tex split
                            new Vector3f(), new Vector3f(), List.of(), // Splits have 0 origin and rotation
                            tex,
                            ListUtils.mapNonNull(vertexHaversForTex, x -> x instanceof VertexHaver.Cube cube ? cube.cube : null),
                            ListUtils.mapNonNull(vertexHaversForTex, x -> x instanceof VertexHaver.Mesh mesh ? mesh.mesh : null)
                    );
                });
                yield new AvatarMaterials.ModelPartMaterials(
                        name, origin, rotation, ListUtils.concat(children, splitInto),
                        -1, List.of(), List.of()
                );
            }
        };
    }

    // Helpers for parsing vectors from json.
    // Assumes values are 0 if the json array doesn't exist (rotation tends to work like this)
    private static Vector2f parseVec2(JsonElement elem) {
        if (elem == null) return new Vector2f();
        JsonArray arr = elem.getAsJsonArray();
        return new Vector2f(arr.get(0).getAsFloat(), arr.get(1).getAsFloat());
    }
    private static Vector3f parseVec3(JsonElement elem) {
        if (elem == null) return new Vector3f();
        JsonArray arr = elem.getAsJsonArray();
        return new Vector3f(arr.get(0).getAsFloat(), arr.get(1).getAsFloat(), arr.get(2).getAsFloat());
    }
    private static Vector3f parseVec3(JsonElement elem, Supplier<Vector3f> getDefault) {
        if (elem == null) return getDefault.get();
        JsonArray arr = elem.getAsJsonArray();
        return new Vector3f(arr.get(0).getAsFloat(), arr.get(1).getAsFloat(), arr.get(2).getAsFloat());
    }
    private static Vector4f parseVec4(JsonElement elem) {
        if (elem == null) return new Vector4f(0);
        JsonArray arr = elem.getAsJsonArray();
        return new Vector4f(arr.get(0).getAsFloat(), arr.get(1).getAsFloat(), arr.get(2).getAsFloat(), arr.get(3).getAsFloat());
    }

    @SuppressWarnings("SameParameterValue")
    @Contract("_, _, !null -> !null")
    private static @Nullable String getStringOrDefault(JsonObject object, String key, @Nullable String defaultVal) {
        JsonElement elem = object.get(key);
        if (elem == null || !elem.isJsonPrimitive()) return defaultVal;
        JsonPrimitive prim = elem.getAsJsonPrimitive();
        if (!prim.isString()) return defaultVal;
        return prim.getAsString();
    }


    @SuppressWarnings("SameParameterValue")
    private static Component getErrorStringOrDefault(JsonObject object, String key, @Translatable String defaultVal) {
        JsonElement elem = object.get(key);
        if (elem.isJsonPrimitive() && !elem.isJsonNull())
            return Component.literal(elem.getAsString());
        return Component.translatable(defaultVal);
    }

    // Helper for sub-files because java is annoying.
    private static File @NotNull [] getSubFiles(File directory) throws AvatarImportingException {
        assert directory.isDirectory();
        File @Nullable[] subFiles = directory.listFiles();
        if (subFiles == null) throw new AvatarImportingException("figura.error.internal.subfile_io_error", directory.getName());
        return subFiles;
    }

    // Helper algebraic type for storing different elements
    private record Element(Map<Integer, VertexHaver> byTexture) {}
    private sealed interface VertexHaver {

        void relativizeOrigin(Vector3f parentOrigin);

        record Cube(AvatarMaterials.CubeData cube) implements VertexHaver {
            @Override
            public void relativizeOrigin(Vector3f parentOrigin) {
                cube.from().sub(parentOrigin);
                cube.to().sub(parentOrigin);
                cube.origin().sub(parentOrigin);
            }
        }
        record Mesh(AvatarMaterials.MeshData mesh) implements VertexHaver {
            @Override
            public void relativizeOrigin(Vector3f parentOrigin) {
                mesh.origin().sub(parentOrigin);
            }
        }
    }

}
