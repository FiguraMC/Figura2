package org.figuramc.figura.util.exception;

@FunctionalInterface
public interface BiThrowingFunction<T, R, E1 extends Throwable, E2 extends Throwable> {
    public R apply(T t) throws E1, E2;
}
