package org.figuramc.figura.ducks.client;

import org.joml.Vector2f;
import org.joml.Vector3f;

// Accessor to grab custom fields in CubeTrackingMixin.
// Texture size values should always be integers for vanilla mobs.
public interface CubeTrackingAccess {
    // Getters also work as setters because mutable
    Vector2f figura$getTextureSize();
    Vector3f figura$getInflate();
    // Getter for mirrored
    boolean figura$getMirrored();
}
