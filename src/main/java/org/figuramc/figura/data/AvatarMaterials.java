package org.figuramc.figura.data;

import com.google.gson.JsonObject;
import net.minecraft.world.item.ItemDisplayContext;
import org.figuramc.figura.util.ListUtils;
import org.jetbrains.annotations.Nullable;
import org.joml.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Contains various types which are used for constructing Avatar instances, but can also be serialized.
 * AvatarMaterials acts as a central point for conversions and loading.
 *   --------------------------
 *   |     Avatar folders     |
 *   --------------------------
 *                |
 *                |
 *                v
 *   --------------------------          ------------------------
 *   |    Avatar Materials    |  ----->  |   Avatar Instances   |
 *   --------------------------          ------------------------
 *           |         ^
 *           |         |
 *           v         |
 *   --------------------------
 *   |        Raw Bytes       |
 *   --------------------------
 *
 */
public record AvatarMaterials(
        MetadataMaterials metadata,
        List<ScriptMaterials> scripts,
        List<TextureMaterials> textures,
        List<ModelPartMaterials> worldRoots,
        ModelPartMaterials entityRoot,
        ModelPartMaterials hudRoot,
        Map<String, VanillaRootPartMaterials> vanillaPartRoots,
        Map<String, CustomItem> customItemRoots
) {

    // METADATA
    public record MetadataMaterials() {}

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
            // Rendering data
            int textureIndex, List<CubeData> cubes, List<MeshData> meshes,
            // Json data. Not serialized, used only during import to make things simpler
            @Nullable @NoSerialize JsonObject groupJson
    ) {
        // Shorthand for creating a wrapper around some children with a name
        public static ModelPartMaterials wrapper(String name, ArrayList<ModelPartMaterials> children) {
            return new ModelPartMaterials(name, new Vector3f(), new Vector3f(), children, -1, List.of(), List.of(), null);
        }
    }
    public record CubeData(Vector3f origin, Vector3f rotation, Vector3f from, Vector3f to, Vector3f inflate, @Nullable CubeFace[] faces) {}
    // Vector stores (uv_min.x, uv_min.y, uv_max.x, uv_max.y). UV values are 0-1 (generally speaking; uv values may technically leave the texture). Rot is 0-3.
    public record CubeFace(Vector4f uv, int rot) {}
    public record MeshData(Vector3f origin, Vector3f rotation, List<VertexData> vertices, List<Vector2f> uvs, List<Vector4i> indices) {}
    public record VertexData(Vector3f pos, @Nullable SkinningData skinningData) {}
    public record SkinningData(Vector4i offsets, Vector4f weights) {}

    // VANILLA PARTS
    public record VanillaRootPartMaterials(ModelPartMaterials partData, boolean replaceVanillaRoot) {
        // Shorthand to wrap parts together
        public static VanillaRootPartMaterials wrapper(String name, List<VanillaRootPartMaterials> children) {
            boolean anyReplace = ListUtils.any(children, VanillaRootPartMaterials::replaceVanillaRoot);
            return new VanillaRootPartMaterials(ModelPartMaterials.wrapper(name, ListUtils.map(children, VanillaRootPartMaterials::partData)), anyReplace);
        }
    }

    // CUSTOM ITEMS
    // Either both null, or neither null
    public record CustomItem(@Nullable CustomItemModel model, int textureIndex) {}
    public record CustomItemModel(ModelPartMaterials model, EnumMap<ItemDisplayContext, ItemPartTransform> transforms) {}
    public record ItemPartTransform(Vector3f translation, Vector3f rotation, Vector3f scale) {}


    // Doesn't do anything, except work as documentation that a certain field should not be serialized.
    // Essentially, it means that this field only exists for convenience during the importing process
    // to make the code cleaner and simpler!
    @Target(ElementType.FIELD)
    private @interface NoSerialize {}

}
