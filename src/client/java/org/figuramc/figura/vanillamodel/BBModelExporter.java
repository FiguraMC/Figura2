package org.figuramc.figura.vanillamodel;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.figuramc.figura.data.AvatarImporter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;
import java.util.UUID;

/**
 * This class is responsible for exporting a given entity type
 * as a BBModel. Only model part data can be exported, not texture
 * data, because textures are dependent on an actual Entity instance,
 * which might not always be present.
 *
 * For example, the textures of horse markings depend on the particular
 * horse. So it's impossible to just "export minecraft:horse" and get
 * a texture.
 *
 * Potentially, at some point in the future, a function could be
 * added to export a specific entity in the world, as it appears on a
 * particular frame, including the textures.
 */
public class BBModelExporter {

    public static JsonObject exportEntity(String modelName, EntityType<?> entityType) {
        EntityRenderer<?> renderer = Minecraft.getInstance().getEntityRenderDispatcher().renderers.get(entityType);
        return fromEntityRenderer(modelName, renderer);
    }

    public static JsonObject exportPlayer(String modelName, boolean slim) {
        PlayerSkin.Model model = slim ? PlayerSkin.Model.SLIM : PlayerSkin.Model.WIDE;
        EntityRenderer<?> renderer = Minecraft.getInstance().getEntityRenderDispatcher().playerRenderers.get(model);
        return fromEntityRenderer(modelName, renderer);
    }

    private static JsonObject fromEntityRenderer(String modelName, EntityRenderer<?> renderer) {
        boolean isLivingEntity = renderer instanceof LivingEntityRenderer<?, ?>;
        // Create the json object
        JsonObject bbmodel = new JsonObject();
        // Metadata
        JsonObject meta = new JsonObject();
        meta.addProperty("format_version", "4.9"); // This is the most recent one as of writing
        meta.addProperty("model_format", "free"); // Generic model
        meta.addProperty("box_uv", true); // Vanilla uses box uv
        bbmodel.add("meta", meta);
        // Resolution, set to 16 for now
        JsonObject resolution = new JsonObject();
        resolution.addProperty("width", 16);
        resolution.addProperty("height", 16);
        bbmodel.add("resolution", resolution);
        // Name
        bbmodel.addProperty("name", modelName);
        // Create arrays
        JsonArray elements = new JsonArray();
        JsonArray outliner = new JsonArray();
        // Iterate over model parts
        List<ModelPart> vanillaModelParts = ModelPartTracker.traceEntityRenderer(renderer);
        for (ModelPart part : vanillaModelParts) {
            Vector3f originPos = new Vector3f(0, isLivingEntity ? 24 : 0, 0);
            JsonObject jsonPart = processModelPart(part, elements, outliner, originPos, new Vector3f(), isLivingEntity);
            if (jsonPart != null) outliner.add(jsonPart);
        }
        // Add them to the bbmodel
        bbmodel.add("elements", elements);
        bbmodel.add("outliner", outliner);
        // Return
        return bbmodel;
    }

    // Process a single model part and return a JsonObject which is a bbmodel group.
    // May modify the "elements" and "outliner" arrays in the process.
    // If this part has no cube children, returns null.
    private static @Nullable JsonObject processModelPart(ModelPart vanillaPart, JsonArray elements, JsonArray outliner, Vector3f parentPos, Vector3f parentRot, boolean isLivingEntity) {
        JsonObject partJson = new JsonObject();

        // Get name
        String name = ModelPartTracker.getName(vanillaPart);
        partJson.addProperty("name", name);

        // Get full name and store in the root key
        String fullName = ModelPartTracker.getFullName(vanillaPart, "/");
        partJson.addProperty(AvatarImporter.VANILLA_ROOT_KEY, fullName);
        partJson.addProperty(AvatarImporter.VANILLA_ROOT_REPLACE_KEY, true); // Replacing by default seems best

        // Get origin and rotation
        PartPose initialPose = vanillaPart.getInitialPose();
        Vector3f pos = new Vector3f(initialPose.x, initialPose.y, initialPose.z);
        Vector3f rotRadians = new Vector3f(initialPose.xRot, initialPose.yRot, initialPose.zRot);
        if (isLivingEntity) {
            // Make adjustments for living entities
            pos.mul(-1, -1, 1);
            rotRadians.mul(-1, 1, 1);
        }
        // Modify pos according to the parent's pos/rot:
        Quaternionf parentQuat = new Quaternionf().rotationZYX(parentRot.z, parentRot.y, parentRot.x);
        pos.rotate(parentQuat).add(parentPos);
        // Modify our rot accordingly too:
        Quaternionf ourQuat = parentQuat.rotateZYX(rotRadians.z, rotRadians.y, rotRadians.x);
        ourQuat.getEulerAnglesZYX(rotRadians);
        // Create json.

        partJson.add("origin", jsonArrayOf(pos.x, pos.y, pos.z));
        partJson.add("rotation", jsonArrayOf(rotRadians.x * 180 / Mth.PI, rotRadians.y * 180 / Mth.PI, rotRadians.z * 180 / Mth.PI));

        // Make up a uuid
        partJson.addProperty("uuid", UUID.randomUUID().toString());

        // Process children:
        JsonArray children = new JsonArray();
        // Process cubes
        for (ModelPart.Cube cube : vanillaPart.cubes)
            children.add(processCube(cube, elements, pos, isLivingEntity));
        // Process groups, add them to outliner instead
        for (ModelPart child : vanillaPart.children.values()) {
            JsonObject jsonPart = processModelPart(child, elements, outliner, pos, rotRadians, isLivingEntity);
            if (jsonPart != null) outliner.add(jsonPart);
        }
        // Add children
        partJson.add("children", children);
        // Return
        return vanillaPart.cubes.isEmpty() ? null : partJson;
    }

    // Create a cube object and place it in the elements array.
    // Return the UUID of the newly created cube.
    private static String processCube(ModelPart.Cube cube, JsonArray elements, Vector3f parentPos, boolean isLivingEntity) {
        JsonObject cubeJson = new JsonObject();
        // Make basic fields
        cubeJson.addProperty("name", "cube");
        cubeJson.addProperty("box_uv", true);
        cubeJson.addProperty("type", "cube");
        cubeJson.add("origin", jsonArrayOf(0, 0, 0));

        // Iterate over the faces, to find the from/to, as well as the cube face data
        JsonObject faces = new JsonObject();
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
        // TODO Figure out UVs. We can't get textures but we should at least be able to get UVs.
//        int textureWidth = ((CubeTrackingAccess) cube).figura$getTextureWidth();
//        int textureHeight = ((CubeTrackingAccess) cube).figura$getTextureHeight();
        for (ModelPart.Polygon quad : cube.polygons) {
            // Get UV coords
            float u1 = quad.vertices[1].u;// * textureWidth;
            float u2 = quad.vertices[0].u;// * textureWidth;
            float v1 = quad.vertices[0].v;// * textureHeight;
            float v2 = quad.vertices[2].v;// * textureHeight;
            // Update max and min x, y, z
            for (ModelPart.Vertex vert : quad.vertices) {
                minX = Math.min(minX, vert.pos.x);
                minY = Math.min(minY, vert.pos.y);
                minZ = Math.min(minZ, vert.pos.z);
                maxX = Math.max(maxX, vert.pos.x);
                maxY = Math.max(maxY, vert.pos.y);
                maxZ = Math.max(maxZ, vert.pos.z);
            }
            // Add face to json
            Direction dir = Direction.getNearest(quad.normal.x, quad.normal.y, quad.normal.z).getOpposite();
            String key = dir.name().toLowerCase();
            JsonObject face = new JsonObject();
            face.add("uv", jsonArrayOf(u1, v1, u2, v2));
            face.addProperty("texture", -1);
            faces.add(key, face);
        }
        cubeJson.add("faces", faces);

        // Set cube's from and to values, after processing them
        if (isLivingEntity) {
            minX = -minX;
            minY = -minY;
            maxX = -maxX;
            maxY = -maxY;
        }
        minX += parentPos.x;
        maxX += parentPos.x;
        minY += parentPos.y;
        maxY += parentPos.y;
        minZ += parentPos.z;
        maxZ += parentPos.z;
        if (isLivingEntity) {
            float temp = minX; minX = maxX; maxX = temp;
            temp = minY; minY = maxY; maxY = temp;
        }
        cubeJson.add("from", jsonArrayOf(minX, minY, minZ));
        cubeJson.add("to", jsonArrayOf(maxX, maxY, maxZ));

        // Add uuid and return
        String uuid = UUID.randomUUID().toString();
        cubeJson.addProperty("uuid", uuid);

        elements.add(cubeJson); // Add to elements list
        return uuid;
    }

    private static JsonArray jsonArrayOf(float... values) {
        JsonArray res = new JsonArray();
        for (float value : values)
            res.add(value);
        return res;
    }

}
