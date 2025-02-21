package org.figuramc.figura.vanillamodel;

import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.entity.EnderDragonRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EvokerFangsRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import org.figuramc.figura.ducks.client.CubeTrackingAccess;
import org.figuramc.figura.util.ErrorReporting;
import org.figuramc.figura.util.JsonUtils;
import org.figuramc.figura.util.ListUtils;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import oshi.util.tuples.Pair;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;

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
 * particular frame, including the textures.
 * <p>
 * TODO do something about model part scale. We can't export it right now.
 */
public class EntityExporter {


    public static final Predicate<ModelPart> LITERALLY_ALL_PARTS = part -> true;

    public static JsonObject exportEntity(EntityType<?> entityType, Predicate<ModelPart> predicate) {
        EntityRenderer<?, ?> renderer = Minecraft.getInstance().getEntityRenderDispatcher().renderers.get(entityType);
        return fromEntityRenderer(renderer, predicate);
    }

    public static JsonObject exportPlayer(PlayerSkin.Model type, Predicate<ModelPart> predicate) {
        EntityRenderer<?, ?> renderer = Minecraft.getInstance().getEntityRenderDispatcher().playerRenderers.get(type);
        return fromEntityRenderer(renderer, predicate);
    }

    private static JsonObject fromEntityRenderer(EntityRenderer<?, ?> renderer, Predicate<ModelPart> predicate) {
        boolean isLivingEntity = renderer instanceof LivingEntityRenderer;

        List<Pair<Vector2f, JsonObject>> generatedTextures = new ArrayList<>();
        int[] nextUniqueGroupId = new int[1];

        JsonArray roots = new JsonArray();
        Vector3f startOrigin = isLivingEntity ? new Vector3f(0, 24, 0) : new Vector3f();
        for (ModelPart part : ModelPartTracker.traceEntityRenderer(renderer))
            processModelPart(part, roots, predicate, startOrigin, new Quaternionf(), isLivingEntity, generatedTextures, nextUniqueGroupId);

        JsonArray textures = new JsonArray();
        for (var pair : generatedTextures) textures.add(pair.getB());

        JsonObject rootPart = new JsonObject();
        rootPart.addProperty("name", "");
        rootPart.add("origin", JsonUtils.ZERO_VEC_3);
        rootPart.add("rotation", JsonUtils.ZERO_VEC_3);
        rootPart.add("children", roots);
        rootPart.add("cubes", new JsonArray());
        rootPart.add("meshes", new JsonArray());

        JsonObject figmodel = new JsonObject();
        figmodel.add("part_data", rootPart);
        figmodel.add("textures", textures);
        return figmodel;
    }

    // Recursively traverse, and add parts to the array.
    // Keep all the generated textures stored according to UV size.
    private static void processModelPart(ModelPart part, JsonArray roots, Predicate<ModelPart> predicate, Vector3f parentOrigin, Quaternionf parentQuat, boolean isLivingEntity, List<Pair<Vector2f, JsonObject>> generatedTextures, int[] nextUniqueGroupId) {

        // Structure
        String name = ModelPartTracker.getAliasOrName(part);
        String vanillaRoot = ModelPartTracker.getAliasOrFullName(part);
        boolean replaceVanillaRoot = true; // On by default, more popular

        // Transform
        PartPose initialPose = part.getInitialPose();
        Vector3f pos = new Vector3f(initialPose.x(), initialPose.y(), initialPose.z());
        Vector3f rotRadians = new Vector3f(initialPose.xRot(), initialPose.yRot(), initialPose.zRot());
        if (isLivingEntity) {
            pos.mul(-1, -1, 1);
            rotRadians.mul(-1, 1, 1);
        }
        // Modify pos according to the parent's pos/rot:
        pos.rotate(parentQuat).add(parentOrigin);
        // Modify our rot accordingly too:
        Quaternionf ourQuat = parentQuat.rotateZYX(rotRadians.z, rotRadians.y, rotRadians.x, new Quaternionf());
        ourQuat.getEulerAnglesZYX(rotRadians);
        Vector3f rot = rotRadians.mul(Mth.RAD_TO_DEG, new Vector3f()); // Get degrees

        // Process children recursively
        for (ModelPart child : part.children.values())
            processModelPart(child, roots, predicate, pos, ourQuat, isLivingEntity, generatedTextures, nextUniqueGroupId);

        // If predicate fails, quit out.
        if (!predicate.test(part)) return;

        // Process cubes, collecting in a map
        Map<Vector2f, JsonArray> cubesByTexSize = new LinkedHashMap<>();
        for (ModelPart.Cube cube : part.cubes) {
            JsonObject cubeJson = processCube2(cube, isLivingEntity);
            Vector2f texSize = ((CubeTrackingAccess) cube).figura$getTextureSize();
            cubesByTexSize.computeIfAbsent(texSize, x -> new JsonArray()).add(cubeJson);
        }

        // Process the map
        JsonArray children = new JsonArray();
        JsonArray cubes;
        int texIndex;

        // Based on the number of cube groups, do things.
        switch (cubesByTexSize.size()) {
            // No groups, no cubes, no texture.
            case 0 -> {
                cubes = new JsonArray();
                texIndex = -1;
            }
            // 1 group only, use its values directly.
            case 1 -> {
                var entry = cubesByTexSize.entrySet().iterator().next();
                texIndex = ListUtils.findIndex(generatedTextures, pair -> pair.getA().equals(entry.getKey()));
                if (texIndex == -1) {
                    generatedTextures.add(new Pair<>(entry.getKey(), genBlankTexture(entry.getKey())));
                    texIndex = generatedTextures.size() - 1;
                }
                cubes = entry.getValue();
            }
            default -> {
                // Multiple groups, so split. (Should be rare...)
                for (var entry : cubesByTexSize.entrySet()) {
                    int subTexIndex = ListUtils.findIndex(generatedTextures, pair -> pair.getA().equals(entry.getKey()));
                    if (subTexIndex == -1) {
                        generatedTextures.add(new Pair<>(entry.getKey(), genBlankTexture(entry.getKey())));
                        subTexIndex = generatedTextures.size() - 1;
                    }

                    JsonObject newGroup = new JsonObject();
                    newGroup.addProperty("name", "tex_split_" + ++nextUniqueGroupId[0]);
                    newGroup.add("origin", JsonUtils.ZERO_VEC_3);
                    newGroup.add("rotation", JsonUtils.ZERO_VEC_3);
                    newGroup.add("children", new JsonArray());
                    newGroup.addProperty("texture_index", subTexIndex);
                    newGroup.add("cubes", entry.getValue());
                    newGroup.add("meshes", new JsonArray());
                    children.add(newGroup);
                }
                cubes = new JsonArray();
                texIndex = -1;
            }
        }

        // Create group and add to list
        JsonObject group = new JsonObject();
        group.addProperty("name", name);
        group.add("origin", JsonUtils.toJson(pos));
        group.add("rotation", JsonUtils.toJson(rot));
        group.add("children", children); // No children in vanilla models, parts are flattened
        if (texIndex != -1) group.addProperty("texture_index", texIndex);
        group.add("cubes", cubes);
        group.add("meshes", new JsonArray());
        group.addProperty("vanilla_root", vanillaRoot);
        group.addProperty("replace_vanilla_root", replaceVanillaRoot);
        roots.add(group);
    }

    private static JsonObject processCube2(ModelPart.Cube cube, boolean isLivingEntity) {
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


    // Process the cube and return it.
    private static final int[] cubeFaceIndexMapping = new int[] { 2, 4, 0, 1, 3, 5 }; // Based on ordering of cube faces in ModelPart.Cube class
    private static final int[] livingEntityFaceIndexMapping = new int[] { 4, 2, 1, 0, 3, 5 }; // Swap x and y ordering...
//    private static final boolean swapX
    private static JsonObject processCube(ModelPart.Cube cube, boolean isLivingEntity) {
        // Fetch or create texture
        Vector2f texSize = ((CubeTrackingAccess) cube).figura$getTextureSize();
        // Process cube faces
        @Nullable ModelPart.Polygon[] faces = cube.polygons;
        JsonArray facesArray = new JsonArray();
        for (int i = 0; i < 6; i++) {

            @Nullable ModelPart.Polygon face = faces[cubeFaceIndexMapping[i]];
            if (face == null) {
                facesArray.add(JsonNull.INSTANCE);
            } else {

                float u2 = face.vertices()[0].u() * texSize.x();
                float v2 = face.vertices()[0].v() * texSize.y();
                float u1 = face.vertices()[2].u() * texSize.x();
                float v1 = face.vertices()[2].v() * texSize.y();




//                Vector2f uvMin = new Vector2f(Float.POSITIVE_INFINITY);
//                Vector2f uvMax = new Vector2f(Float.NEGATIVE_INFINITY);
//                for (var vert : face.vertices()) {
//                    Vector2f uv = new Vector2f(vert.u(), vert.v());
//                    uvMin.min(uv);
//                    uvMax.max(uv);
//                }
//                uvMin.mul(texSize);
//                uvMax.mul(texSize);
//
                JsonObject jsonFace = new JsonObject();
//                jsonFace.add("uv_min", JsonUtils.toJson(uvMin));
//                jsonFace.add("uv_max", JsonUtils.toJson(uvMax));
                jsonFace.add("uv_min", JsonUtils.toJson(u1, v1));
                jsonFace.add("uv_max", JsonUtils.toJson(u2, v2));
                jsonFace.addProperty("rotation", 0);
                facesArray.add(jsonFace);
            }
        }
        // Fetch x/y/z values
        Vector3f inflate = ((CubeTrackingAccess) cube).figura$getInflate();
        Vector3f min = new Vector3f(cube.minX, cube.minY, cube.minZ);
        Vector3f max = new Vector3f(cube.maxX, cube.maxY, cube.maxZ);
        // Modify according to isLivingEntity
        if (isLivingEntity) {
            // Flip on x and y axes
            min.mul(-1, -1, 1);
            max.mul(-1, -1, 1);
            // Swap min and max in XY coordinates
            Vector3f temp = new Vector3f(min);
            min.set(max.x, max.y, min.z);
            max.set(temp.x, temp.y, max.z);
            // Swap UV faces in XY axes
            JsonElement temp2 = facesArray.get(0);
            facesArray.set(0, facesArray.get(1));
            facesArray.set(1, temp2);
            temp2 = facesArray.get(2);
            facesArray.set(2, facesArray.get(3));
            facesArray.set(3, temp2);
        }
        // Create cube itself and add
        JsonObject jsonCube = new JsonObject();
        jsonCube.add("origin", JsonUtils.ZERO_VEC_3);
        jsonCube.add("rotation", JsonUtils.ZERO_VEC_3);
        jsonCube.add("from", JsonUtils.toJson(min));
        jsonCube.add("to", JsonUtils.toJson(max));
        jsonCube.add("inflate", JsonUtils.toJson(inflate));
        jsonCube.add("faces", facesArray);
        return jsonCube;
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
            ErrorReporting.unexpectedError(shouldNotOccur);
        }
        String pngBase64 = Base64.getEncoder().encodeToString(baos.toByteArray());
        // Create json texture.
        JsonObject texture = new JsonObject();
        texture.addProperty("name", size.x + " x " + size.y);
        texture.add("uv_size", JsonUtils.toJson(size));
        texture.addProperty("png_bytes_base64", pngBase64);
        return texture;
    }

    /**
     * Configurable options for the exporter, can be used as a predicate.
     * It will only accept certain specific
     */
    public record ExporterOptions(EnumSet<ModelPartAlias.Group> groups) implements Predicate<ModelPart> {

        public ExporterOptions(ModelPartAlias.Group... groups) {
            this(groups.length == 0 ? EnumSet.noneOf(ModelPartAlias.Group.class) : EnumSet.copyOf(Arrays.asList(groups)));
        }

        @Override
        public boolean test(ModelPart part) {
            if (!ModelPartTracker.hasAlias(part)) return false; // Only accept aliased parts
            return groups.containsAll(ModelPartTracker.getAlias(part).groups()); // Only accept parts where we have all of their groups enabled
        }
    }

}
