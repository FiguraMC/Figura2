package org.figuramc.figura.model.part;

import org.jetbrains.annotations.Nullable;

/**
 * Something that acts somewhat like a model part, including FiguraModelPart and VanillaModel.Part.
 * This allows for code re-use.
 * Operations:
 * - The thing must have a transform
 * - It must optionally be able to get its descendants by name (This is for binding animations to it)
 * @param <T> Must be this type itself!
 */
public interface PartLike<T extends PartLike<T>> {
    // The object has a transform
    PartTransform getTransform();
    // Get child with given name, if any
    @Nullable T getChildByName(String name);

    // Look for a descendant with the given path, separated with slashes.
    // If the path is the empty string "", should return this.
    // If there is no part at the given path, return null.
    @SuppressWarnings("unchecked") // Cast will succeed if the implementor successfully set T = implementor
    default @Nullable T getDescendantWithPath(String path) {
        PartLike<T> part = this;
        // TODO maybe optimize this?
        for (String pathElement : path.split("/")) {
            part = part.getChildByName(pathElement);
            if (part == null) return null;
        }
        return (T) part;
    }
}
