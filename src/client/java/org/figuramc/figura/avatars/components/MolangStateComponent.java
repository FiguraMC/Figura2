package org.figuramc.figura.avatars.components;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.figuramc.figura.animation.AnimationInstance;
import org.figuramc.figura.avatars.AvatarComponent;
import org.figuramc.figura.util.NullEmptyStack;
import org.figuramc.figura.util.functional.ThrowingSupplier;
import org.jetbrains.annotations.Nullable;

import java.util.Stack;

/**
 * Item used as the Actor for an avatar's molang states.
 * All queries supported by Figura's molang are implemented on it.
 */
public class MolangStateComponent implements AvatarComponent<MolangStateComponent> {

    public static final Type<MolangStateComponent> TYPE = new Type<>(EntityUser.TYPE); // Depends on entity user being updated before it
    public Type<MolangStateComponent> getType() { return TYPE; }

    // The entity which has the avatar equipped, if any
    private final @Nullable EntityUser entityUser;
    // The stack of currently used animation instances. Normally only 1 slot is used, unless nested calls happen
    private final Stack<@Nullable AnimationInstance> animInstances = new NullEmptyStack<>();

    public MolangStateComponent(@Nullable EntityUser entityUser) {
        this.entityUser = entityUser;
    }

    public void pushAnim(AnimationInstance instance) { this.animInstances.push(instance); }
    public void popAnim() { this.animInstances.pop(); }

    // Run a task with the given animation instance
    public <T, E extends Throwable> T withAnim(AnimationInstance instance, ThrowingSupplier<T, E> task) throws E {
        animInstances.push(instance);
        try {
            return task.get();
        } finally {
            animInstances.pop();
        }
    }


    // Helper methods.

    // Get tick delta, or return -1 if error occurs
    private @Nullable Entity getEntity() {
        if (entityUser == null) return null;
        return entityUser.getEntity();
    }
    private @Nullable LivingEntity getLivingEntity() {
        Entity e = getEntity(); if (e == null) return null;
        if (!(e instanceof LivingEntity le)) return null;
        return le;
    }
    private @Nullable Player getClientPlayer() {
        return Minecraft.getInstance().player;
    }
    private float getDelta(Entity e) {
        Level level = e.level(); if (level == null) return -1;
        boolean ignoreFreeze = !level.tickRateManager().isEntityFrozen(e);
        return Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(ignoreFreeze);
    }

    // Methods implementing some queries (TODO consider adding more?)

    // Commonly used ones
    public float anim_time() {
        AnimationInstance a = animInstances.peek(); if (a == null) return 0;
        return a.getTime();
    }
    public float life_time() {
        Entity e = getEntity(); if (e == null) return 0;
        float delta = getDelta(e); if (delta == -1) return e.tickCount / 20f; // Convert to seconds
        return (e.tickCount + delta) / 20f; // Divide by 20 to convert to seconds
    }
    public float time_stamp() {
        Level level = Minecraft.getInstance().level; if (level == null) return 0;
        return (float) level.getGameTime();
    }
    // There's some cursed stuff with multipliers for distance moved, molang expects this to return the # of blocks moved...
    public float modified_distance_moved() {
        LivingEntity e = getLivingEntity(); if (e == null) return 0;
        // We multiply times five because the units of position in WalkAnimation are something like,
        // "average blocks moved per tick * 4, affected by easings and baby-ness and stuff".
        // This is overridden by Camels/Frogs/Creakings, so
        // TODO try accounting for those maybe? Maybe re-implement the whole
        //      thing ourselves with mixin instead of using WalkAnimation?
        float delta = getDelta(e); if (delta == -1) return e.walkAnimation.position() * 5.0f;
        return e.walkAnimation.position(delta) * 5.0f;
    }
    // I don't think this technically takes baby-ness into account in the JE codebase
    // like the molang spec claims it should, but I don't think it's a big deal since this is very much an edge case.
    public float modified_move_speed() {
        LivingEntity e = getLivingEntity(); if (e == null) return 0;
        // Again, units are something jank like "blocks per tick * 4" depending on LivingEntity subclass, so mul by 5.
        float delta = getDelta(e); if (delta == -1) return e.walkAnimation.speed() * 5.0f;
        return e.walkAnimation.speed(delta) * 5.0f;
    }
    public float death_ticks() {
        LivingEntity e = getLivingEntity(); if (e == null) return 0;
        float delta = getDelta(e); if (delta == -1) return e.deathTime;
        return e.deathTime + delta;
    }


//    public float blocking() { LivingEntity e = getLivingEntity(); if (e == null) return 0; return e.isBlocking() ? 1 : 0; }
//    public float body_y_rotation() {
//        Entity e = getEntity(); if (e == null) return 0;
//        float delta = getDelta(e); if (delta == -1) return 0;
//        return e.getPreciseBodyRotation(delta);
//    }
//    public float camera_rotation(float axis) {
//        return switch ((int) axis) {
//            case 0 -> Minecraft.getInstance().gameRenderer.getMainCamera().getXRot();
//            case 1 -> Minecraft.getInstance().gameRenderer.getMainCamera().getYRot();
//            default -> 0.0f;
//        };
//    }
//    public float cardinal_facing() { Entity e = getEntity(); if (e == null) return 6; return e.getNearestViewDirection().ordinal(); }
//    public float cardinal_facing_2d() { Entity e = getEntity(); if (e == null) return 6; return Direction.fromYRot(e.getYRot()).ordinal(); }
//    public float cardinal_player_facing() { Player e = getClientPlayer(); if (e == null) return 6; return e.getNearestViewDirection().ordinal(); }
//    public float client_max_render_distance() { return Minecraft.getInstance().options.renderDistance().get(); }
//    public float day() { Level level = Minecraft.getInstance().level; if (level == null) return 0; return (float) (level.getDayTime() / 24000L); }



}
