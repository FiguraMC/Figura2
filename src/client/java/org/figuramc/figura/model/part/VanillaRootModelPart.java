package org.figuramc.figura.model.part;

import org.figuramc.figura.data.AvatarMaterials;
import org.figuramc.figura.model.texture.AvatarTexture;
import org.figuramc.figura.script_hooks.ScriptCallback;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class VanillaRootModelPart extends RootModelPart {

    // An extra transform, modifiable by the script, that affects the actual vanilla part's rendering itself
    public boolean visible;
    public final Vector3f originOffset = new Vector3f();
    public final Vector3f rotationOffset = new Vector3f();
    public final Vector3f scaleMultiplier = new Vector3f(1, 1, 1);
    public final Vector3f positionOffset = new Vector3f();

    // Booleans that say whether to cancel each vanilla transform phase individually
    private boolean cancelVanillaOrigin, cancelVanillaRotation, cancelVanillaScale;

    // Vectors that are saved during rendering and kept for retrieval later
    public final Vector3f storedVanillaOrigin = new Vector3f(), storedVanillaRotation = new Vector3f(), storedVanillaScale = new Vector3f(1, 1, 1);

    // Callbacks which are run before the vanilla part is rendered.
    // Note this refers to the REAL vanilla part's rendering,
    // NOT the blockbench version; the blockbench version is QUEUED to render later!

    // Args to callbacks: None yet
    public final ArrayList<ScriptCallback> vanillaRenderCallbacks = new ArrayList<>(0);


    public VanillaRootModelPart(AvatarMaterials.VanillaRootPartMaterials materials, List<AvatarTexture> textures) {
        super(materials.modelPartMaterials(), textures, true);
        this.visible = !materials.replaceRoot().getValue();
    }

    // // // // // //           // // // // // //
    // // // // // // SCRIPTING // // // // // //
    // // // // // //           // // // // // //

    // Functions involving the extra transforms

    public void setVanillaOrigin(Vector3f origin) { this.originOffset.set(origin); }
    public void setVanillaRotation(Vector3f euler) { this.rotationOffset.set(euler).mul(Mth.DEG_TO_RAD); }
    public void setVanillaScale(Vector3f scale) { this.scaleMultiplier.set(scale); }
    public void setVanillaPosition(Vector3f position) { this.positionOffset.set(position); }
    public void setVanillaVisible(boolean visible) { this.visible = visible; }

    public boolean getVanillaVisible() { return this.visible; }

    // Functions for cancelling the vanilla transforms

    public void setCancelVanillaOrigin(boolean cancel) { this.cancelVanillaOrigin = cancel; }
    public void setCancelVanillaRotation(boolean cancel) { this.cancelVanillaRotation = cancel; }
    public void setCancelVanillaScale(boolean cancel) { this.cancelVanillaScale = cancel; }

    public boolean getCancelVanillaOrigin() { return this.cancelVanillaOrigin; }
    public boolean getCancelVanillaRotation() { return this.cancelVanillaRotation; }
    public boolean getCancelVanillaScale() { return this.cancelVanillaScale; }

    // Getting the stored vanilla values

    public Vector3f getStoredVanillaOrigin() { return new Vector3f(storedVanillaOrigin); }
    public Vector3f getStoredVanillaRotation() { return new Vector3f(storedVanillaRotation).mul(Mth.RAD_TO_DEG); } // Convert to degrees
    public Vector3f getStoredVanillaScale() { return new Vector3f(storedVanillaScale); }

    // Render callbacks
    public void addVanillaRenderCallback(ScriptCallback callback) { vanillaRenderCallbacks.add(callback); }

}
