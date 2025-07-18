package org.figuramc.figura.avatars.components;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRenderer;
import org.figuramc.figura.avatars.AvatarComponent;
import org.figuramc.figura.model.part.PartTransform;
import org.figuramc.figura.model.part.PartLike;
import org.figuramc.figura.script_hooks.callback.ScriptCallback;
import org.figuramc.figura.script_hooks.mem_count.MarkedObjectBase;
import org.figuramc.figura.script_hooks.mem_count.MemoryCounter;
import org.figuramc.figura.vanillamodel.ModelNames;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
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
    public VanillaRendering(EntityRenderer<?, ?> entityRenderer) {
        this.entityRenderer = entityRenderer;
        ModelNames.getModelsByName(entityRenderer).values().stream()
                .flatMap(model -> model.root().getAllParts())
                .forEach(part -> partMap.put(part, new VanillaPart(part)));
    }

    // Object accessible by scripts, interface to model part reading/writing.
    public class VanillaPart extends MarkedObjectBase implements PartLike<VanillaPart> {

        // ModelPart to which this is linked
        public final ModelPart part;

        public VanillaPart(ModelPart part) { this.part = part; }

        // Script-provided values for how to transform the vanilla part
        public final PartTransform figuraTransform = new PartTransform();
        // Script-provided booleans saying whether to cancel each vanilla transform phase:
        public boolean cancelVanillaOrigin, cancelVanillaRotation, cancelVanillaScale;

        // Stored transform values from when the part last rendered:
        public final Vector3f
                storedVanillaOrigin = new Vector3f(),
                storedVanillaRotation = new Vector3f(), // Radians
                storedVanillaScale = new Vector3f(1f),
                storedVanillaPosition = new Vector3f();

        // Callbacks which run when the minecraft part is rendered
        public final ArrayList<ScriptCallback> vanillaRenderCallbacks = new ArrayList<>(0);

        // Getter for component
        public VanillaRendering getComponent() {
            return VanillaRendering.this;
        }

        // Implement transformable
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

        // Since ScriptVanillaPart objects just point to Minecraft's ModelPart,
        // their memory usage is constant aside from the callbacks.
        @Override
        protected long traceNoMark(MemoryCounter counter, int depth) {
            counter.trace(figuraTransform, depth);
            for (ScriptCallback callback : vanillaRenderCallbacks)
                counter.trace(callback, depth);
            return 60 + vanillaRenderCallbacks.size() * POINTER_SIZE;
        }
    }

}
