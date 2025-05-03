package org.figuramc.figura.ducks.client;

import net.minecraft.client.model.geom.ModelPart;
import org.jetbrains.annotations.Nullable;

// Accessor to grab custom fields in ModelPartTrackingMixin
public interface ModelPartTrackingAccess {
    String figura$getName();

    @Nullable ModelPart figura$getParent();
}
