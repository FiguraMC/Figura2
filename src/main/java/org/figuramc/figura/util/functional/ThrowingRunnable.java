package org.figuramc.figura.util.functional;

@FunctionalInterface
public interface ThrowingRunnable<E extends Throwable> {
    void run() throws E;
}
