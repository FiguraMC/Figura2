package org.figuramc.figura.model.part;

import com.mojang.blaze3d.vertex.PoseStack;
import org.figuramc.figura.util.FiguraMatrixStack;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Data representing the customizations of a FiguraModelPart.
 * Extracted into its own class for organization.
 */
public class PartTransform {

    private final Vector3f origin = new Vector3f(); // Origin is the pivot point and also a translation. Same as blockbench
    private final Vector3f position = new Vector3f();
    private final Quaternionf rotation = new Quaternionf();
    private final Vector3f scale = new Vector3f(1, 1, 1);

    private final Matrix4f positionMatrix = new Matrix4f();
    private final Matrix3f normalMatrix = new Matrix3f();

    private boolean visible = true;

    private boolean needsMatrixUpdate = false; // Whether the position and normal matrices need to be recalculated.
    private boolean isDirty = false;
    private boolean isIdentity = true; // Whether this has never been changed, so we can assume it's the identity transform. Most changes set this to false.

    // Getters and setters
    public void setOrigin(Vector3f origin) { this.origin.set(origin); this.needsMatrixUpdate = true; isDirty = true; isIdentity = isIdentity && origin.x == 0 && origin.y == 0 && origin.z == 0; }
    public void setOrigin(float x, float y, float z) { this.origin.set(x, y, z); this.needsMatrixUpdate = true; isDirty = true; isIdentity = isIdentity && x == 0 && y == 0 && z == 0; }
    public Vector3f getOrigin() { return origin; }

    public void setPosition(Vector3f pos) { this.position.set(pos); this.needsMatrixUpdate = true; isDirty = true; isIdentity = isIdentity && pos.x == 0 && pos.y == 0 && pos.z == 0; }
    public void setPosition(float x, float y, float z) { this.position.set(x, y, z); this.needsMatrixUpdate = true; isDirty = true; isIdentity = isIdentity && x == 0 && y == 0 && z == 0; }
    public Vector3f getPosition() { return position; }

    public void setScale(Vector3f scale) { this.scale.set(scale); this.needsMatrixUpdate = true; isDirty = true; isIdentity = isIdentity && scale.x == 1 && scale.y == 1 && scale.z == 1; }
    public void setScale(float x, float y, float z) { this.scale.set(x, y, z); this.needsMatrixUpdate = true; isDirty = true; isIdentity = isIdentity && x == 1 && y == 1 && z == 1; }
    public Vector3f getScale() { return scale; }

    public void setRotation(Quaternionf rotation) { this.rotation.set(rotation); this.needsMatrixUpdate = true; isDirty = true; isIdentity = isIdentity && rotation.x == 0 && rotation.y == 0 && rotation.z == 0 && rotation.w == 1; }
    public void setRotation(float x, float y, float z, float w) { this.rotation.set(x, y, z, w); this.needsMatrixUpdate = true; isDirty = true; isIdentity = isIdentity && x == 0 && y == 0 && z == 0 && w == 1; }
    public void setRotationEulerRad(Vector3f euler) { this.rotation.rotationZYX(euler.z, euler.y, euler.x); this.needsMatrixUpdate = true; isDirty = true; isIdentity = isIdentity && euler.x == 0 && euler.y == 0 && euler.z == 0; }
    public void setRotationEulerRad(float x, float y, float z) { this.rotation.rotationZYX(z, y, x); this.needsMatrixUpdate = true; isDirty = true; isIdentity = isIdentity && x == 0 && y == 0 && z == 0; }
    public void setRotationEulerDeg(Vector3f euler) { float s = Mth.PI / 180; setRotationEulerRad(euler.x*s, euler.y*s, euler.z*s); }
    public void setRotationEulerDeg(float x, float y, float z) { float s = Mth.PI / 180; setRotationEulerRad(x*s, y*s, z*s); }
    public Quaternionf getRotation() { return rotation; }

    public void setVisible(boolean visible) { this.visible = visible; this.isDirty = true; }
    public boolean getVisible() { return this.visible; }

    // Set position matrix and recompute normal matrix from it. Sets needsMatrixUpdate to false.
    public void setMatrix(Matrix4f positionMatrix) { this.positionMatrix.set(positionMatrix); this.positionMatrix.normal(normalMatrix); this.needsMatrixUpdate = false; isDirty = true; isIdentity = false; }

    // Set position matrix without recomputing normal matrix. Sets needsMatrixUpdate to false.
    public void setPositionMatrix(Matrix4f positionMatrix) { this.positionMatrix.set(positionMatrix); this.needsMatrixUpdate = false; isDirty = true; isIdentity = false; }

    // Recalculate things if needed, then set the normal matrix.
    public void setNormalMatrix(Matrix3f normalMatrix) { recalculateIfNeeded(); this.normalMatrix.set(normalMatrix); this.needsMatrixUpdate = false; isDirty = true; isIdentity = false; }

    public void reset() {
        origin.zero();
        position.zero();
        rotation.identity();
        scale.set(1, 1, 1);
        positionMatrix.identity();
        normalMatrix.identity();
        visible = true;
        needsMatrixUpdate = false;
        // Do not reset isDirty!
        isIdentity = true;
    }

    public void markDirty() {
        this.isDirty = true;
    }

    public boolean fetchDirty() {
        boolean wasDirty = isDirty;
        isDirty = false;
        return wasDirty;
    }

    private void recalculateIfNeeded() {
        if (needsMatrixUpdate) {
            // Compute position matrix
            positionMatrix
                    .translation(origin.x / 16, origin.y / 16, origin.z / 16)
                    .rotate(rotation)
                    .scale(scale)
                    .translate(position.x / 16, position.y / 16, position.z / 16);
            // Compute normal matrix
            positionMatrix.normal(normalMatrix);
            // No longer needs update (for now)
            needsMatrixUpdate = false;
        }
    }

    public Matrix4f getPositionMatrix() {
        recalculateIfNeeded();
        return positionMatrix;
    }

    // Affect a matrix stack with this transform.
    public void affect(FiguraMatrixStack matrixStack) {
        if (isIdentity) return; // Identity, this has no effect
        recalculateIfNeeded();
        // Apply the matrices
        matrixStack.multiply(positionMatrix, normalMatrix);
    }

    // Affect a vanilla pose stack with this transform.
    public void affect(PoseStack vanillaPoseStack) {
        if (isIdentity) return; // Identity, this has no effect
        recalculateIfNeeded();
        // Apply the matrices
        vanillaPoseStack.last().pose().mul(positionMatrix);
        vanillaPoseStack.last().normal().mul(normalMatrix);
    }

    public void cloneFrom(PartTransform other) {
        this.origin.set(other.origin);
        this.position.set(other.position);
        this.rotation.set(other.rotation);
        this.scale.set(other.scale);
        this.positionMatrix.set(other.positionMatrix);
        this.normalMatrix.set(other.normalMatrix);
        this.visible = other.visible;
        this.needsMatrixUpdate = other.needsMatrixUpdate;
        this.isIdentity = other.isIdentity;
        this.isDirty = other.isDirty;
    }

}
