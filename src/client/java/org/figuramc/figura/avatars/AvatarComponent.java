package org.figuramc.figura.avatars;

import org.figuramc.figura.util.enumlike.EnumLike;

import java.util.Arrays;

// Self should just be the class itself that's implementing the interface.
public interface AvatarComponent<Self extends AvatarComponent<Self>> {

    // Each subclass of AvatarComponent should simply write the following two lines to support the ID system:
    // public static final Type<ThisClass> TYPE = new Type<>(Types I depend on);
    // public Type<ThisClass> getType() { return TYPE; }
    // Note that this falls apart if there's a dependency loop (A depends on B depends on A), so don't create such loops!

    // AvatarComponent.Type is enum-like for efficient access, and ability for other mods to add more.
    // The generic makes the Avatar.getComponent() method more convenient.
    final class Type<X extends AvatarComponent<X>> extends EnumLike {
        // By passing the possible dependencies as arguments, we ensure they're initialized first, and therefore have smaller IDs.
        public Type(Type<?>... possibleDependencies) {
            // If a value here is null, then you have a dependency cycle.
            assert Arrays.stream(possibleDependencies).allMatch(dep -> dep.id < this.id);
        }
    }

    // Get the type. This should just return a static variable in the class as defined above.
    Type<Self> getType();

    // Once loading is complete, this is run on the main/render thread, to instantiate the avatar.
    default void mainThreadInitialize(Avatar<?> avatar) throws AvatarError, Throwable { }

    // Run on Avatar cleanup. Should eventually destroy any native resources that won't be GC'ed and prevent a memory leak.
    default void destroy() { }

    // Runs when the Avatar is ticked.
    // Dependencies will always run before this, as declared in createId().
    default void tick(Avatar<?> self) { }

}
