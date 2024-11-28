package org.figuramc.figura.ducks.client;

// Accessor to grab custom fields in CubeTrackingMixin
public interface CubeTrackingAccess {
    int figura$getTextureWidth();
    int figura$getTextureHeight();
    void figura$setTextureWidth(int width);
    void figura$setTextureHeight(int height);
}
