package org.figuramc.figura.avatars;

import org.figuramc.figura.manage.AvatarLoadingException;

import java.util.concurrent.atomic.AtomicInteger;

public interface AvatarComponent {

    // Each subclass of AvatarComponent should simply write the following two lines to support the ID system:
    // public static final int ID = createId(<component classes I depend on>);
    // public int getId() { return ID; }
    // Note that this falls apart if there's a dependency loop (A depends on B depends on A), so don't create such loops!
    // Also don't access the NEXT_ID field, its usage is internal to make createId() work properly.
    AtomicInteger NEXT_ID = new AtomicInteger();
    @SafeVarargs
    static int createId(Class<? extends AvatarComponent>... dependsOn) {
        // Because the dependents were passed in, we know that their classes have already been loaded.
        // Therefore, their IDs are guaranteed to be less than this component's ID, so they will run before this one!
        return NEXT_ID.getAndIncrement();
    }
    int getId();

    // This is called upon creation of an Avatar.
    // This method runs on an off-thread, so do not use anything requiring the main threads without appropriate wrapping.
    default void initialize(AvatarModules modules, Avatar<?> self) throws AvatarLoadingException {}

    // Once loading is complete, this is run on the main/render thread, to instantiate the avatar.
    // Return true if there was an error and we should cancel further operations.
    default boolean mainThreadInitialize() { return false; }

    // Run on Avatar cleanup. Should eventually destroy any native resources that won't be GC'ed and prevent a memory leak.
    default void destroy() { }

    // Run when there's an error
    default void onError(Throwable reason) {}

    // Runs when the Avatar is ticked.
    // Dependencies will always run before this, as declared in createId().
    // Return true if there was an error and we should cancel further operations.
    default boolean tick() { return false; }

}
