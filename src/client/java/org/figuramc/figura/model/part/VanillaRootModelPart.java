package org.figuramc.figura.model.part;

import org.figuramc.figura.data.AvatarMaterials;
import org.figuramc.figura.model.texture.AvatarTexture;
import org.figuramc.figura.script_hooks.ScriptCallback;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class VanillaRootModelPart extends RootModelPart {

    // This part transform isn't used for matrices; only for storing and fetching values for the mixins.
    public final PartTransform vanillaTransform;

    // Booleans that say whether to cancel each vanilla transform phase individually
    public boolean cancelVanillaOrigin, cancelVanillaRotation, cancelVanillaScale;

    // Vectors that are saved during rendering and kept for retrieval later
    public final Vector3f
            storedVanillaOrigin = new Vector3f(),
            storedVanillaRotation = new Vector3f(),
            storedVanillaScale = new Vector3f(1, 1, 1);

    // Callbacks which are run before the vanilla part is rendered.
    // Note this refers to the REAL vanilla part's rendering,
    // NOT the blockbench version; the blockbench version is QUEUED to render later!

    // Args to callbacks: None yet
    public final ArrayList<ScriptCallback> vanillaRenderCallbacks = new ArrayList<>(0);

    public VanillaRootModelPart(AvatarMaterials.VanillaRootPartMaterials materials, List<AvatarTexture> textures) {
        super(materials.modelPartMaterials(), textures);
        vanillaTransform = new PartTransform();
        vanillaTransform.setVisible(!materials.replaceRoot().getValue());
    }

}
