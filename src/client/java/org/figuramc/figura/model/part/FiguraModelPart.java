package org.figuramc.figura.model.part;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;
import org.figuramc.figura.data.AvatarMaterials;
import org.figuramc.figura.model.optimized.OptimizedBufferBuilder;
import org.figuramc.figura.model.optimized.OptimizedRendering;
import org.figuramc.figura.model.optimized.PartDataStorageBuffer;
import org.figuramc.figura.model.optimized.RenderingMode;
import org.figuramc.figura.model.texture.AvatarTexture;
import org.figuramc.figura.script_hooks.ScriptError;
import org.figuramc.figura.script_hooks.ScriptCallback;
import org.figuramc.figura.script_hooks.mem_count.MarkedObjectBase;
import org.figuramc.figura.script_hooks.mem_count.MemoryCounter;
import org.figuramc.figura.util.ListUtils;
import org.figuramc.figura.util.FiguraMatrixStack;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.Nullable;
import org.joml.*;

import java.lang.Math;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Corresponds to a Group in Blockbench.
 *
 * Unlike previously, Figura's scripting no longer allows manipulation of individual cubes and meshes, only of groups.
 * Here's why:
 * - This is more in-line with Blockbench, as Blockbench only allows animations to affect groups
 * - Also more in-line with Minecraft's internals, because Vanilla model parts do not have individual cube transforms
 * - This can be more efficient rendering-wise, because most of the time individual cubes are not articulated, allowing
 *   for less unneeded computation. When they do need to be articulated, one can simply add a group for said cube.
 */
public class FiguraModelPart extends MarkedObjectBase {

    public final String name;
    public final PartTransform transform = new PartTransform(); // The transform of this model part
//    private List<Animator> animators; // The animators which affect this model part
    public final ArrayList<FiguraModelPart> children; // The children of this model part in the hierarchy tree
    private final float[] vertices; // The vertices making up the cubes and meshes of the model part
    private @Nullable List<RenderType> renderTypes; // The render types which this part should be rendered with. If null, inherit, if non-null, override (assuming priority is high enough!)
    private int renderTypePriority; // If the render type priority is higher than the parent's, `this.renderTypes` can replace the current set of render types.

    // Script related variables:
    private @Nullable FiguraModelPart parent; // Storing the parent is dubious... might be some edge cases that could warrant removal?

    // Callbacks which are run during various stages of the rendering process.
    private final ArrayList<ScriptCallback>
            preRenderCallbacks = new ArrayList<>(0), // Zero sized at first, since most parts will not have callbacks
            midRenderCallbacks = new ArrayList<>(0),
            postRenderCallbacks = new ArrayList<>(0);


    protected FiguraModelPart(AvatarMaterials.ModelPartMaterials materials, List<AvatarTexture> textures, FiguraModelPart parent, boolean forceCompatible) {
        // Copy basic values out of the materials
        name = materials.name();
        this.parent = parent;
        transform.setOrigin(materials.origin());
        transform.setEulerDeg(materials.rotation());
        // Get children
        children = ListUtils.map(materials.children(), mat -> new FiguraModelPart(mat, textures, this, forceCompatible));

        // Get the list of render types:
        Vector4f uvModifier = new Vector4f(0, 0, 1, 1);
        renderTypes:
        for (boolean iter = true; iter; iter = false) {
            if (materials.textureIndex() != -1) {
                // If tex index is not -1, then generate a render type from the texture:
                AvatarTexture tex = textures.get(materials.textureIndex());
                ResourceLocation loc = tex.getLocation();
                // Choose the render type based on rendering mode
                if (!forceCompatible && RenderingMode.isOptimized()) {
                    // Use the custom optimized render type
                    renderTypes = new ArrayList<>(List.of(OptimizedRendering.OPTIMIZED_RENDER_TYPE.apply(loc)));
                } else {
                    // Use RenderType.entityTranslucent()
                    renderTypes = new ArrayList<>(List.of(RenderType.itemEntityTranslucentCull(loc)));
                }
                // Also, set the UV modifier from the texture (for atlases)
                uvModifier.set(tex.getUvValues());
            } else if (!children.isEmpty()) {
                // Otherwise, attempt to merge from children:
                List<RenderType> first = children.getFirst().renderTypes;
                for (int i = 1; i < children.size(); i++) {
                    if (!Objects.equals(children.get(i).renderTypes, first))
                        break renderTypes;
                }
                for (FiguraModelPart child : children) {
                    child.renderTypes = null;
                }
                this.renderTypes = first;
            }
        }

        // Get vertices
        FloatArrayList vertexData = new FloatArrayList();
        for (AvatarMaterials.CubeData cubeData : materials.cubes()) addVertices(vertexData, cubeData, uvModifier);
        for (AvatarMaterials.MeshData meshData : materials.meshes()) addVertices(vertexData, meshData, uvModifier);
        vertices = vertexData.toArray(new float[0]);
    }

    // Construct by extruding a texture
    public FiguraModelPart(String name, AvatarTexture texture, boolean forceCompatible) {
        this.name = name;
        this.parent = null;
        this.renderTypes = (!forceCompatible && RenderingMode.isOptimized()) ? new ArrayList<>(List.of(OptimizedRendering.OPTIMIZED_RENDER_TYPE.apply(texture.getLocation()))) : new ArrayList<>(List.of(RenderType.itemEntityTranslucentCull(texture.getLocation())));
        Vector4f uvModifier = texture.getUvValues();
        this.children = new ArrayList<>();
        FloatArrayList vertexData = new FloatArrayList();
        // Iterate in each direction!
        byte[] opacityStates = new byte[Math.max(texture.getWidth(), texture.getHeight()) + 2];
        int w = texture.getWidth();
        int h = texture.getHeight();

        // Horizontal sweep with vertical scanline
        for (int x = -1; x <= w; x++) {
            float buildingState = 0;
            for (int y = -1; y <= h; y++) {
                byte opacityState = (x < 0 || y < 0 || x >= w || y >= h) ? 0 : (byte) ((((texture.getPixelRGBA(x, y) >> 24) & 0xFF) + 253) / 254);
                if (x >= 0) {
                    byte prevOpacityState = opacityStates[y+1];
                    float newBuildingState = Math.signum(opacityState - prevOpacityState);
                    if (buildingState != newBuildingState) {
                        // We're either starting a quad or ending one...
                        if (newBuildingState == 0) {
                            // We're ending a quad
                            noSkinVert(vertexData, x, h - y, (buildingState - 1) / -2, (x + 0f) / w, (y + 0f) / h, -1f * buildingState, 0f, 0f, null, null, uvModifier);
                            noSkinVert(vertexData, x, h - y, (buildingState + 1) / 2, (x + buildingState) / w, (y + 0f) / h, -1f * buildingState, 0f, 0f, null, null, uvModifier);
                        } else {
                            // We're starting a quad
                            noSkinVert(vertexData, x, h - y, (newBuildingState + 1) / 2, (x + newBuildingState) / w, (y + 0f) / h, -1f * newBuildingState, 0f, 0f, null, null, uvModifier);
                            noSkinVert(vertexData, x, h - y, (newBuildingState - 1) / -2, (x + 0f) / w, (y + 0f) / h, -1f * newBuildingState, 0f, 0f, null, null, uvModifier);
                        }
                    }
                    buildingState = newBuildingState;
                }
                opacityStates[y+1] = opacityState;
            }
            if (buildingState != 0) throw new IllegalStateException("Failed to extrude texture? Internal bug in Figura, please report!");
        }

        // Vertical sweep with horizontal scanline
        for (int y = -1; y <= h; y++) {
            float buildingState = 0;
            for (int x = -1; x <= w; x++) {
                byte opacityState = (x < 0 || y < 0 || x >= w || y >= h) ? 0 : (byte) ((((texture.getPixelRGBA(x, y) >> 24) & 0xFF) + 253) / 254);
                if (y >= 0) {
                    byte prevOpacityState = opacityStates[x+1];
                    float newBuildingState = Math.signum(opacityState - prevOpacityState);
                    if (buildingState != newBuildingState) {
                        // We're either starting a quad or ending one...
                        if (newBuildingState == 0) {
                            // We're ending a quad
                            noSkinVert(vertexData, x, h - y, (buildingState + 1) / 2, (x + 0f) / w, (y + buildingState) / h, 0f, -1f * buildingState, 0f, null, null, uvModifier);
                            noSkinVert(vertexData, x, h - y, (buildingState - 1) / -2, (x + 0f) / w, (y + 0f) / h, 0f, -1f * buildingState, 0f, null, null, uvModifier);
                        } else {
                            // We're starting a quad
                            noSkinVert(vertexData, x, h - y, (newBuildingState - 1) / -2, (x + 0f) / w, (y + 0f) / h, 0f, -1f * newBuildingState, 0f, null, null, uvModifier);
                            noSkinVert(vertexData, x, h - y, (newBuildingState + 1) / 2, (x + 0f) / w, (y + newBuildingState) / h, 0f, -1f * newBuildingState, 0f, null, null, uvModifier);
                        }
                    }
                    buildingState = newBuildingState;
                }
                opacityStates[x+1] = opacityState;
            }
            if (buildingState != 0) throw new IllegalStateException("Failed to extrude texture? Internal bug in Figura, please report!");
        }

        // Put on the front and back panels
        noSkinVert(vertexData, 0f, 0f, 1f, 0f, 1f, 0f, 0f, 1f, null, null, uvModifier);
        noSkinVert(vertexData, w, 0f, 1f, 1f, 1f, 0f, 0f, 1f, null, null, uvModifier);
        noSkinVert(vertexData, w, h, 1f, 1f, 0f, 0f, 0f, 1f, null, null, uvModifier);
        noSkinVert(vertexData, 0f, h, 1f, 0f, 0f, 0f, 0f, 1f, null, null, uvModifier);

        noSkinVert(vertexData, w, 0f, 0f, 1f, 1f, 0f, 0f, -1f, null, null, uvModifier);
        noSkinVert(vertexData, 0f, 0f, 0f, 0f, 1f, 0f, 0f, -1f, null, null, uvModifier);
        noSkinVert(vertexData, 0f, h, 0f, 0f, 0f, 0f, 0f, -1f, null, null, uvModifier);
        noSkinVert(vertexData, w, h, 0f, 1f, 0f, 0f, 0f, -1f, null, null, uvModifier);

        vertices = vertexData.toArray(new float[0]);

        // Set up transform to be item-ish
        transform.setScale(1f/16, 1f/16, 1f/16);
        transform.setPosition(0f, 0f, 7.5f * 16f);
    }


    private static void addVertices(FloatArrayList vertexData, AvatarMaterials.CubeData cubeData, Vector4f uvModifier) {
        Vector3f f = cubeData.from();
        Vector3f t = cubeData.to();
        Vector3f o = cubeData.origin();
        Vector3f r = cubeData.rotation();

        // Scale down by 1/16 and rotate around its origin.
        Matrix4f transform = new Matrix4f()
                .scale(1.0f / 16)
                .translate(o.x, o.y, o.z)
                .rotate(new Quaternionf().rotationZYX(r.z * Mth.DEG_TO_RAD, r.y * Mth.DEG_TO_RAD, r.x * Mth.DEG_TO_RAD))
                .translate(-o.x, -o.y, -o.z)
        ;

        Matrix3f normalMat = transform.normal(new Matrix3f());

        for (int i = 0; i < 6; i++) {
            AvatarMaterials.CubeFace face = cubeData.faces()[i];
            if (face == null) continue;
            float u1 = face.uv().x();
            float v1 = face.uv().y();
            float u2 = face.uv().z();
            float v2 = face.uv().y();
            float u3 = face.uv().z();
            float v3 = face.uv().w();
            float u4 = face.uv().x();
            float v4 = face.uv().w();
            int faceRot = face.rot();
            while (faceRot > 0) { //rotate texture
                float temp = u1;
                u1 = u2; u2 = u3; u3 = u4; u4 = temp;
                temp = v1;
                v1 = v2; v2 = v3; v3 = v4; v4 = temp;
                faceRot--;
            }
            switch (i) {
                case 0 -> { //north
                    noSkinVert(vertexData, t.x, f.y, f.z, u4, v4, 0f, 0f, -1f, transform, normalMat, uvModifier);
                    noSkinVert(vertexData, f.x, f.y, f.z, u3, v3, 0f, 0f, -1f, transform, normalMat, uvModifier);
                    noSkinVert(vertexData, f.x, t.y, f.z, u2, v2,0f, 0f, -1f, transform, normalMat, uvModifier);
                    noSkinVert(vertexData, t.x, t.y, f.z, u1, v1, 0f, 0f, -1f, transform, normalMat, uvModifier);
                }
                case 1 -> { //east
                    noSkinVert(vertexData, t.x, f.y, t.z, u4, v4, 1f, 0f, 0f, transform, normalMat, uvModifier);
                    noSkinVert(vertexData, t.x, f.y, f.z, u3, v3, 1f, 0f, 0f, transform, normalMat, uvModifier);
                    noSkinVert(vertexData, t.x, t.y, f.z, u2, v2,1f, 0f, 0f, transform, normalMat, uvModifier);
                    noSkinVert(vertexData, t.x, t.y, t.z, u1, v1, 1f, 0f, 0f, transform, normalMat, uvModifier);
                }
                case 2 -> { //south
                    noSkinVert(vertexData, f.x, f.y, t.z, u4, v4, 0f, 0f, 1f, transform, normalMat, uvModifier);
                    noSkinVert(vertexData, t.x, f.y, t.z, u3, v3, 0f, 0f, 1f, transform, normalMat, uvModifier);
                    noSkinVert(vertexData, t.x, t.y, t.z, u2, v2, 0f, 0f, 1f, transform, normalMat, uvModifier);
                    noSkinVert(vertexData, f.x, t.y, t.z, u1, v1, 0f, 0f, 1f, transform, normalMat, uvModifier);
                }
                case 3 -> { //west
                    noSkinVert(vertexData, f.x, f.y, f.z, u4, v4, -1f, 0f, 0f, transform, normalMat, uvModifier);
                    noSkinVert(vertexData, f.x, f.y, t.z, u3, v3, -1f, 0f, 0f, transform, normalMat, uvModifier);
                    noSkinVert(vertexData, f.x, t.y, t.z, u2, v2, -1f, 0f, 0f, transform, normalMat, uvModifier);
                    noSkinVert(vertexData, f.x, t.y, f.z, u1, v1, -1f, 0f, 0f, transform, normalMat, uvModifier);
                }
                case 4 -> { //up
                    noSkinVert(vertexData, f.x, t.y, t.z, u4, v4, 0f, 1f, 0f, transform, normalMat, uvModifier);
                    noSkinVert(vertexData, t.x, t.y, t.z, u3, v3, 0f, 1f, 0f, transform, normalMat, uvModifier);
                    noSkinVert(vertexData, t.x, t.y, f.z, u2, v2, 0f, 1f, 0f, transform, normalMat, uvModifier);
                    noSkinVert(vertexData, f.x, t.y, f.z, u1, v1, 0f, 1f, 0f, transform, normalMat, uvModifier);
                }
                case 5 -> { //down
                    noSkinVert(vertexData, f.x, f.y, f.z, u4, v4, 0f, -1f, 0f, transform, normalMat, uvModifier);
                    noSkinVert(vertexData, t.x, f.y, f.z, u3, v3, 0f, -1f, 0f, transform, normalMat, uvModifier);
                    noSkinVert(vertexData, t.x, f.y, t.z, u2, v2, 0f, -1f, 0f, transform, normalMat, uvModifier);
                    noSkinVert(vertexData, f.x, f.y, t.z, u1, v1, 0f, -1f, 0f, transform, normalMat, uvModifier);
                }
            }
        }
    }

    private static void addVertices(FloatArrayList arr, AvatarMaterials.MeshData meshData, Vector4f uvModifier) {

        // Scale down by 1/16 and rotate around its origin:
        Vector3f o = meshData.origin();
        Vector3f r = meshData.rotation();
        Matrix4f transform = new Matrix4f()
                .scale(1.0f / 16)
                .translate(o.x, o.y, o.z)
                .rotate(new Quaternionf().rotationXYZ(r.x * Mth.DEG_TO_RAD, r.y * Mth.DEG_TO_RAD, r.z * Mth.DEG_TO_RAD)) // Meshes use XYZ rotation order! This is different from other part types!
//                .translate(-o.x, -o.y, -o.z) // Meshes use their origins as translations, unlike cubes which use them only as pivot points!
        ;
        Matrix3f normalMat = transform.normal(new Matrix3f());

        // Create the faces...
        List<AvatarMaterials.VertexData> vertices = meshData.vertices();
        List<Vector2f> uvs = meshData.uvs();
        int uv = 0;
        for (Vector4i face : meshData.faces()) {
            // Always do 3 vertices
            AvatarMaterials.VertexData v1 = vertices.get(face.x);
            AvatarMaterials.VertexData v2 = vertices.get(face.y);
            AvatarMaterials.VertexData v3 = vertices.get(face.z);
            Vector3f normal = computeNormal(v1.pos(), v2.pos(), v3.pos());
            meshVert(arr, v1, normal, uvs.get(uv++), transform, normalMat, uvModifier);
            meshVert(arr, v2, normal, uvs.get(uv++), transform, normalMat, uvModifier);
            meshVert(arr, v3, normal, uvs.get(uv++), transform, normalMat, uvModifier);
            if (face.w != -1) {
                // This is a quad, add the 4th vertex
                AvatarMaterials.VertexData v4 = vertices.get(face.w);
                meshVert(arr, v4, normal, uvs.get(uv++), transform, normalMat, uvModifier);
            } else {
                // This is a triangle but minecraft likes quads, so emit the 3rd vertex again
                meshVert(arr, v3, normal, uvs.get(uv - 1), transform, normalMat, uvModifier);
            }
        }
    }

    private static Vector3f computeNormal(Vector3f v1, Vector3f v2, Vector3f v3) {
        return v2.sub(v1, new Vector3f()).cross(v3.sub(v1, new Vector3f())).normalize();
    }

    private static void meshVert(FloatArrayList arr, AvatarMaterials.VertexData vertexData, Vector3f normalVec, Vector2f uv, Matrix4f transform, Matrix3f normalMat, Vector4f uvModifier) {
        Vector3f p = vertexData.pos();
        if (vertexData.skinningOffsets() == null) {
            emitVert(arr,
                    p.x, p.y, p.z, uv.x, uv.y, normalVec.x, normalVec.y, normalVec.z,
                    0, -1, -1, -1,
                    1f, 0f, 0f, 0f,
                    transform, normalMat, uvModifier
            );
        } else {
            Vector4i so = vertexData.skinningOffsets();
            Vector4f sw = vertexData.skinningWeights();
            emitVert(arr,
                    p.x, p.y, p.z, uv.x, uv.y, normalVec.x, normalVec.y, normalVec.z,
                    so.x, so.y, so.z, so.w, sw.x, sw.y, sw.z, sw.w,
                    transform, normalMat, uvModifier
            );
        }
    }

    private static void noSkinVert(FloatArrayList arr, float x, float y, float z, float u, float v, float nx, float ny, float nz, @Nullable Matrix4f transform, @Nullable Matrix3f normalMat, Vector4f uvModifier) {
        emitVert(arr, x, y, z, u, v, nx, ny, nz, 0, -1, -1, -1, 1f, 0f, 0f, 0f, transform, normalMat, uvModifier);
    }

    private static void emitVert(
            FloatArrayList arr,
            float x, float y, float z,
            float u, float v,
            float nx, float ny, float nz,
            int skinningOffset0, int skinningOffset1, int skinningOffset2, int skinningOffset3,
            float skinningWeight0, float skinningWeight1, float skinningWeight2, float skinningWeight3,
            @Nullable Matrix4f transform, @Nullable Matrix3f normalMat, Vector4f uvModifier
    ) {
        if (transform != null) {
            Vector3f pos = new Vector3f(x, y, z).mulPosition(transform);
            arr.add(pos.x); arr.add(pos.y); arr.add(pos.z);
        } else {
            arr.add(x); arr.add(y); arr.add(z);
        }
        if (normalMat != null) {
            Vector3f norm = new Vector3f(nx, ny, nz).mul(normalMat);
            arr.add(norm.x); arr.add(norm.y); arr.add(norm.z);
        } else {
            arr.add(nx); arr.add(ny); arr.add(nz);
        }
        arr.add(u * uvModifier.z + uvModifier.x); arr.add(v * uvModifier.w + uvModifier.y);
        arr.add(skinningOffset0); arr.add(skinningOffset1); arr.add(skinningOffset2); arr.add(skinningOffset3);
        arr.add(skinningWeight0); arr.add(skinningWeight1); arr.add(skinningWeight2); arr.add(skinningWeight3);
    }

    // Upload vertices to the buffer source. Used for immediate-mode-style, compatible rendering.
    protected void renderImmediate(
            MultiBufferSource bufferSource, // The source for render buffers
            List<RenderType> renderTypes, // The list of render types which the vertices will be passed to
            int renderTypePriority, // The current render type priority. The render types may only be changed if this.renderTypePriority >= renderTypePriority.
            FiguraMatrixStack matrixStack, // The current matrix stack
            float tickDelta,
            int light,
            int overlay
    ) throws StackOverflowError, ScriptError {

        invokeCallbacks(preRenderCallbacks, tickDelta);

        // Cancel if not invisible
        if (!transform.getVisible())
            return;

        // Update render types if we can and this has priority
        if (this.renderTypes != null && this.renderTypePriority >= renderTypePriority) {
            renderTypes = this.renderTypes;
            renderTypePriority = this.renderTypePriority; // Update priority to new level
        }

        // Push matrix stack, apply transforms
        matrixStack.push();
        transform.affect(matrixStack);
        // TODO Animations
//        for (Animator animator : animators)
//            animator.affect(matrixStack);

        invokeCallbacks(midRenderCallbacks, tickDelta);

        // Render children recursively
        for (FiguraModelPart child : children)
            child.renderImmediate(bufferSource, renderTypes, renderTypePriority, matrixStack, tickDelta, light, overlay);

        // If this has vertices, send them in
        if (vertices.length > 0) {
            Vector4f pos = new Vector4f();
            Vector3f norm = new Vector3f();
            Matrix4f posMatrix = matrixStack.peekPosition();
            Matrix3f normalMatrix = matrixStack.peekNormal();
            float[] vertices = this.vertices;
            for (RenderType renderType : renderTypes) {
                VertexConsumer consumer = bufferSource.getBuffer(renderType);
                for (int i = 0; i < vertices.length; i += 16) {
                    pos.set(vertices[i], vertices[i+1], vertices[i+2], 1.0).mul(posMatrix);
                    norm.set(vertices[i+3], vertices[i+4], vertices[i+5]).mul(normalMatrix);

                    // Skinning is ignored outside optimized mode (TODO add to immediate mode also)
                    consumer.addVertex(pos.x, pos.y, pos.z)
                            .setColor(1.0f, 1.0f, 1.0f, 1.0f)
                            .setUv(vertices[i+6], vertices[i+7])
                            .setNormal(norm.x, norm.y, norm.z)
                            .setLight(light)
                            .setOverlay(overlay);
                }
            }
        }

        invokeCallbacks(postRenderCallbacks, tickDelta);

        // Pop matrix stack
        matrixStack.pop();
    }

    // Emits vertices for optimized rendering.
    // This method, alongside calculateOptimizedTransforms, is part of the Optimized Mode analogue to renderImmediate().
    // This should only run rarely, whenever a tree rebuild is necessary.
    protected void buildOptimizedBuffers(
            OptimizedBufferBuilder bufferSource,
            List<RenderType> renderTypes,
            int renderTypePriority,
            MutableInt currentId
    ) throws StackOverflowError {
        // Update render types if we can and this has priority
        if (this.renderTypes != null && this.renderTypePriority >= renderTypePriority) {
            renderTypes = this.renderTypes;
            renderTypePriority = this.renderTypePriority; // Update priority to new level
        }
        // Fetch the part ID
        int partId = currentId.getAndIncrement();
        // If this has vertices, send them in
        if (vertices.length > 0) {
            float[] vertices = this.vertices;
            for (RenderType renderType : renderTypes) {
                VertexConsumer consumer = bufferSource.getBuffer(renderType);
                for (int i = 0; i < vertices.length; i += 16) {
                    // Pos, Normal, and UV are all as usual
                    consumer.addVertex(vertices[i], vertices[i+1], vertices[i+2])
                            .setNormal(vertices[i+3], vertices[i+4], vertices[i+5])
                            .setUv(vertices[i+6], vertices[i+7])
                    // UV1 and UV2 contain part IDs:
                            .setUv1(vertices[i+8] == -1 ? -1 : (partId + (int) vertices[i+8]), vertices[i+9] == -1 ? -1 : (partId + (int) vertices[i+9]))
                            .setUv2(vertices[i+10] == -1 ? -1 : (partId + (int) vertices[i+10]), vertices[i+11] == -1 ? -1 : (partId + (int) vertices[i+11]))
                    // Color contains the weights for the 4 part IDs.
                            .setColor(vertices[i+12], vertices[i+13], vertices[i+14], vertices[i+15]);
                }
            }
        }
        // Recurse on children
        for (FiguraModelPart child : children)
            child.buildOptimizedBuffers(bufferSource, renderTypes, renderTypePriority, currentId);
    }

    // Computes transforms for optimized rendering.
    // This method, alongside buildOptimizedBuffers, is part of the Optimized Mode analogue to renderImmediate().
    // While buildOptimizedBuffers() only runs on tree rebuild, this one will run every time it's rendered.
    protected void calculateOptimizedTransforms(
            PartDataStorageBuffer.StorageBufferUpdater partData,
            FiguraMatrixStack matrixStack,
            float tickDelta,

            boolean currentlyDirty,
            boolean currentlyVisible,
            MutableInt currentId
    ) throws StackOverflowError, ScriptError {
        // Pre-render callback
        invokeCallbacks(preRenderCallbacks, tickDelta);
        // Update visibility
        currentlyVisible = currentlyVisible && transform.getVisible();
        // Get new id
        int id = currentId.getAndIncrement();
        // Calculate transforms if needed
        matrixStack.push();
        transform.affect(matrixStack);
        // TODO Animations
        // Mid-render callback
        if (currentlyVisible) invokeCallbacks(midRenderCallbacks, tickDelta);
        // Update the storage buffer updater if this subtree is dirty
        currentlyDirty |= transform.fetchDirty();
        if (currentlyDirty) {
            partData.updatePartData(id).fillFromStack(matrixStack, currentlyVisible);
        }
        // Recurse to children, passing down dirt
        for (FiguraModelPart child : children)
            child.calculateOptimizedTransforms(partData, matrixStack, tickDelta, currentlyDirty, currentlyVisible, currentId);
        // Post-render callback
        if (currentlyVisible) invokeCallbacks(postRenderCallbacks, tickDelta);
        // Pop matrix stack
        matrixStack.pop();
    }

    private void invokeCallbacks(List<ScriptCallback> functions, Object... args) throws ScriptError {
        for (ScriptCallback f : functions)
            f.call(args);
    }


    // // // // // // MEMORY LIMITING // // // // // //

    @Override
    protected long traceNoMark(MemoryCounter counter, int depth) {
        // Trace other reachable objects
        for (FiguraModelPart child : children)
            counter.trace(child, depth);
        counter.trace(parent, depth);
        for (ScriptCallback callback : preRenderCallbacks) counter.trace(callback, depth);
        for (ScriptCallback callback : midRenderCallbacks) counter.trace(callback, depth);
        for (ScriptCallback callback : postRenderCallbacks) counter.trace(callback, depth);
        // TODO make our own FiguraRenderType which is traceable, instead of Minecraft RenderType.
        // Textures are reachable through render types, so this could be a memory exploit if we don't
        // trace them.

        // Random guess around 200 bytes for the constant sized stuff, don't feel like counting all that
        return 200
                + CHAR_SIZE * name.length()
                + FLOAT_SIZE * vertices.length
                + POINTER_SIZE * (preRenderCallbacks.size() + midRenderCallbacks.size() + postRenderCallbacks.size());
    }
}
