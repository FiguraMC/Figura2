package org.figuramc.figura.model.part;

import com.mojang.blaze3d.vertex.PoseStack;
import org.figuramc.figura.util.FiguraMatrixStack;
import net.minecraft.util.Mth;
import org.joml.*;

/**
 * Data representing the customizations of a FiguraModelPart.
 * Extracted into its own class for organization.
 */
public class PartTransform {

    private final Vector3f origin = new Vector3f(); // Origin is the pivot point and also a translation. Same as blockbench
    private final Vector3f position = new Vector3f();

    // Quaternion and rotation are kept in sync constantly.
    // Whenever one is updated, the other is as well.
    // This is to make the code more similar to THREE.js which is used by Blockbench.
    private final Quaternionf quaternion = new Quaternionf();
    private final Vector3f rotation = new Vector3f(); // ZYX euler angles in radians

    private final Vector3f scale = new Vector3f(1, 1, 1);

    private final Matrix4f positionMatrix = new Matrix4f();
    private final Matrix3f normalMatrix = new Matrix3f();

    private boolean visible = true;

    private boolean needsMatrixUpdate = false; // Whether the position and normal matrices need to be recalculated.
    private boolean isDirty = false; // Whether the transform was changed at all since last time, including via setMatrix (which wouldn't cause a matrix update). Needed for GPU upload.
    private boolean isIdentity = true; // Whether this was ever modified away from the identity matrix. If it never was, we can skip a matrix multiply.

    // Getters and setters
    public void setOrigin(Vector3f origin) { this.origin.set(origin); markDirty(); }
    public void setOrigin(float x, float y, float z) { this.origin.set(x, y, z); markDirty(); }
    public Vector3f getOrigin() { return origin; }

    public void setPosition(Vector3f pos) { this.position.set(pos); markDirty(); }
    public void setPosition(float x, float y, float z) { this.position.set(x, y, z); markDirty(); }
    public Vector3f getPosition() { return position; }

    public void setScale(Vector3f scale) { this.scale.set(scale); markDirty(); }
    public void setScale(float x, float y, float z) { this.scale.set(x, y, z); markDirty(); }
    public Vector3f getScale() { return scale; }

    public void setEulerRad(Vector3f euler) { this.rotation.set(euler); this.quaternion.rotationZYX(euler.z, euler.y, euler.x); markDirty(); }
    public void setEulerRad(float x, float y, float z) { this.rotation.set(x, y, z); this.quaternion.rotationZYX(z, y, x); markDirty(); }
    public void setEulerDeg(Vector3f euler) { setEulerRad(euler.x * Mth.DEG_TO_RAD, euler.y * Mth.DEG_TO_RAD, euler.z * Mth.DEG_TO_RAD); }
    public void setEulerDeg(float x, float y, float z) { setEulerRad(x * Mth.DEG_TO_RAD, y * Mth.DEG_TO_RAD, z * Mth.DEG_TO_RAD); }
    public void setQuaternion(Quaternionf quat) { this.quaternion.set(quat).getEulerAnglesZYX(rotation); markDirty(); }
    public void setQuaternion(float x, float y, float z, float w) { this.quaternion.set(x, y, z, w).getEulerAnglesZYX(rotation); markDirty(); }
    public Vector3f getEulerRad() { return rotation; }
    public Vector3f getEulerDeg() { return new Vector3f(rotation).mul(Mth.RAD_TO_DEG); }
    public Quaternionf getQuaternion() { return quaternion; }

    public void setVisible(boolean visible) { this.visible = visible; this.isDirty = true; }
    public boolean getVisible() { return this.visible; }

    // Set position matrix and recompute normal matrix from it. Sets needsMatrixUpdate to false.
    public void setMatrix(Matrix4f positionMatrix) { this.positionMatrix.set(positionMatrix); this.positionMatrix.normal(normalMatrix); markDirtyNoMatrix(); }

    // Set position matrix without recomputing normal matrix. Sets needsMatrixUpdate to false.
    public void setPositionMatrix(Matrix4f positionMatrix) { this.positionMatrix.set(positionMatrix); markDirtyNoMatrix(); }

    // Recalculate things if needed, then set the normal matrix.
    public void setNormalMatrix(Matrix3f normalMatrix) { recalculateIfNeeded(); this.normalMatrix.set(normalMatrix); markDirtyNoMatrix(); }

    public void reset() {
        origin.zero();
        position.zero();
        rotation.zero();
        quaternion.identity();
        scale.set(1, 1, 1);
        positionMatrix.identity();
        normalMatrix.identity();
        visible = true;
        needsMatrixUpdate = false;
        isIdentity = true; // Is now identity
        isDirty = true; // It changed
    }

    public final void markDirty() {
        this.needsMatrixUpdate = true;
        this.isDirty = true;
        this.isIdentity = false;
    }

    public final void markDirtyNoMatrix() {
        this.isDirty = true;
        this.isIdentity = false;
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
                    .rotate(quaternion)
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
