package org.figuramc.figura.util.exception;

import org.figuramc.figura.util.exception.functional.ThrowingFunction;
import org.figuramc.figura.util.exception.functional.ThrowingSupplier;

import java.util.function.Function;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class ExceptionUtils {

    // Wrap checked exceptions in the given runtime exception wrapper.
    // Runtime exceptions are not wrapped.
    public static <T> Supplier<T> wrapChecked(ThrowingSupplier<T, ?> throwingSupplier, Function<Throwable, ? extends RuntimeException> runtimeWrapper) {
        return () -> {
            try {
                return throwingSupplier.get();
            } catch (Throwable e) {
                if (e instanceof RuntimeException re)
                    throw re;
                throw runtimeWrapper.apply(e);
            }
        };
    }

    public static <T, R> Function<T, R> wrapChecked(ThrowingFunction<T, R, ?> throwingFunction, Function<Throwable, ? extends RuntimeException> runtimeWrapper) {
        return t -> {
            try {
                return throwingFunction.apply(t);
            } catch (Throwable e) {
                if (e instanceof RuntimeException re)
                    throw re;
                throw runtimeWrapper.apply(e);
            }
        };
    }

    public static <T, R, E extends Throwable> ThrowingFunction<T, R, E> wrapAny(ThrowingFunction<T, R, ?> throwingFunction, Function<Throwable, E> runtimeWrapper) {
        return t -> {
            try {
                return throwingFunction.apply(t);
            } catch (Throwable e) {
                if (e instanceof RuntimeException re)
                    throw re;
                throw runtimeWrapper.apply(e);
            }
        };
    }

    public static <R> R tryRun(ThrowingSupplier<R, ?> supplier, Function<Throwable, ? extends RuntimeException> runtimeWrapper) {
        try {
            return supplier.get();
        } catch (Throwable err) {
            throw runtimeWrapper.apply(err);
        }
    }

}
