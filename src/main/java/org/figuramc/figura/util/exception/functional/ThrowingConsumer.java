package org.figuramc.figura.util.exception.functional;

@FunctionalInterface
public interface ThrowingConsumer<T, E extends Throwable> {
    void accept(T value) throws E;
}