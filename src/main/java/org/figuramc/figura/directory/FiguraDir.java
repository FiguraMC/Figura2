package org.figuramc.figura.directory;

import org.figuramc.figura.config.ConfigManager;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Central class for processing and obtaining the Mod Directory for a game instance.
 */
public class FiguraDir {

    private static final List<SubModDirectory> ALL_SUB_DIRECTORIES = new ArrayList<>();

    // Static fields storing, in one place, all the required subfolders in the mod directory,
    // so we're not using raw strings all over the codebase to access them.
    public static final SubModDirectory AVATARS = new SubModDirectory("avatars");
    public static final SubModDirectory CEM = new SubModDirectory("cem");
    public static final SubModDirectory EXPORTS = new SubModDirectory("exports");
    public static final SubModDirectory SHARED_SCRIPTS = new SubModDirectory("shared_scripts");

    // Fetch the main mod directory. If there isn't one, ask the player for it.
    // Note: As long as this is running on a separate thread, it won't block the entire client when asking for the directory, I think
    private static Path getModDirectory() throws IOException {
        // No race conditions from multiple threads needing to ask for it at once
        synchronized (ConfigManager.MOD_DIRECTORY) {
            // Fetch or ask for the mod directory
            @Nullable String modDirString = ConfigManager.MOD_DIRECTORY.getValue();
            if (modDirString == null) askPlayerForModDirectory();
            else if (!Files.isDirectory(Path.of(modDirString))) askPlayerForModDirectory();
            modDirString = ConfigManager.MOD_DIRECTORY.getValue();
            Path modDir = Path.of(modDirString);
            // Create sub-folders if they don't yet exist
            for (SubModDirectory subDir : ALL_SUB_DIRECTORIES)
                if (!Files.isDirectory(modDir.resolve(subDir.key))) Files.createDirectory(modDir.resolve(subDir.key));
            // Return it
            return modDir;
        }
    }

    // Ask the player for a mod directory path and save it in config.
    private static void askPlayerForModDirectory() {
        String path = TinyFileDialogs.tinyfd_selectFolderDialog("Choose or create a folder to use as your Figura mod directory!", "");
        System.out.println("Path = \"" + path + "\"");
        ConfigManager.MOD_DIRECTORY.setValue(path);
    }

    public record SubModDirectory(String key) {
        public SubModDirectory(String key) {
            this.key = key;
            ALL_SUB_DIRECTORIES.add(this);
        }
        public Path get() throws IOException {
            return getModDirectory().resolve(key);
        }
    }

}
