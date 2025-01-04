package org.figuramc.figura.util;

import org.jetbrains.annotations.Nullable;

public class IOUtils {

    public static String stripExtension(String str, String extension) {
        if (str.endsWith("." + extension))
            return str.substring(0, str.length() - extension.length() - 1);
        return str;
    }

    public static @Nullable String getExtension(String str) {
        int idx = str.lastIndexOf('.');
        if (idx == -1) return null;
        return str.substring(idx + 1);
    }

}
