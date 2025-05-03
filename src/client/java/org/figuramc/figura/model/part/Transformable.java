package org.figuramc.figura.model.part;

/**
 * Simply anything with a transform.
 * This can allow duplicating functionality between FiguraModelPart and VanillaModelPart,
 * and perhaps other objects later.
 */
public interface Transformable {
    PartTransform getTransform();
}
