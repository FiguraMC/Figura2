package org.figuramc.figura.util;

import org.figuramc.figura.util.exception.functional.BiThrowingBiFunction;
import org.figuramc.figura.util.exception.functional.BiThrowingFunction;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

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

    // Recursively process a directory and the files in it.
    // "process" is called on non-directory files, and "gather" is called with the directory and the results.
    // If the root file does not exist, acts as if it's an empty directory.
    // Ignores "hidden" files (starting with dot, or hidden however your OS defines it)
    public static <T, E extends Throwable> T recursiveProcess(
            Path root,
            BiThrowingFunction<Path, @Nullable T, E, IOException> process,
            BiThrowingBiFunction<Path, ArrayList<T>, T, E, IOException> gather
    ) throws E, IOException {
        File f = root.toFile();
        if (f.isDirectory()) {
            // Sort files by name, so iteration order is consistent!
            List<File> files = Arrays.asList(f.listFiles());
            files.sort(Comparator.comparing(File::getName));
            ArrayList<T> list = ListUtils.<File, T, E, IOException>mapBiThrowingNonNull(files,
                    file -> file.isHidden() || file.getName().startsWith(".") ? null : recursiveProcess(file.toPath(), process, gather));
            return gather.apply(root, list);
        } else if (f.exists()) {
            return process.apply(root);
        } else return gather.apply(root, new ArrayList<>());
    }

    public static String stringRelativeTo(Path subfile, Path root) {
        subfile = subfile.toAbsolutePath();
        root = root.toAbsolutePath();
        subfile = root.relativize(subfile);
        return subfile.toString().replace(File.separatorChar, '/');
    }

}
