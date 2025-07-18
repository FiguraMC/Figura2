package org.figuramc.figura.animation;

import org.figuramc.figura.data.ModuleMaterials;
import org.figuramc.figura.util.functional.FloatSupplier;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.List;

public class Vec3Keyframe implements Comparable<Vec3Keyframe> {

    // Time at which this keyframe occurs, in seconds.
    private final float time;
    private final FloatSupplier x, y, z;
    private final Interpolation interpolation;

    public Vec3Keyframe(ModuleMaterials.TransformKeyframeMaterials materials) {
        this(
                materials.time(),
                new ConstantFloat(Float.parseFloat(materials.x())), // TODO molang stuff
                new ConstantFloat(Float.parseFloat(materials.y())),
                new ConstantFloat(Float.parseFloat(materials.z())),
                switch (materials.interpolation()) {
                    case ModuleMaterials.InterpolationMaterials.Linear __ -> Interpolation.LINEAR;
                    case ModuleMaterials.InterpolationMaterials.CatmullRom __ -> Interpolation.CATMULLROM;
                    case ModuleMaterials.InterpolationMaterials.Step __ -> Interpolation.STEP;
                    case ModuleMaterials.InterpolationMaterials.Bezier bezier -> throw new UnsupportedOperationException("Bezier interpolation is TODO");
                }
        );
    }

    public Vec3Keyframe(float time, FloatSupplier x, FloatSupplier y, FloatSupplier z, Interpolation interpolation) {
        this.time = time;
        this.x = x;
        this.y = y;
        this.z = z;
        this.interpolation = interpolation;
    }

    // Test constructor
    public Vec3Keyframe(float time, float x, float y, float z) {
        this(time, new ConstantFloat(x), new ConstantFloat(y), new ConstantFloat(z), Interpolation.LINEAR);
    }

    // Evaluate the keyframe into the vector, also return the vector for chaining
    public Vector3f evaluateInto(Vector3f output) {
        return output.set(x.getAsFloat(), y.getAsFloat(), z.getAsFloat());
    }


    // Static helper to convert a sorted list of keyframes + a time into a vec3, also return vector for chaining
    public static Vector3f evaluateTimelineInto(Vector3f output, List<Vec3Keyframe> timeline, float time) {
        if (timeline.isEmpty()) throw new IllegalArgumentException("Internal Figura error - attempt to interpolate animation timeline with no keyframes");

        // Binary search:
        int low = 0, high = timeline.size();
        while (low < high) {
            int mid = low + (high - low) / 2;
            if (timeline.get(mid).time < time) low = mid + 1;
            else high = mid;
        }

        // low-1 is the index of the current/prev keyframe.
        // low is the index of the next keyframe.

        // If we're not between two keyframes, snap to the closer keyframe
        if (low == 0) return timeline.getFirst().evaluateInto(output);
        if (low == timeline.size()) return timeline.getLast().evaluateInto(output);

        // Otherwise, perform interpolation on previous and next keyframes
        Vec3Keyframe prev = timeline.get(low - 1);
        // If interpolation is STEP, return prev directly
        if (prev.interpolation == Interpolation.STEP) return prev.evaluateInto(output);
        // Otherwise, fetch next and alpha so we can interpolate!
        Vec3Keyframe next = timeline.get(low);
        float alpha = (time - prev.time) / (next.time - prev.time);

        if (prev.interpolation == Interpolation.LINEAR) {
            float x0 = prev.x.getAsFloat();
            float y0 = prev.y.getAsFloat();
            float z0 = prev.z.getAsFloat();
            return output.set(
                    org.joml.Math.fma(next.x.getAsFloat() - x0, alpha, x0),
                    org.joml.Math.fma(next.y.getAsFloat() - y0, alpha, y0),
                    org.joml.Math.fma(next.z.getAsFloat() - z0, alpha, z0)
            );
        } else if (prev.interpolation == Interpolation.CATMULLROM) {
            float x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3;
            if (low >= 2) {
                Vec3Keyframe prevPrev = timeline.get(low - 2);
                x0 = prevPrev.x.getAsFloat();
                y0 = prevPrev.y.getAsFloat();
                z0 = prevPrev.z.getAsFloat();
                x1 = prev.x.getAsFloat();
                y1 = prev.y.getAsFloat();
                z1 = prev.z.getAsFloat();
            } else {
                x0 = x1 = prev.x.getAsFloat();
                y0 = y1 = prev.y.getAsFloat();
                z0 = z1 = prev.z.getAsFloat();
            }
            x2 = next.x.getAsFloat();
            y2 = next.y.getAsFloat();
            z2 = next.z.getAsFloat();
            if (low + 1 < timeline.size()) {
                Vec3Keyframe nextNext = timeline.get(low + 1);
                x3 = nextNext.x.getAsFloat();
                y3 = nextNext.y.getAsFloat();
                z3 = nextNext.z.getAsFloat();
            } else { x3 = x2; y3 = y2; z3 = z2; }

            return output.set(
                    catmullrom(alpha, x0, x1, x2, x3),
                    catmullrom(alpha, y0, y1, y2, y3),
                    catmullrom(alpha, z0, z1, z2, z3)
            );
        }
        throw new IllegalArgumentException("Unexpected interpolation");
    }

    private static float catmullrom(float t, float p0, float p1, float p2, float p3) {
        float v0 = (p2 - p0) * 0.5f;
        float v1 = (p3 - p1) * 0.5f;
        float t2 = t * t;
        float d = p2 - p1;
        return (v0 + v1 - 2.0f * d) * t2 * t + (3.0f * d - 2.0f * v0 - v1) * t2 + v0 * t + p1;
    }

    public enum Interpolation {
        LINEAR,
        STEP,
        CATMULLROM,
        // TODO bezier is weird since it has additional data, so we'll get to that later :P
    }

    // Make them sortable by time in a list
    @Override
    public int compareTo(@NotNull Vec3Keyframe o) {
        return Float.compare(time, o.time);
    }

    // Cursed field name lol but it works
    public record ConstantFloat(float getAsFloat) implements FloatSupplier {}
}
