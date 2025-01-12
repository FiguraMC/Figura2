package org.figuramc.figura.ducks.client;

import net.minecraft.client.model.geom.ModelPart;
import org.jetbrains.annotations.Nullable;

// Accessor to grab custom fields in ModelPartTrackingMixin
public interface ModelPartTrackingAccess {
    @Nullable String figura$getName();
    void figura$setName(String name);

    @Nullable String figura$getAlias();
    void figura$setAlias(String alias);

    @Nullable ModelPart figura$getParent();
}
