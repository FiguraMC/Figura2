package org.figuramc.figura.avatars;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.MultiBufferSource;
import org.figuramc.figura.manage.AvatarLoadingException;
import org.figuramc.figura.model.renderers.FiguraModelPartRenderer;
import org.figuramc.figura.script_hooks.ScriptError;
import org.figuramc.figura.script_hooks.mem_count.AllocationTracker;
import org.figuramc.figura.script_hooks.mem_count.MemoryCountable;
import org.figuramc.figura.util.ErrorReporting;
import org.figuramc.figura.util.FiguraTransformStack;
import org.figuramc.figura.util.enumlike.IdMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class Avatar<K> {

    public final K user; // The key which accesses this Avatar in its corresponding AvatarSubManager<K>
    private final IdMap<AvatarComponent.Type, AvatarComponent<?>> components; // Components, where ID -> component if present, null if not. Requires some unchecked sillies because of generics.
    private final @NotNull AvatarComponent<?>[] presentComponents; // Only the non-null components, used for iteration

    private @Nullable AvatarError error;

    // Memory tracker. Null indicates memory shouldn't be tracked,
    // making it faster in cases where full permission is granted.
    private @Nullable AllocationTracker allocationTracker;

    public Avatar(K user, AvatarModules modules, Collection<AvatarComponent<?>> componentSet) throws AvatarLoadingException {
        // Init fields
        this.user = user;
        // Add components to ID map
        this.components = new IdMap<>(AvatarComponent.Type.class);
        componentSet.forEach(component -> components.put(component.getType(), component));
        // Create presentComponents array by removing null elements for faster iteration.
        this.presentComponents = this.components.values().stream().filter(Objects::nonNull).toArray(AvatarComponent[]::new);
        // Initialize each component in order
        for (AvatarComponent<?> component : presentComponents) {
            component.initialize(modules, this);
        }
    }


    // Access this using the static field <subclass of AvatarComponent>.TYPE.
    // This field should exist if they followed the implementation instructions in AvatarComponent correctly.
    @SuppressWarnings("unchecked")
    public <T extends AvatarComponent<T>> @Nullable T getComponent(AvatarComponent.Type<T> type) {
        // Errored avatars act like they have no components.
        // All mixins and such will look for a component on a given avatar and try to use it;
        // but they will be unable to get this component if the avatar is errored.
        if (isErrored()) return null;
        return (T) components.get(type);
    }

    // Track a given object as a memory root.
    public void addMemoryRoot(MemoryCountable root) {
        if (allocationTracker != null)
            allocationTracker.addRoot(root);
    }
    public @Nullable AllocationTracker getAllocationTracker() {
        return allocationTracker;
    }

    public void error(AvatarError reason) {
        // Mark as errored
        this.error = reason;
        // Notify components:
        for (AvatarComponent<?> component : presentComponents)
            component.onError(reason);
        // Report the error to user
        ErrorReporting.avatarRuntimeError(reason);
    }

    // We want to use this function only when strictly necessary; for most usages, the fact
    // that an errored avatar acts like it has no components is good enough.
    public boolean isErrored() {
        return error != null;
    }

    // Run on cleanup. Should be used to prevent memory leaks.
    public void destroy() {
        for (AvatarComponent<?> component : presentComponents)
            component.destroy();
    }

    // Should be run on the main thread, which is why it's not part of the constructor!
    public void mainThreadInitialize() {
        if (!RenderSystem.isOnRenderThread())
            throw new IllegalStateException("Function should only be called on render thread! Bug in Figura!");
        // Run the components' main thread init functions.
        for (AvatarComponent<?> component : presentComponents) {
            component.mainThreadInitialize();
            if (isErrored()) break;
        }
    }

    // Runs each tick. Just ticks each component in the order they were added to the Avatar.
    public void tick() {
        if (isErrored()) return; // Don't tick if errored
        for (AvatarComponent<?> component : presentComponents) {
            component.tick();
            if (isErrored()) break;
        }
    }

    // Various helper methods

    // Attempt to render the model part, and error the avatar if it fails.
    public void tryRenderModelPart(FiguraModelPartRenderer partRenderer, MultiBufferSource bufferSource, FiguraTransformStack transformStack, float tickDelta, int light, int overlay) {
        if (isErrored()) return;
        try {
            partRenderer.render(bufferSource, transformStack, tickDelta, light, overlay);
        } catch (ScriptError ex) {
            error(new AvatarError("figura.error.runtime.model_part.callback", ex, true));
        } catch (StackOverflowError ex) {
            error(new AvatarError("figura.error.runtime.model_part.stack_overflow", ex, false));
        } catch (Throwable other) {
            error(new AvatarError("figura.error.runtime.model_part.unexpected", other, true));
        }
    }

}
