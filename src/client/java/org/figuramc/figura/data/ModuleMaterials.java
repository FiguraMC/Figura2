package org.figuramc.figura.data;

import net.minecraft.world.item.ItemDisplayContext;
import org.figuramc.figura.script_hooks.callback.CallbackType;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.joml.Vector4i;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.nio.file.Path;
import java.util.*;

/**
 * Contains various types which are used for constructing modules, but can also be serialized.
 * ModuleMaterials acts as a central point for conversions and loading.
 *   --------------------------
 *   |     Module folders     |
 *   --------------------------
 *                |
 *                |
 *                v
 *   --------------------------          -----------------------             -----------------------
 *   |    Module Materials    |  ----->  |   Module Instance   | ---Many---> |   Avatar Instance   |
 *   --------------------------          -----------------------             -----------------------
 *           |         ^
 *           |         |
 *           v         |
 *   --------------------------
 *   |        Raw Bytes       |
 *   --------------------------
 */
public record ModuleMaterials(
        MetadataMaterials metadata,
        List<ScriptMaterials> scripts,
        List<TextureMaterials> textures,
        List<ModelPartMaterials> worldRoots,
        @Nullable ModelPartMaterials entityRoot,
        @Nullable ModelPartMaterials hudRoot,
        TreeMap<String, CustomItem> customItemRoots // Tree map for sorted order
) {

    // METADATA
    public record MetadataMaterials(
            // When using scripts, this must be specified.
            // If there are no scripts at all, this will be null.
            @Nullable String language, // "lua" is currently the only valid option
            // For now, dependencies are just strings. TODO improve/make more unique for backend package manager stuff
            // We must maintain the ordering given in the json.
            LinkedHashMap<String, String> dependencies,
            // Exposed API elements:
            // We simply maintain the ordering given in the json.
            LinkedHashMap<String, CallbackType.Func> api
    ) {}

    // SCRIPTS
    public record ScriptMaterials(String name, byte[] data) {}

    // TEXTURES
    public sealed interface TextureMaterials {
        @Nullable String name();
        record OwnedTexture(String name, @Nullable @NoSerialize Path path, byte[] data, boolean noAtlas) implements TextureMaterials {}
        record VanillaTexture(String resourceLocation) implements TextureMaterials { @Override public String name() { return null; }}
    }

    // MODEL PARTS
    public record ModelPartMaterials(
            // Structuring
            String name, Vector3f origin, Vector3f rotation, ArrayList<ModelPartMaterials> children,
            // Vanilla part to mimic if any, in the form "ModelName/PartName" ("ENTITY/head")
            @Nullable String mimic,
            // Rendering data
            int textureIndex, List<CubeData> cubes, List<MeshData> meshes
    ) {
        // Shorthand for creating a wrapper around some children with a name
        public static ModelPartMaterials wrapper(String name, ArrayList<ModelPartMaterials> children) {
            return new ModelPartMaterials(name, new Vector3f(), new Vector3f(), children, null, -1, List.of(), List.of());
        }
    }
    public record CubeData(Vector3f origin, Vector3f rotation, Vector3f from, Vector3f to, Vector3f inflate, @Nullable CubeFace[] faces) {}
    // Vector stores (uv_min.x, uv_min.y, uv_max.x, uv_max.y). UV values are 0-1 (generally speaking; uv values may technically leave the texture). Rot is 0-3.
    public record CubeFace(Vector4f uv, int rot) {}
    public record MeshData(Vector3f origin, Vector3f rotation, List<VertexData> vertices, List<Vector2f> uvs, List<Vector4i> indices) {}
    public record VertexData(Vector3f pos, @Nullable SkinningData skinningData) {}
    public record SkinningData(Vector4i offsets, Vector4f weights) {}

    // CUSTOM ITEMS
    public record CustomItem(@Nullable CustomItemModel model, int textureIndex) {}
    public record CustomItemModel(ModelPartMaterials model, EnumMap<ItemDisplayContext, ItemPartTransform> transforms) {}
    public record ItemPartTransform(Vector3f translation, Vector3f rotation, Vector3f scale) {}


    // Doesn't do anything, except work as documentation that a certain field should not be serialized.
    // Essentially, it means that this field only exists for convenience during the importing process
    // to make the code cleaner and simpler!
    @Target(ElementType.FIELD)
    private @interface NoSerialize {}

}
