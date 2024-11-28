package org.figuramc.figura.util;

public class IOUtils {

    public static String stripExtension(String str, String extension) {
        if (str.endsWith("." + extension))
            return str.substring(0, str.length() - extension.length() - 1);
        return str;
    }

}
