package org.figuramc.figura.util.exception;

@FunctionalInterface
public interface ThrowingBiFunction<T1, T2, R, E extends Throwable> {
    public R apply(T1 t1, T2 t2) throws E;
}
