package org.figuramc.figura.model.part;

import com.mojang.blaze3d.vertex.PoseStack;
import org.figuramc.figura.script_hooks.mem_count.MarkedObjectBase;
import org.figuramc.figura.script_hooks.mem_count.MemoryCountable;
import org.figuramc.figura.script_hooks.mem_count.MemoryCounter;
import org.figuramc.figura.util.FiguraTransformStack;
import net.minecraft.util.Mth;
import org.joml.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Data representing the customizations of a FiguraModelPart.
 * Extracted into its own class for organization.
 */
public class PartTransform extends MarkedObjectBase {

    private final Vector3f origin = new Vector3f(); // Origin is the pivot point and also a translation. Same as blockbench
    private final Vector3f position = new Vector3f();

    // Quaternion and rotation are kept in sync constantly.
    // Whenever one is updated, the other is as well.
    // This is to make the code more similar to THREE.js which is used by Blockbench.
    private final Quaternionf quaternion = new Quaternionf();
    private final Vector3f rotation = new Vector3f(); // ZYX euler angles in radians
    private byte rotQuatState = NO_UPDATE_NEEDED;
    private static final byte NO_UPDATE_NEEDED = 0;
    private static final byte QUAT_NEEDS_UPDATE = 1;
    private static final byte ROT_NEEDS_UPDATE = 2;

    private final Vector3f scale = new Vector3f(1, 1, 1);

    private final Matrix4f positionMatrix = new Matrix4f();
    private final Matrix3f normalMatrix = new Matrix3f();

    private boolean visible = true;

    private final Vector4f color = new Vector4f(1, 1, 1, 1); // Packed color + alpha

    private boolean needsMatrixUpdate = false; // Whether the position and normal matrices need to be recalculated.
    private boolean isDirty = false; // Whether the transform was changed at all since last time, including via setMatrix (which wouldn't cause a matrix update). Needed for GPU upload.
    private boolean isFullyDefault = true; // Whether this was ever modified away from the default transform. If it never was, we can skip the entire transform process.

    // Modifiers applied to this transform (mostly animators and mimics)
    public final List<Modifier> modifiers = new ArrayList<>(0);

    // Getters, setters, and modifiers.
    public void setOrigin(Vector3fc origin) { this.origin.set(origin); markDirty(); }
    public void setOrigin(float x, float y, float z) { this.origin.set(x, y, z); markDirty(); }
    public Vector3fc getOrigin() { return origin; }
    public void addOrigin(Vector3fc offset) { this.origin.add(offset); markDirty(); }
    public void addOrigin(float x, float y, float z) { this.origin.add(x, y, z); markDirty(); }

    public void setPosition(Vector3fc pos) { this.position.set(pos); markDirty(); }
    public void setPosition(float x, float y, float z) { this.position.set(x, y, z); markDirty(); }
    public Vector3fc getPosition() { return position; }
    public void addPosition(Vector3fc offset) { this.position.add(offset); markDirty(); }
    public void addPosition(float x, float y, float z) { this.position.add(x, y, z); markDirty(); }

    public void setScale(Vector3fc scale) { this.scale.set(scale); markDirty(); }
    public void setScale(float x, float y, float z) { this.scale.set(x, y, z); markDirty(); }
    public Vector3fc getScale() { return scale; }
    public void mulScale(Vector3fc multiplier) { this.scale.mul(multiplier); markDirty(); }
    public void mulScale(float x, float y, float z) { this.scale.mul(x, y, z); markDirty(); }

    public void setColor(Vector4f color) { this.color.set(color); markDirtyNoMatrix(); }
    public void setColor(float r, float g, float b, float a) { this.color.set(r, g, b, a); markDirtyNoMatrix(); }
    public Vector4f getColor() { return this.color; }

    public void setEulerRad(Vector3fc euler) { this.rotation.set(euler); this.rotQuatState = QUAT_NEEDS_UPDATE; this.quaternion.rotationZYX(euler.z(), euler.y(), euler.x()); markDirty(); }
    public void setEulerRad(float x, float y, float z) { this.rotation.set(x, y, z); this.rotQuatState = QUAT_NEEDS_UPDATE; markDirty(); }
    public void setEulerDeg(Vector3fc euler) { setEulerRad(euler.x() * Mth.DEG_TO_RAD, euler.y() * Mth.DEG_TO_RAD, euler.z() * Mth.DEG_TO_RAD); }
    public void setEulerDeg(float x, float y, float z) { setEulerRad(x * Mth.DEG_TO_RAD, y * Mth.DEG_TO_RAD, z * Mth.DEG_TO_RAD); }
    public void setQuaternion(Quaternionfc quat) { this.quaternion.set(quat).getEulerAnglesZYX(rotation); this.rotQuatState = ROT_NEEDS_UPDATE; markDirty(); }
    public void setQuaternion(float x, float y, float z, float w) { this.quaternion.set(x, y, z, w).getEulerAnglesZYX(rotation); this.rotQuatState = ROT_NEEDS_UPDATE; markDirty(); }
    public Vector3fc getEulerRad() { updateRot(); return rotation; }
    public Vector3fc getEulerDeg() { updateRot(); return new Vector3f(rotation).mul(Mth.RAD_TO_DEG); }
    public Quaternionfc getQuaternion() { updateQuat(); return quaternion; }

    public void addEulerRad(Vector3fc offset) { updateRot(); rotation.add(offset); this.rotQuatState = QUAT_NEEDS_UPDATE; }
    public void addEulerRad(float x, float y, float z) { updateRot(); rotation.add(x, y, z); this.rotQuatState = QUAT_NEEDS_UPDATE; }
    public void addEulerDeg(Vector3fc offset) { updateRot(); rotation.add(offset.x() * Mth.DEG_TO_RAD, offset.y() * Mth.DEG_TO_RAD, offset.z() * Mth.DEG_TO_RAD); this.rotQuatState = QUAT_NEEDS_UPDATE; }
    public void addEulerDeg(float x, float y, float z) { updateRot(); rotation.add(x * Mth.DEG_TO_RAD, y * Mth.DEG_TO_RAD, z * Mth.DEG_TO_RAD); this.rotQuatState = QUAT_NEEDS_UPDATE; }
    public void mulQuat(Quaternionfc modifier) { updateQuat(); quaternion.mul(modifier); this.rotQuatState = ROT_NEEDS_UPDATE; }
    public void premulQuat(Quaternionfc modifier) { updateQuat(); quaternion.premul(modifier); this.rotQuatState = ROT_NEEDS_UPDATE; }

    // Helpers to ensure rot/quat is up to date
    private void updateRot() { if (this.rotQuatState == ROT_NEEDS_UPDATE) { this.quaternion.getEulerAnglesZYX(rotation); this.rotQuatState = NO_UPDATE_NEEDED; } }
    private void updateQuat() { if (this.rotQuatState == QUAT_NEEDS_UPDATE) { this.quaternion.rotationZYX(rotation.z, rotation.y, rotation.x); this.rotQuatState = NO_UPDATE_NEEDED; } }

    public void setVisible(boolean visible) { this.visible = visible; this.isDirty = true; }
    public boolean getVisible() { return this.visible; }
    public void andVisible(boolean multiplier) { this.visible &= multiplier; this.isDirty = true; }

    // Set position matrix and recompute normal matrix from it. Sets needsMatrixUpdate to false.
    public void setMatrix(Matrix4f positionMatrix) { this.positionMatrix.set(positionMatrix); this.positionMatrix.normal(normalMatrix); markDirtyOverrideMatrix(); }

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
        isFullyDefault = true; // Is now identity
        isDirty = true; // It changed
    }

    public final void markDirty() {
        this.needsMatrixUpdate = true;
        this.isDirty = true;
        this.isFullyDefault = false;
    }

    public final void markDirtyNoMatrix() {
        this.isDirty = true;
        this.isFullyDefault = false;
    }

    public final void markDirtyOverrideMatrix() {
        this.needsMatrixUpdate = false;
        this.isDirty = true;
        this.isFullyDefault = false;
    }

    public boolean fetchDirty() {
        boolean wasDirty = isDirty;
        isDirty = false;
        return wasDirty;
    }

    private void recalculateIfNeeded() {
        if (needsMatrixUpdate) {
            updateQuat(); // Ensure quat is ready before use
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

    // TODO: look into performance of this strategy and see if we can do better
    private static final PartTransform SAVE_STATE = new PartTransform();

    // Affect a transform stack with this transform.
    public void affect(FiguraTransformStack matrixStack) {
        synchronized (SAVE_STATE) { // Should only ever be called on the render thread, but just to be safe we'll synchronize
            // If we have modifiers, apply them
            if (!modifiers.isEmpty()) {
                // Save state before the modifiers are applied
                SAVE_STATE.cloneTransforms(this);
                // Apply modifiers
                for (Modifier modifier : modifiers)
                    modifier.modify(this);
            } else if (isFullyDefault) return; // No modifiers and fully default, return
            recalculateIfNeeded();
            // Apply the matrices and other things
            matrixStack.multiply(positionMatrix, normalMatrix);
            matrixStack.color(this.color);
            // If we had modifiers, undo the savestate
            if (!modifiers.isEmpty()) this.cloneTransforms(SAVE_STATE);
        }
    }

    // Affect a vanilla pose stack with this transform.
    public void affect(PoseStack vanillaPoseStack) {
        synchronized (SAVE_STATE) { // Should only ever be called on the render thread, but just to be safe we'll synchronize
            // If we have modifiers, apply them
            if (!modifiers.isEmpty()) {
                // Save state before the modifiers are applied
                SAVE_STATE.cloneTransforms(this);
                // Apply modifiers
                for (Modifier modifier : modifiers)
                    modifier.modify(this);
            } else if (isFullyDefault) return; // No modifiers and fully default, return
            recalculateIfNeeded();
            // Apply the matrices
            vanillaPoseStack.last().pose().mul(positionMatrix);
            vanillaPoseStack.last().normal().mul(normalMatrix);
            // If we had modifiers, undo the savestate
            if (!modifiers.isEmpty()) this.cloneTransforms(SAVE_STATE);
        }
    }

    // Clone transforms and save them for later, used with save-stating for modifiers
    private void cloneTransforms(PartTransform other) {
        this.origin.set(other.origin);
        this.position.set(other.position);
        this.rotation.set(other.rotation);
        this.scale.set(other.scale);
        this.positionMatrix.set(other.positionMatrix);
        this.normalMatrix.set(other.normalMatrix);
        this.visible = other.visible;
        this.needsMatrixUpdate = other.needsMatrixUpdate;
        this.isFullyDefault = other.isFullyDefault;
        this.isDirty = other.isDirty;
    }

    @Override
    protected long traceNoMark(MemoryCounter counter, int depth) {
        for (Modifier modifier : modifiers)
            counter.trace(modifier, depth);
        return 200 + modifiers.size() * POINTER_SIZE;
    }

    // A modifier for a transform. Each transform keeps a list of these.
    // Modifiers should call things like "addOrigin" or "mulScale" to modify values.
    public interface Modifier extends MemoryCountable {
        void modify(PartTransform transform);
    }



}
