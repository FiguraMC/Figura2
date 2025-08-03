package org.figuramc.figura.vanillamodel;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import org.apache.commons.lang3.mutable.MutableInt;
import org.figuramc.figura.ducks.client.CubeTrackingAccess;
import org.figuramc.figura.util.JsonUtils;
import org.figuramc.figura.util.ListUtils;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;
import org.joml.Vector3f;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This class is responsible for exporting a given entity type
 * as a figmodel. Only model part data can be exported, not texture
 * data, because textures are dependent on an actual Entity instance,
 * which might not always be present.
 * <p>
 * For example, the textures of horse markings depend on the particular
 * horse. So it's impossible to just "export minecraft:horse" and get
 * a texture.
 * <p>
 * Potentially, at some point in the future, a function could be
 * added to export a specific entity in the world, as it appears on a
 * particular frame, including the textures; this would be pretty difficult though.
 * <p>
 * TODO do something about model part scale. We can't export it right now.
 */
public class EntityExporter {

    public static JsonObject exportEntity(EntityType<?> entityType) {
        EntityRenderer<?, ?> renderer = Minecraft.getInstance().getEntityRenderDispatcher().renderers.get(entityType);
        return fromEntityRenderer(renderer);
    }

    public static JsonObject exportPlayer(PlayerSkin.Model type) {
        EntityRenderer<?, ?> renderer = Minecraft.getInstance().getEntityRenderDispatcher().playerRenderers.get(type);
        return fromEntityRenderer(renderer);
    }

    private static JsonObject fromEntityRenderer(EntityRenderer<?, ?> renderer) {
        boolean isLivingEntity = renderer instanceof LivingEntityRenderer;
        LinkedHashMap<Vector2f, JsonObject> generatedTextures = new LinkedHashMap<>(); // Order matters

        // Process model part roots
        JsonObject roots = new JsonObject();
        Map<String, MutableInt> dedupNameMap = new HashMap<>();
        ModelNames.getModelsByName(renderer).forEach((name, model) -> {
            roots.add(name, processModelPart(name, null, model.root(), true, isLivingEntity, generatedTextures, dedupNameMap));
        });

        // Place textures in json
        JsonObject textures = new JsonObject();
        generatedTextures.forEach((k, v) -> textures.add((int) k.x + "x" + (int) k.y, v));

        // Create the final json object
        JsonObject figmodel = new JsonObject();
        figmodel.add("roots", roots);
        figmodel.add("textures", textures);
        return figmodel;
    }

    // Recursively convert the model part to json group format
    private static JsonObject processModelPart(String modelName, @Nullable String partName, ModelPart part, boolean isRoot, boolean isLivingEntity, LinkedHashMap<Vector2f, JsonObject> generatedTextures, Map<String, MutableInt> nameDeduplicator) {

        // Fetch transforms
        PartPose initialPose = part.getInitialPose();
        Vector3f pos = new Vector3f(initialPose.x(), initialPose.y(), initialPose.z());
        Vector3f rot = new Vector3f(initialPose.xRot(), initialPose.yRot(), initialPose.zRot()).mul(Mth.RAD_TO_DEG);
        // Flip and translate stuff if it's a living entity
        if (isLivingEntity) {
            pos.mul(-1, -1, 1);
            if (isRoot) pos.add(0, 24, 0);
            rot.mul(-1, -1, 1);
        }

        // Process cubes and get the texture index
        JsonArray cubes = new JsonArray();
        int texIndex = -1;
        for (ModelPart.Cube cube : part.cubes) {
            cubes.add(processCube(cube, isLivingEntity));
            if (texIndex == -1) {
                Vector2f texSize = ((CubeTrackingAccess) cube).figura$getTextureSize();
                texIndex = ListUtils.indexOf(generatedTextures.keySet(), texSize);
                if (texIndex == -1) {
                    JsonObject generatedTexture = genBlankTexture(texSize);
                    generatedTextures.put(texSize, generatedTexture);
                    texIndex = generatedTextures.size() - 1;
                }
            }
        }

        // Process children
        JsonObject children = new JsonObject();
        for (var child : part.children.entrySet()) {
            String childName = child.getKey();
            ModelPart childPart = child.getValue();
            if (nameDeduplicator.containsKey(childName)) {
                childName = childName + nameDeduplicator.get(childName).getAndIncrement();
            } else {
                nameDeduplicator.put(partName, new MutableInt(2));
            }
            JsonObject jsonChild = processModelPart(modelName, childName, childPart, false, isLivingEntity, generatedTextures, nameDeduplicator);
            children.add(childName, jsonChild);
        }

        // Create and return the final group
        JsonObject group = new JsonObject();
        group.add("origin", JsonUtils.toJson(pos));
        group.add("rotation", JsonUtils.toJson(rot));
        group.add("children", children);
        if (texIndex != -1) group.addProperty("texture_index", texIndex);
        group.add("cubes", cubes);
        group.add("meshes", new JsonArray());
        String mimicPart = modelName + "/" + (partName != null ? partName : "root");
        group.addProperty("mimic_part", mimicPart);
        return group;
    }

    private static JsonObject processCube(ModelPart.Cube cube, boolean isLivingEntity) {
        // Fetch values
        Vector2f texSize = ((CubeTrackingAccess) cube).figura$getTextureSize();
        boolean mirrored = ((CubeTrackingAccess) cube).figura$getMirrored();
        // Process cube faces
        JsonArray facesArray = new JsonArray(6);
        for (int i = 0; i < 6; i++) facesArray.add(JsonNull.INSTANCE);
        for (ModelPart.Polygon polygon : cube.polygons) {
            Direction faceDir = Direction.getApproximateNearest(polygon.normal().x, polygon.normal().y, polygon.normal().z);
            int index = switch (faceDir) { case WEST -> 0; case EAST -> 1; case DOWN -> 2; case UP -> 3; case NORTH -> 4; case SOUTH -> 5; };

            float u1 = polygon.vertices()[0].u() * texSize.x;
            float v1 = polygon.vertices()[0].v() * texSize.y;
            float u2 = polygon.vertices()[2].u() * texSize.x;
            float v2 = polygon.vertices()[2].v() * texSize.y;

            JsonObject jsonFace = new JsonObject();
            jsonFace.add("uv_min", JsonUtils.toJson(u1, v1));
            jsonFace.add("uv_max", JsonUtils.toJson(u2, v2));
            jsonFace.addProperty("rotation", 0);
            facesArray.set(index, jsonFace);

            // These conditions for when to swap U and V were found through tons of trial and error.
            if (faceDir.getAxis().isVertical() != mirrored) swapU(jsonFace);
            if (faceDir.getAxis().isVertical() == mirrored) swapV(jsonFace);
        }
        // Fetch x/y/z values
        Vector3f inflate = ((CubeTrackingAccess) cube).figura$getInflate();
        Vector3f min = new Vector3f(cube.minX, cube.minY, cube.minZ);
        Vector3f max = new Vector3f(cube.maxX, cube.maxY, cube.maxZ);

        // If living entity, flip on X and Y axes
        if (isLivingEntity) {
            // Swap and negate X and Y min and max
            Vector3f min2 = new Vector3f(min);
            min.set(-max.x, -max.y, min.z);
            max.set(-min2.x, -min2.y, max.z);
            // Swap west/east faces and up/down faces
            ListUtils.swap(facesArray.asList(), 0, 1);
            ListUtils.swap(facesArray.asList(), 2, 3);
            // Modify UVs of each face as well
            for (JsonElement e : facesArray) {
                swapU(e.getAsJsonObject());
                swapV(e.getAsJsonObject());
            }
        }

        // Create cube itself and return
        JsonObject jsonCube = new JsonObject();
        jsonCube.add("origin", JsonUtils.ZERO_VEC_3);
        jsonCube.add("rotation", JsonUtils.ZERO_VEC_3);
        jsonCube.add("from", JsonUtils.toJson(min));
        jsonCube.add("to", JsonUtils.toJson(max));
        jsonCube.add("inflate", JsonUtils.toJson(inflate));
        jsonCube.add("faces", facesArray);
        return jsonCube;
    }

    private static void swapU(@Nullable JsonObject cubeFace) { swapUV(cubeFace, 0); }
    private static void swapV(@Nullable JsonObject cubeFace) { swapUV(cubeFace, 1); }
    private static void swapUV(@Nullable JsonObject cubeFace, int i) {
        if (cubeFace == null) return;
        JsonArray uvMin = cubeFace.getAsJsonArray("uv_min");
        JsonArray uvMax = cubeFace.getAsJsonArray("uv_max");
        JsonElement temp = uvMin.get(i);
        uvMin.set(i, uvMax.get(i));
        uvMax.set(i, temp);
    }

    private static JsonObject genBlankTexture(Vector2f size) {
        // Fetch png base 64...
        int w = (int) size.x;
        int h = (int) size.y;
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", baos);
        } catch (IOException shouldNotOccur) {
            throw new IllegalStateException("Failed to convert blank texture? Internal error in Figura, please report!");
        }
        String pngBase64 = Base64.getEncoder().encodeToString(baos.toByteArray());
        // Create json texture.
        JsonObject texture = new JsonObject();
        texture.add("uv_size", JsonUtils.toJson(size));
        texture.addProperty("png_bytes_base64", pngBase64);
        return texture;
    }

}
