package org.figuramc.figura.avatars.components;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRenderer;
import org.figuramc.figura.avatars.AvatarComponent;
import org.figuramc.figura.avatars.AvatarError;
import org.figuramc.figura.model.part.PartTransform;
import org.figuramc.figura.model.part.RiggedHierarchy;
import org.figuramc.figura.script_hooks.callback.ScriptCallback;
import org.figuramc.figura.script_hooks.callback.items.CallbackItem;
import org.figuramc.figura.script_hooks.mem_count.AllocationTracker;
import org.figuramc.figura.vanillamodel.ModelNames;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Component that manages the vanilla entity's rendering for this avatar.
 * Requires an EntityUser component.
 */
public class VanillaRendering implements AvatarComponent<VanillaRendering> {

    public static final Type<VanillaRendering> TYPE = new Type<>(EntityUser.TYPE);
    public Type<VanillaRendering> getType() { return TYPE; }

    // The entity renderer for this entity
    public final EntityRenderer<?, ?> entityRenderer;
    // Helpful toggle to hide all model parts from appearing, but still render them (so mimics and such are still updated)
    public boolean hideAllModelParts;

    // Keep a mapping from ModelPart to associated data for this avatar.
    public final Map<ModelPart, VanillaPart> partMap = new IdentityHashMap<>();

    // Requires an entity renderer to create
    public VanillaRendering(EntityRenderer<?, ?> entityRenderer, @Nullable AllocationTracker allocationTracker) throws AvatarError {
        this.entityRenderer = entityRenderer;
        for (Model model : ModelNames.getModelsByName(entityRenderer).values()) {
            for (ModelPart part : model.root().getAllParts()) {
                partMap.put(part, new VanillaPart(part, allocationTracker));
            }
        }
    }

    // Object accessible by scripts, interface to model part reading/writing.
    public class VanillaPart implements RiggedHierarchy<VanillaPart> {

        // Vanilla ModelPart to which this is linked
        public final ModelPart part;
        // Script-provided values for how to transform the vanilla part
        public final PartTransform figuraTransform;

        public VanillaPart(ModelPart part, @Nullable AllocationTracker allocationTracker) throws AvatarError {
            this.part = part;
            this.figuraTransform = new PartTransform(allocationTracker);
        }


        // Script-provided booleans saying whether to cancel each vanilla transform phase:
        public boolean cancelVanillaOrigin, cancelVanillaRotation, cancelVanillaScale;

        // Stored transform values from when the part last rendered:
        public final Vector3f
                storedVanillaOrigin = new Vector3f(),
                storedVanillaRotation = new Vector3f(), // Radians
                storedVanillaScale = new Vector3f(1f),
                storedVanillaPosition = new Vector3f();

        // Callbacks which run when the minecraft part is rendered
        public final ArrayList<ScriptCallback<CallbackItem.Unit, CallbackItem.Unit>> vanillaRenderCallbacks = new ArrayList<>(0);

        // Getter for component
        public VanillaRendering getComponent() {
            return VanillaRendering.this;
        }

        // Implement RiggedHierarchy
        @Override
        public PartTransform getTransform() {
            return figuraTransform;
        }

        @Override
        public @Nullable VanillaPart getChildByName(String name) {
            ModelPart vanillaChild = part.children.get(name);
            if (vanillaChild == null) return null;
            return VanillaRendering.this.partMap.get(vanillaChild);
        }
    }

}
