package org.figuramc.figura.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.world.item.ItemDisplayContext;
import org.figuramc.figura.util.IOUtils;
import org.figuramc.figura.util.JsonUtils;
import org.figuramc.figura.util.ListUtils;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.joml.Vector4i;

import java.nio.file.Path;
import java.util.*;

/**
 * Class with logic to turn a .figmodel JSON object into ModelPartMaterials.
 * Also adds new textures to the list.
 */
public class FigModelImporter {

    public static AvatarMaterials.ModelPartMaterials parseFigModel(String fileName, String prefix, JsonObject figmodel, ArrayList<AvatarMaterials.TextureMaterials> textures) throws AvatarImportingException {
        try {
            // Process textures and generate a mapping
            ModelLocalTexture[] textureMapping = processTextures(figmodel.getAsJsonArray("textures"), prefix + "/" + fileName, textures);
            // Process model parts and return
            return processGroup(figmodel.getAsJsonObject("part_data"), textureMapping, fileName);
        } catch (Throwable t) {
            throw new AvatarImportingException("figura.error.importing.invalid_figmodel", prefix + "/" + fileName);
        }
    }

    // The different item render contexts, mapped from string to enum variant.
    private static final Map<String, ItemDisplayContext> contexts = new LinkedHashMap<>();
    static {
        contexts.put("none", ItemDisplayContext.NONE);
        contexts.put("thirdperson_lefthand", ItemDisplayContext.THIRD_PERSON_LEFT_HAND);
        contexts.put("thirdperson_righthand", ItemDisplayContext.THIRD_PERSON_RIGHT_HAND);
        contexts.put("firstperson_lefthand", ItemDisplayContext.FIRST_PERSON_LEFT_HAND);
        contexts.put("firstperson_righthand", ItemDisplayContext.FIRST_PERSON_RIGHT_HAND);
        contexts.put("head", ItemDisplayContext.HEAD);
        contexts.put("gui", ItemDisplayContext.GUI);
        contexts.put("ground", ItemDisplayContext.GROUND);
        contexts.put("fixed", ItemDisplayContext.FIXED);
    }

    // Parse a custom item model
    public static AvatarMaterials.CustomItemModel parseCustomItemModel(String fileName, String prefix, JsonObject figmodel, ArrayList<AvatarMaterials.TextureMaterials> textures) throws AvatarImportingException {
        try {
            // First parse the regular model:
            AvatarMaterials.ModelPartMaterials mats = parseFigModel(fileName, prefix, figmodel, textures);
            // Also parse the enum map
            EnumMap<ItemDisplayContext, AvatarMaterials.ItemPartTransform> transforms = new EnumMap<>(ItemDisplayContext.class);
            if (figmodel.has("item_display_data")) {
                JsonObject data = figmodel.getAsJsonObject("item_display_data");
                for (var contextEntry : contexts.entrySet()) {
                    if (data.has(contextEntry.getKey())) {
                        JsonObject transformJson = data.getAsJsonObject(contextEntry.getKey());
                        AvatarMaterials.ItemPartTransform transform = new AvatarMaterials.ItemPartTransform(
                                JsonUtils.parseVec3f(transformJson.getAsJsonArray("translation")),
                                JsonUtils.parseVec3f(transformJson.getAsJsonArray("rotation")),
                                JsonUtils.parseVec3f(transformJson.getAsJsonArray("scale"))
                        );
                        transforms.put(contextEntry.getValue(), transform);
                    }
                }
            }
            return new AvatarMaterials.CustomItemModel(mats, transforms);
        } catch (AvatarImportingException ex) {
            throw ex;
        } catch (Throwable t) {
            throw new AvatarImportingException("figura.error.importing.invalid_figmodel", prefix + "/" + fileName);
        }
    }

    // Return info about textures specific to this model
    private static ModelLocalTexture[] processTextures(JsonArray modelTextures, String filePath, List<AvatarMaterials.TextureMaterials> allTextures) {
        ModelLocalTexture[] mapping = new ModelLocalTexture[modelTextures.size()];
        for (int i = 0; i < modelTextures.size(); i++) {
            JsonObject texture = modelTextures.get(i).getAsJsonObject();
            // Fetch the UV size.
            Vector2f uvSize = JsonUtils.parseVec2f(texture.getAsJsonArray("uv_size"));

            // First, check if it's a vanilla texture:
            @Nullable String vanillaTextureOverride = JsonUtils.getStringOrDefault(texture, "vanilla_texture_override", null);
            if (vanillaTextureOverride != null && !vanillaTextureOverride.isBlank()) {
                allTextures.add(new AvatarMaterials.TextureMaterials.VanillaTexture(vanillaTextureOverride));
                mapping[i] = new ModelLocalTexture(allTextures.size() - 1, uvSize);
                continue;
            }

            // Second, check if it's a linked texture:
            @Nullable String texPath = JsonUtils.getStringOrDefault(texture, "path", null);
            if (texPath != null && !texPath.isBlank()) {
                Path path = Path.of(texPath);
                // Check if this path is used in any existing texture:
                int idx = ListUtils.findIndex(allTextures, tex -> tex instanceof AvatarMaterials.TextureMaterials.OwnedTexture owned && path.equals(owned.path()));
                // If it is, defer to that texture.
                if (idx != -1) {
                    mapping[i] = new ModelLocalTexture(idx, uvSize);
                    continue;
                }
            }

            // Finally, it was neither, so create a new owned texture.
            String textureName = filePath + IOUtils.stripExtension(texture.get("name").getAsString(), "png");
            boolean noAtlas = false;
            if (textureName.endsWith(".noatlas")) {
                textureName = textureName.substring(0, textureName.length() - ".noatlas".length());
                noAtlas = true;
            }
            byte[] data = Base64.getDecoder().decode(texture.get("png_bytes_base64").getAsString());
            AvatarMaterials.TextureMaterials newTexture = new AvatarMaterials.TextureMaterials.OwnedTexture(textureName, null, data, noAtlas);

            // Add to list and update mapping.
            allTextures.add(newTexture);
            mapping[i] = new ModelLocalTexture(allTextures.size() - 1, uvSize);
        }
        return mapping;
    }

    private static AvatarMaterials.ModelPartMaterials processGroup(JsonObject group, ModelLocalTexture[] textureMapping, @Nullable String nameOverride) {
        // Structure
        String name = nameOverride != null ? nameOverride : group.get("name").getAsString();
        Vector3f origin = JsonUtils.parseVec3f(group.getAsJsonArray("origin"));
        Vector3f rotation = JsonUtils.parseVec3f(group.getAsJsonArray("rotation"));
        ArrayList<AvatarMaterials.ModelPartMaterials> children = ListUtils.map(group.getAsJsonArray("children"),
                child -> processGroup(child.getAsJsonObject(), textureMapping, null));

        // Rendering
        int textureIndex = JsonUtils.getIntOrDefault(group, "texture_index", -1);
        int mappedTextureIndex = textureIndex == -1 ? -1 : textureMapping[textureIndex].globalTextureIndex();
        List<AvatarMaterials.CubeData> cubes = ListUtils.map(group.getAsJsonArray("cubes"),
                cube -> processCube(cube.getAsJsonObject(), textureMapping[textureIndex].uvSize()));
        List<AvatarMaterials.MeshData> meshes = ListUtils.map(group.getAsJsonArray("meshes"),
                mesh -> processMesh(mesh.getAsJsonObject(), textureMapping[textureIndex].uvSize()));

        // Return
        return new AvatarMaterials.ModelPartMaterials(name, origin, rotation, children, mappedTextureIndex, cubes, meshes, group);
    }

    private static AvatarMaterials.CubeData processCube(JsonObject cube, Vector2f uvSize) {
        Vector3f origin = JsonUtils.parseVec3f(cube.getAsJsonArray("origin"));
        Vector3f rotation = JsonUtils.parseVec3f(cube.getAsJsonArray("rotation"));
        Vector3f from = JsonUtils.parseVec3f(cube.getAsJsonArray("from"));
        Vector3f to = JsonUtils.parseVec3f(cube.getAsJsonArray("to"));
        Vector3f inflate = JsonUtils.parseVec3f(cube.getAsJsonArray("inflate"));
        JsonArray facesArray = cube.getAsJsonArray("faces");
        if (facesArray.size() != 6) throw new IllegalArgumentException("Unexpected # of cube faces - expected 6, got " + facesArray.size());
        @Nullable AvatarMaterials.CubeFace[] faces = facesArray.asList().stream().map(faceElem -> {
            if (faceElem.isJsonNull()) return null;
            JsonObject face = faceElem.getAsJsonObject();
            Vector2f uv_min = JsonUtils.parseVec2f(face.getAsJsonArray("uv_min")).div(uvSize);
            Vector2f uv_max = JsonUtils.parseVec2f(face.getAsJsonArray("uv_max")).div(uvSize);
            int face_rotation = JsonUtils.getIntOrDefault(face, "rotation", 0);
            if (face_rotation % 90 != 0 || face_rotation < 0 || face_rotation > 270)
                throw new IllegalArgumentException("Unexpected face rotation - expected 0, 90, 180, or 270, got " + face_rotation);
            return new AvatarMaterials.CubeFace(new Vector4f(uv_min.x, uv_min.y, uv_max.x, uv_max.y), face_rotation / 90);
        }).toArray(AvatarMaterials.CubeFace[]::new);
        return new AvatarMaterials.CubeData(origin, rotation, from, to, inflate, faces);
    }

    private static AvatarMaterials.MeshData processMesh(JsonObject mesh, Vector2f uvSize) {
        Vector3f origin = JsonUtils.parseVec3f(mesh.getAsJsonArray("origin"));
        Vector3f rotation = JsonUtils.parseVec3f(mesh.getAsJsonArray("rotation"));
        JsonArray verticesArray = mesh.getAsJsonArray("vertices");
        List<AvatarMaterials.VertexData> vertices = ListUtils.map(verticesArray.asList(), v -> {
            JsonObject vertex = v.getAsJsonObject();
            Vector3f pos = JsonUtils.parseVec3f(vertex.getAsJsonArray("pos"));
            // TODO re-add skinning data
            return new AvatarMaterials.VertexData(pos, null);
        });
        // Get face data
        JsonArray facesArray = mesh.getAsJsonArray("faces");
        List<Vector2f> uvs = new ArrayList<>();
        List<Vector4i> indices = new ArrayList<>();
        facesArray.forEach(faceElem -> {
            JsonObject face = faceElem.getAsJsonObject();
            JsonArray faceVertices = face.getAsJsonArray("vertices");
            if (faceVertices.size() < 3 || faceVertices.size() > 4)
                throw new IllegalArgumentException("Unexpected # of mesh face vertices - expected 3 or 4, got " + faceVertices.size());
            Vector4i indicesVec = new Vector4i(-1);
            for (int i = 0; i < faceVertices.size(); i++) {
                JsonObject faceVertex = faceVertices.get(i).getAsJsonObject();
                indicesVec.setComponent(i, faceVertex.get("index").getAsInt());
                uvs.add(JsonUtils.parseVec2f(faceVertex.getAsJsonArray("uv")).div(uvSize));
            }
            indices.add(indicesVec);
        });
        // Return
        return new AvatarMaterials.MeshData(origin, rotation, vertices, uvs, indices);
    }

    // A model-local texture is mapped to a global texture, and also has a UV size to modify UV values by.
    // UV size is per-model; the same texture may be included in multiple models, with multiple different UV sizes.
    private record ModelLocalTexture(int globalTextureIndex, Vector2f uvSize) {}

}
