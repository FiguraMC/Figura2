package org.figuramc.figura.model.part;

import org.figuramc.figura.data.AvatarMaterials;
import org.figuramc.figura.model.texture.AvatarTexture;
import org.figuramc.figura.script_hooks.ScriptAllow;
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

    @ScriptAllow public void setVanillaOrigin(Vector3f origin) { this.originOffset.set(origin); }
    @ScriptAllow public void setVanillaRotation(Vector3f euler) { this.rotationOffset.set(euler).mul(Mth.DEG_TO_RAD); }
    @ScriptAllow public void setVanillaScale(Vector3f scale) { this.scaleMultiplier.set(scale); }
    @ScriptAllow public void setVanillaPosition(Vector3f position) { this.positionOffset.set(position); }
    @ScriptAllow public void setVanillaVisible(boolean visible) { this.visible = visible; }

    @ScriptAllow public boolean getVanillaVisible() { return this.visible; }

    // Functions for cancelling the vanilla transforms

    @ScriptAllow public void setCancelVanillaOrigin(boolean cancel) { this.cancelVanillaOrigin = cancel; }
    @ScriptAllow public void setCancelVanillaRotation(boolean cancel) { this.cancelVanillaRotation = cancel; }
    @ScriptAllow public void setCancelVanillaScale(boolean cancel) { this.cancelVanillaScale = cancel; }

    @ScriptAllow public boolean getCancelVanillaOrigin() { return this.cancelVanillaOrigin; }
    @ScriptAllow public boolean getCancelVanillaRotation() { return this.cancelVanillaRotation; }
    @ScriptAllow public boolean getCancelVanillaScale() { return this.cancelVanillaScale; }

    // Getting the stored vanilla values

    @ScriptAllow public Vector3f getStoredVanillaOrigin() { return new Vector3f(storedVanillaOrigin); }
    @ScriptAllow public Vector3f getStoredVanillaRotation() { return new Vector3f(storedVanillaRotation).mul(Mth.RAD_TO_DEG); } // Convert to degrees
    @ScriptAllow public Vector3f getStoredVanillaScale() { return new Vector3f(storedVanillaScale); }

    // Render callbacks
    @ScriptAllow public void addVanillaRenderCallback(ScriptCallback callback) { vanillaRenderCallbacks.add(callback); }

}
