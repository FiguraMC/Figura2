package org.figuramc.figura.animation;

import org.figuramc.figura.model.part.RiggedHierarchy;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks current time, speed, etc. of an animation occurring.
 * Created by binding an Animation to a RiggedHierarchy.
 */
public class AnimationInstance {

    // Time in seconds since the start of the animation
    private float time = 0.0f;
    // Strength multiplier for effectiveness of the animation
    private float strength = 1.0f;

    // List of animators, to mark as dirty when this changes
    private final List<Animator> animators;

    // Bind the Animation to a RiggedHierarchy, within an Animations component
    public AnimationInstance(Animation animation, RiggedHierarchy<?> root) {
        animators = new ArrayList<>();
        for (var entry : animation.keyframesByPartPath.entrySet()) {
            String partPath = entry.getKey();
            RiggedHierarchy<?> descendant = root.getDescendantWithPath(partPath);
            if (descendant == null) continue; // TODO some kind of warning when the instance doesn't fully bind?
            Animation.TransformKeyframes keyframes = entry.getValue();
            Animator animator = new Animator(this, keyframes);
            descendant.getTransform().addAnimator(animator);
            animators.add(animator);
        }
    }

    // Mark animators as dirty, so they update when called upon
    private void markDirty() {
        for (Animator animator : animators)
            animator.markDirty();
    }

    // Get/set time
    public float getTime() {
        return time;
    }
    public void setTime(float time) {
        if (this.time != time) {
            this.time = time;
            markDirty();
        }
    }

    // Get/set strength
    public float getStrength() {
        return strength;
    }
    public void setStrength(float strength) {
        if (this.strength != strength) {
            this.strength = strength;
            markDirty();
        }
    }

}
