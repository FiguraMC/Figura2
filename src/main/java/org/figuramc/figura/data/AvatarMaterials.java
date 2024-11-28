package org.figuramc.figura.data;

import net.minecraft.world.item.ItemDisplayContext;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.joml.Vector4i;

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
        Map<String, CustomItemPartMaterials> customItemRoots
) {

    // METADATA
    public record MetadataMaterials(List<String> sharedScripts) {}

    // SCRIPTS
    public record ScriptMaterials(String name, byte[] data) {}

    // TEXTURES
    public interface TextureMaterials {
        @Nullable String name();
        record OwnedTexture(String name, byte[] data, boolean noAtlas) implements TextureMaterials {}
        record VanillaTexture(String resourceLocation) implements TextureMaterials { @Override public String name() { return null; }}
    }

    // MODEL PARTS
    public record ModelPartMaterials(String name, Vector3f origin, Vector3f rotation, List<ModelPartMaterials> children, int textureIndex, List<CubeData> cubes, List<MeshData> meshes) {}
    public record CubeData(Vector3f from, Vector3f to, Vector3f origin, Vector3f rotation, @Nullable CubeFace[] faces) {}
    public record CubeFace(Vector4f uv, int rot) {}
    public record MeshData(Vector3f origin, Vector3f rotation, List<VertexData> vertices, List<Vector2f> uvs, List<Vector4i> faces) {}
    public record VertexData(Vector3f pos, @Nullable Vector4i skinningOffsets, @Nullable Vector4f skinningWeights) {}

    // VANILLA ROOTS
    public record VanillaRootPartMaterials(ModelPartMaterials modelPartMaterials, MutableBoolean replaceRoot) {} // Mutable to make the importing process easier

    // CUSTOM ITEMS
    public record CustomItemPartMaterials(@Nullable ModelPartMaterials modelPartMaterials, @Nullable EnumMap<ItemDisplayContext, ItemPartTransform> transforms, int textureIndex) {} // Either both null, or neither null
    public record ItemPartTransform(Vector3f translation, Vector3f rotation, Vector3f scale) {}

}
