package org.figuramc.figura.avatars;

import com.mojang.blaze3d.systems.RenderSystem;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.data.AvatarMaterials;
import org.figuramc.figura.manage.AvatarLoadingException;
import org.figuramc.figura.script_hooks.mem_count.AllocationTracker;
import org.figuramc.figura.script_hooks.mem_count.MemoryCountable;
import org.figuramc.figura.util.ChatUtils;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.Map;

public class Avatar<K> {

    public final K user; // The key which accesses this Avatar in its corresponding AvatarSubManager<K>
    public final AvatarMaterials.MetadataMaterials metadata;
    private final AvatarComponent[] components;
    private final Map<Class<?>, AvatarComponent> componentsByType = new IdentityHashMap<>();

    private @Nullable Throwable error;
    private boolean isErrored;

    // Memory tracker. Null indicates memory shouldn't be tracked,
    // making it faster in cases where full permission is granted.
    private @Nullable AllocationTracker allocationTracker;

    public Avatar(K user, AvatarMaterials materials, AvatarComponent... components) throws AvatarLoadingException {
        // Init fields
        this.user = user;
        this.metadata = materials.metadata();
        this.components = components;
        for (AvatarComponent component : components) componentsByType.put(component.getClass(), component);

        // Initialize each component
        for (AvatarComponent component : components) component.initialize(materials, this);
    }

    @SuppressWarnings("unchecked")
    public <T extends AvatarComponent> @Nullable T getComponent(Class<T> type) {
        // Errored avatars act like they have no components.
        // All mixins and such will look for a component on a given avatar and try to use it;
        // but they will be unable to get this component if the avatar is errored.
        if (isErrored) return null;
        return (T) componentsByType.get(type);
    }

    // Ensure that an instance of "before" appears earlier than any instance of "self", and return that instance.
    public <T extends AvatarComponent> T assertDependency(Class<T> before, Class<? extends AvatarComponent> self) {
        @Nullable T maybe = optionalDependency(before, self);
        if (maybe == null) throw new IllegalStateException("Invalid state in Avatar construction - component \"" + self.getSimpleName() + "\" depends on component \"" + before.getSimpleName() + "\" before it!");
        return maybe;
    }

    // If an instance of "before" appears earlier than any instance of "self", return it.
    // If one appears after self, error.
    // If none exists at all, return null.
    @SuppressWarnings("unchecked")
    public <T extends AvatarComponent> @Nullable T optionalDependency(Class<T> before, Class<? extends AvatarComponent> self) {
        boolean foundSelf = false;
        for (AvatarComponent component : components) {
            if (before.isInstance(component)) {
                if (foundSelf) throw new IllegalStateException("Invalid state in Avatar construction - component \"" + self.getSimpleName() + "\" depends on component \"" + before.getSimpleName() + "\" before it!");
                return (T) component;
            }
            if (component.getClass() == self) {
                foundSelf = true;
            }
        }
        return null;
    }

    // Track a given object as a memory root.
    public void addMemoryRoot(MemoryCountable root) {
        if (allocationTracker != null)
            allocationTracker.addRoot(root);
    }
    public @Nullable AllocationTracker getAllocationTracker() {
        return allocationTracker;
    }

    // Error out the avatar with the given message and reason.
    // The reason will be shown to chat (TODO only if host), and printed in more detail in the console.
    public void error(MutableComponent message, Throwable reason) {
        // Mark as errored with the given reason
        isErrored = true;
        error = reason;
        // Notify components:
        for (AvatarComponent component : components)
            component.onError(reason);
        // Report the error to user
        ChatUtils.reportErrorWithReason(message, reason, false);
        FiguraMod.LOGGER.error("Avatar with user (" + user + ") encountered an error: ", reason);
    }

    // We want to use this function only when necessary; for most usages, the fact
    // that an errored avatar acts like it has no components is good enough.
    public boolean isErrored() {
        return isErrored;
    }

    // Run on cleanup. Should be used to prevent memory leaks.
    public void destroy() {
        for (AvatarComponent component : components)
            component.destroy();
    }

    // Should be run on the main thread, which is why it's not part of the constructor!
    public void mainThreadInitialize() {
        if (!RenderSystem.isOnRenderThreadOrInit())
            throw new IllegalStateException("Function should only be called on render thread or init! Bug in Figura!");
        // Run the components' main thread init functions.
        for (AvatarComponent component : components)
            if (component.mainThreadInitialize()) break;
    }

    // Runs each tick. Just ticks each component in the order they were added to the Avatar.
    public void tick() {
        if (isErrored) return; // Don't tick if errored
        for (AvatarComponent component : components)
            if (component.tick()) break;
    }

    // Whether the Avatar is completely, fully ready for usage.
    // This cannot usually be the case immediately after the constructor because
    // certain asynchronous tasks need to be done. For example, uploading textures.
    public boolean isReadyAsync() {
        // Avatar is ready async if all of its components are ready async.
        for (AvatarComponent component : components)
            if (!component.isReadyAsync())
                return false;
        return true;
    }


}
