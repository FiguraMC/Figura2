package org.figuramc.figura.animation;

import org.figuramc.figura.data.ModuleMaterials;
import org.figuramc.figura.util.ListUtils;
import org.figuramc.figura.util.MapUtils;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * An Animation is a mapping from (part path) to (keyframe info).
 * An Animation can be BOUND to a RiggedHierarchy to create an AnimationInstance.
 * An AnimationInstance tracks a timer, speed, etc, and updates its Animators.
 * An Animator is placed on a PartTransform, and modifies it.
 * - When an Animator is updated, mark transforms' fields as dirty!
 */
public class Animation {

    public final Map<String, TransformKeyframes> keyframesByPartPath;

    // Construct an animation from materials
    public Animation(ModuleMaterials.AnimationMaterials materials) {
        keyframesByPartPath = MapUtils.mapValues(materials.transformKeyframes(), transformMats -> new TransformKeyframes(
                ListUtils.map(transformMats.origin(), Vec3Keyframe::new),
                ListUtils.map(transformMats.rotation(), Vec3Keyframe::new),
                ListUtils.map(transformMats.scale(), Vec3Keyframe::new)
        ));
        // TODO length / loop mode stuff
        // TODO script keyframes
    }

    public Animation(Map<String, TransformKeyframes> keyframesByPartPath) {
        this.keyframesByPartPath = keyframesByPartPath;
    }

    // Sorted lists. If a field is null, then that channel is unaffected
    public record TransformKeyframes(
            @Nullable List<Vec3Keyframe> origin,
            @Nullable List<Vec3Keyframe> rotation,
            @Nullable List<Vec3Keyframe> scale
    ) {}

}
