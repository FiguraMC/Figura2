package org.figuramc.figura.mixin.client.vanilla_parts.tracking;

import org.figuramc.figura.ducks.client.ModelPartTrackingAccess;
import net.minecraft.client.model.geom.ModelPart;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

@Mixin(ModelPart.class)
public class ModelPartTrackingMixin implements ModelPartTrackingAccess {

    /**
     * Adds "name" and "parent" fields to the model part. We attempt to
     * set these, whenever possible, when creating the model part.
     */
    @Unique public @Nullable String modelPartName;
    // Alias set by ModelPartTracker
    @Unique public @Nullable String alias;
    @Unique public @Nullable ModelPart parent;

    // Inject into constructor:
    @SuppressWarnings({"UnreachableCode", "DataFlowIssue"})
    @Inject(method = "<init>", at = @At("RETURN"))
    private void setNameFieldsOfChildren(List<ModelPart.Cube> cubes, Map<String, ModelPart> children, CallbackInfo ci) {
        // Iterate over children and set their name/parent fields.
        for (Map.Entry<String, ModelPart> childEntry : children.entrySet()) {
            // Set name of child to the entry's key:
            ((ModelPartTrackingMixin) (Object) childEntry.getValue()).modelPartName = childEntry.getKey();
            // Set parent of child to this:
            if (childEntry.getKey() != null) // This will always be true unless someone does something immensely cursed
                ((ModelPartTrackingMixin) (Object) childEntry.getValue()).parent = (ModelPart) (Object) this;
        }
    }

    @Override public void figura$setName(String name) { modelPartName = name; }
    @Override public @Nullable String figura$getName() { return modelPartName; }
    @Override public String figura$getAlias() { return alias; }
    @Override public void figura$setAlias(String alias) { this.alias = alias; }
    @Override public @Nullable ModelPart figura$getParent() { return parent; }

}
