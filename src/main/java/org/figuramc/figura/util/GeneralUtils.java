package org.figuramc.figura.util;

import java.util.function.Supplier;

public class GeneralUtils {

    public static <R> R block(Supplier<R> supplier) {
        return supplier.get();
    }

}
