package org.figuramc.figura.avatars;

import org.figuramc.figura.data.AvatarMaterials;
import org.figuramc.figura.manage.AvatarLoadingException;

public interface AvatarComponent {

    // This is called upon creation of an Avatar.
    // To add script APIs, depend on the `Scripts` component and add them here.
    void initialize(AvatarMaterials materials, Avatar<?> self) throws AvatarLoadingException;

    // Once loading is entirely complete, this is run on the main/render thread, to instantiate the avatar.
    // Return true if there was an error and we should cancel further operations.
    default boolean mainThreadInitialize() { return false; }

    // Returns whether this component is completely ready, including async operations.
    // Won't ever be called until after "initialize" returns.
    default boolean isReadyAsync() { return true; }

    // Run on Avatar cleanup. Should destroy any native resources that won't be GC'ed
    // and prevent a memory leak.
    default void destroy() {}

    // Run when there's an error
    default void onError(Throwable reason) {}

    // Runs when the Avatar is ticked. Runs in the order of components added to the Avatar.
    // Return true if there was an error and we should cancel further operations.
    default boolean tick() { return false; }

}
