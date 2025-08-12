package org.figuramc.figura.util.functional;

@FunctionalInterface
public interface BiThrowingSupplier<T, E1 extends Throwable, E2 extends Throwable> {
    T get() throws E1, E2;
}